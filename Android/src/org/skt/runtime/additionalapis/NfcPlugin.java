package org.skt.runtime.additionalapis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skt.runtime.RuntimeActivity;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;
import org.skt.runtime.api.RuntimeInterface;
import org.skt.runtime.html5apis.DeviceAPIErrors;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.database.Cursor;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

public class NfcPlugin extends Plugin{

	private static final String LOG_TAG = "NFC";

	private RuntimeNfc nfc = null;
	private NfcAdapter mNfcAdapter = null;
	private String callbackId;                      // The ID of the callback to be invoked with our result
	private int CONTACT_PICKER_RESULT = 100;

	private String phoneNumber = null;

	private IntentFilter[] intentFiltersArray;
	private String[][] techListsArray;
	private PendingIntent pendingIntent;

	private Activity ctxActivity;

	private boolean nowNFCUse = false;
	private String readTagCallbackID;

	private byte[] transceiveMessage = null;

	private ArrayList<byte[]> transceiveMessagList = new ArrayList<byte[]>();

	private Tag tag = null;

	@Override
	public void setContext(RuntimeInterface ctx) {
		// TODO Auto-generated method stub
		super.setContext(ctx);
		ctxActivity = (Activity)ctx;
		nfc = new RuntimeNfc((RuntimeActivity)ctx);
		this.setNFCUse(false);
	}

