package com.syn.synaturequeue;

import org.ksoap2.serialization.PropertyInfo;
import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings.Secure;
import com.j1tth4.util.DotNetWebServiceTask;

public class QueueDisplayMainService extends DotNetWebServiceTask {
	public static final String PARAM_SHOP_ID = "iShopID";
	public static final String PARAM_DEVICE_CODE = "szDeviceCode";
	public static final String GET_CURR_ALL_QUEUE_METHOD = "WSiQueue_JSON_GetCurrentAllQueueDisplay";
	
	@SuppressLint({ "NewApi", "InlinedApi" })
	public QueueDisplayMainService(Context c, String method, String shopId) {
		super(c, method);
		
		mProperty = new PropertyInfo();
		mProperty.setName(PARAM_SHOP_ID);
		mProperty.setValue(shopId);
		mProperty.setType(int.class);
		mSoapRequest.addProperty(mProperty);
		
		mProperty = new PropertyInfo();
		mProperty.setName(PARAM_DEVICE_CODE);
		mProperty.setValue(Secure.getString(c.getContentResolver(),
				Secure.ANDROID_ID));
		mProperty.setType(String.class);
		mSoapRequest.addProperty(mProperty);
	}
}
