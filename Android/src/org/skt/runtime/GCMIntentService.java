package org.skt.runtime;

import java.util.List;

import org.skt.runtime.additionalapis.Preferences;
import org.skt.runtime.push.PushNotification;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService  extends GCMBaseIntentService{

	private static String LOG_TAG = "CornerstonePush";

	private Context ctx;

	private String message;
	private String title;
	private String getURL;

	private String registrationID;

	private static final int NICELYDONE				= 0;
	private static final int NICELYREGISTRED		= 1;
	private static final int BADTHINGSGOTHAPPENED	= 2;

	//서비스 생성자
	public GCMIntentService() {	
		//Input your project ID 
		super("173065860439");

		Log.d(LOG_TAG,"GCM서비스 생성자 실행");
	}

	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case NICELYDONE: 
				showPushNotification(title,message,getURL);
				break;
			case NICELYREGISTRED:
				showRegistrationID();
				break;
			case BADTHINGSGOTHAPPENED: 
				break;
			}
		}
	};

	private void showRegistrationID(){
		Toast.makeText(this, String.valueOf(registrationID + " is your registration ID"), Toast.LENGTH_LONG).show();
	}

	private void showPushNotification(String title, String message,String getURL){
		Preferences pref = new Preferences();
		String usePush = pref.getItemForStatic(ctx, "usePush");
		String pushType = pref.getItemForStatic(ctx, "pushType");

		PushNotification pn = new PushNotification(ctx);	
		pn.wakeLock();

		//TODO 만일 이미 앱이 실행된 경우 
		try{
			ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
			List<RunningTaskInfo> taskInfo = am.getRunningTasks(1);
			ComponentName topActivity=taskInfo.get(0).topActivity;
			String topactivityname=topActivity.getPackageName();
			Log.d(LOG_TAG,"top package name = " + topactivityname);
			Log.d(LOG_TAG,"cornerstone app package name = " + this.getPackageName());
			
			//현재 푸시를 받은 앱이 실행 중이 아니라면 
			if(!topactivityname.equals(this.getPackageName())){
				if(usePush.equals("true")){	
					if(pushType.equals("alert")){
						Intent i = new Intent(ctx, PushNotification.class);
						Bundle b = new Bundle();
						b.putString("title", title);
						b.putString("msg", message);
						b.putString("url",getURL);
						i.putExtras(b);
						i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						ctx.startActivity(i);
					}
					else { //banner or defalt
						pn.makePushNotification(title,message,getURL);
					}     
				}
				else{
					//do nothing
				}
			}
			//현재 푸쉬를 방은 앱이 실행중인 경우 
			else{
				if(usePush.equals("true")){	
					if(pushType.equals("alert")){
						Intent i = new Intent(ctx, PushNotification.class);
						Bundle b = new Bundle();
						b.putString("title", title);
						b.putString("msg", message);
						b.putString("url",getURL);
						i.putExtras(b);
						i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						ctx.startActivity(i);
					}
					else { //banner or defalt
						pn.makePushNotification(title,message,getURL);
					}     
				}
				else{
					//do nothing
				}
			}
		}
		catch(Exception e)
		{
		}

		
	}

	@Override
	protected void onRegistered(Context arg0, String arg1) {
		Log.d(LOG_TAG,"등록ID:"+arg1);
		ctx = arg0;
		//save registration id
		Preferences pref = new Preferences();
		pref.setItemForStaic(ctx, "registrationID", arg1);

		registrationID = arg1;
		mHandler.sendMessage(Message.obtain(null, NICELYREGISTRED));
	}

	//GCM에 해지하였을경우 발생하는 메소드
	@Override
	protected void onUnregistered(Context arg0, String arg1) {
		Log.d(LOG_TAG,"해지ID:"+arg1);
	}

	//GCM이 메시지를 보내왔을때 발생하는 메소드
	@Override
	protected void onMessage(Context context, Intent intent) {
		ctx = context;
		// TODO Auto-generated method stub
		Log.d(LOG_TAG, "GCMReceiver Message");


		try {
			title = intent.getStringExtra("title");
			message = intent.getStringExtra("message");
			getURL = intent.getStringExtra("getURL");

			Log.d(LOG_TAG, title + ":" + message);

			mHandler.sendMessage(Message.obtain(null, NICELYDONE));

		} catch (Exception e) {
			Log.e(LOG_TAG, "[onMessage] Exception : " + e.getMessage());
		}
	}

	//오류를 핸들링하는 메소드
	@Override
	protected void onError(Context arg0, String arg1) {
		Log.d(LOG_TAG,arg1); 
	}

}
