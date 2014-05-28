package com.syn.synaturequeue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class QueueServerSocket implements Runnable{
	public static final int PORT = 6060;
	
	private ServerSocketListener mListener;
	private ServerSocket mSocket; 
	private boolean mIsClosed = false;

	public QueueServerSocket(ServerSocketListener listener) throws IOException{
		mSocket = new ServerSocket(PORT);
		mListener = listener;
	}
	
	@Override
	public void run() {
		Socket socket = null;
		while(!mIsClosed){
			try {
				socket = mSocket.accept();
				BufferedReader bf = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String msg = bf.readLine();
				if(msg != null)
					mListener.onReceipt(msg);
			} catch (IOException e) {
				mListener.onAcceptErr(e.getMessage());
			}
		}
	}
	
	public void closeSocket() throws IOException{
		mSocket.close();
		mIsClosed = true;
	}
	
	public boolean isClosed(){
		return mIsClosed;
	}
	
	public static interface ServerSocketListener{
		void onReceipt(String msg);
		void onAcceptErr(String msg);
	}
}
