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

public class HumiditySensor extends Plugin  implements SensorEventListener {

	private static final String LOG_TAG = "HumiditySensor";
	
	public static int STOPPED = 0;
	public static int STARTING = 1;
	public static int RUNNING = 2;
	public static int ERROR_FAILED_TO_START = 3;

	private double percentage ;
	
	private int status;							    // status of listener
	private int accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;

	private SensorManager sensorManager;    // Sensor manager
	private Sensor mSensor;						      

	private String callbackId;              // Keeps track of the single "start" callback ID passed in from JS

	/**
	 * Create an lightsensor listener.
	 */
	public HumiditySensor() {
		this.setStatus(HumiditySensor.STOPPED);
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
			Log.e(LOG_TAG, "HumoditySensor listener is registered.");
			if (this.status != HumiditySensor.RUNNING) {
				this.start();
			}
		}
		else if (action.equals("stop")) {
			if (this.status == HumiditySensor.RUNNING) {
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
		if ((this.status == HumiditySensor.RUNNING) || (this.status == HumiditySensor.STARTING)) {
			return this.status;
		}

		this.setStatus(HumiditySensor.STARTING);

		// Get humidity from sensor manager
		List<Sensor> list = this.sensorManager.getSensorList(Sensor.TYPE_RELATIVE_HUMIDITY); //TODO :: unsupported ins Android 4.0.3
		// If found, then register as listener
		if ((list != null) && (list.size() > 0)) {
			this.mSensor = list.get(0);
			this.sensorManager.registerListener(this, this.mSensor, SensorManager.SENSOR_DELAY_UI);
			this.setStatus(HumiditySensor.STARTING);
		} else {
			this.setStatus(HumiditySensor.ERROR_FAILED_TO_START);
			this.fail(HumiditySensor.ERROR_FAILED_TO_START, "No sensors found to register HumoditySensor listening to.");
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
			this.setStatus(HumiditySensor.ERROR_FAILED_TO_START);
			this.fail(HumiditySensor.ERROR_FAILED_TO_START, "HumoditySensor could not be started.");
		}
		return this.status;
	}

	/**
	 * Stop listening to HumiditySensor.
	 */
	private void stop() {
		if (this.status != HumiditySensor.STOPPED) {
			this.sensorManager.unregisterListener(this);
		}
		this.setStatus(HumiditySensor.STOPPED);
		this.accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
	}

	/**
	 * Called when the accuracy of the sensor has changed.
	 * 
	 * @param sensor
	 * @param accuracy
	 */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Only look at humidity events
		if (sensor.getType() != Sensor.TYPE_RELATIVE_HUMIDITY) {
			return;
		}

		// If not running, then just return
		if (this.status == HumiditySensor.STOPPED) {
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
		// Only look at Humidity events
		if (event.sensor.getType() != Sensor.TYPE_RELATIVE_HUMIDITY) {
			return;
		}

		// If not running, then just return
		if (this.status == HumiditySensor.STOPPED) {
			return;
		}

		this.setStatus(HumiditySensor.RUNNING);

		if (this.accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {

			this.percentage = event.values[0];
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
		PluginResult result = new PluginResult(PluginResult.Status.OK, this.getHumidityJSON());
		result.setKeepCallback(true);

		this.success(result, this.callbackId);
	}

	private void setStatus(int status) {
		this.status = status;
	}

	private JSONObject getHumidityJSON() {
		JSONObject r = new JSONObject();
		try {
			r.put("type", 3);
			r.put("value",this.percentage);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return r;
	}

}
