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
package org.skt.runtime;
import org.json.JSONArray;
import org.json.JSONException;
import org.skt.runtime.api.LOG;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.EditText;

/**
 * This class is the WebChromeClient that implements callbacks for our web view.
 */
public class RuntimeChromeClient extends WebChromeClient {
    

    private String TAG = "SKTLog";
    private long MAX_QUOTA = 100 * 1024 * 1024;
    private RuntimeActivity ctx;
    
    //[20120614][chisu]use for media capture
    public static final int FILE_RESULTCODE = 1001;
    public static final int IMAGE_RESULTCODE = 1002;
    public static final int VIDEO_RESULTCODE = 1003;
    public static final int AUDIO_RESULTCODE = 10044;
    public static ValueCallback<Uri> uploadMessage = null;
    
    /**
     * Constructor.
     * 
     * @param ctx
     */
    public RuntimeChromeClient(Context ctx) {
        this.ctx = (RuntimeActivity) ctx;
    }

    /**
     * Tell the client to display a javascript alert dialog.
     * 
     * @param view
     * @param url
     * @param message
     * @param result
     */
    @Override
    public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
        AlertDialog.Builder dlg = new AlertDialog.Builder(this.ctx);
        dlg.setMessage(message);
        dlg.setTitle("Alert");
        //Don't let alerts break the back button
        dlg.setCancelable(true);
        dlg.setPositiveButton(android.R.string.ok,
            new AlertDialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            });
        dlg.setOnCancelListener(
           new DialogInterface.OnCancelListener() {
               public void onCancel(DialogInterface dialog) {
                   result.confirm();
                   }
               });
        dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
            //DO NOTHING
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_BACK)
                {
                    result.confirm();
                    return false;
                }
                else
                    return true;
                }
            });
        dlg.create();
        dlg.show();
        return true;
    }       

    /**
     * Tell the client to display a confirm dialog to the user.
     * 
     * @param view
     * @param url
     * @param message
     * @param result
     */
    @Override
    public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
        AlertDialog.Builder dlg = new AlertDialog.Builder(this.ctx);
        dlg.setMessage(message);
        dlg.setTitle("Confirm");
        dlg.setCancelable(true);
        dlg.setPositiveButton(android.R.string.ok, 
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            });
        dlg.setNegativeButton(android.R.string.cancel, 
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.cancel();
                }
            });
        dlg.setOnCancelListener(
            new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    result.cancel();
                    }
                });
        dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
            //DO NOTHING
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_BACK)
                {
                    result.cancel();
                    return false;
                }
                else
                    return true;
                }
            });
        dlg.create();
        dlg.show();
        return true;
    }

    /**
     * Tell the client to display a prompt dialog to the user. 
     * If the client returns true, WebView will assume that the client will 
     * handle the prompt dialog and call the appropriate JsPromptResult method.
     * 
     * Since we are hacking prompts for our own purposes, we should not be using them for 
     * this purpose, perhaps we should hack console.log to do this instead!
     * 
     * @param view
     * @param url
     * @param message
     * @param defaultValue
     * @param result
     */
    @Override
    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
        
        // Security check to make sure any requests are coming from the page initially
        // loaded in webview and not another loaded in an iframe.
        boolean reqOk = false;
        if (url.startsWith("file://") || url.indexOf(this.ctx.baseUrl) == 0 || ctx.isUrlWhiteListed(url)) {
            reqOk = true;
        }
        
        // Calling PluginManager.exec() to call a native service using 
        // prompt(this.stringify(args), "gap:"+this.stringify([service, action, callbackId, true]));
        if (reqOk && defaultValue != null && defaultValue.length() > 3 && defaultValue.substring(0, 4).equals("gap:")) {
            JSONArray array;
            try {
                array = new JSONArray(defaultValue.substring(4));
                String service = array.getString(0);
                String action = array.getString(1);
                String callbackId = array.getString(2);
                boolean async = array.getBoolean(3);
                String r = ctx.pluginManager.exec(service, action, callbackId, message, async);
                result.confirm(r);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        // Polling for JavaScript messages 
        else if (reqOk && defaultValue != null && defaultValue.equals("gap_poll:")) {
            String r = ctx.callbackServer.getJavascript();
            result.confirm(r);
        }
        
        // Calling into CallbackServer
        else if (reqOk && defaultValue != null && defaultValue.equals("gap_callbackServer:")) {
            String r = "";
            if (message.equals("usePolling")) {
                r = ""+ ctx.callbackServer.usePolling();
            }
            else if (message.equals("restartServer")) {
                ctx.callbackServer.restartServer();
            }
            else if (message.equals("getPort")) {
                r = Integer.toString(ctx.callbackServer.getPort());
            }
            else if (message.equals("getToken")) {
                r = ctx.callbackServer.getToken();
            }
            result.confirm(r);
        }
        
        // SKTRuntime JS has initialized, so show webview
        // (This solves white flash seen when rendering HTML)
        else if (reqOk && defaultValue != null && defaultValue.equals("gap_init:")) {
            if (ctx.splashscreen != 0) {
                //ctx.root.setBackgroundResource(0);
            }
            ctx.appView.setVisibility(View.VISIBLE);
            ctx.spinnerStop();
            result.confirm("OK");
        }

        // Show dialog
        else {
            final JsPromptResult res = result;
            AlertDialog.Builder dlg = new AlertDialog.Builder(this.ctx);
            dlg.setMessage(message);
            final EditText input = new EditText(this.ctx);
            if (defaultValue != null) {
                input.setText(defaultValue);
            }
            dlg.setView(input);
            dlg.setCancelable(false);
            dlg.setPositiveButton(android.R.string.ok, 
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    String usertext = input.getText().toString();
                    res.confirm(usertext);
                }
            });
            dlg.setNegativeButton(android.R.string.cancel, 
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    res.cancel();
                }
            });
            dlg.create();
            dlg.show();
        }
        return true;
    }
    
    /**
     * Handle database quota exceeded notification.
     *
     * @param url
     * @param databaseIdentifier
     * @param currentQuota
     * @param estimatedSize
     * @param totalUsedQuota
     * @param quotaUpdater
     */
    @Override
    public void onExceededDatabaseQuota(String url, String databaseIdentifier, long currentQuota, long estimatedSize,
            long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater)
    {
        LOG.d(TAG, "DroidGap:  onExceededDatabaseQuota estimatedSize: %d  currentQuota: %d  totalUsedQuota: %d", estimatedSize, currentQuota, totalUsedQuota);

        if( estimatedSize < MAX_QUOTA)
        {
            //increase for 1Mb
            long newQuota = estimatedSize;
            LOG.d(TAG, "calling quotaUpdater.updateQuota newQuota: %d", newQuota);
            quotaUpdater.updateQuota(newQuota);
        }
        else
        {
            // Set the quota to whatever it is and force an error
            // TODO: get docs on how to handle this properly
            quotaUpdater.updateQuota(currentQuota);
        }
    }

    // console.log in api level 7: http://developer.android.com/guide/developing/debug-tasks.html
    @Override
    public void onConsoleMessage(String message, int lineNumber, String sourceID)
    {       
        LOG.d(TAG, "%s: Line %d : %s", sourceID, lineNumber, message);
        super.onConsoleMessage(message, lineNumber, sourceID);
    }
    
    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage)
    {       
        if(consoleMessage.message() != null)
            LOG.d(TAG, consoleMessage.message());
        return super.onConsoleMessage(consoleMessage);
    }

    @Override
    /**
     * Instructs the client to show a prompt to ask the user to set the Geolocation permission state for the specified origin. 
     * 
     * @param origin
     * @param callback
     */
    public void onGeolocationPermissionsShowPrompt(String origin, Callback callback) {
        super.onGeolocationPermissionsShowPrompt(origin, callback);
        callback.invoke(origin, true, false);
    }

    
    //[20121102][chisu] For Android 4.1+ mediacapture
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        openFileChooser( uploadMsg, acceptType);
    }
    
    //[20120614][chisu] For Android 4.0.3 HTML5 Media capture support 
    public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType) {
    	uploadMessage = uploadFile;;
    	Intent intent = null;
    	
    	//[20130723][chisu]<input type="file"></input>
    	if(acceptType.equals("")){
    		//[20120614][chisu]call gallery 
    		intent = new Intent(Intent.ACTION_GET_CONTENT);
    		intent.addCategory(Intent.CATEGORY_OPENABLE);
    		intent.setType("*/*");
    		
    		
    		Intent chooser = createChooserIntent(new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE), 
    											 new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE),
    											 new Intent(android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION));
            chooser.putExtra(Intent.EXTRA_INTENT, intent);
            
            
    		ctx.startActivityForResult(chooser, FILE_RESULTCODE);
    	}
    	//[20130723][chisu]<input type="file" name="image" accept="image/*" capture>
    	else if (acceptType.equals("image/*")){  		
    		intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            ctx.startActivityForResult(intent, IMAGE_RESULTCODE);
    	}
    	//[20130723][chisu]<input type="file" name="video" accept="video/*" capture>
    	else if(acceptType.equals("video/*")){
    		intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
    		ctx.startActivityForResult(intent, VIDEO_RESULTCODE);
    	}
    	//[20130723][chisu]<input type="file" name="audio" accept="audio/*" capture>
    	else if(acceptType.equals("audio/*")){
    		intent = new Intent(android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION);
    		ctx.startActivityForResult(intent, AUDIO_RESULTCODE);
    	}
    	//[20131011][chisu]FIXME:<input type="file" name="image" accept="image/S4" capture>
    	else if(acceptType.equals("image/S4")){
    		//[20120614][chisu]call gallery 
    		intent = new Intent(Intent.ACTION_GET_CONTENT);
    		intent.addCategory(Intent.CATEGORY_OPENABLE);
    		intent.setType("*/*");
    		
    		
    		Intent chooser = createChooserIntent(new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE));
            chooser.putExtra(Intent.EXTRA_INTENT, intent);
            
            
    		ctx.startActivityForResult(chooser, FILE_RESULTCODE);
    	}
	}

    private Intent createChooserIntent(Intent... intents) {
        Intent chooser = new Intent(Intent.ACTION_CHOOSER);
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents);
        chooser.putExtra(Intent.EXTRA_TITLE,"Select Application");
        return chooser;
    }
    
    //[20120816][chisu]
	@Override
	public void onProgressChanged(WebView view, int newProgress) {
		
		//Log.e("chisu", "progress::" + newProgress);
		
		// TODO Auto-generated method stub
		if (ctx.preferences.prefMatches("loadingprogressbar","navigator")) {
			ctx.mProgressHorizontal.setProgress(newProgress);
		}
		else if(ctx.preferences.prefMatches("loadingprogressbar","dialog")) {
			ctx.mProgressdialog.setProgress(newProgress);
		}
		//super.onProgressChanged(view, newProgress);
	}
    
	
	//[20121102][chisu]video full screen 
	private Bitmap 		mDefaultVideoPoster;
	private View 		mVideoProgressView;
	
	@Override
	public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback)
	{
		
		ctx.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		
		//Log.i(LOGTAG, "here in on ShowCustomView");
        ctx.appView.setVisibility(View.GONE);
        
        // if a view already exists then immediately terminate the new one
        if (ctx.appView.mCustomView != null) {
            callback.onCustomViewHidden();
            return;
        }
        
        ctx.appView.mCustomViewContainer.addView(view);
        ctx.appView.mCustomView = view;
        ctx.appView.mCustomViewCallback = callback;
        ctx.appView.mCustomViewContainer.setVisibility(View.VISIBLE);
	}
	
	@Override
	public void onHideCustomView() {
		
		ctx.setDeviceOrientation();
		
		if (ctx.appView.mCustomView == null)
			return;	       
		
		// Hide the custom view.
		ctx.appView.mCustomView.setVisibility(View.GONE);
		
		// Remove the custom view from its container.
		ctx.appView.mCustomViewContainer.removeView(ctx.appView.mCustomView);
		ctx.appView.mCustomView = null;
		ctx.appView.mCustomViewContainer.setVisibility(View.GONE);
		ctx.appView.mCustomViewCallback.onCustomViewHidden();
		
		ctx.appView.setVisibility(View.VISIBLE);
		
        //Log.i(LOGTAG, "set it to webVew");
	}
	
	@Override
	public Bitmap getDefaultVideoPoster() {
		//Log.i(LOGTAG, "here in on getDefaultVideoPoster");	
		if (mDefaultVideoPoster == null) {
			mDefaultVideoPoster = BitmapFactory.decodeResource(
					ctx.getResources(), R.drawable.default_video_poster);
	    }
		return mDefaultVideoPoster;
	}
	
	@Override
	public View getVideoLoadingProgressView() {
		//Log.i(LOGTAG, "here in on getVideoLoadingPregressView");
		
        if (mVideoProgressView == null) {
            LayoutInflater inflater = LayoutInflater.from(ctx);
            mVideoProgressView = inflater.inflate(R.layout.video_loading_progress, null);
        }
        return mVideoProgressView; 
	}
	
	 @Override
     public void onReceivedTitle(WebView view, String title) {
        ((Activity) ctx).setTitle(title);
     }

}
