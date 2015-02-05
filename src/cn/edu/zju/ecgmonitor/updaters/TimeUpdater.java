package cn.edu.zju.ecgmonitor.updaters;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Looper;
import android.widget.Button;

public class TimeUpdater extends Timer {
	Button btn;
	Handler recordHandler;
	DecimalFormat df = new DecimalFormat("00");
	long recordTime = 0;
	int period = 1000;
	
	public TimeUpdater(Button btn) {
		this.btn = btn;
		
		recordHandler = new Handler(Looper.getMainLooper());
	}
	
	TimerTask updateTime = new TimerTask() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			recordHandler.post(new Runnable() {
				@Override
				public void run() {
					String second = df.format(recordTime%60);
					String minite = df.format(((recordTime-recordTime%60)/60)%60);
					String hour = df.format(recordTime/3600);
					btn.setText(hour+":"+minite+":"+second);
					recordTime += 1;
				}
			});
		}
	};
	
	public void schedule(long delay){
		this.schedule(updateTime, delay, period);
	}
	
	public void cancel(final int stringResId) {
		super.cancel();
		recordHandler.post(new Runnable() {
			@Override
			public void run() {
				btn.setText(stringResId);
			}
		});
	}
}
