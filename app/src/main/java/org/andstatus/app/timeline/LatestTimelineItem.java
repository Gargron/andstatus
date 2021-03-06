/**
 * Copyright (C) 2013-2016 yvolk (Yuri Volkov), http://yurivolkov.com
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

package org.andstatus.app.timeline;

import org.andstatus.app.data.MyQuery;
import org.andstatus.app.net.social.TimelinePosition;
import org.andstatus.app.util.MyLog;

import java.util.Date;

/**
 * Retrieve and save information about position in a Timeline of the latest downloaded timeline item.
 * The "timeline item" is e.g. a "message" for Twitter and an "Activity" for Pump.Io.
 */
public class LatestTimelineItem {
    private static final String TAG = LatestTimelineItem.class.getSimpleName();

    private final Timeline timeline;
    /** The timeline is of this User, for all timeline types. */
    private long userId = 0;
    
    private boolean maySaveThis = false;
    
    TimelinePosition position = TimelinePosition.getEmpty();
    /** 0 - none were downloaded */
    long timelineItemDate = 0;
    /**
     * Last date when this timeline was successfully downloaded.
     * It is used to know when it will be time for the next automatic update
     */
    long timelineDownloadedDate = 0;
    
    /**
     * We will update only what really changed
     */
    private boolean timelineItemChanged = false;
    private boolean timelineDateChanged = false;
    
    /**
     * Retrieve information about the last downloaded message from this timeline
     */
    public LatestTimelineItem(Timeline timeline) {
        this.timeline = timeline;
        maySaveThis = timeline.isValid();

        timelineDownloadedDate = timeline.getYoungestSyncedDate();
        timelineItemDate = timeline.getYoungestItemDate();
        position = new TimelinePosition(timeline.getYoungestPosition());
    }
    
    /**
     * @return Id of the last downloaded message from this timeline
     */
    public TimelinePosition getPosition() {
        return position;
    }

    /**
     * @return Sent Date of the last downloaded message from this timeline
     */
    public long getTimelineItemDate() {
        return timelineItemDate;
    }

    /**
     * @return Last date when this timeline was successfully downloaded
     */
    public long getTimelineDownloadedDate() {
        return timelineDownloadedDate;
    }

    /** A new Timeline Item was downloaded   */
    public void onNewMsg(TimelinePosition timelineItemPosition, long timelineItemDate) {
        if (timelineItemPosition != null 
                && !timelineItemPosition.isEmpty() 
                && (timelineItemDate > this.timelineItemDate)) {
            this.timelineItemDate = timelineItemDate;
            this.position = timelineItemPosition;
            timelineItemChanged = true;
        }
    }
    
    public void onTimelineDownloaded() {
        timelineDownloadedDate = System.currentTimeMillis();
        timelineDateChanged = true;
    }
    
    /** Save to the Timeline (not to the Database yet... */
    public void save() {
        if (MyLog.isVerboseEnabled()) {
            MyLog.v(this, this.toString());
        }
        if (maySaveThis && changed()) {
            saveChanged();
        }
    }
    
    private boolean changed() {
        return timelineDateChanged || timelineItemChanged;
    }
    
    @Override
    public String toString() {
        return TAG + "[" + timeline.toString()
                    + (getTimelineDownloadedDate() > 0
                            ? " downloaded at " + (new Date(getTimelineDownloadedDate()).toString()) 
                            : " never downloaded")
                    + (changed() ? "" : " not changed")                    
                    + " latest position=" + MyQuery.quoteIfNotQuoted(position.getPosition())
                    + "]";
    }

    private void saveChanged() {
        if (timelineDateChanged) {
            timeline.setYoungestSyncedDate(timelineDownloadedDate);
        }
        if (timelineItemChanged) {
            timeline.setYoungestItemDate(timelineItemDate);
            timeline.setYoungestPosition(position.getPosition());
        }
    }

    public void clearPosition() {
        position = new TimelinePosition("");
    }
}
