/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ucai.superwechat.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.easemob.redpacketsdk.constant.RPConstant;
import com.easemob.redpacketui.utils.RedPacketUtil;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMContactListener;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMConversation.EMConversationType;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.util.EMLog;
import com.hyphenate.util.NetUtils;
import com.umeng.analytics.MobclickAgent;
import com.umeng.update.UmengUpdateAgent;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.ucai.superwechat.Constant;
import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatDemoHelper;
import cn.ucai.superwechat.adapter.MainTabAdapter;
import cn.ucai.superwechat.db.InviteMessgeDao;
import cn.ucai.superwechat.db.UserDao;
import cn.ucai.superwechat.runtimepermissions.PermissionsManager;
import cn.ucai.superwechat.runtimepermissions.PermissionsResultAction;
import cn.ucai.superwechat.ui.fragment.ProfileFragment;
import cn.ucai.superwechat.widget.DMTabHost;
import cn.ucai.superwechat.widget.MFViewPager;
import cn.ucai.superwechat.widget.TitleMenu.ActionItem;
import cn.ucai.superwechat.widget.TitleMenu.TitlePopup;

@SuppressLint("NewApi")
public class MainActivity extends BaseActivity implements ViewPager.OnPageChangeListener, DMTabHost.OnCheckedChangeListener {

    protected static final String TAG = "MainActivity";
    @BindView(R.id.txt_left)
    TextView txtLeft;
    @BindView(R.id.layout_viewpage)
    MFViewPager layoutViewpage;
    @BindView(R.id.layout_tabhost)
    DMTabHost layoutTabhost;
    @BindView(R.id.txt_right)
    TextView txtRight;
    // textview for unread message count
    //未读地址数量
     private TextView unreadLabel;
    // textview for unread event message
    //未读地址标签
    private TextView unreadAddressLable;

    private Button[] mTabs;
    private ContactListFragment contactListFragment;
    private Fragment[] fragments;
    private int index;
    private int currentTabIndex;
    // user logged into another device
    public boolean isConflict = false;
    // user account was removed
    private boolean isCurrentAccountRemoved = false;

    MainTabAdapter mAdapter;
    TitlePopup mPopup;
    /**
     * check if current user account was remove
     */
    public boolean getCurrentAccountRemoved() {
        return isCurrentAccountRemoved;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savePower();//省电
        checkAccount(savedInstanceState);//检查账号

        setContentView(R.layout.em_activity_main);
        ButterKnife.bind(this);
        // runtime permission for android 6.0, just require all permissions here for simple
        requestPermissions();

        initView();
        umengInit();//友盟


        showExceptionDialogFromIntent(getIntent());

        inviteMessgeDao = new InviteMessgeDao(this);
        UserDao userDao = new UserDao(this);
        initFragment();//初始化


        //register broadcast receiver to receive the change of group from SuperWeChatDemoHelper
        registerBroadcastReceiver();


        EMClient.getInstance().contactManager().setContactListener(new MyContactListener());
        //debug purpose only
        registerInternalDebugReceiver();
    }

    private void initFragment() {

        conversationListFragment = new ConversationListFragment();
        contactListFragment = new ContactListFragment();
        //SettingsFragment settingFragment = new SettingsFragment();
        //// FIXME: 2017/3/31
        //替换上面的settingFragment，把个人中心界面改改
        ProfileFragment profileFragment = new ProfileFragment();
        //--f
        fragments = new Fragment[]{conversationListFragment, contactListFragment};

		/*getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, conversationListFragment)
                .add(R.id.fragment_container, contactListFragment).hide(contactListFragment).show(conversationListFragment)
				.commit();*/
        mAdapter = new MainTabAdapter(getSupportFragmentManager());
        mAdapter.addFragment(conversationListFragment, getString(R.string.app_name));
        mAdapter.addFragment(contactListFragment, getString(R.string.contacts));
        mAdapter.addFragment(new DicoverFragment(), getString(R.string.discover));
        // mAdapter.addFragment(settingFragment,getString(R.string.me));
        //// FIXME: 2017/3/31　　//替换布局
        mAdapter.addFragment(profileFragment, getString(R.string.me));
        //--f
        layoutViewpage.setAdapter(mAdapter);
        layoutViewpage.setOnPageChangeListener(this);
        layoutTabhost.setOnCheckedChangeListener(this);
        layoutTabhost.setChecked(0);
    }

