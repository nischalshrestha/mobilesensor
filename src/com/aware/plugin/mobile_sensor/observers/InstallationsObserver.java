package com.aware.plugin.mobile_sensor.observers;

import java.util.Calendar;
import java.util.GregorianCalendar;

import com.aware.ESM;
import com.aware.plugin.mobile_sensor.Plugin;
import com.aware.plugin.mobile_sensor.MobileSensor_Provider.MobileSensor_Data;
import com.aware.providers.Communication_Provider.Messages_Data;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;

public class InstallationsObserver extends ContentObserver{

	private Plugin plugin;
	
	/**
	 * Initiate MessageObserver ContentObserver with the handler, and plugin for context
	 * @param handler
	 * @param plugin
	 */
	public InstallationsObserver(Handler handler, Plugin plugin) {
		super(handler);
		this.plugin = plugin;
	}
	
	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		if(Plugin.screenIsOn){
//			if( Aware.DEBUG )
//				Log.d("Installations","User is either installing or uninstalling an app!");
			Plugin.installations = 1;
			plugin.CONTEXT_PRODUCER.onContext();
			Plugin.installations = 0;
		}
	}
	
	

}
