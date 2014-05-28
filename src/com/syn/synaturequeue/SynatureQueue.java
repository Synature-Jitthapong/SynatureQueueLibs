package com.syn.synaturequeue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.syn.pos.QueueDisplayInfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

@SuppressLint("NewApi")
public class SynatureQueue extends LinearLayout implements Runnable, 
	QueueServerSocket.ServerSocketListener, SpeakCallingQueue.OnPlaySoundListener{
	
	public static final int SPEAK_DELAYED = 10 * 100;
	public static final int UPDATE_QUEUE_INTERVAL = 10 * 1000;
	public static final int LIMIT_SPEAK_TIME = 3;
	
	private boolean mIsSpeaking = false;
	
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
	private Thread mLoadCallingQueueThread;

	/**
	 * Queue sound player
	 */
	private SpeakCallingQueue mSpeakCallingQueue;
	private Handler mHandlerSpeakQueue;
	
	/**
	 * Current calling queue idx
	 */
	private int mCurrentQueueIdx = -1;
	
	private Context mContext;
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

	public SynatureQueue(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		setupView();
	}

	public SynatureQueue(Context context, String shopId, String serverUrl,
			String soundDir){
		this(context, null);

		mShopId = shopId;
		mServerUrl = serverUrl;
		mServerUrl += "/ws_mpos.asmx";
		mSoundDir = soundDir;
		
		mTimerUpdateQueue = new Timer();
		mSpeakCallingQueue = new SpeakCallingQueue(mContext, 
				mSoundDir, this);
		mHandlerSpeakQueue = new Handler();
	}

	private void setupView() {
		LayoutInflater inflater = (LayoutInflater)
				mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rootView = inflater.inflate(R.layout.queue_container, null, false);
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
		addView(rootView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 
				LinearLayout.LayoutParams.MATCH_PARENT));
	}

	/**
	 * Schedule update queue 
	 */
	private void startUpdateQueue(){
		mTimerUpdateQueue.schedule(new UpdateQueueTask(), 1000, UPDATE_QUEUE_INTERVAL);
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
		stopSocket();
		stopUpdateQueue();
		stopLoadQueueThread();
		mHandlerSpeakQueue.removeCallbacks(mSpeakQueueRunnable);
		super.onDetachedFromWindow();
	}

	/**
	 * Called by mLoadQueueListener
	 * @param queueDisplayInfo
	 */
	private void updateQueue(QueueDisplayInfo queueDisplayInfo){
		QueueName qA = new QueueName();
		QueueName qB = new QueueName();
		QueueName qC = new QueueName();
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
		
		if(!queueDisplayInfo.getSzCurQueueGroupA().isEmpty()){
			String callingA = queueDisplayInfo.getSzCurQueueGroupA();
			mTvCallingA.setText(callingA);
			mTvCustA.setText(callingA);
			qA.setQueueName(callingA);
			if(!mQueueNameLst.contains(qA) && !mIsSpeaking)
				mQueueNameLst.add(qA);
		}else{
			mTvCallingA.setText(null);
			mTvCustA.setText(null);
		}
		
		if(!queueDisplayInfo.getSzCurQueueGroupB().isEmpty()){
			String callingB = queueDisplayInfo.getSzCurQueueGroupB();
			mTvCallingB.setText(callingB);
			mTvCustB.setText(callingB);
			qB.setQueueName(callingB);
			if(!mQueueNameLst.contains(qB) && !mIsSpeaking)
				mQueueNameLst.add(qB);
		}else{
			mTvCallingB.setText(null);
			mTvCustB.setText(null);
		}
		
		if(!queueDisplayInfo.getSzCurQueueGroupC().isEmpty()){
			String callingC = queueDisplayInfo.getSzCurQueueGroupC();
			mTvCallingC.setText(callingC);
			mTvCustC.setText(callingC);
			qC.setQueueName(callingC);
			if(!mQueueNameLst.contains(qC) && !mIsSpeaking)
				mQueueNameLst.add(qC);
		}else{
			mTvCallingC.setText(null);
			mTvCustC.setText(null);
		}
		
		mLvA.setAdapter(new TableQueueAdapter(mContext, listA));
		mLvB.setAdapter(new TableQueueAdapter(mContext, listB));
		mLvC.setAdapter(new TableQueueAdapter(mContext, listC));
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
				QueueName q = mQueueNameLst.get(mCurrentQueueIdx);
				int callingTime = q.getCallingTime();
				if(callingTime < LIMIT_SPEAK_TIME){
					try {
						mSpeakCallingQueue.speak(q.getQueueName());
						q.setCallingTime(++callingTime);
						mQueueNameLst.set(mCurrentQueueIdx, q);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}	
				}
			}
		}
		
	};

	private void startLoadQueueThread(){
		mLoadCallingQueueThread = new Thread(this);
		mLoadCallingQueueThread.start();
	}
	
	private synchronized void stopLoadQueueThread(){
		if(mLoadCallingQueueThread != null)
		{
			try {
				mLoadCallingQueueThread.interrupt();
				mLoadCallingQueueThread = null;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
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
	public void run() {
		while(!mLoadCallingQueueThread.isInterrupted()){
			if(mCurrentQueueIdx == -1){
				if(mQueueNameLst.size() > 0){
					mCurrentQueueIdx = 0;
					mHandlerSpeakQueue.postDelayed(mSpeakQueueRunnable, SPEAK_DELAYED);
				}
			}
		}
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
		QueueName q = mQueueNameLst.get(mCurrentQueueIdx);
		if(q != null){
			if(q.getCallingTime() < LIMIT_SPEAK_TIME){
				mIsSpeaking = true;
			}else{
				mIsSpeaking = false;
				mQueueNameLst.remove(q);
			}
		}
		if(mCurrentQueueIdx < mQueueNameLst.size() - 1){
			mHandlerSpeakQueue.postDelayed(mSpeakQueueRunnable, SPEAK_DELAYED);
			mCurrentQueueIdx++;
		}else{
			mCurrentQueueIdx = -1;
		}
	}
	
	/**
	 * @author j1tth4
	 * 
	 */
	private class QueueName{
		private String queueName;
		private int callingTime;
		
		public String getQueueName() {
			return queueName;
		}
		public void setQueueName(String queueName) {
			this.queueName = queueName;
		}
		public int getCallingTime() {
			return callingTime;
		}
		public void setCallingTime(int callingTime) {
			this.callingTime = callingTime;
		}
	}
}
