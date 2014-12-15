package cn.edu.zju.ecgmonitor.updaters;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import cn.edu.zju.curveplotter.CurveSurfaceView;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

public class RrIntervalsUpdater extends Timer{
	public final static String TAG = "RrIntervalsUpdater";
	
	ArrayList<Double> intervals = new ArrayList<Double>();
	int lastPoint = -1;
	double avrgInterval = 0.8;

	SignalProcessor signalProcessor;
	CurveSurfaceView rrIntervalsSfv;
	TextView tvHundred, tvTen, tvOne, tvBpm;
	ImageView ivHeart;
	AlphaAnimation alphaAnimation = new AlphaAnimation(1, (float) 0.5);
	
	public RrIntervalsUpdater(SignalProcessor signalProcessor, CurveSurfaceView rrIntervalsSfv, ImageView ivHeart,
								TextView tvHundred, TextView tvTen, TextView tvOne, TextView tvBpm) {
		this.signalProcessor = signalProcessor;
		this.rrIntervalsSfv = rrIntervalsSfv;
		
		this.ivHeart = ivHeart;
		this.tvHundred = tvHundred;
		this.tvTen = tvTen;
		this.tvOne = tvOne;
		this.tvBpm = tvBpm;
		
		alphaAnimation.setRepeatMode(Animation.REVERSE);
		alphaAnimation.setRepeatCount(Animation.INFINITE);
		alphaAnimation.setDuration(5000);
		ivHeart.startAnimation(alphaAnimation);
	}
	

	/**
	 * Update R-R intervals and heart beat from rpeaks[]
	 */
	TimerTask updateRrIntervals = new TimerTask() {
		@Override
		public void run() {
			if(signalProcessor.getRpeaksUpdated()){
				Log.d(TAG, "rpeaks are used");
				//add new points to R-R intevals curve
				double[] rpeaks = signalProcessor.getRpeaks();
				if(rpeaks[0]>0){
					ArrayList<Double> newIntervals = new ArrayList<Double>();
					for(int i=0; i<rpeaks[0]; i++){
						if(i*3+1 >= rpeaks.length) return;
						int point = (int) rpeaks[i*3+1];
						if(lastPoint >=0 ){
							int interval = point - lastPoint;
							if(i==0) interval += 1024;
							newIntervals.add(1.0*interval/500);
						}
						lastPoint = point;
					}
					ArrayList<Double> intervalsPoints = new ArrayList<Double>();
					for (Double newInterval : newIntervals) {
						if(newInterval<avrgInterval*1.5){
							intervalsPoints.add( (newInterval-0.3)/1.2 );
							intervals.add(newInterval);
						}
					}
					
					rrIntervalsSfv.addPoints(intervalsPoints);
				}
				
				//caculate heart beat
				if(intervals.size()>3){
					double sum = 0;
					for (int i = 0; i < intervals.size(); i++) {
						sum += intervals.get(i);
					}
					avrgInterval = sum/intervals.size();
					
					Handler heartbeatViewsHandler = new Handler(Looper.getMainLooper());
					Runnable updateHeartbeatViews = new Runnable() {
						@Override
						public void run() {
							int heartBeat = (int) (60.0/avrgInterval);
							tvHundred.setText(String.valueOf(heartBeat/100));
							tvTen.setText(String.valueOf(heartBeat/10));
							tvOne.setText(String.valueOf(heartBeat%10));
							tvHundred.setTextColor(Color.WHITE);
							tvTen.setTextColor(Color.WHITE);
							tvOne.setTextColor(Color.WHITE);
							tvBpm.setTextColor(Color.WHITE);
							if(heartBeat<100) tvHundred.setTextColor(Color.GRAY);
							
							//update the heart icon
							alphaAnimation.setDuration((long) (1000*avrgInterval/2));
							
						}
					};
					heartbeatViewsHandler.post(updateHeartbeatViews);
				}
				
				//Maintain the intervals arrayList, keep its size less than 30
				while(intervals.size()>30){
					intervals.remove(0);
				}
			}
		}
	};

	public void schedule(long delay, long period){
		this.schedule(updateRrIntervals, delay, period);
	}
}
