package com.demo_downloadservice.services;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.HttpStatus;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.demo_downloadservice.entities.FileInfo;

public class DownloadService extends Service {
	public static final String DOWNLOAD_PATH = Environment
			.getExternalStorageDirectory().getAbsolutePath() + "/test";
	public static final String ACTION_START = "ACTION_START";
	public static final String ACTION_STOP = "ACTION_STOP";
	public static final String ACTION_UPDATE = "ACTION_UPDATE";
	public static final String ACTION_FINISH = "ACTION_FINISH";
	public static final int INIT_MSG = 0x01;

	public static final int MSG_BIND = 0x02;
	public static final int MSG_START = 0x03;
	public static final int MSG_STOP = 0x04;
	public static final int MSG_FINISH = 0x05;
	public static final int MSG_UPDATE = 0x06;

	private Messenger mActivityMessenger = null;// ����activity����ʹ

	// ��������ļ���
	private Map<Integer, DownloadTask> mTasks = new LinkedHashMap<Integer, DownloadTask>();

	private Handler mHandler = new Handler() {

		public void handleMessage(Message msg) {
			FileInfo fileInfo = null;
			DownloadTask task = null;
			switch (msg.what) {
			case INIT_MSG:
				fileInfo = (FileInfo) msg.obj;
				Log.i("lc", "Init:" + fileInfo);
				task = new DownloadTask(DownloadService.this, fileInfo, 3,
						mActivityMessenger);
				task.download();
				// ������������ӵ�������ȥ
				mTasks.put(fileInfo.getId(), task);
				// ����һ����������Ĺ㲥
				// Intent intent = new Intent(DownloadService.ACTION_START);
				// intent.putExtra("fileInfo", fileInfo);
				// sendBroadcast(intent);
				Message msg1 = new Message();
				msg1.what = MSG_START;
				msg1.obj = fileInfo;
				try {
					mActivityMessenger.send(msg1);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case MSG_BIND:// ����󶨵�Messenger
				mActivityMessenger = msg.replyTo;
				break;
			case MSG_START:
				fileInfo = (FileInfo) msg.obj;
				Log.i("lc", "start:" + fileInfo.toString());
				// ������ʼ���߳�
				// new InitThread(fileInfo).start();
				DownloadTask.sExecutorService.execute(new InitThread(fileInfo));
				break;
			case MSG_STOP:
				fileInfo = (FileInfo) msg.obj;
				Log.i("lc", "stop:" + fileInfo.toString());
				task = mTasks.get(fileInfo.getId());
				if (task != null) {
					task.isPause = true;
				}
				break;
			default:
				break;
			}
		}

	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// if (ACTION_START.equals(intent.getAction())) {
		// FileInfo fileInfo = (FileInfo) intent
		// .getSerializableExtra("fileInfo");
		// Log.i("lc", "start:" + fileInfo.toString());
		// // ������ʼ���߳�
		// //new InitThread(fileInfo).start();
		// DownloadTask.sExecutorService.execute(new InitThread(fileInfo));
		// } else if (ACTION_STOP.equals(intent.getAction())) {
		// FileInfo fileInfo = (FileInfo) intent
		// .getSerializableExtra("fileInfo");
		// Log.i("lc", "stop:" + fileInfo.toString());
		// DownloadTask task = mTasks.get(fileInfo.getId());
		// if (task != null) {
		// task.isPause = true;
		// }
		//
		// }

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// ����һ��Messager
		Messenger messenger = new Messenger(mHandler);
		// ����Messager��Binder
		return messenger.getBinder();
	}

	/*
	 * ��ʼ�����߳�
	 */
	class InitThread extends Thread {
		private FileInfo mFileInfo = null;

		public InitThread(FileInfo mFileInfo) {
			super();
			this.mFileInfo = mFileInfo;
		}

		@Override
		public void run() {
			HttpURLConnection conn = null;
			RandomAccessFile raf = null;
			try {
				// ��������
				URL url = new URL(mFileInfo.getUrl());
				conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(3000);
				conn.setRequestMethod("GET");
				int length = -1;
				if (conn.getResponseCode() == HttpStatus.SC_OK) {
					// ��ȡ�ļ�����
					length = conn.getContentLength();
				}
				if (length < 0) {
					return;
				}

				File dirFile = new File(DOWNLOAD_PATH);
				if (!dirFile.exists()) {
					dirFile.mkdir();
				}
				File file = new File(dirFile, mFileInfo.getFileName());
				// ���ļ������ļ�д�����
				raf = new RandomAccessFile(file, "rwd");
				raf.setLength(length);
				mFileInfo.setLength(length);
				mHandler.obtainMessage(INIT_MSG, mFileInfo).sendToTarget();
				// Message msg = Message.obtain();
				// msg.obj = mFileInfo;

				// ��ȡ�ļ�����
				// �ڱ��ش����ļ�
				// �����ļ�����
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					raf.close();
					conn.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
	}

}
