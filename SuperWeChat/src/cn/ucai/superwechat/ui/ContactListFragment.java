/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ucai.superwechat.ui;

import java.util.Hashtable;
import java.util.Map;

import com.hyphenate.chat.EMClient;

import cn.ucai.superwechat.SuperWeChatDemoHelper;
import cn.ucai.superwechat.SuperWeChatDemoHelper.DataSyncListener;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.db.IUserModel;
import cn.ucai.superwechat.db.InviteMessgeDao;
import cn.ucai.superwechat.db.OnCompleteListener;
import cn.ucai.superwechat.db.UserDao;
import cn.ucai.superwechat.db.UserModel;
import cn.ucai.superwechat.utils.MFGT;
import cn.ucai.superwechat.utils.Result;
import cn.ucai.superwechat.utils.ResultUtils;
import cn.ucai.superwechat.widget.ContactItemView;
import cn.ucai.superwechat.widget.TitleMenu.ActionItem;
import cn.ucai.superwechat.widget.TitleMenu.TitlePopup;

import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.ui.EaseContactListFragment;
import com.hyphenate.util.EMLog;
import com.hyphenate.util.NetUtils;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

/**
 * contact list
 * 
 */
public class ContactListFragment extends EaseContactListFragment {
	
    private static final String TAG = ContactListFragment.class.getSimpleName();
    private ContactSyncListener contactSyncListener;
    private BlackListSyncListener blackListSyncListener;
    private ContactInfoSyncListener contactInfoSyncListener;
    private View loadingView;
    private ContactItemView applicationItem;
    private InviteMessgeDao inviteMessgeDao;
    ProgressDialog pd;
TitlePopup mPopup;
    @SuppressLint("InflateParams")
    @Override
    protected void initView() {
        super.initView();
        @SuppressLint("InflateParams") View headerView = LayoutInflater.from(getActivity()).inflate(R.layout.em_contacts_header, null);
        HeaderItemClickListener clickListener = new HeaderItemClickListener();
        //申请与通知
        applicationItem = (ContactItemView) headerView.findViewById(R.id.application_item);
        applicationItem.setOnClickListener(clickListener);
        headerView.findViewById(R.id.group_item).setOnClickListener(clickListener);
       // headerView.findViewById(R.id.chat_room_item).setOnClickListener(clickListener);
      //  headerView.findViewById(R.id.robot_item).setOnClickListener(clickListener);
        listView.addHeaderView(headerView);
        //add loading view
        loadingView = LayoutInflater.from(getActivity()).inflate(R.layout.em_layout_loading_data, null);
        contentContainer.addView(loadingView);

        registerForContextMenu(listView);
    }
    