	private ArrayList<byte[]> maketransceiveMessagArray(JSONArray args){
		JSONArray messageArray = args.optJSONArray(0);

		if(messageArray!= null){
			for(int i = 0 ; i < messageArray.length() ; i ++){
				transceiveMessagList.add(hexToByteArray(messageArray.optString(i)));
			}
		}

		return transceiveMessagList;
	}
	@Override
	public PluginResult execute(String action, JSONArray args, String callbackId) {
		PluginResult.Status status = PluginResult.Status.NO_RESULT;
		String message = "";
		PluginResult result = new PluginResult(status, message);

		this.callbackId = callbackId;

		if(action.equals("isNFCSupport")){
			boolean support = checkNFCSupport();
			result = new PluginResult(PluginResult.Status.OK, support);
			return result;
		}
		else if(action.equals("setNFCUse")){
			setNFCUse(args.optBoolean(0));
			return result;
		}
		else if(action.equals("transceive")){

			if(!nowNFCUse){
				result = new PluginResult(PluginResult.Status.ERROR,"Set NFCUse True!!");
				return result;
			}
			
			if(args.optJSONArray(0) != null){
				transceiveMessagList.clear();
				transceiveMessagList  = maketransceiveMessagArray(args);

				JSONObject retval = transceive(transceiveMessagList);

				if(retval != null){
					result = new PluginResult(PluginResult.Status.OK,retval);	
					return result;
				}
				else{
					result = new PluginResult(PluginResult.Status.ERROR);
					return result;
				}

			}
			else{
				result = new PluginResult(PluginResult.Status.ERROR);
				return result;
			}
		}
		else if(action.equals("setReadTagCallback")){
			readTagCallbackID = callbackId;
			result.setKeepCallback(true);
			return result;
		}
		else if(action.equals("tagclose")){
			tagClose();
			result = new PluginResult(PluginResult.Status.OK);
			return result;
		}
		else if(action.equals("clearReadTagCallback")){
			result = new PluginResult(PluginResult.Status.OK);
			result.setKeepCallback(false);
			this.success(result, this.readTagCallbackID);	
		}
		else if(action.equals("setTransceiveMessage")){
			if(!args.optString(0).equals("null")){
				transceiveMessage  = hexToByteArray(args.optString(0));
				result = new PluginResult(PluginResult.Status.OK);
				return result;
			}
			else{
				result = new PluginResult(PluginResult.Status.ERROR);
				return result;
			}

		}
		else if (action.equals("maketaginfo")) {
			try {

				if(args.optInt(0) == RuntimeNfc.NFC_URL){

					nfc.setDataType(RuntimeNfc.NFC_URL);

					String url = args.optString(1);
					if(!url.equals("null")){
						nfc.setTagData(url);
					}
					else{
						nfc.setTagData("file:///android_asset/www/index.html");
					}

					return new PluginResult(PluginResult.Status.OK);

				}
				else if(args.optInt(0) == RuntimeNfc.NFC_LOCATION){
					nfc.setDataType(RuntimeNfc.NFC_LOCATION);
					setLocation(args.optJSONObject(1));
					return new PluginResult(PluginResult.Status.OK);
				}
				else if(args.optInt(0) == RuntimeNfc.NFC_MYNUMBER){

					return new PluginResult(PluginResult.Status.OK);
				}
				else if(args.optInt(0) == RuntimeNfc.NFC_CONTACTNUMBER){
					nfc.setDataType(RuntimeNfc.NFC_CONTACTNUMBER);
					result.setKeepCallback(true);
					pickContact();
				}
				else if(args.optInt(0) == RuntimeNfc.NFC_MESSAGE){
					return new PluginResult(PluginResult.Status.OK);
				}
				else if(args.optInt(0) == RuntimeNfc.NFC_IMAGE){

					String imageurl = args.optString(1);
					if(!imageurl.equals("null")){	
						boolean returnval = nfc.setImageData(imageurl);

						if(returnval){
							nfc.setDataType(RuntimeNfc.NFC_IMAGE);
							return new PluginResult(PluginResult.Status.OK);
						}
						else
							return new PluginResult(PluginResult.Status.ERROR, 
									createErrorObject(DeviceAPIErrors.NOT_SUPPORTED_ERR, "There is no file or file size is over 1000KB!"));	
					}
					else{
						return new PluginResult(PluginResult.Status.ERROR, 
								createErrorObject(DeviceAPIErrors.NOT_FOUND_ERR, "1st arg is need!"));						
					}
				}
				else{					
					return new PluginResult(PluginResult.Status.ERROR, 
							createErrorObject(DeviceAPIErrors.NOT_FOUND_ERR, "There is no type."));
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return new PluginResult(PluginResult.Status.JSON_EXCEPTION,createErrorObject(DeviceAPIErrors.UNKNOWN_ERR,"exception is happened!"));
			}		
		}

		return result;
	}


	@Override
	public void onPause(boolean multitasking) {
		// TODO Auto-generated method stub
		super.onPause(multitasking);

		if(nowNFCUse){
			stopNFC();
		}
	}

	@Override
	public void onResume(boolean multitasking) {
		// TODO Auto-generated method stub
		super.onResume(multitasking);

		if(nowNFCUse){
			startNFC();
		}
	}

	public void processTagParse(Intent intent){

		//[20121115][chisu]to support nfc
		if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(ctxActivity.getIntent().getAction())){

			//Test 
			//			String returndata = nfc.processIntent(ctxActivity.getIntent());
			//			PluginResult result = null;;
			//			try {
			//				result = new PluginResult(PluginResult.Status.OK,new JSONObject().put("tag", returndata));
			//			} catch (JSONException e) {
			//				// TODO Auto-generated catch block
			//				e.printStackTrace();
			//			}
			//			result.setKeepCallback(true);
			//			this.success(result, this.readTagCallbackID);
			//			return;

			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			Parcelable[] messages = intent.getParcelableArrayExtra((NfcAdapter.EXTRA_NDEF_MESSAGES));

			PluginResult result = null;
			JSONObject tagobj = new JSONObject();

			Ndef ndef = Ndef.get(tag);
			tagobj = ndefToJSON(ndef);

			result = new PluginResult(PluginResult.Status.OK,tagobj);
			result.setKeepCallback(true);
			this.success(result, this.readTagCallbackID);
			return;
		}
		else if(NfcAdapter.ACTION_TECH_DISCOVERED.equals(ctxActivity.getIntent().getAction())){

			Log.e("NFC", "ACTION_TECH_DISCOVERED");

			//Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

			Parcelable[] messages = intent.getParcelableArrayExtra((NfcAdapter.EXTRA_NDEF_MESSAGES));

			PluginResult result = null;
			JSONObject tagobj = new JSONObject();

			//Step 3. IsoDep
			for (String tagTech : tag.getTechList()) {
				if(tagTech.equals(IsoDep.class.getName())){

					tagobj = parseIsoDep(tag);	

					if(tagobj != null){
						result = new PluginResult(PluginResult.Status.OK,tagobj);
						result.setKeepCallback(true);
						this.success(result, this.readTagCallbackID);
						return;	
					}
					else{
						//NO ACTION
						return ;
					}
				}
			}

			//Step 1. NfcA 
			for (String tagTech : tag.getTechList()) {

				Log.d(LOG_TAG, tagTech);

				if(tagTech.equals(NfcA.class.getName())){

					tagobj = parseNfcA(tag);	

					if(tagobj != null){
						result = new PluginResult(PluginResult.Status.OK,tagobj);
						result.setKeepCallback(true);
						this.success(result, this.readTagCallbackID);
						return;
					}
					else{
						//NO ACTION
						return ;
					}

				}
			}

			//Step 2. NfcB
			for (String tagTech : tag.getTechList()) {
				if(tagTech.equals(NfcB.class.getName())){

					tagobj = parseNfcB(tag);	

					result = new PluginResult(PluginResult.Status.OK,tagobj);
					result.setKeepCallback(true);
					this.success(result, this.readTagCallbackID);
					return;
				}
			}

			//Step 4: NdefFormatable
			for (String tagTech : tag.getTechList()) {
				if (tagTech.equals(NdefFormatable.class.getName())) {

				}
			}

			//Step 5 : Ndef
			for (String tagTech : tag.getTechList()) {
				if (tagTech.equals(Ndef.class.getName())) {

					Ndef ndef = Ndef.get(tag);
					tagobj = ndefToJSON(ndef);

					result = new PluginResult(PluginResult.Status.OK,tagobj);
					result.setKeepCallback(true);
					this.success(result, this.readTagCallbackID);
					return;
				}
			}

		}
	}

