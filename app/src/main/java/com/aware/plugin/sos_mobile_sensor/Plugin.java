package com.aware.plugin.sos_mobile_sensor;


import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.provider.CalendarContract.Reminders;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.Screen;
import com.aware.plugin.modeoftransportation.MoT_Provider.MoT;
import com.aware.plugin.sos_mobile_sensor.MobileSensor_Provider.MobileSensor_Data;
import com.aware.plugin.sos_mobile_sensor.activities.Settings;
import com.aware.plugin.sos_mobile_sensor.calendar.CalendarEvent;
import com.aware.plugin.sos_mobile_sensor.calendar.CalendarObserver;
import com.aware.plugin.sos_mobile_sensor.observers.ESMObserver;
import com.aware.plugin.sos_mobile_sensor.observers.InstallationsObserver;
import com.aware.plugin.sos_mobile_sensor.observers.MessageObserver;
import com.aware.plugin.sos_mobile_sensor.observers.MoTObserver;
import com.aware.plugin.sos_mobile_sensor.observers.MultitaskingObserver;
import com.aware.plugin.sos_mobile_sensor.observers.NoiseObserver;
import com.aware.plugin.sos_mobile_sensor.observers.ScreenObserver;
import com.aware.plugin.sos_mobile_sensor.observers.VoiceCallObserver;
import com.aware.providers.Applications_Provider.Applications_Foreground;
import com.aware.providers.Communication_Provider.Calls_Data;
import com.aware.providers.Communication_Provider.Messages_Data;
import com.aware.providers.ESM_Provider.ESM_Data;
import com.aware.providers.Installations_Provider.Installations_Data;
import com.aware.utils.Aware_Plugin;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


public class Plugin extends Aware_Plugin {
	
	/**
	* Broadcasted event: the context of this sensor is being shared
	*/
	public static final String ACTION_AWARE_PLUGIN_SOS_MOBILE_SENSOR = "ACTION_AWARE_PLUGIN_SOS_MOBILE_SENSOR";
	/**
	* Broadcasted event: the user has turned on his phone
	*/
	public static final String STATUS_PLUGIN_SOS_MOBILE_SENSOR = "status_plugin_sos_mobile_sensor";
	/**
	* Extra (int): whether the user was multitasking or not 
	*/
	public static final String EXTRA_MULTITASKING = "multitasking";
	/**
	* Extra (int): whether the user has changed mode of transportation or not 
	*/
	public static final String EXTRA_MOT = "mode_of_transportation";
	/**
	* Extra (double): whether the user has changed mode of transportation or not 
	*/
	public static final String EXTRA_ABS_NOISE = "noise_level";
//	/**
//	* Extra (double): whether the user has changed mode of transportation or not 
//	*/
//	public static final String EXTRA_REL_NOISE = "noise_level";
	/**
	* Extra (int): whether the user was just on the phone or not
	*/
	public static final String EXTRA_CALLS = "voice_call";
	/**
	* Extra (int): whether the user has sent/received a text message 
	*/
	public static final String EXTRA_MESSAGING = "messaging";
	/**
	* Extra (int): whether the user has just installed/uninstalled an app 
	*/
	public static final String EXTRA_INSTALLATIONS = "installations";
	/**
	* Extra (int): whether the user has an upcoming event 
	*/
	public static final String EXTRA_CALENDAR = "calendar";
	/**
	* Extra (int): whether the user is using an email app or not 
	*/
	public static final String EXTRA_EMAIL = "email";
	
	/**
	 * A multi-thread handler manager and the relevant sensor threads.
	 */
	public static HandlerThread thread_multitasking;
	public static HandlerThread thread_mot;
	public static HandlerThread thread_phone;
	public static HandlerThread thread_messages;
	public static HandlerThread thread_esm;
	public static HandlerThread thread_calendar;
	public static HandlerThread thread_screen;
	public static HandlerThread thread_cal_alarm;
	public static HandlerThread thread_install;
	
	public static Handler thread_sensor_screen = null;
	public static Handler thread_sensor_multi = null;
	public static Handler thread_sensor_mot = null;
	public static Handler thread_sensor_noise = null;
//	public static Handler thread_sensor_ambient_noise = null;
	public static Handler thread_sensor_phone = null;
	public static Handler thread_sensor_messages = null;
	public static Handler thread_sensor_install = null;
	
	public static Handler thread_observer_calendar = null;
	public static Handler thread_sensor_esm = null;
	public static Handler thread_esm_alarm = null;
	public static Handler thread_calendar_alarm = null;
	private ArrayList<Handler> thread_calendar_alarms;
	private ArrayList<Runnable> event_alarms;
	private static Handler thread_calSetup;
	
