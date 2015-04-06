package com.aware.plugin.sos_mobile_sensor.observers;

import android.database.ContentObserver;
import android.os.Handler;

import com.aware.plugin.sos_mobile_sensor.Plugin;

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
