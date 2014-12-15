package cn.edu.zju.ecgmonitor.updaters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DataRecorder {
	File recordFile;
	FileOutputStream fos;
	String fileName, recordDirPath;
	public DataRecorder(String recordDirPath){
		this.recordDirPath = recordDirPath;
	}
	
	public void startWriting(){
		Date currentDate = new Date(System.currentTimeMillis()); 
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.CHINA); 
		fileName = formatter.format(currentDate) + ".txt";
		
		recordFile = new File(recordDirPath, fileName);
		try {
			fos = new FileOutputStream(recordFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void writeToFile(String string){
		try {
			fos.write(string.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public void stopWriting(){
		try {
			if(fos != null) fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