	/**
	 * The Context Observers
	 */
	//BroadcastReceiver that will receiver screen ON/OFF events from AWARE
	public ScreenObserver screenListener;
	//Multitasking
	public static MultitaskingObserver multitask_observer = null;
	//Mode of Transportation
	public static MoTObserver mot_observer = null;
	//Noise Level
	public static NoiseObserver noise_observer = null;
	//Calendar
//	public static AmbientNoiseObserver ambient_noise_observer = null;
	//Voice Call
	public static VoiceCallObserver phone_observer = null;
	//SMS
	public static MessageObserver messages_observer = null;
	//ESM
	public static ESMObserver esm_observer = null;
	//Calendar
	public static CalendarObserver calendar_observer = null;
	//Installations
	public static InstallationsObserver install_observer = null;	
	
	/**
	 * Private variables that hold the latest values to be shared whenever ACTION_AWARE_CURRENT_CONTEXT 
	 * is broadcasted
	 */
	public static double noise_level;
	public static int multitasking, voice_messaging, text_messaging, email, calendar_event, ambient_noise, installations;
	/** Screen variables */
	public static long screenOnTime;
	public static boolean screenIsOn;
	public static String movement = "still";
	/** MoT variables */
	public static long lastNegativeESM;
	private Calendar cal;
	private static final String morning = "morning";
	private static final String evening = "evening";
	private int noonESM = 0;
	private int eveningESM = 0;
	//Constants
	public static final int MORNING = 8;
	public static final int NOON = 12;
	public static final int EVENING = 20;
	/** Calendar variables */
	public static ArrayList<CalendarEvent> eventList = new ArrayList<CalendarEvent>();
	private int lastCalendarAlarm;
	public static long lastCalendarESM;
	//Prompt every half-hour
	public static final long throttle = 1800000;
	public static long lastThrottleTime;
	public static String rating;
	//Initial user input variables
	public static String participantID;
	public static String deviceID;
	public static String loudnessRating;
	//Loggingn filename
	public static final String outputFile = "/sosm-out.csv";
	
