package org.skt.runtime.additionalapis;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skt.runtime.ChildActivity;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;
import org.skt.runtime.html5apis.DeviceAPIErrors;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings.PluginState;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class ChildBrowser extends Plugin{

	protected static final String LOG_TAG = "ChildBrowser";
	private static int CLOSE_EVENT = 0;
	private static int LOCATION_CHANGED_EVENT = 1;
	private static int EMPTY_EVENT = -1;

	private String browserCallbackId = null;

	private Dialog dialog;
	private WebView webview;
	private EditText edittext;
	private boolean showLocationBar = true;

	public static final int CALL_CHILD_BROWSER = 10001;     
	private String callbackId;    
	
	/**
	 * Executes the request and returns PluginResult.
	 *
	 * @param action        The action to execute.
	 * @param args          JSONArry of arguments for the plugin.
	 * @param callbackId    The callback id used when calling back into JavaScript.
	 * @return              A PluginResult object with a status and message.
	 */
	public PluginResult execute(String action, JSONArray args, String callbackId) {
		PluginResult.Status status = PluginResult.Status.OK;
		String result = "";

		this.callbackId = callbackId;
		try {
			if (action.equals("showWebPage")) {
				this.browserCallbackId = callbackId;

				Intent childintent = new Intent(ctx.getContext(), ChildActivity.class);
				childintent.putExtra("childurl", args.optString(0));

				if(ChildActivity.childActivity == null){
					this.ctx.startActivityForResult((Plugin) this, childintent, CALL_CHILD_BROWSER);
					
					//this.ctx.startActivityForResult(childintent, CALL_CHILD_BROWSER);
					//ctx.startActivity(childintent);
					
					PluginResult pluginResult = new PluginResult(status, result);
					pluginResult.setKeepCallback(true);
					return pluginResult;
				}
				else{
					status = PluginResult.Status.ERROR;
					PluginResult pluginResult = new PluginResult(status, result);
					return pluginResult;
				}

				//	                // If the ChildBrowser is already open then throw an error
				//	                if (dialog != null && dialog.isShowing()) {
				//	                    return new PluginResult(PluginResult.Status.ERROR, createErrorObject(DeviceAPIErrors.INVALID_STATE_ERR,"ChildBrowser is already open"));
				//	                }
				//
				//	                result = this.showWebPage(args.getString(0), args.optJSONObject(1));
				//
				//	                if (result.length() > 0) {
				//	                    status = PluginResult.Status.ERROR;
				//	                    return new PluginResult(status, result);
				//	                } else {
				//	                    PluginResult pluginResult = new PluginResult(status, result);
				//	                    pluginResult.setKeepCallback(true);
				//	                    return pluginResult;
				//	                }
			}
			else if (action.equals("close")) {
				//closeDialog();
				if(ChildActivity.childActivity != null)
					ChildActivity.childActivity.finish();

				JSONObject obj = new JSONObject();
				obj.put("type", CLOSE_EVENT);

				PluginResult pluginResult = new PluginResult(status, obj);
				pluginResult.setKeepCallback(false);
				return pluginResult;
			}
			else if (action.equals("openExternal")) {
				result = this.openExternal(args.getString(0), args.optBoolean(1));
				if (result.length() > 0) {
					status = PluginResult.Status.ERROR;
				}
			}
			else {
				status = PluginResult.Status.INVALID_ACTION;
			}
			return new PluginResult(status, result);
		} catch (JSONException e) {
			return new PluginResult(PluginResult.Status.JSON_EXCEPTION);
		}
	}

	/**
	 * Display a new browser with the specified URL.
	 *
	 * @param url           The url to load.
	 * @param usePhoneGap   Load url in PhoneGap webview
	 * @return              "" if ok, or error message.
	 */
	public String openExternal(String url, boolean usePhoneGap) {
		try {
			Intent intent = null;
			if (usePhoneGap) {
				intent = new Intent().setClass(this.ctx.getContext(), org.skt.runtime.RuntimeActivity.class);
				intent.setData(Uri.parse(url)); // This line will be removed in future.
				intent.putExtra("url", url);

				// Timeout parameter: 60 sec max - May be less if http device timeout is less.
				intent.putExtra("loadUrlTimeoutValue", 60000);

				// These parameters can be configured if you want to show the loading dialog
				intent.putExtra("loadingDialog", "Wait,Loading web page...");   // show loading dialog
				intent.putExtra("hideLoadingDialogOnPageLoad", true);           // hide it once page has completely loaded
			}
			else {
				intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(url));
			}
			this.ctx.getContext().startActivity(intent);
			return "";
		} catch (android.content.ActivityNotFoundException e) {
			Log.d(LOG_TAG, "ChildBrowser: Error loading url "+url+":"+ e.toString());
			return e.toString();
		}
	}

	/**
	 * Closes the dialog
	 */
	private void closeDialog() {
		if (dialog != null) {
			dialog.dismiss();
		}
	}

	/**
	 * Checks to see if it is possible to go back one page in history, then does so.
	 */
	private void goBack() {
		if (this.webview.canGoBack()) {
			this.webview.goBack();
		}
	}

	/**
	 * Checks to see if it is possible to go forward one page in history, then does so.
	 */
	private void goForward() {
		if (this.webview.canGoForward()) {
			this.webview.goForward();
		}
	}

	/**
	 * Navigate to the new page
	 *
	 * @param url to load
	 */
	private void navigate(String url) {
		InputMethodManager imm = (InputMethodManager)this.ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(edittext.getWindowToken(), 0);

		if (!url.startsWith("http") && !url.startsWith("file:")) {
			this.webview.loadUrl("http://" + url);
		} else {
			this.webview.loadUrl(url);
		}
		this.webview.requestFocus();
	}


	/**
	 * Should we show the location bar?
	 *
	 * @return boolean
	 */
	private boolean getShowLocationBar() {
		return this.showLocationBar;
	}

	//[20130417][chisu]childbrowser callback
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		Log.d(LOG_TAG, "childbrowser result:: " + requestCode);

		// Result received okay

		if(requestCode == CALL_CHILD_BROWSER){
			JSONObject obj = new JSONObject();
			PluginResult.Status status = PluginResult.Status.OK;

			try {
				obj.put("type", CLOSE_EVENT);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			PluginResult pluginResult = new PluginResult(status, obj);
			pluginResult.setKeepCallback(false);
			
			this.success(pluginResult, this.callbackId);
		}
	}

	/**
	 * Display a new browser with the specified URL.
	 *
	 * @param url           The url to load.
	 * @param jsonObject
	 */
	public String showWebPage(final String url, JSONObject options) {
		// Determine if we should hide the location bar.
		if (options != null) {
			showLocationBar = options.optBoolean("showLocationBar", true);
		}

		// Create dialog in new thread
		Runnable runnable = new Runnable() {
			/**
			 * Convert our DIP units to Pixels
			 *
			 * @return int
			 */
			private int dpToPixels(int dipValue) {
				int value = (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP,
						(float) dipValue,
						ctx.getResources().getDisplayMetrics()
						);

				return value;
			}

			public void run() {
				// Let's create the main dialog
				dialog = new Dialog(ctx.getContext(), android.R.style.Theme_NoTitleBar);
				dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dialog.setCancelable(true);
				dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						try {
							JSONObject obj = new JSONObject();
							obj.put("type", CLOSE_EVENT);

							sendUpdate(obj, false);
						} catch (JSONException e) {
							Log.d(LOG_TAG, "Should never happen");
						}
					}
				});

				// Main container layout
				LinearLayout main = new LinearLayout(ctx.getContext());
				main.setOrientation(LinearLayout.VERTICAL);

				// Toolbar layout
				RelativeLayout toolbar = new RelativeLayout(ctx.getContext());
				toolbar.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, this.dpToPixels(44)));
				toolbar.setPadding(this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2));
				toolbar.setHorizontalGravity(Gravity.LEFT);
				toolbar.setVerticalGravity(Gravity.TOP);

				// Action Button Container layout
				RelativeLayout actionButtonContainer = new RelativeLayout(ctx.getContext());
				actionButtonContainer.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				actionButtonContainer.setHorizontalGravity(Gravity.LEFT);
				actionButtonContainer.setVerticalGravity(Gravity.CENTER_VERTICAL);
				actionButtonContainer.setId(1);

				// Back button
				ImageButton back = new ImageButton(ctx.getContext());
				RelativeLayout.LayoutParams backLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
				backLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
				back.setLayoutParams(backLayoutParams);
				back.setContentDescription("Back Button");
				back.setId(2);
				try {
					back.setImageBitmap(loadDrawable("sktjs/childbrowser/icon_arrow_left.png"));
				} catch (IOException e) {
					Log.e(LOG_TAG, e.getMessage(), e);
				}
				back.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						goBack();
					}
				});

				// Forward button
				ImageButton forward = new ImageButton(ctx.getContext());
				RelativeLayout.LayoutParams forwardLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
				forwardLayoutParams.addRule(RelativeLayout.RIGHT_OF, 2);
				forward.setLayoutParams(forwardLayoutParams);
				forward.setContentDescription("Forward Button");
				forward.setId(3);
				try {
					forward.setImageBitmap(loadDrawable("sktjs/childbrowser/icon_arrow_right.png"));
				} catch (IOException e) {
					Log.e(LOG_TAG, e.getMessage(), e);
				}
				forward.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						goForward();
					}
				});

				// Edit Text Box
				edittext = new EditText(ctx.getContext());
				RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				textLayoutParams.addRule(RelativeLayout.RIGHT_OF, 1);
				textLayoutParams.addRule(RelativeLayout.LEFT_OF, 5);
				edittext.setLayoutParams(textLayoutParams);
				edittext.setId(4);
				edittext.setSingleLine(true);
				edittext.setText(url);
				edittext.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
				edittext.setImeOptions(EditorInfo.IME_ACTION_GO);
				edittext.setInputType(InputType.TYPE_NULL); // Will not except input... Makes the text NON-EDITABLE
				edittext.setOnKeyListener(new View.OnKeyListener() {
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						// If the event is a key-down event on the "enter" button
						if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
							navigate(edittext.getText().toString());
							return true;
						}
						return false;
					}
				});

				// Close button
				ImageButton close = new ImageButton(ctx.getContext());
				RelativeLayout.LayoutParams closeLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
				closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				close.setLayoutParams(closeLayoutParams);
				forward.setContentDescription("Close Button");
				close.setId(5);
				try {
					close.setImageBitmap(loadDrawable("sktjs/childbrowser/icon_close.png"));
				} catch (IOException e) {
					Log.e(LOG_TAG, e.getMessage(), e);
				}
				close.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						closeDialog();
					}
				});

				// WebView
				webview = new WebView(ctx.getContext());
				webview.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				webview.setWebChromeClient(new WebChromeClient());
				WebViewClient client = new ChildBrowserClient(edittext);
				webview.setWebViewClient(client);
				WebSettings settings = webview.getSettings();
				settings.setJavaScriptEnabled(true);
				settings.setJavaScriptCanOpenWindowsAutomatically(true);
				settings.setBuiltInZoomControls(true);
				settings.setPluginState(PluginState.ON);
				settings.setDomStorageEnabled(true);
				webview.loadUrl(url);
				webview.setId(6);
				webview.getSettings().setLoadWithOverviewMode(true);
				webview.getSettings().setUseWideViewPort(true);
				webview.requestFocus();
				webview.requestFocusFromTouch();

				// Add the back and forward buttons to our action button container layout
				actionButtonContainer.addView(back);
				actionButtonContainer.addView(forward);

				// Add the views to our toolbar
				toolbar.addView(actionButtonContainer);
				toolbar.addView(edittext);
				toolbar.addView(close);

				// Don't add the toolbar if its been disabled
				if (getShowLocationBar()) {
					// Add our toolbar to our main view/layout
					main.addView(toolbar);
				}

				// Add our webview to our main view/layout
				main.addView(webview);

				WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
				lp.copyFrom(dialog.getWindow().getAttributes());
				lp.width = WindowManager.LayoutParams.MATCH_PARENT;
				lp.height = WindowManager.LayoutParams.MATCH_PARENT;

				dialog.setContentView(main);
				dialog.show();
				dialog.getWindow().setAttributes(lp);
			}

			private Bitmap loadDrawable(String filename) throws java.io.IOException {
				InputStream input = ctx.getAssets().open(filename);
				return BitmapFactory.decodeStream(input);
			}
		};
		this.ctx.runOnUiThread(runnable);
		return "";
	}

	/**
	 * Create a new plugin result and send it back to JavaScript
	 *
	 * @param obj a JSONObject contain event payload information
	 */
	private void sendUpdate(JSONObject obj, boolean keepCallback) {
		if (this.browserCallbackId != null) {
			PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
			result.setKeepCallback(keepCallback);
			this.success(result, this.browserCallbackId);
		}
	}

	/**
	 * The webview client receives notifications about appView
	 */
	public class ChildBrowserClient extends WebViewClient {
		EditText edittext;

		/**
		 * Constructor.
		 *
		 * @param mContext
		 * @param edittext
		 */
		public ChildBrowserClient(EditText mEditText) {
			this.edittext = mEditText;
		}

		/**
		 * Notify the host application that a page has started loading.
		 *
		 * @param view          The webview initiating the callback.
		 * @param url           The url of the page.
		 */
		@Override
		public void onPageStarted(WebView view, String url,  Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			String newloc;
			if (url.startsWith("http:") || url.startsWith("https:") || url.startsWith("file:")) {
				newloc = url;
			} else {
				newloc = "http://" + url;
			}

			if (!newloc.equals(edittext.getText().toString())) {
				edittext.setText(newloc);
			}

			try {
				JSONObject obj = new JSONObject();
				obj.put("type", LOCATION_CHANGED_EVENT);
				obj.put("location", url);

				sendUpdate(obj, true);
			} catch (JSONException e) {
				Log.d("ChildBrowser", "This should never happen");
			}
		}
	}

}
