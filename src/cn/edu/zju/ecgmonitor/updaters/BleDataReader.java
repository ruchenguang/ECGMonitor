package cn.edu.zju.ecgmonitor.updaters;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;

import cn.edu.zju.curveplotter.CurveSurfaceView;
import cn.zju.edu.ecgsignalprocess.WaveletCWrapper;

public class BleDataReader extends Timer{
	public final static String TAG = "BleDataReader";
	
	String deviceName, deviceAddress;
	CurveSurfaceView ecgSfv;
	Context mContext;
	boolean isReading = true;	//save the state for pause function
	int denoisNum = 1024;
	boolean isDenoise = true;
	double avrg = 0.5;
	
	ArrayList<Double> pointsToAdd = new ArrayList<Double>();	//store ble raw data
	double[] rpeaks = new double[37];
	public double[] getRpeaks(){
		return rpeaks;
	}
	
	boolean isRpeaksUpdated = false;
	public void setRpeaksUpdated(boolean isRpeaksUpdated){
		this.isRpeaksUpdated = isRpeaksUpdated; 
	}
	
	TimerTask updateEcgSfv = new TimerTask() {
		@Override
		public void run() {
			if(pointsToAdd.size() >= denoisNum){
				double[] pointsBeforeDenoise = new double[denoisNum];
				for(int i=0; i<denoisNum; i++){
					pointsBeforeDenoise[i] = pointsToAdd.remove(0);
				}
				double[] pointsAfterDenoise;
				if(isDenoise){
					pointsAfterDenoise = WaveletCWrapper.signalDenoise(pointsBeforeDenoise, denoisNum);
					rpeaks = WaveletCWrapper.findRPeak(pointsAfterDenoise, 0.2, 1, 0.2);
					setRpeaksUpdated(true);
				}
				else
					pointsAfterDenoise = pointsBeforeDenoise;
				
				ArrayList<Double> pointsArrayList = new ArrayList<Double>();
				for(int i=0; i<denoisNum; i++){
					pointsArrayList.add(pointsAfterDenoise[i] + avrg);
				}
				ecgSfv.addPoints(pointsArrayList);
			}
		}
	};
	public BleDataReader(CurveSurfaceView sfv) {
		this.ecgSfv = sfv;
	}
	
	public void setDenoise(boolean isDenoise){
		this.isDenoise = isDenoise;
		avrg = isDenoise? 0.5:0.17;
		pointsToAdd = new ArrayList<Double>();
		for(int i=0; i<denoisNum; i++)
			pointsToAdd.add(0.5-avrg);
	}
	
	public void addRawData(int[] bleInts) {
		for(int i=0; i<bleInts.length; i++){
			pointsToAdd.add((bleInts[i]+0.0)/1024);
		}
	}
}