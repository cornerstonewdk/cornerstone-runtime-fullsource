package org.skt.runtime.additionalapis;

import java.util.ArrayList;

import org.json.JSONArray;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;
import org.skt.runtime.api.RuntimeInterface;

import android.view.MenuItem;

public class MenuManager extends Plugin{
	private static final String LOG_TAG = "MenuManager";

	private String CallbackId;
	
	public class SktMenuItem{
		public String action;
		public String callbackID;
		
		public SktMenuItem(String action, String callbackID) {
			super();
			this.action = action;
			this.callbackID = callbackID;
		}
	}
	
	//[20130110][chisu]use default menu
	public static boolean useOpenBrowser = true;
	public static boolean useRefresh = true;
	public static boolean useExitApp = true;
	
	public static ArrayList<SktMenuItem> menuList = new ArrayList<MenuManager.SktMenuItem>();

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

		if (action.equals("addMenu")) {
			//[20130109][chisu]default Menu
			if(args.optString(0).equals("OPENBROWSER")){
				useOpenBrowser = true;
			}
			else if(args.optString(0).equals("REFRESH")){
				useRefresh = true;
			}
			else if(args.optString(0).equals("EXITAPP")){
				useExitApp = true;
			}
			else {
				menuList.add(new SktMenuItem(args.optString(0), callbackId));
				result.setKeepCallback(true);				
			}
		}
		else if(action.equals("removeMenu")){
			String removeAction = args.optString(0);
			
			//[20130109][chisu]default Menu
			if(removeAction.equals("OPENBROWSER")){
				useOpenBrowser = false;
			}
			else if(removeAction.equals("REFRESH")){
				useRefresh = false;
			}
			else if(removeAction.equals("EXITAPP")){
				useExitApp = false;
			}
			else{
				for(int i = 0 ; i < menuList.size(); i ++){
					if(menuList.get(i).action.equals(removeAction)){
						result.setKeepCallback(false);
						menuList.remove(i);
					}
				}
			}	
		}
		else if(action.equals("removeAll")){
			menuList.clear();
		}

		return result;
	}

	@Override
	public void onMessage(String id, Object data) {
		// TODO Auto-generated method stub
		if(id.equals("onOptionsItemSelected")){
			MenuItem item = (MenuItem)data;
			int index = item.getItemId();
			
			PluginResult result = new PluginResult(PluginResult.Status.OK);
    		result.setKeepCallback(true);
    		this.success(result, menuList.get(index).callbackID);
		}
		
		super.onMessage(id, data);
	}
	
	
}
