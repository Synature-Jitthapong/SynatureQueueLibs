package com.syn.synaturequeue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.syn.pos.QueueDisplayInfo;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

@SuppressLint("NewApi")
public class SynatureQueueFragment extends Fragment implements Runnable, 
	QueueServerSocket.ServerSocketListener, SpeakCallingQueue.OnPlaySoundListener{
	
	/**
	 * Timer for update queue
	 */
	private Timer mTimerUpdateQueue;
	
	private String mShopId;
	private String mServerUrl;
	private String mSoundDir;
	
	private TextView mTvCallingA;
	private TextView mTvCallingB;
	private TextView mTvCallingC;
	private TextView mTvCustA;
	private TextView mTvCustB;
	private TextView mTvCustC;
	private TextView mTvSumA;
	private TextView mTvSumB;
	private TextView mTvSumC;
	private ListView mLvA;
	private ListView mLvB;
	private ListView mLvC;

	/**
	 * @param shopId
	 * @param serverUrl
	 * @param soundDir
	 * @return Fragment instance
	 */
	public static SynatureQueueFragment newInstance(String shopId, String serverUrl, 
			String soundDir){
		SynatureQueueFragment f = new SynatureQueueFragment();
		Bundle b = new Bundle();
		b.putString("shopId", shopId);
		b.putString("serverUrl", serverUrl);
		b.putString("soundDir", soundDir);
		f.setArguments(b);
		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mShopId = getArguments().getString("shopId");
		mServerUrl = getArguments().getString("serverUrl");
		mSoundDir = getArguments().getString("soundDir");
		
		mServerUrl += "/ws_mpos.asmx";
		
		mTimerUpdateQueue = new Timer();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.queue_container, container, false);
			View viewA = (View) rootView.findViewById(R.id.queueA);
			View viewB = (View) rootView.findViewById(R.id.queueB);
			View viewC = (View) rootView.findViewById(R.id.queueC);
			mLvA = (ListView) viewA.findViewById(R.id.lvQueue);
			mLvB = (ListView) viewB.findViewById(R.id.lvQueue);
			mLvC = (ListView) viewC.findViewById(R.id.lvQueue);
			mTvCallingA = (TextView) viewA.findViewById(R.id.tvCalling);
			mTvCallingB = (TextView) viewB.findViewById(R.id.tvCalling);
			mTvCallingC = (TextView) viewC.findViewById(R.id.tvCalling);
			mTvCustA = (TextView) viewA.findViewById(R.id.tvCustName);
			mTvCustB = (TextView) viewB.findViewById(R.id.tvCustName);
			mTvCustC = (TextView) viewC.findViewById(R.id.tvCustName);
			mTvSumA = (TextView) viewA.findViewById(R.id.tvSum);
			mTvSumB = (TextView) viewB.findViewById(R.id.tvSum);
			mTvSumC = (TextView) viewC.findViewById(R.id.tvSum);
			
			((TextView) viewA.findViewById(R.id.tvQueueTitle)).setText("A");
			((TextView) viewB.findViewById(R.id.tvQueueTitle)).setText("B");
			((TextView) viewC.findViewById(R.id.tvQueueTitle)).setText("C");
		return rootView;
	}

	private void startUpdateQueue(){
		mTimerUpdateQueue.schedule(new UpdateQueueTask(), 1000, 10 * 1000);
	}
	
	private void stopUpdateQueue(){
		try {
			mTimerUpdateQueue.cancel();
			mTimerUpdateQueue.purge();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		startSocket();
		startUpdateQueue();
	}

	@Override
	public void onDestroy() {
		stopSocket();
		stopUpdateQueue();
		super.onDestroy();
	}
	
	/**
	 * Called by mLoadQueueListener
	 * @param queueDisplayInfo
	 */
	private void updateQueue(QueueDisplayInfo queueDisplayInfo){
		List<QueueDisplayInfo.QueueInfo> listA = 
				new ArrayList<QueueDisplayInfo.QueueInfo>();
		List<QueueDisplayInfo.QueueInfo> listB = 
				new ArrayList<QueueDisplayInfo.QueueInfo>();
		List<QueueDisplayInfo.QueueInfo> listC = 
				new ArrayList<QueueDisplayInfo.QueueInfo>();
		
		int totalQueueA = 0;
		int totalQueueB = 0;
		int totalQueueC = 0;
		for(QueueDisplayInfo.QueueInfo queueInfo : queueDisplayInfo.xListQueueInfo){
			if(queueInfo.getiQueueGroupID() == 1){
				listA.add(queueInfo);
				totalQueueA++;
			}
			
			if(queueInfo.getiQueueGroupID() == 2){
				listB.add(queueInfo);
				totalQueueB++;
			}
			
			if(queueInfo.getiQueueGroupID() == 3){
				listC.add(queueInfo);
				totalQueueC++;
			}
		}
		
		if(!queueDisplayInfo.getSzCurQueueGroupA().equals("")){
			mTvCallingA.setText(queueDisplayInfo.getSzCurQueueGroupA());
			mTvCustA.setText(queueDisplayInfo.getSzCurQueueCustomerA());
			//mQueueDatabase.addCallingQueue(queueDisplayInfo.getSzCurQueueGroupA());
		}else{
			mTvCallingA.setText(null);
			mTvCustA.setText(null);
		}
		
		if(!queueDisplayInfo.getSzCurQueueGroupB().equals("")){
			mTvCallingB.setText(queueDisplayInfo.getSzCurQueueGroupB());
			mTvCustB.setText(queueDisplayInfo.getSzCurQueueCustomerB());
			//mQueueDatabase.addCallingQueue(queueDisplayInfo.getSzCurQueueGroupB());
		}else{
			mTvCallingB.setText(null);
			mTvCustB.setText(null);
		}
		
		if(!queueDisplayInfo.getSzCurQueueGroupC().equals("")){
			mTvCallingC.setText(queueDisplayInfo.getSzCurQueueGroupC());
			mTvCustC.setText(queueDisplayInfo.getSzCurQueueCustomerC());
			//mQueueDatabase.addCallingQueue(queueDisplayInfo.getSzCurQueueGroupC());
		}else{
			mTvCallingC.setText(null);
			mTvCustC.setText(null);
		}
		
		mLvA.setAdapter(new TableQueueAdapter(getActivity(), listA));
		mLvB.setAdapter(new TableQueueAdapter(getActivity(), listB));
		mLvC.setAdapter(new TableQueueAdapter(getActivity(), listC));
		mTvSumA.setText(String.valueOf(totalQueueA));
		mTvSumB.setText(String.valueOf(totalQueueB));
		mTvSumC.setText(String.valueOf(totalQueueC));
	}

	/**
	 * Listener for load queue
	 */
	private QueueDisplayService.LoadQueueListener mLoadQueueListener = 
			new QueueDisplayService.LoadQueueListener() {
				
				@Override
				public void onPre() {
				}
				
				@Override
				public void onPost() {
				}
				
				@Override
				public void onError(String msg) {
				}
				
				@Override
				public void onPost(QueueDisplayInfo queueInfo) {
					updateQueue(queueInfo);
				}
	};
	
	/**
	 * @author j1tth4
	 * Timer Task for request queue from web service
	 */
	private class UpdateQueueTask extends TimerTask{

		@Override
		public void run() {
			if(!mServerUrl.isEmpty()){
				try {
					new QueueDisplayService(getActivity(), mShopId,
							mLoadQueueListener).execute(mServerUrl);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	private Thread mConnThread;
	private QueueServerSocket mSocket;
	
	@SuppressLint("NewApi")
	private void startSocket(){
		if(!this.mServerUrl.isEmpty()){
			try {
				mSocket = new QueueServerSocket(this);
				mConnThread = new Thread(mSocket);
				mConnThread.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
	}
	
	private synchronized void stopSocket(){
		if(mConnThread != null)
		{
			try {
				mConnThread.interrupt();
				mConnThread = null;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void setServerUrl(String serverUrl) {
		this.mServerUrl = serverUrl;
		startSocket();
	}

	public void setSoundDir(String soundDir) {
		this.mSoundDir = soundDir;
	}

	@Override
	public void run() {
//		while(!mLoadCallingQueueThread.isInterrupted()){
//			if(mQueueIdx == -1){
//				mCallingQueueLst = mQueueDatabase.listCallingQueueName(
//						Integer.parseInt(QueueApplication.getSpeakTimes()));
//				if(mCallingQueueLst.size() > 0){
//					mQueueIdx = 0;
//					mHandlerSpeakQueue.postDelayed(mSpeakQueueRunnable, SPEAK_DELAY);
//				}
//			}
//		}
	}

	@Override
	public void onReceipt(String msg) {
		stopUpdateQueue();
		startUpdateQueue();
	}

	@Override
	public void onAcceptErr(String msg) {
	}

	@Override
	public void onSpeaking() {
		
	}

	@Override
	public void onSpeakComplete() {
		
	}
}