	private JSONObject ndefToJSON(Ndef ndef) {
		JSONObject json = new JSONObject();

		if (ndef != null) {
			try {
				json.put("tag", ndef.getTag());
				json.put("type", "ndef");
				json.put("tagtype", ndef.getType());
				json.put("maxSize", ndef.getMaxSize());
				json.put("isWritable", ndef.isWritable());
				json.put("ndefMessage", messageToJSON(ndef.getCachedNdefMessage()));
				try {
					json.put("canMakeReadOnly", ndef.canMakeReadOnly());
				} catch (NullPointerException e) {
					json.put("canMakeReadOnly", null);
				}
			} catch (JSONException e) {
				Log.e(LOG_TAG, "Failed to convert ndef into json: " + ndef.toString(), e);
			}
		}
		return json;
	}

	private JSONArray messageToJSON(NdefMessage message) {
		if (message == null) {
			return null;
		}

		List<JSONObject> list = new ArrayList<JSONObject>();

		for (NdefRecord ndefRecord : message.getRecords()) {
			list.add(recordToJSON(ndefRecord));
		}

		return new JSONArray(list);
	}

	private JSONObject recordToJSON(NdefRecord record) {
		JSONObject json = new JSONObject();
		try {
			json.put("tnf", record.getTnf());
			json.put("type", byteArrayToJSON(record.getType()));
			json.put("id", byteArrayToJSON(record.getId()));
			json.put("payload", byteArrayToJSON(record.getPayload()));
		} catch (JSONException e) {
			Log.e(LOG_TAG, "Failed to convert ndef record into json: " + record.toString(), e);
		}
		return json;
	}

	private JSONArray byteArrayToJSON(byte[] bytes) {
		JSONArray json = new JSONArray();
		for (byte aByte : bytes) {
			json.put(aByte);
		}
		return json;
	}

