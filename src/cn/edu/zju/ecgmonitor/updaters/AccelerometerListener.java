package cn.edu.zju.ecgmonitor.updaters;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class AccelerometerListener {
	public final static String TAG = "AccelerometerListener";
	
	SensorManager sm;
	Sensor accelerometer;
	MySensorEventListener mListener;
	float[] acceValues = new float[3];
	public AccelerometerListener(Context context) {
		sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mListener = new MySensorEventListener();
		sm.registerListener(mListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	public float[] getAcceDate(){
		return acceValues;
	}
	
	public void stopListening(){
		sm.unregisterListener(mListener);
	}
	
	class MySensorEventListener implements SensorEventListener{
		@Override
		public void onSensorChanged(SensorEvent event) {
			acceValues = event.values;
		}
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			
		}
	}
}
