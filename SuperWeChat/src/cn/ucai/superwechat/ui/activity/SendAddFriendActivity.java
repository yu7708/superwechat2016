package cn.ucai.superwechat.ui.activity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.widget.EaseAlertDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatDemoHelper;
import cn.ucai.superwechat.ui.BaseActivity;
import cn.ucai.superwechat.utils.MFGT;

/**
 * Created by Administrator on 2017/4/5.
 */
public class SendAddFriendActivity extends BaseActivity {
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.et_msg)
    EditText etMsg;
    String toAddUserName=null;
    ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_send_add_friend);
        ButterKnife.bind(this);
        initView();
        initData();
    }

    private void initData() {
        toAddUserName= getIntent().getStringExtra(I.User.USER_NAME);
        if(toAddUserName==null){
            finish();
        }
    }

    private void initView() {
        etMsg.setText(getString(R.string.addcontact_send_msg_prefix)+
                SuperWeChatDemoHelper.getInstance().getCurrentUsernName());
    }

    @OnClick({R.id.iv_back, R.id.btn_send})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_back:
                MFGT.finish(SendAddFriendActivity.this);
                break;
            case R.id.btn_send:
                if(toAddUserName!=null){
                    addContact();
                }
                break;
        }
    }
    /**
     * add contact
     *
     * @param
     */
    //// FIXME: 2017/4/5 复制的addContactActivity的里的添加的方法,是由环信提供的
    public void addContact() {
            //如果添加的人等于自己,显示不能添加你自己
        if (EMClient.getInstance().getCurrentUser().equals(toAddUserName)) {
            new EaseAlertDialog(this, R.string.not_add_myself).show();
            return;
        }
            //获得数据库里的联系人列表
        if (SuperWeChatDemoHelper.getInstance().getAppContactList().containsKey(toAddUserName)) {
            //let the user know the contact already in your contact list
            if (EMClient.getInstance().contactManager().getBlackListUsernames().contains(toAddUserName)) {
                new EaseAlertDialog(this, R.string.user_already_in_contactlist).show();
                return;
            }
            new EaseAlertDialog(this, R.string.This_user_is_already_your_friend).show();
            return;
        }

        progressDialog = new ProgressDialog(this);
        String stri = getResources().getString(R.string.Is_sending_a_request);
        progressDialog.setMessage(stri);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        new Thread(new Runnable() {
            public void run() {

                try {
                    //demo use a hardcode reason here, you need let user to input if you like
                    //String s = getResources().getString(R.string.Add_a_friend);//这个是添加好友发送的信息
                    //// FIXME: 2017/4/5 改为
                    String s=etMsg.getText().toString();
                    EMClient.getInstance().contactManager().addContact(toAddUserName, s);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            progressDialog.dismiss();
                            String s1 = getResources().getString(R.string.send_successful);
                            Toast.makeText(getApplicationContext(), s1, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            progressDialog.dismiss();
                            String s2 = getResources().getString(R.string.Request_add_buddy_failure);
                            Toast.makeText(getApplicationContext(), s2 + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
}
