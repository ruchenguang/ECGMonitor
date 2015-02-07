package cn.edu.zju.ecgmonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.edu.zju.ecgmonitor.ble.BluetoothLeService;
import cn.edu.zju.ecgmonitor.ble.SampleGattAttributes;
import cn.edu.zju.ecgmonitor.updaters.DataRecorder;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceDisplayActivity extends Activity {
	public final static String TAG = "MainActivity";

	TextView tvState;
	Button recordButton;
	
	DataRecorder dataRecorder;
	
	boolean isDevice = true;
	boolean isWritingToFile = false;
	boolean isConnected = false;
	
	//*********BLE*********
	private BluetoothLeService mBluetoothLeService;
	String deviceName, deviceAddress;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";  
    private BluetoothGattCharacteristic mNotifyCharacteristic;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_display);
        //keep the screen on
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 
        
        //get info from the select activity
        Intent intent = getIntent();
        deviceName = intent.getStringExtra(SelectActivity.EXTRAS_DEVICE_NAME);
    	deviceAddress = intent.getStringExtra(SelectActivity.EXTRAS_DEVICE_ADDRESS);
        
        //set the ui state and listener
        tvState = (TextView) findViewById(R.id.textView1);
        recordButton = (Button) findViewById(R.id.button1);
        recordButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isWritingToFile){
					//stop writing data to file
					if(dataRecorder != null)
						dataRecorder.stopWriting();
					
					recordButton.setText("Start Recording");
					tvState.setText("Waiting...");
				} else{
					//start writing data to file
					dataRecorder = new DataRecorder(SelectActivity.ecgRecordsDirPath);
					dataRecorder.startWriting();
					
					recordButton.setText("Stop Recording");
					tvState.setText("Writing BLE data to file");
				}
				isWritingToFile = !isWritingToFile;	
			}
		});
        
    	//start the ble service
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }
	
    @Override
    protected void onResume() {
        super.onResume();
    	
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(deviceAddress);
            Toast.makeText(this, "Connect request result=" + result, Toast.LENGTH_LONG).show();
        } else 
        	 Toast.makeText(this, "Please try to connect again", Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        unregisterReceiver(mGattUpdateReceiver);
        mBluetoothLeService.close();
        mBluetoothLeService = null;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        
    	if(isWritingToFile) dataRecorder.stopWriting();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (isConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(deviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(deviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            	//write to file
            	byte[] bleRawBytes = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
            	if(isWritingToFile) 
            		dataRecorder.writeToFile(bleRawBytes);
            	
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
            	isConnected = false;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            	connectCharacteristic(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action) ) {
            	isConnected = true;
                invalidateOptionsMenu();
            }
        }
    };
    
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        
        return intentFilter;
    }

    
    void connectCharacteristic(List<BluetoothGattService> gattServices){
    	if (gattServices == null) {
    		Log.e(TAG, "gattServices is null!");
    		return;
    	}
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = 
        		new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, cn.edu.zju.ecgmonitor.ble.SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
        
        // Initiate the connection of ECG signal on the last char of the last service
        // Here the positon is (2, 0)
        final BluetoothGattCharacteristic characteristic =
                mGattCharacteristics.get(2).get(0);
        final int charaProp = characteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on the user interface.
            if (mNotifyCharacteristic != null) {
                mBluetoothLeService.setCharacteristicNotification(
                        mNotifyCharacteristic, false);
                mNotifyCharacteristic = null;
            }
            //mBluetoothLeService.readCharacteristic(characteristic);
        }
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = characteristic;
            mBluetoothLeService.setCharacteristicNotification(
                    characteristic, true);
        }
    }
    
}
