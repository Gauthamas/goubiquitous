package com.example.android.sunshine.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class ListenPhoneService extends WearableListenerService {
	public final String LOG_TAG = "ListenPhoneService";
    public static final String LOCAL_DATA = "local_data";

    public static final String WEATHER_HIGH = "watch_high";
    public static final String WEATHER_LOW = "watch_low";
    public static final String WEATHER_DESC = "watch_desc";
    public static final String WEATHER_ID = "watch_weather_id";
    public static final String WATCH_TIME = "watch_time";

    public ListenPhoneService() {
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.d(LOG_TAG, "message received");
    }
	
	@Override
    public void onDataChanged(DataEventBuffer dataEvents) {
		DataMap dataMap;
        Log.d(LOG_TAG, "data received");
        for (DataEvent event : dataEvents) {

            
            if (event.getType() == DataEvent.TYPE_CHANGED) {

			    String formatHigh, formatLow, desc;
				int weatherId;
				long watchTime;
                dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
				
				formatHigh=dataMap.getString(WEATHER_HIGH);
                formatLow=dataMap.getString(WEATHER_LOW);
                desc=dataMap.getString(WEATHER_DESC);
                weatherId = dataMap.getInt(WEATHER_ID);
                watchTime = dataMap.getLong(WATCH_TIME);
				
				Log.d(LOG_TAG, "Temperature: " + formatHigh + ", " + formatLow+" , "+desc);
                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);

                Intent i = new Intent(LOCAL_DATA);
                i.putExtra(WEATHER_HIGH, formatHigh);
                i.putExtra(WEATHER_LOW, formatLow);
                i.putExtra(WEATHER_DESC, desc);
                i.putExtra(WEATHER_ID, weatherId);
                i.putExtra(WATCH_TIME, watchTime);

                localBroadcastManager.sendBroadcast(i);

                
            }
		}			
	}
}
