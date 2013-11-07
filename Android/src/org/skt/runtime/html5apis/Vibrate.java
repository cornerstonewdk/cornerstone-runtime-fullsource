package org.skt.runtime.html5apis;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;

/**
 * This class provides access to notifications on the device.
 */
public class Vibrate extends Plugin {
   
  /**
   * Constructor.
   */
  public Vibrate() {
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
    
    try {
      if (action.equals("vibrate")) {
        this.vibrate(args.getLong(0));
      }
      else if(action.equals("vibratepattern")){
    	  this.vibratepattern(args.getString(0));  
      }
      return new PluginResult(status, result);
    } catch (JSONException e) {
      return new PluginResult(PluginResult.Status.JSON_EXCEPTION);
    }
  }
  
  /**
   * Vibrates the device for the specified amount of time.
   * 
   * @param time      Time to vibrate in ms.
   */
  public void vibrate(long time){
        // Start the vibration, 0 defaults to half a second.
    if (time == 0) {
      time = 500;
    }
    	Log.e("chisu","vibrate");
        Vibrator vibrator = (Vibrator) this.ctx.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(time);
  }
  
  /**
   * Vibrates the device for the specified amount of time.
   * 
   * @param pattern      pattern to vibrate.
   */
  public void vibratepattern(String pattern){
	  
	  Vibrator vibrator = (Vibrator) this.ctx.getSystemService(Context.VIBRATOR_SERVICE);
	  vibrator.vibrate(makepattern(pattern), -1);
  }
  
  private long[] makepattern(String pattern){
	  //split String pattern
	  String[] patternarray = pattern.replace("[", "").replace("]", "").split(",");
	  ArrayList<Long> patternList = new ArrayList<Long>();
	  patternList.add((long)0);
	  for(int i = 0 ; i < patternarray.length ; i ++ ){
		  patternList.add(Long.valueOf(patternarray[i]));
	  }
	  long[] patternArr = new long[patternList.size()];
	  for(int i = 0 ; i < patternArr.length ; i ++){
		  patternArr[i] = patternList.get(i);
	  }

	  return patternArr;
  }
}
