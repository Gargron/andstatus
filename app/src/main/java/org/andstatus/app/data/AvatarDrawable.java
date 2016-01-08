/**
 * Copyright (C) 2014 yvolk (Yuri Volkov), http://yurivolkov.com
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

package org.andstatus.app.data;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;

import org.andstatus.app.R;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.context.MyTheme;
import org.andstatus.app.util.MyLog;

public class AvatarDrawable {
    private final long userId;
    private final DownloadFile downloadFile;
    public static final int AVATAR_SIZE_DIP = 48;
    public static final int AVATAR_PADDING_LEFT_DIP = 2;
    public static final int AVATAR_PADDING_TOP_DIP = 6;
    public static final int AVATAR_PADDING_RIGHT_DIP = 6;
    public static final int AVATAR_PADDING_BOTTOM_DIP = 2;

    private static final Drawable DEFAULT_AVATAR = loadDefaultAvatar(false);
    private static final Drawable DEFAULT_AVATAR_LIGHT = loadDefaultAvatar(true);
    
    public AvatarDrawable(long userIdIn, String filename) {
        userId = userIdIn;
        downloadFile = new DownloadFile(filename);
    }
    
    private static Drawable loadDefaultAvatar(boolean lightTheme) {
        Drawable avatar = null;
        MyLog.v(AvatarDrawable.class, "Loading default avatar");
        Context context = MyContextHolder.get().context();
        if (context != null) {
            avatar = getDrawableCompat(context,
                    lightTheme ? R.drawable.ic_action_user_light : R.drawable.ic_action_user);
        }
        return avatar;
    }

    public static Drawable getDefaultDrawable() {
        return MyTheme.isThemeLight() ? DEFAULT_AVATAR_LIGHT : DEFAULT_AVATAR;
    }
    
    public Drawable getDrawable() {
        if (downloadFile.exists()) {
            return AttachedImageDrawable.drawableFromPath(this, downloadFile.getFile().getAbsolutePath());
        } 
        AvatarData.asyncRequestDownload(userId);
        return getDefaultDrawable();
    }

    @Override
    public String toString() {
        return "AvatarDrawable [userId=" + userId + ", " + downloadFile + "]";
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Drawable getDrawableCompat(Context context, int drawableId) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)  {
            return context.getTheme().getDrawable(drawableId);
        } else {
            return context.getResources().getDrawable(drawableId);
        }
    }
}
