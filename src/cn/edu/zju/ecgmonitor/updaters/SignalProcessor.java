package cn.edu.zju.ecgmonitor.updaters;

import android.util.Log;
import cn.edu.zju.curveplotter.CurveSurfaceView;
import cn.zju.edu.ecgsignalprocess.WaveletCWrapper;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class SignalProcessor extends Timer{
	public final static String TAG = "SignalProcessor";
	
	CurveSurfaceView ecgSfv;
	
	//denoise related params
	boolean isDenoise = false;
	int denoisNum = 1024;
	double avrg = 0.5;

	//store the double data from record file or ble
	ArrayList<Double> dataArrayList = new ArrayList<Double>();	
	public void addData(int[] data) {
		ArrayList<Double> newData = new ArrayList<Double>();
		for(int i=0; i<data.length; i++){
			newData.add((data[i]+0.0)/1024);
		}	
		Log.d(TAG, "received data are " + newData);
		synchronized (dataArrayList) {
			dataArrayList.addAll(newData);
		}
	}
	
	//the rpeaks caculated 
	double[] rpeaks = new double[37];
	public double[] getRpeaks(){
		return rpeaks;
	}
	//params for find rpeak
	int rpeakMode = 1;
	double rpeakThreshold = 0.2;
	double rpeakShift = 0.2;
	//tag the rpeaks updated or not 
	boolean isRpeaksUpdated = false;
	public boolean getRpeaksUpdated(){
		boolean isUpdated = isRpeaksUpdated;
		isRpeaksUpdated = false;
		return isUpdated;
	}
	
	public SignalProcessor(CurveSurfaceView sfv, boolean isDenoised) {
		ecgSfv = sfv;
		//set the default denoise state
		setDenoise(isDenoised);
	}
	
	public void setDenoise(boolean isDenoise){
		this.isDenoise = isDenoise;
		avrg = isDenoise? 0.5:0.16;
		dataArrayList = new ArrayList<Double>();
		for(int i=0; i<denoisNum; i++)
			dataArrayList.add(0.5-avrg);
	}
	
	public void schedule(long delay, long period){
		this.scheduleAtFixedRate(updateEcgSfv, delay, period);
	}
	
	long timeSum = 0;
	long cnt =0;
	long upModeCnt = 0;
	long downModeCnt = 0;
	TimerTask updateEcgSfv = new TimerTask() {
		@Override
		public void run() {
			Log.d(TAG, "we got points "+dataArrayList.size());
			if(dataArrayList.size() >= denoisNum){
				double[] pointsBeforeDenoise = new double[denoisNum];
				synchronized (dataArrayList) {
					for(int i=0; i<denoisNum; i++){
						pointsBeforeDenoise[i] = dataArrayList.remove(0);
					}
				}
				double[] pointsAfterDenoise;
				if(isDenoise){
					long start = System.currentTimeMillis();
					pointsAfterDenoise = WaveletCWrapper.signalDenoise(pointsBeforeDenoise, denoisNum);
					long period = System.currentTimeMillis() - start;
					timeSum += period;
					cnt++;
					Log.d("Time Test", "Cost time is "+ (timeSum+0.0)/cnt);
				}
				else
					pointsAfterDenoise = pointsBeforeDenoise;
				
				double[] rpeaksDown = WaveletCWrapper.findRPeak(pointsAfterDenoise, 0.15, 1, 0.2);
				double[] rpeaksUp = WaveletCWrapper.findRPeak(pointsAfterDenoise, 0.15, 0, 0.1);
				Log.d(TAG, "up mode rpeaks: " + rpeaksUp[0] + ", down mode rpeaks: " +rpeaksDown[0]);
				if(rpeaksUp[0] > rpeaksDown[0]) upModeCnt++;
				if(rpeaksDown[0] > rpeaksUp[0]) downModeCnt++;
				
				if(upModeCnt >= downModeCnt){
					Log.d(TAG, "rpeaks is in up mode " + upModeCnt);
					ecgSfv.setRpeakParam(CurveSurfaceView.R_PEAK_MODE_UP);
					rpeaks = rpeaksUp;
				} else {
					Log.d(TAG, "rpeaks is in down mode " + downModeCnt);
					ecgSfv.setRpeakParam(CurveSurfaceView.R_PEAK_MODE_DOWN);
					rpeaks = rpeaksDown;
				}
				isRpeaksUpdated = true;
				
				ArrayList<Double> pointsArrayList = new ArrayList<Double>();
				for(int i=0; i<denoisNum; i++){
					pointsArrayList.add(pointsAfterDenoise[i] + avrg);
				}
				ecgSfv.addPoints(pointsArrayList);
			}
		}
	};
}