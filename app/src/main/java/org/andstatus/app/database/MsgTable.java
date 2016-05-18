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

package org.andstatus.app.database;

import android.provider.BaseColumns;

import org.andstatus.app.data.DownloadStatus;

/**
 * Table for both public and direct messages
 * i.e. for tweets, dents, notices
 * and also for "direct messages", "direct dents" etc.
 */
public final class MsgTable implements BaseColumns {
    public static final String TABLE_NAME = "msg";

    private MsgTable() {
    }

    // Table columns are below:
    /*
     * {@link BaseColumns#_ID} is primary key in this database
     * No, we can not rename the {@link BaseColumns#_ID}, it should always be "_id".
     * See <a href="http://stackoverflow.com/questions/3359414/android-column-id-does-not-exist">Android column '_id' does not exist?</a>
     */

    /**
     * ID of the originating (source) system (twitter.com, identi.ca, ... ) where the row was created
     * See {@link OriginTable#_ID}
     */
    public static final String ORIGIN_ID =  "origin_id";
    /**
     * ID in the originating system
     * The id is not unique for this table, because we have IDs from different systems in one column
     * and IDs from different systems may overlap.
     */
    public static final String MSG_OID = "msg_oid";
    /**
     * See {@link DownloadStatus}. Defaults to {@link DownloadStatus#UNKNOWN}
     */
    public static final String MSG_STATUS = "msg_status";
    /**
     * A link to the representation of the resource. Currently this is simply URL to the HTML
     * representation of the resource (its "permalink")
     */
    public static final String URL = "url";
    /**
     * Author of the message = User._ID
     * If message was "Reblogged" ("Retweeted", "Repeated", ...) this is Original author (whose message was reblogged)
     */
    public static final String AUTHOR_ID = "author_id";
    /**
     * Sender of the message = User._ID
     */
    public static final String SENDER_ID = "sender_id";
    /**
     * Recipient of the message = User._ID
     * null for public messages
     * not null for direct messages
     */
    public static final String RECIPIENT_ID = "recipient_id";
    /**
     * Text of the message ("TEXT" may be reserved word so it was renamed here)
     */
    public static final String BODY = "body";
    /**
     * String generally describing Client's software used to post this message
     * It's like "User Agent" string in the browsers?!: "via ..."
     * (This is "source" field in tweets)
     */
    public static final String VIA = "via";
    /**
     * If not null: Link to the Msg._ID in this table
     */
    public static final String IN_REPLY_TO_MSG_ID = "in_reply_to_msg_id";
    /**
     * If not null: to which Sender this message is a reply = User._ID
     * This field is not necessary but speeds up IN_REPLY_TO_NAME calculation
     */
    public static final String IN_REPLY_TO_USER_ID = "in_reply_to_user_id";
    /**
     * Date and time when the row was created in the originating system.
     * We store it as long milliseconds.
     */
    public static final String CREATED_DATE = "msg_created_date";
    /**
     * Date and time when the message was sent,
     * it's not equal to {@link MsgTable#CREATED_DATE} for reblogged messages
     * We change the value if we reblog the message in the application
     * or if we receive new reblog of the message
     * This value is set for unsent messages also. So it is updated after successful retrieval
     * of this sent message from a Social Network.
     */
    public static final String SENT_DATE = "msg_sent_date";
    /**
     * Date and time the row was inserted into this database
     */
    public static final String INS_DATE = "msg_ins_date";
    /**
     * The Msg is public
     */
    public static final String PUBLIC = "public";

    /*
     * Derived columns (they are not stored in this table but are result of joins and aliasing)
     */
    /**
     * Alias for the primary key
     */
    public static final String MSG_ID =  "msg_id";

    public static final String DESC_SORT_ORDER = SENT_DATE + " DESC";
    public static final String ASC_SORT_ORDER = SENT_DATE + " ASC";
}