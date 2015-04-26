package com.aware.plugin.sos_mobile_sensor.calendar;

//import java.text.SimpleDateFormat;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.CalendarContract.CalendarAlerts;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.provider.CalendarContract.Reminders;
import android.util.Log;

import com.aware.plugin.sos_mobile_sensor.Plugin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

//import java.util.Date;
/**
 * This class observes changes in the calendar with events that have reminders.
 * Perhaps we can incorporate duration later i.e. the time window around an event
 * @author Nischal Shrestha
 */
public class CalendarObserver extends ContentObserver {

	private Plugin plugin;
	
	public CalendarObserver(Handler handler, Plugin plugin) {
		super(handler);
		this.plugin = plugin;
	}        
	
	@SuppressLint({ "NewApi", "SimpleDateFormat" })
	public void onChange(boolean selfChange){
//		Log.d("Reminder", "Inside onChange");
		Calendar cal = new GregorianCalendar();
		Calendar newCal = new GregorianCalendar();
		newCal.set(Calendar.HOUR_OF_DAY, 24);
		newCal.set(Calendar.MINUTE, 0);
		newCal.set(Calendar.SECOND, 0);
		String mSelection = "allDay = ?";
		long start = cal.getTimeInMillis(); //current time
		long stop = newCal.getTimeInMillis(); //end of day
		String[] mSelectionArgs = new String[]{"0"};
		Cursor calendar = plugin.getContentResolver().query(Events.CONTENT_URI, null, mSelection, mSelectionArgs, Events.DTSTART+" ASC");
//		Log.d("Calendar","cursor size:"+calendar.getCount());
		if(calendar != null && calendar.moveToFirst()){
				ArrayList<CalendarEvent> list = Plugin.eventList;
				do{
					String repeating = calendar.getString(calendar.getColumnIndex("rrule")); 
					long begin = calendar.getLong(calendar.getColumnIndex(Events.DTSTART));
					long end = calendar.getLong(calendar.getColumnIndex(Events.DTEND));
					if((repeating == null && begin >= start && end <= stop) || (repeating != null && begin >= start)){
						String newEventID = calendar.getString(calendar.getColumnIndex(Events._ID));
						String[] INSTANCE_PROJECTION = new String[] {
							    Instances.EVENT_ID,      // 0
							    Instances.BEGIN,         // 1
							    Instances.END,			 // 2
							    Instances.TITLE          // 3
							};
						String selection = Instances.EVENT_ID + " = ?";
						String[] selectionArgs = new String[] {newEventID};
						Cursor instances = null;
						Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
						ContentUris.appendId(builder, start);
						ContentUris.appendId(builder, stop);
						instances = plugin.getContentResolver().query(builder.build(), INSTANCE_PROJECTION, selection, selectionArgs, null);
//						Log.d("Instances","(calendar) cursor size:"+instances.getCount());
						if(instances.getCount() > 0){
							instances.moveToNext();
							end = instances.getLong(2);
						}
						String name = calendar.getString(calendar.getColumnIndex(Events.TITLE));
						String[] toReturn = {""};
						toReturn[0] = ""+newEventID;
						Cursor alerts = plugin.getContentResolver().query(CalendarAlerts.CONTENT_URI, null, "event_id = ?", toReturn, null);
						String status = "";
						if(alerts != null && alerts.moveToLast()){
//							Log.d("Reminder","Alert needs to be deleted:"+alerts.getString(alerts.getColumnIndex(CalendarAlerts.EVENT_ID)));
							status = alerts.getString(alerts.getColumnIndex(CalendarAlerts.STATE));
//							Log.d("Reminder","Alert status:"+status);
						} if(alerts != null && ! alerts.isClosed() ) alerts.close();
						int maxReminder = 0;
						Cursor reminders = plugin.getContentResolver().query(Reminders.CONTENT_URI, null, "event_id = ?", toReturn, null);
						if(reminders != null && reminders.moveToLast()){
							Log.d("Alerts", ""+reminders.getCount());
							do{
								int rem = reminders.getInt(reminders.getColumnIndex(Reminders.MINUTES)) ;
								if(rem > maxReminder)
									maxReminder = rem; 
							}while(reminders.moveToPrevious());
						}
						CalendarEvent newEvent = new CalendarEvent(newEventID,name,begin,end,maxReminder);
						//New event, not deleted; Old event, diff max rem / time (the rem / time has been changed)
						if(status.equals("0") && !list.contains(newEvent) && calendar.getString(calendar.getColumnIndex(Events.DELETED)).equals("0") 
								&& maxReminder >= 1
								&& maxReminder <= 60){
							CalendarEvent toRemove = null;
							int ID = 0;
							for(CalendarEvent e : list){
								if(e.id.equals(newEvent.id)){
									toRemove = e;
//									Log.d("Calendar", "To remove: "+toRemove.getTitle());
									break;
								}
								ID++;
							}
							if(toRemove != null){
								plugin.removeEvent(ID);
							} 
							plugin.addEvent(newEvent);
						} 
						//An old event needs to be deleted; user has deleted the reminders; or, the reminder has been fired (status = 1)
						else if(status.equals("1") || calendar.getString(calendar.getColumnIndex(Events.DELETED)).equals("1") || maxReminder == 0){
//							Log.d("Calendar", "To remove event with delete status : "+calendar.getString(calendar.getColumnIndex(Events.TITLE)));
							//Find event idx to remove
							int ID = 0;
							int existingEvent = 0;
							for(CalendarEvent e : list){
								if(e.id.equals(newEvent.id)){
									existingEvent++;
									break;
								}
								ID++;
							}
							//if user deleted event or deleted its reminders
//						    Log.d("Reminder","Deleted? "+calendar.getString(calendar.getColumnIndex(Events.DELETED)).equals("1"));
							if((calendar.getString(calendar.getColumnIndex(Events.DELETED)).equals("1") || maxReminder == 0) && existingEvent > 0){
								if(Plugin.eventList.size() > 0)
									plugin.removeEvent(ID); //remove from eventList array
							}
						}
					}
				} while(calendar.moveToNext());
		} if(calendar != null && ! calendar.isClosed() ) calendar.close();
	}
}
