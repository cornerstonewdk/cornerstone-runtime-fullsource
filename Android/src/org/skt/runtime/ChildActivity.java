package org.skt.runtime;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ChildActivity extends RuntimeActivity {
	
	public static ChildActivity childActivity;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(childActivity == null)
        	childActivity = this;
        
        Intent intent = getIntent();
        String url = intent.getStringExtra("childurl");
        
        if(url != null){
        	//url = url.replace("http", "https");
        	super.loadUrl(url);
        }       
    }		
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		childActivity = null;
	}	
}
