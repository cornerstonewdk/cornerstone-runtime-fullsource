package org.skt.runtime.additionalapis;

import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.skt.runtime.api.LOG;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;
import org.skt.runtime.api.RuntimeInterface;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences extends Plugin{

	private static final String KEY_DEFAULT = "default";
	private static final String LOG_TAG = "Preferences";

	private String callbackId;
	private String prefFileName;

	/**
	 * Sets the context of the Command. This can then be used to do things like
	 * get file paths associated with the Activity.
	 * 
	 * @param ctx The context of the main Activity.
	 */
	public void setContext(RuntimeInterface ctx) {
		super.setContext(ctx);
		initPreference();
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

		if (action.equals("setItem")) {
			String key = args.optString(0);
			String value = args.optString(1);
			
			setItem(key, value);
			return new PluginResult(PluginResult.Status.OK,"Set Item Success");
		}
		else if(action.equals("getItem")){

			String key = args.optString(0);
			String value = getItem(key);
			
			return new PluginResult(PluginResult.Status.OK,value);
		}
		else if(action.equals("removeItem")){

			String key = args.optString(0);
			removeItem(key);
			
			return new PluginResult(PluginResult.Status.OK,"Remove Item success");
		}
		else if(action.equals("clear")){
			clear();
			return new PluginResult(PluginResult.Status.OK);
		}

		return result;
	}

	@Override
	public boolean isSynch(String action) {
		// TODO Auto-generated method stub
		return true;
	}

	public void initPreference(){	
		prefFileName = ctx.getPackageName();
		SharedPreferences prefs = ctx.getContext().getSharedPreferences(prefFileName, Context.MODE_PRIVATE);

		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(KEY_DEFAULT, KEY_DEFAULT); // for distinguish 
		editor.commit();
	}

	public void setItem(String key, String value){

		SharedPreferences prefs = ctx.getContext().getSharedPreferences(prefFileName, Context.MODE_PRIVATE);

		if(!prefs.contains(key)){
			//Toast.makeText(context, "There is no such preference key.", Toast.LENGTH_LONG).show();
			//Log.d(TAG, "There is no such preference key. Let's make it.");
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(key, value);
			editor.putString(is(key), "false");
			editor.commit();
			return;
		}

		String check = prefs.getString(is(key), "false");

		if(check.equals("true")){
			//Log.d(TAG, "Can't modify this preference value which has a read only property.");
			//Toast.makeText(context, "Can't modify this preference value which has a read only property.", Toast.LENGTH_LONG).show();
		}else{
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(key, value);
			editor.commit();
		}
	}
	
	public void setItemForStaic(Context context, String key, String value){

		prefFileName = context.getPackageName();
		SharedPreferences prefs = context.getSharedPreferences(prefFileName, Context.MODE_PRIVATE);

		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(KEY_DEFAULT, KEY_DEFAULT); // for distinguish 
		editor.commit();
		

		if(!prefs.contains(key)){
			//Toast.makeText(context, "There is no such preference key.", Toast.LENGTH_LONG).show();
			//Log.d(TAG, "There is no such preference key. Let's make it.");
			editor = prefs.edit();
			editor.putString(key, value);
			editor.putString(is(key), "false");
			editor.commit();
			return;
		}

		String check = prefs.getString(is(key), "false");

		if(check.equals("true")){
			//Log.d(TAG, "Can't modify this preference value which has a read only property.");
			//Toast.makeText(context, "Can't modify this preference value which has a read only property.", Toast.LENGTH_LONG).show();
		}else{
			editor = prefs.edit();
			editor.putString(key, value);
			editor.commit();
		}
	}

	public String getItem(String key){

		SharedPreferences prefs = ctx.getContext().getSharedPreferences(prefFileName, Context.MODE_PRIVATE);

		if(!prefs.contains(key)){
			LOG.e(LOG_TAG, "getItem's result1 : "+prefs.getString(key, ""));
			return "undefined";
		}
		return prefs.getString(key, "undefined");
	}
	
	public String getItemForStatic(Context context,String key){

		prefFileName = context.getPackageName();
		SharedPreferences prefs = context.getSharedPreferences(prefFileName, Context.MODE_PRIVATE);
		
		if(!prefs.contains(key)){
			LOG.e(LOG_TAG, "getItem's result1 : "+prefs.getString(key, ""));
			return "undefined";
		}
		return prefs.getString(key, "undefined");
	}

	public void removeItem(String key){
		 SharedPreferences prefs = ctx.getContext().getSharedPreferences(prefFileName, Context.MODE_PRIVATE);

		 if(prefs.contains(key)){
			 String readonly = prefs.getString(is(key), "false");
			 if(readonly.equals("false")){
				 SharedPreferences.Editor editor = prefs.edit();
				 editor.remove(key);
				 editor.remove(is(key));
				 editor.commit();
			 }
		 }
	 }
	
	public void clear(){

		 SharedPreferences prefs = ctx.getContext().getSharedPreferences(prefFileName, Context.MODE_PRIVATE);
		 Map<String, ?>keys = prefs.getAll();
		 Iterator it = keys.entrySet().iterator();
		 while(it.hasNext()){
			 Map.Entry pairs = (Map.Entry)it.next();
			 String key = (String)pairs.getKey(); 

			 if(key.startsWith("is") && !key.equals("is") && !key.startsWith("isis")) //FIXME: if key name is "isisis", bug can be occurred.
				 continue;

			 // ex. key : hello
			 String isKey = is(key); // ishello
			 String readonly = prefs.getString(isKey, "true"); // (isKey1, false)
			 if(readonly != null && readonly.equals("false")){
//				 Log.d("Test", key+" is readonly. let's clear");
				 SharedPreferences.Editor editor = prefs.edit();
				 editor.remove(key);
				 editor.remove(isKey);
				 editor.commit();
			 }
		 }
	 }
	
	private String is(String key){
		return "is"+key+"ReadOnly";
	}
}
