package jp.satoshi.sakamoto.mapsminusext.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import jp.satoshi.sakamoto.mapsminusext.R;
import jp.satoshi.sakamoto.mapsminusext.coderminus.maps.*;
import jp.satoshi.sakamoto.mapsminusext.db.*;
import jp.satoshi.sakamoto.mapsminusext.ext.ExtOsmMapView;

public class MapsActivity extends Activity implements TileQueueSizeWatcher
{
	
	private static final int MENU_UPDATE_MAP_ID     = Menu.FIRST;
	private static final int MENU_SAVE_MAP_ID       = Menu.FIRST +  1;
	private static final int MENU_MY_LOCATION_ID    = Menu.FIRST +  2;
	private static final int MENU_PREFERENCES_ID    = Menu.FIRST +  3;
	
	private static final int EDIT_PREFERENCES_CODE = 0;
	private static final int SELECT_CACHE_LEVEL_DIALOG = 1;
	protected static final String TAG = "MapsActivity";
	
	private ExtOsmMapView mapView;
	private SharedPreferences prefs;
	private int zoomLevel = 1;
	private LocationManager locationManager = null;
	private TextView localQueuTextView;
	private TextView remoteQueueTextView;
	private TextView resizingQueueTextView;
	private TextView zoomPosTextView;
	private ImageButton zoomInButton;
	private ImageButton zoomOutButton;
	private CacheLevelAdapter cacheLevelAdapter;
	
	private SQLiteDatabase db = null;
	
	//	private Listener gpsStatusListener = new Listener() {
//		
//		@Override
//		public void onGpsStatusChanged(int event) {
//			//GpsStatus gpsStatus = locationManager.getGpsStatus(null);
//			
//			//onMyLocation();
//			//gpsStatus.
//		}
//		
//	};
	
	public class CacheLevel 
	{
		
		private String text;
		private Bitmap icon;
		
		public CacheLevel(String text, Bitmap icon) 
		{
			this.text = text;
			this.icon = icon;
		}
		
		public String getText() {
			return this.text;
		}
		
		public Bitmap getIcon() {
			return this.icon;
		}
		
	}
	
