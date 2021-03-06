package com.synature.synaturequeue;

import java.io.File;
import java.io.IOException;

import com.synature.util.MediaManager;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;

public class SpeakCallingQueue implements 
	OnCompletionListener, OnPreparedListener{
	private MediaManager mMediaManager;
	private MediaPlayer mMediaPlayer;
	private OnPlaySoundListener mOnPlaySoundListener;
	
	public SpeakCallingQueue(Context context, String soundDir, 
			OnPlaySoundListener onPlayListener){
		mMediaManager = new MediaManager(context, soundDir);
		mMediaPlayer = new MediaPlayer();
		mOnPlaySoundListener = onPlayListener;
	}
	
	public void speak(String queueText){
		String soundPath = getCallingSoundPath(queueText);
		if(soundPath != null)
			playSound(soundPath);
		else
			mOnPlaySoundListener.onSpeakComplete();
	}
	
	private void playSound(String soundPath){
		try {
			mMediaPlayer.reset();
			mMediaPlayer.setDataSource(soundPath);
			mMediaPlayer.prepare();
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.setOnCompletionListener(this);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			mOnPlaySoundListener.onSpeakComplete();
		} catch (SecurityException e) {
			e.printStackTrace();
			mOnPlaySoundListener.onSpeakComplete();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			mOnPlaySoundListener.onSpeakComplete();
		} catch (IOException e) {
			e.printStackTrace();
			mOnPlaySoundListener.onSpeakComplete();
		}
	}
	
	private File[] listFiles(){
		File sdCard = mMediaManager.getSdCard();
		return sdCard.listFiles(new MediaManager.MP3ExtensionFilter());
	}
	
	private String getCallingSoundPath(String queueText){
		File[] files = listFiles();
		if(files == null)
			return null;
		
		if(files.length > 0){
			for(int i = 0; i < files.length; i++){
				File f = files[i];
				String fileName = f.getName().replaceFirst("[.][^.]+$", "");
				if(fileName.equalsIgnoreCase(queueText)){
					return f.getPath();
				}
			}
		}
		return null;
	}
	
	private void startPlayback(){
		mMediaPlayer.start();
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		startPlayback(); 
		mOnPlaySoundListener.onSpeaking();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		mOnPlaySoundListener.onSpeakComplete();
	}
	
	public static interface OnPlaySoundListener{
		void onSpeaking();
		void onSpeakComplete();
	}
}
