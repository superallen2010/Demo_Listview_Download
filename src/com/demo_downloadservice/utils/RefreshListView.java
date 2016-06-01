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

	View mHeaderView = null;// ����
	View mFooterView = null;//�ײ�
	int headerHeight = 0;// �������ָ߶�
	int firstVisibleItem = 0;// ��ǰ��һ���ɼ���
	boolean isRemark;// ���,��ǰ����listeview��˰��µ�
	int startY;// ����ʱ��Yֵ

	int totalItemCount;//������
	int lastVisibleItem;//���һ���ɼ���item
	boolean isLoading;//���ڼ���
	
	int scrollState;// ��ǰ����״̬
	int state;// ��ǰ��״̬
	final int NONE = 0x00;// ����״̬
	final int PULL = 0x01;// ��ʾ����״̬
	final int RELEASE = 0x02;// ��ʾ�ͷ�״̬
	final int REFRESHING = 0x03;// ����ˢ��

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
		topPadding(-headerHeight);//���»���
		this.addHeaderView(mHeaderView);// ��ӵ������ļ�
		this.addFooterView(mFooterView);//��ӵײ���ͼ
		this.setOnScrollListener(this);
	}

	/*
	 * step2 ֪ͨ�����֣�ռ�õĿ�ȣ��߶�
	 */
	private void measureView(View view) {
		ViewGroup.LayoutParams p = view.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
		}

		int width = ViewGroup.getChildMeasureSpec(0, 0, p.width);
		int height;//���Ƹ߶�
		int tempHeight = p.height;
		Log.i("lc", "tempHeight" + tempHeight);
		if (tempHeight > 0) {//�߶ȴ���0�Ļ�������ã���������Ϊ0
			height = MeasureSpec.makeMeasureSpec(tempHeight,
					MeasureSpec.EXACTLY);
		} else {
			height = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
			
		}
		Log.i("lc", "width:" + width + "," + "height:" + height);
		view.measure(width, height);
	}

	/*
	 * step1 ����header�����ϱ߾�
	 */
	private void topPadding(int topPadding) {
		mHeaderView.setPadding(mHeaderView.getPaddingLeft(), topPadding,
				mHeaderView.getPaddingRight(), mHeaderView.getPaddingBottom());

		mHeaderView.invalidate();// �޸�ĳ��view����ʾʱ������invalidate()���ܿ������»��ƵĽ��档invalidate()�ĵ����ǰ�֮ǰ�ľɵ�view����UI�̶߳�����pop��
	}

	/*
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
		this.scrollState = scrollState;
		
		if(totalItemCount == lastVisibleItem && scrollState == SCROLL_STATE_IDLE){//�������׶˲���ֹͣ
			if(!isLoading){//�����ʱ�����һ����footer��totalItemCount�ǰ���header��footer��
				Log.e("lc", "$$$firstVisibleItem" + firstVisibleItem);
				Log.e("lc", "$$$totalItemCount" + totalItemCount);
				mFooterView.findViewById(R.id.loadingview).setVisibility(View.VISIBLE);
				//���ظ���
				mRefershListener.OnLoading();
			}
		}
	}
	/*
	 * step3
	 */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {//totalItemCountͬʱҲ������header��footer
		// TODO Auto-generated method stub
		this.firstVisibleItem = firstVisibleItem;
		
		this.lastVisibleItem = firstVisibleItem + visibleItemCount;
		this.totalItemCount = totalItemCount;
		/*Log.i("lc", "firstVisibleItem" + firstVisibleItem);
		Log.i("lc", "visibleItemCount" + visibleItemCount);
		Log.i("lc", "totalItemCount" + totalItemCount);*/
	}

	/*
	 * ���Ƽ���
	 */
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (firstVisibleItem == 0) {// �������
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
				// ������������
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
	 * STEP4 �ж��ƶ������еĲ���
	 */
	private void OnMove(MotionEvent ev) {
		if (!isRemark) {//���˰����򷵻�
			return;
		}
		int tempY = (int) ev.getY();// ��ʼλ��
		int space = tempY - startY;// �ƶ�����
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
	 * step5 ���ݵ�ǰ״̬�ı������ʾ
	 */
	private void refreshViewByState() {
		TextView tipTextureView = (TextView) mHeaderView.findViewById(R.id.tip);
		ImageView arrowImageView = (ImageView) mHeaderView
				.findViewById(R.id.arrow);
		ProgressBar progressBar = (ProgressBar) mHeaderView
				.findViewById(R.id.progress);
		// ��Ӽ�ͷ����
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
			tipTextureView.setText("��������ˢ��");
			arrowImageView.clearAnimation();
			arrowImageView.setAnimation(animation);
			break;
		case RELEASE:
			arrowImageView.setVisibility(View.VISIBLE);
			progressBar.setVisibility(View.GONE);
			tipTextureView.setText("�ͷſ���ˢ��");
			arrowImageView.clearAnimation();
			arrowImageView.setAnimation(animation1);
			break;
		case REFRESHING:
			topPadding(50);
			arrowImageView.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
			tipTextureView.setText("����ˢ��...");
			arrowImageView.clearAnimation();
			break;
		default:
			break;
		}
	}

	/*
	 * ��ȡ������
	 */
	public void refreshComplete() {
		state = NONE;
		isRemark = false;
		refreshViewByState();
		TextView lastUpdateTimeTextView = (TextView) mHeaderView
				.findViewById(R.id.time);
		SimpleDateFormat format = new SimpleDateFormat(
				"yyyy��MM��dd��     hh:mm:ss");
		Date date = new Date(System.currentTimeMillis());
		String time = format.format(date);
		lastUpdateTimeTextView.setText(time);
	}

	public void setInterface(IRefershListener mRefershListener) {
		this.mRefershListener = mRefershListener;
	}

	/*
	 * ˢ�����ݽӿ� lastStep
	 */
	public interface IRefershListener {
		public void OnRefresh();
		public void OnLoading();
	}

	/*
	 * �������
	 */
	public void loadCompelete(){
		isLoading = false;
		mFooterView.findViewById(R.id.loadingview).setVisibility(View.GONE);
	}
}
