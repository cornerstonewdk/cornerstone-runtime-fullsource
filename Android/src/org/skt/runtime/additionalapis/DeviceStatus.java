package org.skt.runtime.additionalapis;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;
import org.skt.runtime.api.RuntimeInterface;
import org.skt.runtime.html5apis.DeviceAPIErrors;

import android.content.Context;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;


public class DeviceStatus extends Plugin {

	private static final String LOG_TAG = "DeviceStatus";
	private String callbackId;                      // The ID of the callback to be invoked with our result

	private HashMap<String, Boolean> aspectSupported = new HashMap<String, Boolean>();

	private HashMap<String, Boolean> cellularSupported = new HashMap<String, Boolean>();
	private HashMap<String, Boolean> deviceSupported = new HashMap<String, Boolean>();
	private HashMap<String, Boolean> osSupported = new HashMap<String, Boolean>();
	private HashMap<String, Boolean> runtimeSupported = new HashMap<String, Boolean>();
	private HashMap<String, Boolean> wifiSupported = new HashMap<String, Boolean>();

	/*
	private String[] aspectArray = {"CellularNetwork", "Device" , "OperatingSystem" , "Runtime" , "WiFiNetwork"};

	private String[] cellularProperty = {"isInRoaming" , "signalStrength","operatorName","mcc", "mnc"};
	private String[] deviceProperty = {"imei", "model", "vendor","imsi","version","platform"};
	private String[] OSProperty = {"language","version", "name", "vendor"};
	private String[] RuntimeProperty = {"version","name","vendor"};
	private String[] WifiProperty = {"ssid","signalStrength","networkStatus"};
	 
	 */
	//[20120814][chisu]use to get cell signal
	TelephonyManager telephonyManager = null;
	WifiManager wifiManager = null;
	
	private int celsignal = 0 ;
	private WifiInfo wifiInfo;
	
	public final static String RUNTIME_VERSION = "1.0"; 
	public final static String RUNTIME_NAME = "SKT HTML5 Runtime";
	public final static String RUNTIME_VENDOR = "SK Telecom";

	public final static String STATUS_CONNECTED = "connected";
	public final static String STATUS_AVAILABLE = "available";
	public final static String STATUS_FORBIDDEN = "forbidden";
	
	/**
	 * Sets the context of the Command. This can then be used to do things like
	 * get file paths associated with the Activity.
	 * 
	 * @param ctx The context of the main Activity.
	 */
	public void setContext(RuntimeInterface ctx) {
		super.setContext(ctx);
		setSupportvalue();
		
		telephonyManager = (TelephonyManager)ctx.getSystemService(Context.TELEPHONY_SERVICE);
		wifiManager = (WifiManager)ctx.getSystemService(Context.WIFI_SERVICE);
		
		startWatchCellSignal();
		
	}

	@Override
	public PluginResult execute(String action, JSONArray args, String callbackId) {
		PluginResult.Status status = PluginResult.Status.OK;
		this.callbackId = callbackId;

		if (action.equals("isSupported")) {
			boolean supported = getPropertySupport(args);
			return new PluginResult(status, supported);
		}
		else if(action.equals("getPropertyValue")){
			JSONObject value = getProperty(args);
			if(value != null)
				return new PluginResult(status,value);
			else 
				return new PluginResult(PluginResult.Status.ERROR,createErrorObject(DeviceAPIErrors.NOT_SUPPORTED_ERR, "UNSUPPORTED"));
		}

		PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
		return r;
	}
	
	/**
	 * Send error message to JavaScript.
	 * 
	 * @param err
	 */
	public void fail(JSONObject err) {
		this.error(new PluginResult(PluginResult.Status.ERROR, err), this.callbackId);
	}
	

	/**
	 * Identifies if action to be executed returns a value and should be run synchronously.
	 * 
	 * @param action    The action to execute
	 * @return          T=returns value
	 */
	public boolean isSynch(String action) {
		if (action.equals("isSupported")) {
			return true;
		}
		return false;
	}
	

