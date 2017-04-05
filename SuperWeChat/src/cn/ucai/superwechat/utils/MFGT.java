package cn.ucai.superwechat.utils;

import android.app.Activity;
import android.content.Intent;

import com.hyphenate.easeui.domain.User;

import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.ui.GuideActivity;
import cn.ucai.superwechat.ui.LoginActivity;
import cn.ucai.superwechat.ui.MainActivity;
import cn.ucai.superwechat.ui.RegisterActivity;
import cn.ucai.superwechat.ui.SettingsActivity;
import cn.ucai.superwechat.ui.UserProfileActivity;
import cn.ucai.superwechat.ui.activity.FriendProfileActivity;
import cn.ucai.superwechat.ui.activity.SendAddFriendActivity;


/**
 * Created by Administrator on 2017/3/16.
 */

public class MFGT {
    public static void finish(Activity activity){
        activity.finish();
        activity.overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
    }
    public static void startActivity(Activity activity,Class cls){
        activity.startActivity(new Intent(activity,cls));
        activity.overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
    }
    public static void startActivity(Activity activity,Intent intent){
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
    }
    public static void gotoMain(Activity activity){
        startActivity(activity, MainActivity.class);
    }
    public static void startActivityForResult(Activity activity,Intent intent,int requestCode){
        activity.startActivityForResult(intent,requestCode);
        activity.overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
    }
    public static void gotoGuide(Activity activity){
        startActivity(activity,GuideActivity.class);
    }

    public static void gotoLogin(Activity activity) {
        startActivity(activity, LoginActivity.class);
    }
    public static void gotoRegister(Activity activity){
        startActivity(activity, RegisterActivity.class);
    }

    public static void gotoSetting(Activity activity) {
        startActivity(activity,SettingsActivity.class);
    }

    public static void gotoUserInfo(Activity activity, boolean setting, String currentUser) {
        startActivity(activity,new Intent(activity,UserProfileActivity.class)
        .putExtra("setting",setting)
        .putExtra("username",currentUser));
    }

    public static void gotoFriend(Activity activity, User user) {
        startActivity(activity,new Intent(activity,FriendProfileActivity.class)
        .putExtra(I.User.TABLE_NAME,user));
    }

    public static void gotoSendAddFriend(Activity activity, String userName) {
        startActivity(activity,new Intent(activity,SendAddFriendActivity.class)
        .putExtra(I.User.USER_NAME,userName));
    }
}
