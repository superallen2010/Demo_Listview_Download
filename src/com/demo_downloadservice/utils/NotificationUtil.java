package com.demo_downloadservice.utils;

import java.util.HashMap;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.demo_downloadservice.app.MainActivity;
import com.demo_downloadservice.entities.FileInfo;
import com.demo_downloadservice.services.DownloadService;
import com.example.demo_downloadservice.R;
/*
 * 通知工具类
 */
public class NotificationUtil {
	
	private NotificationManager mNotificationManager = null;
	private Map<Integer, Notification> mNotificationsMap = null;
	private Context mContext = null;
	
	public NotificationUtil(Context context){
		mContext = context;
		
		//获得通知系统服务
		mNotificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
		//创建通知的集合
		mNotificationsMap = new HashMap<Integer, Notification>();
	
	}
	
	/*
	 * 显示通知
	 */
	public void showNotification(FileInfo fileInfo){
		//判断通知是否已经显示
		if(!mNotificationsMap.containsKey(fileInfo.getId())){
			//创建通知对象
			Notification notification = new Notification();
			//设置滚动文字
			notification.tickerText = fileInfo.getFileName() + "开始下载";
			//设置显示时间
			notification.when = System.currentTimeMillis();
			//设置图标
			notification.icon = R.drawable.ic_launcher;
			//设置通知特性
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			//设置点击通知栏的操作
			Intent intent = new Intent(mContext, MainActivity.class);
			PendingIntent pIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
			notification.contentIntent = pIntent;
			//创建RemoteView对象
			RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.notification);
			//设置textview
			remoteViews.setTextViewText(R.id.tvNfTitle, fileInfo.getFileName());
			//设置开始按钮操作
			Intent intentStart = new Intent(mContext, DownloadService.class);
			intentStart.setAction(DownloadService.ACTION_START);
			intentStart.putExtra("fileInfo", fileInfo);
			PendingIntent piStart = PendingIntent.getService(mContext, 0, intentStart, 0);
			remoteViews.setOnClickPendingIntent(R.id.btNfStart,piStart);
			//设置停止按钮操作
			Intent intentStop = new Intent(mContext, DownloadService.class);
			intentStop.setAction(DownloadService.ACTION_STOP);
			intentStop.putExtra("fileInfo", fileInfo);
			PendingIntent piStop = PendingIntent.getService(mContext, 0, intentStop, 0);
			remoteViews.setOnClickPendingIntent(R.id.btNfStop,piStop);
			//设置Notification的视图
			notification.contentView = remoteViews;
			//发出通知
			mNotificationManager.notify(fileInfo.getId(), notification);
			//通知加到集合中
			mNotificationsMap.put(fileInfo.getId(), notification);
		}
	}
	
	
	
	
	//取消通知
	public void cancelNotification(int id){
		mNotificationManager.cancel(id);
		mNotificationsMap.remove(id);
	}
	
	//更新进度条
	public void updateNotification(int id,int progress){
		Notification notification = mNotificationsMap.get(id);
		if(notification != null){
			//更新进度条
			notification.contentView.setProgressBar(R.id.pbNfProgress, 100, progress, false);
			mNotificationManager.notify(id, notification);
		}
		
		
	}
	
	
}
