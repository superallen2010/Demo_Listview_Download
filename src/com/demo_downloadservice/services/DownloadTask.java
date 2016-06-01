package com.demo_downloadservice.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpStatus;

import android.content.Context;
import android.content.Intent;
import android.nfc.cardemulation.OffHostApduService;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.demo_downloadservice.db.ThreadDAOImpl;
import com.demo_downloadservice.entities.FileInfo;
import com.demo_downloadservice.entities.ThreadInfo;

/**
 * �����ļ���
 * 
 * @author Allen
 * 
 */
public class DownloadTask {

	private Context mContext = null;
	private FileInfo mFileInfo = null;
	private ThreadDAOImpl mDao = null;
	public boolean isPause = false;
	private int mThreadCount = 1;
	private ThreadInfo threadInfo = null;
	private int finished = 0;
	private List<DownloadThread> mThreadList;

	public static ExecutorService sExecutorService = Executors
			.newCachedThreadPool();// �̳߳�
	private Timer mTimer = new Timer();// ��ʱ��
	private Messenger mMessenger = null; 
	
	
	
	
	public DownloadTask(Context mContext, FileInfo mFileInfo, int mThreadCount,Messenger mMessenger) {
		this.mContext = mContext;
		this.mFileInfo = mFileInfo;
		this.mMessenger = mMessenger;
		this.mThreadCount = mThreadCount;
		mDao = new ThreadDAOImpl(mContext);
	}

	// ����
	public void download() {
		// ��ȡ���ݿ��߳���Ϣ
		List<ThreadInfo> threadInfos = mDao.getThreads(mFileInfo.getUrl());

		/*
		 * if(threadInfos.size() == 0){ Log.i("lc", "size0:" + 0); threadInfo =
		 * new ThreadInfo(0, mFileInfo.getUrl(), 0, mFileInfo.getLength(), 0);
		 * }else{ Log.i("lc", "size:" + threadInfos.size()); threadInfo =
		 * threadInfos.get(0); } //�������߳̿�ʼ���� new
		 * DownloadThread(threadInfo).start();
		 */

		/*
		 * ���߳�����
		 */
		if (threadInfos.size() == 0) {
			// ��ȡÿ���߳����صĳ���
			int length = mFileInfo.getLength() / mThreadCount;
			Log.i("lc", "threadInfos ==null:" + length);
			for (int i = 0; i < mThreadCount; i++) {
				// �����߳���Ϣ
				ThreadInfo threadInfo = new ThreadInfo(i, mFileInfo.getUrl(),
						length * i, (i + 1) * length - 1, 0);
				if (i == mThreadCount - 1) {
					threadInfo.setEnd(mFileInfo.getLength());
				}
				threadInfos.add(threadInfo);
				// �����ݿ�����߳���Ϣ
				mDao.insertThread(threadInfo);
			}
		}

		Log.i("lc", "threadInfos !=null:" + threadInfos.size()+"");
		mThreadList = new ArrayList<DownloadThread>();
		// ��������߳̽�������
		for (ThreadInfo info : threadInfos) {
			DownloadThread thread = new DownloadThread(info);
			// thread.start();
			sExecutorService.execute(thread);
			// ����̵߳�������
			mThreadList.add(thread);
		}
		// ������ʱ����
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				// ���͹㲥�޸�Activity����
//				Intent intent = new Intent(DownloadService.ACTION_UPDATE);
//				int k = (int) (100 * (1.0 * finished / mFileInfo.getLength()));
//				intent.putExtra("finished", k);
//				Log.i("lc",
//						"finish:" + finished + "length:"
//								+ mFileInfo.getLength() + "/" + k);
//				intent.putExtra("id", mFileInfo.getId());
//				mContext.sendBroadcast(intent);
				Message msg = new Message();
				msg.what = DownloadService.MSG_UPDATE;
				msg.arg1 = (int) (100 * (1.0 * finished / mFileInfo.getLength()));
				msg.arg2 = mFileInfo.getId();
				try {
					mMessenger.send(msg);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, 1000, 1000);

	}

	/*
	 * �ж��Ƿ������߳�ִ�����
	 */

	private synchronized void checkAllThreadsFinished() {
		boolean allFinished = true;
		// �����̼߳��ϣ��ж��س��Ƿ�ִ�����
		for (DownloadThread thread : mThreadList) {
			if (!thread.isFinished) {
				allFinished = false;
				break;
			}
		}

		if (allFinished) {
			//ȡ����ʱ��
			mTimer.cancel();
			// ������ɣ�ɾ���߳�
			mDao.deleteThread(mFileInfo.getUrl());
			// ���͹㲥֪ͨUI���ؽ���
//			Intent intent = new Intent(DownloadService.ACTION_FINISH);
//			intent.putExtra("fileInfo", mFileInfo);
//			mContext.sendBroadcast(intent);
			Message msg = new Message();
			msg.what = DownloadService.MSG_FINISH;
			msg.obj = mFileInfo;
			try {
				mMessenger.send(msg);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/*
	 * �����߳�
	 */

	class DownloadThread extends Thread {
		private ThreadInfo mThreadInfo = null;
		public boolean isFinished = false;// ��ʶ�߳��Ƿ�ִ�����

		public DownloadThread(ThreadInfo mThreadInfo) {
			this.mThreadInfo = mThreadInfo;
		}

		@Override
		public void run() {
			HttpURLConnection conn = null;
			InputStream is = null;
			RandomAccessFile raf = null;
			try {
				URL url = new URL(mFileInfo.getUrl());
				conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(3000);
				conn.setRequestMethod("GET");
				// ��������λ��
				int start = mThreadInfo.getStart() + mThreadInfo.getFinished();
				conn.setRequestProperty("Range", "bytes=" + start + "-"
						+ mThreadInfo.getEnd());
				// �����ļ�д��λ��
				File file = new File(DownloadService.DOWNLOAD_PATH,
						mFileInfo.getFileName());
				raf = new RandomAccessFile(file, "rwd");
				raf.seek(start);

				finished = mThreadInfo.getFinished();

				// ��ʼ����
				if (conn.getResponseCode() == HttpStatus.SC_PARTIAL_CONTENT) {
					// ��ȡ����

					is = conn.getInputStream();
					byte[] buffer = new byte[1024 * 4];
					int len = -1;
					//long time = System.currentTimeMillis();
					while ((len = is.read(buffer)) != -1) {
						// д���ļ�
						raf.write(buffer, 0, len);
						// �����ؽ��ȹ㲥��Avtivity
						finished += len;
						//Log.i("lc", "finished:" + (int) (100 * (1.0 * finished / mFileInfo.getLength())));
						// �ۼӽ���
						mThreadInfo
								.setFinished(mThreadInfo.getFinished() + len);
						// ������ͣʱ���������ؽ���
						if (isPause) {
							mDao.updateThread(mThreadInfo.getUrl(),
									mThreadInfo.getId(),
									mThreadInfo.getFinished());
							return;
						}
					}
					// ��ʶ�߳�ִ�����
					isFinished = true;
					// ������������Ƿ����
					checkAllThreadsFinished();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					is.close();
					raf.close();
					conn.disconnect();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// ��ʼ����
		}
	}
}
