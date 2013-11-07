package org.skt.runtime.push;

import java.util.Timer;
import java.util.TimerTask;

import org.skt.runtime.R;
import org.skt.runtime.RuntimeStandAlone;
import org.skt.runtime.UriReceiver;
import org.skt.runtime.UriReceiver.UriData;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

public class PushNotification extends Activity{
	private NotificationManager nm;
	private Context ctx;

	private String LOGTAG = "PushNotification";

	private static PowerManager.WakeLock sCpuWakeLock;

	private ImageView imageAnim = null;
	private AnimationDrawable animation = null;

	public PushNotification(){
		super();
	}

	public PushNotification(Context ctx) {
		super();
		this.ctx = ctx;
	}

	private String getURL;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

		String title, msg;
		Bundle bun = getIntent().getExtras();
		title = bun.getString("title");
		msg = bun.getString("msg");
		getURL = bun.getString("url");

		AlertDialog.Builder alertDialog = new AlertDialog.Builder(PushNotification.this);

		alertDialog.setPositiveButton("닫기", new AlertDialog.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				//PushWakeLock.releaseCpuLock();
				PushNotification.this.finish();
			}
		});

		alertDialog.setNegativeButton("보기", new AlertDialog.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				//KeyguardManager km = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
				//KeyguardManager.KeyguardLock keyLock = km.newKeyguardLock(KEYGUARD_SERVICE);
				//keyLock.disableKeyguard(); //순정 락스크린 해제

				//Intent loadintent = new Intent(PushNotification.this.getApplicationContext(), RuntimeStandAlone.class);
				Intent loadintent = new Intent("skt.cornerstone.runtime.push");
				if(getURL != null) loadintent.putExtra(UriReceiver.FLAG_DATA, parseUriArgs(getURL));
				//loadintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

				startActivity(loadintent);
				PushNotification.this.finish();
			}
		});

		alertDialog.setIcon(R.drawable.pushicon);
		alertDialog.setTitle(title);
		alertDialog.setMessage(msg);
		//alertDialog.setView(getLayoutInflater().inflate(R.layout.customdialoglayout, null));
		alertDialog.show();

		//chisu test 
		//		AlertDialog dialog = alertDialog.create();
		//		dialog.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
		//		dialog.getWindow().setGravity(Gravity.BOTTOM);
		//		dialog.show();



		//		Dialog dialog = new Dialog(this);
		//		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		//		dialog.setContentView(R.layout.customdialoglayout);
		//		dialog.getWindow().setGravity(Gravity.TOP);
		//		dialog.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
		//		//dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		//		dialog.getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.filled_box));
		//		dialog.setOnDismissListener(new OnDismissListener() {
		//			public void onDismiss(DialogInterface dialog) {
		//				// TODO Auto-generated method stub
		//				PushNotification.this.finish();
		//			}
		//		});
		//		dialog.getWindow().findViewById(R.id.cmd_submit).setOnClickListener(this);
		//		dialog.show();
		//
		//		imageAnim =  (ImageView) dialog.getWindow().findViewById(R.id.image);

		//		animation = new AnimationDrawable();
		//		animation.addFrame(getResources().getDrawable(R.drawable.pushicon), 500);
		//		animation.addFrame(getResources().getDrawable(R.drawable.map), 500);
		//		animation.addFrame(getResources().getDrawable(R.drawable.default_video_poster), 500);
		//		animation.setOneShot(false);
		//		imageAnim.setBackgroundDrawable(animation);
		//		animation.start();




		//		AnimationSet rootSet= new AnimationSet(true);
		//		rootSet.setInterpolator(new AccelerateInterpolator(10.0f));
		//		
		//		TranslateAnimation trans = new TranslateAnimation(300.0f, 0.0f, 0.0f, 0.0f);
		//		trans.setDuration(1500L);
		//		trans.setStartOffset(0L);
		//		
		//		RotateAnimation rotate = new RotateAnimation(0f, 360f, 0.5f, 0.5f);
		//		rotate.setStartOffset(0L);
		//		rotate.setDuration(2000L);
		//		rotate.setRepeatCount(1);
		//		
		//		ScaleAnimation scale = new ScaleAnimation(0f, 1f, 0f, 1f, 0.5f, 0.5f);
		//		scale.setStartOffset(0L);
		//		scale.setDuration(2000L);
		//		scale.setFillBefore(true);
		//		
		//		AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
		//		alpha.setDuration(1500L);
		//		
		//		rootSet.addAnimation(trans);
		//		//rootSet.addAnimation(rotate);
		//		//rootSet.addAnimation(scale);
		//		rootSet.addAnimation(alpha);
		//		
		//		rootSet.setStartOffset(1000);
		//		
		//		imageAnim.startAnimation(rootSet);
		//



		//vibration operate
		Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(500);

		//
		Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(),RingtoneManager.TYPE_NOTIFICATION);
		Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
		ringtone.play();

		// 폰 설정의 조명시간을 가져와서 해당 시간만큼만 화면을 켠다.
		int defTimeOut = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 15000);
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				releaseCpuLock();
			}
		};

		Timer timer = new Timer();
		timer.schedule(task, defTimeOut);

	}


	public void makePushNotification(String title, String message, String getURL){

		nm = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);

		//Intent loadintent = new Intent(ctx, RuntimeStandAlone.class);
		Intent loadintent = new Intent("skt.cornerstone.runtime.push");
		if(getURL != null) loadintent.putExtra(UriReceiver.FLAG_DATA, parseUriArgs(getURL));
		//loadintent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent intent = PendingIntent.getActivity(ctx, 0,loadintent,PendingIntent.FLAG_UPDATE_CURRENT);

		long[] vi = {0,300};
		Bitmap bm = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.pushicon);

		Notification.Builder builder = new Notification.Builder(ctx);
		//상태바 이미지 및 노티 왼쪽 이미지 defaul 
		builder.setSmallIcon(R.drawable.pushicon);
		//노티 오른쪽 이미지 
		//builder.setLargeIcon(bm);
		builder.setTicker(message);
		builder.setWhen(System.currentTimeMillis());
		//builder.setNumber(10);
		builder.setContentTitle(title);
		builder.setContentText(message);
		builder.setContentIntent(intent);
		builder.setVibrate(vi);
		builder.setSound(RingtoneManager.getActualDefaultRingtoneUri(ctx,RingtoneManager.TYPE_NOTIFICATION));
		builder.setAutoCancel(true);

		//Notification notification = builder.build();//required API level 16 
		Notification notification = builder.getNotification();

		// Create Notification Object 
		nm.notify(1234, notification);
	}

	private UriData parseUriArgs(String path) {
		UriData data = new UriData();
		data.page = path;		
		return data;
	}

	public void wakeLock(){
		PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
		sCpuWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP |PowerManager.ON_AFTER_RELEASE,"SKT Runtime PUSH");
		sCpuWakeLock.acquire();
		//       PowerManager.SCREEN_BRIGHT_WAKE_LOCK
	}

	public void releaseCpuLock() {
		Log.e("PushWakeLock", "Releasing cpu wake lock");

		if (sCpuWakeLock != null) {
			sCpuWakeLock.release();
			sCpuWakeLock = null;
		}
	}

//	public void onClick(View v) {
//		int view = v.getId();
//		if(view == R.id.cmd_submit){		
//			Log.e(LOGTAG, "dialog button click");				
//		}
//	}
}
