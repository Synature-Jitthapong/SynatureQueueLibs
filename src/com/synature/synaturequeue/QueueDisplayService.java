package com.synature.synaturequeue;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.synature.pos.QueueDisplayInfo;

import android.content.Context;

public class QueueDisplayService extends QueueDisplayMainService {
	private LoadQueueListener mListener;
	
	public QueueDisplayService(Context c, String shopId, LoadQueueListener listener) {
		super(c, GET_CURR_ALL_QUEUE_METHOD, shopId);
		mListener = listener;
	}

	@Override
	protected void onPostExecute(String result) {
		Gson gson = new Gson();
		Type type = new TypeToken<QueueDisplayInfo>() {}.getType();
		try {
			QueueDisplayInfo queueDisplayInfo = 
					(QueueDisplayInfo) gson.fromJson(result, type);
			
			mListener.onPost(queueDisplayInfo);
		} catch (Exception e) {
			mListener.onError(e.getMessage());
		}
	}

	@Override
	protected void onPreExecute() {
		mListener.onPre();
	}
	
	public static interface LoadQueueListener extends WebServiceProgressListener{
		void onPost(QueueDisplayInfo qInfo);
	}
}
