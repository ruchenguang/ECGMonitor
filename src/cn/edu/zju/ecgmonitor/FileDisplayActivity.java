package cn.edu.zju.ecgmonitor;

import cn.edu.zju.curveplotter.CurveSurfaceView;
import cn.edu.zju.ecgmonitor.updaters.AccelerometerUpdater;
import cn.edu.zju.ecgmonitor.updaters.DataRecorder;
import cn.edu.zju.ecgmonitor.updaters.RecordReader;
import cn.edu.zju.ecgmonitor.updaters.RrIntervalsUpdater;
import cn.edu.zju.ecgmonitor.updaters.SignalProcessor;
import cn.edu.zju.ecgmonitor.updaters.TimeUpdater;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class FileDisplayActivity extends Activity {
	public final static String TAG = "MainActivity";

	CurveSurfaceView ecgCurveSfv, rrIntervalsSfv;
	TextView tvHundred, tvTen, tvOne, tvBpm;
	TextView tvAcceX, tvAcceY, tvAcceZ, tvAcceS;
	TextView tvState;
	ImageView ivHeart;
	Button recordButton;
	
	RecordReader recordReader;
	SignalProcessor signalProcessor;
	RrIntervalsUpdater rrIntervalsUpdater;
	AccelerometerUpdater acceUpdater;
	DataRecorder dataRecorder;
	TimeUpdater timeUpdater;
	
	boolean isDisplaying = true;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        
        getActionBar().setTitle(R.string.app_name);
        //keep the screen on
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 
        
        //initialize rrintervlas update to update heart beat and rr intervals curve
        rrIntervalsSfv = (CurveSurfaceView) findViewById(R.id.curveSurfaceView1);
        rrIntervalsSfv.setPointOnScreen(20);
        rrIntervalsSfv.setRedrawParams(200, 1, 1, 1);
        rrIntervalsSfv.setScale(false);
        rrIntervalsSfv.setDisplayRpeaks(false);
        rrIntervalsSfv.setFindRpeak(false);
        
        ivHeart = (ImageView) findViewById(R.id.imageView1);
        tvHundred = (TextView) findViewById(R.id.TextViewHundred);
        tvTen = (TextView) findViewById(R.id.textViewTen);
        tvOne = (TextView) findViewById(R.id.TextViewOne);
        tvBpm = (TextView) findViewById(R.id.textViewBpm);
        
        //initialize record reader
        ecgCurveSfv = (CurveSurfaceView) findViewById(R.id.CurveSurfaceView01);
        ecgCurveSfv.setPointOnScreen(1024);
        ecgCurveSfv.setRedrawParams(40, 10, 2, 500);
        recordButton = (Button) findViewById(R.id.button1);
        recordButton.setText("Reading data...");
        timeUpdater = new TimeUpdater(recordButton);
        
        Intent intent = getIntent();
        String recordPath = intent.getStringExtra(SelectActivity.EXTRAS_FILE_PATH);
        String recordName = intent.getStringExtra(SelectActivity.EXTRAS_FILE_NAME);
        
        signalProcessor = new SignalProcessor(ecgCurveSfv, true);
        signalProcessor.schedule(100, 100);
        recordReader = new RecordReader(this, signalProcessor, timeUpdater, recordPath + "/" + recordName);
        recordReader.schedule(0, 100);
        rrIntervalsUpdater = new RrIntervalsUpdater(signalProcessor, rrIntervalsSfv, ivHeart, 
        											tvHundred, tvTen, tvOne, tvBpm);
        rrIntervalsUpdater.schedule(500, 200);    
        
        //initialize acceUpdater to update acce sensors data to the textviews
        tvAcceX = (TextView) findViewById(R.id.TextViewAcceX);
        tvAcceY = (TextView) findViewById(R.id.TextViewAcceY);
        tvAcceZ = (TextView) findViewById(R.id.TextViewAcceZ);
        tvAcceS = (TextView) findViewById(R.id.TextViewAcceS);
        acceUpdater = new AccelerometerUpdater(this, tvAcceX, tvAcceY, tvAcceZ, tvAcceS);
        acceUpdater.schedule(500, 500);
        
        tvState = (TextView) findViewById(R.id.textViewState);
        tvState.setText(R.string.state_reading_date_from_file);
    }
	
    @Override
    protected void onPause() {
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
		recordReader.cancel();
		signalProcessor.cancel();
		rrIntervalsUpdater.cancel();
		acceUpdater.cancel();
		timeUpdater.cancel();
    }
}
