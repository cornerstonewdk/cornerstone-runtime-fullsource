package org.skt.runtime.html5apis;

import java.net.URI;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skt.runtime.api.IPlugin;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;
import org.skt.runtime.api.RuntimeInterface;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.MimeTypeMap;

public class Messaging extends Plugin{

	private static final String LOG_TAG = "Messaging";
	private final int TYPE_SMS 		= 1 ;
	private final int TYPE_MMS 		= 2 ;
	private final int TYPE_EMAIL 	= 3 ;

	private final int SMS_RESULT 	= 101;
	private final int MMS_RESULT 	= 102;
	private final int EMAIL_RESULT 	= 103;

	private String callbackId;

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
		result.setKeepCallback(true);	

		if (action.equals("sendMessage")) {

			Log.e(LOG_TAG, "sendMessage");
			this.callbackId = callbackId;

			JSONObject msg;
			try {
				msg = args.getJSONObject(0);
				int msgtype = msg.getInt("type");

				switch(msgtype){
				case TYPE_SMS:
					sendSMS(msg);break;
				case TYPE_MMS:
					sendMMS(msg);break;
				case TYPE_EMAIL:
					sendEmail(msg);break;
				default:
					break;
				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			// Unsupported action
			return new PluginResult(PluginResult.Status.INVALID_ACTION);
		}

		return result;
	}

	private void sendSMS(JSONObject msg) throws JSONException {
		//		[20120627][chisu]Send sms using SKT API 
		//    	String[] strArrData = mMessage.getTo();
		//    	for(int i = 0 ; i <strArrData.length ; i ++){
		//    		String strBody = mMessage.getBody();
		//    		String strFrom = mMessage.getFrom();
		//    		Intent it = new Intent("com.android.mms.transaction.Send.SMS");
		//    		it.putExtra("recipient", new String[]{strArrData[i]} ); 
		//    		it.putExtra("sms_voip_sender", strFrom);
		//    		
		//    		if(TextUtils.isEmpty(mMessage.getBody()))
		//    			it.putExtra("text","empty String");
		//    		else
		//    			it.putExtra("text", strBody); 
		//    		
		//    		mContext.sendBroadcast(it);	   
		//    		
		//    		try {
		//				Thread.sleep(2000);
		//			} catch (InterruptedException e) {
		//				// TODO Auto-generated catch block
		//				e.printStackTrace();
		//			}

		String body = msg.getString("body");
		JSONArray toarray = msg.getJSONArray("to");
		String to = getToList(toarray);

		Uri smsuri = Uri.parse("sms:" + to);
		Intent sendIntent = new Intent(Intent.ACTION_SENDTO, smsuri);
		sendIntent.putExtra("sms_body", body);

		this.ctx.startActivityForResult((Plugin)this,sendIntent,SMS_RESULT);
	}

	//[20120919][chisu]support file:// and file:///
	private String makeAttachmentFilename(String beforeStr){
		if((beforeStr.contains("file:///mnt/sdcard")))
			return beforeStr;
		else if((beforeStr.contains("file://mnt/sdcard")))
			return beforeStr;
		else if((beforeStr.contains("file:///")))
			return beforeStr.replace("file:///","file:///mnt/");
		else if(beforeStr.contains("file://"))
			return beforeStr.replace("file://","file://mnt/");		
		else
			return beforeStr;
	}
		
	private void sendMMS(JSONObject msg) throws JSONException{
		Intent sendIntent = new Intent();

		//[20120619][chisu]get attachment from msg
		if(msg.has("attachments")){
			JSONArray attachments = msg.getJSONArray("attachments");
			int cnt = attachments.length();
			
			if(cnt == 0) {
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.setType("text/plain");
			} else if(cnt == 1) {
				
				JSONObject attachment = attachments.getJSONObject(0);
				sendIntent.setAction(Intent.ACTION_SEND);

				String fullPath = getJsonString(attachment, "fullPath");
				
				if(fullPath != null){
					fullPath = makeAttachmentFilename(fullPath);
					URI uri = URI.create(fullPath);
					
					if(fullPath != null){		
						Uri fileUri = getAttachmentUri(uri.toString());				
						sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
						sendIntent.setType(getMimeType(fileUri.toString()));
					}
				}
			} else if(cnt >= 2) {
				sendIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
				ArrayList<Uri> fileUris = new ArrayList<Uri>();
				for(int i = 0; i < cnt; i++) {				
					JSONObject attachment = attachments.getJSONObject(i);
					String fullPath = getJsonString(attachment, "fullPath");
					
					if(fullPath != null){
						fullPath = makeAttachmentFilename(fullPath);
						URI uri = URI.create(fullPath);
						if(fullPath != null){		
							fileUris.add(getAttachmentUri(uri.toString()))	;
						}
					}								
				}
				
				sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
				sendIntent.setType(getMimeType(fileUris.get(0).toString()));
			}
		}
		else {
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.setType("text/plain");
		}


		//[20120619][chisu]get to array from msg
		JSONArray toarray = msg.getJSONArray("to");
		ArrayList<String> toList = new ArrayList<String>();
		for(int i = 0 ; i < toarray.length() ; i ++){
			toList.add(toarray.getString(i));
		}

		sendIntent.putStringArrayListExtra("sendto", toList);
		sendIntent.putExtra("exit_on_sent", true);

		//[20120619][chisu]get subject from msg
		//if(!getVendorString().equalsIgnoreCase("samsung")){
		if(msg.has("subject")){
			String subject = msg.getString("subject");
			sendIntent.putExtra("subject", subject);			
		}
		//}

		//[20120619][chisu]get body from msg
		if(msg.has("body")){
			String body = msg.getString("body");
			sendIntent.putExtra("sms_body", body);
		}

		this.ctx.startActivityForResult((Plugin)this,
				Intent.createChooser(sendIntent, "Please pick your preferred MMS application."),
				MMS_RESULT);
	}

	private void sendEmail(JSONObject msg) throws JSONException{
		Intent sendIntent = new Intent();

		// attachments
		if(msg.has("attachments")) {
			JSONArray attachments = msg.getJSONArray("attachments");
			int cnt = attachments.length();
			if(cnt == 0) {
				sendIntent.setAction(Intent.ACTION_SEND);
			} else if(cnt == 1) {
				JSONObject attachment = attachments.getJSONObject(0);
				sendIntent.setAction(Intent.ACTION_SEND);

				String fullPath = getJsonString(attachment, "fullPath");
				if(fullPath != null){	
					fullPath = makeAttachmentFilename(fullPath);
				
					//[20120905][chisu]if path has white space
					if(fullPath.contains(" "))
						fullPath = fullPath.replace(" ", "%20");

					URI uri = URI.create(fullPath);
					
					Uri fileUri = getAttachmentUri(uri.toString());				
					sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
				}
				
			} else if(cnt >= 2) {
				sendIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
				ArrayList<Uri> fileUris = new ArrayList<Uri>();
				
				for(int i = 0; i < cnt; i++) {
					JSONObject attachment = attachments.getJSONObject(i);
					String fullPath = getJsonString(attachment, "fullPath");
					
					if(fullPath != null){		
						fullPath = makeAttachmentFilename(fullPath);
					
						//[20120905][chisu]if path has white space
						if(fullPath.contains(" "))
							fullPath = fullPath.replace(" ", "%20");

						URI uri = URI.create(fullPath);

						fileUris.add(getAttachmentUri(uri.toString()))	;
					}	
				}
				sendIntent.putExtra(Intent.EXTRA_STREAM, fileUris);
			}
		} else {
			sendIntent.setAction(Intent.ACTION_SEND);
		}

		sendIntent.setType("text/plain");
		//sendIntent.setType("image/jpeg");
		sendIntent.putExtra(Intent.EXTRA_EMAIL,convertJSONArrayToStringArray(msg,"to"));
		sendIntent.putExtra(Intent.EXTRA_CC,convertJSONArrayToStringArray(msg,"cc") );
		sendIntent.putExtra(Intent.EXTRA_BCC,convertJSONArrayToStringArray(msg,"bcc") );
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, msg.has("subject")?msg.getString("subject"):null);
		sendIntent.putExtra(Intent.EXTRA_TEXT, msg.has("body")?msg.getString("body"):null);

		this.ctx.startActivityForResult((IPlugin)this,
				Intent.createChooser(sendIntent, "Please pick your preferred email application."),
				EMAIL_RESULT);
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

		// Result received okay
		//if (resultCode == Activity.RESULT_OK) {
		if (requestCode == SMS_RESULT) {
			PluginResult result = new PluginResult(PluginResult.Status.OK);
			result.setKeepCallback(false);

			this.success(result, this.callbackId);
		} else if (requestCode == MMS_RESULT) {
			PluginResult result = new PluginResult(PluginResult.Status.OK);
			result.setKeepCallback(false);

			this.success(result, this.callbackId);
		} else if (requestCode == EMAIL_RESULT) {
			PluginResult result = new PluginResult(PluginResult.Status.OK);
			result.setKeepCallback(false);

			this.success(result, this.callbackId);
		}
		//}

		// If canceled
		else if (resultCode == Activity.RESULT_CANCELED) {
			this.error(new PluginResult(PluginResult.Status.ERROR, createErrorObject(DeviceAPIErrors.UNKNOWN_ERR, "SendMessage Canceled.")), this.callbackId);
		}
		// If something else
		else {
			this.error(new PluginResult(PluginResult.Status.ERROR, createErrorObject(DeviceAPIErrors.UNKNOWN_ERR, "SendMessage Canceled.")), this.callbackId);
		}
	}