	private byte[] jsonToByteArray(JSONArray json) throws JSONException {
		byte[] b = new byte[json.length()];
		for (int i = 0; i < json.length(); i++) {
			b[i] = (byte) json.getInt(i);
		}
		return b;
	}

	private JSONObject transceive(ArrayList<byte[]> writedata){
		//PluginResult result = null;
		if(!nowNFCUse)
			return null;
		
		JSONObject tagobj = new JSONObject();

		if(tag != null){

			//Step 3. IsoDep
			for (String tagTech : tag.getTechList()) {
				if(tagTech.equals(IsoDep.class.getName())){

					//tagobj = parseIsoDep(tag);
					tagobj = transceiveIsoDep(tag);

					if(tagobj != null){						
						return tagobj;
					}
					else{
						//NOACTION
						return null;
					}


				}
			}

			//Step 1. NfcA 
			for (String tagTech : tag.getTechList()) {

				Log.d(LOG_TAG, tagTech);

				if(tagTech.equals(NfcA.class.getName())){

					tagobj = transceiveNfcA(tag);	

					if(tagobj != null){					
						return tagobj;
					}
					else{
						//NOACTION
						return null;
					}

				}
			}

			//Step 2. NfcB
			for (String tagTech : tag.getTechList()) {
				if(tagTech.equals(NfcB.class.getName())){

					tagobj = transceiveNfcB(tag);	

					if(tagobj != null){

						return tagobj;
					}
					else{
						return null;
					}
				}
			}
		}

		return null;
	}

