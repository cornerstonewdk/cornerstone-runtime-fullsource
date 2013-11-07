package org.skt.runtime.additionalapis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.skt.runtime.RuntimeActivity;
import org.skt.runtime.UnzipUtils;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;
import org.skt.runtime.api.RuntimeInterface;
import org.skt.runtime.html5apis.DeviceAPIErrors;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class DeviceInteraction extends Plugin{

	private static final String LOG_TAG = "DeviceInteraction";

	private String wallPapaerCallbackId;
	private String softpackagingId;

	private MediaPlayer mAudio = null;
	private boolean isPlay = false;
	private int StreamType = 0 ; 
	
	private WallpaperManager wallpapermgr;
	private WallpaperReceiver recv = null;
	private boolean isBeepStop = false;

	/**
	 * Sets the context of the Command. This can then be used to do things like
	 * get file paths associated with the Activity.
	 * 
	 * @param ctx The context of the main Activity.
	 */
	public void setContext(RuntimeInterface ctx) {
		super.setContext(ctx);
		wallpapermgr = WallpaperManager.getInstance(ctx.getContext());	
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

		if (action.equals("startBeep")) {
			long beepTime = args.optLong(0);
			if(beepTime > 0)
				this.startBeep(beepTime);
		}
		else if(action.equals("stopBeep")){
			this.stopBeep();
		}
		else if(action.equals("setCallRingtone")){
			String path = args.optString(0);
			String name = args.optString(1);
			if(path != null){
				boolean returnval = this.setCallRingtone(path,name);
				if(returnval){
					return new PluginResult(PluginResult.Status.OK);
				}
				else{
					return new PluginResult(PluginResult.Status.ERROR,createErrorObject(DeviceAPIErrors.NOT_AVAILABLE_ERR,"can not change the ringtone"));
				}
			}
			else 
				return new PluginResult(PluginResult.Status.ERROR,createErrorObject(DeviceAPIErrors.INVALID_VALUES_ERR,"path is null"));

		}
		else if(action.equals("setWallpaper")){
			String path = args.optString(0);

			if(path != null){
				wallPapaerCallbackId = callbackId;
				this.setWallPapaer(path);

				// Don't return any result now, since status results will be sent when events come in from broadcast receiver 
				PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
				pluginResult.setKeepCallback(true);
				return pluginResult;

			}
			else 
				return new PluginResult(PluginResult.Status.ERROR, createErrorObject(DeviceAPIErrors.NOT_AVAILABLE_ERR,"error is happen"));
		}
		else if (action.equals("exitApp")) {
        	this.exitApp();
        }
		else if(action.equals("playRingtone")){
			boolean returnval = this.playRingtone();
			if(returnval){
				return new PluginResult(PluginResult.Status.OK);
			}
			else{
				return new PluginResult(PluginResult.Status.ERROR,createErrorObject(DeviceAPIErrors.NOT_AVAILABLE_ERR,"can not play the ringtone"));
			}
		}
		else if(action.equals("stopRingtone")){
			this.stopRingtone();
		}
		else if(action.equals("hydrationupdate")){
			softpackagingId = callbackId;
			PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
			pluginResult.setKeepCallback(true);
			
			boolean updateresult = this.hydrationupdate();
			if(updateresult){
				try {
					UnzipUtils.unzip(new File("/data/data/" + ctx.getPackageName() + "/hydapp/www.zip"), new File("/data/data/" + ctx.getPackageName() + "/hydapp"), false);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//JS?®Ïóê???ÖÎç∞?¥Ìä∏Î•?Ï≤òÎ¶¨?úÎã§.
				//updateSoftPackagingVer();
				
				ctx.loadUrl("file:///data/data/" + ctx.getPackageName() + "/hydapp/index.html");
			}
			else{
				
			}
			
			return pluginResult;
		}
		else if(action.equals("getPackageName")){
			String packageName = ctx.getPackageName();
			result = new PluginResult(PluginResult.Status.OK, packageName);
			return result;
		}
		else if(action.equals("clearHistory")){
			this.clearHistory();
			return result;
		}
		else {
			// Unsupported action
			return new PluginResult(PluginResult.Status.INVALID_ACTION);
		}

		return result;
	}

	 public void clearHistory() {
	    	((RuntimeActivity)this.ctx).clearHistoryAll();
	    }
	 
	public void updateSoftPackagingVer(){
		Preferences pref = new Preferences();
		pref.setItemForStaic((Context)ctx, "softpackagingVer","1.1");
	}
	public boolean hydrationupdate(){
		Log.e(LOG_TAG, "hydration update");
		
		Preferences pref = new Preferences();
		String hydrationVer = pref.getItemForStatic((Context)ctx, "softpackagingVer");
		String hydrationURL = pref.getItemForStatic((Context)ctx, "softpackagingURL");
		
		 URL zipURL;  
		 int Read;  
		 try {  
			 zipURL = new URL(hydrationURL);  
			 HttpURLConnection conn = (HttpURLConnection) zipURL.openConnection();  
			 int len = conn.getContentLength();  
			 byte[] tmpByte = new byte[len];  
			 InputStream is = conn.getInputStream();  
			 File hydappdir = new File("/data/data/" + ctx.getPackageName() + "/hydapp");
			 hydappdir.mkdir();
			 File file = new File("/data/data/" + ctx.getPackageName() + "/hydapp/www.zip");  
			 FileOutputStream fos = new FileOutputStream(file);
			 long total = 0 ; 
			 for (;;) {  
				 Read = is.read(tmpByte);
				 total += Read;
				 
				 //Log.e(LOG_TAG, String.valueOf(total*100/len));
				 PluginResult result = new PluginResult(PluginResult.Status.OK, total*100/len);
				 result.setKeepCallback(true);
				 this.success(result, softpackagingId);
					
				 if (Read <= 0) {
					 result.setKeepCallback(false);
					 result = new PluginResult(PluginResult.Status.OK, 100);
					 this.success(result, softpackagingId);
					 break;  
				 }  
				 fos.write(tmpByte, 0, Read);  
			 }  
			 is.close();  
			 fos.close();  
			 conn.disconnect();  

		 } catch (MalformedURLException e) {  
			 Log.e(LOG_TAG, e.getMessage());  
			 return false;
		 } catch (IOException e) {  
			 Log.e(LOG_TAG, e.getMessage());
			 return false;
		 } 
		 
		 Log.e(LOG_TAG, "hydration update OK");
		 return true;
	}
	
	public void setWallPapaer(String path){	
		path = "mnt/" + path.replace("file://", ""); 
		File file = new File(path);

		if(!file.exists()){
			returnToJsFail("There is no File in the path");		
		}
		
		if(recv == null){
			recv = new WallpaperReceiver();
			ctx.registerReceiver(recv, new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED));			
		}

		try {
			String fullFileName = "file://"+ path;
			wallpapermgr.setStream(ctx.getContentResolver().openInputStream(Uri.parse(fullFileName)));
		}
		catch (FileNotFoundException e) {				
			e.printStackTrace();
			Log.e(LOG_TAG, "Error : " + e.getMessage());
			returnToJsFail(e.getMessage());

		} catch (IOException e) {					
			e.printStackTrace();
			Log.e(LOG_TAG, "Error : " + e.getMessage());
			returnToJsFail(e.getMessage());
		}
		catch (Throwable e) {
			Log.e(LOG_TAG, "Error : " + e.getMessage());
			returnToJsFail(e.getMessage());
		}
	}

	public void returnToJsFail(String error){
		if (wallPapaerCallbackId != null) {
			PluginResult result = new PluginResult(PluginResult.Status.ERROR, createErrorObject(DeviceAPIErrors.NOT_FOUND_ERR,error));
			result.setKeepCallback(false);
			this.error(result, wallPapaerCallbackId);
		}

		wallPapaerCallbackId = null;
	}
	
	public void returnToJSSucess(){
		if (wallPapaerCallbackId != null) {
			PluginResult result = new PluginResult(PluginResult.Status.OK, "Wallpaper is changed");
			result.setKeepCallback(false);
			this.success(result, wallPapaerCallbackId);
		}

		wallPapaerCallbackId = null;
	}
	
	
	public boolean setCallRingtone(String path, String name){
		path = "mnt/" + path.replace("file://", ""); 
		File file = new File(path);
		
		if(name.equals("null")){
			name = "RingToneFromSRT";
		}
		
		if(!file.exists()){
			return false;		
		}
		else {
			try {
				ContentValues values = new ContentValues();
				values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
				values.put(MediaStore.MediaColumns.TITLE, name);
				values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/*");
				values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
				values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
				values.put(MediaStore.Audio.Media.IS_ALARM, false);
				values.put(MediaStore.Audio.Media.IS_MUSIC, false);

				Uri uri = MediaStore.Audio.Media.getContentUriForPath(file.getAbsolutePath()); 
				int deleted = ctx.getContentResolver().delete(uri, MediaStore.MediaColumns.DATA + "=\"" + file.getAbsolutePath() + "\"", null);
				Uri newUri = ctx.getApplicationContext().getContentResolver().insert(uri, values);
				RingtoneManager.setActualDefaultRingtoneUri(ctx.getApplicationContext(), RingtoneManager.TYPE_RINGTONE, newUri); 
			} catch (Exception e) {
				e.printStackTrace();
				//Toast.makeText(ctx.getApplicationContext(), "Fail to set Ringtone", Toast.LENGTH_SHORT).show();
				return false;
			}
			//Toast.makeText(ctx.getApplicationContext(), "Success to set Ringtone.", Toast.LENGTH_SHORT).show();
			return true;
		}
	}

	public void stopBeep(){
		isBeepStop = true;
		
		Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		Ringtone notification = RingtoneManager.getRingtone(this.ctx.getContext(), ringtone);

		if(notification.isPlaying())
			notification.stop();
	}

	public void startBeep(long count) {
		
		isBeepStop = false;
		Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		Ringtone notification = RingtoneManager.getRingtone(this.ctx.getContext(), ringtone);

		// If phone is not set to silent mode
		if (notification != null) {
			for (long i = 0; i < count; ++i) {
				if(isBeepStop == false){
					notification.play();
				}
				long timeout = 5000;
				while (notification.isPlaying() && (timeout > 0)) {
					timeout = timeout - 100;
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
				}
			}
		}
	}

	/**
     * Exit the Android application.
     */
    public void exitApp() {
    	((RuntimeActivity)this.ctx).endActivity();
    }
    
    public boolean playRingtone(){
    	
    	if(isPlay == false){
    		try{
        		mAudio = new MediaPlayer();
        		Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        		mAudio.setDataSource(alert.toString());
        		mAudio.setAudioStreamType(AudioManager.STREAM_RING);
        		mAudio.setLooping(true);
        		mAudio.prepare();
        	}
        	catch(Exception e){
        		Log.e(LOG_TAG, e.toString());
        		return false;
        	}
        	mAudio.start();
        	
        	isPlay = true;
        	
        	return true;
    	}
    	else{
    		return false;
    	}
    }
    
    public void stopRingtone(){
    	if(mAudio != null && mAudio.isPlaying()){
    		mAudio.stop();
    		isPlay = false;
    	}
    }
    
    public void pauseRingtone(){
    	if(mAudio.isPlaying()){
    		mAudio.pause();
    	}
    }
    
    public void resumeRingtone(){
    	if(!mAudio.isPlaying()){
    		mAudio.start();
    	}
    }
    
	@Override
	public void onPause(boolean multitasking) {
		if(mAudio != null)
			this.pauseRingtone();
		// TODO Auto-generated method stub
		super.onPause(multitasking);
	}

	@Override
	public void onResume(boolean multitasking) {
		if(mAudio != null)
			this.resumeRingtone();
		// TODO Auto-generated method stub
		super.onResume(multitasking);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		if(mAudio != null)
			this.stopRingtone();
		
		if(this.recv != null){
			ctx.unregisterReceiver(this.recv);
			this.recv = null;
		}
		
		super.onDestroy();
	}


	public class WallpaperReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {		
			if(intent.getAction().equals(Intent.ACTION_WALLPAPER_CHANGED)){
				Log.e(LOG_TAG, "wallpaper change is success!!");
				try{
					ctx.unregisterReceiver(recv);	
					recv = null;	
					returnToJSSucess();	
				}
				catch(Exception e){
					Log.e(LOG_TAG,"exception happened while unregister wallpaper change receiver! ");
				}
			}
		}
	};
}
