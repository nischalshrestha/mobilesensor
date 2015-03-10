package com.aware.plugin.sos_mobile_sensor.calendar;

public class CalendarEvent {
	
	public String id;
	public String title;
	public long begin;
	public long end;
	public int maxReminder;

	public CalendarEvent(String id, String title, long begin, long end, int maxReminder){
		this.id = id;
		this.title = title;
		this.begin = begin;
		this.end = end;
		this.maxReminder = maxReminder;
	}
}
