package jp.satoshi.sakamoto.mapsminusext.coderminus.maps;

public interface TileQueueSizeWatcher 
{

	void onSizeChanged(int size, int id);

	void enableZoomOut();

	void enableZoomIn();

}
