package org.skt.runtime.html5apis.calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;
import org.skt.runtime.html5apis.DeviceAPIErrors;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Calendars;
import android.util.Log;

public class CalendarManager extends Plugin{

	private static final String LOG_TAG = "CalendarManager";

	private CalendarInterface calendar;

	public CalendarManager(){

	}

	@Override
	/**
	 * Executes the request and returns PluginResult.
	 * 
	 * @param action    The action to execute.
	 * @param args      JSONArry of arguments for the plugin.
	 * @param callbackId  The callback id used when calling back into JavaScript.
	 * @return        A PluginResult object with a status and message.
	 */
	public PluginResult execute(String action, JSONArray args, String callbackId) {

		calendar = getCalendar();

		PluginResult.Status status = PluginResult.Status.OK;
		String result = "";

		try{
			if (action.equals("addEvent")) {
				Log.e(LOG_TAG, "addEvent");
				JSONObject res = calendar.addEvent(args.getJSONObject(0));
				if(res != null)
					return new PluginResult(status,res);
				else 
					return new PluginResult(PluginResult.Status.ERROR, createErrorObject(DeviceAPIErrors.UNKNOWN_ERR,"unknown error"));
			}
			else if (action.equals("findEvents")) {
				Log.e(LOG_TAG, "FindEvents");
				
				JSONArray returnarray ;
				
				if(!args.isNull(0)){
					returnarray = calendar.findEvents(args.getJSONObject(0));
				}
				else{
					returnarray = calendar.findEvents(null);
				}
				if(returnarray != null)
					return new PluginResult(status,returnarray);
				else 
					return new PluginResult(PluginResult.Status.ERROR, createErrorObject(DeviceAPIErrors.UNKNOWN_ERR,"unknown error"));
				
			}
			else if (action.equals("deleteEvent")) {
				Log.e(LOG_TAG, "deleteEvent");
				
				int res = calendar.deleteEvent(args.getString(0));
				
				if(res != 1) // delete is faiil
					return new PluginResult(PluginResult.Status.ERROR, createErrorObject(DeviceAPIErrors.UNKNOWN_ERR,"unknown error"));
				
				return new PluginResult(status);
				
			}
		} catch (JSONException e) {
			Log.e(LOG_TAG, e.getMessage(), e);
			return new PluginResult(PluginResult.Status.JSON_EXCEPTION);
		}
		// If we get to this point an error has occurred
		return new PluginResult(PluginResult.Status.ERROR, createErrorObject(DeviceAPIErrors.UNKNOWN_ERR,"unknown error"));
	}

	//[20120625][chisu]access to Native Calendar
	private CalendarInterface getCalendar(){
		// Run query
		Cursor c = null;
		ContentResolver cr = ctx.getContentResolver();
		Uri uri = Calendars.CONTENT_URI;   
		c = cr.query(uri, IcsGoogleCalendar.EVENT_PROJECTION, null, null, null);
		if(c == null) {
			throw new RuntimeException("Device has no calendar provider.");
		}


		CalendarInterface[] calendars = new IcsGoogleCalendar[c.getCount()];
		int i = 0;
		while(c.moveToNext()){
			calendars[i++] = new IcsGoogleCalendar(ctx.getContext(), c.getInt(0), c.getString(1));
		}
		c.close();

		return calendars[0];
	}

}
