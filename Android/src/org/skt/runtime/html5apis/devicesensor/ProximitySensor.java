package org.skt.runtime.html5apis.devicesensor;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;
import org.skt.runtime.api.RuntimeInterface;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class ProximitySensor extends Plugin  implements SensorEventListener {

	private static final String LOG_TAG = "ProximitySensor";
	
	public static int STOPPED = 0;
	public static int STARTING = 1;
	public static int RUNNING = 2;
	public static int ERROR_FAILED_TO_START = 3;

	private double centimetres  ;
	
	private int status;							    // status of listener
	private int accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;

	private SensorManager sensorManager;    // Sensor manager
	private Sensor mSensor;						      

	private String callbackId;              // Keeps track of the single "start" callback ID passed in from JS

	/**
	 * Create an lightsensor listener.
	 */
	public ProximitySensor() {
		this.setStatus(ProximitySensor.STOPPED);
	}

	public void setContext(RuntimeInterface ctx) {
		super.setContext(ctx);
		this.sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
	}

	public PluginResult execute(String action, JSONArray args, String callbackId) {
		PluginResult.Status status = PluginResult.Status.NO_RESULT;
		String message = "";
		PluginResult result = new PluginResult(status, message);
		result.setKeepCallback(true);	

		if (action.equals("start")) {
			this.callbackId = callbackId;
			Log.e(LOG_TAG, "ProximitySensor listener is registered.");
			if (this.status != ProximitySensor.RUNNING) {
				this.start();
			}
		}
		else if (action.equals("stop")) {
			if (this.status == ProximitySensor.RUNNING) {
				this.stop();
			}
		} else {
			// Unsupported action
			return new PluginResult(PluginResult.Status.INVALID_ACTION);
		}
		return result;
	}
	public void onDestroy() {
		this.stop();
	}

	//--------------------------------------------------------------------------
	// LOCAL METHODS
	//--------------------------------------------------------------------------
	//
	private int start() {
		// If already starting or running, then just return
		if ((this.status == ProximitySensor.RUNNING) || (this.status == ProximitySensor.STARTING)) {
			return this.status;
		}

		this.setStatus(ProximitySensor.STARTING);

		// Get Proximity from sensor manager
		List<Sensor> list = this.sensorManager.getSensorList(Sensor.TYPE_PROXIMITY); 
		// If found, then register as listener
		if ((list != null) && (list.size() > 0)) {
			this.mSensor = list.get(0);
			this.sensorManager.registerListener(this, this.mSensor, SensorManager.SENSOR_DELAY_UI);
			this.setStatus(ProximitySensor.STARTING);
		} else {
			this.setStatus(ProximitySensor.ERROR_FAILED_TO_START);
			this.fail(ProximitySensor.ERROR_FAILED_TO_START, "No sensors found to register ProximitySensor listening to.");
			return this.status;
		}

		return this.status;
	}

	/**
	 * Stop proximity to ProximitySensor.
	 */
	private void stop() {
		if (this.status != ProximitySensor.STOPPED) {
			this.sensorManager.unregisterListener(this);
		}
		this.setStatus(ProximitySensor.STOPPED);
		this.accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
	}

	/**
	 * Called when the accuracy of the sensor has changed.
	 * 
	 * @param sensor
	 * @param accuracy
	 */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Only look at proximity events
		if (sensor.getType() != Sensor.TYPE_PROXIMITY) {
			return;
		}

		// If not running, then just return
		if (this.status == ProximitySensor.STOPPED) {
			return;
		}
		this.accuracy = accuracy;
	}

	/**
	 * Sensor listener event.
	 * 
	 * @param SensorEvent event
	 */
	public void onSensorChanged(SensorEvent event) {
		// Only look at Proximity events
		if (event.sensor.getType() != Sensor.TYPE_PROXIMITY) {
			return;
		}

		// If not running, then just return
		if (this.status == ProximitySensor.STOPPED) {
			return;
		}

		this.setStatus(ProximitySensor.RUNNING);

		//Log.e(LOG_TAG, "MaximumRange of Proximity = " +event.sensor.getMaximumRange());
		
		if (this.accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {

			this.centimetres = event.values[0];
			this.win();
		}
	}

	// Sends an error back to JS
	private void fail(int code, String message) {
		// Error object
		JSONObject errorObj = new JSONObject();
		try {
			errorObj.put("code", code);
			errorObj.put("message", message);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		PluginResult err = new PluginResult(PluginResult.Status.ERROR, errorObj);
		err.setKeepCallback(true);

		this.error(err, this.callbackId);
	}

	private void win() {
		// Success return object
		PluginResult result = new PluginResult(PluginResult.Status.OK, this.getProximityJSON());
		result.setKeepCallback(true);

		this.success(result, this.callbackId);
	}

	private void setStatus(int status) {
		this.status = status;
	}

	private JSONObject getProximityJSON() {
		JSONObject r = new JSONObject();
		try {
			r.put("type", 5);
			r.put("value",this.centimetres);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return r;
	}

}
