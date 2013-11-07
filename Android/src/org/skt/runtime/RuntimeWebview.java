package org.skt.runtime;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class RuntimeWebview extends WebView {
	
	private Context 							mContext;
	public View								    mCustomView;
	public FrameLayout							mCustomViewContainer;
	public WebChromeClient.CustomViewCallback 	mCustomViewCallback;
	
	public FrameLayout							mContentView;
	public FrameLayout							mBrowserFrameLayout;
	public FrameLayout							mLayout;
	
    static final String LOGTAG = "HTML5WebView";
	    
	private void init(Context context) {
		mContext = context;		
		Activity a = (Activity) mContext;
		
		mLayout = new FrameLayout(context);
		
		mBrowserFrameLayout = (FrameLayout) LayoutInflater.from(a).inflate(R.layout.custom_screen, null);
		mContentView = (FrameLayout) mBrowserFrameLayout.findViewById(R.id.main_content);
		mCustomViewContainer = (FrameLayout) mBrowserFrameLayout.findViewById(R.id.fullscreen_custom_content);
		
		mLayout.addView(mBrowserFrameLayout, COVER_SCREEN_PARAMS);
    
	    mContentView.addView(this);
	}

	public RuntimeWebview(Context context) {
		super(context);
		init(context);
	}

	public RuntimeWebview(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public RuntimeWebview(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	public FrameLayout getLayout() {
		return mLayout;
	}
	
    public boolean inCustomView() {
		return (mCustomView != null);
	}
	
	static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS =
        new FrameLayout.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
}