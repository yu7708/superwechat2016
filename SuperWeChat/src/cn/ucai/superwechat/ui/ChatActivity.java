package cn.ucai.superwechat.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import cn.ucai.superwechat.R;
import cn.ucai.superwechat.runtimepermissions.PermissionsManager;
import cn.ucai.superwechat.utils.MFGT;

import com.hyphenate.easeui.ui.EaseChatFragment;
import com.hyphenate.util.EasyUtils;

/*
*
 * chat activity，EaseChatFragment was used {@link #EaseChatFragment}
 *
*/
public class ChatActivity extends BaseActivity{
    private static final String TAG = "ChatActivity";
    public static ChatActivity activityInstance;
    private EaseChatFragment chatFragment;
    String toChatUsername;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.em_activity_chat);
        activityInstance = this;
        //get user id or group id
        toChatUsername = getIntent().getExtras().getString("userId");
        //use EaseChatFratFragment
        chatFragment = new ChatFragment();
        //pass parameters to chat fragment
        chatFragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().add(R.id.container, chatFragment).commit();
        
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityInstance = null;
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
    	// make sure only one chat activity is opened
        String username = intent.getStringExtra("userId");
        if (toChatUsername.equals(username))
            super.onNewIntent(intent);
        else {
            finish();
            startActivity(intent);
        }

    }
    
    @Override
    public void onBackPressed() {
        Log.e(TAG, "onBackPressed: ");
        //
        chatFragment.onBackPressed();
        Log.e(TAG, "onBackPressed:--------1");
        //判断是不是单例
        if (EasyUtils.isSingleActivity(this)) {
            Log.e(TAG, "onBackPressed:--------2");
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            //这里为什么要跳转到主页面,得到的数据从哪里看出是这个覆写的方法传参
            MFGT.gotoMain(ChatActivity.this,true);
            //接收在主函数中,跳回主函数,显示的应该是在微信的第一部分,
            // 放在OnResume和放在newIntent有什么区别
        }
        Log.e(TAG, "onBackPressed:-----3");
    }
    
    public String getToChatUsername(){
        return toChatUsername;
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
        @NonNull int[] grantResults) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }
}
