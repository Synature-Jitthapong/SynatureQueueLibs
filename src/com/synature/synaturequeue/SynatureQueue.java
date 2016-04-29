package com.synature.synaturequeue;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.synature.pos.QueueDisplayInfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

@SuppressLint({ "NewApi", "ViewConstructor" })
public class SynatureQueue extends LinearLayout implements 
	QueueServerSocket.ServerSocketListener, SpeakCallingQueue.OnPlaySoundListener{
	
	public static final String TAG = SynatureQueue.class.getSimpleName();
	
	public static final int SPEAK_DELAYED = 10 * 100;
	public static final int UPDATE_QUEUE_INTERVAL = 10 * 10000;
	public static final int LIMIT_SPEAK_TIME = 1;
	public static final int MAX_GROUP_QUEUE = 4;
	public static final int DEFAULT_GROUP_QUEUE = 3;
	
	/**
	 * Thread for run listener socket 
	 */
	private Thread mConnThread;
	private QueueServerSocket mSocket;
	
	/**
	 * Timer for update queue
	 */
	private Timer mTimerUpdateQueue;
	
	/**
	 * List for collect queue name for play calling sound
	 */
	private List<QueueName> mQueueNameLst = new ArrayList<QueueName>();
	
	/**
	 * Thread for load queue collection
	 */
	private Thread mLoadCallingPlaylistThread;

	/**
	 * Queue sound player
	 */
	private SpeakCallingQueue mSpeakCallingQueue;
	private Handler mHandlerSpeakQueue;
	
	/**
	 * The listener for handler on speak event
	 */
	private SpeakCallingQueue.OnPlaySoundListener mSpeakListener;
	
	/**
	 * Calling time
	 */
	private int mCallingTime = LIMIT_SPEAK_TIME;
	
	/**
	 * Current calling queue idx
	 */
	private int mCurrentQueueIdx = -1;
	
	/**
	 * Queue column
	 */
	private int mQueueGroupColumn = DEFAULT_GROUP_QUEUE;
	
	private Context mContext;
	private String mShopId;
	private String mServerUrl;
	private String mSoundDir;
	
	private TextView mTvCallingA;
	private TextView mTvCallingB;
	private TextView mTvCallingC;
	private TextView mTvCallingD;
	private TextView mTvSumA;
	private TextView mTvSumB;
	private TextView mTvSumC;
	private TextView mTvSumD;
	private ListView mLvA;
	private ListView mLvB;
	private ListView mLvC;
	private ListView mLvD;

	/**
	 * @param context
	 * @param shopId
	 * @param serverUrl
	 * @param soundDir
	 * @param queueColumn
	 * @param callingTime
	 * @param listener
	 */
	public SynatureQueue(Context context, String shopId, String serverUrl,
			String soundDir, int queueColumn, int callingTime, 
			SpeakCallingQueue.OnPlaySoundListener listener){
		super(context, null);

		mContext = context;
		mShopId = shopId;
		mServerUrl = serverUrl;
		mServerUrl += "/ws_mpos.asmx";
		mSoundDir = soundDir;
		mSpeakListener = listener;
		mCallingTime = callingTime;
		mQueueGroupColumn = queueColumn;
		
		mSpeakCallingQueue = new SpeakCallingQueue(mContext, 
				mSoundDir, this);
		mHandlerSpeakQueue = new Handler();
		setupView();
	}

	/**
	 * @param context
	 * @param shopId
	 * @param serverUrl
	 * @param soundDir
	 */
	public SynatureQueue(Context context, String shopId, String serverUrl,
			String soundDir){
		this(context, shopId, serverUrl, soundDir, DEFAULT_GROUP_QUEUE, 
				LIMIT_SPEAK_TIME, new SpeakCallingQueue.OnPlaySoundListener() {
					
					@Override
					public void onSpeaking() {
					}
					
					@Override
					public void onSpeakComplete() {
					}
		});
	}
	
	private void setupView() {
		LayoutInflater inflater = (LayoutInflater)
				mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rootView = inflater.inflate(R.layout.queue_container, null, false);
		View viewA = (View) rootView.findViewById(R.id.queueA);
		View viewB = (View) rootView.findViewById(R.id.queueB);
		View viewC = (View) rootView.findViewById(R.id.queueC);
		View viewD = (View) rootView.findViewById(R.id.queueD);
		mLvA = (ListView) viewA.findViewById(R.id.lvQueue);
		mLvB = (ListView) viewB.findViewById(R.id.lvQueue);
		mLvC = (ListView) viewC.findViewById(R.id.lvQueue);
		mLvD = (ListView) viewD.findViewById(R.id.lvQueue);
		mTvCallingA = (TextView) viewA.findViewById(R.id.tvCalling);
		mTvCallingB = (TextView) viewB.findViewById(R.id.tvCalling);
		mTvCallingC = (TextView) viewC.findViewById(R.id.tvCalling);
		mTvCallingD = (TextView) viewD.findViewById(R.id.tvCalling);
		mTvSumA = (TextView) viewA.findViewById(R.id.tvSum);
		mTvSumB = (TextView) viewB.findViewById(R.id.tvSum);
		mTvSumC = (TextView) viewC.findViewById(R.id.tvSum);
		mTvSumD = (TextView) viewD.findViewById(R.id.tvSum);
		
		((TextView) viewA.findViewById(R.id.tvQueueTitle)).setText("A");
		((TextView) viewB.findViewById(R.id.tvQueueTitle)).setText("B");
		((TextView) viewC.findViewById(R.id.tvQueueTitle)).setText("C");
		((TextView) viewD.findViewById(R.id.tvQueueTitle)).setText("D");
		
		switch(mQueueGroupColumn){
		case 1:
			viewB.setVisibility(View.GONE);
			viewC.setVisibility(View.GONE);
			viewD.setVisibility(View.GONE);
			break;
		case 2:
			viewC.setVisibility(View.GONE);
			viewD.setVisibility(View.GONE);
			break;
		case 3:
			viewD.setVisibility(View.GONE);
			break;
		}
		addView(rootView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 
				LinearLayout.LayoutParams.MATCH_PARENT));
	}

	/**
	 * Schedule update queue 
	 */
	private void startUpdateQueue(){
		try {
			mTimerUpdateQueue = new Timer();
			mTimerUpdateQueue.schedule(new UpdateQueueTask(), 1000, UPDATE_QUEUE_INTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Cancel schedule update queue
	 */
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
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		startSocket();
		startUpdateQueue();
		startLoadQueueThread();
	}

	@Override
	protected void onDetachedFromWindow() {
		stopUpdateQueue();
		stopLoadQueueThread();
		stopSocket();
		closeSocket();
		mHandlerSpeakQueue.removeCallbacks(mSpeakQueueRunnable);
		super.onDetachedFromWindow();
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
		List<QueueDisplayInfo.QueueInfo> listD = 
				new ArrayList<QueueDisplayInfo.QueueInfo>();
		
		int totalQueueA = 0;
		int totalQueueB = 0;
		int totalQueueC = 0;
		int totalQueueD = 0;
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
			if(queueInfo.getiQueueGroupID() == 4){
				listD.add(queueInfo);
				totalQueueD++;
			}
		}
		if(!queueDisplayInfo.getSzCurQueueGroupA().isEmpty()){
			String callingA = queueDisplayInfo.getSzCurQueueGroupA();
			mTvCallingA.setText(callingA);
		}else{
			mTvCallingA.setText(null);
		}
		if(!queueDisplayInfo.getSzCurQueueGroupB().isEmpty()){
			String callingB = queueDisplayInfo.getSzCurQueueGroupB();
			mTvCallingB.setText(callingB);
		}else{
			mTvCallingB.setText(null);
		}
		if(!queueDisplayInfo.getSzCurQueueGroupC().isEmpty()){
			String callingC = queueDisplayInfo.getSzCurQueueGroupC();
			mTvCallingC.setText(callingC);
		}else{
			mTvCallingC.setText(null);
		}if(!queueDisplayInfo.getSzCurQueueGroupD().isEmpty()){
			String callingD = queueDisplayInfo.getSzCurQueueGroupD();
			mTvCallingD.setText(callingD);
		}else{
			mTvCallingD.setText(null);
		}
		mLvA.setAdapter(new TableQueueAdapter(mContext, listA));
		mLvB.setAdapter(new TableQueueAdapter(mContext, listB));
		mLvC.setAdapter(new TableQueueAdapter(mContext, listC));
		mLvD.setAdapter(new TableQueueAdapter(mContext, listD));
		mTvSumA.setText(String.valueOf(totalQueueA));
		mTvSumB.setText(String.valueOf(totalQueueB));
		mTvSumC.setText(String.valueOf(totalQueueC));
		mTvSumD.setText(String.valueOf(totalQueueD));
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
					new QueueDisplayService(mContext, mShopId,
							mLoadQueueListener).execute(mServerUrl);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	/**
	 * Runnable play speak sound
	 */
	private Runnable mSpeakQueueRunnable = new Runnable(){

		@Override
		public void run() {
			if(mQueueNameLst.size() > 0){
				try {
					QueueName q = mQueueNameLst.get(mCurrentQueueIdx);
					int callingTime = q.getCallingTime();
					if(callingTime < mCallingTime){
						try {
							mSpeakCallingQueue.speak(q.getQueueName());
							q.setCallingTime(++callingTime);
							mQueueNameLst.set(mCurrentQueueIdx, q);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							onSpeakComplete();
							e.printStackTrace();
						}
					}else{
						// skip 
						onSpeakComplete();
					}
				} catch (Exception e) {
					// skip
					onSpeakComplete();
					e.printStackTrace();
				}
			}
		}
		
	};

	/**
	 * Thread for load queue playlist
	 */
	private void startLoadQueueThread(){
		mLoadCallingPlaylistThread = new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					while(!mLoadCallingPlaylistThread.isInterrupted()){
						if(mCurrentQueueIdx == -1){
							if(mQueueNameLst.size() > 0){
								mCurrentQueueIdx = 0;
								mHandlerSpeakQueue.postDelayed(mSpeakQueueRunnable, SPEAK_DELAYED);
							}
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally{
					mLoadCallingPlaylistThread = null;
				}
			}
			
		});
		mLoadCallingPlaylistThread.start();
	}
	
	/**
	 * stop load calling sound playlist
	 */
	private synchronized void stopLoadQueueThread(){
		if(mLoadCallingPlaylistThread != null)
		{
			try {
				mLoadCallingPlaylistThread.interrupt();
				mLoadCallingPlaylistThread = null;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Start server socket
	 */
	private void startSocket(){
		try {
			mSocket = new QueueServerSocket(this);
			mConnThread = new Thread(mSocket);
			mConnThread.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Stop socket thread
	 */
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
	
	/**
	 * Close socket
	 */
	private void closeSocket(){
		try {
			mSocket.closeSocket();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setShopId(String shopId){
		this.mShopId = shopId;
	}
	
	public void setServerUrl(String serverUrl) {
		this.mServerUrl = serverUrl;
	}

	public void setSoundDir(String soundDir) {
		this.mSoundDir = soundDir;
	}

	@Override
	public void onReceipt(String msg) {
		CallingQueue call = parseCallingQueue(msg);
		if(call != null){
			if(call.iCmdType == 2 && call.szQueueName != null){
				if(checkAddedQueue(call.szQueueName)){
					removeQueue(call.szQueueName);
				}
				mQueueNameLst.add(new QueueName(call.szQueueName));
			}
			stopUpdateQueue();
			startUpdateQueue();
		}
	}

	@Override
	public void onAcceptErr(String msg) {
	}

	@Override
	public void onSpeaking() {
		mSpeakListener.onSpeaking();
	}

	@Override
	public void onSpeakComplete() {
		if(mCurrentQueueIdx < mQueueNameLst.size() - 1){
			mHandlerSpeakQueue.postDelayed(mSpeakQueueRunnable, SPEAK_DELAYED);
			mCurrentQueueIdx++;
		}else{
			mQueueNameLst = new ArrayList<QueueName>();
			mCurrentQueueIdx = -1;
			mSpeakListener.onSpeakComplete();
		}
	}
	
	private int removeQueue(String queueName){
		Iterator<QueueName> it = mQueueNameLst.iterator();
		while(it.hasNext()){
			QueueName q = it.next();
			if(q.getQueueName().equals(queueName))
			{
				it.remove();
				return 1;
			}
		}
		return 0;
	}
	
	private boolean checkAddedQueue(String queueName){
		Iterator<QueueName> it = mQueueNameLst.iterator();
		while(it.hasNext()){
			QueueName q = it.next();
			if(q.getQueueName().equals(queueName))
			{
				return true;
			}
		}
		return false;
	}

	private CallingQueue parseCallingQueue(String json){
		Gson gson = new Gson();
		Type type = new TypeToken<CallingQueue>() {}.getType();
		CallingQueue callingQueue = null;
		try {
			callingQueue = gson.fromJson(json, type);
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			Log.d("parse calling queue", e.getMessage());
		}
		return callingQueue;
	}
	
	private class CallingQueue{
		private int iCmdType;
		private String szQueueName;
	}
	
	/**
	 * @author j1tth4
	 * 
	 */
	private class QueueName{
		private String queueName;
		private int callingTime;
		
		public QueueName(String queueName){
			this.queueName = queueName;
		}
		public String getQueueName() {
			return queueName;
		}
		public int getCallingTime() {
			return callingTime;
		}
		public void setCallingTime(int callingTime) {
			this.callingTime = callingTime;
		}
	}
}
