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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.platform.comapi.map.C;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMGroupManager.EMGroupOptions;
import com.hyphenate.chat.EMGroupManager.EMGroupStyle;

import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatDemoHelper;
import cn.ucai.superwechat.db.GroupModel;
import cn.ucai.superwechat.db.IGroupModel;
import cn.ucai.superwechat.db.OnCompleteListener;
import cn.ucai.superwechat.domain.Group;
import cn.ucai.superwechat.utils.L;
import cn.ucai.superwechat.utils.Result;
import cn.ucai.superwechat.utils.ResultUtils;

import com.hyphenate.easeui.widget.EaseAlertDialog;
import com.hyphenate.exceptions.HyphenateException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class NewGroupActivity extends BaseActivity {
	private static final String TAG = "NewGroupActivity";
	private EditText groupNameEditText;
	private ProgressDialog progressDialog;
	private EditText introductionEditText;
	private CheckBox publibCheckBox;
	private CheckBox memberCheckbox;
	private TextView secondTextView;
	//private ProgressDialog dialog;
	private String avatarName;
	LinearLayout linearLayoutGroupIcon;
	IGroupModel mModel=null;
	ImageView ivAvatar;
	File avatarFile;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.em_activity_new_group);
		groupNameEditText = (EditText) findViewById(R.id.edit_group_name);
		introductionEditText = (EditText) findViewById(R.id.edit_group_introduction);
		publibCheckBox = (CheckBox) findViewById(R.id.cb_public);
		memberCheckbox = (CheckBox) findViewById(R.id.cb_member_inviter);
		secondTextView = (TextView) findViewById(R.id.second_desc);
		linearLayoutGroupIcon= (LinearLayout) findViewById(R.id.layout_group_icon);
		ivAvatar= (ImageView) findViewById(R.id.iv_avatar);
		mModel=new GroupModel();
		setListener();
	}

	private void setListener() {
		//是否公开的点击事件
		publibCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					//加入公开群需要群主同意
					secondTextView.setText(R.string.join_need_owner_approval);
				}else{
					//开放群成员邀请
					secondTextView.setText(R.string.Open_group_members_invited);
				}
			}
		});
		//// FIXME: 2017/4/10 获取群图标
		linearLayoutGroupIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				uploadHeadPhoto();
			}
		});
	}
	private void uploadHeadPhoto() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dl_title_upload_photo);
		builder.setItems(new String[]{getString(R.string.dl_msg_take_photo), getString(R.string.dl_msg_local_upload)},
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						switch (which) {
							case 0:
								//拍照上传
								Toast.makeText(NewGroupActivity.this, getString(R.string.toast_no_support),
										Toast.LENGTH_SHORT).show();
								break;
							case 1:
								//本地上传
								Intent pickIntent = new Intent(Intent.ACTION_PICK, null);
								pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
								startActivityForResult(pickIntent, I.REQUEST_CODE_PICK_PIC);
								//成功就传一个结果集,前面是你的意图,后面一个是你的请求码
								//上面的intent涉及的是源码,我也不太懂
								break;
							default:
								break;
						}
					}
				});
		builder.create().show();
	}
	/**
	 * @param v
	 */
	public void save(View v) {
		//保存群名
		String name = groupNameEditText.getText().toString();
		if (TextUtils.isEmpty(name)) {
		    new EaseAlertDialog(this, R.string.Group_name_cannot_be_empty).show();
		} else {
			// select from contact list
			//跳转到选择群名的Activity,并传了群名,和一个0
			startActivityForResult(new Intent(this, GroupPickContactsActivity.class).putExtra("groupName", name), I.REQUEST_CODE_PICK_CONTACT);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
		//// FIXME: 2017/4/10 添加群头像的下载
		switch (requestCode) {
			//选择图片
			case I.REQUEST_CODE_PICK_PIC:
				//如果数据为空,或者数据里的数据为空,则返回
				if (data == null || data.getData() == null) {
					return;
				}
				//这是选择,执行这方法的最后一步就是请求裁剪
				startPhotoZoom(data.getData());
				break;
			//请求_代码_切割
			case I.REQUEST_CODE_CUTTING:
				if (data != null) {
					setPicToView(data);
				}
				break;
			case I.REQUEST_CODE_PICK_CONTACT:
				if (resultCode == RESULT_OK) {
					//new group
					//// FIXME: 2017/4/10 提取出分成两个方法
					showDialog();
					createEMGroup(data);//建立易信群组
				}
				break;
			default:
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	//// FIXME: 2017/4/10
	public void startPhotoZoom(Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		intent.putExtra("crop", true);
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("outputX", 300);
		intent.putExtra("outputY", 300);
		intent.putExtra("return-data", true);
		intent.putExtra("noFaceDetection", true);
		//开始剪裁的请求码
		startActivityForResult(intent, I.REQUEST_CODE_CUTTING);
	}

	private void setPicToView(Intent picdata) {
		Bundle extras = picdata.getExtras();
		if (extras != null) {
			Bitmap photo = extras.getParcelable("data");
			Drawable drawable = new BitmapDrawable(getResources(), photo);
			ivAvatar.setImageDrawable(drawable);
			// uploadUserAvatar(Bitmap2Bytes(photo));
			//// FIXME: 2017/4/4  仿造上面这个方法,我们现在传的参数为file
			saveBitmapFile(photo);
		}
	}
	public static String getAvatarPath(Context context, String path){
		File dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		File folder = new File(dir,path);
		if(!folder.exists()){
			folder.mkdir();
		}
		return folder.getAbsolutePath();
	}
	private String getAvatarName() {
		avatarName = I.AVATAR_TYPE_GROUP_PATH+ System.currentTimeMillis();
		//得到文件名为当前的用户名加上系统的时间戳
		L.e(TAG,"avatarname="+avatarName);
		return avatarName;
	}
	private void saveBitmapFile(Bitmap bitmap) {
		if (bitmap != null) {
			//将定义的bitmap进行判断,有数据的话,先拿到路径,然后调用文件,输出bitmap
			String imagePath = getAvatarPath(NewGroupActivity.this,I.AVATAR_TYPE)+"/"+getAvatarName()+".jpg";
			File file = new File(imagePath);//将要保存图片的路径
			L.e("file path="+file.getAbsolutePath());
			try {
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
				bos.flush();
				bos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			avatarFile=file;
		}
	}

	//// FIXME: 2017/4/10
	private void createEMGroup(final Intent data) {
		final String st2 = getResources().getString(R.string.Failed_to_create_groups);
		new Thread(new Runnable() {
			@Override
			public void run() {
				final String groupName = groupNameEditText.getText().toString().trim();
				String desc = introductionEditText.getText().toString();
				String[] members = data.getStringArrayExtra("newmembers");
				try {
					EMGroupOptions option = new EMGroupOptions();
					//最大人数200人
					option.maxUsers = 200;
					//是否邀请
					option.inviteNeedConfirm = true;

					String reason = NewGroupActivity.this.getString(R.string.invite_join_group);
					reason  = EMClient.getInstance().getCurrentUser() + reason + groupName;

					if(publibCheckBox.isChecked()){
						option.style = memberCheckbox.isChecked() ? EMGroupStyle.EMGroupStylePublicJoinNeedApproval : EMGroupStyle.EMGroupStylePublicOpenJoin;
					}else{
						option.style = memberCheckbox.isChecked()?EMGroupStyle.EMGroupStylePrivateMemberCanInvite:EMGroupStyle.EMGroupStylePrivateOnlyOwnerInvite;
					}
					EMGroup emGroup = EMClient.getInstance().groupManager().createGroup(groupName, desc, members, reason, option);
					//// FIXME: 2017/4/10 写上自己的添加群组,让环信的先获取再自己服务器获取
					createAppGroup(emGroup,members);

				} catch (final HyphenateException e) {
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							Toast.makeText(NewGroupActivity.this, st2 + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
						}
					});
				}

			}
		}).start();
	}

	private void createAppGroup(final EMGroup emGroup, final String[] members) {
		if(emGroup!=null){
			mModel.newGroup(NewGroupActivity.this, emGroup.getGroupId(), emGroup.getGroupName(),
					emGroup.getDescription(), emGroup.getOwner(), emGroup.isPublic(), emGroup.isAllowInvites(),
					avatarFile, new OnCompleteListener<String>() {
						@Override
						public void onSuccess(String s) {
							Log.e(TAG, "onSuccess: s"+s);
							boolean isSuccess=false;
							if(s!=null){
								Result result = ResultUtils.getResultFromJson(s, Group.class);
									if(result!=null&&result.isRetMsg()){
									Group group = (Group) result.getRetData();
									if(group!=null){
										if(members.length>0){
											addMembers(emGroup.getGroupId(),members);
										}else {
											isSuccess = true;
										}
									}
								}
							}
							if(members.length<=0) {
								createSuccess(isSuccess);
							}
						}

						@Override
						public void onError(String error) {
							createSuccess(false);
						}
					});
		}
	}
	private void addMembers(String hxid,String[] members){
		mModel.addMembers(NewGroupActivity.this, getMember(members), hxid,
				new OnCompleteListener<String>() {
					@Override
					public void onSuccess(String s) {
						boolean success=false;
						if(s!=null){
							Result result = ResultUtils.getResultFromJson(s, Group.class);
							if(result!=null&&result.isRetMsg()){
									success=true;
							}
						}
						createSuccess(success);
					}

					@Override
					public void onError(String error) {
						createSuccess(false);
					}
				});
	}
	//// FIXME: 2017/4/11 返回值写成默认的空,所以报空指针
	private String getMember(String[] members){
		String s="";
		for(String str:members){
			s+=str+",";
		}
		return s;
	}
	private void createSuccess(final boolean success) {
		runOnUiThread(new Runnable() {
			public void run() {
				progressDialog.dismiss();
				if(success){
				setResult(RESULT_OK);
				finish();
				}else {
					Toast.makeText(NewGroupActivity.this,R.string.Failed_to_create_groups, Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	private void showDialog() {
		String st1 = getResources().getString(R.string.Is_to_create_a_group_chat);
		//如果返回成功,显示一个进程框
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(st1);
		progressDialog.setCanceledOnTouchOutside(false);
		progressDialog.show();
	}

	public void back(View view) {
		finish();
	}
}
