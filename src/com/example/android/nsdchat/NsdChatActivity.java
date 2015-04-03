/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.nsdchat;

import java.io.IOException;
import java.net.InetAddress;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class NsdChatActivity extends Activity {

    NsdHelper mNsdHelper;

    private TextView mStatusView;
    private Handler mUpdateHandler;

    public static final String TAG = "NsdChat";

    ChatConnection mConnection;

    private static MulticastLock multicastLock;    
    @Override 
    protected void onStart() 
    { 
    	Log.i(TAG, "Starting ServiceActivity..."); super.onStart(); 
    	//try { 
    		Log.i(TAG, "Starting Mutlicast Lock..."); 
    	WifiManager wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE); 
    	// get the device ip address 
    	//final InetAddress deviceIpAddress = getDeviceIpAddress(wifi); 
    	multicastLock = wifi.createMulticastLock(getClass().getName()); 
    	multicastLock.setReferenceCounted(true); 
    	multicastLock.acquire(); 
    	Log.i(TAG, "Starting ZeroConf probe...."); 
    	//jmdns = JmDNS.create(deviceIpAddress, HOSTNAME); jmdns.addServiceTypeListener(this); 
    	//} catch (IOException ex) { 
    		 
    	// 
    	Log.i(TAG, "Started ZeroConf probe...."); 
    }
    
    @Override protected void onStop() 
    { 
    	Log.i(TAG, "Stopping ServiceActivity..."); 
    	super.onStop(); 
    	stopScan(); 
    }

    private static void stopScan() 
    { 
    	try { 
    		if (multicastLock != null) { 
    			Log.i(TAG, "Releasing Mutlicast Lock..."); 
    			multicastLock.release(); 
    			multicastLock = null; 
    			} 
    		} catch (Exception ex) { 
    			Log.e(TAG, ex.getMessage(), ex); 
    		} 
    	}

    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
    	
        mStatusView = (TextView) findViewById(R.id.status);

        mUpdateHandler = new Handler() {
                @Override
            public void handleMessage(Message msg) {
                String chatLine = msg.getData().getString("msg");
                addChatLine(chatLine);
            }
        };

        mConnection = new ChatConnection(mUpdateHandler);

        mNsdHelper = new NsdHelper(this);
        EditText messageView = (EditText) this.findViewById(R.id.hostname);
    	messageView.setText(mNsdHelper.mServiceName);

        mNsdHelper.initializeNsd();
        
        
        
    }

    public void clickAdvertise(View v) {
        // Register service
        if(mConnection.getLocalPort() > -1) {
            mNsdHelper.registerService(mConnection.getLocalPort());
        } else {
            Log.d(TAG, "ServerSocket isn't bound.");
        }
    }

    public void clickDiscover(View v) {
        mNsdHelper.discoverServices();
    }

    public void clickConnect(View v) {
        NsdServiceInfo service = mNsdHelper.getChosenServiceInfo();
        if (service != null) {
            Log.d(TAG, "Connecting: " + service);
            mConnection.connectToServer(service.getHost(),
                    service.getPort());
        } else {
            Log.d(TAG, "No service to connect to!");
        }
    }

    public void clickSend(View v) {
        EditText messageView = (EditText) this.findViewById(R.id.chatInput);
        if (messageView != null) {
            String messageString = messageView.getText().toString();
            if (!messageString.isEmpty()) {
                mConnection.sendMessage(messageString);
            }
            messageView.setText("");
        }
    }

    public void clickSetHost(View v){
    	EditText messageView = (EditText) this.findViewById(R.id.hostname);
    	mNsdHelper.mServiceName = messageView.getText().toString();
    }
    
    public void addChatLine(String line) {
        mStatusView.append("\n" + line);
    }

    @Override
    protected void onPause() {
        if (mNsdHelper != null) {
            mNsdHelper.stopDiscovery();
        }
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (mNsdHelper != null) {
            mNsdHelper.discoverServices();
        }
    }
    
    @Override
    protected void onDestroy() {
        mNsdHelper.tearDown();
        mConnection.tearDown();
        super.onDestroy();
    }
}
