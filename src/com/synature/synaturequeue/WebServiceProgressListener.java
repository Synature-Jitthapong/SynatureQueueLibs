package com.synature.synaturequeue;

public interface WebServiceProgressListener {
	void onPre();
	void onPost();
	void onError(String msg);
}