	/**
	 * Code here when add-on is turned on.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
        TAG = "AWARE::Mobile Sensor";

        //Our provider tables
        DATABASE_TABLES = MobileSensor_Provider.DATABASE_TABLES;
        //Our table fields
        TABLES_FIELDS = MobileSensor_Provider.TABLES_FIELDS;
        //Our provider URI
        CONTEXT_URIS = new Uri[]{ MobileSensor_Data.CONTENT_URI };

		startAwareSensors();
		initializeThreads();
		startAwarePlugins();
		startAlarms();
		startContentObservers();
		//Shares this plugin’s context to AWARE and applications
		CONTEXT_PRODUCER = new ContextProducer() {
			@SuppressLint("SimpleDateFormat")
			@Override
			public void onContext() {
				if(null != participantID && Plugin.screenIsOn && Settings.initialized){
					//change this to debug any sensor and if they're being inserted when sensed
					Calendar newCal = new GregorianCalendar();
					SimpleDateFormat sdf = new SimpleDateFormat("M-dd-yyyy");
					String date = sdf.format(newCal.getTime());
//					Log.d("Time","Date: "+date);
					SimpleDateFormat sdf2 = new SimpleDateFormat("k:mm:ss");
					long timestamp = System.currentTimeMillis();
					String timestampString = sdf2.format(new Date(System.currentTimeMillis()));
					deviceID = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID);
					String context = date+","+timestampString+","+participantID+","+multitasking+","+movement+","+noise_level+","+voice_messaging
							+","+text_messaging+","+email+","+calendar_event+","+installations+","+rating+","+loudnessRating;
					createOutput(context);
					ContentValues context_data = new ContentValues();
					context_data.put(MobileSensor_Data.TIMESTAMP, timestamp);
					context_data.put(MobileSensor_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
					context_data.put(MobileSensor_Data.MULTITASKING, multitasking);
					context_data.put(MobileSensor_Data.MOT, movement);
					context_data.put(MobileSensor_Data.NOISE_LEVEL, noise_level);
//					context_data.put(MobileSensor_Data.AMBIENT_NOISE, ambient_noise);
					context_data.put(MobileSensor_Data.CALLS, voice_messaging);
					context_data.put(MobileSensor_Data.MESSAGING, text_messaging);
					context_data.put(MobileSensor_Data.CALENDAR, calendar_event);
					context_data.put(MobileSensor_Data.EMAIL, email);
					context_data.put(MobileSensor_Data.INSTALLATIONS, installations);
					//insert data to table MobileSensor_Data
					getContentResolver().insert(MobileSensor_Data.CONTENT_URI, context_data);
					//share context
					Intent sharedContext = new Intent(ACTION_AWARE_PLUGIN_SOS_MOBILE_SENSOR);
					sharedContext.putExtra(EXTRA_MULTITASKING, multitasking);
					sharedContext.putExtra(EXTRA_MOT, movement);
					sharedContext.putExtra(EXTRA_ABS_NOISE, noise_level);
//					sharedContext.putExtra(EXTRA_REL_NOISE, ambient_noise);
					sharedContext.putExtra(EXTRA_CALLS, voice_messaging);
					sharedContext.putExtra(EXTRA_MESSAGING, text_messaging);
					sharedContext.putExtra(EXTRA_CALENDAR, calendar_event);
					sharedContext.putExtra(EXTRA_EMAIL, email);
					sharedContext.putExtra(EXTRA_INSTALLATIONS, installations);
					sendBroadcast(sharedContext);
					rating = null;
					loudnessRating = null;
				}
			}
		};
	}
	
	private void initializeThreads(){
		thread_multitasking = new HandlerThread("Multitasking");
		thread_mot = new HandlerThread("MoT");
		thread_phone = new HandlerThread("Phone");
		thread_messages = new HandlerThread("Messaging");
		thread_esm = new HandlerThread("ESM");
		thread_calendar = new HandlerThread("Calendar");
		thread_screen = new HandlerThread("Screen");
		thread_cal_alarm = new HandlerThread("Calendar_Alarm");
		thread_install = new HandlerThread("Installations");
	}
	
	public void startAwareSensors(){
		Intent aware = new Intent(this, Aware.class);
	    startService(aware);
	    //Activate core sensors
		Aware.setSetting(getApplicationContext(), STATUS_PLUGIN_SOS_MOBILE_SENSOR, true);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_SCREEN, true);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_INSTALLATIONS, true);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_APPLICATIONS, true);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_CALLS, true);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_MESSAGES, true);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);
		Aware.setSetting(this, Aware_Preferences.DEBUG_FLAG, true);
		Intent apply = new Intent(Aware.ACTION_AWARE_REFRESH);
		sendBroadcast(apply);
	}
	
	public void startAwarePlugins(){
		//Activate plugins
        //Denzil: this plugin no longer is supported. Use Google Activity Recognition plugin instead
//  		Intent mot = new Intent(this, com.aware.plugin.modeoftransportation.Plugin.class);
//  	    startService(mot);
//  		Intent rel_noise = new Intent(this, com.aware.plugin.ambient_noise.Plugin.class);
//  	    startService(rel_noise);
		//Initialize Screen/Multitasking/MoT/Noise vars
		screenOnTime = System.currentTimeMillis();
		lastNegativeESM = screenOnTime;
		lastThrottleTime = screenOnTime;
		screenIsOn = true;
	}
	
	/**
	 * Calendar events alarm and MoT alarm
	 */
	public void startAlarms(){
		if(!Settings.initialized){
			thread_cal_alarm.start();
		}else{
			thread_cal_alarm = new HandlerThread("Calendar_Alarm");
			thread_cal_alarm.start();
		}
		thread_calendar_alarms = new ArrayList<Handler>();
		event_alarms = new ArrayList<Runnable>();
		Plugin.lastCalendarESM = screenOnTime;
		//Alarm for the MoT Esm question
		thread_esm_alarm = new Handler(thread_cal_alarm.getLooper());
		thread_esm_alarm.postDelayed(esmAlarm, 5000);
		//Alarm for setting up events for Calendar
		thread_calSetup = new Handler(thread_cal_alarm.getLooper());
		thread_calSetup.postDelayed(setUpEvents, 1000);
	}
	
