package com.hyphenate.easeui.utils;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hyphenate.easeui.R;
import com.hyphenate.easeui.controller.EaseUI;
import com.hyphenate.easeui.controller.EaseUI.EaseUserProfileProvider;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.domain.User;

public class EaseUserUtils {
    
    static EaseUserProfileProvider userProvider;
    
    static {
        userProvider = EaseUI.getInstance().getUserProfileProvider();
    }
    
    /**
     * get EaseUser according username
     * @param username
     * @return
     */
    public static EaseUser getUserInfo(String username){
        if(userProvider != null)
            return userProvider.getUser(username);
        
        return null;
    }
    //// FIXME: 2017/3/31
    public static User getAppUserInfo(String username){
        if(userProvider != null)
            return userProvider.getAppUser(username);

        return null;
    }
    /**
     * set user avatar
     * @param username
     */
    public static void setUserAvatar(Context context, String username, ImageView imageView){
    	EaseUser user = getUserInfo(username);
        if(user != null && user.getAvatar() != null){
            try {
                int avatarResId = Integer.parseInt(user.getAvatar());
                Glide.with(context).load(avatarResId).into(imageView);
            } catch (Exception e) {
                //use default avatar
                Glide.with(context).load(user.getAvatar()).diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(R.drawable.ease_default_avatar).into(imageView);
            }
        }else{
            Glide.with(context).load(R.drawable.ease_default_avatar).into(imageView);
        }
    }
    
    /**
     * set user's nickname
     */
    public static void setUserNick(String username,TextView textView){
        if(textView != null){
        	EaseUser user = getUserInfo(username);
        	if(user != null && user.getNick() != null){
        		textView.setText(user.getNick());
        	}else{
        		textView.setText(username);
        	}
        }
    }
    //// FIXME: 2017/3/31 //设置头像
    /**
     * set user avatar
     * @param username
     */
    public static void setAppUserAvatar(Context context, String username, ImageView imageView){
        User user = getAppUserInfo(username);
        if(user != null && user.getAvatar() != null){
            try {
                int avatarResId = Integer.parseInt(user.getAvatar());
                Glide.with(context).load(avatarResId).into(imageView);
            } catch (Exception e) {
                //use default avatar
                Glide.with(context).load(user.getAvatar()).diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(R.drawable.ease_default_avatar).into(imageView);
            }
        }else{
            Glide.with(context).load(R.drawable.ease_default_avatar).into(imageView);
        }
    }
    public static void setAppAvatar(Context context, String avatarPath, ImageView imageView){
        //User user = getAppUserInfo(username);
        if(avatarPath!=null){
            try {
                int avatarResId = Integer.parseInt(avatarPath);
                Glide.with(context).load(avatarResId).into(imageView);
            } catch (Exception e) {
                //use default avatar
                Glide.with(context).load(avatarPath).diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(R.drawable.ease_default_avatar).into(imageView);
            }
        }else{
            Glide.with(context).load(R.drawable.ease_default_avatar).into(imageView);
        }
    }
    //// FIXME: 2017/4/11 添加群聊显示群头像
    public static void setAppGroupAvatar(Context context, String avatarPath, ImageView imageView){
        //// FIXME: 2017/4/11 设置默认群头像,找到图的地址,
        //User user = getAppUserInfo(username);
        if(avatarPath!=null){
            try {
                int avatarResId = Integer.parseInt(avatarPath);
                Glide.with(context).load(avatarResId).into(imageView);
            } catch (Exception e) {
                //use default avatar
                Glide.with(context).load(avatarPath).diskCacheStrategy(DiskCacheStrategy.ALL).
                        placeholder(R.drawable.ease_group_icon).into(imageView);
            }
        }else{
            Glide.with(context).load(R.drawable.ease_group_icon).into(imageView);
        }
    }
    //// FIXME: 2017/3/31 思考两个头像的方法,差不多,合并多写个方法

    //// FIXME: 2017/3/31 //设置昵称
    /**
     * set user's nickname
     */
    public static void setAppUserNick(String username,TextView textView){
        if(textView != null){
            User user = getAppUserInfo(username);
            if(user != null && user.getMUserNick() != null){
                textView.setText(user.getMUserNick());
            }else{
                textView.setText(username);
            }
        }
    }

}
