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
package org.skt.runtime.html5apis;

import org.json.JSONArray;
import org.skt.runtime.api.RuntimeInterface;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkManager extends Plugin {
    
    public static int NOT_REACHABLE = 0;
    public static int REACHABLE_VIA_CARRIER_DATA_NETWORK = 1;
    public static int REACHABLE_VIA_WIFI_NETWORK = 2;

    public static final String WIFI = "wifi";
    public static final String WIMAX = "wimax";
    // mobile
    public static final String MOBILE = "mobile";
    // 2G network types
    public static final String GSM = "gsm";
    public static final String GPRS = "gprs";
    public static final String EDGE = "edge";
    // 3G network types
    public static final String CDMA = "cdma";
    public static final String UMTS = "umts";
    public static final String HSPA = "hspa";
    public static final String HSUPA = "hsupa";
    public static final String HSDPA = "hsdpa";
    public static final String ONEXRTT = "1xrtt";
    public static final String EHRPD = "ehrpd";
    // 4G network types
    public static final String LTE = "lte";
    public static final String UMB = "umb";
    public static final String HSPA_PLUS = "hspa+";
    // return type
    public static final String TYPE_UNKNOWN = "unknown";
    public static final String TYPE_ETHERNET = "ethernet";
    public static final String TYPE_WIFI = "wifi";
    public static final String TYPE_2G = "2g";
    public static final String TYPE_3G = "3g";
    public static final String TYPE_4G = "4g";
    public static final String TYPE_NONE = "none";
    
    private static final String LOG_TAG = "NetworkManager";

    private String connectionCallbackId;

    ConnectivityManager sockMan;
    BroadcastReceiver receiver;
    
    /**
     * Constructor.
     */
    public NetworkManager()    {
        this.receiver = null;
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     * 
     * @param ctx The context of the main Activity.
     */
    public void setContext(RuntimeInterface ctx) {
        super.setContext(ctx);
        this.sockMan = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.connectionCallbackId = null;
        
        // We need to listen to connectivity events to update navigator.connection
        IntentFilter intentFilter = new IntentFilter() ;
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        if (this.receiver == null) {
            this.receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    updateConnectionInfo((NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO));                
                }
            };
            ctx.registerReceiver(this.receiver, intentFilter);
        }

    }
    
    /**
     * Executes the request and returns PluginResult.
     * 
     * @param action         The action to execute.
     * @param args             JSONArry of arguments for the plugin.
     * @param callbackId    The callback id used when calling back into JavaScript.
     * @return                 A PluginResult object with a status and message.
     */
    public PluginResult execute(String action, JSONArray args, String callbackId) {
        PluginResult.Status status = PluginResult.Status.INVALID_ACTION;
        String result = "Unsupported Operation: " + action;
        
        if (action.equals("getConnectionInfo")) {
            this.connectionCallbackId = callbackId;
            NetworkInfo info = sockMan.getActiveNetworkInfo();
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, this.getConnectionInfo(info));
            pluginResult.setKeepCallback(true);
            return pluginResult;
        }
        
        return new PluginResult(status, result);
    }

    /**
     * Identifies if action to be executed returns a value and should be run synchronously.
     * 
     * @param action    The action to execute
     * @return            T=returns value
     */
    public boolean isSynch(String action) {
        return true;
    }
    
    /**
     * Stop network receiver.
     */
    public void onDestroy() {
        if (this.receiver != null) {
            try {
                this.ctx.unregisterReceiver(this.receiver);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error unregistering network receiver: " + e.getMessage(), e);
            }
        }
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------


    /**
     * Updates the JavaScript side whenever the connection changes
     * 
     * @param info the current active network info
     * @return
     */
    private void updateConnectionInfo(NetworkInfo info) {     
        // send update to javascript "navigator.network.connection"
        sendUpdate(this.getConnectionInfo(info));
    }

    /** 
     * Get the latest network connection information
     * 
     * @param info the current active network info
     * @return a JSONObject that represents the network info
     */
    private String getConnectionInfo(NetworkInfo info) {
        String type = TYPE_NONE;
        if (info != null) {
            // If we are not connected to any network set type to none
            if (!info.isConnected()) {
                type = TYPE_NONE;
            }
            else {
                type = getType(info);
            }
        }
        return type;
    }
    
    /**
     * Create a new plugin result and send it back to JavaScript
     * 
     * @param connection the network info to set as navigator.connection
     */
    private void sendUpdate(String type) {
        PluginResult result = new PluginResult(PluginResult.Status.OK, type);
        result.setKeepCallback(true);
        this.success(result, this.connectionCallbackId);
        
        // Send to all plugins
        this.ctx.postMessage("networkconnection", type);
    }
    
    /**
     * Determine the type of connection
     * 
     * @param info the network info so we can determine connection type.
     * @return the type of mobile network we are on
     */
    private String getType(NetworkInfo info) {
        if (info != null) {
            String type = info.getTypeName();

            if (type.toLowerCase().equals(WIFI)) {
                return TYPE_WIFI;
            }
            else if (type.toLowerCase().equals(MOBILE)) {
                type = info.getSubtypeName();
                if (type.toLowerCase().equals(GSM) || 
                        type.toLowerCase().equals(GPRS) ||
                        type.toLowerCase().equals(EDGE)) {
                    return TYPE_2G;
                }
                else if (type.toLowerCase().startsWith(CDMA) || 
                        type.toLowerCase().equals(UMTS)  ||
                        type.toLowerCase().equals(ONEXRTT) ||
                        type.toLowerCase().equals(EHRPD) ||
                        type.toLowerCase().equals(HSUPA) ||
                        type.toLowerCase().equals(HSDPA) ||
                        type.toLowerCase().equals(HSPA)) {
                    return TYPE_3G;
                }
                else if (type.toLowerCase().equals(LTE) || 
                        type.toLowerCase().equals(UMB) ||
                        type.toLowerCase().equals(HSPA_PLUS)) {
                    return TYPE_4G;
                }
            }
        } 
        else {
            return TYPE_NONE;
        }
        return TYPE_UNKNOWN;
    }
}
