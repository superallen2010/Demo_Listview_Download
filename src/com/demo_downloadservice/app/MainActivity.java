package com.demo_downloadservice.app;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.ListView;
import android.widget.Toast;

import com.demo_downloadservice.entities.FileInfo;
import com.demo_downloadservice.services.DownloadService;
import com.demo_downloadservice.utils.NotificationUtil;
import com.demo_downloadservice.utils.RefreshListView;
import com.demo_downloadservice.utils.RefreshListView.IRefershListener;
import com.example.demo_downloadservice.R;

public class MainActivity extends Activity implements IRefershListener{

	private RefreshListView mFileListView = null;
	private FileListAdapter mFileListAdapter = null;
	private List<FileInfo> mFileInfos = null;
	private NotificationUtil mNotificationUtil =null;
	private Messenger mServiceMessenger = null;//service中的messenger

	// 定义广播接收器
//	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
//
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			if (DownloadService.ACTION_UPDATE.equals(intent.getAction())) {
//				//更新进度条
//				int finished = intent.getIntExtra("finished", 0);
//				int id = intent.getIntExtra("id", 0);
//				mFileListAdapter.updatePrpgress(id, finished);
//				//更新通知里的进度
//				mNotificationUtil.updateNotification(id, finished);
//				// mPbProgressBar.setProgress(finished);
//			} else if (DownloadService.ACTION_FINISH.equals(intent.getAction())) {
//				//下载结束
//				//int id = intent.getIntExtra("id", 0);
//				FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
//				mFileListAdapter.updatePrpgress(fileInfo.getId(), 0);
//				Toast.makeText(MainActivity.this, fileInfo.getFileName() + "下载完成", Toast.LENGTH_SHORT).show();
//				mNotificationUtil.cancelNotification(fileInfo.getId());
//				Log.i("lc", "finished:" + fileInfo.getFinished());
//				// mPbProgressBar.setProgress(finished);
//			}else if(DownloadService.ACTION_START.equals(intent.getAction())){
//				//显示一个通知
//				mNotificationUtil.showNotification((FileInfo)intent.getSerializableExtra("fileInfo"));
//			}
//
//		}
//	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// 初始化控件
		setUpWidget();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		//unregisterReceiver(mReceiver);
	}

	// 初始化组件
	private void setUpWidget() {
		mFileInfos = new ArrayList<FileInfo>();
		mNotificationUtil = new NotificationUtil(this);
		FileInfo fileInfo1 = new FileInfo(
				0,
				"http://gdown.baidu.com/data/wisegame/18d56bb24a23a691/kugouyinle_8066.apk",
				"酷狗音乐", 0, 0);
		FileInfo fileInfo2 = new FileInfo(
				1,
				"http://gdown.baidu.com/data/wisegame/18d56bb24a23a691/kugouyinle_8066.apk",
				"网易音乐", 0, 0);

		mFileInfos.add(fileInfo1);
		mFileInfos.add(fileInfo2);

		mFileListView = (RefreshListView) findViewById(R.id.lvFile);
		mFileListAdapter = new FileListAdapter(this, mFileInfos);
		mFileListView.setAdapter(mFileListAdapter);
		mFileListView.setInterface(this);

		// 注册广播接收器
//		IntentFilter filter = new IntentFilter();
//		filter.addAction(DownloadService.ACTION_START);
//		filter.addAction(DownloadService.ACTION_UPDATE);
//		filter.addAction(DownloadService.ACTION_FINISH);
//		registerReceiver(mReceiver, filter);
		
		//绑定service
		Intent intent = new Intent(this, DownloadService.class);
		bindService(intent, mConnection, Service.BIND_AUTO_CREATE);
	}
	
	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DownloadService.MSG_UPDATE:
				//更新进度条
				int finished = msg.arg1;
				int id = msg.arg2;
				mFileListAdapter.updatePrpgress(id, finished);
				//Log.i("lc", "id:" + id + "-finished:" + finished);
				//更新通知里的进度
				mNotificationUtil.updateNotification(id, finished);
				break;
			case DownloadService.MSG_FINISH:
				FileInfo fileInfo = (FileInfo) msg.obj;
				mFileListAdapter.updatePrpgress(fileInfo.getId(), 0);
				Toast.makeText(MainActivity.this, fileInfo.getFileName() + "下载完成", Toast.LENGTH_SHORT).show();
				mNotificationUtil.cancelNotification(fileInfo.getId());
				break;
			case DownloadService.MSG_START:
				mNotificationUtil.showNotification((FileInfo)msg.obj);
			default:
				break;
			}
		}
		
	};
	ServiceConnection mConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			//获得service中的Messenger
			mServiceMessenger = new Messenger(service);
			//设置适配器中的Messenger，activity向service通信
			mFileListAdapter.setMessenger(mServiceMessenger);
			
			//创建activity中的Messenger
			Messenger messenger = new Messenger(mHandler);
			//创建消息
			Message message = new Message();
			message.what = DownloadService.MSG_BIND;
			message.replyTo = messenger;
			//使用Service的Messenger发送Activity中的Messager
			try {
				mServiceMessenger.send(message);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	@Override
	public void OnRefresh() {
		// TODO Auto-generated method stub
		//获得最新的数据
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				FileInfo fileInfo3 = new FileInfo(
						2,
						"http://gdown.baidu.com/data/wisegame/18d56bb24a23a691/kugouyinle_8066.apk",
						"顶部刷新", 0, 0);

				mFileInfos.add(fileInfo3);
				//通知界面显示数据
				mFileListAdapter.notifyDataSetChanged();
				//通知listview刷新完毕
				mFileListView.refreshComplete();
			}
		}, 2000);
	
	}

	@Override
	public void OnLoading() {
		// TODO Auto-generated method stub
		//加载更多
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				FileInfo fileInfo3 = new FileInfo(
						2,
						"http://gdown.baidu.com/data/wisegame/18d56bb24a23a691/kugouyinle_8066.apk",
						"底部刷新", 0, 0);

				mFileInfos.add(fileInfo3);
				//通知界面显示数据
				mFileListAdapter.notifyDataSetChanged();
				//通知listview刷新完毕
				mFileListView.loadCompelete();
			}
		}, 2000);
	}
}
