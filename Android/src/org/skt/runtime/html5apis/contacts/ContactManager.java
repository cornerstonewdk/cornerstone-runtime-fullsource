/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package org.skt.runtime.html5apis.contacts;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;
import org.skt.runtime.html5apis.DeviceAPIErrors;
import org.skt.runtime.html5apis.calendar.CalendarManager;

import android.util.Log;

public class ContactManager extends Plugin {

    private ContactAccessor contactAccessor;
    private static final String LOG_TAG = "Contact Query";

    /**
     * Constructor.
     */
    public ContactManager() {
    }

    /**
     * Executes the request and returns PluginResult.
     * 
     * @param action    The action to execute.
     * @param args      JSONArry of arguments for the plugin.
     * @param callbackId  The callback id used when calling back into JavaScript.
     * @return        A PluginResult object with a status and message.
     */
    public PluginResult execute(String action, JSONArray args, String callbackId) {
        PluginResult.Status status = PluginResult.Status.OK;
        String result = "";

        /**
         * Check to see if we are on an Android 1.X device.  If we are return an error as we 
         * do not support this as of SKTRuntime 1.0.
         */
        if (android.os.Build.VERSION.RELEASE.startsWith("1.")) {
            return new PluginResult(PluginResult.Status.ERROR, createErrorObject(DeviceAPIErrors.NOT_SUPPORTED_ERR,"not support error"));
        }

        /**
         * Only create the contactAccessor after we check the Android version or the program will crash
         * older phones.
         */
        if (this.contactAccessor == null) {
            this.contactAccessor = new ContactAccessorSdk5(this.webView, this.ctx.getContext());
        }

        try {
            if (action.equals("search")) {
                JSONArray res = contactAccessor.search(args.getJSONArray(0), args.optJSONObject(1));
                return new PluginResult(status, res);
            }
            else if(action.equals("pickcontact")){
            	JSONArray res = contactAccessor.pickcontact(args.optJSONObject(0));
                return new PluginResult(status, res);
            }
            else if (action.equals("add")) {
                String id = contactAccessor.add(args.getJSONObject(0));
                if (id != null) {
                    JSONObject res = contactAccessor.getContactById(id);
                    if (res != null) {
                        return new PluginResult(status, res);
                    }
                }
            }
            else if (action.equals("remove")) {
                if (contactAccessor.remove(args.getString(0))) {
                    return new PluginResult(status, result);
                }
            }
            // If we get to this point an error has occurred
            return new PluginResult(PluginResult.Status.ERROR, createErrorObject(DeviceAPIErrors.UNKNOWN_ERR,"unknown error"));
        } catch (JSONException e) {
          Log.e(LOG_TAG, e.getMessage(), e);
          return new PluginResult(PluginResult.Status.JSON_EXCEPTION);
        }
    }
}
