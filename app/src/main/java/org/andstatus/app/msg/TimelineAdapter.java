/*
 * Copyright (c) 2015 yvolk (Yuri Volkov), http://yurivolkov.com
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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.andstatus.app.R;
import org.andstatus.app.context.MyPreferences;
import org.andstatus.app.util.MyLog;
import org.andstatus.app.util.MyUrlSpan;
import org.andstatus.app.widget.MyBaseAdapter;

/**
 * @author yvolk@yurivolkov.com
 */
public class TimelineAdapter extends MyBaseAdapter {
    private final MessageContextMenu contextMenu;
    private final int listItemLayoutId;
    private final TimelinePages pages;
    private final boolean showAvatars = MyPreferences.showAvatars();

    public TimelineAdapter(MessageContextMenu contextMenu, int listItemLayoutId,
                           TimelineAdapter oldAdapter, TimelinePage loadedPage) {
        this.contextMenu = contextMenu;
        this.listItemLayoutId = listItemLayoutId;
        this.pages = new TimelinePages( oldAdapter == null ? null : oldAdapter.getPages(), loadedPage);
    }

    @Override
    public int getCount() {
        return pages.getItemsCount();
    }

    @Override
    public TimelineViewItem getItem(View view) {
        return (TimelineViewItem) super.getItem(view);
    }

    @Override
    public TimelineViewItem getItem(int position) {
        return pages.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).msgId;
    }

    public TimelinePages getPages() {
        return pages;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView == null ? newView() : convertView;
        view.setOnCreateContextMenuListener(contextMenu);
        view.setOnClickListener(contextMenu);
        setPosition(view, position);
        TimelineViewItem item = getItem(position);
        MyUrlSpan.showText(view, R.id.message_author, item.authorName, false);
        MyUrlSpan.showText(view, R.id.message_body, item.body, false);
        MyUrlSpan.showText(view, R.id.message_details, item.getDetails(contextMenu.getContext()), false);
        if (showAvatars) {
            showAvatar(item, view);
        }
        showAttachedImage(item, view);
        showFavorited(item, view);
        return view;
    }

    private View newView() {
        return LayoutInflater.from(contextMenu.messageList.getActivity()).inflate(listItemLayoutId, null);
    }

    private void showAvatar(TimelineViewItem item, View view) {
        ImageView avatar = (ImageView) view.findViewById(R.id.avatar_image);
        avatar.setImageDrawable(item.getAvatar());
    }

    private void showAttachedImage(TimelineViewItem item, View view) {
        ImageView imageView = (ImageView) view.findViewById(R.id.attached_image);
        if (item.getAttachedImage() != null) {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageDrawable(item.getAttachedImage());
        } else {
            imageView.setVisibility(View.GONE);
        }
    }

    private void showFavorited(TimelineViewItem item, View view) {
        View favorited = view.findViewById(R.id.message_favorited);
        favorited.setVisibility(item.favorited ? View.VISIBLE : View.GONE );
    }

    @Override
    public String toString() {
        return MyLog.formatKeyValue(this, pages);
    }
}
