/*
 * Copyright (c) 2016 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.andstatus.app.service;

import android.text.TextUtils;

import org.andstatus.app.R;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.data.DataInserter;
import org.andstatus.app.database.DatabaseHolder;
import org.andstatus.app.data.FriendshipValues;
import org.andstatus.app.data.LatestTimelineItem;
import org.andstatus.app.data.LatestUserMessages;
import org.andstatus.app.data.MyQuery;
import org.andstatus.app.data.TimelineType;
import org.andstatus.app.data.UserMsg;
import org.andstatus.app.net.http.ConnectionException;
import org.andstatus.app.net.social.Connection;
import org.andstatus.app.net.social.MbUser;
import org.andstatus.app.util.MyLog;
import org.andstatus.app.util.RelativeTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author yvolk@yurivolkov.com
 */
public class CommandExecutorFollowers extends CommandExecutorStrategy {
    long userId = 0;
    String userOid = "";
    List<MbUser> usersNew = new ArrayList<>();
    String commandSummary = "";

    @Override
    void execute() {
        commandSummary = execContext.getCommandSummary();
        try {
            TimelineType timelineType = getTimelineType();
            if (lookupUser()) return;
            switch (timelineType) {
                case FOLLOWERS:
                    syncFollowers();
                    break;
                case FRIENDS:
                    syncFriends();
                    break;
                default:
                    MyLog.e(this, "Unexpected timeline or command here: " + commandSummary);
                    break;
            }

            LatestTimelineItem latestTimelineItem = new LatestTimelineItem(timelineType, userId);
            latestTimelineItem.onTimelineDownloaded();
            latestTimelineItem.save();

            MyLog.d(this, commandSummary + " ended, " + usersNew.size() + " users");
            logOk(true);
        } catch (ConnectionException e) {
            logConnectionException(e, commandSummary);
        }
    }

    private TimelineType getTimelineType() {
        TimelineType timelineType;
        switch (execContext.getCommandData().getCommand()) {
            case GET_FOLLOWERS:
                timelineType = TimelineType.FOLLOWERS;
                break;
            case GET_FRIENDS:
                timelineType = TimelineType.FRIENDS;
                break;
            default:
                timelineType = execContext.getTimelineType();
                break;
        }
        return timelineType;
    }

    private boolean lookupUser() {
        final String method = "lookupUser";
        userId = execContext.getCommandData().itemId;
        if (userId == 0) {
            userId = execContext.getTimelineUserId();
        }
        userOid = MyQuery.idToOid(DatabaseHolder.OidEnum.USER_OID, userId, 0);
        if (TextUtils.isEmpty(userOid)) {
            execContext.getResult().incrementParseExceptions();
            MyLog.e(this, method + "; userOid not found for id: " + userId);
            return true;
        }
        return false;
    }

    private void syncFollowers() throws ConnectionException {
        if (execContext.getMyAccount().getConnection()
                .isApiSupported(Connection.ApiRoutineEnum.GET_FOLLOWERS)) {
            usersNew = execContext.getMyAccount().getConnection().getFollowers(userOid);
        } else if (execContext.getMyAccount().getConnection()
                .isApiSupported(Connection.ApiRoutineEnum.GET_FOLLOWERS_IDS)) {
            List<String> userOidsNew =
                    execContext.getMyAccount().getConnection().getFollowersIds(userOid);
            if (getUsersForOids(userOidsNew, usersNew)) return;
        } else {
            throw new ConnectionException(ConnectionException.StatusCode.UNSUPPORTED_API,
                    Connection.ApiRoutineEnum.GET_FOLLOWERS
                    + " and " + Connection.ApiRoutineEnum.GET_FOLLOWERS_IDS);
        }

        Set<Long> userIdsOld = MyQuery.getFollowersIds(userId);
        execContext.getResult().incrementDownloadedCount();
        broadcastProgress(execContext.getContext().getText(R.string.followers).toString()
                + ": " + userIdsOld.size() + " -> " + usersNew.size(), false);

        if (updateNewUsersAndTheirLatestMessages(usersNew)) return;

        for (MbUser mbUser : usersNew) {
            userIdsOld.remove(mbUser.userId);
            FriendshipValues.setFollowed(mbUser.userId, userId);
        }
        for (long userIdOld : userIdsOld) {
            FriendshipValues.setNotFollowed(userIdOld, userId);
        }
    }

