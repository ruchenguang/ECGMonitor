package cn.edu.zju.ecgmonitor.updaters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class RecordReader extends Timer{
	public final static String TAG = "RecordReader";
	
	int numberOnce = 50;
	int sampleRate = 500;
	
	File ecgRecordFile = null;
	FileInputStream ecgRecordFis = null;
	
	SignalProcessor signalProcessor;
	TimeUpdater timeUpdater;
	Context context;
	
	ArrayList<Double> pointsToAdd = new ArrayList<Double>(); 
	TimerTask updateSignalProcessorData = new TimerTask() {
		@Override
		public void run() {
			if(null != ecgRecordFis){
				byte[] buffer = new byte[2];
				int[] newData = new int[numberOnce];
				try {
					for(int i=0; i<numberOnce; i++){
						if(ecgRecordFis.read(buffer) > 0){
							newData[i] =(int) (buffer[0]&0xff)*128+(buffer[1]&0xff);
						}
						else{
							this.cancel();
							Handler handler = new Handler(Looper.getMainLooper());
							handler.postDelayed(new Runnable() {
								@Override
								public void run() {
									timeUpdater.cancel();
									Toast.makeText(context, "This is the end of this record file", Toast.LENGTH_LONG).show();
								}
							}, 2000);
							Log.e(TAG, "This is the end of this record file");
							break;
						}
					}
					signalProcessor.addData(newData);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
			
		}
	};
	
	public RecordReader(Context context, SignalProcessor signalProcessor, TimeUpdater timeUpdater, String filePath) {
		this.context = context;
		this.timeUpdater = timeUpdater;
		ecgRecordFile = new File(filePath);
		this.signalProcessor = signalProcessor;
		
		if(ecgRecordFile.exists()){
			try {
				ecgRecordFis = new FileInputStream(ecgRecordFile);
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else{
			Log.e(TAG, "No such ECG record file!");
		}
	}
	
	public void schedule(long delay, long period){
		this.schedule(updateSignalProcessorData, delay, period);
		timeUpdater.schedule(delay+2000);
	}
}
