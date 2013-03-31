package jp.satoshi.sakamoto.mapsminusext.ext;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import jp.satoshi.sakamoto.mapsminusext.R;
import jp.satoshi.sakamoto.mapsminusext.coderminus.maps.OsmMapView;
import jp.satoshi.sakamoto.mapsminusext.db.LocationData;

public class ExtOsmMapView extends OsmMapView {
	
	private ArrayList<PastLocation> pastLocationList = null;
	
	private Paint paint = new Paint();
	private Bitmap pastPos = null;
	
	public ExtOsmMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		pastPos = BitmapFactory.decodeResource(this.getContext().getResources(), R.drawable.ic_maps_indicator_past_position);
		this.pastLocationList = new ArrayList<PastLocation>();
	}
	
	public static class PastLocation {
		
		private double latitude;
		private double longitude;
		
		public PastLocation(double latitude, double longitude) {
			this.latitude  = latitude;
			this.longitude = longitude;
		}
		
		public double getLatitude() {
			return this.latitude;
		}
		public double getLongitude() {
			return this.longitude;
		}
		
	}
	
	public ArrayList<PastLocation> getPastLocationList() {
		return this.pastLocationList;
	}
	
	public void setPastLocationList(ArrayList<PastLocation> pastLocationList) {
		this.pastLocationList = pastLocationList;
	}
	
	public void setPastLocationListFromDB(SQLiteDatabase db) {
		
		// modified 20130225
		
		if (db != null) {
		
		Cursor cursor = LocationData.reload(db);
		
		if (cursor != null) {
			
			ArrayList<PastLocation> pastLocationList = new ArrayList<PastLocation>();
			
			cursor.moveToFirst();
			int size = cursor.getCount();
			// skip the first record (current location)
			cursor.moveToNext();
			for (int i = 0; i < size - 1; i++) {
			PastLocation pastLocation;
			pastLocation = new ExtOsmMapView.PastLocation(cursor.getDouble(LocationData.LATITUDE), cursor.getDouble(LocationData.LONGITUDE));
			pastLocationList.add(pastLocation);
			cursor.moveToNext();
			}
			cursor.close();
			
			setPastLocationList(pastLocationList);
			
		}
		
		}
		
		// dummy-test 20130225
		//setDummyLocation();
		
	}
	
	private void setDummyLocation() {
		
		// SANGENJAYA - station
		PastLocation dummyLocation1 = new PastLocation(35.643515, 139.671162);
		this.pastLocationList.add(dummyLocation1);
		
		// SHIBUYA - station
		PastLocation dummyLocation2 = new PastLocation(35.658517, 139.701334);
		this.pastLocationList.add(dummyLocation2);
		
		// DAIKANYAMA - station
		PastLocation dummyLocation3 = new PastLocation(35.648104, 139.703168);
		this.pastLocationList.add(dummyLocation3);
		
		// EBISU - station
		PastLocation dummyLocation4 = new PastLocation(35.64669, 139.710106);
		this.pastLocationList.add(dummyLocation4);
		
	}
	
	private synchronized void drawPastLocation(Canvas canvas) {
		
		for (PastLocation pastLocation : this.pastLocationList) {
			
			if (pastLocation != null && 
				pastLocation.getLatitude()  != 0 && 
				pastLocation.getLongitude() != 0) {
				
				double merc_x = convertLonToMercX(pastLocation.getLongitude());
				double merc_y = convertLatToMercY(pastLocation.getLatitude());
				
				double mapWidth = (Math.pow(2, zoomLevel) * 256);
				
				double pixelSize = mapWidth / (Mercator.MAX_X * 2);
				
				double pixelX = Mercator.MAX_X - (0 - merc_x);
				double pixelY = Mercator.MAX_Y - merc_y;
				
				// for the BUG "offsetPixelX cast doubule → int"
				// modified "offsetPixelX cast doubule → long → int"
				// the case Android 2.2 au IS03  20130227
				
				int offsetPixelX = (int) (long) (pixelX * pixelSize);
				int offsetPixelY = (int) (long) (pixelY * pixelSize);
				
				int locationOffsetX = 0 - offsetPixelX;
				int locationOffsetY = 0 - offsetPixelY;
				
				int x = getOffsetX() - locationOffsetX - 20;
				int y = getOffsetY() - locationOffsetY - 20;
				
				canvas.drawBitmap(
					pastPos, 
					x, 
					y, this.paint);
				
			}
			
		}
		
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		drawPastLocation(canvas);
	}
	
}