	/**
	 * Initialize the multithread handler manager for ContentObservers and
	 * initialize the context observers for the sensors
	 */
	@SuppressLint("NewApi")
	public void startContentObservers(){
		//Register the sensor handler thread with the handler managers
		
		//Multitasking
		thread_multitasking.start();
		thread_sensor_multi = new Handler(thread_multitasking.getLooper());
		multitask_observer = new MultitaskingObserver(thread_sensor_multi, this);
		MultitaskingObserver.lastMultiESM = screenOnTime;
		MultitaskingObserver.lastEmailESM = screenOnTime;
		
		//MoT
		thread_mot.start();
		thread_sensor_mot = new Handler(thread_mot.getLooper());
		mot_observer = new MoTObserver(thread_sensor_mot, this);
		
		//NoiseLevel
//		thread_sensor_noise = new Handler(thread_handler.getLooper());
		//Initialize the context observers with the sensor thread for performance
//		noise_observer = new NoiseObserver(thread_sensor_noise, this);
//		NoiseObserver.lastNoisyESM = screenOnTime;
		
		//Phone 
		thread_phone.start();
		thread_sensor_phone = new Handler(thread_phone.getLooper());
		phone_observer = new VoiceCallObserver(thread_sensor_phone, this);
		VoiceCallObserver.lastVoiceESM = screenOnTime;
		
		//Text Messaging
		thread_messages.start();
		thread_sensor_messages = new Handler(thread_messages.getLooper());
		messages_observer = new MessageObserver(thread_sensor_messages, this);
		MessageObserver.lastMessageESM = screenOnTime;
		
		//Installations
		thread_install.start();
		thread_sensor_install = new Handler(thread_install.getLooper());
		install_observer = new InstallationsObserver(thread_sensor_install, this);
		
		//ESM
		thread_esm.start();
		thread_sensor_esm = new Handler(thread_esm.getLooper());
		esm_observer = new ESMObserver(thread_sensor_esm, this);
		
		//Calendar
		thread_calendar.start();
		thread_observer_calendar = new Handler(thread_calendar.getLooper());
		calendar_observer = new CalendarObserver(thread_observer_calendar, this);
		
		//Screen
		thread_screen.start();
		thread_sensor_screen = new Handler(thread_screen.getLooper());
		//create a context filter for screen
		IntentFilter screenFilter = new IntentFilter();
		screenFilter.addAction(Screen.ACTION_AWARE_SCREEN_ON);
		screenFilter.addAction(Screen.ACTION_AWARE_SCREEN_OFF);
		screenListener = new ScreenObserver(this);
		
		//Ambient noise
//		thread_sensor_ambient_noise = new Handler(thread_handler.getLooper());
//		ambient_noise_observer = new AmbientNoiseObserver(thread_sensor_ambient_noise, this);
//		ambient_noise_observer.setLastAmbientESM(screenOnTime);
		
		//Ask Android to register our context receiver
		registerReceiver(screenListener, screenFilter, null, thread_sensor_screen);
		//Start listening to changes on the Applications_Foreground, MoT, and NoiseLevel databases
		getContentResolver().registerContentObserver(Applications_Foreground.CONTENT_URI, true, multitask_observer);
		getContentResolver().registerContentObserver(MoT.CONTENT_URI, true, mot_observer);
		getContentResolver().registerContentObserver(Installations_Data.CONTENT_URI, true, install_observer);
//		getContentResolver().registerContentObserver(NoiseLevel.CONTENT_URI, true, noise_observer);
//		getContentResolver().registerContentObserver(AmbientNoise_Data.CONTENT_URI, true, ambient_noise_observer);
		getContentResolver().registerContentObserver(Calls_Data.CONTENT_URI, true, phone_observer);
		getContentResolver().registerContentObserver(Messages_Data.CONTENT_URI, true, messages_observer);
		getContentResolver().registerContentObserver(ESM_Data.CONTENT_URI, true, esm_observer);
		getContentResolver().registerContentObserver(Reminders.CONTENT_URI, true, calendar_observer);
	}
		
