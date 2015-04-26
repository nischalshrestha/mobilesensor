package com.aware.plugin.sos_mobile_sensor;


import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.provider.CalendarContract.Reminders;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.Screen;
import com.aware.plugin.ambient_noise.Provider.AmbientNoise_Data;
import com.aware.plugin.sos_mobile_sensor.MobileSensor_Provider.MobileSensor_Data;
import com.aware.plugin.sos_mobile_sensor.activities.Settings;
import com.aware.plugin.sos_mobile_sensor.calendar.CalendarEvent;
import com.aware.plugin.sos_mobile_sensor.calendar.CalendarObserver;
import com.aware.plugin.sos_mobile_sensor.observers.ActivityObserver;
import com.aware.plugin.sos_mobile_sensor.observers.AmbientNoiseObserver;
import com.aware.plugin.sos_mobile_sensor.observers.ESMObserver;
import com.aware.plugin.sos_mobile_sensor.observers.InstallationsObserver;
import com.aware.plugin.sos_mobile_sensor.observers.MessageObserver;
import com.aware.plugin.sos_mobile_sensor.observers.MultitaskingObserver;
import com.aware.plugin.sos_mobile_sensor.observers.ScreenObserver;
import com.aware.plugin.sos_mobile_sensor.observers.VoiceCallObserver;
import com.aware.providers.Applications_Provider.Applications_Foreground;
import com.aware.providers.Communication_Provider.Calls_Data;
import com.aware.providers.Communication_Provider.Messages_Data;
import com.aware.providers.ESM_Provider.ESM_Data;
import com.aware.providers.Installations_Provider.Installations_Data;
import com.aware.utils.Aware_Plugin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

//import android.util.Log;

//import com.aware.plugin.noise_level.NoiseLevel_Provider.NoiseLevel;
//import com.aware.plugin.sos_mobile_sensor.observers.NoiseObserver;

public class Plugin extends Aware_Plugin {
	
	/**
	* Broadcasted event: the context of this sensor is being shared
	*/
	public static final String ACTION_AWARE_PLUGIN_SOS_MOBILE_SENSOR = "ACTION_AWARE_PLUGIN_SOS_MOBILE_SENSOR";
	
	/**
	 * A multi-thread handler manager and the relevant sensor threads.
	 */
	public static HandlerThread thread_multitasking;
	public static HandlerThread thread_activity;
	public static HandlerThread thread_phone;
	public static HandlerThread thread_messages;
	public static HandlerThread thread_esm;
	public static HandlerThread thread_calendar;
	public static HandlerThread thread_screen;
	public static HandlerThread thread_cal_alarm;
	public static HandlerThread thread_install;
	public static HandlerThread thread_ambient_noise;
	public static HandlerThread thread_retroq;
	
	public static Handler thread_sensor_multi = null;
	public static Handler thread_sensor_activity = null;
	public static Handler thread_sensor_phone = null;
	public static Handler thread_sensor_messages = null;
	public static Handler thread_sensor_esm = null;
	public static Handler thread_observer_calendar = null;
	public static Handler thread_sensor_screen = null;
	public static Handler thread_sensor_install = null;
	public static Handler thread_sensor_ambient_noise = null;
	
	public static Handler thread_esm_alarm = null;
	public static Handler thread_retroq_alarm = null;
	private ArrayList<Handler> thread_calendar_alarms;
	@SuppressWarnings("unused")
	private ArrayList<Runnable> event_alarms;
	private static Handler thread_calSetup;
	
	/**
	 * The Context Observers
	 */
	//Screen
	public static ScreenObserver screenListener;
	//Mode of Transportation
	public static ActivityObserver activityListener;
	//Multitasking
	public static MultitaskingObserver multitask_observer = null;
	//Ambient Noise
	public static AmbientNoiseObserver ambient_noise_observer = null;
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
	 * Private variables that hold the latest values to be shared w/ ACTION_AWARE_CURRENT_CONTEXT
	 */
	/** Noise */
	public static double noise_level;
	public static String ambient_noise = "";
	/** Binary values */
	public static int multitasking, voice_messaging, text_messaging, email, installations;
	public static String calendar_event="0";
	/** Screen */
	public static long screenOnTime;
	public static boolean screenIsOn;
	/** MoT variables */
	public static String movement = "";
	public static int movementConfidence = 0;
	public static long lastNegativeESM;
	/** Calendar */
	private Calendar cal;
	private static final String morning = "morning";
	private static final String evening = "evening";
	public static String time = "";
	public static ArrayList<CalendarEvent> eventList = new ArrayList<CalendarEvent>();
	private int lastCalendarAlarm;
	public static long lastCalendarESM;
	/** Constants */
	public static final int MORNING = 8;
	public static final int NOON = 12;
	public static final int EVENING = 20;
	/** Throttle */
	public static final long throttle = 3600000;
	/** Initial user input variables */
	public static String participantID = "";
    /** Stressor variables */
    public static String initStressor = "";
    public static boolean stressInit = false;
    public static long initStressorTime = 0;
    public static int stressCount = 0;
    public static int stressEvents = 0;
    
