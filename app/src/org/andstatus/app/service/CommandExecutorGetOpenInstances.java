/*
 * Copyright (C) 2015 yvolk (Yuri Volkov), http://yurivolkov.com
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

import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.net.http.ConnectionException;
import org.andstatus.app.net.social.Connection;
import org.andstatus.app.net.social.MbOrigin;
import org.andstatus.app.origin.DiscoveredOrigins;
import org.andstatus.app.origin.Origin;
import org.andstatus.app.origin.OriginConnectionData;
import org.andstatus.app.origin.OriginType;
import org.andstatus.app.util.MyLog;
import org.andstatus.app.util.TriState;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CommandExecutorGetOpenInstances extends CommandExecutorStrategy {

    @Override
    void execute() {
        boolean ok = false;
        OriginType originType = OriginType.fromId(execContext.getCommandData().itemId);
        List<MbOrigin> result = null;
        try {
            result = newConnection(originType).getOpenInstances();
            ok = !result.isEmpty();
            logOk(ok);
        } catch (ConnectionException e) {
            logConnectionException(e, "");
        }
        if (ok) {
            List<Origin> newOrigins = new ArrayList<Origin>();
            for (MbOrigin mbOrigin : result) {
                execContext.getResult().incrementDownloadedCount();
                Origin origin = new Origin.Builder(originType).setName(mbOrigin.name)
                        .setHostOrUrl(mbOrigin.urlString)
                        .build();
                if (origin.isValid()
                        && !MyContextHolder.get().persistentOrigins().fromName(origin.getName())
                                .isValid()
                        && !haveOriginsWithThisHostName(origin.getUrl())) {
                    newOrigins.add(origin);
                } else {
                    MyLog.d(this, "Origin is not valid: " + origin.toString());
                }
            }
            DiscoveredOrigins.addAll(newOrigins);
        }
    }

    private Connection newConnection(OriginType originType) throws ConnectionException {
        Origin origin1 = (new Origin.Builder(originType)).setName("non-existent").build();
        OriginConnectionData connectionData = origin1.getConnectionData(TriState.UNKNOWN);
        Connection connection;
        try {
            connection = connectionData.getConnectionClass().newInstance();
            connection.enrichConnectionData(connectionData);
            connection.setAccountData(connectionData);
        // TODO: Since API19 we will use ReflectiveOperationException as a common superclass 
        // of these two exceptions: InstantiationException and IllegalAccessException
        } catch (InstantiationException|IllegalAccessException e) {
            throw new ConnectionException(origin1.toString(), e);
        }
        return connection;
    }

    private boolean haveOriginsWithThisHostName(URL url) {
        if (url == null) {
            return true;
        }
        for (Origin origin : MyContextHolder.get().persistentOrigins().collection()) {
            if ( origin.getUrl() != null && origin.getUrl().getHost().equals(url.getHost())) {
                return true;
            }
        }
        return false;
    }

}
