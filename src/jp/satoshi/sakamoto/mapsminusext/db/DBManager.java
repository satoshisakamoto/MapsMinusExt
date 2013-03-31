package jp.satoshi.sakamoto.mapsminusext.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public class DBManager extends SQLiteOpenHelper {
	
	public static final int DB_VERSION = 1;
	
	public static final String DB_NAME = "location.db";
	
	public static final String LOCATION_TABLE = "location";
	
	public DBManager(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		// CREATE TABLE
		String sql = "CREATE TABLE " + LOCATION_TABLE + " (" + "_id INTEGER PRIMARY KEY, "
				+ "time LONG, " + "longitude DOUBLE, " + "latitude DOUBLE " + ")";
		db.execSQL(sql);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
	
}
