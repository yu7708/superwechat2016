/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ucai.superwechat.ui;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatDemoHelper;
import cn.ucai.superwechat.db.IUserModel;
import cn.ucai.superwechat.db.OnCompleteListener;
import cn.ucai.superwechat.db.UserModel;
import cn.ucai.superwechat.utils.CommonUtils;
import cn.ucai.superwechat.utils.MD5;
import cn.ucai.superwechat.utils.MFGT;
import cn.ucai.superwechat.utils.Result;
import cn.ucai.superwechat.utils.ResultUtils;

/**
 * register screen
 */
public class RegisterActivity extends BaseActivity {
    private static final String TAG = "RegisterActivity";
    @BindView(R.id.txt_title)
    TextView txtTitle;
    @BindView(R.id.username)
    EditText etUserName;
    @BindView(R.id.nick)
    EditText etNick;
    @BindView(R.id.password)
    EditText etPassword;
    @BindView(R.id.confirm_password)
    EditText etConfirmPassword;
    ProgressDialog pd;
    String username, nickname, password;

    IUserModel mModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.em_activity_register);
        ButterKnife.bind(this);
        mModel=new UserModel();
        initView();
    }

    private void initView() {
        txtTitle.setVisibility(View.VISIBLE);
        txtTitle.setText(R.string.register);
    }

    public boolean checkInput() {
        username = etUserName.getText().toString().trim();
        nickname = etNick.getText().toString().trim();
        password = etPassword.getText().toString().trim();
        String confirm_pwd = etConfirmPassword.getText().toString().trim();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, getResources().getString(R.string.User_name_cannot_be_empty), Toast.LENGTH_SHORT).show();
            etUserName.requestFocus();
            return false;
        } else if (TextUtils.isEmpty(nickname)) {
            Toast.makeText(this, getResources().getString(R.string.toast_nick_not_isnull), Toast.LENGTH_SHORT).show();
            etPassword.requestFocus();
            return false;
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, getResources().getString(R.string.Password_cannot_be_empty), Toast.LENGTH_SHORT).show();
            etPassword.requestFocus();
            return false;
        } else if (TextUtils.isEmpty(confirm_pwd)) {
            Toast.makeText(this, getResources().getString(R.string.Confirm_password_cannot_be_empty), Toast.LENGTH_SHORT).show();
            etConfirmPassword.requestFocus();
            return false;
        } else if (!password.equals(confirm_pwd)) {
            Toast.makeText(this, getResources().getString(R.string.Two_input_password), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showDialog() {
        pd = new ProgressDialog(this);
        pd.setMessage(getResources().getString(R.string.Is_the_registered));
        pd.show();
    }

    private void registerEMServer() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    // call method in SDK
                    EMClient.getInstance().createAccount(username, MD5.getMessageDigest(password));
                    //环信持有实例化
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (!RegisterActivity.this.isFinishing())
                                pd.dismiss();
                            // save current user
                            SuperWeChatDemoHelper.getInstance().setCurrentUserName(username);
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registered_successfully), Toast.LENGTH_SHORT).show();
                            MFGT.gotoLogin(RegisterActivity.this);
                        }
                    });
                } catch (final HyphenateException e) {
                    //处理环信的异常
                    unregister();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (!RegisterActivity.this.isFinishing())
                                pd.dismiss();
                            int errorCode = e.getErrorCode();
                            if (errorCode == EMError.NETWORK_ERROR) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.network_anomalies), Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.USER_ALREADY_EXIST) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.User_already_exists), Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.USER_AUTHENTICATION_FAILED) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.registration_failed_without_permission), Toast.LENGTH_SHORT).show();
                            } else if (errorCode == EMError.USER_ILLEGAL_ARGUMENT) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.illegal_user_name), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registration_failed), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }

        }).start();
    }
    private void unregister() {
        mModel.unregister(RegisterActivity.this, username,
                new OnCompleteListener<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Log.e(TAG,"unregister,result="+result);
                    }

                    @Override
                    public void onError(String error) {

                    }
                });
    }
    public void back(View view) {
        finish();
    }

    @OnClick(R.id.img_back)
    public void onBack() {
        MFGT.finish(RegisterActivity.this);
    }

    @OnClick(R.id.btn_register)
    public void onRegister() {
        registerAppServer();//注册一个自己的服务器
    }

    private void registerAppServer() {
        //先判断注册填写是否正确
        if(checkInput()){
            showDialog();
            Log.e(TAG,"MD5--password="+MD5.getMessageDigest(password));
            mModel.register(RegisterActivity.this, username, nickname, MD5.getMessageDigest(password),
                    new OnCompleteListener<String>() {
                        @Override
                        public void onSuccess(String s) {
                            boolean success=false;
                            if(s!=null){
                                //要解析才能拿到返回的json数据
                                Result result = ResultUtils.getResultFromJson(s, String.class);
                                if(result!=null){
                                    if(result.isRetMsg()){
                                        success=true;
                                        //拿到数据再推送到环信
                                        registerEMServer();
                                    }else if(result.getRetCode()== I.MSG_REGISTER_USERNAME_EXISTS){
                                        CommonUtils.showShortToast(R.string.User_already_exists);
                                    }else{
                                        CommonUtils.showShortToast(R.string.Registration_failed);
                                    }
                                }
                                //然后考虑注册失败就得返回，避免多加，定标志位
                                if(!success){
                                    pd.dismiss();
                                }
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG,"registerAppServer,onError="+error);
                            pd.dismiss();
                            CommonUtils.showShortToast(R.string.Registration_failed);
                        }
                    });
        }
    }
}
