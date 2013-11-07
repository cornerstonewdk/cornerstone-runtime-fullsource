package org.skt.runtime.html5apis;

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

public class Orientation extends Plugin implements SensorEventListener  {

	public static int STOPPED = 0;
	public static int STARTING = 1;
	public static int RUNNING = 2;
	public static int ERROR_FAILED_TO_START = 3;

	private float alpha,beta,gamma;					// most recent rotation values
	private long timestamp;					        // time of most recent value
	private int status;							    // status of listener
	private int accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;

	private SensorManager sensorManager;    // Sensor manager
	private Sensor mSensor;						      // Orientation sensor returned by sensor manager

	private String callbackId;              // Keeps track of the single "start" callback ID passed in from JS

	/**
	 * Create an accelerometer listener.
	 */
	public Orientation() {
		this.alpha = 0;
		this.beta = 0;
		this.gamma = 0;
		this.timestamp = 0;
		this.setStatus(Orientation.STOPPED);
	}

	/**
	 * Sets the context of the Command. This can then be used to do things like
	 * get file paths associated with the Activity.
	 * 
	 * @param ctx The context of the main Activity.
	 */
	public void setContext(RuntimeInterface ctx) {
		super.setContext(ctx);
		this.sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
	}

	/**
	 * Executes the request and returns PluginResult.
	 * 
	 * @param action 		The action to execute.
	 * @param args 			JSONArry of arguments for the plugin.
	 * @param callbackId	The callback id used when calling back into JavaScript.
	 * @return 				A PluginResult object with a status and message.
	 */
	public PluginResult execute(String action, JSONArray args, String callbackId) {
		PluginResult.Status status = PluginResult.Status.NO_RESULT;
		String message = "";
		PluginResult result = new PluginResult(status, message);
		result.setKeepCallback(true);	

		if (action.equals("start")) {
			this.callbackId = callbackId;
			if (this.status != Orientation.RUNNING) {
				// If not running, then this is an async call, so don't worry about waiting
				// We drop the callback onto our stack, call start, and let start and the sensor callback fire off the callback down the road
				this.start();
			}
		}
		else if (action.equals("stop")) {
			if (this.status == Orientation.RUNNING) {
				this.stop();
			}
		} else {
			// Unsupported action
			return new PluginResult(PluginResult.Status.INVALID_ACTION);
		}
		return result;
	}

	/**
	 * Called by AccelBroker when listener is to be shut down.
	 * Stop listener.
	 */
	public void onDestroy() {
		this.stop();
	}

	//--------------------------------------------------------------------------
	// LOCAL METHODS
	//--------------------------------------------------------------------------
	//
	/**
	 * Start listening for acceleration sensor.
	 * 
	 * @return 			status of listener
	 */
	private int start() {
		// If already starting or running, then just return
		if ((this.status == Orientation.RUNNING) || (this.status == Orientation.STARTING)) {
			return this.status;
		}

		this.setStatus(Orientation.STARTING);

		// Get accelerometer from sensor manager
		List<Sensor> list = this.sensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
		// If found, then register as listener
		if ((list != null) && (list.size() > 0)) {
			this.mSensor = list.get(0);
			this.sensorManager.registerListener(this, this.mSensor, SensorManager.SENSOR_DELAY_UI);
			this.setStatus(Orientation.STARTING);
		} else {
			this.setStatus(Orientation.ERROR_FAILED_TO_START);
			this.fail(Orientation.ERROR_FAILED_TO_START, "No sensors found to register orientation listening to.");
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
			this.setStatus(Orientation.ERROR_FAILED_TO_START);
			this.fail(Orientation.ERROR_FAILED_TO_START, "Orientation could not be started.");
		}
		return this.status;
	}

	/**
	 * Stop listening to acceleration sensor.
	 */
	private void stop() {
		if (this.status != Orientation.STOPPED) {
			this.sensorManager.unregisterListener(this);
		}
		this.setStatus(Orientation.STOPPED);
		this.accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
	}

	/**
	 * Called when the accuracy of the sensor has changed.
	 * 
	 * @param sensor
	 * @param accuracy
	 */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Only look at accelerometer events
		if (sensor.getType() != Sensor.TYPE_ORIENTATION) {
			return;
		}

		// If not running, then just return
		if (this.status == Orientation.STOPPED) {
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
		// Only look at orientation events
		if (event.sensor.getType() != Sensor.TYPE_ORIENTATION) {
			return;
		}

		// If not running, then just return
		if (this.status == Orientation.STOPPED) {
			return;
		}

		this.setStatus(Orientation.RUNNING);

		if (this.accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {

			// Save time that event was received
			this.timestamp = System.nanoTime();
			this.alpha = event.values[0];
			this.beta = event.values[1];
			this.gamma = event.values[2];

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
		PluginResult result = new PluginResult(PluginResult.Status.OK, this.getRotationJSON());
		result.setKeepCallback(true);

		this.success(result, this.callbackId);
	}

	private void setStatus(int status) {
		this.status = status;
	}

	private JSONObject getRotationJSON() {
		JSONObject r = new JSONObject();
		try {
			r.put("alpha", this.alpha);
			r.put("beta", this.beta);
			r.put("gamma", this.gamma);
			r.put("timestamp", this.timestamp);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return r;
	}
}