	private LocationListener locationListener = new LocationListener() 
	{
		@Override
		public void onLocationChanged(Location location) 
		{
			Log.d(TAG, "location changed : lat - " + location.getLatitude() + "lon - " + location.getLongitude());
			
			// modified 20120528
			LocationData.save(db, location.getLatitude(), location.getLongitude());
			
			if(PreferenceManager.getDefaultSharedPreferences(MapsActivity.this).getBoolean("KEY_AUTOFOLLOW_LOCATION", false)) 
			{
				onMyLocation(location);
			}
		}
		
		@Override
		public void onProviderDisabled(String provider) 
		{
			Log.d(TAG, "provider disabled : " + provider);
		}
		
		@Override
		public void onProviderEnabled(String provider) 
		{
			Log.d(TAG, "provider enabled : " + provider);
		}
		
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) 
		{
		}
	};
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		final RelativeLayout rl = (RelativeLayout)findViewById(R.id.relativeLayout);
		
		mapView = (ExtOsmMapView) findViewById(R.id.osmMapView);
		mapView.setSizeWatcher(this);
		
		initializeDataMembers();
		
		// modified 20120527
		registerLocationListeners();
		
		//locationManager.addGpsStatusListener(gpsStatusListener);
		
		localQueuTextView = (TextView)findViewById(R.id.localQueuTextView);
		localQueuTextView.setTextColor(Color.WHITE);
		localQueuTextView.setShadowLayer(1.0f, 0.3f, 0.3f, Color.BLACK);
		localQueuTextView.setTypeface(Typeface.DEFAULT_BOLD);
		
		resizingQueueTextView = (TextView)findViewById(R.id.resizedQueuTextView);
		resizingQueueTextView.setTextColor(Color.WHITE);
		resizingQueueTextView.setShadowLayer(1.0f, 0.3f, 0.3f, Color.BLACK);
		resizingQueueTextView.setTypeface(Typeface.DEFAULT_BOLD);
		
		remoteQueueTextView = (TextView)findViewById(R.id.remoteQueuTextView);
		remoteQueueTextView.setTextColor(Color.WHITE);
		remoteQueueTextView.setShadowLayer(1.0f, 0.3f, 0.3f, Color.BLACK);
		remoteQueueTextView.setTypeface(Typeface.DEFAULT_BOLD);
		
		zoomPosTextView = new TextView(this);
		zoomPosTextView.setTextColor(Color.WHITE);
		zoomPosTextView.setShadowLayer(1.0f, 0.3f, 0.3f, Color.BLACK);
		zoomPosTextView.setTypeface(Typeface.DEFAULT_BOLD);
		
		zoomPosTextView.setText("Z : " + zoomLevel);
		final RelativeLayout.LayoutParams zoomTextParams = 
			new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		zoomTextParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		zoomTextParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		rl.addView(zoomPosTextView, zoomTextParams);
		
		zoomOutButton = new ImageButton(this);
		zoomOutButton.setBackgroundColor(Color.TRANSPARENT);
		zoomOutButton.setImageResource(android.R.drawable.btn_minus);
		zoomOutButton.setOnClickListener(new OnClickListener() 
		{
				@Override
				public void onClick(View v) 
				{
					onZoomOut();
				}
				});
		
		final RelativeLayout.LayoutParams zoomOutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		zoomOutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		zoomOutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rl.addView(zoomOutButton, zoomOutParams);
		
		zoomInButton = new ImageButton(this);
		zoomInButton.setBackgroundColor(Color.TRANSPARENT);
		zoomInButton.setImageResource(android.R.drawable.btn_plus);
		zoomInButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				onZoomIn();
			}
			});
		
		final RelativeLayout.LayoutParams zoomInParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		zoomInParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		zoomInParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rl.addView(zoomInButton, zoomInParams);
		
		cacheLevelAdapter = new CacheLevelAdapter(this);
		
		initializeCacheLevelAdapter();
		
		DBManager dbManager = new DBManager(this,
				DBManager.DB_NAME,
				null,
				DBManager.DB_VERSION);
		db = dbManager.getWritableDatabase();
		
	}
	
	private void initializeCacheLevelAdapter() 
	{
		cacheLevelAdapter.addCacheLevel(
				new CacheLevel(
						"Level 2", 
						BitmapFactory.decodeResource(getResources(), R.drawable.level2)));
		cacheLevelAdapter.addCacheLevel(
				new CacheLevel(
						"Level 3", 
						BitmapFactory.decodeResource(getResources(), R.drawable.level3)));
		cacheLevelAdapter.addCacheLevel(
				new CacheLevel(
						"Level 4", 
						BitmapFactory.decodeResource(getResources(), R.drawable.level4)));
		cacheLevelAdapter.addCacheLevel(
				new CacheLevel(
						"Level 5", 
						BitmapFactory.decodeResource(getResources(), R.drawable.level5)));
		cacheLevelAdapter.addCacheLevel(
				new CacheLevel(
						"Level 6", 
						BitmapFactory.decodeResource(getResources(), R.drawable.level6)));
		cacheLevelAdapter.addCacheLevel(
				new CacheLevel(
						"Level 7", 
						BitmapFactory.decodeResource(getResources(), R.drawable.level7)));
		cacheLevelAdapter.addCacheLevel(
				new CacheLevel(
						"Level 8", 
						BitmapFactory.decodeResource(getResources(), R.drawable.level8)));
		cacheLevelAdapter.addCacheLevel(
				new CacheLevel(
						"Level 9", 
						BitmapFactory.decodeResource(getResources(), R.drawable.level9)));
		cacheLevelAdapter.addCacheLevel(
				new CacheLevel(
						"Level 10", 
						BitmapFactory.decodeResource(getResources(), R.drawable.level10)));
		cacheLevelAdapter.addCacheLevel(
				new CacheLevel(
						"Level 11", 
						BitmapFactory.decodeResource(getResources(), R.drawable.level11)));
		cacheLevelAdapter.addCacheLevel(
				new CacheLevel(
						"Level 12", 
						BitmapFactory.decodeResource(getResources(), R.drawable.level12)));
		cacheLevelAdapter.addCacheLevel(
				new CacheLevel(
						"Level 13", 
						BitmapFactory.decodeResource(getResources(), R.drawable.level13)));
		cacheLevelAdapter.addCacheLevel(
				new CacheLevel(
						"Level 14", 
						BitmapFactory.decodeResource(getResources(), R.drawable.level14)));
		cacheLevelAdapter.addCacheLevel(
				new CacheLevel(
						"Level 15", 
						BitmapFactory.decodeResource(getResources(), R.drawable.level15)));
		cacheLevelAdapter.addCacheLevel(
				new CacheLevel(
						"Level 16", 
						BitmapFactory.decodeResource(getResources(), R.drawable.level16)));
		cacheLevelAdapter.addCacheLevel(
				new CacheLevel(
						"Level 17", 
						BitmapFactory.decodeResource(getResources(), R.drawable.level17)));
	}
	
	private void initializeDataMembers() 
	{
		prefs = getSharedPreferences("MapsMinus", Context.MODE_PRIVATE);
		zoomLevel = prefs.getInt("zoomLevel", 1);
		
		mapView.setZoom(zoomLevel);
		mapView.setOffsetX(prefs.getInt("offsetX", 0));
		mapView.setOffsetY(prefs.getInt("offsetY", 0));
		
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	}
	
	protected void onZoomOut() 
	{
		if(zoomLevel > 0) 
		{
			--zoomLevel;
			mapView.animateZoomOut(zoomLevel);
			zoomPosTextView.setText("Z : " + (zoomLevel));
			zoomOutButton.setEnabled(false);
		}
	}
	
	protected void onZoomIn() 
	{
		if(zoomLevel < 18) 
		{
			++zoomLevel;
			mapView.animateZoomIn(zoomLevel);
			zoomPosTextView.setText("Z : " + (zoomLevel));
			zoomInButton.setEnabled(false);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_UPDATE_MAP_ID , 0, R.string.menu_update_map ).setIcon(android.R.drawable.ic_menu_mapmode   );
		menu.add(0, MENU_SAVE_MAP_ID   , 0, R.string.menu_save_map   ).setIcon(android.R.drawable.ic_menu_save      );
		
		menu.add(0, MENU_MY_LOCATION_ID, 0, R.string.menu_my_location).setIcon(android.R.drawable.ic_menu_mylocation );
		menu.add(0, MENU_PREFERENCES_ID, 0, R.string.menu_preferences).setIcon(android.R.drawable.ic_menu_preferences);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) 
		{
			case MENU_UPDATE_MAP_ID: 
			{
				onUpdateMap();
				break;
			}
			case MENU_SAVE_MAP_ID: 
			{
				onSaveMap();
				break;
			}
			case MENU_MY_LOCATION_ID: 
			{
				Criteria criteria = new Criteria();
				criteria.setAccuracy(Criteria.ACCURACY_FINE);
				criteria.setAltitudeRequired(false);
				criteria.setBearingRequired(false);
				criteria.setCostAllowed(true);
				criteria.setPowerRequirement(Criteria.POWER_LOW);
				// for the case "provider == null" modified 20130220
				if (locationManager.getBestProvider(criteria, true) != null) {
				onMyLocation(locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, true)));
				}
				break;
			}
			case MENU_PREFERENCES_ID: 
			{
				onPreferences();
				break;
			}
		}			
		
		return super.onOptionsItemSelected(item);
	}
	
	private void onPreferences() 
	{
		Intent intent = new Intent(this, EditPreferencesActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		startActivityForResult(intent, EDIT_PREFERENCES_CODE);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		if(requestCode == EDIT_PREFERENCES_CODE) 
		{
			resetLocationListener();
			setScreenProperties();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private void resetLocationListener() 
	{
		unregisterLocationListeners();
		registerLocationListeners();
	}
	
	private void onMyLocation(Location location) 
	{
		
		if(location != null)
		{
			
			final Double lat = location.getLatitude();  // *1E6;
			final Double lon = location.getLongitude(); // *1E6;
			
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
				
				mapView.setPastLocationListFromDB(db);
				
				mapView.centerMapTo(lon, lat);
				
				mapView.invalidate();
				
				}
			});
			
		}
		
	}
	
	private void onSaveMap() 
	{
		if(zoomLevel > 1) 
		{
			cacheLevelAdapter.setCurrentZoomLevel(zoomLevel - 2);
			cacheLevelAdapter.notifyDataSetChanged();
		}
		showDialog(SELECT_CACHE_LEVEL_DIALOG);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) 
	{
		switch (id) 
		{
			case SELECT_CACHE_LEVEL_DIALOG:
				return new AlertDialog.Builder(this)
					.setTitle("Select Cache Level")
					.setAdapter(cacheLevelAdapter, new DialogInterface.OnClickListener() 
					{
							@Override
							public void onClick(DialogInterface dialog, int which) 
							{
								onCacheLevelSelected(which + zoomLevel);
							}
					})
					.setNeutralButton("Cancel", new DialogInterface.OnClickListener() 
					{
							public void onClick(DialogInterface dialog, int whichButton) 
							{
							}
					})
					.create();
		}
		return null;
	}
	
	protected void onCacheLevelSelected(int level) 
	{
		mapView.cacheCurrentMap(level);
	}
	
	private void onUpdateMap() 
	{
		mapView.clearCurrentCache();
		mapView.invalidate();
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) 
	{
		savedInstanceState.putInt("offsetX", mapView.getOffsetX());
		savedInstanceState.putInt("offsetY", mapView.getOffsetY());
		super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) 
	{
		super.onRestoreInstanceState(savedInstanceState);
		mapView.setOffsetX(savedInstanceState.getInt("offsetX"));
		mapView.setOffsetY(savedInstanceState.getInt("offsetY"));
	}
	
	//@Override
	protected void onResume() {
		// modified 20120527
		//registerLocationListeners();
		setScreenProperties();
		super.onResume();
	}
	
	private void setScreenProperties() 
	{
		if(PreferenceManager.getDefaultSharedPreferences(MapsActivity.this).getBoolean("KEY_SCREEN_ON", false)) 
		{
			mapView.setKeepScreenOn(true);
		}
		else
		{
			mapView.setKeepScreenOn(false);
		}
	}
	
	private void registerLocationListeners() {
		long updateTime = 300;
		float updateDistance = 25.0f;
		try 
		{
			updateTime = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(MapsActivity.this).getString("KEY_LOCATION_UPDATE_TIME", "50"));
			updateDistance = Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(MapsActivity.this).getString("KEY_LOCATION_UPDATE_DISTANCE", "5"));
		}
		catch (Exception e) 
		{
			// TODO: handle exception
		}
		
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				updateTime*1000, updateDistance, locationListener);
	}
	
	@Override
	protected void onPause() 
	{
		super.onPause();
		SharedPreferences.Editor ed = prefs.edit();
		ed.putInt("offsetX", mapView.getOffsetX());
		ed.putInt("offsetY", mapView.getOffsetY());
		ed.putInt("zoomLevel", zoomLevel);
		ed.commit();
		// modified 20120527
		//unregisterLocationListeners();
	}
	
	private void unregisterLocationListeners() 
	{
		locationManager.removeUpdates(locationListener);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterLocationListeners();
		// Database close
		db.close();
	}
	
	@Override
	public void onSizeChanged(int size, int id) 
	{
		if(size == 0)
		{
			if(id == 0) localQueuTextView.setText("");
			if(id == 1) remoteQueueTextView.setText("");
			if(id == 2) resizingQueueTextView.setText("");
		}
		else
		{
			if(id == 0)	
			{
				localQueuTextView.setText  ("Loading : " + size);
			}
			if(id == 1) 
			{
//				if(!remoteQueueTextView.getText().toString().equals("Caching : " + size))
				{
					remoteQueueTextView.setText("Caching : " + size);
				}
			}
			if(id == 2)	
			{
				resizingQueueTextView.setText  ("Resizing : " + size);
			}
		}
	}
	
	@Override
	public void enableZoomIn() 
	{
		zoomInButton.setEnabled(true);
	}
	
	@Override
	public void enableZoomOut() 
	{
		zoomOutButton.setEnabled(true);
	}
	
}