    /**
     * Aware calls this to make sure plugin is still up and running
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
		TAG = "AWARE::Mobile Sensor";
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");
        return super.onStartCommand(intent, flags, startId);
    }
	
	/**
	 * When the Plugin is turned on, or activated.
	 */
	@SuppressLint("NewApi")
	@Override
	public void onCreate() {
		super.onCreate();
		TAG = "AWARE::Mobile Sensor";
        participantID = "";
		//Our provider tables
		DATABASE_TABLES = MobileSensor_Provider.DATABASE_TABLES;
		//Our table fields
		TABLES_FIELDS = MobileSensor_Provider.TABLES_FIELDS;
		//Our provider URI
		CONTEXT_URIS = new Uri[]{ MobileSensor_Data.CONTENT_URI };
		
		startAwareSensors();
		initializeThreads();
		initializeSensorVariables();
		startAlarms();
		startContentObservers();
		
		//Shares this plugin’s context to AWARE and applications
		CONTEXT_PRODUCER = new ContextProducer() {
			@SuppressLint("SimpleDateFormat")
			@Override
			public void onContext() {
//                Log.d("Context","Pushing data!");
//                Log.d("Context","participantID: "+participantID);
//                Log.d("Context","screenIsOn: "+Plugin.screenIsOn);
//                Log.d("Context", "initialized: "+Settings.initialized);
//                Log.d("Context", "stressInit: "+Plugin.stressInit);
				if(Plugin.screenIsOn){
//                        && Settings.initialized){
//					Log.d("Context","Pushing data!");
					//change this to debug any sensor and if they're being inserted when sensed
					Calendar newCal = new GregorianCalendar();
					SimpleDateFormat sdf = new SimpleDateFormat("M-dd-yyyy");
					String date = sdf.format(newCal.getTime());
//					Log.d("Time","Date: "+date);
					SimpleDateFormat sdf2 = new SimpleDateFormat("k:mm:ss");
					long timestamp = System.currentTimeMillis();
					String time = sdf2.format(new Date(System.currentTimeMillis()));
//					deviceID = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID);
					ContentValues context_data = new ContentValues();
					context_data.put(MobileSensor_Data.TIMESTAMP, timestamp);
					context_data.put(MobileSensor_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                    context_data.put(MobileSensor_Data.DATE, date);
                    context_data.put(MobileSensor_Data.TIME, time);
					context_data.put(MobileSensor_Data.PARTICIPANT_ID, participantID);
					context_data.put(MobileSensor_Data.MULTITASKING, multitasking);
					context_data.put(MobileSensor_Data.MOT, movement);
					context_data.put(MobileSensor_Data.MOT_CONFIDENCE, movementConfidence);
					context_data.put(MobileSensor_Data.AMBIENT_NOISE, ambient_noise);
					context_data.put(MobileSensor_Data.CALLS, voice_messaging);
					context_data.put(MobileSensor_Data.MESSAGING, text_messaging);
					context_data.put(MobileSensor_Data.CALENDAR, calendar_event);
					context_data.put(MobileSensor_Data.EMAIL, email);
					context_data.put(MobileSensor_Data.INSTALLATIONS, installations);
					//insert data to table MobileSensor_Data
					getContentResolver().insert(MobileSensor_Data.CONTENT_URI, context_data);
					Intent sharedContext = new Intent(ACTION_AWARE_PLUGIN_SOS_MOBILE_SENSOR);
                    sendBroadcast(sharedContext);
				}
			}
		};
		
	}
	
	private void initializeThreads(){
		thread_multitasking = new HandlerThread("Multitasking");
		thread_activity = new HandlerThread("MoT");
		thread_phone = new HandlerThread("Phone");
		thread_messages = new HandlerThread("Messaging");
		thread_esm = new HandlerThread("ESM");
		thread_calendar = new HandlerThread("Calendar");
		thread_screen = new HandlerThread("Screen");
		thread_cal_alarm = new HandlerThread("Calendar_Alarm");
		thread_install = new HandlerThread("Installations");
		thread_ambient_noise = new HandlerThread("Ambient Noise");
	}
	
	public void startAwareSensors(){
		Intent aware = new Intent(this, Aware.class);
	    startService(aware);
	    //Activate core sensors
		Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_SOS_MOBILE_SENSOR, true);
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
	
	public void initializeSensorVariables(){
		screenOnTime = System.currentTimeMillis();
		lastNegativeESM = screenOnTime;
        initStressorTime = screenOnTime;
		screenIsOn = true;
	}
	
	/**
	 * Calendar events alarm and MoT alarm
	 */
	public void startAlarms(){
		if(!Settings.initialized){
			thread_cal_alarm.start();
		} else{
			thread_cal_alarm = new HandlerThread("Calendar_Alarm");
			thread_cal_alarm.start();
//			
		}
		thread_retroq =  new HandlerThread("Retro_Alarm");
		thread_retroq.start();
		thread_calendar_alarms = new ArrayList<Handler>();
		event_alarms = new ArrayList<Runnable>();
		Plugin.lastCalendarESM = screenOnTime;
		//Alarm for the MoT Esm question
		thread_esm_alarm = new Handler(thread_cal_alarm.getLooper());
		thread_esm_alarm.postDelayed(esmAlarm, 5000);
		//Alarm for setting up events for Calendar
		thread_calSetup = new Handler(thread_cal_alarm.getLooper());  
		thread_calSetup.postDelayed(setUpEvents, 10000);
		//Alarm for retroactive question
		thread_retroq_alarm = new Handler(thread_retroq.getLooper());
		thread_retroq_alarm.postDelayed(retroQuestion, 60000);
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
		
		//Create a context filter for Activity
		thread_activity.start();
		thread_sensor_activity = new Handler(thread_activity.getLooper());
		IntentFilter activityFilter = new IntentFilter();
		activityFilter.addAction(ActivityObserver.ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION);
		activityListener = new ActivityObserver(this);
		ActivityObserver.lastActivityESM = screenOnTime;
		
		//Ambient noise
		thread_ambient_noise.start();
		thread_sensor_ambient_noise = new Handler(thread_ambient_noise.getLooper());
		ambient_noise_observer = new AmbientNoiseObserver(thread_sensor_ambient_noise, this);
		AmbientNoiseObserver.lastAmbientESM = screenOnTime;
		
		//Ask Android to register our context receiver
		registerReceiver(screenListener, screenFilter, null, thread_sensor_screen);
		registerReceiver(activityListener, activityFilter, null, thread_sensor_activity);
		//Start listening to changes on the Applications_Foreground, MoT, and NoiseLevel databases
		getContentResolver().registerContentObserver(Applications_Foreground.CONTENT_URI, true, multitask_observer);
		getContentResolver().registerContentObserver(Installations_Data.CONTENT_URI, true, install_observer);
		getContentResolver().registerContentObserver(AmbientNoise_Data.CONTENT_URI, true, ambient_noise_observer);
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
		participantID = null;
		//Unregister all observers and receivers
		unregisterReceiver(screenListener);
		unregisterReceiver(activityListener);
		getContentResolver().unregisterContentObserver(multitask_observer);
		getContentResolver().unregisterContentObserver(phone_observer);
		getContentResolver().unregisterContentObserver(messages_observer);
		getContentResolver().unregisterContentObserver(esm_observer);
		getContentResolver().unregisterContentObserver(calendar_observer);
		getContentResolver().unregisterContentObserver(install_observer);
		getContentResolver().unregisterContentObserver(ambient_noise_observer);
		//Stop listening to changes in the database(s)
		thread_sensor_screen.removeCallbacksAndMessages(null);
		thread_sensor_multi.removeCallbacksAndMessages(null);
		thread_sensor_activity.removeCallbacksAndMessages(null);
		thread_sensor_messages.removeCallbacksAndMessages(null);
		thread_sensor_esm.removeCallbacksAndMessages(null);
		thread_esm_alarm.removeCallbacksAndMessages(null);
		thread_observer_calendar.removeCallbacksAndMessages(null);
		thread_sensor_install.removeCallbacksAndMessages(null);
		thread_sensor_ambient_noise.removeCallbacksAndMessages(null);
		//Stop all threads
		if(thread_calendar_alarms.size() != 0){
			for(Handler h : thread_calendar_alarms){
				h.removeCallbacksAndMessages(null);
			}
			thread_calendar_alarms.clear();
		}
		thread_multitasking.interrupt();
		thread_activity.interrupt();
		thread_messages.interrupt();
		thread_phone.interrupt();
		thread_esm.interrupt();
		thread_calendar.interrupt();
		thread_screen.interrupt();
		thread_install.interrupt();
		thread_cal_alarm.interrupt();
		thread_ambient_noise.interrupt();
		thread_multitasking = null;
		thread_activity = null;
		thread_messages = null;
		thread_phone = null;
		thread_esm = null;
		thread_calendar = null;
		thread_screen = null;
		thread_install = null;
		thread_cal_alarm = null;
		thread_ambient_noise = null;
		//Deactivate the sensors
		Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_SOS_MOBILE_SENSOR, false);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_SCREEN, false);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_INSTALLATIONS, false);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_APPLICATIONS, false);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_CALLS, false);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_MESSAGES, false);
		Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, false);
		Intent refresh = new Intent(Aware.ACTION_AWARE_REFRESH);
		sendBroadcast(refresh);
		//Deactivate the plugins
		Aware.stopPlugin(this, "com.aware.plugin.google.activity_recognition");
		Aware.stopPlugin(this, "com.aware.plugin.ambient_noise");
	}
	
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
				HandlerThread alarm = new HandlerThread("Calendar");
				alarm.start();
				Handler newAlarm = new Handler(alarm.getLooper());
				thread_calendar_alarms.add(id,newAlarm);
			}
			long nextAlarm = ((event.begin-(event.maxReminder*60000))+120000) - System.currentTimeMillis();
			thread_calendar_alarms.get(id).postDelayed(calendarAlarm, nextAlarm);
		}
	}
	
	/**
	 * Stop running the thread that we just finished prompting for
	 * @param id
	 */
	public void stopCalendarAlarm(int id){
		eventList.remove(id);
		thread_calendar_alarms.get(id).removeCallbacksAndMessages(null);
		thread_calendar_alarms.remove(id);
		if(eventList.size() > 0){
			startCalendarAlarm(id);
		} 
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
		@SuppressWarnings("unused")
		@Override
		public void run() {
			if(event == null){ //This is the reminder
				ID = lastCalendarAlarm;
				event = eventList.get(ID).title; //error here when CalendarObserver has already deleted it
                lastCalendarESM = System.currentTimeMillis();
                if(!Plugin.stressInit){
			        Plugin.stressInit = true;
			        Plugin.initStressorTime = lastCalendarESM;
			        Plugin.initStressor = "Calendar Reminder";
			        Plugin.stressCount++;
                } else if(Plugin.stressInit 
                		&& lastCalendarESM-Plugin.initStressorTime < 60000 
                		&& !Plugin.initStressor.equals("Calendar Reminder")){
                    Plugin.stressCount++;
                }
                Plugin.calendar_event = event;
                //Share context
                CONTEXT_PRODUCER.onContext();
                Plugin.calendar_event = "0";
                if(Calendar.DAY_OF_WEEK > 1 && Calendar.DAY_OF_WEEK < 7){
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
	                if(Plugin.screenIsOn)
	                    sendBroadcast(esm);
                }
				long end = eventList.get(ID).end;
				thread_calendar_alarms.get(ID).postDelayed(this, end-System.currentTimeMillis()+120000);
			} else{  //This is the follow up
				lastCalendarESM = System.currentTimeMillis();
				if(!Plugin.stressInit){
			        Plugin.stressInit = true;
			        Plugin.initStressorTime = lastCalendarESM;
			        Plugin.initStressor = "Calendar Feedback";
			        Plugin.stressCount++;
                } else if(Plugin.stressInit 
                		&& lastCalendarESM-Plugin.initStressorTime < 60000 
                		&& !Plugin.initStressor.equals("Calendar Feedback")){
                    Plugin.stressCount++;
                }
                Plugin.calendar_event = event;
                //Share context
                CONTEXT_PRODUCER.onContext();
                Plugin.calendar_event = "0";
                if(Calendar.DAY_OF_WEEK > 1 && Calendar.DAY_OF_WEEK < 7){
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
	                if(Plugin.screenIsOn)
	                    sendBroadcast(esm);
                }
				event = null;
				stopCalendarAlarm(ID);
			}
		}
	};
	
	@SuppressLint("NewApi")
	private Runnable setUpEvents = new Runnable(){
		@Override
		public void run() {
			clearAllEvents();
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
								if(maxReminder >= 1 && maxReminder <= 60){
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
			if(eventList.size() > 0)
				startCalendarAlarm(0);
			thread_calSetup.postDelayed(this, (stop-current));
		}
	};
	
	/**
	 * 
	 * @param e
	 */
	public void addEvent(CalendarEvent e){
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
			startCalendarAlarm(ID);
		}
	}
	
	public void removeEvent(int id){
		eventList.remove(id);
		if(thread_calendar_alarms.size() > id){
			thread_calendar_alarms.get(id).removeCallbacksAndMessages(null);
			thread_calendar_alarms.remove(id);
		}
	}
	
	/**
	 * Clear calendar events
	 */
	public void clearAllEvents(){
		if(eventList.size()>0)
			eventList.clear();
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
		@SuppressWarnings("unused")
		@Override
		public void run() {
			//Get Calendar to determine current date/time and figure out the date/time for the
			//next alarm
//			Log.d("Thread","ESM: Is this the main thread?"+(Looper.myLooper() == Looper.getMainLooper()));
			cal = Calendar.getInstance();
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
			} else if(hourOfDay >= EVENING-1){ //At or after 7pm, set to 12pm next day
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
				if(mot.getCount() == 0){
					thread_esm_alarm.postDelayed(this, (cal.getTimeInMillis()-System.currentTimeMillis()));
				}
			} else if(hourOfDay >= NOON){ //At or after noon and before 7pm, set to 7pm 
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
				cal.set(Calendar.HOUR_OF_DAY, EVENING-1);
				Cursor mot = getContentResolver().query(MobileSensor_Data.CONTENT_URI, projections, mSelection, mSelectionArgs, MobileSensor_Data.TIMESTAMP + " ASC");
//				Log.d("MoT",""+mot.getCount());
				if(mot.getCount() == 0){
					thread_esm_alarm.postDelayed(this, (cal.getTimeInMillis()-System.currentTimeMillis()));
				}
			}
			//Prompt questionnaire for MoT
			if(hourOfDay >= NOON            &&
			   Calendar.DAY_OF_WEEK > 1 	&&
			   Calendar.DAY_OF_WEEK < 7
				){
				lastNegativeESM = cal.getTimeInMillis();				
				Intent esm = new Intent();
				esm.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
			    String esmStr = "[{'esm':{"
			    		+ "'esm_type': 3, "
			    		+ "'esm_title': 'Mode of Transportation', "
			    		+ "'esm_instructions': 'What was your mode of transportation this "+time+"?', "
			    		+ "'esm_checkboxes':['Walking','Running','Driving','Biking','Other'], "
			    		+ "'esm_submit': 'Done', "
			    		+ "'esm_expiration_threashold': 180, "
			    		+ "'esm_trigger':'False Negative Test Alarm'}}]";
				esm.putExtra(ESM.EXTRA_ESM,esmStr);
				if(Plugin.screenIsOn)
					sendBroadcast(esm);	
				thread_esm_alarm.postDelayed(this, (cal.getTimeInMillis()-System.currentTimeMillis()));
			}
		}
	};
	
	/**
	 * This questions asks the user about what caused the stressful events of the day (rating > 3)
	 */
	private Runnable retroQuestion = new Runnable() {
		
		@SuppressWarnings("unused")
		@Override
		public void run() {
//			Log.d("Retro","Right now: "+System.currentTimeMillis());
			cal = new GregorianCalendar();
			Calendar newCal = new GregorianCalendar();
			long next = 0;
			if(Calendar.DAY_OF_WEEK > 1 && Calendar.DAY_OF_WEEK < 7){ //don't bother on weekends
	            int hourOfDay = Calendar.HOUR_OF_DAY;
	            if((hourOfDay >= 0 || hourOfDay == 24) && hourOfDay < 8){ //0am
	                newCal.set(Calendar.HOUR_OF_DAY, 20);
	                newCal.set(Calendar.MINUTE, 0);
	                newCal.set(Calendar.SECOND, 0);
	                next = newCal.getTimeInMillis() - System.currentTimeMillis();
	//                Log.d("Retro","Inside 1st: "+next);
	            } else if(hourOfDay >= 8 && hourOfDay < 20){ //8am to before 8pm normal questions
	                newCal.set(Calendar.HOUR_OF_DAY, 20);
	                newCal.set(Calendar.MINUTE, 0);
	                newCal.set(Calendar.SECOND, 0);
	                next = newCal.getTimeInMillis() - System.currentTimeMillis();
	            } else if(hourOfDay == 20){ //8pm retro question
	                newCal.add(Calendar.DAY_OF_WEEK, 1); //recalculates calendar if at the end
	                newCal.set(Calendar.HOUR_OF_DAY, 20);
	                newCal.set(Calendar.MINUTE, 0);
	                newCal.set(Calendar.SECOND, 0);
	                next = newCal.getTimeInMillis() - System.currentTimeMillis();
	                //Access ESM database to check whether there are any stressful events (rating > 3)
	                Calendar morning = new GregorianCalendar();
	                morning.set(Calendar.HOUR_OF_DAY, MORNING);
	                morning.set(Calendar.MINUTE, 0);
	                morning.set(Calendar.SECOND, 0);
					Calendar evening = new GregorianCalendar();
					evening.set(Calendar.HOUR_OF_DAY, EVENING);
					evening.set(Calendar.MINUTE, 0);
					evening.set(Calendar.SECOND, 0);
	                String mSelection = "TIMESTAMP > ? AND TIMESTAMP < ? AND esm_type == ? AND esm_status == ?";
					String[] mSelectionArgs = new String[4];
					mSelectionArgs[0] = ""+morning.getTimeInMillis();
					mSelectionArgs[1] = ""+evening.getTimeInMillis();
					mSelectionArgs[2] = "4";
					mSelectionArgs[3] = "2";
	                Cursor esmDB = getContentResolver().query(ESM_Data.CONTENT_URI, null, mSelection, mSelectionArgs, null);
	                if(esmDB != null && esmDB.moveToFirst()){
	                	do{
	                    	if(Double.parseDouble(esmDB.getString(esmDB.getColumnIndex("esm_user_answer"))) > 3.0){
	                    		stressEvents++; 
	                    	}
	                    } while(esmDB.moveToNext());	
	                }
	                //Make an array for all events and iterate through them to ask.
	                //Share context
	                if(esmDB.getCount() > 0 && stressEvents > 0){
		                Intent esm = new Intent();
		                esm.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
		                String esmStr = "[" +
		                        "{'esm': {" +
		                        "'esm_type': 1, " +
		                        "'esm_title': 'Retroactive Question', " +
		                        "'esm_instructions': 'You rated above a 3 on the stress scale "+stressEvents+" times today! "
		                        + "Please describe what affected your ratings today.', " +
		                        "'esm_submit':'Done', " +
		                        "'esm_expiration_threashold': 240, " +
		                        "'esm_trigger': 'Retroactive Question' }}]";
		                esm.putExtra(ESM.EXTRA_ESM,esmStr);
		                if(Plugin.screenIsOn)
		                    sendBroadcast(esm);
	                }
	                if(esmDB != null && !esmDB.isClosed()){ esmDB.close(); }
	            } else if(hourOfDay > 20 && hourOfDay < 24){
	            	newCal.add(Calendar.DAY_OF_WEEK, 1); //recalculates calendar if at the end
	                newCal.set(Calendar.HOUR_OF_DAY, 20);
	                newCal.set(Calendar.MINUTE, 0);
	                newCal.set(Calendar.SECOND, 0);
	                next = newCal.getTimeInMillis() - System.currentTimeMillis();
	            }
			} else{
				if(Calendar.DAY_OF_WEEK == 1){
					newCal.add(Calendar.DAY_OF_WEEK, 1); //have it delayed until the next day
				} else{
					newCal.add(Calendar.DAY_OF_WEEK, 2); //have it delayed until next 2 days
				}
				newCal.set(Calendar.HOUR_OF_DAY, 20);
                newCal.set(Calendar.MINUTE, 0);
                newCal.set(Calendar.SECOND, 0);
                next = newCal.getTimeInMillis() - System.currentTimeMillis();
			}
//			Log.d("Retro","Next one in ms: "+next);
			thread_retroq_alarm.postDelayed(this, next);
		}
	};
	
}