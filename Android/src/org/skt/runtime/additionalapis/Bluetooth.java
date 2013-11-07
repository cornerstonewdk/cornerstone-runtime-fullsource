package org.skt.runtime.additionalapis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;
import org.skt.runtime.api.RuntimeInterface;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class Bluetooth extends Plugin{

	private static final String LOG_TAG = "CornerstoneBluetooth";
	
	private static final String BASE_UUID = "00001101-0000-1000-8000-00805F9B34FB";
	
	private String callbackId;

	BluetoothAdapter mBluetoothAdapter = null;
	ArrayList<String >mNewDevicesArrayAdapter = new ArrayList<String>();

	 private BluetoothSocket clientSocket = null;
	 private BluetoothServerSocket serverSocket = null;
	 private BluetoothDevice clientDevice = null;
	    
	/**
	 * Sets the context of the Command. This can then be used to do things like
	 * get file paths associated with the Activity.
	 * 
	 * @param ctx The context of the main Activity.
	 */
	public void setContext(RuntimeInterface ctx) {
		super.setContext(ctx);
	}

	/**
	 * Executes the request and returns PluginResult.
	 * 
	 * @param action        The action to execute.
	 * @param args          JSONArry of arguments for the plugin.
	 * @param callbackId    The callback id used when calling back into JavaScript.
	 * @return              A PluginResult object with a status and message.
	 */
	public PluginResult execute(String action, JSONArray args, String callbackId) {

		PluginResult.Status status = PluginResult.Status.NO_RESULT;
		String message = "";
		PluginResult result = new PluginResult(status, message);
		
		getDefaultBTAdapter();
		
		if (action.equals("scanDevice")) {
			
			this.callbackId = callbackId;
			
			Log.e(LOG_TAG, "scanDevice");
			
			//getDefaultBTAdapter();
			enableBTIfdisable();
			scanBTDevice();
		
			result.setKeepCallback(true);
			
		}
		else if(action.equals("stopScanDevice")){
			if(mBluetoothAdapter != null)
				mBluetoothAdapter.startDiscovery();
		}
		else if(action.equals("setDeviceDiscoverable")){
			requestDiscoverable();
		}
		else if(action.equals("connectDevice")){
			String address = args.optString(0);
			if(!address.equals("null")){
				BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);	
				String name = device.getName();
				Log.e(LOG_TAG, name);
				
				connectClientThread(device);
				connectClientSocket();
			}
		}
		else if(action.equals("disConnectDevice")){
			
		}
		else if(action.equals("makeServerSocket")){
			//getDefaultBTAdapter();
			connectServerThread();
			connectServerSocket();	
		}
		
		return result;
	}

	private void returnBTDevicestoJS(){
		
		JSONArray btDevices = getBTDevices();
		
		PluginResult result = new PluginResult(PluginResult.Status.OK,btDevices);
		result.setKeepCallback(false);

		this.success(result, this.callbackId);
	}
	
	private void connectServerThread(){
		 // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
        	UUID serveruuid = UUID.fromString(BASE_UUID);
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("CornerStoneRuntime", serveruuid);
            
        } catch (IOException e) { }
        
        serverSocket = tmp;
	}
	
	public void connectServerSocket() {
		
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned
        while (true) {
            try {
            	Log.e(LOG_TAG, "Server Socket Accept start");
                socket = serverSocket.accept();
            } catch (IOException e) {
                break;
            }
            // If a connection was accepted
            if (socket != null) {
            	Log.e(LOG_TAG, "Server Socket is returned");
                // Do work to manage the connection (in a separate thread)
                //manageConnectedSocket(socket);
                try {
					serverSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                break;
            }
        }
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancelServerSocket() {
        try {
        	serverSocket.close();
        } catch (IOException e) { }
    }
    
    
    
	private void connectClientThread(BluetoothDevice device){
		 // because mmSocket is final
        BluetoothSocket tmp = null;
        
		clientDevice = device;
		
		// Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(BASE_UUID));
        } catch (IOException e) { }
        
        clientSocket = tmp;
        
	}
	
	public void connectClientSocket(){
        // Cancel discovery because it will slow down the connection
        mBluetoothAdapter.cancelDiscovery();

        try {
        	Log.e(LOG_TAG, "Client Socket Connect start");
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
        	clientSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            try {
            	clientSocket.close();
            	Log.e(LOG_TAG, connectException.getMessage());
            } catch (IOException closeException) { }
            return;
        }
        
        Log.e(LOG_TAG, "Client Socket is returned");

        // TODO :: Do work to manage the connection (in a separate thread)
        //manageConnectedSocket(clientSocket);
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancelClientSocket() {
        try {
        	clientSocket.close();
        } catch (IOException e) { }
    }
    
    
    
	private JSONArray getBTDevices(){
		
		JSONArray bTArray = new JSONArray();
		
		for(int i = 0 ; i < mNewDevicesArrayAdapter.size() ; i ++){
			
			String device_address = mNewDevicesArrayAdapter.get(i);
			String name = device_address.split("=")[0];
			String address = device_address.split("=")[1];
			
			JSONObject btDevice = new JSONObject();
			try {
				btDevice.put("name", name);
				btDevice.put("address", address);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			bTArray.put(btDevice);
		}
		
		return bTArray;
	}
	
	private void requestDiscoverable(){
		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
		this.ctx.startActivity(discoverableIntent);
	}

	private void getDefaultBTAdapter(){

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if(mBluetoothAdapter == null){
			// Device does not support Bluetooth
		}
	}

	private void enableBTIfdisable(){

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			this.ctx.startActivityForResult((Plugin) this, enableBtIntent, 0);
		}
	}

	private void scanBTDevice(){
		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.ctx.registerReceiver(mReceiver, filter);
		// Register for broadcasts when discovery has finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.ctx.registerReceiver(mReceiver, filter);

		mBluetoothAdapter.startDiscovery();
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					mNewDevicesArrayAdapter.add(device.getName() + "=" + device.getAddress());
					Log.e(LOG_TAG, device.getName() + "::" + device.getAddress());
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				if (mNewDevicesArrayAdapter.size() == 0) {
					mNewDevicesArrayAdapter.add("There is no device");
				}
				Log.e(LOG_TAG, "btsize ::" + mNewDevicesArrayAdapter.size());
				
				returnBTDevicestoJS();
			}
		}
	};
}