	private String[] convertJSONArrayToStringArray(JSONObject msg , String name) throws JSONException{
		if(msg.has(name)){
			JSONArray jarray = msg.getJSONArray(name);
			ArrayList<String> strList = new ArrayList<String>();
			for(int i = 0 ; i < jarray.length() ; i ++){
				strList.add(jarray.getString(i));
			}
			String[] reval = new String[strList.size()];
			for(int i = 0 ; i < strList.size() ; i ++){
				reval[i] = strList.get(i);
			}	
			return reval;
		}
		else
			return null;
	}

	private String getToList(JSONArray toarray){

		StringBuilder sb = new StringBuilder("");
		for(int i = 0 ; i < toarray.length() ; i ++){
			try {
				sb.append(toarray.get(i));
				sb.append(",");
				//sb.append(";");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	private String getMimeType(String fileUrl) {
		MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
		String extension = MimeTypeMap.getFileExtensionFromUrl(fileUrl);
		String mimeType;

		if (extension != null && extension.equalsIgnoreCase("m4v")) {  // Can not find m4v;;
			mimeType = "video/mp4";
		} else {
			mimeType = mimeTypeMap.getMimeTypeFromExtension(extension);
		}

		return mimeType;
	}

	private  String getVendorString() {
		String vendor = Build.MANUFACTURER;
		return vendor;
	}

	protected String getJsonString(JSONObject obj, String property) {
		String value = null;
		try {
			if (obj != null) {
				value = obj.getString(property);
				if (value.equals("null")) {
					Log.d(LOG_TAG, property + " is string called 'null'");
					value = null;
				}
			}
		}
		catch (JSONException e) {
			Log.d(LOG_TAG, "Could not get = " + e.getMessage());
		}   
		return value;
	}

	private Uri getAttachmentUri(String fullpath) {
		try {
			return Uri.parse(fullpath);
		}
		catch (Exception e) {
			return null;
		}
	}
}
