package com.demo_downloadservice.app;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.demo_downloadservice.entities.FileInfo;
import com.demo_downloadservice.services.DownloadService;
import com.example.demo_downloadservice.R;

/*
 * 文件列表适配器
 */
public class FileListAdapter extends BaseAdapter {

	private Context mContext;
	private List<FileInfo> mFileList = null;
	private Messenger mMessenger = null;

	public FileListAdapter(Context mContext, List<FileInfo> mFileList) {
		this.mContext = mContext;
		this.mFileList = mFileList;
	}

	public void setMessenger(Messenger messenger){
		this.mMessenger = messenger;
	}
	
	@Override
	public int getCount() {
		return mFileList.size();
	}

	@Override
	public Object getItem(int position) {
		return mFileList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		// 设置视图中的控件
		final FileInfo fileInfo = mFileList.get(position);
		if (convertView == null) {
			// 加载视图
			convertView = LayoutInflater.from(mContext).inflate(
					R.layout.listitem, null);
			// 获得布局中的控件
			viewHolder = new ViewHolder();
			viewHolder.tvFile = (TextView) convertView
					.findViewById(R.id.tvTitle);
			viewHolder.btStart = (Button) convertView
					.findViewById(R.id.btStart);
			viewHolder.btStop = (Button) convertView.findViewById(R.id.btStop);
			viewHolder.tvFile = (TextView) convertView
					.findViewById(R.id.tvTitle);
			viewHolder.pbFile = (ProgressBar) convertView
					.findViewById(R.id.pbProgress);
			
			viewHolder.tvFile.setText(fileInfo.getFileName());
			viewHolder.pbFile.setMax(100);
			
			viewHolder.btStart.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// 通过intent传递参数给service
//					Intent intent = new Intent(mContext, DownloadService.class);
//					intent.setAction(DownloadService.ACTION_START);
//					intent.putExtra("fileInfo", fileInfo);
//					mContext.startService(intent);
					Message msg = new Message();
					msg.what = DownloadService.MSG_START;
					msg.obj = fileInfo;
					try {
						mMessenger.send(msg);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			viewHolder.btStop.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// 通过intent传递参数给service
//					Intent intent = new Intent(mContext, DownloadService.class);
//					intent.setAction(DownloadService.ACTION_STOP);
//					intent.putExtra("fileInfo", fileInfo);
//					mContext.startService(intent);
					Message msg = new Message();
					msg.what = DownloadService.MSG_STOP;
					msg.obj = fileInfo;
					try {
						mMessenger.send(msg);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		viewHolder.pbFile.setProgress(fileInfo.getFinished());
	
		return convertView;
	}

	// 更新进度条
	public void updatePrpgress(int id, int progress) {
		FileInfo fileInfo = mFileList.get(id);
		fileInfo.setFinished(progress);
		Log.i("lc", "fileInfo:"+ fileInfo);
		Log.i("lc", "update:"+ progress);
		notifyDataSetChanged();
	}

	static class ViewHolder {
		TextView tvFile;
		Button btStop, btStart;
		ProgressBar pbFile;
	}

}
