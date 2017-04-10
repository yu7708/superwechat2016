package cn.ucai.superwechat.ui.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.utils.EaseUserUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatDemoHelper;
import cn.ucai.superwechat.db.IUserModel;
import cn.ucai.superwechat.db.InviteMessgeDao;
import cn.ucai.superwechat.db.OnCompleteListener;
import cn.ucai.superwechat.db.UserModel;
import cn.ucai.superwechat.domain.InviteMessage;
import cn.ucai.superwechat.ui.BaseActivity;
import cn.ucai.superwechat.ui.ChatActivity;
import cn.ucai.superwechat.ui.ImageGridActivity;
import cn.ucai.superwechat.ui.VideoCallActivity;
import cn.ucai.superwechat.utils.MFGT;
import cn.ucai.superwechat.utils.Result;
import cn.ucai.superwechat.utils.ResultUtils;

/**
 * Created by Administrator on 2017/4/5.
 */
public class FriendProfileActivity extends BaseActivity {
    @BindView(R.id.add_list_friends)
    TextView addListFriends;
    @BindView(R.id.search)
    Button search;
    @BindView(R.id.title)
    RelativeLayout title;
    @BindView(R.id.profile_image)
    ImageView profileImage;
    @BindView(R.id.tv_userinfo_nick)
    TextView tvUserinfoNick;
    @BindView(R.id.tv_userinfo_name)
    TextView tvUserinfoName;
    @BindView(R.id.view_user)
    RelativeLayout viewUser;
    @BindView(R.id.txt_note_mark)
    TextView txtNoteMark;
    @BindView(R.id.btn_add_contact)
    Button btnAddContact;
    @BindView(R.id.btn_send_msg)
    Button btnSendMsg;
    @BindView(R.id.btn_send_video)
    Button btnSendVideo;
    User user = null;
    IUserModel userModel;
    InviteMessage msg;
    boolean isFriend=false;
    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_friend_profile);
        ButterKnife.bind(this);
        initView();
        initData();
    }

    private void initData() {
        userModel=new UserModel();
        user = (User) getIntent().getSerializableExtra(I.User.TABLE_NAME);
        if (user != null) {
            showUserInfo();
        } else {
            //由于新的好友我们还没加,不想把他放到数据库中,并且得不到user对象,只有username
            msg = (InviteMessage) getIntent().getSerializableExtra(I.User.NICK);
            if(msg!=null){
                user=new User(msg.getFrom());
                user.setMUserNick(msg.getNickName());
                user.setAvatar(msg.getAvatar());//前面是转化数据,才能传递到下面的showUserInfo()
                showUserInfo();
            }else {
                MFGT.finish(FriendProfileActivity.this);
            }
        }
    }

    private void showUserInfo() {
        //拿到了上面来了
        isFriend = SuperWeChatDemoHelper.getInstance().getContactList().containsKey(user.getMUserName());
        if (isFriend) {
            //这一句会使列表添加当前user
            SuperWeChatDemoHelper.getInstance().saveAppContact(user);
        }
        tvUserinfoName.setText(user.getMUserName());
        //Toast.makeText(this, ""+user.getMUserNick(), Toast.LENGTH_SHORT).show();
        //EaseUserUtils.setAppUserNick(user.getMUserName(), tvUserinfoNick);
        tvUserinfoNick.setText(user.getMUserNick());
        EaseUserUtils.setAppUserAvatar(FriendProfileActivity.this, user.getMUserName(),
                profileImage);
        //这个拿到相关数据的是map里的一个方法，反正是获取列表
        //这样写,无法判断是否是好友,得放到外面,再加上一层判读
        showIsFriend(isFriend);
        syncUserInfo();
    }

    private void initView() {
        addListFriends.setText("详细资料");

    }

    private void showIsFriend(boolean isFriend) {
        btnAddContact.setVisibility(isFriend ? View.GONE : View.VISIBLE);
        btnSendMsg.setVisibility(isFriend ? View.VISIBLE : View.GONE);
        btnSendVideo.setVisibility(isFriend ? View.VISIBLE : View.GONE);
    }

    @OnClick(R.id.iv_back)
    public void onClick() {
        MFGT.finish(FriendProfileActivity.this);
    }

    @OnClick(R.id.btn_add_contact)
    public void onConfirm() {
        boolean isConfirm=true;
        if(isConfirm){
            //发送验证消息
            MFGT.gotoSendAddFriend(FriendProfileActivity.this,user.getMUserName());
        }else{
            //直接加好友
        }
    }
    private void syncUserInfo(){
        //从服务器异步加载用户的最新信息,填充到好友列表或者新的朋友的列表
        userModel.loadUserInfo(FriendProfileActivity.this, user.getMUserName(),
                new OnCompleteListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        if(s!=null){
                            Result result = ResultUtils.getResultFromJson(s, User.class);
                            if(result!=null&&result.isRetMsg()){
                                User user= (User) result.getRetData();
                                if(user!=null){
                                    if(msg!=null){
                                        //update msg
                                        ContentValues values=new ContentValues();
                                        values.put(InviteMessgeDao.COLUMN_NAME_NICK,user.getMUserNick());
                                        values.put(InviteMessgeDao.COLUMN_NAME_AVATAR,user.getAvatar());
                                        InviteMessgeDao dao=new InviteMessgeDao(FriendProfileActivity.this);
                                        dao.updateMessage(msg.getId(),values);
                                    }else if(isFriend){
                                        //update user
                                        SuperWeChatDemoHelper.getInstance().saveAppContact(user);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(String error) {

                    }
                });
    }
    @OnClick(R.id.btn_send_msg)
    public void sendMsg(){
        finish();
        //跳转到chat页面,传个名字
        MFGT.gotoChat(FriendProfileActivity.this,user.getMUserName());
    }
    @OnClick(R.id.btn_send_video)
    public void sendVideo(){
        //// FIXME: 2017/4/10 发送视频聊天的请求
        startActivity(new Intent(FriendProfileActivity.this, VideoCallActivity.class)
                .putExtra("username", user.getMUserName())
                .putExtra("isComingCall", false));
    }
}
