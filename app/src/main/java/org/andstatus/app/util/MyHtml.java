/*
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

package org.andstatus.app.util;

import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.util.Linkify;

public class MyHtml {
    private MyHtml() {
        // Empty
    }

    public static String prepareForView(String text) {
        String text2 = stripUnnecessaryNewlines(text);
        if (text2.endsWith("</p>") && StringUtils.countOfOccurrences(text2, "<p") == 1) {
            text2 = text2.replaceAll("<p[^>]*>","").replaceAll("</p>","");
        }
        return text2;
    }

	public static String htmlify(String text) {
		if (TextUtils.isEmpty(text)) {
			return "";
		}
        String text2 = hasHtmlMarkup(text) ? text : htmlifyPlain(text);
        return stripUnnecessaryNewlines(text2);
    }
	
    private static String htmlifyPlain(String text) {
        SpannableString spannable = SpannableString.valueOf(text);
        Linkify.addLinks(spannable, Linkify.WEB_URLS);
        return Html.toHtml(spannable);
    }

    /** Strips HTML markup from the String */
    public static String fromHtml(String text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        } else {
            String text2 = text;
            if ( MyHtml.hasHtmlMarkup(text2)) {
                text2 = Html.fromHtml(text2).toString();
            }
            return stripUnnecessaryNewlines(text2);
        }
    }

    public static String stripUnnecessaryNewlines(String text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        } else {
            String text2 = text.trim();
            String NEWLINE_SEARCH = "\n";
            String NEWLINE_REPLACE = "\n";
            text2 = text2.replaceAll(NEWLINE_SEARCH + "\\s*" + NEWLINE_SEARCH, NEWLINE_REPLACE);
            if (text2.endsWith(NEWLINE_REPLACE)) {
                text2 = text2.substring(0, text2.length() - NEWLINE_REPLACE.length());
            }
            return text2;
        }
    }


    /** Very simple method  
     */
    public static boolean hasHtmlMarkup(String text) {
        boolean has = false;
        if (text != null){
            has = text.contains("<") && text.contains(">");
        }
        return has; 
    }

}
