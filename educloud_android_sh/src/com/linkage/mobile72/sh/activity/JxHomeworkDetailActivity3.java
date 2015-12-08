package com.linkage.mobile72.sh.activity;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.gauss.speex.recorder.SpeexPlayer;
import com.j256.ormlite.stmt.QueryBuilder;
import com.linkage.mobile72.sh.R;
import com.linkage.mobile72.sh.activity.CreateHomeworkActivity.AudioAnimationHandler;
import com.linkage.mobile72.sh.activity.im.NewChatActivity;
import com.linkage.mobile72.sh.app.BaseActivity;
import com.linkage.mobile72.sh.app.BaseApplication;
import com.linkage.mobile72.sh.data.Contact;
import com.linkage.mobile72.sh.data.http.JXBean;
import com.linkage.mobile72.sh.data.http.JXBeanDetail;
import com.linkage.mobile72.sh.data.http.JXBeanDetail.JXMessageAttachment;
import com.linkage.mobile72.sh.data.http.JXBeanDetail.JXVote;
import com.linkage.mobile72.sh.datasource.DataHelper;
import com.linkage.mobile72.sh.http.WDJsonObjectRequest;
import com.linkage.mobile72.sh.utils.FileUtils;
import com.linkage.mobile72.sh.utils.HttpDownloader;
import com.linkage.mobile72.sh.utils.ProgressDialogUtils;
import com.linkage.mobile72.sh.utils.StatusUtils;
import com.linkage.mobile72.sh.utils.StringUtils;
import com.linkage.mobile72.sh.utils.UIUtilities;
import com.linkage.mobile72.sh.Consts;
import com.linkage.mobile72.sh.Consts.ChatType;
import com.linkage.lib.util.LogUtils;
/*
 * 家长的查看作业、通知、点评详情页面
 * @author Yao
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class JxHomeworkDetailActivity3 extends BaseActivity implements OnClickListener {
	
	private static final String TAG = JxHomeworkDetailActivity3.class.getName();
	
	private JXBean bean; 
	private JXBeanDetail jxbean;//请求接口后返回的详情对象
	private Button back;
	private TextView senderText, sendTimeText;
	private EditText sendContentText;
	private GridView gridView;
	private RelativeLayout imageLayout, voiceLayout;
	private JXBeanDetail.JXMessageAttachment voiceAttach;
	private ArrayList<JXBeanDetail.JXMessageAttachment> photos;
    private ArrayList<String> choosePics;
	private PicGridAdapter imgsAdapter;
	private TextView reply, vote, call;
	private ImageView voiceAnimImageView;
	private TextView voiceTimeText;
    private SpeexPlayer splayer = null;
    private String voicePathName = "";
    private Timer mTimer = null;
    private TimerTask mTimerTask = null;  
    
    // 记录语音动画图片
 	private int index = 1;
 	private AudioAnimationHandler audioAnimationHandler = null;
    private Context mContext;  
    private DataHelper helper ;
 	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if(intent == null) {
			finish();
			return;
		}
		bean = (JXBean)intent.getSerializableExtra("jxbean");
		setContentView(R.layout.activity_homework_detail2);
		if(String.valueOf(Consts.JxhdType.COMMENT).equals(bean.getSmsMessageType())) {
			setTitle("点评详情");
		}else if(String.valueOf(Consts.JxhdType.HOMEWORK).equals(bean.getSmsMessageType())) {
			setTitle("作业详情");
		}else if(String.valueOf(Consts.JxhdType.NOTICE).equals(bean.getSmsMessageType())) {
			setTitle("通知详情");
		}else {
			setTitle("消息详情");
		}
		back = (Button)findViewById(R.id.back);
		senderText = (TextView) findViewById(R.id.receiver);
		sendTimeText = (TextView) findViewById(R.id.subject);
		sendContentText = (EditText)findViewById(R.id.edit_input);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			sendContentText.setTextIsSelectable(true);
			sendContentText.setKeyListener(null);
		}else{
			sendContentText.setKeyListener(null);
		}
		voiceTimeText = (TextView)findViewById(R.id.voice_time);
		imageLayout = (RelativeLayout)findViewById(R.id.layout_pic);
		voiceLayout = (RelativeLayout)findViewById(R.id.layout_voice);
		voiceAnimImageView = (ImageView) findViewById(R.id.voice_anim_img);
		gridView = (GridView) findViewById(R.id.pic_gridview);
		reply = (TextView)findViewById(R.id.reply_btn);
		vote = (TextView)findViewById(R.id.vote_btn);
		call = (TextView)findViewById(R.id.call_btn);
		photos = new ArrayList<JXBeanDetail.JXMessageAttachment>();
		/*for(int i=0;i<7;i++) {
			photos.add("/static/ucenter/user/60000/0331.jpg");
        }*/
		reply.setEnabled(false);
		call.setEnabled(false);
		imgsAdapter = new PicGridAdapter(this, photos);
        gridView.setAdapter(imgsAdapter);
        voiceLayout.setOnClickListener(this);
        back.setOnClickListener(this);
        reply.setOnClickListener(this);
        vote.setOnClickListener(this);
        call.setOnClickListener(this);
        
        mContext = this;  
        helper = DataHelper.getHelper(mContext);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(JxHomeworkDetailActivity3.this,
                        PictureReviewNetActivity.class);
                intent.putStringArrayListExtra(PictureReviewNetActivity.RES, choosePics);
                intent.putExtra("position", position);
                startActivity(intent);
            }
        });
    	getDetail();
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if(mApp.getflag() == 1) {
			getDetail();
		}
	}
	
	class PicGridAdapter extends BaseAdapter {

	    private Context context;
	    private List<JXBeanDetail.JXMessageAttachment> data;

		public PicGridAdapter(Context context, List<JXBeanDetail.JXMessageAttachment> data) {
			this.context = context;
			this.data = data;
		}

	    public void addData(List<JXBeanDetail.JXMessageAttachment> list, boolean append) {
	        if(list != null && list.size() > 0) {
	            if(!append)
	                data.clear();
	            data.addAll(list);
	        }
	        notifyDataSetChanged();
	    }

		@Override
		public int getCount() {
	        if(data == null || data.size() == 0) {
	            return 0;
	        }else if(data.size() < 8) {
	            return data.size();//1-7张的加个加号
	        }else {
	            return 8;
	        }
		}

		@Override
		public JXBeanDetail.JXMessageAttachment getItem(int arg0) {
			return data.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
	        Holder holder;
	        if (convertView == null || convertView.getTag() == null) {
	            convertView = LayoutInflater.from(context).inflate(R.layout.adapter_attachment_pic_grid, parent, false);
	            holder = new Holder();
	            holder.imageView = (ImageView) convertView.findViewById(R.id.imageView1);
	            convertView.setTag(holder);
	        } else {
	            holder = (Holder) convertView.getTag();
	        }
	        JXBeanDetail.JXMessageAttachment ma = getItem(position);
	        if(ma != null) {
                imageLoader.displayImage(ma.getAttachmentUrl(), holder.imageView, defaultOptionsPhoto);
	        }
			return convertView;
		}
		
		class Holder{
			ImageView imageView;
		}

	}
	
	private void getDetail() {
		mApp.setflag(0);
		ProgressDialogUtils.showProgressDialog("", this, true);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("commandtype", "getMessageDetail");
		params.put("studentid", getDefaultAccountChild().getId()+"");
		params.put("id", ""+bean.getId());
		params.put("type", "1");
		params.put("smsMessageType", bean.getSmsMessageType());
		WDJsonObjectRequest mRequest = new WDJsonObjectRequest(Consts.SERVER_getMessageDetail,
				Request.Method.POST, params, true, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						ProgressDialogUtils.dismissProgressBar();
						if (response.optInt("ret") == 0) {
							jxbean = JXBeanDetail.parseFromJson(response.optJSONObject("data"), String.valueOf(Consts.JxhdType.NOTICE));
							fillDataToPage(jxbean);
							
						}else {
							if(StringUtils.isEmpty(response.optString("msg"))) {
								UIUtilities.showToast(JxHomeworkDetailActivity3.this, "获取详情失败");
							}else {
								UIUtilities.showToast(JxHomeworkDetailActivity3.this, response.optString("msg"));
							}
							finish();
						}
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ProgressDialogUtils.dismissProgressBar();
						StatusUtils.handleError(arg0, JxHomeworkDetailActivity3.this);
						finish();
					}
				});
		BaseApplication.getInstance().addToRequestQueue(mRequest, TAG);
	}
	
	private void read(long id) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("commandtype", "messageRead");
		params.put("studentid", getDefaultAccountChild().getId()+"");
		params.put("id", ""+id);
		WDJsonObjectRequest mRequest = new WDJsonObjectRequest(Consts.SERVER_messageRead,
				Request.Method.POST, params, true, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
					}
				});
		BaseApplication.getInstance().addToRequestQueue(mRequest, TAG);
	}
	
	private void reply(long id) {
		if (jxbean == null) {
			return;
		}
		ProgressDialogUtils.showProgressDialog("", this, true);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("commandtype", "messageReplied");
		params.put("studentid", getDefaultAccountChild().getId() + "");
		params.put("id", "" + id);
		WDJsonObjectRequest mRequest = new WDJsonObjectRequest(
				Consts.SERVER_messageReplied, Request.Method.POST, params,
				true, new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						ProgressDialogUtils.dismissProgressBar();
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ProgressDialogUtils.dismissProgressBar();
					}
				});
		BaseApplication.getInstance().addToRequestQueue(mRequest, TAG);

		if (jxbean.getSendUserId() == 0) {
			UIUtilities.showToast(this, "回复用户不存在");
			return;
		}
		if (jxbean.getSendUserId() == getCurAccount().getUserId()) {
			UIUtilities.showToast(this, "不能对自己进行回复");
			return;
		}
		String loginName = BaseApplication.getInstance().getDefaultAccount()
				.getLoginname();
		try {
			QueryBuilder<Contact, Integer> contactBuilder = helper
					.getContactData().queryBuilder();
			contactBuilder.where().eq("loginName", loginName).and()
					.eq("id", jxbean.getSendUserId());
			List<Contact> mContacts = contactBuilder.query();
			if (mContacts != null && mContacts.size() > 0) {
				Contact contact = (Contact) mContacts.get(0);
//				Intent intent = NewChatActivity.getIntent(mContext,
//						jxbean.getSendUserId(), contact.getName(),
//						ChatType.CHAT_TYPE_SINGLE, 1);
//				mContext.startActivity(intent);
				Intent intent = new Intent();
				intent.setClass(mContext, ChatActivity.class);
				Bundle bundle = new Bundle();

				bundle.putString("chatid", Consts.APP_ID + jxbean.getSendUserId());
				bundle.putInt("chattype",  ChatType.CHAT_TYPE_SINGLE);
				bundle.putString("name", contact.getName());
				LogUtils.v("name" + contact.getName());
				bundle.putInt("type", 0);

				intent.putExtra("data", bundle);
				LogUtils.d("starting chat----> buddyId=" + jxbean.getSendUserId()
						+ " chattype=" + ChatType.CHAT_TYPE_SINGLE);
				startActivity(intent);
			} else {
//				Intent intent = NewChatActivity.getIntent(mContext,
//						jxbean.getSendUserId(), jxbean.getSendUserName(),
//						ChatType.CHAT_TYPE_SINGLE, 1);
//				mContext.startActivity(intent);
				Intent intent = new Intent();
				intent.setClass(mContext, ChatActivity.class);
				Bundle bundle = new Bundle();

				bundle.putString("chatid", Consts.APP_ID + jxbean.getSendUserId());
				bundle.putInt("chattype",  ChatType.CHAT_TYPE_SINGLE);
				bundle.putString("name", jxbean.getSendUserName());
				LogUtils.v("name" + jxbean.getSendUserName());
				bundle.putInt("type", 0);

				intent.putExtra("data", bundle);
				LogUtils.d("starting chat----> buddyId=" + jxbean.getSendUserId()
						+ " chattype=" + ChatType.CHAT_TYPE_SINGLE);
				startActivity(intent);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	
	private void fillDataToPage(JXBeanDetail jxbean) {
		if(jxbean != null) {
			senderText.setText(jxbean.getSendUserName());
			sendTimeText.setText(jxbean.getSendTime());
			sendContentText.setText(jxbean.getMessageContent());
			 if(jxbean.getSendUserId() != 0) {
				 reply.setEnabled(true);
			 }
			if(jxbean.getSendPhone()!=null && !"".equalsIgnoreCase(jxbean.getSendPhone()))
			{
			call.setEnabled(true);
			}
			List<JXBeanDetail.JXMessageAttachment> attachs = jxbean.getMsgAttachment();
			List<JXBeanDetail.JXMessageAttachment> imageAttachs = new ArrayList<JXBeanDetail.JXMessageAttachment>();
			if(attachs != null && attachs.size() > 0) {
                choosePics = new ArrayList<String>();
				for(JXMessageAttachment ma : attachs) {
					if(ma.getAttachmentType() == 0) {
                        choosePics.add(ma.getAttachmentUrl());
						imageAttachs.add(ma);
					}else {
						voiceAttach = ma;
					}
				}
			}
			ArrayList<JXVote> votes = jxbean.getVoteJson();
			if(jxbean.getVoteStatus() == 1 || (votes == null || votes.size() <= 0)) {
				vote.setVisibility(View.GONE);
			}else {
				vote.setVisibility(View.VISIBLE);
			}
			if(null != voiceAttach) {
				final HttpDownloader download = new HttpDownloader();
				voicePathName = mApp.getWorkspaceVoice().getAbsolutePath() + FileUtils.getFileExtend(voiceAttach.getAttachmentUrl());
				new Thread(new Runnable() {
					@Override
					public void run() {
						download.downFile(voiceAttach.getAttachmentUrl(), mApp.getWorkspaceVoice().getAbsolutePath(), FileUtils.getFileExtend(voiceAttach.getAttachmentUrl()));
					}
				}).start();
			}
			if(imageAttachs != null && imageAttachs.size() > 0) {
				imageLayout.setVisibility(View.VISIBLE);
				imgsAdapter.addData(imageAttachs, false);
			}else {
				imageLayout.setVisibility(View.INVISIBLE);
			}
			if(voiceAttach != null) {
				voiceLayout.setVisibility(View.VISIBLE);
				int voiceTime = 0;
				if(!TextUtils.isEmpty(voiceAttach.getVoiceTime())){
					try {
						voiceTime = (int)Double.valueOf(voiceAttach.getVoiceTime()).intValue();
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
				voiceTimeText.setText(voiceTime+"\"");
			}else {
				voiceLayout.setVisibility(View.INVISIBLE);
			}
			//Read Flag
			read(bean.getId());
		}
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.back:
			finish();
			break;
		case R.id.reply_btn:
			reply(bean.getId());
			break;
		case R.id.vote_btn:
			Intent i = new Intent(this, VoteSubmitActivity.class);
			i.putExtra(VoteSubmitActivity.MSG_ID, bean.getId());
			i.putExtra(VoteSubmitActivity.EXTRAS, jxbean.getVoteJson());
			startActivity(i);
			break;
		case R.id.call_btn:
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_CALL);
			if(TextUtils.isEmpty(jxbean.getSendPhone())) {
				UIUtilities.showToast(this, "发送者号码未获取到，无法拨打电话");
			}else {
				intent.setData(Uri.parse("tel:"+jxbean.getSendPhone()));
				startActivity(intent);
			}
			break;
		case R.id.layout_voice:
			splayer = new SpeexPlayer(voicePathName);
			splayer.endPlay();
			splayer.startPlay();
			playAudioAnimation(voiceAnimImageView, 1);
			break;
		}
	}
	
	/**
	 * 停止
	 */
	private void stopTimer() {
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}

		if (mTimerTask != null) {
			mTimerTask.cancel();
			mTimerTask = null;
		}

	}
	
    private void playAudioAnimation(final ImageView imageView, final int inOrOut) {
		// 定时器检查播放状态
		stopTimer();
		mTimer = new Timer();
		// 将要关闭的语音图片归位
		if (audioAnimationHandler != null) {
			Message msg = new Message();
			msg.what = 3;
			audioAnimationHandler.sendMessage(msg);
		}

		audioAnimationHandler = new AudioAnimationHandler(imageView, inOrOut);
		mTimerTask = new TimerTask() {
			public boolean hasPlayed = false;

			@Override
			public void run() {
				if (splayer.isAlive()) {
					hasPlayed = true;
					index = (index + 1) % 3;
					Message msg = new Message();
					msg.what = index;
					audioAnimationHandler.sendMessage(msg);
				} else {
					// 当播放完时
					Message msg = new Message();
					msg.what = 3;
					audioAnimationHandler.sendMessage(msg);
					// 播放完毕时需要关闭Timer等
					if (hasPlayed) {
						stopTimer();
					}
				}
			}
		};
		// 调用频率为500毫秒一次
		mTimer.schedule(mTimerTask, 0, 500);
	}
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	BaseApplication.getInstance().cancelPendingRequests(TAG);
    	if(splayer != null && splayer.isAlive()) {
			splayer.endPlay();
		}
    }
}
