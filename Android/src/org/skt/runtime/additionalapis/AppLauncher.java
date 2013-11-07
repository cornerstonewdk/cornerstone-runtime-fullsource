package org.skt.runtime.additionalapis;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;
import org.skt.runtime.api.RuntimeInterface;
import org.skt.runtime.html5apis.DeviceAPIErrors;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

public class AppLauncher extends Plugin{

	private static final String LOG_TAG = "AppLauncher";

	private String wallPapaerCallbackId;

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

		if (action.equals("getInstalledApplications")) {
			JSONArray installedApp = getInstalledApplication();
			return new PluginResult(PluginResult.Status.OK,installedApp);
		}
		else if(action.equals("launchApplication")){
			String appURI = args.optString(0);
		
			if(appURI.equals("null"))
				return new PluginResult(PluginResult.Status.ERROR , createErrorObject(DeviceAPIErrors.INVALID_VALUES_ERR, "app uri is null"));
			else
				startApplication(args);
			
			return new PluginResult(PluginResult.Status.OK);
			
		}
		return result;
	}

	public JSONArray getInstalledApplication(){
		
		List<PackageInfo> appinfo;appinfo = ctx.getApplicationContext().getPackageManager().getInstalledPackages(PackageManager.GET_ACTIVITIES);
		
		JSONArray returnarray = new JSONArray();

		for(int i = 0 ; i <appinfo.size() ; i ++){
			PackageInfo pi = appinfo.get(i);
			ActivityInfo[] af = pi.activities;

			Intent intent = ctx.getApplicationContext().getPackageManager().getLaunchIntentForPackage(pi.packageName);
			if(intent != null){
				returnarray.put(pi.packageName);
			}
		}
	
		return returnarray;
	}

	//public void startApplication(String appURI){
	public void startApplication(JSONArray args){
		
		String appURI = args.optString(0);
		
		//[20120914][chisu]if args has some argument to use in application 
		JSONArray appArg = args.optJSONArray(1);
		
		//[220120924][chisu]if has android action
		Intent sendIntent = null;
		
		
		if(args.optJSONObject(2) != null){
			//[20120924][chisu]set user action
			JSONObject actionObj = args.optJSONObject(2);
			
			sendIntent = new Intent(actionObj.optString("action"));
			
			if(!actionObj.optString("type").equals(""))
				sendIntent.setType(actionObj.optString("type"));
			
			if(!actionObj.optString("uri").equals(""))
				sendIntent.setData(Uri.parse(actionObj.optString("uri")));
			
			if(!actionObj.optString("category").equals(""))
				sendIntent.addCategory(actionObj.optString("category"));	
		}
		else{
			//use android.action.MAIN
			sendIntent = ctx.getContext().getPackageManager().getLaunchIntentForPackage(appURI);
		}
		
		
		if(appArg != null){
			for(int i = 0  ; i < appArg.length() ; i ++){
				putSomeExtra(sendIntent, appArg.optJSONObject(i));
			}
		}
		
		ctx.startActivity(sendIntent);
	}
	
	//[20120914][chisu]check arg object type
	void putSomeExtra(Intent intent , JSONObject arg){
		String type = arg.optString("type");
		
		if(type != null){
			if(type.equalsIgnoreCase("string"))
				intent.putExtra(arg.optString("name"), arg.optString("value"));	
			else if(type.equalsIgnoreCase("number"))
				intent.putExtra(arg.optString("name"), arg.optInt("value"));
			else if(type.equalsIgnoreCase("boolean"))
				intent.putExtra(arg.optString("name"), arg.optBoolean("value"));
			else if(type.equalsIgnoreCase("numberArray")){
				JSONArray intarray = arg.optJSONArray("value");
				int[] putarray = new int[intarray.length()];
				for(int i = 0 ; i < intarray.length() ; i ++){
					putarray[i] = intarray.optInt(i);
				}	
				intent.putExtra(arg.optString("name"), putarray);
			}
			else if(type.equalsIgnoreCase("stringArray")){
				JSONArray stringarray = arg.optJSONArray("value");
				String[] putarray = new String[stringarray.length()];
				for(int i = 0 ; i < stringarray.length() ; i ++){
					putarray[i] = stringarray.optString(i);
				}	
				intent.putExtra(arg.optString("name"), putarray);
			}
			else{
				//default is string
				intent.putExtra(arg.optString("name"), arg.optString("value"));	
			}
		}
		else{
			//default is string
			intent.putExtra(arg.optString("name"), arg.optString("value"));	
		}
	}
	@Override
	public boolean isSynch(String action) {
		// TODO Auto-generated method stub	
		return super.isSynch(action);
	}

}
