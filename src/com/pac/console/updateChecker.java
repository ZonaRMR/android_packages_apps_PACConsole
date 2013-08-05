package com.pac.console;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.pac.console.util.LocalTools;
import com.pac.console.util.RemoteTools;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class updateChecker extends Service {

	Handler handler = new Handler();

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();

		// INITIALIZE SCREEN RECEIVER
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		BroadcastReceiver mReceiver = new ScreenReceiver();
		registerReceiver(mReceiver, filter);
		Thread check = new Thread(checkOTA);
		check.start();
	}

	class ScreenReceiver extends BroadcastReceiver {
		// THANKS JASON
		public boolean wasScreenOn = true;

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				// DO WHATEVER YOU NEED TO DO HERE
				wasScreenOn = false;
			} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				if (!wasScreenOn){
					Thread check = new Thread(checkOTA);
					check.start();
				}
				wasScreenOn = true;
			}
		}
	}

	private Thread checkOTA = new Thread(new Runnable(){

		public void run() {
			// TODO Auto-generated method stub
			// checky McCheck
			Calendar currentDate = Calendar.getInstance();
			
			Date today = currentDate.getTime();
			Date finalDay = null;
			try {
				finalDay = new Date(Settings.System.getLong(getContentResolver(), "lastUpdate"));
			} catch (Exception e) {
				finalDay = new Date(0);
			}
			int numberOfHours = (int) ((finalDay.getTime() - today.getTime()) / (3600 * 1000));
			
			Log.d("SERVICE", "hours - " + numberOfHours);
			//TODO 6 hours passed? 
			if (numberOfHours+6 < 0){
				Settings.System.putLong(getContentResolver(), "lastUpdate", today.getTime());
				AsyncTask checkTast = new CheckRemote();
				String[] dev = { " " };
				dev[0] = (String) (LocalTools.getProp("ro.cm.device")!=null? LocalTools.getProp("ro.cm.device"): Build.DEVICE);
				checkTast.execute(dev);

			}
			//check for update on server adn reset time oonce done
			
			//Settings.System.putString(getContentResolver(), "lastUpdate", "hhmmddyymmdd");
			
			//else do nothing
			today = null;
			finalDay = null;
			currentDate = null;
		}
		
	});
	private class CheckRemote extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... arg0) {
			// TODO Auto-generated method stub
			String out = RemoteTools.checkRom(arg0[0]);
			return out;
		}

		@Override
		protected void onPostExecute(final String result) {

			if (result != null) {
				Log.d("REMOTE", "got this: " + result);
				String[] results = result.split(",");
				
				//data.putString("version", results[2]);
				String[] dlurl = results[0].split("/");
				//data.putString("file", dlurl[dlurl.length - 1]);
				//data.putString("url", results[0]);
				//data.putString("md5", results[3]);
				Settings.System.putString(getContentResolver(), "OTA_Update", result);
				// NOTIFY!
				
			}
		}
	}

}
