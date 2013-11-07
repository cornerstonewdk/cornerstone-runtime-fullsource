package org.skt.runtime.additionalapis;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;
import org.skt.runtime.api.RuntimeInterface;
import org.skt.runtime.html5apis.DeviceAPIErrors;
import org.skt.runtime.html5apis.file.FileUtils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

public class ScreenShot extends Plugin{

	private static final String LOG_TAG = "ScreenShot";

	private String callbackId;

	private static final int CROP_FROM_CAMERA = 200;
	private String screenshotname = null;
	private boolean useCrop = false;

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

		this.callbackId = callbackId;
		PluginResult.Status status = PluginResult.Status.NO_RESULT;
		String message = "";
		PluginResult result = new PluginResult(status, message);

		if (action.equals("captureScreenshot")) {
			try {
				useCrop = args.optBoolean(1);
				if(useCrop == false){
					String path = screenshot(args);			
					return new PluginResult(PluginResult.Status.OK,path);					
				}
				else{
					result.setKeepCallback(true);
					screenshotWithCrop(args);
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return new PluginResult(PluginResult.Status.JSON_EXCEPTION,createErrorObject(DeviceAPIErrors.UNKNOWN_ERR,"exception is happened!"));
			}		
		}

		return result;
	}

	private static Bitmap pictureDrawable2Bitmap(PictureDrawable pictureDrawable){
		Bitmap bitmap = Bitmap.createBitmap(pictureDrawable.getIntrinsicWidth(),pictureDrawable.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawPicture(pictureDrawable.getPicture());
		return bitmap;
	}

	public String screenshot(JSONArray args) throws Exception {      

		screenshotname = args.optString(0);
		if(screenshotname.equals("null"))
			screenshotname = "screenshot.png";

		//use webview source
		Picture webviewpicture = this.webView.capturePicture();
		Bitmap screenshot = pictureDrawable2Bitmap(new PictureDrawable(webviewpicture));

		//use my source
		//View view = this.ctx.getRuntimeInterfaceWindow().getDecorView();
		//view.setDrawingCacheEnabled(true);
		//Bitmap screenshot = view.getDrawingCache();

		// crop image
		int x = args.optInt(2);
		int y = args.optInt(3);
		int width = args.optInt(4);
		int height = args.optInt(5);
		
		if(x >= 0 && y >= 0  && width != 0 && height != 0)
			screenshot = cropCenterBitmap(screenshot, x, y,width,height);

		File f = null;
		try {
			File tempDir = new File(Environment.getExternalStorageDirectory() + "/DCIM/camera/screenshot");
			tempDir.mkdirs();

			f = new File(Environment.getExternalStorageDirectory() + "/DCIM/camera/screenshot/", screenshotname);
			f.createNewFile();
			OutputStream outStream = new FileOutputStream(f);
			screenshot.compress(Bitmap.CompressFormat.PNG, 100, outStream);
			outStream.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//view.setDrawingCacheEnabled(false);

		ctx.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
				Uri.parse("file://" + Environment.getExternalStorageDirectory())));

		return "file://" + f.getAbsolutePath();
	}

	private void screenshotWithCrop(JSONArray args){
		screenshotname = args.optString(0);
		if(screenshotname.equals("null"))
			screenshotname = "screenshot.png";

		//use webview source
		Picture webviewpicture = this.webView.capturePicture();
		Bitmap screenshot = pictureDrawable2Bitmap(new PictureDrawable(webviewpicture));

		//use my source
		//View view = this.ctx.getRuntimeInterfaceWindow().getDecorView();
		//view.setDrawingCacheEnabled(true);
		//Bitmap screenshot = view.getDrawingCache();

		// crop image
		//screenshot = cropCenterBitmap(screenshot, 200, 600);

		File f = null;
		try {
			File tempDir = new File(Environment.getExternalStorageDirectory() + "/DCIM/camera/screenshot");
			tempDir.mkdirs();

			f = new File(Environment.getExternalStorageDirectory() + "/DCIM/camera/screenshot/" + screenshotname);
			f.createNewFile();
			OutputStream outStream = new FileOutputStream(f);
			screenshot.compress(Bitmap.CompressFormat.PNG, 100, outStream);
			outStream.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		//view.setDrawingCacheEnabled(false);		

		callCropActivity(f);
	}

	private void callCropActivity(File image){

		Intent intent = new Intent("com.android.camera.action.CROP");
		Uri tempimageUri = Uri.parse("file://" + image.getAbsolutePath());

		intent.setDataAndType(tempimageUri, "image/*");

		//intent.putExtra("outputX", 200);
		//intent.putExtra("outputY", 200);
		//intent.putExtra("aspectX", 1);
		//intent.putExtra("aspectY", 1);
		//intent.putExtra("scale", true);
		//intent.putExtra("return-data", true);
		intent.putExtra("output", tempimageUri);

		this.ctx.startActivityForResult((Plugin) this, intent, CROP_FROM_CAMERA);
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

		Log.d(LOG_TAG, "crop result:: " + requestCode);

		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == CROP_FROM_CAMERA) {
				final Bundle extras = intent.getExtras();
				if(extras != null)
				{
					File f = new File(Environment.getExternalStorageDirectory() + "/DCIM/camera/screenshot/", screenshotname);
					String fullpath = "file://" + f.getAbsolutePath();
					
					PluginResult result = new PluginResult(PluginResult.Status.OK,fullpath);
					result.setKeepCallback(false);

					this.success(result, this.callbackId);

					ctx.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
							Uri.parse("file://" + Environment.getExternalStorageDirectory())));
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

	/**
	 * Bitmap ?¥Î?ÏßÄÎ•?Í∞Ä?¥Îç∞Î•?Í∏∞Ï??ºÎ°ú w, h ?¨Í∏∞ ÎßåÌÅº crop?úÎã§. 
	 * 
	 * @param src ?êÎ≥∏
	 * @param x ?úÏûë x
	 * @param y ?úÏûë y
	 * @param w ?ìÏù¥
	 * @param h ?íÏù¥
	 * @return
	 */
	
	public Bitmap cropCenterBitmap(Bitmap src, int x, int y , int w, int h) {
		if(src == null)
			return null;

		int width = src.getWidth();
		int height = src.getHeight();

		//[20130719][chisu]exception process
		if(width < x || height < y)
			return src;
		if(width < w && height < h)
			return src;

		//[20130719][chisu]this code is used for center crop 
//		int x = 0;
//		int y = 0;
//
//		if(width > w)
//			x = (width - w)/2;
//
//		if(height > h)
//			y = (height - h)/2;

		int cw = w; // crop width
		int ch = h; // crop height

		if(w > width)
			cw = width;

		if(h > height)
			ch = height;

		return Bitmap.createBitmap(src, x, y, cw, ch);
	}

	@Override
	public boolean isSynch(String action) {
		// TODO Auto-generated method stub
		if(action.equals("captureScreenshot"))
			return true;

		return super.isSynch(action);
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