	/**
	 * Code here when add-on is turned off.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
//		Log.d("service","mobile sensor being destroyed!");
		participantID = null;
		deviceID = null;
		unregisterReceiver(screenListener);
		//unregister our Applications, MoT, Noise Level, Communications context observers from Android
		getContentResolver().unregisterContentObserver(multitask_observer);
		getContentResolver().unregisterContentObserver(mot_observer);
//		getContentResolver().unregisterContentObserver(noise_observer);
		getContentResolver().unregisterContentObserver(phone_observer);
		getContentResolver().unregisterContentObserver(messages_observer);
		getContentResolver().unregisterContentObserver(esm_observer);
		getContentResolver().unregisterContentObserver(calendar_observer);
		getContentResolver().unregisterContentObserver(install_observer);
//		getContentResolver().unregisterContentObserver(ambient_noise_observer);
		
		//stop listening to changes in the database(s)
		thread_sensor_screen.removeCallbacksAndMessages(null);
		thread_sensor_multi.removeCallbacksAndMessages(null);
		thread_sensor_mot.removeCallbacksAndMessages(null);
//		thread_sensor_noise.removeCallbacksAndMessages(null);
		thread_sensor_messages.removeCallbacksAndMessages(null);
		thread_sensor_esm.removeCallbacksAndMessages(null);
		thread_esm_alarm.removeCallbacksAndMessages(null);
		thread_observer_calendar.removeCallbacksAndMessages(null);
		thread_sensor_install.removeCallbacksAndMessages(null);
		
		if(thread_calendar_alarms.size() != 0){
			for(Handler h : thread_calendar_alarms){
				h.removeCallbacksAndMessages(null);
			}
			thread_calendar_alarms.clear();
		}
		
		//Kill threads
		thread_multitasking.interrupt();
		thread_mot.interrupt();
		thread_messages.interrupt();
		thread_phone.interrupt();
		thread_esm.interrupt();
		thread_calendar.interrupt();
		thread_screen.interrupt();
		thread_install.interrupt();
		thread_cal_alarm.interrupt();
		thread_multitasking = null;
		thread_mot = null;
		thread_messages = null;
		thread_phone = null;
		thread_esm = null;
		thread_calendar = null;
		thread_screen = null;
		thread_install = null;
		thread_cal_alarm = null;
		
		//Deactivate the sensors
		Aware.setSetting(getApplicationContext(), STATUS_PLUGIN_SOS_MOBILE_SENSOR, false);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_SCREEN, false);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_INSTALLATIONS, false);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_APPLICATIONS, false);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_CALLS, false);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_MESSAGES, false);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, false);
		Intent refresh = new Intent(Aware.ACTION_AWARE_REFRESH);
		sendBroadcast(refresh);
		//Deactive the plugins
		
//		Intent noise = new Intent(this, com.aware.plugin.noise_level.Plugin.class);
//	    stopService(noise);
  		Intent mot = new Intent(this, com.aware.plugin.modeoftransportation.Plugin.class);
  	    stopService(mot);
//  		Intent rel_noise = new Intent(this, com.aware.plugin.ambient_noise.Plugin.class);
//  	    stopService(rel_noise);
	}
	
	/**
	 * This is an alarm for the retrospective question for mode of transportation 
	 * that could have been detected earlier in the day
	 */
	@SuppressLint("SimpleDateFormat")
	private Runnable esmAlarm = new Runnable(){
		/**
		 * This method will make sure that this Runnable runs at appropriate times
		 * according to the time of the day, to run the retrospective questionnaires
		 * for movement sensor
		 */
		@Override
		public void run() {
			//Get Calendar to determine current date/time and figure out the date/time for the
			//next alarm
//			Log.d("Thread","ESM: Is this the main thread?"+(Looper.myLooper() == Looper.getMainLooper()));
			cal = Calendar.getInstance();
			String time = "";
			int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
			//Prepare the filter for query and set the mSelectionArgs below
			String[] projections = new String[]{"mode_of_transportation"};
			String[] mSelectionArgs = new String[2];
			String mSelection = "TIMESTAMP > ? AND TIMESTAMP < ?";
			//This will be our Calendar for the next alarm
			cal = Calendar.getInstance(); 
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			//If the app has just been installed and/or no data is available for previous time periods
			if(hourOfDay >= 0 && hourOfDay < NOON){ //At or after 12am and before noon, set to noon
				cal.set(Calendar.HOUR_OF_DAY, NOON);
				thread_esm_alarm.postDelayed(this, (cal.getTimeInMillis()-System.currentTimeMillis()));
			} else if(hourOfDay >= EVENING){ //At or after 8pm, set to 12pm next day
				time = evening;
				Calendar newCal = new GregorianCalendar();
				newCal.set(Calendar.HOUR_OF_DAY, NOON);
				newCal.set(Calendar.MINUTE, 0);
				newCal.set(Calendar.SECOND, 0);
				mSelectionArgs[0] = ""+newCal.getTimeInMillis();
				newCal.set(Calendar.HOUR_OF_DAY, EVENING);
				newCal.set(Calendar.MINUTE, 0);
				newCal.set(Calendar.SECOND, 0);
				mSelectionArgs[1] = ""+newCal.getTimeInMillis();
				cal.add(Calendar.DAY_OF_WEEK, 1); //recalculates calendar if at the end of month
				cal.set(Calendar.HOUR_OF_DAY, NOON);
				Cursor mot = getContentResolver().query(MobileSensor_Data.CONTENT_URI, projections, mSelection, mSelectionArgs, MobileSensor_Data.TIMESTAMP + " ASC");
//				Log.d("MoT","count: "+mot.getCount());
				if(mot.getCount() == 0)
					thread_esm_alarm.postDelayed(this, (cal.getTimeInMillis()-System.currentTimeMillis()));
			} else if(hourOfDay >= NOON){ //At or after noon and before 8pm, set to 8pm 
				time = morning;
				Calendar newCal = new GregorianCalendar();
				newCal.set(Calendar.HOUR_OF_DAY, MORNING);
				newCal.set(Calendar.MINUTE, 0);
				newCal.set(Calendar.SECOND, 0);
				mSelectionArgs[0] = ""+newCal.getTimeInMillis();
				newCal.set(Calendar.HOUR_OF_DAY, NOON);
				newCal.set(Calendar.MINUTE, 0);
				newCal.set(Calendar.SECOND, 0);
				mSelectionArgs[1] = ""+newCal.getTimeInMillis();
				cal.set(Calendar.HOUR_OF_DAY, EVENING);
				Cursor mot = getContentResolver().query(MobileSensor_Data.CONTENT_URI, projections, mSelection, mSelectionArgs, MobileSensor_Data.TIMESTAMP + " ASC");
//				Log.d("MoT",""+mot.getCount());
				if(mot.getCount() == 0)
					thread_esm_alarm.postDelayed(this, (cal.getTimeInMillis()-System.currentTimeMillis()));
			}
			//Prompt questionnaire for MoT; ask 3 times if not answered
			if(hourOfDay >= NOON && (ESMObserver.noonCheck == 0 || ESMObserver.eveningCheck == 0)){
				lastNegativeESM = cal.getTimeInMillis();
				Intent esm = new Intent();
				esm.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
			    String esmStr = "[{'esm':{"
			    		+ "'esm_type': 2, "
			    		+ "'esm_title': 'Mode of Transportation', "
			    		+ "'esm_instructions': 'Which one of the following were you doing this "+time+"?', "
			    		+ "'esm_radios':['Walking','Running','Biking','Driving'], "
			    		+ "'esm_submit': 'Done', "
			    		+ "'esm_expiration_threashold': 60, "
			    		+ "'esm_trigger':'False Negative Test Alarm'}}]";
				esm.putExtra(ESM.EXTRA_ESM,esmStr);
				if(hourOfDay < EVENING && noonESM < 3){ 
					noonESM++;
					if(Plugin.screenIsOn && Plugin.participantID != null && Plugin.deviceID != null)
						sendBroadcast(esm);	
					thread_esm_alarm.postDelayed(this, Plugin.throttle);
				} else if(hourOfDay == EVENING && eveningESM < 3){
					eveningESM++;
					if(Plugin.screenIsOn && Plugin.participantID != null && Plugin.deviceID != null)
						sendBroadcast(esm);
					thread_esm_alarm.postDelayed(this, Plugin.throttle/2); //Only ask till 8:45p
				} else{
					ESMObserver.noonCheck = 0;
					ESMObserver.eveningCheck = 0;
					thread_esm_alarm.postDelayed(this, (cal.getTimeInMillis()-System.currentTimeMillis()));
				}
			} else if(ESMObserver.noonCheck == 1 && ESMObserver.eveningCheck == 1){
				ESMObserver.noonCheck = 0;
				ESMObserver.eveningCheck = 0;
				thread_esm_alarm.postDelayed(this, (cal.getTimeInMillis()-System.currentTimeMillis()));
			}
		}
	};
	