    private void syncFriends() throws ConnectionException {
        if (execContext.getMyAccount().getConnection()
                .isApiSupported(Connection.ApiRoutineEnum.GET_FRIENDS)) {
            usersNew = execContext.getMyAccount().getConnection().getFriends(userOid);
        } else if (execContext.getMyAccount().getConnection()
                .isApiSupported(Connection.ApiRoutineEnum.GET_FRIENDS_IDS)) {
            List<String> userOidsNew =
                    execContext.getMyAccount().getConnection().getFriendsIds(userOid);
            if (getUsersForOids(userOidsNew, usersNew)) return;
        } else {
            throw new ConnectionException(ConnectionException.StatusCode.UNSUPPORTED_API,
                    Connection.ApiRoutineEnum.GET_FRIENDS
                            + " and " + Connection.ApiRoutineEnum.GET_FRIENDS_IDS);
        }

        Set<Long> userIdsOld = MyQuery.getFriendsIds(userId);
        execContext.getResult().incrementDownloadedCount();
        broadcastProgress(execContext.getContext().getText(R.string.friends).toString()
                + ": " + userIdsOld.size() + " -> " + usersNew.size(), false);

        if (updateNewUsersAndTheirLatestMessages(usersNew)) return;

        for (MbUser mbUser : usersNew) {
            userIdsOld.remove(mbUser.userId);
            FriendshipValues.setFollowed(userId, mbUser.userId);
        }
        for (long userIdOld : userIdsOld) {
            FriendshipValues.setNotFollowed(userId, userIdOld);
        }
    }

    private boolean getUsersForOids(List<String> userOidsNew, List<MbUser> usersNew) {
        long count = 0;
        for (String userOidNew : userOidsNew) {
            MbUser mbUser = null;
            try {
                count++;
                mbUser = execContext.getMyAccount().getConnection().getUser(userOidNew, null);
                execContext.getResult().incrementDownloadedCount();
            } catch (ConnectionException e) {
                long userId = MyQuery.oidToId(DatabaseHolder.OidEnum.USER_OID,
                        execContext.getMyAccount().getOriginId(), userOidNew);
                if (userId == 0) {
                    MyLog.i(this, "Failed to identify a User for oid=" + userOidNew, e);
                } else {
                    MyLog.v(this, "Server doesn't return User object for oid=" + userOidNew, e);
                    mbUser = MbUser.fromOriginAndUserOid(
                            execContext.getMyAccount().getOriginId(), userOidNew);
                    mbUser.userId = userId;
                }
            }
            if (mbUser != null) {
                broadcastProgress(String.valueOf(count) + ". "
                        + execContext.getContext().getText(R.string.get_user)
                        + ": " + mbUser.getNamePreferablyWebFingerId(), true);
                usersNew.add(mbUser);
            }
            if (logSoftErrorIfStopping()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if we need to interrupt process
     */
    private boolean updateNewUsersAndTheirLatestMessages(List<MbUser> usersNew) {
        DataInserter di = new DataInserter(execContext);
        LatestUserMessages lum = new LatestUserMessages();
        boolean messagesLoaded = false;
        long count = 0;
        for (MbUser mbUser : usersNew) {
            count++;
            broadcastProgress(String.valueOf(count) + ". "
                    + execContext.getContext().getText(R.string.button_save)
                    + ": " + mbUser.getNamePreferablyWebFingerId(), true);
            di.insertOrUpdateUser(mbUser, lum);
            if (mbUser.hasLatestMessage()) {
                messagesLoaded = true;
            }
        }
        if (!messagesLoaded) {
            count = 0;
            for (MbUser mbUser : usersNew) {
                count++;
                try {
                    broadcastProgress(String.valueOf(count) + ". "
                            + execContext.getContext().getText(R.string.title_command_get_status)
                            + ": " + mbUser.getNamePreferablyWebFingerId(), true);
                    di.downloadOneMessageBy(mbUser.oid, lum);
                    execContext.getResult().incrementDownloadedCount();
                } catch (ConnectionException e) {
                    long lastMsgId = MyQuery.userIdToLongColumnValue(DatabaseHolder.User.USER_MSG_ID,
                            mbUser.userId);
                    if (lastMsgId == 0) {
                        lastMsgId = MyQuery.conditionToLongColumnValue(DatabaseHolder.Msg.TABLE_NAME,
                                DatabaseHolder.Msg._ID,
                                DatabaseHolder.Msg.SENDER_ID + "=" + mbUser.userId
                        + " ORDER BY " + DatabaseHolder.Msg.SENT_DATE + " DESC LIMIT 0,0");
                    }
                    if (lastMsgId == 0) {
                        MyLog.v(this, "Failed to find User's message for "
                                + mbUser.getNamePreferablyWebFingerId(), e);
                    } else {
                        long sentDate = MyQuery.msgIdToLongColumnValue(DatabaseHolder.Msg.SENT_DATE, lastMsgId);
                        lum.onNewUserMsg(new UserMsg(mbUser.userId, lastMsgId, sentDate));
                        MyLog.v(this, "Server didn't return User's message for "
                                        + mbUser.getNamePreferablyWebFingerId()
                                        + " found msg " + RelativeTime.
                                        getDifference(MyContextHolder.get().context(), sentDate),
                                e);
                    }
                }
                if (logSoftErrorIfStopping()) {
                    return true;
                }
            }
        }
        lum.save();
        return false;
    }
}
