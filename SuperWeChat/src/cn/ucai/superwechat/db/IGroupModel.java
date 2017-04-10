package cn.ucai.superwechat.db;

import android.content.Context;

import java.io.File;

import cn.ucai.superwechat.domain.Group;
import cn.ucai.superwechat.utils.OkHttpUtils;

/**
 * Created by Administrator on 2017/3/30.
 */

public interface IGroupModel {
   void newGroup(Context context,String hxid,String groupName,String description,String owner,
                 boolean groupIsPublic,boolean allowInvite,File file,OnCompleteListener<String> listener);
}