    private void umengInit() {
        //umeng api
        MobclickAgent.updateOnlineConfig(this);
        UmengUpdateAgent.setUpdateOnlyWifi(false);
        UmengUpdateAgent.update(this);
    }

    private void checkAccount(Bundle savedInstanceState) {
        //make sure activity will not in background if user is logged into another device or removed
        if (savedInstanceState != null && savedInstanceState.getBoolean(Constant.ACCOUNT_REMOVED, false)) {
            SuperWeChatDemoHelper.getInstance().logout(false, null);
            finish();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        } else if (savedInstanceState != null && savedInstanceState.getBoolean("isConflict", false)) {
            finish();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
    }

    private void savePower() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
    }

    @TargetApi(23)
    private void requestPermissions() {
        PermissionsManager.getInstance().requestAllManifestPermissionsIfNecessary(this, new PermissionsResultAction() {
            @Override
            public void onGranted() {
//				Toast.makeText(MainActivity.this, "All permissions have been granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDenied(String permission) {
                //Toast.makeText(MainActivity.this, "Permission " + permission + " has been denied", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * init views
     */
    private void initView() {
        /*unreadLabel = (TextView) findViewById(R.id.unread_msg_number);
		unreadAddressLable = (TextView) findViewById(R.id.unread_address_number);
		mTabs = new Button[3];
		mTabs[0] = (Button) findViewById(R.id.btn_conversation);
		mTabs[1] = (Button) findViewById(R.id.btn_address_list);
		mTabs[2] = (Button) findViewById(R.id.btn_setting);
		// select first tab
		mTabs[0].setSelected(true);*/
        //// FIXME: 2017/3/31 显示在每一个界面的主题
        txtLeft.setVisibility(View.VISIBLE);
        txtRight.setVisibility(View.VISIBLE);
        mPopup=new TitlePopup(MainActivity.this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopup.addAction(new ActionItem(MainActivity.this,R.string.menu_groupchat,R.drawable.icon_menu_group));
        mPopup.addAction(new ActionItem(MainActivity.this,R.string.menu_addfriend,R.drawable.icon_menu_addfriend));
        mPopup.addAction(new ActionItem(MainActivity.this,R.string.menu_qrcode,R.drawable.icon_menu_sao));
        mPopup.addAction(new ActionItem(MainActivity.this,R.string.menu_money,R.drawable.icon_menu_money));
        txtRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(MainActivity.this, "aaa", Toast.LENGTH_SHORT).show();
              if(NetUtils.hasDataConnection(MainActivity.this)){
                mPopup.show(txtRight);}
            }
        });
        mPopup.setItemOnClickListener(new TitlePopup.OnItemOnClickListener() {
            @Override
            public void onItemClick(ActionItem item, int position) {
                switch (position){
                    case 1:
                        //显示好友
                        startActivity(new Intent(MainActivity.this, AddContactActivity.class));
                        break;
                }
            }
        });
    }/*txtRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //// FIXME: 2017/4/5 设置弹窗
                mPopup=new TitlePopup(MainActivity.this, ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                mPopup.addAction(new ActionItem(MainActivity.this,R.string.menu_groupchat,R.drawable.icon_menu_group));
                mPopup.addAction(new ActionItem(MainActivity.this,R.string.menu_addfriend,R.drawable.icon_menu_addfriend));
                mPopup.addAction(new ActionItem(MainActivity.this,R.string.menu_qrcode,R.drawable.icon_menu_sao));
                mPopup.addAction(new ActionItem(MainActivity.this,R.string.menu_money,R.drawable.icon_menu_money));
            }
        });
        mPopup.show(txtRight);*/

    /**
     * on tab clicked
     *
     * @param view
     */
	/*public void onTabClicked(View view) {
		switch (view.getId()) {
		case R.id.btn_conversation:
			index = 0;
			break;
		case R.id.btn_address_list:
			index = 1;
			break;
		case R.id.btn_setting:
			index = 2;
			break;
		}
		if (currentTabIndex != index) {
			FragmentTransaction trx = getSupportFragmentManager().beginTransaction();
			trx.hide(fragments[currentTabIndex]);
			if (!fragments[index].isAdded()) {
				trx.add(R.id.fragment_container, fragments[index]);
			}
			trx.show(fragments[index]).commit();
		}
		mTabs[currentTabIndex].setSelected(false);
		// set current tab selected
		mTabs[index].setSelected(true);
		currentTabIndex = index;
	}
*/
    EMMessageListener messageListener = new EMMessageListener() {

        @Override
        public void onMessageReceived(List<EMMessage> messages) {
            // notify new message
            for (EMMessage message : messages) {
                SuperWeChatDemoHelper.getInstance().getNotifier().onNewMsg(message);
            }
            refreshUIWithMessage();
        }

        @Override
        public void onCmdMessageReceived(List<EMMessage> messages) {
            //red packet code : 处理红包回执透传消息
            for (EMMessage message : messages) {
                EMCmdMessageBody cmdMsgBody = (EMCmdMessageBody) message.getBody();
                final String action = cmdMsgBody.action();//获取自定义action
                if (action.equals(RPConstant.REFRESH_GROUP_RED_PACKET_ACTION)) {
                    RedPacketUtil.receiveRedPacketAckMessage(message);
                }
            }
            //end of red packet code
            refreshUIWithMessage();
        }

        @Override
        public void onMessageRead(List<EMMessage> messages) {
        }

        @Override
        public void onMessageDelivered(List<EMMessage> message) {
        }

        @Override
        public void onMessageChanged(EMMessage message, Object change) {
        }
    };

    private void refreshUIWithMessage() {
        runOnUiThread(new Runnable() {
            public void run() {
                // refresh unread count
                updateUnreadLabel();
                if (currentTabIndex == 0) {
                    // refresh conversation list
                    if (conversationListFragment != null) {
                        conversationListFragment.refresh();
                    }
                }
            }
        });
    }

    @Override
    public void back(View view) {
        super.back(view);
    }

    private void registerBroadcastReceiver() {
        broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.ACTION_CONTACT_CHANAGED);
        intentFilter.addAction(Constant.ACTION_GROUP_CHANAGED);
        intentFilter.addAction(RPConstant.REFRESH_GROUP_RED_PACKET_ACTION);
        broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                updateUnreadLabel();
                updateUnreadAddressLable();
                if (currentTabIndex == 0) {
                    // refresh conversation list
                    if (conversationListFragment != null) {
                        conversationListFragment.refresh();
                    }
                } else if (currentTabIndex == 1) {
                    if (contactListFragment != null) {
                        contactListFragment.refresh();
                    }
                }
                String action = intent.getAction();
                if (action.equals(Constant.ACTION_GROUP_CHANAGED)) {
                    if (EaseCommonUtils.getTopActivity(MainActivity.this).equals(GroupsActivity.class.getName())) {
                        GroupsActivity.instance.onResume();
                    }
                }
                //red packet code : 处理红包回执透传消息
                if (action.equals(RPConstant.REFRESH_GROUP_RED_PACKET_ACTION)) {
                    if (conversationListFragment != null) {
                        conversationListFragment.refresh();
                    }
                }
                //end of red packet code
            }
        };
        broadcastManager.registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        Log.e(TAG, "onPageScrolled,position=" + position + ",positionOffset=" + positionOffset + ",positionOffsetPixels=" + positionOffsetPixels);

    }

    @Override
    public void onPageSelected(int position) {
        Log.e(TAG, "onPageSelected,position=" + position);
        //// FIXME: 2017/3/31 
        layoutTabhost.setChecked(position);//这边设置按钮显示哪个
        //// FIXME: 2017/4/6 判断红点时的当前下标
        currentTabIndex=position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        Log.e(TAG, "onPageScrollStateChanged,state:" + state);
    }

    @Override
    public void onCheckedChange(int checkedPosition, boolean byUser) {
        Log.e(TAG, "onCheckedChange,checkedPosition:" + checkedPosition + ",byUser=" + byUser);
        //这边显示按钮同步的滑动,两边调用另一方
        //// FIXME: 2017/3/31
        layoutViewpage.setCurrentItem(checkedPosition, false);
        //// FIXME: 2017/4/6
        currentTabIndex=checkedPosition;
    }

    public class MyContactListener implements EMContactListener {
        @Override
        public void onContactAdded(String username) {
        }

        @Override
        public void onContactDeleted(final String username) {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (ChatActivity.activityInstance != null && ChatActivity.activityInstance.toChatUsername != null &&
                            username.equals(ChatActivity.activityInstance.toChatUsername)) {
                        String st10 = getResources().getString(R.string.have_you_removed);
                        Toast.makeText(MainActivity.this, ChatActivity.activityInstance.getToChatUsername() + st10, Toast.LENGTH_LONG)
                                .show();
                        ChatActivity.activityInstance.finish();
                    }
                }
            });
            updateUnreadAddressLable();
        }

        @Override
        public void onContactInvited(String username, String reason) {
        }

        @Override
        public void onFriendRequestAccepted(String username) {
        }

        @Override
        public void onFriendRequestDeclined(String username) {
        }
    }

    private void unregisterBroadcastReceiver() {
        broadcastManager.unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (exceptionBuilder != null) {
            exceptionBuilder.create().dismiss();
            exceptionBuilder = null;
            isExceptionDialogShow = false;
        }
        unregisterBroadcastReceiver();

        try {
            unregisterReceiver(internalDebugReceiver);
        } catch (Exception e) {
        }

    }

    /**
     * update unread message count
     */
    public void updateUnreadLabel() {
    //// FIXME: 2017/4/8 显示会话列表的消息数量
        int count = getUnreadMsgCountTotal();
        layoutTabhost.setUnreadCount(0,count);
        /*if (count > 0) {
            unreadLabel.setText(String.valueOf(count));
            unreadLabel.setVisibility(View.VISIBLE);
        } else {
            unreadLabel.setVisibility(View.INVISIBLE);
        }*/
    }

    /**
     * update the total unread count
     */
    public void updateUnreadAddressLable() {
        //// FIXME: 2017/4/6 显示有消息时的小圆点
        runOnUiThread(new Runnable() {
            public void run() {
                int count = getUnreadAddressCountTotal();
                layoutTabhost.setHasNew(1,count>0);
               /* if (count > 0) {
                    unreadAddressLable.setVisibility(View.VISIBLE);
                } else {
                    unreadAddressLable.setVisibility(View.INVISIBLE);
                }*/
            }
        });

    }

    /**
     * get unread event notification count, including application, accepted, etc
     *
     * @return
     */
    public int getUnreadAddressCountTotal() {
        int unreadAddressCountTotal = 0;
        unreadAddressCountTotal = inviteMessgeDao.getUnreadMessagesCount();
        return unreadAddressCountTotal;
    }

    /**
     * get unread message count
     *
     * @return
     */
    public int getUnreadMsgCountTotal() {
        int unreadMsgCountTotal = 0;
        int chatroomUnreadMsgCount = 0;
        unreadMsgCountTotal = EMClient.getInstance().chatManager().getUnreadMessageCount();
        for (EMConversation conversation : EMClient.getInstance().chatManager().getAllConversations().values()) {
            if (conversation.getType() == EMConversationType.ChatRoom)
                chatroomUnreadMsgCount = chatroomUnreadMsgCount + conversation.getUnreadMsgCount();
        }
        return unreadMsgCountTotal - chatroomUnreadMsgCount;
    }

    private InviteMessgeDao inviteMessgeDao;

    @Override
    protected void onResume() {
        super.onResume();
            //放在这里是返回显示时恢复数据
            //// FIXME: 2017/4/7 在单例中传intent不能新建实例,再次开启,不是走的oncreate().而是走的onNewIntent()的方法
        if (!isConflict && !isCurrentAccountRemoved) {
            updateUnreadLabel();
            updateUnreadAddressLable();
        }

        // unregister this event listener when this activity enters the
        // background
        SuperWeChatDemoHelper sdkHelper = SuperWeChatDemoHelper.getInstance();
        sdkHelper.pushActivity(this);

        EMClient.getInstance().chatManager().addMessageListener(messageListener);
    }

    @Override
    protected void onStop() {
        EMClient.getInstance().chatManager().removeMessageListener(messageListener);
        SuperWeChatDemoHelper sdkHelper = SuperWeChatDemoHelper.getInstance();
        sdkHelper.popActivity(this);

        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("isConflict", isConflict);
        outState.putBoolean(Constant.ACCOUNT_REMOVED, isCurrentAccountRemoved);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private AlertDialog.Builder exceptionBuilder;
    private boolean isExceptionDialogShow = false;
    private BroadcastReceiver internalDebugReceiver;
    private ConversationListFragment conversationListFragment;
    private BroadcastReceiver broadcastReceiver;
    private LocalBroadcastManager broadcastManager;

    private int getExceptionMessageId(String exceptionType) {
        if (exceptionType.equals(Constant.ACCOUNT_CONFLICT)) {
            return R.string.connect_conflict;
        } else if (exceptionType.equals(Constant.ACCOUNT_REMOVED)) {
            return R.string.em_user_remove;
        } else if (exceptionType.equals(Constant.ACCOUNT_FORBIDDEN)) {
            return R.string.user_forbidden;
        }
        return R.string.Network_error;
    }

    /**
     * show the dialog when user met some exception: such as login on another device, user removed or user forbidden
     */
    private void showExceptionDialog(String exceptionType) {
        isExceptionDialogShow = true;
        SuperWeChatDemoHelper.getInstance().logout(false, null);
        String st = getResources().getString(R.string.Logoff_notification);
        if (!MainActivity.this.isFinishing()) {
            // clear up global variables
            try {
                if (exceptionBuilder == null)
                    exceptionBuilder = new AlertDialog.Builder(MainActivity.this);
                exceptionBuilder.setTitle(st);
                exceptionBuilder.setMessage(getExceptionMessageId(exceptionType));
                exceptionBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        exceptionBuilder = null;
                        isExceptionDialogShow = false;
                        finish();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });
                exceptionBuilder.setCancelable(false);
                exceptionBuilder.create().show();
                isConflict = true;
            } catch (Exception e) {
                EMLog.e(TAG, "---------color conflictBuilder error" + e.getMessage());
            }
        }
    }

    private void showExceptionDialogFromIntent(Intent intent) {
        EMLog.e(TAG, "showExceptionDialogFromIntent");
        if (!isExceptionDialogShow && intent.getBooleanExtra(Constant.ACCOUNT_CONFLICT, false)) {
            showExceptionDialog(Constant.ACCOUNT_CONFLICT);
        } else if (!isExceptionDialogShow && intent.getBooleanExtra(Constant.ACCOUNT_REMOVED, false)) {
            showExceptionDialog(Constant.ACCOUNT_REMOVED);
        } else if (!isExceptionDialogShow && intent.getBooleanExtra(Constant.ACCOUNT_FORBIDDEN, false)) {
            showExceptionDialog(Constant.ACCOUNT_FORBIDDEN);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showExceptionDialogFromIntent(intent);
        //// FIXME: 2017/4/7 你保存了数据,但是Activity没有停止,你可以得到当时保存的状态,如果是resume()就会丢失
        boolean isChat = intent.getBooleanExtra(I.IS_FROM_CHAT, false);
        if(isChat){
            //成功跳转改变到微信的首页,这只是虚拟键盘的返回键
            layoutTabhost.setChecked(0);
            layoutViewpage.setCurrentItem(0);
        }else{

        }
    }

    /**
     * debug purpose only, you can ignore this
     */
    private void registerInternalDebugReceiver() {
        internalDebugReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                SuperWeChatDemoHelper.getInstance().logout(false, new EMCallBack() {

                    @Override
                    public void onSuccess() {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                finish();
                                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                            }
                        });
                    }

                    @Override
                    public void onProgress(int progress, String status) {
                    }

                    @Override
                    public void onError(int code, String message) {
                    }
                });
            }
        };
        IntentFilter filter = new IntentFilter(getPackageName() + ".em_internal_debug");
        registerReceiver(internalDebugReceiver, filter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }
}
