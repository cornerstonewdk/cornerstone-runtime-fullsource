package org.skt.runtime.additionalapis;

import java.io.File;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;
import org.skt.runtime.html5apis.DeviceAPIErrors;
import org.skt.runtime.html5apis.file.FileUtils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;


public class Capture extends Plugin {

	private static final String VIDEO_3GPP = "video/3gpp";
	private static final String VIDEO_MP4  = "video/mp4";
	private static final String AUDIO_3GPP = "audio/3gpp";
	private static final String IMAGE_JPEG = "image/jpeg";

	private static final int CAPTURE_AUDIO = 0;     // Constant for capture audio
	private static final int CAPTURE_IMAGE = 1;     // Constant for capture image
	private static final int CAPTURE_VIDEO = 2;     // Constant for capture video
	private static final String LOG_TAG = "Capture";

	private static final int CAPTURE_NO_MEDIA_FILES = 3;

	private String callbackId;                      // The ID of the callback to be invoked with our result
	private Uri imageUri;                           // Uri of captured image
	private Uri videoUri;
	private Uri audioUri;

	@Override
	public PluginResult execute(String action, JSONArray args, String callbackId) {
		this.callbackId = callbackId;

		if (action.equals("getFormatData")) {
			try {
				JSONObject obj = getFormatData(args.getString(0), args.getString(1));
				return new PluginResult(PluginResult.Status.OK, obj);
			} catch (JSONException e) {
				return new PluginResult(PluginResult.Status.ERROR);
			}
		}
		else if (action.equals("captureAudio")) {
			this.captureAudio(args);
		}
		else if (action.equals("captureImage")) {
			this.captureImage(args);
		}
		else if (action.equals("captureVideo")) {
			this.captureVideo(args);    
		}

		PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
		r.setKeepCallback(true);
		return r;
	}

	/**
	 * Provides the media data file data depending on it's mime type
	 * 
	 * @param filePath path to the file
	 * @param mimeType of the file
	 * @return a MediaFileData object
	 */
	private JSONObject getFormatData(String filePath, String mimeType) {
		JSONObject obj = new JSONObject();
		try {
			// setup defaults
			obj.put("height", 0);
			obj.put("width", 0);
			obj.put("bitrate", 0);
			obj.put("duration", 0);
			obj.put("codecs", "");

			// If the mimeType isn't set the rest will fail
			// so let's see if we can determine it.
			if (mimeType == null || mimeType.equals("")) {
				mimeType = FileUtils.getMimeType(filePath);
			}
			Log.d(LOG_TAG, "Mime type = " + mimeType);

			if (mimeType.equals(IMAGE_JPEG) || filePath.endsWith(".jpg")) {
				obj = getImageData(filePath, obj);
			}
			else if (mimeType.endsWith(AUDIO_3GPP)) {
				obj = getAudioVideoData(filePath, obj, false);
			}
			else if (mimeType.equals(VIDEO_3GPP) || mimeType.equals(VIDEO_MP4)) {
				obj = getAudioVideoData(filePath, obj, true);
			}
		}
		catch (JSONException e) {
			Log.d(LOG_TAG, "Error: setting media file data object");
		}
		return obj;
	}

	/**
	 * Get the Image specific attributes
	 * 
	 * @param filePath path to the file
	 * @param obj represents the Media File Data
	 * @return a JSONObject that represents the Media File Data
	 * @throws JSONException
	 */
	private JSONObject getImageData(String filePath, JSONObject obj) throws JSONException {
		Bitmap bitmap = BitmapFactory.decodeFile(FileUtils.stripFileProtocol(filePath));
		obj.put("height", bitmap.getHeight());
		obj.put("width", bitmap.getWidth());
		bitmap.recycle();
		return obj;
	}

	/**
	 * Get the Image specific attributes
	 * 
	 * @param filePath path to the file
	 * @param obj represents the Media File Data
	 * @param video if true get video attributes as well
	 * @return a JSONObject that represents the Media File Data
	 * @throws JSONException
	 */
	private JSONObject getAudioVideoData(String filePath, JSONObject obj, boolean video) throws JSONException {
		MediaPlayer player = new MediaPlayer();
		try {
			player.setDataSource(filePath);
			player.prepare();
			obj.put("duration", player.getDuration()/1000);
			if (video) {
				obj.put("height", player.getVideoHeight());
				obj.put("width", player.getVideoWidth());
			}
		}
		catch (IOException e) {
			Log.d(LOG_TAG, "Error: loading video file");
		} 
		return obj;
	}