	/**
	 * Only run an alarm if the reminder is reasonably far from event
	 * @param id
	 */
	@SuppressLint("SimpleDateFormat")
	public void startCalendarAlarm(int id){
		if(thread_calendar_alarms.size() == 0){
			CalendarEvent event = eventList.get(id);
			lastCalendarAlarm = id;
			int existingEvent = 0;
			for(CalendarEvent e : eventList){
				if(e.id.equals(id)){
					existingEvent++;
					break;
				}
			}
			if(existingEvent == 0){
//				Handler newAlarm = new Handler(thread_handler.getLooper());
				HandlerThread alarm = new HandlerThread("Calendar");
				alarm.start();
				Handler newAlarm = new Handler(alarm.getLooper());
				thread_calendar_alarms.add(id,newAlarm);
//				Log.d("CalendarAlarm","Adding: "+event.getTitle()+" at idx: "+id);
//				Log.d("CalendarAlarm","Begin: "+event.getBegin());
//				Log.d("CalendarAlarm","Reminder to subtract: "+(event.getMaxReminder()*60000));
//				Log.d("CalendarAlarm","Begin-Reminder+2min: "+((event.getBegin()-(event.getMaxReminder()*60000))+120000));
//				Log.d("CalendarAlarm","Size of thread list after adding: "+thread_calendar_alarms.size());
			}
//			Log.d("CalendarAlarm","**********");
//			//print list
//			for(CalendarEvent e : eventList){
//				Log.d("CalendarAlarm",e.getTitle());
//			}
//			Log.d("CalendarAlarm","Size of event list after adding: "+eventList.size());
//			Log.d("CalendarAlarm","**********");
			long nextAlarm = ((event.begin-(event.maxReminder*60000))+120000) - System.currentTimeMillis();
//			Log.d("CalendarAlarm","Delay for pre-alarm: "+nextAlarm);
			thread_calendar_alarms.get(id).postDelayed(calendarAlarm, nextAlarm);
		}
	}
	
	/**
	 * Stop running the thread that we just finished prompting for
	 * @param id
	 */
	public void stopCalendarAlarm(int id){
//		Log.d("CalendarAlarm","**********");
//		Log.d("CalendarAlarm","Removing: "+eventList.get(id).getTitle());
		eventList.remove(id);
		thread_calendar_alarms.get(id).removeCallbacksAndMessages(null);
		thread_calendar_alarms.remove(id);
//		Log.d("CalendarAlarm","Size of thread list after removing: "+thread_calendar_alarms.size());
		//print list
//		for(CalendarEvent e : eventList){
//			Log.d("CalendarAlarm",e.getTitle());
//		}
//		Log.d("CalendarAlarm","Size of event list after removing: "+eventList.size());
//		Log.d("CalendarAlarm","**********");
//		Log.d("CalendarAlarm","Attempting next alarm!");
		if(eventList.size() > 0){
//			Log.d("CalendarAlarm","There's one more! size: "+eventList.size());
//			Log.d("CalendarAlarm","Starting next alarm!");
			startCalendarAlarm(id);
		} 
//		else{
////			Log.d("CalendarAlarm","No more events!");
//		}
//		Log.d("CalendarAlarm","**********");
	}

