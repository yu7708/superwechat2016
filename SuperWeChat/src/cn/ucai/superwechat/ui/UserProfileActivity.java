package cn.ucai.superwechat.ui;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.utils.EaseUserUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.Inflater;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatDemoHelper;
import cn.ucai.superwechat.utils.L;

public class UserProfileActivity extends BaseActivity implements OnClickListener {
    private static final String TAG = "UserProfileActivity";
    private static final int REQUESTCODE_PICK = 1;
    private static final int REQUESTCODE_CUTTING = 2;
    @BindView(R.id.title)
    RelativeLayout title;
    @BindView(R.id.user_head_avatar)
    ImageView userHeadAvatar;
    @BindView(R.id.user_head_headphoto_update)
    ImageView userHeadHeadphotoUpdate;
    //图像旁的用户名
    @BindView(R.id.user_username)
    TextView userUsername;
    //图像的昵称
    @BindView(R.id.user_nickname)
    TextView userNickname;
    @BindView(R.id.ic_right_arrow)
    ImageView icRightArrow;
    @BindView(R.id.rl_nickname)
    RelativeLayout rlNickname;
    @BindView(R.id.tv_userinfo_name)
    TextView tvUserinfoName;
    @BindView(R.id.layout_userinfo_name)
    LinearLayout layoutUserinfoName;
    @BindView(R.id.tv_userinfo_qrcode)
    TextView tvUserinfoQrcode;
    @BindView(R.id.tv_userinfo_address)
    TextView tvUserinfoAddress;
    @BindView(R.id.tv_userinfo_sex)
    TextView tvUserinfoSex;
    @BindView(R.id.layout_userinfo_sex)
    LinearLayout layoutUserinfoSex;
    @BindView(R.id.tv_userinfo_area)
    TextView tvUserinfoArea;
    @BindView(R.id.layout_userinfo_area)
    LinearLayout layoutUserinfoArea;
    @BindView(R.id.tv_userinfo_sign)
    TextView tvUserinfoSign;
    @BindView(R.id.layout_userinfo_sign)
    LinearLayout layoutUserinfoSign;
    private ImageView headAvatar;
    private ImageView headPhotoUpdate;
    private ImageView iconRightArrow;
    private TextView tvNickName;
    private TextView tvUsername;
    private ProgressDialog dialog;
    private RelativeLayout rlNickName;
    User user=null;
    updateReceiver mReceiver;
    updateAvatarReceiver mAvatarReceiver;
    String avatarName;
    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.em_activity_user_profile);
        ButterKnife.bind(this);
        initView();
        initData();
        initListener();
    }
    //// FIXME: 2017/4/1 添加数据
    private void initData() {
        user = SuperWeChatDemoHelper.getInstance().getUserProfileManager().getCurrentAppUserInfo();
        if(user==null){
            finish();
            return;
        }else{
            showUserInfo();
        }
    }

    private void showUserInfo() {
        userUsername.setText(user.getMUserName());
        EaseUserUtils.setAppUserNick(user.getMUserName(),userNickname);
        EaseUserUtils.setAppUserAvatar(UserProfileActivity.this,user.getMUserName(),userHeadAvatar);
    }

    private void initView() {
        headAvatar = (ImageView) findViewById(R.id.user_head_avatar);
        headPhotoUpdate = (ImageView) findViewById(R.id.user_head_headphoto_update);
        tvUsername = (TextView) findViewById(R.id.user_username);
        tvNickName = (TextView) findViewById(R.id.user_nickname);
        rlNickName = (RelativeLayout) findViewById(R.id.rl_nickname);
        iconRightArrow = (ImageView) findViewById(R.id.ic_right_arrow);
    }

    private void initListener() {
        //更新完后要注册广播,就这三句话.
        mReceiver=new updateReceiver();
        IntentFilter filter=new IntentFilter(I.REQUEST_UPDATE_USER_NICK);
        registerReceiver(mReceiver,filter);

        mAvatarReceiver=new updateAvatarReceiver();
        IntentFilter filter1=new IntentFilter(I.REQUEST_UPDATE_AVATAR);
        registerReceiver(mAvatarReceiver,filter1);




        Intent intent = getIntent();
        String username = intent.getStringExtra("username");
        boolean enableUpdate = intent.getBooleanExtra("setting", false);
        if (enableUpdate) {
            headPhotoUpdate.setVisibility(View.VISIBLE);
            iconRightArrow.setVisibility(View.VISIBLE);
            rlNickName.setOnClickListener(this);
            headAvatar.setOnClickListener(this);
        } else {
            headPhotoUpdate.setVisibility(View.GONE);
            iconRightArrow.setVisibility(View.INVISIBLE);
        }
        if (username != null) {
            if (username.equals(EMClient.getInstance().getCurrentUser())) {
                //// FIXME: 2017/4/1
                tvUserinfoName.setText(EMClient.getInstance().getCurrentUser());
                //--f
                tvUsername.setText(EMClient.getInstance().getCurrentUser());
                EaseUserUtils.setUserNick(username, tvNickName);
                EaseUserUtils.setUserAvatar(this, username, headAvatar);
            } else {
                //// FIXME: 2017/4/1
                tvUserinfoName.setText(username);
                //--f
                tvUsername.setText(username);
                EaseUserUtils.setUserNick(username, tvNickName);
                EaseUserUtils.setUserAvatar(this, username, headAvatar);
                asyncFetchUserInfo(username);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.user_head_avatar:
                uploadHeadPhoto();
                break;
            case R.id.rl_nickname:
                final EditText editText = new EditText(this);
                //// FIXME: 2017/4/1 编辑昵称时显示当前昵称,并且全选
                editText.setText(user.getMUserNick());
                editText.setSelectAllOnFocus(true);
                //--f
                new Builder(this).setTitle(R.string.setting_nickname).setIcon(android.R.drawable.ic_dialog_info).setView(editText)
                        .setPositiveButton(R.string.dl_ok, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String nickString = editText.getText().toString();
                                if (TextUtils.isEmpty(nickString)) {
                                    Toast.makeText(UserProfileActivity.this, getString(R.string.toast_nick_not_isnull), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                updateRemoteNick(nickString);//更新到远程服务器
                            }
                        }).setNegativeButton(R.string.dl_cancel, null).show();
                break;
            default:
                break;
        }

    }

    public void asyncFetchUserInfo(String username) {
        SuperWeChatDemoHelper.getInstance().getUserProfileManager().asyncGetUserInfo(username, new EMValueCallBack<EaseUser>() {

            @Override
            public void onSuccess(EaseUser user) {
                if (user != null) {
                    SuperWeChatDemoHelper.getInstance().saveContact(user);
                    if (isFinishing()) {
                        return;
                    }
                    tvNickName.setText(user.getNick());
                    if (!TextUtils.isEmpty(user.getAvatar())) {
                        Glide.with(UserProfileActivity.this).load(user.getAvatar()).placeholder(R.drawable.em_default_avatar).into(headAvatar);
                    } else {
                        Glide.with(UserProfileActivity.this).load(R.drawable.em_default_avatar).into(headAvatar);
                    }
                }
            }

            @Override
            public void onError(int error, String errorMsg) {
            }
        });
    }

    //// TODO: 2017/4/4   更新图片
    private void uploadHeadPhoto() {
        Builder builder = new Builder(this);
        builder.setTitle(R.string.dl_title_upload_photo);
        builder.setItems(new String[]{getString(R.string.dl_msg_take_photo), getString(R.string.dl_msg_local_upload)},
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        switch (which) {
                            case 0:
                                //拍照上传
                                Toast.makeText(UserProfileActivity.this, getString(R.string.toast_no_support),
                                        Toast.LENGTH_SHORT).show();
                                break;
                            case 1:
                                //本地上传
                                Intent pickIntent = new Intent(Intent.ACTION_PICK, null);
                                pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                                startActivityForResult(pickIntent, REQUESTCODE_PICK);
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


    private void updateRemoteNick(final String nickName) {
        dialog = ProgressDialog.show(this, getString(R.string.dl_update_nick), getString(R.string.dl_waiting));
        new Thread(new Runnable() {

            @Override
            public void run() {
                //拿到当前昵称
                boolean updatenick = SuperWeChatDemoHelper.getInstance().getUserProfileManager().updateCurrentUserNickName(nickName);
                if (UserProfileActivity.this.isFinishing()) {
                    return;
                }
            }
        }).start();
    }
    private void updateNickView(Boolean success){
        //这里设置了一个返回值,根据返回值判段修改成功与否
        if (!success) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatenick_fail), Toast.LENGTH_SHORT)
                            .show();
                    dialog.dismiss();
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                    Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatenick_success), Toast.LENGTH_SHORT)
                            .show();
                    //更新成功后,他的重新显示昵称,先调用已经保存在内存中的,然后显示图片
                    user=SuperWeChatDemoHelper.getInstance().getUserProfileManager().getCurrentAppUserInfo();
                    tvNickName.setText(user.getMUserNick());
                }
            });
        }
    }
    //写这个就是用来接收你所写的startActivityForResult的请求码
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            //选择图片
            case REQUESTCODE_PICK:
                //如果数据为空,或者数据里的数据为空,则返回
                if (data == null || data.getData() == null) {
                    return;
                }
                //这是选择,执行这方法的最后一步就是请求裁剪
                startPhotoZoom(data.getData());
                break;
            //请求_代码_切割
            case REQUESTCODE_CUTTING:
                if (data != null) {
                    setPicToView(data);
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

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
        startActivityForResult(intent, REQUESTCODE_CUTTING);
    }

    /**
     * save the picture data
     *
     * @param picdata
     */
    private void setPicToView(Intent picdata) {
        Bundle extras = picdata.getExtras();
        if (extras != null) {
            Bitmap photo = extras.getParcelable("data");
            Drawable drawable = new BitmapDrawable(getResources(), photo);
            headAvatar.setImageDrawable(drawable);
           // uploadUserAvatar(Bitmap2Bytes(photo));
            //// FIXME: 2017/4/4  仿造上面这个方法,我们现在传的参数为file
            uploadAppUserAvatar(saveBitmapFile(photo));
        }
    }

    private void uploadAppUserAvatar(File file) {
        dialog = ProgressDialog.show(this, getString(R.string.dl_update_photo), getString(R.string.dl_waiting));
        SuperWeChatDemoHelper.getInstance().getUserProfileManager().uploadUserAvatar(file);
        dialog.show();
    }

    private String getAvatarName() {
        avatarName = user.getMUserName()+ System.currentTimeMillis();
        //得到文件名为当前的用户名加上系统的时间戳
        L.e(TAG,"avatarname="+avatarName);
        return avatarName;
    }
    /**
     * 返回头像保存在sd卡的位置:
     * Android/data/cn.ucai.superwechat/files/pictures/user_avatar
     * @param context
     * @param path
     * @return
     */
    public static String getAvatarPath(Context context, String path){
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File folder = new File(dir,path);
        if(!folder.exists()){
            folder.mkdir();
        }
        return folder.getAbsolutePath();
    }

    private File saveBitmapFile(Bitmap bitmap) {
        if (bitmap != null) {
            //将定义的bitmap进行判断,有数据的话,先拿到路径,然后调用文件,输出bitmap
            String imagePath = getAvatarPath(UserProfileActivity.this,I.AVATAR_TYPE)+"/"+getAvatarName()+".jpg";
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
            return file;
        }
        return null;
    }

  /*  private void uploadUserAvatar(final byte[] data) {
        dialog = ProgressDialog.show(this, getString(R.string.dl_update_photo), getString(R.string.dl_waiting));
        new Thread(new Runnable() {

            @Override
            public void run() {
                final String avatarUrl = SuperWeChatDemoHelper.getInstance().getUserProfileManager().uploadUserAvatar(data);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        if (avatarUrl != null) {
                            Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatephoto_success),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatephoto_fail),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
        dialog.show();
    }*/

    //暂去,这是原生的
    /*public byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }*/

    class updateAvatarReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            //拿到发送的广播,manager中的是putExtra,这边是getExtra
            boolean success = intent.getBooleanExtra(I.Avatar.UPDATE_TIME, false);
            //成功后更新
            updateAvatarView(success);
            //// TODO: 2017/4/4 写完还得注册
        }
    }
    private void updateAvatarView(boolean success) {
        dialog.dismiss();
        if (success) {
            //更新头像成功
            Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatephoto_success),
                    Toast.LENGTH_SHORT).show();
            //拿到数据,更新头像
            user=SuperWeChatDemoHelper.getInstance().getUserProfileManager().getCurrentAppUserInfo();
            EaseUserUtils.setAppUserAvatar(UserProfileActivity.this,user.getMUserName(),userHeadAvatar);
        } else {
            //更新头像失败
            Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatephoto_fail),
                    Toast.LENGTH_SHORT).show();
        }
    }
    class updateReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(I.User.NICK, false);
            //成功后更新
            updateNickView(success);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        initData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mReceiver!=null){
            //取消注册
            unregisterReceiver(mReceiver);
        }
        if(mAvatarReceiver!=null){
            unregisterReceiver(mAvatarReceiver);
        }
    }
}