	/**
	 * Sets up an intent to capture audio.  Result handled by onActivityResult()
	 */
	private void captureAudio(JSONArray args) {
		JSONObject arg = args.optJSONObject(0);

		String filename =  null;
		boolean highRes = false;

		if(arg!=null){
			
			//[20120919][chisu]support file:// and file:///
			String tempFilename = arg.optString("destinationFilename");
			if(tempFilename != null){
				filename = makeCapturedFilename(tempFilename);
			}
			
			highRes = arg.optBoolean("highRes");

			if(filename == null) filename = "audio.wav";
			if(highRes) highRes = true;
		}
		
		Intent intent = new Intent(android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION);
		
		if(filename != null){
			// Specify file so that large image is captured and returned
			File audio = new File(Environment.getExternalStorageDirectory(),  filename);
			intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(audio));
			this.audioUri = Uri.fromFile(audio);
		}
		
		this.ctx.startActivityForResult((Plugin) this, intent, CAPTURE_AUDIO);
	}

	//[20120919][chisu]support file:// and file:///
	private String makeCapturedFilename(String beforeStr){
		if((beforeStr.contains("file:///mnt/sdcard")))
			return beforeStr.replace("file:///mnt/sdcard","");
		else if((beforeStr.contains("file://mnt/sdcard")))
			return beforeStr.replace("file://mnt/sdcard","");
		else if((beforeStr.contains("file:///")))
			return beforeStr.replace("file:///","mnt/");
		else if(beforeStr.contains("file://"))
			return beforeStr.replace("file://","mnt/");		
		else
			return beforeStr;
	}
	
	private void checkDirisExists(JSONObject arg){
		String tempFilename = arg.optString("destinationFilename");
		String filedir = null;
		if(tempFilename != null){
			//[20120919][chisu]support file:// and file:///
			filedir = makeCapturedFilename(tempFilename);
			int lastslash = filedir.lastIndexOf("/");
			filedir = filedir.substring(0, lastslash);
			File saveddir = new File(filedir);
			if(!saveddir.exists())
				saveddir.mkdirs();
		}
	}
	
	/**
	 * Sets up an intent to capture images.  Result handled by onActivityResult()
	 */
	private void captureImage(JSONArray args) {
		JSONObject arg = args.optJSONObject(0);

		String filename =  null;
		boolean highRes = false;

		if(arg!=null){
			//[20120913][chisu]check dir exists
			checkDirisExists(arg);	
			
			//[20120919][chisu]support file:// and file:///
			String tempFilename = arg.optString("destinationFilename");
			if(tempFilename != null){
				filename = makeCapturedFilename(tempFilename);
			}
			
			highRes = arg.optBoolean("highRes");

			if(highRes) highRes = true;
			
			if(filename != null){
				//[20120720][chisu]check extend format which is supported in Android 
				if(!checkSupportMediaFormat("image",filename)){
					Log.e(LOG_TAG, filename + " is not supported format in Android.");
					filename = null;
				}
			}		
		}

		Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

		if(filename != null){
			// Specify file so that large image is captured and returned
			File photo = new File(Environment.getExternalStorageDirectory(),  filename);
			intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
			this.imageUri = Uri.fromFile(photo);
		}
		
		this.ctx.startActivityForResult((Plugin) this, intent, CAPTURE_IMAGE);
	}

	/**
	 * Sets up an intent to capture video.  Result handled by onActivityResult()
	 */
	private void captureVideo(JSONArray args) {
		
		JSONObject arg = args.optJSONObject(0);

		String filename =  null;
		boolean highRes = false;

		if(arg!=null){
			//[20120913][chisu]check dir exists
			checkDirisExists(arg);	
			
			//[20120919][chisu]support file:// and file:///
			String tempFilename = arg.optString("destinationFilename");
			if(tempFilename != null){
				filename = makeCapturedFilename(tempFilename);
			}
			
			highRes = arg.optBoolean("highRes");

			if(highRes) highRes = true;
			
			if(filename != null){
				//[20120720][chisu]check extend format which is supported in Android 
				if(!checkSupportMediaFormat("video",filename)){
					Log.e(LOG_TAG, filename + " is not supported format in Android.");
					filename = null;
				}
			}	
		}
		
		Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
		
		if(filename != null){
			// Specify file so that large image is captured and returned
			File video = new File(Environment.getExternalStorageDirectory(),  filename);
			intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(video));
			this.videoUri = Uri.fromFile(video);
		}
		// Introduced in API 8
		//intent.putExtra(android.provider.MediaStore.EXTRA_DURATION_LIMIT, duration);

		this.ctx.startActivityForResult((Plugin) this, intent, CAPTURE_VIDEO);
	}

	/**
	 * Called when the video view exits. 
	 * 
	 * @param requestCode       The request code originally supplied to startActivityForResult(), 
	 *                          allowing you to identify who this result came from.
	 * @param resultCode        The integer result code returned by the child activity through its setResult().
	 * @param intent            An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
	 * @throws JSONException 
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		Log.d(LOG_TAG, "capture result:: " + requestCode);
		
		// Result received okay
		if (resultCode == Activity.RESULT_OK) {
			// An audio clip was requested
			if (requestCode == CAPTURE_AUDIO) {
				//[20120719][chisu]media scan first
				this.ctx.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
						Uri.parse("file://"
						+ Environment.getExternalStorageDirectory()))); 
				
				if(intent != null){
					Uri data = intent.getData();
					// create a file full path from the uri
					String fullpath = getFullPath(data);
					// Send Uri back to JavaScript for viewing image
					this.success(new PluginResult(PluginResult.Status.OK, fullpath), this.callbackId);					
				}
				else{
					String fullpath = this.audioUri.toString();
					this.success(new PluginResult(PluginResult.Status.OK, fullpath), this.callbackId);
				}
			} else if (requestCode == CAPTURE_IMAGE) {
				//[20120719][chisu]media scan first
				this.ctx.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
						Uri.parse("file://"
						+ Environment.getExternalStorageDirectory()))); 
				
				if(intent != null){
					Uri data = intent.getData();
					// create a file full path from the uri
					String fullpath = getFullPath(data);
					// Send Uri back to JavaScript for viewing image
					this.success(new PluginResult(PluginResult.Status.OK, fullpath), this.callbackId);					
				}
				else{
					String fullpath = this.imageUri.toString();
					this.success(new PluginResult(PluginResult.Status.OK, fullpath), this.callbackId);
				}

			} else if (requestCode == CAPTURE_VIDEO) {
				//[20120719][chisu]media scan first
				this.ctx.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
						Uri.parse("file://"
						+ Environment.getExternalStorageDirectory()))); 
				
				if(intent != null){
					Uri data = intent.getData();
					// create a file full path from the uri
					String fullpath = getFullPath(data);
					// Send Uri back to JavaScript for viewing image
					this.success(new PluginResult(PluginResult.Status.OK, fullpath), this.callbackId);					
				}
				else{
					String fullpath = this.videoUri.toString();
					this.success(new PluginResult(PluginResult.Status.OK, fullpath), this.callbackId);
				}
			}
		}
		// If canceled
		else if (resultCode == Activity.RESULT_CANCELED) {
			this.fail(createErrorObject(DeviceAPIErrors.UNKNOWN_ERR, "Canceled."));

		}
		// If something else
		else {
			this.fail(createErrorObject(DeviceAPIErrors.UNKNOWN_ERR, "Did not complete!"));
		}
	}
	//[20120719][chisu]make path easily
	private String getFullPath(Uri data){
		File fp = new File(FileUtils.getRealPathFromURI(data, this.ctx));
		return "file://"+ fp.getAbsolutePath();
	}

	//[20120720][chisu]checksupprotFormat
	private boolean checkSupportMediaFormat(String type,String filename){
		String[] supportedImage = {".jpg",".gif",".png",".bmp",".webp"};
		String[] supportedVideo = {".3gp",".mp4",".mkv",".webm"};
		
		if(type.equalsIgnoreCase("image")){
			for(String imgformat : supportedImage){
				if(filename.contains(imgformat))
					return true;
			}
		}
		else if(type.equalsIgnoreCase("video")){
			for(String videoformat : supportedVideo){
				if(filename.contains(videoformat))
					return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Creates a JSONObject that represents a File from the Uri
	 *  
	 * @param data the Uri of the audio/image/video
	 * @return a JSONObject that represents a File
	 * @throws IOException 
	 */
	private JSONObject createMediaFile(Uri data){
		File fp = new File(FileUtils.getRealPathFromURI(data, this.ctx));
		JSONObject obj = new JSONObject();

		try {       
			// File properties
			obj.put("name", fp.getName());
			obj.put("fullPath", "file://" + fp.getAbsolutePath());

			// Because of an issue with MimeTypeMap.getMimeTypeFromExtension() all .3gpp files 
			// are reported as video/3gpp. I'm doing this hacky check of the URI to see if it 
			// is stored in the audio or video content store.
			if (fp.getAbsoluteFile().toString().endsWith(".3gp") || fp.getAbsoluteFile().toString().endsWith(".3gpp")) {
				if (data.toString().contains("/audio/")) {
					obj.put("type", AUDIO_3GPP);                
				} else {
					obj.put("type", VIDEO_3GPP);                
				}               
			} else {
				obj.put("type", FileUtils.getMimeType(fp.getAbsolutePath()));                
			}

			obj.put("lastModifiedDate", fp.lastModified());
			obj.put("size", fp.length());
		} catch (JSONException e) {
			// this will never happen
			e.printStackTrace();
		}

		return obj;
	}

	/**
	 * Send error message to JavaScript.
	 * 
	 * @param err
	 */
	public void fail(JSONObject err) {
		this.error(new PluginResult(PluginResult.Status.ERROR, err), this.callbackId);
	}
}