	public void setSupportvalue(){	
		aspectSupported.put("CellularNetwork", true);
		aspectSupported.put("Device", true);
		aspectSupported.put("OperatingSystem", true);
		aspectSupported.put("Runtime", true);
		aspectSupported.put("WiFiNetwork", true);

		cellularSupported.put("isInRoaming",true);
		cellularSupported.put("signalStrength",true);
		cellularSupported.put("operatorName",true);
		cellularSupported.put("mcc",true);
		cellularSupported.put("mnc",true);

		deviceSupported.put("imei",true);
		deviceSupported.put("model",true);
		deviceSupported.put("vendor",true);
		deviceSupported.put("imsi",true);
		deviceSupported.put("version",true);
		deviceSupported.put("platform",true);

		osSupported.put("language",true);
		osSupported.put("version",true);
		osSupported.put("name",true);
		osSupported.put("vendor",true);

		runtimeSupported.put("version",true);
		runtimeSupported.put("name",true);
		runtimeSupported.put("vendor",true);

		wifiSupported.put("ssid",true);
		wifiSupported.put("signalStrength",true);
		wifiSupported.put("networkStatus",true);
	}
	

	public boolean getPropertySupport(JSONArray args){
		
		String aspect = args.optString(0);
		String property = args.optString(1);
		
		if(aspect != null && property != null){
			if(!aspectSupported.containsKey(aspect))
				return false;
			else{
				if(aspect.equals("CellularNetwork")){
					if(cellularSupported.containsKey(property))
						return cellularSupported.get(property);
					else
						return false;
				}
				else if(aspect.equals("Device")){
					if(deviceSupported.containsKey(property))
						return deviceSupported.get(property);
					else
						return false;
				}
				else if(aspect.equals("OperatingSystem")){
					if(osSupported.containsKey(property))
						return osSupported.get(property);
					else
						return false;
				}
				else if(aspect.equals("Runtime")){
					if(runtimeSupported.containsKey(property))
						return runtimeSupported.get(property);
					else
						return false;
				}
				else if(aspect.equals("WiFiNetwork")){
					if(wifiSupported.containsKey(property))
						return wifiSupported.get(property);
					else
						return false;
				}
				else{
					return false;
				}				
			}	
		}
		else{
			return false;
		}
	}

