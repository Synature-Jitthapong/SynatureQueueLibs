package com.synature.synaturequeue;

import java.util.List;

import com.synature.pos.QueueDisplayInfo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TableQueueAdapter extends BaseAdapter{
	private List<QueueDisplayInfo.QueueInfo> mQueueLst;
	private LayoutInflater mInflater;
	
	public TableQueueAdapter(Context c, List<QueueDisplayInfo.QueueInfo> queueInfoLst){
		mQueueLst = queueInfoLst;
		mInflater = (LayoutInflater)
				c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public int getCount() {
		return mQueueLst != null ? mQueueLst.size() : 0;
	}

	@Override
	public QueueDisplayInfo.QueueInfo getItem(int position) {
		return mQueueLst.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if(convertView == null){
			convertView = mInflater.inflate(R.layout.queue_template, null);
			holder = new ViewHolder();
			holder.tvQueueName = (TextView) convertView.findViewById(R.id.tvQueueName);
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder) convertView.getTag();
		}
		QueueDisplayInfo.QueueInfo queueInfo = mQueueLst.get(position);
		holder.tvQueueName.setText(queueInfo.getSzQueueName());
		return convertView;
	}

	public static class ViewHolder{
		TextView tvQueueName;
	}
}
