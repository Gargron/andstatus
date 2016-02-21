/* 
 * Copyright (c) 2014 yvolk (Yuri Volkov), http://yurivolkov.com
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

package org.andstatus.app.msg;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.ListView;

import org.andstatus.app.context.MyPreferences;
import org.andstatus.app.data.MyDatabase;
import org.andstatus.app.data.TimelineType;
import org.andstatus.app.util.MyLog;

import java.util.Date;

/**
 * Determines where to save / retrieve position in the list
 * Information on two rows is stored for each "position" hence two keys. 
 * Plus Query string is being stored for the search results.
 * 2014-11-15 We are storing {@link MyDatabase.Msg#SENT_DATE} for the last item to retrieve, not its ID as before
 * @author yvolk@yurivolkov.com
 */
class TimelineListPositionStorage {
    public static final String TAG = TimelineListPositionStorage.class.getSimpleName();
    private static final String KEY_PREFIX = "timeline_position_";

    private final TimelineAdapter mAdapter;
    private final ListView mListView;
    private final TimelineListParameters mListParameters;

    /**
     * SharePreferences to use for storage 
     */
    private final SharedPreferences sp = MyPreferences.getDefaultSharedPreferences();
    private String keyFirstVisibleItemId = "";
    private String keyMinSentDate = "";
    private String keyQueryString = "";
    private final String queryString;
    
    TimelineListPositionStorage(TimelineAdapter listAdapter, ListView listView, TimelineListParameters listParameters) {
        this.mAdapter = listAdapter;
        this.mListView = listView;
        this.mListParameters = listParameters;
        
        queryString = listParameters.mSearchQuery;
        long userId = 0;
        if (listParameters.mTimelineType == TimelineType.USER) {
            userId = listParameters.mSelectedUserId;
        } else if (!listParameters.mTimelineCombined) {
            userId = listParameters.myAccountUserId;
        }
        keyFirstVisibleItemId = KEY_PREFIX
                + listParameters.mTimelineType.save()
                + "_user" + Long.toString(userId)
                + (TextUtils.isEmpty(queryString) ? "" : "_search");
        keyMinSentDate = keyFirstVisibleItemId + "_last";
        keyQueryString = keyFirstVisibleItemId + "_query_string";
    }

    void save() {
        final String method = "save";
        if (mListView == null || mAdapter == null || mListParameters.isEmpty() || mAdapter.getCount() == 0) {
            MyLog.v(this, method + "; skipped");
            return;
        }
        TimelineAdapter la = mAdapter;
        int firstVisiblePosition = mListView.getFirstVisiblePosition();
        int itemCount = la.getCount();
        if (firstVisiblePosition >= itemCount) {
            firstVisiblePosition = itemCount - 1;
        }
        long firstVisibleItemId = 0;
        int lastPosition = -1;
        long minSentDate = 0;
        if (firstVisiblePosition >= 0) {
            firstVisibleItemId = la.getItemId(firstVisiblePosition);
            MyLog.v(this, method + "; firstVisiblePos:" + firstVisiblePosition + " of " + itemCount
                    + "; itemId:" + firstVisibleItemId);
            lastPosition = mListView.getLastVisiblePosition() + 10;
            if (lastPosition >= itemCount) {
                lastPosition = itemCount - 1;
            }
            minSentDate = la.getItem(lastPosition).sentDate;
        }

        if (firstVisibleItemId <= 0) {
            clear();
        } else {
            put(firstVisibleItemId, minSentDate);
        }
        if (MyLog.isVerboseEnabled()) {
            String msgLog = " key=" + keyFirstVisibleItemId
                    + (TextUtils.isEmpty(queryString) ? "" : ", q='" + queryString + "'")
                    + ", id=" + firstVisibleItemId + " at pos=" + firstVisiblePosition
                    + ", minDate=" + minSentDate
                    + " at pos=" + lastPosition + " of " + itemCount;
            if (firstVisibleItemId <= 0) {
                MyLog.v(this, method + "; failed " + msgLog
                        + "\n no visible items for " + mListParameters.toTimelineTitleAndSubtitle());
            } else {
                MyLog.v(this, method + "; succeeded " + msgLog);
            }
        }

    }
    
