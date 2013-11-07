package org.skt.runtime;

import java.io.Serializable;
import java.util.regex.Pattern;

import org.skt.runtime.RuntimeStandAlone;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class UriReceiver extends Activity {
	
	private static final String TAG = "UriReceiver";
	
	private static final String ARGS_PAGE = "page";
	private static final String ARGS_ARGS = "args";
	
	public static final String FLAG_DATA = "URI_DATA";
	
	public static class UriData implements Serializable{
		private static final long serialVersionUID = 1L;
		
		public String page;
		public String args;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/*
		 * App 실행용 URI를 전달받는 과정
		 * 브라우저 -> 웹앱 실행을 위한 호출 규격은 다음과 같다.
		 * 
		 * - scheme : srtapp
		 * - host   : [App Name] - 웹앱 개발자가 별도로 Manifest에 정의해야 함
		 * - srtapp://[App Name]/page=[path]&args=[arg1|arg2|...]
		 */
		Uri uriData = getIntent().getData();
		
		Log.e(TAG, "*Received Uri*");
		Log.e(TAG, "scheme:"+uriData.getScheme());
		Log.e(TAG, "host:"+uriData.getHost());
		Log.e(TAG, "path:"+uriData.getPath());
		Log.e(TAG, "query:" + uriData.getQuery());
		
		//Intent relayIntent = new Intent(this, RuntimeStandAlone.class);
		Intent relayIntent = new Intent("skt.cornerstone.runtime.push");
		
		String path = uriData.getPath();
		String query = uriData.getQuery();
		
		//if(path != null)
		if(!path.equals(""))
			relayIntent.putExtra(FLAG_DATA, parseUriArgs(path,query));
		
		startActivity(relayIntent);
		finish();
	}
	
	private UriData parseUriArgs(String path, String query) {
		//path = path.replace("/", "");
		
		//[20130810][chisu]척번째 "/"를 삭제한다. 
		path = path.substring(1, path.length());
		
		UriData data = new UriData();
		
		if(query != null)
			data.page = path + "?" + query;
		else 
			data.page = path;
		
		return data;
	}
	
//	private UriData parseUriArgs(String path) {
//		UriData data = new UriData();
//		
//		path = path.replace("/", "");
//		
//		Pattern p = Pattern.compile("&");
//		String[] m = p.split(path);
//		
//		for(String str : m) {
//			
//			Log.e(TAG, "->"+str);
//			
//			if(str.contains(ARGS_PAGE)) {
//				data.page = str.replace(ARGS_PAGE+"=", "");
//			}
//			else if(str.contains(ARGS_ARGS)) {
//				data.args = str.replace(ARGS_PAGE+"=", "");
//			}
//		}
//		
//		return data;
//	}

}