	public JSONObject getProperty(JSONArray args){
		
		
		JSONObject ref = args.optJSONObject(0);
		
		String aspect = null;
		String property = null;
		Object result = null;
		
		if(ref != null){
			aspect = ref.optString("aspect");
			property = ref.optString("property");	
		}
		
		JSONArray  temp = new JSONArray();
		temp.put(aspect);
		temp.put(property);
		
		if(getPropertySupport(temp)){
	
			//CellularNetwork
			if(property.equals("isInRoaming") && aspect.equals("CellularNetwork")){
				result = telephonyManager.isNetworkRoaming();
				Log.i(LOG_TAG, "CellularNetwork isInRoaming::" + result);
				return makereturnval(aspect,property,result);	
			}
			else if(property.equals("signalStrength")&& aspect.equals("CellularNetwork")){
				//startWatchCellSignal();
				return makereturnval(aspect,property,celsignal);
			}
			else if(property.equals("operatorName")&& aspect.equals("CellularNetwork")){
				result = telephonyManager.getNetworkOperatorName();
				Log.i(LOG_TAG, "CellularNetwork operatorName::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			else if(property.equals("mcc")&& aspect.equals("CellularNetwork")){
				String networkOperator = telephonyManager.getSimOperator();
				if (networkOperator != null) {
					result = networkOperator.substring(0, 3); //mcc
				}
				Log.i(LOG_TAG, "CellularNetwork mcc::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			else if(property.equals("mnc")&& aspect.equals("CellularNetwork")){
				String networkOperator = telephonyManager.getSimOperator();
				if (networkOperator != null) {
					result = networkOperator.substring(3); // mnc
				}
				Log.i(LOG_TAG, "CellularNetwork mnc::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			
			//Device
			else if(property.equals("imei")&& aspect.equals("Device")){
				result = telephonyManager.getDeviceId();
				Log.i(LOG_TAG, "Device imei::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			else if(property.equals("model")&& aspect.equals("Device")){
				result = Build.MODEL;
				Log.i(LOG_TAG, "Device model::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			else if(property.equals("vendor")&& aspect.equals("Device")){
				result = Build.MANUFACTURER;
				Log.i(LOG_TAG, "Device vendor::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			else if(property.equals("imsi")&& aspect.equals("Device")){
				result = telephonyManager.getSubscriberId();
				return makereturnval(aspect,property,result);	
			}
			else if(property.equals("version") && aspect.equals("Device")){
				result = Build.VERSION.RELEASE;
				Log.i(LOG_TAG, "device version::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			else if(property.equals("platform") && aspect.equals("Device")){
				result = "Android";
				Log.i(LOG_TAG, "device platform::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			
			//Operating System
			else if(property.equals("language") && aspect.equals("OperatingSystem")){
				result = ctx.getResources().getConfiguration().locale.getCountry();
				Log.i(LOG_TAG, "language::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			else if(property.equals("version") && aspect.equals("OperatingSystem")){
				result = System.getProperty("os.version");
				Log.i(LOG_TAG, "os version::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			else if(property.equals("name") && aspect.equals("OperatingSystem")){
				result = System.getProperty("os.name");
				Log.i(LOG_TAG, "os name::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			else if(property.equals("vendor") && aspect.equals("OperatingSystem")){
				result = System.getProperty("java.vendor");
				Log.i(LOG_TAG, "os vendor::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			
			//Runtime
			else if(property.equals("version") && aspect.equals("Runtime")){
				result = RUNTIME_VERSION;
				Log.i(LOG_TAG, "runtime version::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			else if(property.equals("name") && aspect.equals("Runtime")){
				result = RUNTIME_NAME;
				Log.i(LOG_TAG, "runtime name::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			else if(property.equals("vendor") && aspect.equals("Runtime")){
				result = RUNTIME_VENDOR;
				Log.i(LOG_TAG, "runtime vendor::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			
			//WiFiNetwork
			else if(property.equals("ssid") && aspect.equals("WiFiNetwork")){
				wifiInfo = wifiManager.getConnectionInfo();
				result = wifiInfo.getSSID();
				if(result == null)
					result = "undefined";
				
				Log.i(LOG_TAG, "WiFiNetwork ssid::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			else if(property.equals("signalStrength") && aspect.equals("WiFiNetwork")){
				result = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5) * 25; // 0 ~ 100
				Log.i(LOG_TAG, "WiFiNetwork signalStrength::" + String.valueOf(result));
				return makereturnval(aspect,property,result);	
			}
			else if(property.equals("networkStatus") && aspect.equals("WiFiNetwork")){
				SupplicantState i = wifiInfo.getSupplicantState();
				String name = i.name();
				
//				if(name.equals("UNINITIALIZED"))
//					result = STATUS_FORBIDDEN;
//				else if(name.equals("COMPLETED"))
//					result = STATUS_CONNECTED;
//				else 
//					result = STATUS_AVAILABLE;
				
				Log.i(LOG_TAG, "WiFiNetwork networkStatus::" + String.valueOf(name));
				return makereturnval(aspect,property,name);	
			}
		}
		else{
			return null;
		}
		return null;
	}

	public JSONObject makereturnval(String aspect, String property , Object value){
		JSONObject proRef = new JSONObject();
		JSONObject returnval = new JSONObject();
		try{
			proRef.put("aspect", aspect);
			proRef.put("property",property);

			returnval.put("value", value);
			returnval.put("property",proRef);	
		}
		catch(JSONException e){
			Log.e(LOG_TAG, "error in make return val");
		}
		return returnval;	
	}
	

	public static int calculateSignalStrength(int asu) {
		Log.i("asu : ", ": " + asu);
		int result = 0;
		if(asu <= 2 || asu > 31)	result = 0;
		else if(asu >= 16)	result = 100;
		else result = (int)Math.round(asu * 6.25);
		return result;
	}
	
	public void startWatchCellSignal(){
		telephonyManager.listen(new PhoneStateListener(){
			@Override
			public void onSignalStrengthsChanged(SignalStrength signalStrength) {
				// TODO Auto-generated method stub
				super.onSignalStrengthsChanged(signalStrength);
				celsignal = calculateSignalStrength(signalStrength.getGsmSignalStrength());
				Log.i("signalStrength : ", "" + celsignal);
				//telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
			}
			
		}, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		telephonyManager.listen(new PhoneStateListener(), PhoneStateListener.LISTEN_NONE);
	}
	
	
}