    void put(long firstVisibleItemId, long minSentDate) {
        sp.edit().putLong(keyFirstVisibleItemId, firstVisibleItemId)
        .putLong(keyMinSentDate, minSentDate)
        .putString(keyQueryString, queryString).apply();
    }

    private static final long NOT_FOUND_IN_LIST_POSITION_STORAGE = -4;
    private static final long NOT_FOUND_IN_SHARED_PREFERENCES = -1;
    /** Valid id is > 0 */
    long getFirstVisibleItemId() {
        long savedItemId = NOT_FOUND_IN_LIST_POSITION_STORAGE;
        if (isThisPositionStored()) {
            savedItemId = sp.getLong(keyFirstVisibleItemId, NOT_FOUND_IN_SHARED_PREFERENCES);
        }
        return savedItemId;
    }
    
    private boolean isThisPositionStored() {
        return queryString.compareTo(sp.getString(
                        keyQueryString, "")) == 0;
    }

    /** @return 0 if not found */
    long getMinSentDate() {
        long date = 0;
        if (isThisPositionStored()) {
            date = sp.getLong(keyMinSentDate, 0);
        }
        return date;
    }
    
    void clear() {
        sp.edit().remove(keyFirstVisibleItemId).remove(keyMinSentDate)
                .remove(keyQueryString).apply();
        if (MyLog.isVerboseEnabled()) {
            MyLog.v(this, "Position forgot key=" + keyFirstVisibleItemId);
        }
    }
    
    /**
     * Restore (the first visible item) position saved for this timeline
     */
    public void restore() {
        final String method = "restore";
        if (mListView == null || mAdapter == null || mListParameters.isEmpty() || mAdapter.getCount() == 0) {
            MyLog.v(this, method + "; skipped");
            return;
        }
        boolean restored = false;
        int position = -1;
        long firstItemId = -3;
        try {
            firstItemId = getFirstVisibleItemId();
            if (firstItemId > 0) {
                position = getPositionById(firstItemId);
            }
            if (position >= 0) {
                mListView.setSelectionFromTop(position, 0);
                restored = true;
            } else {
                // There is no stored position
                if (mListParameters.whichPage.isYoungest()
                        || !TextUtils.isEmpty(mListParameters.mSearchQuery)
                        || mListView.getCount() < 10) {
                    // In search mode start from the most recent message!
                    position = 0;
                } else {
                    position = mListView.getCount() - 10;

                }
                if (position >= 0) {
                    setPosition(mListView, position);
                }
            }
        } catch (Exception e) {
            MyLog.v(this, method, e);
        }
        if (MyLog.isVerboseEnabled()) {
            MyLog.v(this, method + "; " + (restored ? "succeeded" : "failed" )
                    + " key=" + keyFirstVisibleItemId
                    + (TextUtils.isEmpty(queryString) ? "" : ", q='" + queryString + "'")
                    + ", id=" + firstItemId + " at pos=" + position
                    + " of " + mListView.getCount());
        }
        if (!restored) {
            clear();
        }
        mAdapter.setPositionRestored(true);
    }

    public static void setPosition(ListView listView, int position) {
        if (listView == null) {
            return;
        }
        int viewHeight = listView.getHeight();
        int childHeight = 30;
        int y = viewHeight - childHeight;
        if (MyLog.isVerboseEnabled()) {
            MyLog.v(TAG, "Set position of " + position + " item to " + y + "px");
        }
        listView.setSelectionFromTop(position, y);
    }

    /**
     * @return the position in the list or -1 if the item was not found
     */
    private int getPositionById(long itemId) {
        if (mAdapter == null) {
            return -1;
        }
        return mAdapter.getPositionById(itemId);
    }
}