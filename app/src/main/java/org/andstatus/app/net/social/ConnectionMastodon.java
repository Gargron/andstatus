package org.andstatus.app.net.social;

import android.net.Uri;

import org.andstatus.app.net.http.ConnectionException;
import org.andstatus.app.util.MyLog;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class ConnectionMastodon extends Connection {
    static final String  APPLICATION_ID = "http://andstatus.org/andstatus";

    @Override
    protected String getApiPath1(ApiRoutineEnum routine) {
        String url;

        switch(routine) {
            case REGISTER_CLIENT:
                url = "apps";
                break;
            case STATUSES_HOME_TIMELINE:
                url = "statuses/home";
                break;
            case STATUSES_MENTIONS_TIMELINE:
                url = "statuses/mentions";
                break;
            default:
                url = "";
                break;
        }

        return prependWithBasicPath(url);
    }

    @Override
    public MbRateLimitStatus rateLimitStatus() throws ConnectionException {
        return null;
    }

    @Override
    public MbUser verifyCredentials() throws ConnectionException {
        return null;
    }

    @Override
    public MbMessage destroyFavorite(String statusId) throws ConnectionException {
        return null;
    }

    @Override
    public MbMessage createFavorite(String statusId) throws ConnectionException {
        return null;
    }

    @Override
    public boolean destroyStatus(String statusId) throws ConnectionException {
        return false;
    }

    @Override
    protected MbMessage getMessage1(String statusId) throws ConnectionException {
        return null;
    }

    @Override
    public MbMessage updateStatus(String message, String statusId, String inReplyToId, Uri mediaUri) throws ConnectionException {
        JSONObject formParams = new JSONObject();

        try {
            formParams.put("status", message);
            formParams.put("in_reply_to_id", inReplyToId);
        } catch (JSONException e) {
            MyLog.e(this, e);
        }

        JSONObject jso = http.postRequest(getApiPath(ApiRoutineEnum.POST_MESSAGE), formParams);
        return messageFromJson(jso);
    }

    private MbMessage messageFromJson(JSONObject jso) {
        if(jso == null) {
            return MbMessage.getEmpty();
        }

        return null;
    }

    @Override
    public MbMessage postDirectMessage(String message, String statusId, String userId, Uri mediaUri) throws ConnectionException {
        return null;
    }

    @Override
    public MbMessage postReblog(String rebloggedId) throws ConnectionException {
        return null;
    }

    @Override
    public List<MbTimelineItem> getTimeline(ApiRoutineEnum apiRoutine, TimelinePosition sinceId, int limit, String userId) throws ConnectionException {
        return null;
    }

    @Override
    public List<MbTimelineItem> search(TimelinePosition youngestPosition, int limit, String searchQuery) throws ConnectionException {
        return null;
    }

    @Override
    public MbUser followUser(String userId, Boolean follow) throws ConnectionException {
        return null;
    }

    @Override
    public MbUser getUser(String userId, String userName) throws ConnectionException {
        return null;
    }
}