	/**
	 * This is an alarm that fires up the Reminder ESM which asks the user whether an event is coming
	 * up after the reminder for that event goes off. In addition, it ask for the user stress rating,
	 * and another post ESM to ask whether the event just ended and their stress rating.
	 */
	private Runnable calendarAlarm = new Runnable(){
		
		private String event;
		private int ID;
		
		/**
		 * This will fire an ESM intent for the calendar false positive, at which
		 * the ESMObserver will follow up with a stress rating question
		 */
		@Override
		public void run() {
			if(event == null){ //This is the reminder
				lastCalendarESM = System.currentTimeMillis();
				Plugin.calendar_event = 1;
				//Share context
				CONTEXT_PRODUCER.onContext();
				ID = lastCalendarAlarm;
				event = eventList.get(ID).title; //error here when CalendarObserver has already deleted it
				Intent esm = new Intent();
			    esm.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
				String esmStr = "[" +
		                "{'esm': {" +
		                "'esm_type': 5, " +
		                "'esm_title': 'An event called "+event+" is coming up', " +
		                "'esm_instructions': 'Is this true?', " +
		                "'esm_quick_answers':  ['Yes','No'], " +
		                "'esm_expiration_threashold': 60, " +
		                "'esm_trigger': 'Calendar Reminder' }}]";
				esm.putExtra(ESM.EXTRA_ESM,esmStr);
				if(Plugin.screenIsOn && Plugin.participantID != null && Plugin.deviceID != null)
					sendBroadcast(esm);
				Plugin.calendar_event = 0;
				long end = eventList.get(ID).end;
//				Log.d("CalendarAlarm","Fired the pre-alarm for "+event+", now delaying for post! Delay: "+(end-System.currentTimeMillis()+120000));
				thread_calendar_alarms.get(ID).postDelayed(this, end-System.currentTimeMillis()+120000);
			} else{  //This is the follow up
				lastCalendarESM = System.currentTimeMillis();
//				Log.d("CalendarAlarm","Now in post-alarm for "+event+"!");
//				Log.d("CalendarAlarm","Now in post-alarm for event with ID, "+ID+"!");
				Intent esm = new Intent();
			    esm.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
				String esmStr = "[" +
		                "{'esm': {" +
		                "'esm_type': 5, " +
		                "'esm_title': 'An event called "+event+" just ended', " +
		                "'esm_instructions': 'Is this true?', " +
		                "'esm_quick_answers':  ['Yes','No'], " +
		                "'esm_expiration_threashold': 60, " +
		                "'esm_trigger': 'Calendar Feedback' }}]";
				esm.putExtra(ESM.EXTRA_ESM,esmStr);
				if(Plugin.screenIsOn && Plugin.participantID != null && Plugin.deviceID != null)
					sendBroadcast(esm);
				event = null;
				stopCalendarAlarm(ID);
			}
		}
	};
	
	/**
	 * 
	 * @param calendar_event
	 */
	public void setCalendarEvent(int calendar_event){
		Plugin.calendar_event = calendar_event;
	}
	
