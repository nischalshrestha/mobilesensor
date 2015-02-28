package com.aware.plugin.mobile_sensor.activities;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.aware.plugin.mobile_sensor.R;
import com.aware.plugin.mobile_sensor.R.id;
import com.aware.plugin.mobile_sensor.R.layout;
import com.aware.plugin.mobile_sensor.R.menu;
import com.aware.plugin.mobile_sensor.observers.NoiseObserver;
import com.aware.plugin.noise_level.NoiseLevel_Provider.NoiseLevel;
import com.infteh.comboseekbar.ComboSeekBar;

@SuppressLint("NewApi")
public class NoiseCalibrationActivity extends ActionBarActivity {

	private static int quietCount = -1;
	private static int middleCount = -1;
	private static int loudCount = -1;
	private static int currentSound = 0;
	private static int count = 0;
	private static String currentTitle = "";
	private static boolean soundMeterRead;
	private static double[] quietSounds;
	private static double[] middleSounds;
	private static double[] loudSounds;
	private static double[] quietReading;
	private static double[] middleReading;
	private static double[] loudReading;
	private static EditText input;
	public static SimpleRegression s;
	
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_noise_calibration);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()	
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		//Begin walkthrough with a dialogue
		soundMeterRead = true;
		showIntroDialog(NoiseCalibrationActivity.this);
		initialize();
	}
	
	public void initialize(){
		quietSounds = new double[3];
		middleSounds = new double[3];
		loudSounds = new double[3];
		quietReading = new double[3];
		middleReading = new double[3];
		loudReading = new double[3];
		s = new SimpleRegression();
	}
	
	/**
	 * Begins the walkthrough
	 * @param context
	 */
	@SuppressWarnings("deprecation")
	public void showIntroDialog(Context context){
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();
		alertDialog.setTitle("Get ready to calibrate!");
	    alertDialog.setMessage("Start by clicking the Record Quiet Sound button");
	    alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Button b = (Button) findViewById(R.id.toggleQuietButton);
				b.setEnabled(true);
	        }
	    });
	    alertDialog.setCancelable(false);
	    alertDialog.show();
	}
	
	/**
	 * This dialogue prompts the user to play 3 different sounds and enter in the Db reading
	 * of the sound meter for each
	 * @param title
	 * @param message
	 * @param context
	 * @param id
	 */
	@SuppressWarnings("deprecation")
	public void showMainDialog(String title, String message, Context context, int id){
		if(id <= 2){
			soundMeterRead = false;
			currentTitle = title;
			count = id;
			AlertDialog alertDialog = new AlertDialog.Builder(context).create();
			alertDialog.setTitle(title);
			alertDialog.setMessage(message);
			alertDialog.setButton("RECORD", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
//		        	if(!soundMeterRead){
		        	new Handler().postDelayed(new Runnable() {
    				    public void run() {
    				    	Cursor noise = getContentResolver().query(NoiseLevel.CONTENT_URI, null, null, null, NoiseLevel.TIMESTAMP + " DESC");
//    				    	Log.d("Calibration","count: "+noise.getCount());
    				    	if( noise!= null && noise.moveToFirst()) {
    				    		if(noise.getCount() >= 3){
//	    				    		for(int i = 0; i < 3; i++){
//	    				    			//quiet samples
	    				    			if(quietCount == 0 && middleCount == -1 && loudCount == -1){
//	    				    				Log.d("Calibration","quiet noise: "+noise.getDouble(noise.getColumnIndex(NoiseLevel.NOISE)));
	    				    				quietSounds[count] = noise.getDouble(noise.getColumnIndex(NoiseLevel.NOISE));
//	//    				    			//middle samples
	    				    			}else if(quietCount == 0 && middleCount == 0 && loudCount == -1){
//	    				    				Log.d("Calibration","middle noise: "+noise.getDouble(noise.getColumnIndex(NoiseLevel.NOISE)));
	    				    				middleSounds[count] = noise.getDouble(noise.getColumnIndex(NoiseLevel.NOISE));
//	//    				    			//loud samples
	    				    			}else if(quietCount == 0 && middleCount == 0 && loudCount == 0){
//	    				    				Log.d("Calibration","loud noise: "+noise.getDouble(noise.getColumnIndex(NoiseLevel.NOISE)));
	    				    				loudSounds[count] = noise.getDouble(noise.getColumnIndex(NoiseLevel.NOISE));
	    				    			}
//	    				    			SimpleRegression s = new SimpleRegression();
//	    				    		}
    				    		}
    				    	} if(noise != null && ! noise.isClosed() ) noise.close();
    				    	showEnterDecibelDialog("Sound Meter Reading","Please enter the decibels recorded by the sound meter",NoiseCalibrationActivity.this,count++);
    				    }
    				}, 5000);
		        }
		     });
		     alertDialog.setCancelable(false);
		     alertDialog.show();
		} else{
			soundMeterRead = true;
			if(currentSound == 0){
				recordQuietSound(getCurrentFocus());
			} else if(currentSound == 1){
				recordMiddleSound(getCurrentFocus());
			} else if(currentSound == 2){
				recordLoudSound(getCurrentFocus());
			}
		}
	}
	
	/**
	 * The prompt for the quiet sound which leads in to the middle sound prompt
	 * @param view
	 */
	@SuppressWarnings("deprecation")
	public void recordQuietSound(View view){
		if(soundMeterRead){
			if(quietCount == -1){
				new Handler().postDelayed(new Runnable() {
				    public void run() {
				    	showMainDialog("Record a quiet sound","When you're ready press RECORD and play sound for 5s",NoiseCalibrationActivity.this,++quietCount);
				    }
				}, 100);
			} else if(quietCount == 0){
				AlertDialog alertDialog = new AlertDialog.Builder(NoiseCalibrationActivity.this).create();
				alertDialog.setTitle("Thank you!");
			    alertDialog.setMessage("Now click on the Record Middle Sound button");
			    alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) {
						currentSound++;
						soundMeterRead = true;
						Button b = (Button) findViewById(R.id.toggleQuietButton);
						b.setEnabled(false);
						Button b2 = (Button) findViewById(R.id.toggleMiddleButton);
						b2.setEnabled(true);
			        }
			    });
			    alertDialog.setCancelable(false);
			    alertDialog.show();
			}
		}
	}
	
	/**
	 * The prompt for the middle sound which leads in to the loud sound prompt
	 * @param view
	 */
	@SuppressWarnings("deprecation")
	public void recordMiddleSound(View view){
		if(soundMeterRead){
			if(middleCount == -1){
				new Handler().postDelayed(new Runnable() {
				    public void run() {
				    	showMainDialog("Record a middle sound","When you're ready press RECORD and play sound for 5s",NoiseCalibrationActivity.this,++middleCount);
				    }
				}, 100);
			} else if(middleCount == 0){
				AlertDialog alertDialog = new AlertDialog.Builder(NoiseCalibrationActivity.this).create();
				alertDialog.setTitle("Thank you!");
			    alertDialog.setMessage("Now click on the Record Loud Sound button");
			    alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) {
			        	currentSound++;
						soundMeterRead = true;
						Button b = (Button) findViewById(R.id.toggleMiddleButton);
						b.setEnabled(false);
						Button b2 = (Button) findViewById(R.id.toggleLoudButton);
						b2.setEnabled(true);
			        }
			    });
			    alertDialog.setCancelable(false);
			    alertDialog.show();
			}
		}
	}

	/**
	 * The prompt for the loud sound which ends the calibration process
	 * @param view
	 */
	@SuppressWarnings("deprecation")
	public void recordLoudSound(View view){	
		if(soundMeterRead){
			if(loudCount == -1){
				new Handler().postDelayed(new Runnable() {
				    public void run() {
				    	showMainDialog("Record a loud sound","When you're ready press RECORD and play sound for 5s",NoiseCalibrationActivity.this,++loudCount);
				    }
				}, 100);
			} else if(loudCount == 0){
				AlertDialog alertDialog = new AlertDialog.Builder(NoiseCalibrationActivity.this).create();
				alertDialog.setTitle("Thank you!");
			    alertDialog.setMessage("You have successfully completed the calibration! Goodbye!");
			    alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) {
			        	currentSound++;
						soundMeterRead = true;
						Button b = (Button) findViewById(R.id.toggleLoudButton);
						b.setEnabled(false);
						Button b2 = (Button) findViewById(R.id.toggleResetButton);
						b2.setEnabled(true);
						
//						Log.d("Calibration","size of quit sounds: "+quietSounds.length);
//						Log.d("Calibration","size of quit readings: "+quietReading.length);
//						
//						Log.d("Calibration","size of middle sounds: "+middleSounds.length);
//						Log.d("Calibration","size of middle readings: "+middleReading.length);
//						
//						Log.d("Calibration","size of loud sounds: "+loudSounds.length);
//						Log.d("Calibration","size of loud readings: "+loudReading.length);
//						
//						Log.d("Calibration","Setting function..");
						Intent noise = new Intent(NoiseCalibrationActivity.this, com.aware.plugin.noise_level.Plugin.class);
					    startService(noise);
						NoiseObserver.s = s;
			        }
			    });
			    alertDialog.setCancelable(false);
			    alertDialog.show();
			}
		}
	}
	
	public static SimpleRegression returnFunction(){
		return s;
	}
	
	/**
	 * Resets the calibration process and starts over
	 * @param view
	 */
	@SuppressWarnings("deprecation")
	public void reset(View view){
		new Handler().postDelayed(new Runnable() {
		    public void run() {
		    	AlertDialog alertDialog = new AlertDialog.Builder(NoiseCalibrationActivity.this).create();
				alertDialog.setTitle("Reset");
			    alertDialog.setMessage("Are you sure you want to reset and start over?");
			    alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) {
			    		currentSound = 0;
			    		quietCount = -1;
			    		middleCount = -1;
			    		loudCount = -1;
			    		initialize();
			    		showIntroDialog(NoiseCalibrationActivity.this);
						Button b = (Button) findViewById(R.id.toggleResetButton);
						s.clear();
						b.setEnabled(false);
			        }
			    });
			    alertDialog.show();
		    }
		}, 100);
	}

	/**
	 * Has the user enter in the dB read by the sound meter for a sound
	 * @param title
	 * @param message
	 * @param context
	 * @param sample
	 */
	@SuppressWarnings("deprecation")
	public void showEnterDecibelDialog(String title, String message, Context context, int sample){
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();
//		count = sample;
		alertDialog.setTitle(title);
	     alertDialog.setMessage(message);
	     alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) {
	        	soundMeterRead = true;
	        	//quiet samples
//	        	Log.d("Calibration","reading index: "+(count-1));
    			if(quietCount == 0 && middleCount == -1 && loudCount == -1){
    				quietReading[count-1] = Double.parseDouble(input.getText().toString());
    				s.addData(quietSounds[count-1], quietReading[count-1]);
//    				Log.d("Calibration","quiet noise reading: "+quietReading[count-1]);
    			//middle samples
    			}else if(quietCount == 0 && middleCount == 0 && loudCount == -1){
    				middleReading[count-1] = Double.parseDouble(input.getText().toString());
    				s.addData(middleSounds[count-1], middleReading[count-1]);
//    				Log.d("Calibration","middle noise reading: "+middleReading[count-1]);
    			//loud samples
    			}else if(quietCount == 0 && middleCount == 0 && loudCount == 0){
    				loudReading[count-1] = Double.parseDouble(input.getText().toString());
    				s.addData(loudSounds[count-1], loudReading[count-1]);
//    				Log.d("Calibration","loud noise reading: "+loudReading[count-1]);
    			}
	        	if(count <= 1){
	        		showMainDialog(currentTitle, "Please repeat", NoiseCalibrationActivity.this, count);
	        	}else{
	        		showMainDialog(currentTitle, "One last time", NoiseCalibrationActivity.this, count);
	        	}
	        }
	     });
		  input = new EditText(context);
		  LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
		                        LinearLayout.LayoutParams.MATCH_PARENT,
		                        LinearLayout.LayoutParams.MATCH_PARENT);
		  input.setLayoutParams(lp);
		  alertDialog.setView(input);
		  alertDialog.setCancelable(false);
		  alertDialog.show();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.nosie_calibration, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		
		public Button quietButton;
		
		public PlaceholderFragment() {
		}

		/**
		 * Sets up the buttons required for the calibration
		 */
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(
					R.layout.fragment_noise_calibration, container, false);
			Button recalibrateButton = (Button) rootView.findViewById(R.id.toggleResetButton);
			quietButton = (Button) rootView.findViewById(R.id.toggleQuietButton);
			Button middleButton = (Button) rootView.findViewById(R.id.toggleMiddleButton);
			Button loudButton = (Button) rootView.findViewById(R.id.toggleLoudButton);
			quietButton.setEnabled(false);
			middleButton.setEnabled(false);
			loudButton.setEnabled(false);
			recalibrateButton.setEnabled(false);
			return rootView;
		}
		
	}

}
