package cn.ucai.superwechat.db;

import android.content.Context;

import cn.ucai.superwechat.utils.OkHttpUtils;

/**
 * Created by Administrator on 2017/3/30.
 */

public interface IUserModel {
    void register(Context context, String username, String nickname, String password,
                  OnCompleteListener<String> listener);
    void login(Context context,String username,String password,
               OnCompleteListener<String> listener);
    void unregister(Context context,String username,OnCompleteListener<String> listener);
}