	@SuppressLint("NewApi")
	private Runnable setUpEvents = new Runnable(){
		@Override
		public void run() {
//			Log.d("Thread","Calendar: Is this the main thread?"+(Looper.myLooper() == Looper.getMainLooper()));
			clearAllEvents();
//			Log.d("Events","Inside setUpEvents");
			cal = new GregorianCalendar();
			Calendar newCal = new GregorianCalendar();
			//Reset at 12:30am
			int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
			int minOfDay = cal.get(Calendar.MINUTE);
			if(hourOfDay == 0 && minOfDay < 30){
				newCal.set(Calendar.HOUR_OF_DAY, 0);
				newCal.set(Calendar.MINUTE, 30);
				newCal.set(Calendar.SECOND, 0);	
			} else{
				newCal.add(Calendar.DAY_OF_WEEK, 1); //recalculates calendar if at the end
				newCal.set(Calendar.HOUR_OF_DAY, 0);
				newCal.set(Calendar.MINUTE, 30);
				newCal.set(Calendar.SECOND, 0);
			}
//			SimpleDateFormat sdf = new SimpleDateFormat("M-dd-yyyy");
//			String date = sdf.format(newCal.getTime());
//			Log.d("Events","Next reset will be at: "+date);
			String[] mSelectionArgs = new String[3];
			String mSelection = "DELETED = ? AND hasAlarm = ? AND allDay = ?";
			long start = cal.getTimeInMillis(); //current time
			long stop = newCal.getTimeInMillis(); //end of day
			mSelectionArgs[0] = "0"; 
			mSelectionArgs[1] = "1"; 
			mSelectionArgs[2] = "0"; 
			Cursor calendar = getContentResolver().query(Events.CONTENT_URI, null, mSelection, mSelectionArgs, Events.DTSTART+" ASC");
			//Go through and grab all events with reminders with our time constraint
			if(calendar != null && calendar.moveToFirst()){
				do{
					String repeating = calendar.getString(calendar.getColumnIndex("rrule"));
					long begin = calendar.getLong(calendar.getColumnIndex(Events.DTSTART));
					long end = calendar.getLong(calendar.getColumnIndex(Events.DTEND));
					if((repeating == null && begin >= start && end <= stop) || (repeating != null)){
						String id = calendar.getString(calendar.getColumnIndex(Events._ID));
						String name = calendar.getString(calendar.getColumnIndex(Events.TITLE));
						//Filter out deleted instances and grab repeating events
						String[] INSTANCE_PROJECTION = new String[] {
						    Instances.EVENT_ID,      // 0
						    Instances.BEGIN,         // 1
						    Instances.END,			 // 2
						    Instances.TITLE          // 3
						};
						String selection = Instances.EVENT_ID + " = ?";
						String[] selectionArgs = new String[] {id};
						Cursor instances = null;
						//Uri for events withing start and stop
						Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
						ContentUris.appendId(builder, start);
						ContentUris.appendId(builder, stop);
						instances = getContentResolver().query(builder.build(), INSTANCE_PROJECTION, selection, selectionArgs, null);
						if(instances == null || instances.getCount() > 0 && instances.moveToNext()){
							//Make sure the instance's begin is after current time (since we didn't check it before)
							if(instances.getLong(instances.getColumnIndex((Instances.BEGIN))) >= start){
								begin = instances.getLong(instances.getColumnIndex(Instances.BEGIN));
								end = instances.getLong(instances.getColumnIndex(Instances.END));
								String[] toReturn = {""};
								toReturn[0] = ""+id;
								int maxReminder = 0;
								Cursor reminders = getContentResolver().query(Reminders.CONTENT_URI, null, "event_id = ?", toReturn, null);
								if(reminders != null && reminders.moveToLast()){
									do{
										int rem = reminders.getInt(reminders.getColumnIndex(Reminders.MINUTES)) ;
										if(rem > maxReminder)
											maxReminder = rem; 
									}while(reminders.moveToPrevious());
								}
								if(maxReminder >= 10 && maxReminder <= 60){
									CalendarEvent e = new CalendarEvent(id,name,begin,end,maxReminder);
									eventList.add(e);
								}
							}
						} if(instances != null && ! instances.isClosed() ) instances.close(); 
					}
				}while(calendar.moveToNext());
			} if(calendar != null && ! calendar.isClosed() ) calendar.close();
			//Run this again when you reach 12:30am either today or the next day
			long current = System.currentTimeMillis();
//			Log.d("EventList","**********");
//			//print list
//			for(CalendarEvent event : eventList){
//				Log.d("EventList",event.getTitle());
//			}
//			Log.d("EventList","size: "+eventList.size());
//			Log.d("EventList","**********");
			if(eventList.size() > 0)
				startCalendarAlarm(0);
			thread_calSetup.postDelayed(this, (stop-current));
		}
	};
	
//	/**
//	 * 
//	 * @return
//	 */
//	public ArrayList<CalendarEvent> getCalEvents(){
//		return eventList;
//	}
	
	/**
	 * 
	 * @param e
	 */
	public void addEvent(CalendarEvent e){
//		Log.d("EventList","**********");
//		Log.d("EventList","Inside addEvent");
		int ID = 0;
		if(!eventList.contains(e)){
			//Add to an index
			int i;
			for(i = 0; i < eventList.size(); i++){
				if(e.begin < eventList.get(i).begin){
					eventList.add(i,e);	
					break;
				}
				ID++;
			}
			//Add to the end
			if(i == eventList.size()){
				eventList.add(e);
			}
//			//print list
//			for(CalendarEvent event : eventList){
//				Log.d("EventList",event.getTitle());
//			}
//			Log.d("EventList","size: "+eventList.size());
//			Log.d("EventList","**********");
			startCalendarAlarm(ID);
		}
	}
	
	public void removeEvent(int id){
//		Log.d("EventList","**********");
//		Log.d("EventList","Inside removeEvent");
		eventList.remove(id);
		if(thread_calendar_alarms.size() > id){
			thread_calendar_alarms.get(id).removeCallbacksAndMessages(null);
			thread_calendar_alarms.remove(id);
//			Log.d("EventList","Size of thread list after removing: "+thread_calendar_alarms.size());
		}
//		//print list
//		for(CalendarEvent event : eventList){
//			Log.d("EventList",event.getTitle());
//		}
//		Log.d("EventList","Size of event list after removing: "+eventList.size());
//		Log.d("EventList","**********");
	}
	
	/**
	 * Clear calendar events
	 */
	public void clearAllEvents(){
		if(eventList.size()>0)
			eventList.clear();
	}
	
	/**
	 * Updates output csv file
	 */
	private void createOutput(String summary){
		try {
//			FileWriter writer = new FileWriter(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+outputFile,true);
			FileWriter writer = new FileWriter(Environment.getExternalStorageDirectory() + "/AWARE" + Plugin.outputFile,true);
			writer.append(summary+"\n");
    		writer.flush();
    		writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}