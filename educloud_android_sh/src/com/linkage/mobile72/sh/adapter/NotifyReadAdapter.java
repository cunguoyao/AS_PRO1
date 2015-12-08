package com.linkage.mobile72.sh.adapter;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.linkage.mobile72.sh.R;
import com.linkage.mobile72.sh.data.NotifyRead;
import com.linkage.lib.util.LogUtils;

public class NotifyReadAdapter extends BaseAdapter {
	
	private int type;
	private List<NotifyRead> replies;
	private LayoutInflater inflater;
	private Context context;
	private View mLastView;
	private int mLastPosition;
	private int mLastVisibility;

	public NotifyReadAdapter(List<NotifyRead> replies, Context context, int type) {
		this.type = type;
		this.replies = replies;
		this.context = context;
		inflater = LayoutInflater.from(context);
		mLastPosition = -1;
	}

	public void addData(List<NotifyRead> data, boolean append) {
        if(data != null && data.size() > 0) {
            if(!append)
            	replies.clear();
            replies.addAll(data);
        }
        notifyDataSetChanged();
    }
	
	@Override
	public int getCount() {
		return replies.size();
	}

	@Override
	public NotifyRead getItem(int position) {
		return replies.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		long mPosition = getItemId(position);
		NotifyRead np = getItem(position);
		LogUtils.e(mPosition + "    " + position + ":" + np.getState());
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.notify_read_list_item, parent, false);
			holder = new ViewHolder();
			holder.nameText = (TextView) convertView.findViewById(R.id.nameText);
			holder.stateText = (TextView) convertView.findViewById(R.id.stateText);
			holder.messageText = (LinearLayout) convertView.findViewById(R.id.btn_message);
			holder.phoneText = (LinearLayout) convertView.findViewById(R.id.btn_call);
			holder.expandLayout = (LinearLayout) convertView.findViewById(R.id.expend_buttons);
			holder.layout1 = (LinearLayout) convertView.findViewById(R.id.layout1);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		if (np.getState() == true) {
			if(type == 1) {
				holder.stateText.setText("已读");
			}else {
				holder.stateText.setText("已回");
			}
			holder.stateText.setTextColor(Color.GREEN);
		} else {
			if(type == 1) {
				holder.stateText.setText("未读");
			}else {
				holder.stateText.setText("未回");
			}
			holder.stateText.setTextColor(Color.RED);
		}
		holder.nameText.setText(replies.get(position).getName());
		holder.layout1.setOnClickListener(new MyClickListner(convertView, position, np));
		return convertView;
	}

	class ViewHolder {
		private TextView nameText, stateText;
		private LinearLayout expandLayout, layout1, messageText, phoneText;
	}

	public void changeImageVisible(View view, int position) {
		if (mLastView != null && mLastPosition != position) {
			ViewHolder holder = (ViewHolder) mLastView.getTag();
			switch (holder.expandLayout.getVisibility()) {
			case View.VISIBLE:
				holder.expandLayout.setVisibility(View.GONE);
				mLastVisibility = View.GONE;
				break;
			default:
				break;
			}
		}
		mLastPosition = position;
		mLastView = view;
		ViewHolder holder = (ViewHolder) view.getTag();
		switch (holder.expandLayout.getVisibility()) {
		case View.GONE:
			holder.expandLayout.setVisibility(View.VISIBLE);
			mLastVisibility = View.VISIBLE;
			break;
		case View.VISIBLE:
			holder.expandLayout.setVisibility(View.GONE);
			mLastVisibility = View.GONE;
			break;
		}
	}
	
	class MyClickListner implements OnClickListener {
		private View mView;
		private int position;
		private NotifyRead np;
		private MyClickListner(View mView, int position, NotifyRead np){
			this.mView = mView;
			this.position = position;
			this.np = np;
		}

		public void onClick(View arg0) {
			changeImageVisible(mView, position);
			ViewHolder holder = (ViewHolder) mView.getTag();
			if (mLastPosition == position) {
				holder.expandLayout.setVisibility(mLastVisibility);
				holder.messageText.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View arg0) {
						Intent intent = new Intent();
						//系统默认的action，用来打开默认的短信界面
						intent.setAction(Intent.ACTION_SENDTO);
						//需要发短息的号码
						intent.setData(Uri.parse("smsto:"+np.getPhone()));
						context.startActivity(intent);
					}
				});
				holder.phoneText.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View arg0) {
						Uri uri = Uri.parse("tel:"+np.getPhone());
						Intent intent = new Intent(Intent.ACTION_DIAL, uri);
						context.startActivity(intent);
					}
				});
			} else {
				holder.expandLayout.setVisibility(View.GONE);
			}
		}
		
	}
}