	private JSONObject parseNfcA(Tag tag){

		JSONObject tagObject = new JSONObject();

		NfcA nfca = NfcA.get(tag);

		boolean exceptionHappen = false;

		try{
			nfca.connect();

			try {
				tagObject.put("tag", tag);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			byte[] atqa = nfca.getAtqa();
			int maxTransceivelength = nfca.getMaxTransceiveLength();
			short sak = nfca.getSak();
			byte[] transceive = null;
			if(transceiveMessage != null)
				transceive = nfca.transceive(transceiveMessage); 
			int timeout = nfca.getTimeout();

			try {
				tagObject.put("type", "NfcA");
				tagObject.put("atqa",byteArrayToJSON(atqa));
				tagObject.put("maxTransceivelength", maxTransceivelength);

				if(transceive != null)
					tagObject.put("transceive",byteArrayToJSON(transceive));

				tagObject.put("sak",sak);
				tagObject.put("timeout",timeout);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}catch(IOException e){
			Log.e(LOG_TAG,"IOException while writing NfcA message...", e);
			exceptionHappen = true;
		}finally{
			if(nfca != null){
				try{
					nfca.close();
					//exception happen
					if(exceptionHappen== true)
						return null;
				}
				catch(IOException e){
					Log.e(LOG_TAG,"Error closing tag...", e);
				}
			}
		}

		return tagObject;
	}

	private JSONObject parseNfcB(Tag tag){

		JSONObject tagObject = new JSONObject();

		try {
			tagObject.put("tag", tag);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		NfcB nfcb = NfcB.get(tag);

		try{
			nfcb.connect();

			byte[] applicationData = nfcb.getApplicationData();
			int maxTransceivelength = nfcb.getMaxTransceiveLength();
			byte[] protocallInfo = nfcb.getProtocolInfo();
			byte[] transceive = null;
			if(transceiveMessage != null)
				transceive = nfcb.transceive(transceiveMessage);

			try {
				tagObject.put("type", "NfcB");
				tagObject.put("applicationData",byteArrayToJSON(applicationData));
				tagObject.put("maxTransceivelength",maxTransceivelength);
				tagObject.put("protocallInfo",byteArrayToJSON(protocallInfo));
				if(transceive != null)
					tagObject.put("transceive",byteArrayToJSON(transceive));

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}catch(IOException e){
			Log.e(LOG_TAG,"IOException while writing NfcB message...", e);
		}finally{
			if(nfcb != null){
				try{
					nfcb.close();
				}
				catch(IOException e){
					Log.e(LOG_TAG,"Error closing tag...", e);
				}
			}
		}

		return tagObject;
	}

	private boolean tagConnected = false;
	private String currentFormat = null;

	private void tagClose(){
		tagConnected = false;

		if(currentFormat.equals("IsoDep")){
			IsoDep isoDep = IsoDep.get(tag);

			try {
				isoDep.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if(currentFormat.equals("NfcA")){
			NfcA nfca = NfcA.get(tag);

			try {
				nfca.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		else if(currentFormat.equals("NfcB")){
			NfcB nfcb = NfcB.get(tag);

			try {
				nfcb.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	private JSONObject transceiveNfcB(Tag tag){

		JSONObject tagObject = new JSONObject();

		NfcB nfcb = NfcB.get(tag);
		currentFormat = "NfcB";

		try{
			if(tagConnected == false){
				nfcb.connect();
				tagConnected = true;
			}

			try {
				tagObject.put("tag", tag);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			try {
				tagObject.put("type", "NfcB");

				JSONArray transceiveArray = new JSONArray();

				if(transceiveMessagList != null){

					for(int i = 0 ; i < transceiveMessagList.size() ; i ++){
						String returnmeg = byteArrayToHex(nfcb.transceive(transceiveMessagList.get(i)));
						transceiveArray.put(returnmeg);
					}

					tagObject.put("transceive", transceiveArray);
				}

				transceiveMessagList.clear();

			}
			catch(IOException e){//태그와 연결되지 않았을 경우 발생 
				PluginResult cr = new PluginResult(PluginResult.Status.ERROR, "Please contact Device to the tag!");
				ctx.sendJavascript(cr.toErrorCallbackString(callbackId));

				tagClose();
			}
			catch(IllegalStateException e){//close가 되지 않았는데 connect를 시도할 경우 발생 
				tagClose();
				transceiveIsoDep(tag);

			}
			catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}catch(IOException e){
			Log.e(LOG_TAG,"IOException while writing NfcB message...", e);
		}finally{

		}

		return tagObject;
	}
	
	private JSONObject transceiveNfcA(Tag tag){

		JSONObject tagObject = new JSONObject();

		NfcA nfca = NfcA.get(tag);
		currentFormat = "NfcA";

		try{
			if(tagConnected == false){
				nfca.connect();
				tagConnected = true;
			}

			try {
				tagObject.put("tag", tag);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			try {
				tagObject.put("type", "NfcA");

				JSONArray transceiveArray = new JSONArray();

				if(transceiveMessagList != null){

					for(int i = 0 ; i < transceiveMessagList.size() ; i ++){
						String returnmeg = byteArrayToHex(nfca.transceive(transceiveMessagList.get(i)));
						transceiveArray.put(returnmeg);
					}

					tagObject.put("transceive", transceiveArray);
				}

				transceiveMessagList.clear();

			}
			catch(IOException e){//태그와 연결되지 않았을 경우 발생 
				PluginResult cr = new PluginResult(PluginResult.Status.ERROR, "Please contact Device to the tag!");
				ctx.sendJavascript(cr.toErrorCallbackString(callbackId));

				tagClose();
			}
			catch(IllegalStateException e){//close가 되지 않았는데 connect를 시도할 경우 발생 
				tagClose();
				transceiveIsoDep(tag);

			}
			catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}catch(IOException e){
			Log.e(LOG_TAG,"IOException while writing NfcA message...", e);
		}finally{

		}

		return tagObject;
	}

	private JSONObject transceiveIsoDep(Tag tag){

		JSONObject tagObject = new JSONObject();

		try {
			tagObject.put("tag", tag);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		IsoDep isoDep = IsoDep.get(tag);
		currentFormat = "IsoDep";

		try{
			if(tagConnected == false){
				isoDep.connect();
				tagConnected = true;
			}

			try {
				tagObject.put("type", "IsoDep");

				JSONArray transceiveArray = new JSONArray();

				if(transceiveMessagList != null){

					for(int i = 0 ; i < transceiveMessagList.size() ; i ++){
						String returnmeg = byteArrayToHex(isoDep.transceive(transceiveMessagList.get(i)));
						returnmeg = returnmeg.toUpperCase();
						transceiveArray.put(returnmeg);
					}

					tagObject.put("transceive", transceiveArray);
				}

				transceiveMessagList.clear();

			}
			catch(IOException e){//태그와 연결되지 않았을 경우 발생 
				PluginResult cr = new PluginResult(PluginResult.Status.ERROR, "Please contact Device to the tag!");
				ctx.sendJavascript(cr.toErrorCallbackString(callbackId));

				tagClose();
			}
			catch(IllegalStateException e){//close가 되지 않았는데 connect를 시도할 경우 발생 
				//PluginResult cr = new PluginResult(PluginResult.Status.ERROR, "Please close connection first!");
				//ctx.sendJavascript(cr.toErrorCallbackString(callbackId));
				tagClose();
				return transceiveIsoDep(tag);

			}
			catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}catch(IOException e){
			Log.e(LOG_TAG,"IOException while writing IsoDep message...", e);
		}finally{

		}

		return tagObject;
	}

	private JSONObject parseIsoDep(Tag tag){

		JSONObject tagObject = new JSONObject();

		try {
			tagObject.put("tag", tag);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		IsoDep isoDep = IsoDep.get(tag);

		try{
			isoDep.connect();

			byte[] hiLayerResponse = isoDep.getHiLayerResponse();
			int maxTransceivelength = isoDep.getMaxTransceiveLength();
			byte[] historicalBytes = isoDep.getHistoricalBytes();

			try {
				tagObject.put("type", "IsoDep");
				if(hiLayerResponse != null)
					tagObject.put("hiLayerResponse",byteArrayToJSON(hiLayerResponse));
				tagObject.put("maxTransceivelength",maxTransceivelength);
				if(historicalBytes != null)
					tagObject.put("historicalBytes",byteArrayToJSON(historicalBytes));

				//				JSONArray transceiveArray = new JSONArray();
				//				
				//				if(transceiveMessagList.size() != 0){
				//					
				//					for(int i = 0 ; i < transceiveMessagList.size() ; i ++){
				//						String returnmeg = byteArrayToHex(isoDep.transceive(transceiveMessagList.get(i)));
				//						transceiveArray.put(returnmeg);
				//					}
				//					
				//					tagObject.put("transceive", transceiveArray);
				//				}
				//				
				//				transceiveMessagList.clear();

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}catch(IOException e){
			Log.e(LOG_TAG,"IOException while writing IsoDep message...", e);
		}finally{
			if(isoDep != null){
				try{
					isoDep.close();
				}
				catch(IOException e){
					Log.e(LOG_TAG,"Error closing tag...", e);
				}
			}
		}

		return tagObject;
	}

	// hex to byte[]
	public byte[] hexToByteArray(String hex) {
		if (hex == null || hex.length() == 0) {
			return null;
		}

		byte[] ba = new byte[hex.length() / 2];
		for (int i = 0; i < ba.length; i++) {
			ba[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}
		return ba;
	}

	// byte[] to hex
	public String byteArrayToHex(byte[] ba) {
		if (ba == null || ba.length == 0) {
			return null;
		}

		StringBuffer sb = new StringBuffer(ba.length * 2);
		String hexNumber;
		for (int x = 0; x < ba.length; x++) {
			hexNumber = "0" + Integer.toHexString(0xff & ba[x]);

			sb.append(hexNumber.substring(hexNumber.length() - 2));
		}
		return sb.toString();
	} 

	@Override
	public void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
		ctxActivity.setIntent(intent);
		processTagParse(intent);
	}

	//[20121115][chisu]to support NFC
	public boolean checkNFCSupport(){

		// NFC Adapter가 유효한지 체크
		mNfcAdapter =NfcAdapter.getDefaultAdapter(ctx.getContext());
		if(mNfcAdapter == null){
			Toast.makeText(ctx.getContext(),"NFC is not available",Toast.LENGTH_LONG).show();
			return false;
		}

		return true;  	

	}

	private void startNFC(){

		// NFC Adapter가 유효한지 체크
		mNfcAdapter =NfcAdapter.getDefaultAdapter(ctx.getContext());

		if(mNfcAdapter != null){
			//device가 발신 대상일때의 콜백을 등록 
			mNfcAdapter.setNdefPushMessageCallback(nfc,(Activity)ctx);

			//device가 수신대상일 때 콜백을 등록 
			pendingIntent = PendingIntent.getActivity(
					ctxActivity, 0, new Intent(ctxActivity, ctxActivity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP| Intent.FLAG_ACTIVITY_CLEAR_TOP), 0);

			IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
			//IntentFilter ndeftag = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
			try {
				ndef.addDataType("*/*");    /* Handles all MIME based dispatches.
                                           You should specify only the ones that you need. */
			}
			catch (MalformedMimeTypeException e) {
				throw new RuntimeException("fail", e);
			}

			intentFiltersArray = new IntentFilter[] {ndef, };
			techListsArray = new String[][]{new String[]{NfcA.class.getName()}};

			ctxActivity.runOnUiThread(new Runnable() {

				public void run() {
					// TODO Auto-generated method stub
					mNfcAdapter.enableForegroundDispatch
					(ctxActivity, pendingIntent, intentFiltersArray, techListsArray);
				}
			});

		}
	}

	private void stopNFC(){
		// NFC Adapter가 유효한지 체크
		mNfcAdapter =NfcAdapter.getDefaultAdapter(ctx.getContext());

		if(mNfcAdapter != null){
			ctxActivity.runOnUiThread(new Runnable() {
				public void run() {
					// TODO Auto-generated method stub
					mNfcAdapter.disableForegroundDispatch(ctxActivity);    		
				}
			});
		}
	}

	public void setNFCUse(boolean use){
		nowNFCUse = use;

		if(use){
			startNFC();
		}
		else{
			//clear watch
			if(readTagCallbackID != null){
				PluginResult result = new PluginResult(PluginResult.Status.OK);
				result.setKeepCallback(false);
				this.success(result, this.readTagCallbackID);
			}
			stopNFC();    		
		}
	}

	public void setLocation(JSONObject location){	
		String latitude = location.optString("latitude");
		String longitude = location.optString("longitude");

		nfc.setTagData("geo:"+latitude+","+longitude);

	}
	public void pickContact(){

		Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,ContactsContract.CommonDataKinds.Phone.CONTENT_URI); 
		this.ctx.startActivityForResult((Plugin) this, contactPickerIntent, CONTACT_PICKER_RESULT);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// TODO Auto-generated method stub
		if (resultCode == Activity.RESULT_OK) {
			//if contact get
			if(requestCode == CONTACT_PICKER_RESULT){
				Cursor cursor = ctx.getContentResolver().query(intent.getData(), 
						new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, 
					ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);

				cursor.moveToFirst();
				Log.e(LOG_TAG,cursor.getString(0));        //이름 얻어오기
				Log.e(LOG_TAG,cursor.getString(1));     //번호 얻어오기

				phoneNumber = "con:" + cursor.getString(0)+ ":" + cursor.getString(1);
				cursor.close(); 

				nfc.setTagData(phoneNumber);

				PluginResult result = new PluginResult(PluginResult.Status.OK);
				result.setKeepCallback(false);
				this.success(result, this.callbackId);	
			}

		} else {  
			// gracefully handle failure  
			Log.w(LOG_TAG, "Warning: activity result not ok");  
		}  
		super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override
	public boolean isSynch(String action) {
		// TODO Auto-generated method stub
		if(action.equals("isNFCSupport")){
			return true;
		}
		//		if(action.equals("transceive")){
		//			return true;
		//		}
		return super.isSynch(action);
	}



}
