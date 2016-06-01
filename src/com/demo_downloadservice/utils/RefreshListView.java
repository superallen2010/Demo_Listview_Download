package com.demo_downloadservice.utils;

import java.sql.Date;
import java.text.SimpleDateFormat;

import android.R.integer;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;

import com.example.demo_downloadservice.R;

public class RefreshListView extends ListView implements OnScrollListener {

	View mHeaderView = null;// 顶部
	View mFooterView = null;//底部
	int headerHeight = 0;// 顶部布局高度
	int firstVisibleItem = 0;// 当前第一个可见项
	boolean isRemark;// 标记,当前是在listeview最顶端按下的
	int startY;// 按下时的Y值

	int totalItemCount;//总数量
	int lastVisibleItem;//最后一个可见的item
	boolean isLoading;//正在加载
	
	int scrollState;// 当前滚动状态
	int state;// 当前的状态
	final int NONE = 0x00;// 正常状态
	final int PULL = 0x01;// 提示下拉状态
	final int RELEASE = 0x02;// 提示释放状态
	final int REFRESHING = 0x03;// 正在刷新

	private IRefershListener mRefershListener;

	public RefreshListView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		initView(context);
	}

	public RefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		initView(context);
	}

	public RefreshListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		initView(context);
	}

	private void initView(Context context) {
		LayoutInflater inflater = LayoutInflater.from(context);
		mHeaderView = inflater.inflate(R.layout.header_layout, null);
		
		mFooterView = inflater.inflate(R.layout.footer_layout, null);
		mFooterView.findViewById(R.id.loadingview).setVisibility(View.GONE);
		
		measureView(mHeaderView);
		//mHeaderView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		headerHeight = mHeaderView.getMeasuredHeight();
		Log.i("lc", headerHeight + "");
		topPadding(-headerHeight);//重新绘制
		this.addHeaderView(mHeaderView);// 添加到顶部文件
		this.addFooterView(mFooterView);//添加底部视图
		this.setOnScrollListener(this);
	}

	/*
	 * step2 通知父布局，占用的宽度，高度
	 */
	private void measureView(View view) {
		ViewGroup.LayoutParams p = view.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
		}

		int width = ViewGroup.getChildMeasureSpec(0, 0, p.width);
		int height;//绘制高度
		int tempHeight = p.height;
		Log.i("lc", "tempHeight" + tempHeight);
		if (tempHeight > 0) {//高度大于0的话，则采用，否则设置为0
			height = MeasureSpec.makeMeasureSpec(tempHeight,
					MeasureSpec.EXACTLY);
		} else {
			height = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
			
		}
		Log.i("lc", "width:" + width + "," + "height:" + height);
		view.measure(width, height);
	}

	/*
	 * step1 设置header布局上边距
	 */
	private void topPadding(int topPadding) {
		mHeaderView.setPadding(mHeaderView.getPaddingLeft(), topPadding,
				mHeaderView.getPaddingRight(), mHeaderView.getPaddingBottom());

		mHeaderView.invalidate();// 修改某个view的显示时，调用invalidate()才能看到重新绘制的界面。invalidate()的调用是把之前的旧的view从主UI线程队列中pop掉
	}

	/*
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
		this.scrollState = scrollState;
		
		if(totalItemCount == lastVisibleItem && scrollState == SCROLL_STATE_IDLE){//滚动到底端并且停止
			if(!isLoading){//当相等时，最后一项是footer，totalItemCount是包括header与footer的
				Log.e("lc", "$$$firstVisibleItem" + firstVisibleItem);
				Log.e("lc", "$$$totalItemCount" + totalItemCount);
				mFooterView.findViewById(R.id.loadingview).setVisibility(View.VISIBLE);
				//加载更多
				mRefershListener.OnLoading();
			}
		}
	}
	/*
	 * step3
	 */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {//totalItemCount同时也包括了header和footer
		// TODO Auto-generated method stub
		this.firstVisibleItem = firstVisibleItem;
		
		this.lastVisibleItem = firstVisibleItem + visibleItemCount;
		this.totalItemCount = totalItemCount;
		/*Log.i("lc", "firstVisibleItem" + firstVisibleItem);
		Log.i("lc", "visibleItemCount" + visibleItemCount);
		Log.i("lc", "totalItemCount" + totalItemCount);*/
	}

	/*
	 * 手势监听
	 */
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (firstVisibleItem == 0) {// 界面最顶端
				isRemark = true;
				startY = (int) ev.getY();
			}
			break;
		case MotionEvent.ACTION_MOVE:
			OnMove(ev);
			break;
		case MotionEvent.ACTION_UP:
			if (RELEASE == state) {
				state = REFRESHING;
				// 加载最新数据
				refreshViewByState();
				mRefershListener.OnRefresh();
			} else if (PULL == state) {
				state = NONE;
				isRemark = false;
				refreshViewByState();
			}
			break;
		default:
			break;
		}
		return super.onTouchEvent(ev);
	}

	/*
	 * STEP4 判断移动过程中的操作
	 */
	private void OnMove(MotionEvent ev) {
		if (!isRemark) {//顶端按下则返回
			return;
		}
		int tempY = (int) ev.getY();// 开始位置
		int space = tempY - startY;// 移动距离
		int topPadding = space - headerHeight;

		Log.i("lc", "onMove topPadding:"  + topPadding);
		switch (state) {
		case NONE:
			if (space > 0) {
				state = PULL;
				refreshViewByState();
			}
			break;
		case PULL:
			topPadding(topPadding);
			if (space > headerHeight + 30
					&& scrollState == SCROLL_STATE_TOUCH_SCROLL) {
				state = RELEASE;
				refreshViewByState();
			}
			break;
		case RELEASE:
			topPadding(topPadding);
			if (space < headerHeight + 30) {
				state = PULL;
				refreshViewByState();
			} else if (space <= 0) {
				state = NONE;
				isRemark = false;
				refreshViewByState();
			}
			break;
		case REFRESHING:

			break;
		default:
			break;
		}
	}

	/*
	 * step5 根据当前状态改变界面显示
	 */
	private void refreshViewByState() {
		TextView tipTextureView = (TextView) mHeaderView.findViewById(R.id.tip);
		ImageView arrowImageView = (ImageView) mHeaderView
				.findViewById(R.id.arrow);
		ProgressBar progressBar = (ProgressBar) mHeaderView
				.findViewById(R.id.progress);
		// 添加箭头动画
		RotateAnimation animation = new RotateAnimation(0, 180,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		animation.setDuration(500);
		animation.setFillAfter(true);

		RotateAnimation animation1 = new RotateAnimation(180, 0,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		animation1.setDuration(500);
		animation1.setFillAfter(true);
		switch (state) {
		case NONE:
			topPadding(-headerHeight);
			break;
		case PULL:
			arrowImageView.setVisibility(View.VISIBLE);
			progressBar.setVisibility(View.GONE);
			tipTextureView.setText("下拉可以刷新");
			arrowImageView.clearAnimation();
			arrowImageView.setAnimation(animation);
			break;
		case RELEASE:
			arrowImageView.setVisibility(View.VISIBLE);
			progressBar.setVisibility(View.GONE);
			tipTextureView.setText("释放可以刷新");
			arrowImageView.clearAnimation();
			arrowImageView.setAnimation(animation1);
			break;
		case REFRESHING:
			topPadding(50);
			arrowImageView.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
			tipTextureView.setText("正在刷新...");
			arrowImageView.clearAnimation();
			break;
		default:
			break;
		}
	}

	/*
	 * 获取完数据
	 */
	public void refreshComplete() {
		state = NONE;
		isRemark = false;
		refreshViewByState();
		TextView lastUpdateTimeTextView = (TextView) mHeaderView
				.findViewById(R.id.time);
		SimpleDateFormat format = new SimpleDateFormat(
				"yyyy年MM月dd日     hh:mm:ss");
		Date date = new Date(System.currentTimeMillis());
		String time = format.format(date);
		lastUpdateTimeTextView.setText(time);
	}

	public void setInterface(IRefershListener mRefershListener) {
		this.mRefershListener = mRefershListener;
	}

	/*
	 * 刷新数据接口 lastStep
	 */
	public interface IRefershListener {
		public void OnRefresh();
		public void OnLoading();
	}

	/*
	 * 加载完毕
	 */
	public void loadCompelete(){
		isLoading = false;
		mFooterView.findViewById(R.id.loadingview).setVisibility(View.GONE);
	}
}
