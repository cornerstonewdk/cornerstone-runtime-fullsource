package org.skt.runtime.html5apis.webworker;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;
import org.skt.runtime.api.RuntimeInterface;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebWorker extends Plugin{

	private static final String LOG_TAG = "WebWorker";
	private WebView workerView;
	private AssetManager am = null;

	private String callbackId;
	private String jscontent;
	
	private Handler handler;

	/**
	 * Sets the context of the Command. This can then be used to do things like
	 * get file paths associated with the Activity.
	 * 
	 * @param ctx The context of the main Activity.
	 */
	public void setContext(RuntimeInterface ctx) {
		super.setContext(ctx);
		am = super.ctx.getAssets();
		workerView = new WebView((Activity)super.ctx);
		handler = new Handler();
		initwebview();
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

		try {		
			if (action.equals("makeworker")) {
				String workerjs;
				workerjs = args.getString(0);
				jscontent = parsejs(workerjs);
				
				//test 1. 
//				workerView.loadUrl("javascript:" + jscontent);	
				
				//test 2. 
//				super.ctx.runOnUiThread(new Runnable() {
//					
//					public void run() {
//						// TODO Auto-generated method stub
//						workerView.loadUrl("javascript:" + jscontent);		
//					}
//				});
				
				//test 3.
//				boolean postresult = workerView.post(new Runnable() {	
//					public void run() {
//						// TODO Auto-generated method stub
//						workerView.loadUrl("javascript:" + jscontent);						
//					}
//				});
//				
//				Log.e("postresult", String.valueOf(postresult));
				
				//test 4
				boolean postresult = handler.post(new Runnable() {	
					public void run() {
						// TODO Auto-generated method stub
						workerView.loadUrl("javascript:" + jscontent);						
					}
				});
				
				Log.e("postresult", String.valueOf(postresult));
							
			}
			else if(action.equalsIgnoreCase("postMessage")){
				jscontent = "onmessage(0)";
			
				//test 1.
				//workerView.loadUrl("javascript:" + jscontent);
						
				//test 2.
//				super.ctx.runOnUiThread(new Runnable() {
//
//					public void run() {
//						// TODO Auto-generated method stub
//						workerView.loadUrl("javascript:" + jscontent);		
//					}
//				});

//				//test 3 
//				boolean postresult = workerView.post(new Runnable() {	
//					public void run() {
//						// TODO Auto-generated method stub
//						workerView.loadUrl("javascript:" + jscontent);						
//					}
//				});
//				Log.e("postresult", String.valueOf(postresult));
				
				//test 4
				boolean postresult = handler.post(new Runnable() {	
					public void run() {
						// TODO Auto-generated method stub
						workerView.loadUrl("javascript:" + jscontent);						
					}
				});
				
				//test 5 
//				for(int i = 0 ; i < 100000 ; i ++){
//					Log.e("worker", String.valueOf(i));					
//				}
			}
			else {
				// Unsupported action
				return new PluginResult(PluginResult.Status.INVALID_ACTION);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}

	private void initwebview(){
		workerView.getSettings().setJavaScriptEnabled(true); 
		workerView.setWebChromeClient(new WebChromeClient());
		workerView.setWebViewClient(new WebViewClient());
	}

	private String parsejs(String workerjs){
		String jscontent = "";
		try{
			InputStream is = am.open("example/"+ workerjs); //am = Activity.getAssets()
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);

			String line;
			while (( line = br.readLine()) != null) {
				jscontent += line;
				jscontent += '\n';
			}
			is.close(); 
		}
		catch(Exception e){
			Log.e("exceiption", e.getMessage());
		}
		Log.e("Test", jscontent);

		return jscontent;
		//view.loadUrl("javascript:(" + jscontent + ")()");
		//view.loadUrl("javascript:" + jscontent );
	}

	
	@Override
	public boolean isSynch(String action) {
		// TODO Auto-generated method stub
		if(action.equalsIgnoreCase("makeworker") || action.equalsIgnoreCase("postMessage"))
			return false;
		return super.isSynch(action);
	}
}
