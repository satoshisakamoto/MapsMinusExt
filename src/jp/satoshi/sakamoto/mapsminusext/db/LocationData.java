package jp.satoshi.sakamoto.mapsminusext.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class LocationData {
	
	private static final int MAX_LOCATION_SIZE_LIMIT = 100;
	
	// DB table column
	private static final String[] projection
			= new String[] {
					"_id",
					"time",
					"latitude",
					"longitude",
					};
	
	// column index
	public static final int ID        = 0;
	public static final int TIME      = 1;
	public static final int LATITUDE  = 2;
	public static final int LONGITUDE = 3;
	
	public static Cursor reload(SQLiteDatabase db) {
		Cursor cursor = db.query(DBManager.LOCATION_TABLE,
								projection,
								null, null, null, null,
								"time desc",
								String.valueOf(MAX_LOCATION_SIZE_LIMIT)
						);
		return cursor;
		
	}
	
	public static void save(SQLiteDatabase db, double latitude, double longitude) {
		ContentValues values = new ContentValues();
		values.put("time", System.currentTimeMillis());
		values.put("latitude", latitude);
		values.put("longitude", longitude);
		db.insert(DBManager.LOCATION_TABLE, null, values);
	}
	
}
