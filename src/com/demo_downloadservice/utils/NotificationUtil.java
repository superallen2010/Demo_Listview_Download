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
 * ֪ͨ������
 */
public class NotificationUtil {
	
	private NotificationManager mNotificationManager = null;
	private Map<Integer, Notification> mNotificationsMap = null;
	private Context mContext = null;
	
	public NotificationUtil(Context context){
		mContext = context;
		
		//���֪ͨϵͳ����
		mNotificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
		//����֪ͨ�ļ���
		mNotificationsMap = new HashMap<Integer, Notification>();
	
	}
	
	/*
	 * ��ʾ֪ͨ
	 */
	public void showNotification(FileInfo fileInfo){
		//�ж�֪ͨ�Ƿ��Ѿ���ʾ
		if(!mNotificationsMap.containsKey(fileInfo.getId())){
			//����֪ͨ����
			Notification notification = new Notification();
			//���ù�������
			notification.tickerText = fileInfo.getFileName() + "��ʼ����";
			//������ʾʱ��
			notification.when = System.currentTimeMillis();
			//����ͼ��
			notification.icon = R.drawable.ic_launcher;
			//����֪ͨ����
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			//���õ��֪ͨ���Ĳ���
			Intent intent = new Intent(mContext, MainActivity.class);
			PendingIntent pIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
			notification.contentIntent = pIntent;
			//����RemoteView����
			RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.notification);
			//����textview
			remoteViews.setTextViewText(R.id.tvNfTitle, fileInfo.getFileName());
			//���ÿ�ʼ��ť����
			Intent intentStart = new Intent(mContext, DownloadService.class);
			intentStart.setAction(DownloadService.ACTION_START);
			intentStart.putExtra("fileInfo", fileInfo);
			PendingIntent piStart = PendingIntent.getService(mContext, 0, intentStart, 0);
			remoteViews.setOnClickPendingIntent(R.id.btNfStart,piStart);
			//����ֹͣ��ť����
			Intent intentStop = new Intent(mContext, DownloadService.class);
			intentStop.setAction(DownloadService.ACTION_STOP);
			intentStop.putExtra("fileInfo", fileInfo);
			PendingIntent piStop = PendingIntent.getService(mContext, 0, intentStop, 0);
			remoteViews.setOnClickPendingIntent(R.id.btNfStop,piStop);
			//����Notification����ͼ
			notification.contentView = remoteViews;
			//����֪ͨ
			mNotificationManager.notify(fileInfo.getId(), notification);
			//֪ͨ�ӵ�������
			mNotificationsMap.put(fileInfo.getId(), notification);
		}
	}
	
	
	
	
	//ȡ��֪ͨ
	public void cancelNotification(int id){
		mNotificationManager.cancel(id);
		mNotificationsMap.remove(id);
	}
	
	//���½�����
	public void updateNotification(int id,int progress){
		Notification notification = mNotificationsMap.get(id);
		if(notification != null){
			//���½�����
			notification.contentView.setProgressBar(R.id.pbNfProgress, 100, progress, false);
			mNotificationManager.notify(id, notification);
		}
		
		
	}
	
	
}
