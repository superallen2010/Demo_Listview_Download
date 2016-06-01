package com.demo_downloadservice.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

/*
 * 数据库帮助类
 */
public class DBHelper extends SQLiteOpenHelper {

	
	
	private static final String DB_NAME = "download.db";
	private static final int  VERSION = 1;
	private static DBHelper sHelper = null;
	
	
	
	private static final String SQL_CREATE = "create table thread_info(_id integer primary key autoincrement," +
			"thread_id integer,url text,start integer,end integer,finished integer)";
	
	private static final String SQL_DROP = "drop table if exits thread_info";
	
	private DBHelper(Context context) {
		super(context, DB_NAME, null, VERSION);
	}
	
	
	
	/*
	 * 单例模式
	 */
	
	public static DBHelper getInstance(Context context){
		if(sHelper == null){
			sHelper = new DBHelper(context);
		}
		return sHelper;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(SQL_DROP);
		db.execSQL(SQL_CREATE);
	}

}
