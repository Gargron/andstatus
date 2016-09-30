package org.andstatus.app.net.http;

import android.text.TextUtils;

import org.andstatus.app.net.social.Connection;

public class HttpConnectionOAuthMastodon extends HttpConnectionOAuthJavaNet {
    @Override
    protected String getApiUrl(Connection.ApiRoutineEnum routine) throws ConnectionException {
        String url;

        switch(routine) {
            case OAUTH_ACCESS_TOKEN:
            case OAUTH_REQUEST_TOKEN:
                url =  data.getOauthPath() + "/token";
                break;
            case OAUTH_AUTHORIZE:
                url = data.getOauthPath() + "/authorize";
                break;
            case OAUTH_REGISTER_CLIENT:
                url = data.getBasicPath() + "/apps";
                break;
            default:
                url = "";
                break;
        }

        if (!TextUtils.isEmpty(url)) {
            url = pathToUrlString(url);
        }

        return url;
    }
}
