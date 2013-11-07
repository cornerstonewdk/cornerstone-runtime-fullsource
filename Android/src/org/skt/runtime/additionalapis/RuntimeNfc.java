package org.skt.runtime.additionalapis;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.skt.runtime.RuntimeActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.util.Log;

public class RuntimeNfc implements CreateNdefMessageCallback {

	private RuntimeActivity ctx;
	
	public static int NFC_URL = 0;
	public static int NFC_LOCATION = 1;
	public static int NFC_MYNUMBER = 2;
	public static int NFC_CONTACTNUMBER = 3;
	public static int NFC_MESSAGE = 4;
	public static int NFC_IMAGE = 5;
	
	private int dataType = -1;
	
	private String data = null;
	private byte[] imagebytes = null; //only use to send image
	
	private MediaScanBroadCastReceiver mediascanrcv = null;
	
	public RuntimeNfc(RuntimeActivity ctx) {
		super();
		this.ctx = ctx;
	}

	public void setDataType(int type){
		this.dataType = type;
	}
	
	public void setTagData(String data){
		this.data = data;
	}
	
	public boolean setImageData(String imageurl){
		String afterUrl = makeImageFilename(imageurl);
		
		if(!checkFileSiseAndexists(afterUrl)){
			return false;
		}
		
		Bitmap testbitmap = BitmapFactory.decodeFile(afterUrl);
		
		if(testbitmap != null){
			imagebytes = bitmapToByteArray(testbitmap);	
			return true;
		}
		else 
			return false;
	}
	
	boolean checkFileSiseAndexists(String filepath){
		File imagefile = new File(filepath);
		
		if(!imagefile.exists())
			return false;
		
		long imagefilelength = imagefile.length();

		//1M over
		if(imagefilelength > 1000000){
			return false;
		}
		
		return true;
	}
	public NdefMessage createNdefMessage(NfcEvent event) {
		NdefMessage msg;
		
		if(this.dataType == -1){
			return null;
		}
		else if(this.dataType == NFC_IMAGE){
			msg = new NdefMessage(
					new NdefRecord[]{ createMimeRecord(
							"application/org.skt.runtime", imagebytes)});
		}
		else{
			msg = new NdefMessage(
					new NdefRecord[]{ createMimeRecord(
							"application/org.skt.runtime", data.getBytes())});
		}
		
		return msg;
	}
	
	/**
	 * 커스텀 MIME 타입을 캡슐화한 NDEF 레코드를 생성한다.
	 */
	public NdefRecord createMimeRecord(String mimeType, byte[] payload){
		byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
		NdefRecord mimeRecord = new NdefRecord(
				NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
		return mimeRecord;
	}
		
	/**
	 * 인텐트로 부터 NDEF Message를 파싱하고 TextView에 프린트한다.
	 */
	public String processIntent(Intent intent){
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
				NfcAdapter.EXTRA_NDEF_MESSAGES);
		// 오직 하나의 메시지가 빔을 통해 전달받는다.
		NdefMessage msg = (NdefMessage) rawMsgs[0];
		Log.e("NFC", new String(msg.getRecords()[0].getPayload()));
		
		String loaddata = new String(msg.getRecords()[0].getPayload());
		
		//TYPE GEO
		if(loaddata.contains("geo:")){
			Intent geointent = new Intent(Intent.ACTION_VIEW);
			geointent.setData(Uri.parse(loaddata));
           ctx.startActivity(geointent);
		}
		//TYPE URL (external url)
		else if(loaddata.contains("http://") || loaddata.contains("https://")){
			try {			
				Intent urlintent = new Intent(Intent.ACTION_VIEW);
				urlintent.setData(Uri.parse(loaddata));
				if(loaddata.contains("youtube"))
					urlintent.setPackage("com.google.android.youtube");
				ctx.startActivity(urlintent);
			} catch (android.content.ActivityNotFoundException e) {
				Log.e("NFC", "Error loading url "+ loaddata, e);
			}
		}
		//TYPE URL(internal URL)
		else if(loaddata.contains("file://")){
			ctx.loadUrl(loaddata);
		}
		//TYPE CONTACT
		else if(loaddata.contains("con:")){
			callAddContactActivirty(loaddata);
		}
		//TYPE IMAGE
		else {
			registerMediaScanRev();
			
			Bitmap bitmap = byteArrayToBitmap(msg.getRecords()[0].getPayload());
			
			String imgname = "mnt/sdcard/DCIM/" + System.currentTimeMillis() +".jpg";
			SaveBitmapToFileCache(bitmap, imgname);
			
			//[20120719][chisu]media scan first
			this.ctx.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
					Uri.parse("file://"
					+ Environment.getExternalStorageDirectory()))); 
			
			return imgname;	
		}
		
		return loaddata;
	}
	
	public void callAddContactActivirty(String contactInfo){
		
		String[] contact = contactInfo.split(":");
		String name = contact[1];
		String number = contact[2];
		
		Intent intent = new Intent(Intent.ACTION_INSERT);
		intent.setType(ContactsContract.Contacts.CONTENT_TYPE);

		intent.putExtra(ContactsContract.Intents.Insert.NAME, name);
		intent.putExtra(ContactsContract.Intents.Insert.PHONE, number);
		
		this.ctx.startActivity(intent);
	}
	
	//UTIL function below
	
	//[20120919][chisu]support file:// and file:///
	private String makeImageFilename(String beforeStr){
		if((beforeStr.contains("file:///mnt/sdcard")))
			return beforeStr.replace("file:///mnt/sdcard","/sdcard");
		else if((beforeStr.contains("file://mnt/sdcard")))
			return beforeStr.replace("file://mnt/sdcard","/sdcard");
		else if((beforeStr.contains("file:///")))
			return beforeStr.replace("file:///","/");
		else if(beforeStr.contains("file://"))
			return beforeStr.replace("file://","/");		
		else
			return beforeStr;
	}
		
	public byte[] bitmapToByteArray( Bitmap bitmap ) {  
        ByteArrayOutputStream stream = new ByteArrayOutputStream() ;  
        bitmap.compress( CompressFormat.JPEG, 50, stream) ;  
        byte[] byteArray = stream.toByteArray() ;  
        return byteArray ;  
    }  
	
	public Bitmap byteArrayToBitmap( byte[] $byteArray ) {  
	    Bitmap bitmap = BitmapFactory.decodeByteArray( $byteArray, 0, $byteArray.length ) ;  
	    return bitmap ;  
	} 
	
	private void SaveBitmapToFileCache(Bitmap bitmap, String strFilePath)
	{
		File fileCacheItem = new File(strFilePath);
		OutputStream out = null;

		try
		{
			fileCacheItem.createNewFile();
			out = new FileOutputStream(fileCacheItem);

			bitmap.compress(CompressFormat.JPEG, 50, out);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				out.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void registerMediaScanRev(){
		mediascanrcv = new MediaScanBroadCastReceiver();
		IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
//		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
//		filter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
//		filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
//		filter.addAction(Intent.ACTION_MEDIA_EJECT);
		filter.addDataScheme("file");

		ctx.registerReceiver(mediascanrcv, filter);
	}
	
	public class MediaScanBroadCastReceiver extends BroadcastReceiver{

	    @Override
	    public void onReceive(Context context, Intent intent) {

	    	if(intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)){
				Uri uri = Uri.parse("content://media/external/images/media");
	            Intent resultintent = new Intent(Intent.ACTION_VIEW, uri);
	            
	            ctx.unregisterReceiver(mediascanrcv);
	            
	            try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            ctx.startActivity(resultintent);
	            
			}
	    }
	}
	
}
