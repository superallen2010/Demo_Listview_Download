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
 * 下载文件类
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
			.newCachedThreadPool();// 线程池
	private Timer mTimer = new Timer();// 定时器
	private Messenger mMessenger = null; 
	
	
	
	
	public DownloadTask(Context mContext, FileInfo mFileInfo, int mThreadCount,Messenger mMessenger) {
		this.mContext = mContext;
		this.mFileInfo = mFileInfo;
		this.mMessenger = mMessenger;
		this.mThreadCount = mThreadCount;
		mDao = new ThreadDAOImpl(mContext);
	}

	// 下载
	public void download() {
		// 读取数据库线程信息
		List<ThreadInfo> threadInfos = mDao.getThreads(mFileInfo.getUrl());

		/*
		 * if(threadInfos.size() == 0){ Log.i("lc", "size0:" + 0); threadInfo =
		 * new ThreadInfo(0, mFileInfo.getUrl(), 0, mFileInfo.getLength(), 0);
		 * }else{ Log.i("lc", "size:" + threadInfos.size()); threadInfo =
		 * threadInfos.get(0); } //创建子线程开始下载 new
		 * DownloadThread(threadInfo).start();
		 */

		/*
		 * 多线程下载
		 */
		if (threadInfos.size() == 0) {
			// 获取每个线程下载的长度
			int length = mFileInfo.getLength() / mThreadCount;
			Log.i("lc", "threadInfos ==null:" + length);
			for (int i = 0; i < mThreadCount; i++) {
				// 创建线程信息
				ThreadInfo threadInfo = new ThreadInfo(i, mFileInfo.getUrl(),
						length * i, (i + 1) * length - 1, 0);
				if (i == mThreadCount - 1) {
					threadInfo.setEnd(mFileInfo.getLength());
				}
				threadInfos.add(threadInfo);
				// 向数据库出入线程信息
				mDao.insertThread(threadInfo);
			}
		}

		Log.i("lc", "threadInfos !=null:" + threadInfos.size()+"");
		mThreadList = new ArrayList<DownloadThread>();
		// 启动多个线程进行下载
		for (ThreadInfo info : threadInfos) {
			DownloadThread thread = new DownloadThread(info);
			// thread.start();
			sExecutorService.execute(thread);
			// 添加线程到集合中
			mThreadList.add(thread);
		}
		// 启动定时任务
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				// 发送广播修改Activity进度
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
	 * 判断是否所有线程执行完毕
	 */

	private synchronized void checkAllThreadsFinished() {
		boolean allFinished = true;
		// 遍历线程集合，判断县城是否执行完毕
		for (DownloadThread thread : mThreadList) {
			if (!thread.isFinished) {
				allFinished = false;
				break;
			}
		}

		if (allFinished) {
			//取消定时器
			mTimer.cancel();
			// 下载完成，删除线程
			mDao.deleteThread(mFileInfo.getUrl());
			// 发送广播通知UI下载结束
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
	 * 下载线程
	 */

	class DownloadThread extends Thread {
		private ThreadInfo mThreadInfo = null;
		public boolean isFinished = false;// 标识线程是否执行完毕

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
				// 设置下载位置
				int start = mThreadInfo.getStart() + mThreadInfo.getFinished();
				conn.setRequestProperty("Range", "bytes=" + start + "-"
						+ mThreadInfo.getEnd());
				// 设置文件写入位置
				File file = new File(DownloadService.DOWNLOAD_PATH,
						mFileInfo.getFileName());
				raf = new RandomAccessFile(file, "rwd");
				raf.seek(start);

				finished = mThreadInfo.getFinished();

				// 开始下载
				if (conn.getResponseCode() == HttpStatus.SC_PARTIAL_CONTENT) {
					// 读取数据

					is = conn.getInputStream();
					byte[] buffer = new byte[1024 * 4];
					int len = -1;
					//long time = System.currentTimeMillis();
					while ((len = is.read(buffer)) != -1) {
						// 写入文件
						raf.write(buffer, 0, len);
						// 把下载进度广播给Avtivity
						finished += len;
						//Log.i("lc", "finished:" + (int) (100 * (1.0 * finished / mFileInfo.getLength())));
						// 累加进度
						mThreadInfo
								.setFinished(mThreadInfo.getFinished() + len);
						// 下载暂停时，保存下载进度
						if (isPause) {
							mDao.updateThread(mThreadInfo.getUrl(),
									mThreadInfo.getId(),
									mThreadInfo.getFinished());
							return;
						}
					}
					// 标识线程执行完毕
					isFinished = true;
					// 检查下载任务是否完毕
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
			// 开始下载
		}
	}
}
