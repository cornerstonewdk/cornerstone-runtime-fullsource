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

public class LightSensor extends Plugin  implements SensorEventListener {

	private static final String LOG_TAG = "LightSensor";
	
	public static int STOPPED = 0;
	public static int STARTING = 1;
	public static int RUNNING = 2;
	public static int ERROR_FAILED_TO_START = 3;

	private double lux;
	
	private int status;							    // status of listener
	private int accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;

	private SensorManager sensorManager;    // Sensor manager
	private Sensor mSensor;						      

	private String callbackId;              // Keeps track of the single "start" callback ID passed in from JS

	/**
	 * Create an lightSensor listener.
	 */
	public LightSensor() {
		this.setStatus(LightSensor.STOPPED);
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
			Log.e(LOG_TAG, "lightsensor listener is registered.");
			if (this.status != LightSensor.RUNNING) {
				this.start();
			}
		}
		else if (action.equals("stop")) {
			if (this.status == LightSensor.RUNNING) {
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
		if ((this.status == LightSensor.RUNNING) || (this.status == LightSensor.STARTING)) {
			return this.status;
		}

		this.setStatus(LightSensor.STARTING);

		// Get light from sensor manager
		List<Sensor> list = this.sensorManager.getSensorList(Sensor.TYPE_LIGHT); 
		// If found, then register as listener
		if ((list != null) && (list.size() > 0)) {
			this.mSensor = list.get(0);
			this.sensorManager.registerListener(this, this.mSensor, SensorManager.SENSOR_DELAY_UI);
			//this.sensorManager.registerListener(this, this.mSensor, SensorManager.SENSOR_DELAY_NORMAL);
			this.setStatus(LightSensor.STARTING);
		} else {
			this.setStatus(LightSensor.ERROR_FAILED_TO_START);
			this.fail(LightSensor.ERROR_FAILED_TO_START, "No sensors found to register light listening to.");
			return this.status;
		}

		// Wait until running
		long timeout = 2000;
		while ((this.status == STARTING) && (timeout > 0)) {
			timeout = timeout - 100;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (timeout == 0) {
			this.setStatus(LightSensor.ERROR_FAILED_TO_START);
			this.fail(LightSensor.ERROR_FAILED_TO_START, "lightSensor could not be started.");
		}
		return this.status;
	}

	/**
	 * Stop listening to light sensor.
	 */
	private void stop() {
		if (this.status != LightSensor.STOPPED) {
			this.sensorManager.unregisterListener(this);
		}
		this.setStatus(LightSensor.STOPPED);
		this.accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
	}

	/**
	 * Called when the accuracy of the sensor has changed.
	 * 
	 * @param sensor
	 * @param accuracy
	 */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Only look at light events
		if (sensor.getType() != Sensor.TYPE_LIGHT) {
			return;
		}

		// If not running, then just return
		if (this.status == LightSensor.STOPPED) {
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
		// Only look at light events
		if (event.sensor.getType() != Sensor.TYPE_LIGHT) {
			return;
		}

		// If not running, then just return
		if (this.status == LightSensor.STOPPED) {
			return;
		}

		Log.e(LOG_TAG, "MaximumRange of Light = " +event.sensor.getMaximumRange());
		Log.e(LOG_TAG, "value of Light = " + event.values[0]);
		this.setStatus(LightSensor.RUNNING);

		if (this.accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {

			this.lux = event.values[0];
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
		PluginResult result = new PluginResult(PluginResult.Status.OK, this.getLightJSON());
		result.setKeepCallback(true);

		this.success(result, this.callbackId);
	}

	private void setStatus(int status) {
		this.status = status;
	}

	private JSONObject getLightJSON() {
		JSONObject r = new JSONObject();
		try {
			r.put("type", 4);
			r.put("value",this.lux);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return r;
	}

}
