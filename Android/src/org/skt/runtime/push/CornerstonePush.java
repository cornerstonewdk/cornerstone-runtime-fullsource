package org.skt.runtime.push;

import org.json.JSONArray;
import org.skt.runtime.RuntimeActivity;
import org.skt.runtime.additionalapis.Preferences;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;
import org.skt.runtime.api.RuntimeInterface;

import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

public class CornerstonePush extends Plugin{

	private static final String LOG_TAG = "CornerstonePush";
	private String CallbackId;

	public static String PROJECT_ID;

	/**
	 * Sets the context of the Command. This can then be used to do things like
	 * get file paths associated with the Activity.
	 * 
	 * @param ctx The context of the main Activity.
	 */
	public void setContext(RuntimeInterface ctx) {
		super.setContext(ctx);
	}

	/**
	 * Executes the request and returns PluginResult.
	 * 
	 * @param action        The action to execute.
	 * @param args          JSONArry of arguments for the plugin.
	 * @param callbackId    The callback id used when calling back into JavaScript.
	 * @return              A PluginResult object with a status and message.
	 */
	public PluginResult execute(String action, JSONArray args, String callbackId) {

		PluginResult.Status status = PluginResult.Status.NO_RESULT;
		String message = "";
		PluginResult result = new PluginResult(status, message);

		if (action.equals("requestRemotePermission")) {
			Log.e(LOG_TAG, "requestRemotePermission::projectID = " + args.optString(0));	

			String projectID = args.optString(0);

			if(!projectID.equals("null")){
				PROJECT_ID = projectID;
				registerProjectIDtoGCM(projectID);
			}	
		}
		else if(action.equals("unrequestRemotePermission")){

			//GCMRegistrar.unregister(ctx.getContext());
			Intent unregIntent = new Intent("com.goole.android.c2dm.intent.UNREGISTER");
			unregIntent.putExtra("app", PendingIntent.getBroadcast(ctx.getContext(), 0, new Intent(), 0));
			ctx.getContext().startService(unregIntent);
		}
		else if(action.equals("getregistrationID")){
			Preferences pref = new Preferences();
			pref.setContext(ctx);
			
			String registrationID = pref.getItem("registrationID");
			if(!registrationID.equals("undefined"))
				return new PluginResult(PluginResult.Status.OK,registrationID);
			else 
				return new PluginResult(PluginResult.Status.ERROR , createErrorObject(-1 , "registrationID is null"));	
		}
		else if(action.equals("usePushService")){
			Preferences pref = new Preferences();
			pref.setContext(ctx);
			
			boolean usePush = args.optBoolean(0);
			if(usePush){
				pref.setItem("usePush", "true");
			}
			else{
				pref.setItem("usePush", "false");
			}		
			
			return new PluginResult(PluginResult.Status.OK);
		}
		else if(action.equals("setPushType")){
			Preferences pref = new Preferences();
			pref.setContext(ctx);
			
			String pushType = args.optString(0);
			if(pushType.equals("TYPE_ALERT")){
				pref.setItem("pushType", "alert");
			}
			else if(pushType.equals("TYPE_BANNER")){
				pref.setItem("pushType", "banner");
			}		
			
			return new PluginResult(PluginResult.Status.OK);
		}
		
		else if(action.equals("getCurrentURL")){
			return new PluginResult(PluginResult.Status.OK,RuntimeActivity.currenturl);
		}
		

		return result;
	}

	public void registerProjectIDtoGCM(String projectID){

		GCMRegistrar.checkDevice(ctx.getContext());
		GCMRegistrar.checkManifest(ctx.getContext());

		final String regId = GCMRegistrar.getRegistrationId(ctx.getContext());

		if ("".equals(regId)) {
			//GCMRegistrar.register(ctx.getContext(), projectID);

			//App 을 GCM Server 에 등록하기
			Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");

			// sets the app name in the intent
			registrationIntent.putExtra("app", PendingIntent.getBroadcast(ctx.getContext(), 0, new Intent(), 0));
			registrationIntent.putExtra("sender", projectID);

			ctx.getContext().startService(registrationIntent);

		} else {
			Log.v(LOG_TAG, "Already registered::" + regId);
		}


	}

	@Override
	public boolean isSynch(String action) {
		// TODO Auto-generated method stub
		if(action.equals("requestRemotePermission"))
			return false;
		else if(action.equals("getregistrationID"))
			return false;
		else if(action.equals("getCurrentURL"))
			return true;
		
		return super.isSynch(action);
	}


}
