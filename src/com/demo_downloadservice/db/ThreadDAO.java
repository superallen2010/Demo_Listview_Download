package com.demo_downloadservice.db;

import java.util.List;

import android.R.integer;

import com.demo_downloadservice.entities.FileInfo;
import com.demo_downloadservice.entities.ThreadInfo;

/*
 * ���ݷ��ʽӿ�
 */
public interface ThreadDAO {

	/*
	 * �����߳���Ϣ
	 */
	public void insertThread(ThreadInfo threadInfo);
	
	/*
	 * ɾ���߳�
	 */
	public void deleteThread(String url);
	
	/*
	 * �����߳����ؽ���
	 */
	public void updateThread(String url,int thread_id,int finished);
	
	/*
	 * ��ѯ�ļ����߳���Ϣ
	 */
	public List<ThreadInfo> getThreads(String url);
	/*
	 * �߳���Ϣ�Ƿ����
	 */
	public boolean isExists(String url,int thread_id);
	
	
	
}
