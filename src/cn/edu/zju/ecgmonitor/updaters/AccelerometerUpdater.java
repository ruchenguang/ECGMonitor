package cn.edu.zju.ecgmonitor.updaters;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

public class AccelerometerUpdater extends Timer{
	Context context;
	
	AccelerometerListener acceListener;
	float[] acceValues = new float[3];
	String state;
	DecimalFormat df = new DecimalFormat("0.00");
	TextView tvAcceX, tvAcceY, tvAcceZ, tvAcceS;
	
	public AccelerometerUpdater(Context context,
								TextView tvAcceX, TextView tvAcceY, 
								TextView tvAcceZ, TextView tvAcceS){
		this.tvAcceX = tvAcceX;
		this.tvAcceY = tvAcceY;
		this.tvAcceZ = tvAcceZ;
		this.tvAcceS = tvAcceS;
		
		acceListener = new AccelerometerListener(context);
	}
	
	Timer acceTimer = new Timer();
	TimerTask updateAcce = new TimerTask() {
		@Override
		public void run() {
			acceValues = acceListener.getAcceDate();
			state = "Rest";
			Handler acceTextHandler = new Handler(Looper.getMainLooper());
			Runnable updateAcceText = new Runnable() {
				@Override
				public void run() {
					String x = df.format(acceValues[0]);
					String y = df.format(acceValues[1]);
					String z = df.format(acceValues[2]);
					tvAcceX.setText(x);
					tvAcceY.setText(y);
					tvAcceZ.setText(z);
					tvAcceS.setText(state);
				}
			};
			acceTextHandler.post(updateAcceText);
		}
	};
	
	public void schedule(long delay, long period){
		this.scheduleAtFixedRate(updateAcce, delay, period);
	}
	
	/**
	 * Stop listening to the sensor and call super.cancel()
	 */
	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		super.cancel();
		acceListener.stopListening();
	}
}
