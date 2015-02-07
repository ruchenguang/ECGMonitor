package cn.edu.zju.ecgmonitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SelectActivity extends ListActivity {
	public final static String TAG = "SelectActivity";
	
	public final static String EXTRAS_IS_DEVICE = "ECGMonitor::isDevice";
	public final static String EXTRAS_FILE_NAME = "ECGMonitor::fileName";
	public final static String EXTRAS_FILE_PATH = "ECGMonitor::filePath";
	public final static String EXTRAS_DEVICE_NAME =  "ECGMonitor::deviceName";
	public final static String EXTRAS_DEVICE_ADDRESS = "ECGMonitor::deviceAddress";
	
	public static String ecgRecordsDirPath, sensorRecordsDirPath, tmpDirPath;
//	View bluetoothView, recordsView;
	TextView emptyTextView;
	File ecgRecordsDir, sensorRecordsDir, ecgMonitorDir, tmpDir;
	
	boolean isBluetoothEnabled = false;
	boolean isBleSupported = false;
	boolean isFileTab = false;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select);
		
		emptyTextView = (TextView) findViewById(android.R.id.empty);
        //initialize directory
        initDirectory();
        
		mHandler = new Handler();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
        } else {
        	isBleSupported = true;
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
        		(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }
	}

	@Override
	protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if(isBleSupported){
            // Initializes list view adapter.
            mLeDeviceListAdapter = new LeDeviceListAdapter();
            setListAdapter(mLeDeviceListAdapter);
        	
            if (!mBluetoothAdapter.isEnabled()) {
            	Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else 
            	scanLeDevice(true);
        }				
        
//        recordsView.setAlpha((float) 0.5);
//		bluetoothView.setAlpha(1);
//		
		initDeviceList();
	};
	
    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        if(mLeDeviceListAdapter != null) mLeDeviceListAdapter.clear();
    }
    
	void initDirectory(){
		ecgMonitorDir = new File(Environment.getExternalStorageDirectory(), "ECG_Monitor");
		if(!ecgMonitorDir.exists()){
			ecgMonitorDir.mkdir();
			Log.d(TAG, "make directory ECGMonitor under sdcard");
		}
		
		ecgRecordsDir = new File(ecgMonitorDir.getAbsoluteFile(), "ECG_Records");
		if(!ecgRecordsDir.exists()){
			ecgRecordsDir.mkdir();
			Log.d(TAG, "make directory ECG_Records under ECG_Monitor");
		}
		ecgRecordsDirPath = ecgRecordsDir.getAbsolutePath();
		
		
		//create a temp file for temperal data
		tmpDir = new File(ecgMonitorDir.getAbsolutePath(), "tmp");
		if(!tmpDir.exists()){
			tmpDir.mkdir();
			Log.d(TAG, "make directory tmp under ECG_Monitor");
		}
		tmpDirPath = tmpDir.getAbsolutePath();
	}
	
	void writeSampleRecords(File dir, String fileName, int resourceId){
		byte[] buffer = new byte[1024];
		try {
			File sampleRecord1 = new File(dir, fileName);
			FileOutputStream fos = new FileOutputStream(sampleRecord1);
			InputStream is = getResources().openRawResource(resourceId);
			while(is.read(buffer)>0){
				fos.write(buffer);
			}
			fos.close();
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        if(isFileTab){
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_scan:
        	mLeDeviceListAdapter.clear();
            scanLeDevice(true);
            break;
        case R.id.menu_stop:
            scanLeDevice(false);
            break;
        }
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent;
		if(isDevice){
			intent = new Intent(this, DeviceDisplayActivity.class);
			final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
			if(device == null) return;
			intent.putExtra(EXTRAS_DEVICE_NAME, device.getName());
			intent.putExtra(EXTRAS_DEVICE_ADDRESS, device.getAddress());
	        if (mScanning) {
	            mBluetoothAdapter.stopLeScan(mLeScanCallback);
	            mScanning = false;
	        }
			intent.putExtra(EXTRAS_IS_DEVICE, isDevice);
			startActivity(intent);
		} 

	};
	
	
	//init the file list
	boolean isDevice = true;
	String[] fileNames;
	
	//init the device list
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
	void initDeviceList(){
		isDevice = true;
		emptyTextView.setText(R.string.empty_text_device);
		setTitle(R.string.title_devices);
		setListAdapter(mLeDeviceListAdapter);
		if(isBleSupported && isBluetoothEnabled)
			scanLeDevice(true);
	}

	void scanLeDevice(final boolean enable){
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                	scanLeDevice(false);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
	}
	
	
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @SuppressLint("InflateParams") @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
