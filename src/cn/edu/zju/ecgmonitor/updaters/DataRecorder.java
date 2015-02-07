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
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()); 
		fileName = formatter.format(currentDate) + ".txt";
		
		recordFile = new File(recordDirPath, fileName);
		try {
			fos = new FileOutputStream(recordFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void writeToFile(byte[] bleRawBytes){
		//convert bytes to int [0, 1024], two bytes to one int
    	int[] bleRawInts = new int[bleRawBytes.length/2];
    	for(int i=0; i<bleRawBytes.length/2; i++){
    		bleRawInts[i] = (int) (bleRawBytes[2*i]&0xff)*128 + (bleRawBytes[2*i+1]&0xff);
        }
    	
		try {
			for(int i=0; i<bleRawInts.length; i++)
				fos.write((String.valueOf(bleRawInts[i]) + " ").getBytes());
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
