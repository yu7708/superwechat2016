package cn.ucai.superwechat.db;

import android.content.Context;

import java.io.File;

import cn.ucai.superwechat.I;
import cn.ucai.superwechat.utils.OkHttpUtils;

/**
 * Created by Administrator on 2017/4/10.
 */

public class GroupModel implements IGroupModel{
    @Override
    public void newGroup(Context context, String hxid, String groupName, String description, String owner, boolean groupIsPublic, boolean allowInvite, File file, OnCompleteListener<String> listener) {
        OkHttpUtils<String> utils=new OkHttpUtils<>(context);
        utils.setRequestUrl(I.REQUEST_CREATE_GROUP)
                .addParam(I.Group.HX_ID,hxid)
                .addParam(I.Group.NAME,groupName)
                .addParam(I.Group.DESCRIPTION,description)
                .addParam(I.Group.OWNER,owner)
                .addParam(I.Group.IS_PUBLIC,groupIsPublic+"")
                .addParam(I.Group.ALLOW_INVITES,allowInvite+"")
                .addFile2(file)
                .post()
                .targetClass(String.class)
                .execute(listener);
    }

    @Override
    public void addMembers(Context context, String members, String hxid, OnCompleteListener<String> listener) {
        OkHttpUtils<String> utils=new OkHttpUtils<>(context);
        utils.setRequestUrl(I.REQUEST_ADD_GROUP_MEMBERS)
                .addParam(I.Member.USER_NAME,members)
                .addParam(I.Member.GROUP_HX_ID,hxid)
                .targetClass(String.class)
                .execute(listener);
    }
}