    @Override
    public void refresh() {
        //// FIXME: 2017/4/7 这里我们要换源,把原生的环信的改成自己的\
        Map<String, User> m = cn.ucai.superwechat.SuperWeChatDemoHelper.getInstance().getAppContactList();
        if (m instanceof Hashtable<?, ?>) {
            //noinspection unchecked
            m = (Map<String, User>) ((Hashtable<String, User>)m).clone();
        }
        setContactsMap(m);
        super.refresh();
        if(inviteMessgeDao == null){
            inviteMessgeDao = new InviteMessgeDao(getActivity());
        }
        if(inviteMessgeDao.getUnreadMessagesCount() > 0){
            applicationItem.showUnreadMsgView();
        }else{
            applicationItem.hideUnreadMsgView();
        }
    }
    
    
    @SuppressWarnings("unchecked")
    @Override
    protected void setUpView() {
        //// FIXME: 2017/4/5 设置弹窗
        mPopup=new TitlePopup(getContext(), ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopup.addAction(new ActionItem(getContext(),R.string.menu_groupchat,R.drawable.icon_menu_group));
        mPopup.addAction(new ActionItem(getContext(),R.string.menu_addfriend,R.drawable.icon_menu_addfriend));
        mPopup.addAction(new ActionItem(getContext(),R.string.menu_qrcode,R.drawable.icon_menu_sao));
        mPopup.addAction(new ActionItem(getContext(),R.string.menu_money,R.drawable.icon_menu_money));
        //设置通讯录的显示和监听
        titleBar.setRightImageResource(R.drawable.em_add);
        titleBar.setRightLayoutClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
//                startActivity(new Intent(getActivity(), AddContactActivity.class));
                //判断网络连接,这里我看不到源码,不知道具体内容
                NetUtils.hasDataConnection(getActivity());
                mPopup.show(titleBar);
            }
        });
        //设置联系人数据
        Map<String, User> m = cn.ucai.superwechat.SuperWeChatDemoHelper.getInstance().getAppContactList();
        if (m instanceof Hashtable<?, ?>) {
            m = (Map<String, User>) ((Hashtable<String, User>)m).clone();
        }
        setContactsMap(m);
        super.setUpView();
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                User user = (User)listView.getItemAtPosition(position);
                if (user != null) {
                   /* String username = user.getMUserName();
                    // demo中直接进入聊天页面，实际一般是进入用户详情页
                    startActivity(new Intent(getActivity(), ChatActivity.class).putExtra("userId", username));*/
                    MFGT.gotoFriend(getActivity(),user);
                }
            }
        });

        
     /*   // 进入添加好友页
        titleBar.getRightLayout().setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
               startActivity(new Intent(getActivity(), AddContactActivity.class));
                }
        });*/
        
        
        contactSyncListener = new ContactSyncListener();
        SuperWeChatDemoHelper.getInstance().addSyncContactListener(contactSyncListener);
        
        blackListSyncListener = new BlackListSyncListener();
        SuperWeChatDemoHelper.getInstance().addSyncBlackListListener(blackListSyncListener);
        
        contactInfoSyncListener = new ContactInfoSyncListener();
        cn.ucai.superwechat.SuperWeChatDemoHelper.getInstance().getUserProfileManager().addSyncContactInfoListener(contactInfoSyncListener);
        
        if (SuperWeChatDemoHelper.getInstance().isContactsSyncedWithServer()) {
            loadingView.setVisibility(View.GONE);
        } else if (SuperWeChatDemoHelper.getInstance().isSyncingContactsWithServer()) {
            loadingView.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (contactSyncListener != null) {
            cn.ucai.superwechat.SuperWeChatDemoHelper.getInstance().removeSyncContactListener(contactSyncListener);
            contactSyncListener = null;
        }
        
        if(blackListSyncListener != null){
            cn.ucai.superwechat.SuperWeChatDemoHelper.getInstance().removeSyncBlackListListener(blackListSyncListener);
        }
        
        if(contactInfoSyncListener != null){
            cn.ucai.superwechat.SuperWeChatDemoHelper.getInstance().getUserProfileManager().removeSyncContactInfoListener(contactInfoSyncListener);
        }
    }
    
	
	protected class HeaderItemClickListener implements OnClickListener{

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.application_item:
                // 进入申请与通知页面
                startActivity(new Intent(getActivity(), NewFriendsMsgActivity.class));
                break;
            case R.id.group_item:
                // 进入群聊列表页面
                startActivity(new Intent(getActivity(), GroupsActivity.class));
                break;
        /*    case R.id.chat_room_item:
                //进入聊天室列表页面
                startActivity(new Intent(getActivity(), PublicChatRoomsActivity.class));
                break;
            case R.id.robot_item:
                //进入Robot列表页面
                startActivity(new Intent(getActivity(), RobotsActivity.class));
                break;
*/
            default:
                break;
            }
        }
	    
	}
	

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
        //// FIXME: 2017/4/8 长按好友程序崩溃,改了EaseUser为user
        toBeProcessUser = (User) listView.getItemAtPosition(((AdapterContextMenuInfo) menuInfo).position);
	    toBeProcessUsername = toBeProcessUser.getMUserName();
		getActivity().getMenuInflater().inflate(R.menu.em_context_contact_list, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
        //// FIXME: 2017/4/8 点击判断执行删除有没有走这里的方法
        Log.e(TAG, "onContextItemSelected: user="+toBeProcessUser);
        //如果点击的是删除按钮
		if (item.getItemId() == R.id.delete_contact) {
			try {
                // delete contact删除联系人,这里写的是环信的删除,我们也在这个方法里面写,
                deleteContact(toBeProcessUser);
                // remove invitation message
                InviteMessgeDao dao = new InviteMessgeDao(getActivity());
                dao.deleteMessage(toBeProcessUser.getMUserName());
            } catch (Exception e) {
                e.printStackTrace();
            }
			return true;
		}else if(item.getItemId() == R.id.add_to_blacklist){
			moveToBlacklist(toBeProcessUsername);
			return true;
		}
		return super.onContextItemSelected(item);
	}


	/**
	 * delete contact
	 * 
	 * @param tobeDeleteUser
	 */
	public void deleteContact(final User tobeDeleteUser) {
		String st1 = getResources().getString(R.string.deleting);
		pd = new ProgressDialog(getActivity());
		pd.setMessage(st1);
		pd.setCanceledOnTouchOutside(false);
		pd.show();
        removeContact(tobeDeleteUser);
	}
    private void removeEMContact(final User tobeDeleteUser){
        final String st2 = getResources().getString(R.string.Delete_failed);
        new Thread(new Runnable() {
            public void run() {
                try {
                    EMClient.getInstance().contactManager().deleteContact(tobeDeleteUser.getMUserName());
                    // remove user from memory and database
                    //从数据库中删除联系人
                    UserDao dao = new UserDao(getActivity());
                    // FIXME: 2017/4/8 仿造环信删除数据库里的联系人
                    // FIXME: 2017/4/8 在这边删除了一次数据库,为什么在SuperWechatHelper的onContactDeleted()中也删除了数据库
                    dao.deleteAppContact(tobeDeleteUser.getMUserName());
                    SuperWeChatDemoHelper.getInstance().getAppContactList().remove(tobeDeleteUser.getMUserName());
                    ///----还得从服务器上请求删除联系人.-----f
                    dao.deleteContact(tobeDeleteUser.getMUserName());
                    SuperWeChatDemoHelper.getInstance().getContactList().remove(tobeDeleteUser.getMUserName());
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            pd.dismiss();
                            contactList.remove(tobeDeleteUser);
                            contactListLayout.refresh();

                        }
                    });
                } catch (final Exception e) {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            pd.dismiss();
                            Toast.makeText(getActivity(), st2 + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

                }

            }
        }).start();

    }
    //// FIXME: 2017/4/8 网络请求删除联系人,还得加层保护,先网络请求删除了联系人再数据库等本地删除
    private void removeContact(final User tobeDeleteUser){
        IUserModel userModel=new UserModel();
        userModel.deleteContact(getContext(), EMClient.getInstance().getCurrentUser(), tobeDeleteUser.getMUserName(),
                new OnCompleteListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        if(s!=null){
                            Result result = ResultUtils.getResultFromJson(s, User.class);
                            if(result!=null&&result.isRetMsg()){
                                //执行删除,把上面的方法拆分一下,调用分离出来的环信的方法
                                removeEMContact(tobeDeleteUser);
                            }
                        }
                    }
                    @Override
                    public void onError(String error) {
                        final String st2 = getResources().getString(R.string.Delete_failed);
                        pd.dismiss();
                        Toast.makeText(getActivity(), st2 , Toast.LENGTH_LONG).show();
                    }
                });
    }
	class ContactSyncListener implements DataSyncListener{
        @Override
        public void onSyncComplete(final boolean success) {
            EMLog.d(TAG, "on contact list sync success:" + success);
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    getActivity().runOnUiThread(new Runnable(){

                        @Override
                        public void run() {
                            if(success){
                                loadingView.setVisibility(View.GONE);
                                refresh();
                            }else{
                                String s1 = getResources().getString(R.string.get_failed_please_check);
                                Toast.makeText(getActivity(), s1, Toast.LENGTH_LONG).show();
                                loadingView.setVisibility(View.GONE);
                            }
                        }
                        
                    });
                }
            });
        }
    }
    
    class BlackListSyncListener implements DataSyncListener{

        @Override
        public void onSyncComplete(boolean success) {
            getActivity().runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    refresh();
                }
            });
        }
        
    }

    class ContactInfoSyncListener implements DataSyncListener{

        @Override
        public void onSyncComplete(final boolean success) {
            EMLog.d(TAG, "on contactinfo list sync success:" + success);
            getActivity().runOnUiThread(new Runnable() {
                
                @Override
                public void run() {
                    loadingView.setVisibility(View.GONE);
                    if(success){
                        refresh();
                    }
                }
            });
        }
        
    }
	
}
