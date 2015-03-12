package com.aware.plugin.sos_mobile_sensor;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

public class MobileSensor_Provider extends ContentProvider {
	
	/**
	* Authority of this content provider
	*/
	public static String AUTHORITY = "com.aware.plugin.sos_mobile_sensor.provider.sos_mobile_sensor";
	/**
	* ContentProvider database version. Increment every time you modify the database structure
	*/
	public static final int DATABASE_VERSION = 2;
	//ContentProvider query indexes
	private static final int MOBILE_SENSOR = 1;
	private static final int MOBILE_SENSOR_ID = 2;
	/**
	* Database stored in external folder: /AWARE/plugin_phone_usage.db
	*/
	public static final String DATABASE_NAME = Environment.getExternalStorageDirectory() + "/AWARE/plugin_sos_mobile_sensor.db";
	/**
	* Database tables:
	* – plugin_phone_usage
	*/
	public static final String[] DATABASE_TABLES = {"plugin_sos_mobile_sensor"};
	/**
	* Database table fields
	*/
	public static final String[] TABLES_FIELDS = {
		MobileSensor_Data._ID + " integer primary key autoincrement," +
		MobileSensor_Data.TIMESTAMP + " real default 0," +
		MobileSensor_Data.DEVICE_ID + " text default ''," +
		MobileSensor_Data.MULTITASKING + " integer default 0," +
		MobileSensor_Data.MOT + " text default ''," +
		MobileSensor_Data.NOISE_LEVEL + " real default 0," +
//		MobileSensor_Data.AMBIENT_NOISE + " double default 0," +
		MobileSensor_Data.CALLS + " integer default 0," +
		MobileSensor_Data.MESSAGING + "  integer default 0," +
		MobileSensor_Data.CALENDAR + " integer default 0," +
		MobileSensor_Data.EMAIL + " integer default 0," +
		MobileSensor_Data.INSTALLATIONS + " integer default 0," +
		"UNIQUE (" + MobileSensor_Data.TIMESTAMP + "," + MobileSensor_Data.DEVICE_ID + ")"
	};
	private static UriMatcher sUriMatcher = null;
	private static HashMap<String, String> tableMap = null;
	private static DatabaseHelper databaseHelper = null;
	private static SQLiteDatabase database = null;
	private boolean initializeDB() {
		if (databaseHelper == null) {
			databaseHelper = new DatabaseHelper( getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS );
		}
		if( databaseHelper != null && ( database == null || ! database.isOpen() )) {
			database = databaseHelper.getWritableDatabase();
		}
		return( database != null && databaseHelper != null);
	}

	public static final class MobileSensor_Data implements BaseColumns {
		private MobileSensor_Data(){};
		/**
		* Your ContentProvider table content URI.
		* The last segment needs to match your database table name
		*/
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/plugin_sos_mobile_sensor");
		/**
		* How your data collection is identified internally in Android (vnd.android.cursor.dir).
		* It needs to be /vnd.aware.plugin.XXX where XXX is your plugin name (no spaces!).
		*/
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.sos_mobile_sensor";
		/**
		* How each row is identified individually internally in Android (vnd.android.cursor.item). 
		* It needs to be /vnd.aware.plugin.XXX where XXX is your plugin name (no spaces!).
		*/
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.sos_mobile_sensor";
		public static final String _ID = "_id";
		public static final String TIMESTAMP = "timestamp";
		public static final String DEVICE_ID = "device_id";
		public static final String MULTITASKING = "multitasking";
		public static final String MOT = "mode_of_transportation";
		public static final String NOISE_LEVEL = "double_noise_level"; //replicating the database in MySQL requires the keyword double_xxx for real columns from SQLite
//		public static final String AMBIENT_NOISE = "ambient_noise";
		public static final String CALLS = "voice_messaging";
		public static final String MESSAGING = "text_messaging";
		public static final String CALENDAR = "calendar_event";
		public static final String EMAIL = "email";
		public static final String INSTALLATIONS = "installations";
	}
	

