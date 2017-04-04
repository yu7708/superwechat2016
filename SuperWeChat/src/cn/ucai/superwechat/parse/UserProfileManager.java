package cn.ucai.superwechat.parse;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;

import cn.ucai.superwechat.I;
import cn.ucai.superwechat.SuperWeChatDemoHelper;
import cn.ucai.superwechat.SuperWeChatDemoHelper.DataSyncListener;
import cn.ucai.superwechat.db.IUserModel;
import cn.ucai.superwechat.db.OnCompleteListener;
import cn.ucai.superwechat.db.UserModel;
import cn.ucai.superwechat.utils.PreferenceManager;
import cn.ucai.superwechat.utils.Result;
import cn.ucai.superwechat.utils.ResultUtils;

import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.domain.User;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UserProfileManager {
	private static final String TAG = "UserProfileManager";
	/**
	 * application context
	 */
	protected Context appContext = null;

	/**
	 * init flag: test if the sdk has been inited before, we don't need to init
	 * again
	 */
	private boolean sdkInited = false;

	/**
	 * HuanXin sync contact nick and avatar listener
	 */
	private List<DataSyncListener> syncContactInfosListeners;

	private boolean isSyncingContactInfosWithServer = false;

	private EaseUser currentUser;
	//自己是用的类
	private User currentAppUser;


	//// FIXME: 2017/4/1 之前数据库操作时调用过
	IUserModel userModel;

	public UserProfileManager() {
	}

	public synchronized boolean init(Context context) {
		if (sdkInited) {
			return true;
		}
		ParseManager.getInstance().onInit(context);
		syncContactInfosListeners = new ArrayList<DataSyncListener>();
		appContext=context;
		sdkInited = true;
		userModel=new UserModel();
		return true;
	}

	public void addSyncContactInfoListener(DataSyncListener listener) {
		if (listener == null) {
			return;
		}
		if (!syncContactInfosListeners.contains(listener)) {
			syncContactInfosListeners.add(listener);
		}
	}

	public void removeSyncContactInfoListener(DataSyncListener listener) {
		if (listener == null) {
			return;
		}
		if (syncContactInfosListeners.contains(listener)) {
			syncContactInfosListeners.remove(listener);
		}
	}

	public void asyncFetchContactInfosFromServer(List<String> usernames, final EMValueCallBack<List<EaseUser>> callback) {
		if (isSyncingContactInfosWithServer) {
			return;
		}
		isSyncingContactInfosWithServer = true;
		ParseManager.getInstance().getContactInfos(usernames, new EMValueCallBack<List<EaseUser>>() {

			@Override
			public void onSuccess(List<EaseUser> value) {
				isSyncingContactInfosWithServer = false;
				// in case that logout already before server returns,we should
				// return immediately
				if (!SuperWeChatDemoHelper.getInstance().isLoggedIn()) {
					return;
				}
				if (callback != null) {
					callback.onSuccess(value);
				}
			}

			@Override
			public void onError(int error, String errorMsg) {
				isSyncingContactInfosWithServer = false;
				if (callback != null) {
					callback.onError(error, errorMsg);
				}
			}

		});

	}

	public void notifyContactInfosSyncListener(boolean success) {
		for (DataSyncListener listener : syncContactInfosListeners) {
			listener.onSyncComplete(success);
		}
	}

	public boolean isSyncingContactInfoWithServer() {
		return isSyncingContactInfosWithServer;
	}

	public synchronized void reset() {
		isSyncingContactInfosWithServer = false;
		currentUser = null;
		currentAppUser=null;
		PreferenceManager.getInstance().removeCurrentUserInfo();
	}
	public synchronized User getCurrentAppUserInfo(){
		//放到sharedPreference里
		if(currentAppUser==null){
			String username=EMClient.getInstance().getCurrentUser();
			currentAppUser=new User(username);
			String nick=getCurrentUserNick();
			currentAppUser.setMUserNick(nick!=null?nick:username);
		}
		return currentAppUser;
	}
	public synchronized EaseUser getCurrentUserInfo() {
		if (currentUser == null) {
			String username = EMClient.getInstance().getCurrentUser();
			currentUser = new EaseUser(username);
			//这个昵称递归调用自己，拿不到昵称的数据
			String nick = getCurrentUserNick();
			currentUser.setNick((nick != null) ? nick : username);
			currentUser.setAvatar(getCurrentUserAvatar());
		}
		return currentUser;
	}

	public boolean updateCurrentUserNickName(final String nickname) {
		//// FIXME: 2017/4/1
		//我们修改得进行网络的访问,然后返回昵称数据
		userModel.updateUserNick(appContext, EMClient.getInstance().getCurrentUser(), nickname,
				new OnCompleteListener<String>() {
					@Override
					public void onSuccess(String s) {
						//生成一个boolean判断,返回接收的广播类型
						boolean updatenick=false;
						//首先是得到的数据要转化,转化的要判断是否接受,接受的要判断返回值是否正常
						if(s!=null){
							Result result = ResultUtils.getResultFromJson(s, User.class);
							if(result.isRetMsg()){
								User user = (User) result.getRetData();
								if(user!=null){
									updatenick=true;
									//存在sharedP里
									setCurrentAppUserNick(user.getMUserNick());
									//存在数据库中
									SuperWeChatDemoHelper.getInstance().saveAppContact(user);
								}
							}
						}
						//发送广播接收
						appContext.sendBroadcast(new Intent(I.REQUEST_UPDATE_USER_NICK)
								.putExtra(I.User.NICK,updatenick));
					}

					@Override
					public void onError(String error) {
						appContext.sendBroadcast(new Intent(I.REQUEST_UPDATE_USER_NICK)
								.putExtra(I.User.NICK,false));
					}
				});
		//// FIXME: 2017/4/1 改后不用了
		/*//提供的第三方的,得自己建立方法
		boolean isSuccess = ParseManager.getInstance().updateParseNickName(nickname);
		if (isSuccess) {
			//这一句表示昵称保存在sharedPreference里
			setCurrentUserNick(nickname);
		}
		return isSuccess;*/
		return false;
	}

	/*public String uploadUserAvatar(byte[] data) {
		String avatarUrl = ParseManager.getInstance().uploadParseAvatar(data);
		if (avatarUrl != null) {
			setCurrentUserAvatar(avatarUrl);
		}
		return avatarUrl;
	}*/
	public void uploadUserAvatar(File file){
		userModel.updateAvatar(appContext, EMClient.getInstance().getCurrentUser(), file,
				new OnCompleteListener<String>() {
					@Override
					public void onSuccess(String s) {
						boolean success=false;
						//设置布尔类型,是根据标志位判读是否发送广播
						if(s!=null){
							//先判断的的得到的结果不为空,然后转化为json,然后
							Result result=ResultUtils.getResultFromJson(s,User.class);
							if(result!=null&&result.isRetMsg()){
								//一直不是很懂这个isRetMsg是什么意思,里面都是直接返回
								User user= (User) result.getRetData();
								//也就是说这里返回的是uesr
								if(user!=null){
									success=true;
									//更改图片
									setCurrentAppUserAvatar(user.getAvatar());
									//
									SuperWeChatDemoHelper.getInstance().saveAppContact(user);
								}
							}
						}
						//发送广播接收,写上你的发送码,后面也是你传的参数,
						appContext.sendBroadcast(new Intent(I.REQUEST_UPDATE_AVATAR)
								.putExtra(I.Avatar.UPDATE_TIME,success));
					}

					@Override
					public void onError(String error) {
						//失败也传一个广播
						appContext.sendBroadcast(new Intent(I.REQUEST_UPDATE_AVATAR)
								.putExtra(I.Avatar.UPDATE_TIME,false));
					}
				});
	}
	public void asyncGetCurrentAppUserInfo() {
		//异步取得本地用户数据
		userModel.loadUserInfo(appContext, EMClient.getInstance().getCurrentUser(),
				new OnCompleteListener<String>() {
					@Override
					public void onSuccess(String s) {
						if(s!=null){
							Result result = ResultUtils.getResultFromJson(s, User.class);
							//拿到返回的信息
							if(result!=null&&result.isRetMsg()){
								User user = (User) result.getRetData();
								Log.e(TAG,"asyncGetCurrentAppUserInfo,user="+user);
								//仿下面拿到图片和昵称
								if(user!=null) {
									//// FIXME: 2017/4/4
									currentAppUser=user;
									//f
									setCurrentAppUserNick(user.getMUserNick());
									setCurrentAppUserAvatar(user.getAvatar());
									SuperWeChatDemoHelper.getInstance().saveAppContact(user);
								}
							}
						}
					}

					@Override
					public void onError(String error) {

					}
				});
	}
	public void asyncGetCurrentUserInfo() {
		//异步取得环信用户数据
		ParseManager.getInstance().asyncGetCurrentUserInfo(new EMValueCallBack<EaseUser>() {

			@Override
			public void onSuccess(EaseUser value) {
			    if(value != null){
					//保存头像，名称
    				setCurrentUserNick(value.getNick());
    				setCurrentUserAvatar(value.getAvatar());
			    }
			}

			@Override
			public void onError(int error, String errorMsg) {

			}
		});

	}
	public void asyncGetUserInfo(final String username,final EMValueCallBack<EaseUser> callback){
		ParseManager.getInstance().asyncGetUserInfo(username, callback);
	}
	private void setCurrentUserNick(String nickname) {
		//一个设置，一个放在本地
		getCurrentUserInfo().setNick(nickname);
		PreferenceManager.getInstance().setCurrentUserNick(nickname);
	}
	// ---f
	private void setCurrentAppUserNick(String nickname){
		getCurrentAppUserInfo().setMUserNick(nickname);
		PreferenceManager.getInstance().setCurrentUserNick(nickname);
	}
	//图像的数据是分散的，在user中新建一个对象avatar
	private void setCurrentAppUserAvatar(String avatar){
		getCurrentAppUserInfo().setAvatar(avatar);
		PreferenceManager.getInstance().setCurrentUserAvatar(avatar);
	}
	// ---f
	private void setCurrentUserAvatar(String avatar) {
		getCurrentUserInfo().setAvatar(avatar);
		PreferenceManager.getInstance().setCurrentUserAvatar(avatar);
	}

	private String getCurrentUserNick() {
		return PreferenceManager.getInstance().getCurrentUserNick();
	}

	private String getCurrentUserAvatar() {
		return PreferenceManager.getInstance().getCurrentUserAvatar();
	}


}