	/**
	 * Delete data from your database
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if( !initializeDB() ) {
			Log.w(AUTHORITY,"Database unavailable…");
			return 0;
		}
		int count = 0;
		switch (sUriMatcher.match(uri)) {
			case MOBILE_SENSOR:
				count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	/**
	 * What your database returns as content URI when you access your database
	 */
	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
			case MOBILE_SENSOR:
				return MobileSensor_Data.CONTENT_TYPE;
			case MOBILE_SENSOR_ID:
				return MobileSensor_Data.CONTENT_ITEM_TYPE;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}
	
	/**
	 * Insert new data to your database
	 */
	@Override
	public Uri insert(Uri uri, ContentValues new_values) {
		if( ! initializeDB() ) {
			Log.w(AUTHORITY,"Database unavailable…");
			return null;
		}
		ContentValues values = (new_values != null) ? new ContentValues(new_values) : new ContentValues();
		switch (sUriMatcher.match(uri)) {
			case MOBILE_SENSOR:
				long _id = database.insert(DATABASE_TABLES[0], MobileSensor_Data.DEVICE_ID, values);
				if (_id > 0) {
					Uri dataUri = ContentUris.withAppendedId(MobileSensor_Data.CONTENT_URI, _id);
					getContext().getContentResolver().notifyChange(dataUri, null);
					return dataUri;
				}
				throw new SQLException("Failed to insert row into " + uri);
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	
	@Override
	public boolean onCreate() {

        AUTHORITY = getContext().getPackageName() + ".provider.sos_mobile_sensor";

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], MOBILE_SENSOR); //URI for all records
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0]+"/#", MOBILE_SENSOR_ID); //URI for a single record

        tableMap = new HashMap<String, String>();
        tableMap.put(MobileSensor_Data._ID, MobileSensor_Data._ID);
        tableMap.put(MobileSensor_Data.TIMESTAMP, MobileSensor_Data.TIMESTAMP);
        tableMap.put(MobileSensor_Data.DEVICE_ID, MobileSensor_Data.DEVICE_ID);
        tableMap.put(MobileSensor_Data.MULTITASKING, MobileSensor_Data.MULTITASKING);
        tableMap.put(MobileSensor_Data.MOT, MobileSensor_Data.MOT);
        tableMap.put(MobileSensor_Data.NOISE_LEVEL, MobileSensor_Data.NOISE_LEVEL);
//		tableMap.put(MobileSensor_Data.AMBIENT_NOISE, MobileSensor_Data.AMBIENT_NOISE);
        tableMap.put(MobileSensor_Data.CALLS, MobileSensor_Data.CALLS);
        tableMap.put(MobileSensor_Data.MESSAGING, MobileSensor_Data.MESSAGING);
        tableMap.put(MobileSensor_Data.CALENDAR, MobileSensor_Data.CALENDAR);
        tableMap.put(MobileSensor_Data.EMAIL, MobileSensor_Data.EMAIL);
        tableMap.put(MobileSensor_Data.INSTALLATIONS, MobileSensor_Data.INSTALLATIONS);

		return true;
	}

	/**
	 * 
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		if( ! initializeDB() ) {
			Log.w(AUTHORITY,"Database unavailable…");
			return null;
		}
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		switch (sUriMatcher.match(uri)) {
			case MOBILE_SENSOR:
				qb.setTables(DATABASE_TABLES[0]);
				qb.setProjectionMap(tableMap);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
		try {
			Cursor c = qb.query(database, projection, selection, selectionArgs, null, null, sortOrder);
			c.setNotificationUri(getContext().getContentResolver(), uri);
			return c;
		} catch (IllegalStateException e) {
			if (Aware.DEBUG)
				Log.e(Aware.TAG, e.getMessage());
			return null;
		}
	}

	/**
	 * 
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if( ! initializeDB() ) {
			Log.w(AUTHORITY,"Database unavailable…");
			return 0;
		}
		int count = 0;
		switch (sUriMatcher.match(uri)) {
			case MOBILE_SENSOR:
				count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
				break;
			default:
				database.close();
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

}