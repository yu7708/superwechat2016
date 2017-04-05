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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.widget.EaseAlertDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatDemoHelper;
import cn.ucai.superwechat.db.IUserModel;
import cn.ucai.superwechat.db.OnCompleteListener;
import cn.ucai.superwechat.db.UserModel;
import cn.ucai.superwechat.utils.MFGT;
import cn.ucai.superwechat.utils.Result;
import cn.ucai.superwechat.utils.ResultUtils;

public class AddContactActivity extends BaseActivity {
    private static final String TAG = "AddContactActivity";
    @BindView(R.id.ll_user)
    RelativeLayout llUser;
    @BindView(R.id.search_name)
    TextView searchName;
    private EditText editText;
    private RelativeLayout searchedUserLayout;
    private TextView nameText;
    private Button searchBtn;
    private String toAddUsername;
    private ProgressDialog progressDialog;
    IUserModel mModel;
    User user=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.em_activity_add_contact);
        ButterKnife.bind(this);
        TextView mTextView = (TextView) findViewById(R.id.add_list_friends);

        editText = (EditText) findViewById(R.id.edit_note);
        String strAdd = getResources().getString(R.string.add_friend);
        mTextView.setText(strAdd);
        String strUserName = getResources().getString(R.string.user_name);
        editText.setHint(strUserName);
        searchedUserLayout = (RelativeLayout) findViewById(R.id.ll_user);
        nameText = (TextView) findViewById(R.id.name);
        searchBtn = (Button) findViewById(R.id.search);
        mModel = new UserModel();
    }


    /**
     * search contact
     *
     * @param v
     */
    public void searchContact(View v) {
        //搜索查询
        final String name = editText.getText().toString();
        String saveText = searchBtn.getText().toString();

        if (getString(R.string.button_search).equals(saveText)) {
            toAddUsername = name;
            if (TextUtils.isEmpty(name)) {
                new EaseAlertDialog(this, R.string.Please_enter_a_username).show();
                return;
            }

            // TODO you can search the user from your app server here.

            //show the userame and add button if user exist
            searchedUserLayout.setVisibility(View.VISIBLE);
            //// FIXME: 2017/4/5
            Toast.makeText(this, "username" + toAddUsername, Toast.LENGTH_SHORT).show();
            //有此人,就网络请求下载

            //这个属性的名字就是查找的名字
            nameText.setText(toAddUsername);
            //// FIXME: 2017/4/5 为了好看,还加个对话框
            dialog();
            searchUser();
        }
    }

    private void dialog() {
        progressDialog=new ProgressDialog(AddContactActivity.this);
        //搜索的时候显示正在搜索联系人
        progressDialog.setMessage(getString(R.string.addcontact_search));
        progressDialog.show();
    }

    private void searchUser() {
        mModel.loadUserInfo(AddContactActivity.this, toAddUsername,
                new OnCompleteListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        boolean success = false;
                        if (s != null) {
                            Result result = ResultUtils.getResultFromJson(s, User.class);
                            if (result != null && result.isRetMsg()) {
                                user= (User) result.getRetData();
                                success = true;
                            }
                        }
                        showResult(success);
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(AddContactActivity.this, "查找用户失败", Toast.LENGTH_SHORT).show();
                        showResult(false);
                    }
                });
    }

    private void showResult(boolean success) {
        progressDialog.dismiss();
        searchName.setVisibility(success ? View.GONE : View.VISIBLE);
        if (success) {
            //跳转到详情界面
            //为什么生成的Activity没注册前,怎么输入都是有帐号,而且判断的显示也是相反的
            MFGT.gotoFriend(AddContactActivity.this,user);
        }
    }

    /**
     * add contact
     *
     * @param
     */
    /*public void addContact(View view) {
            //如果添加的人等于自己,显示不能添加你自己
        if (EMClient.getInstance().getCurrentUser().equals(nameText.getText().toString())) {
            new EaseAlertDialog(this, R.string.not_add_myself).show();
            return;
        }
            //获得数据库里的联系人列表
        if (SuperWeChatDemoHelper.getInstance().getContactList().containsKey(nameText.getText().toString())) {
            //let the user know the contact already in your contact list
            if (EMClient.getInstance().contactManager().getBlackListUsernames().contains(nameText.getText().toString())) {
                new EaseAlertDialog(this, R.string.user_already_in_contactlist).show();
                return;
            }
            new EaseAlertDialog(this, R.string.This_user_is_already_your_friend).show();
            return;
        }

        progressDialog = new ProgressDialog(this);
        String stri = getResources().getString(R.string.Is_sending_a_request);
        progressDialog.setMessage(stri);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        new Thread(new Runnable() {
            public void run() {

                try {
                    //demo use a hardcode reason here, you need let user to input if you like
                    String s = getResources().getString(R.string.Add_a_friend);
                    EMClient.getInstance().contactManager().addContact(toAddUsername, s);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            progressDialog.dismiss();
                            String s1 = getResources().getString(R.string.send_successful);
                            Toast.makeText(getApplicationContext(), s1, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            progressDialog.dismiss();
                            String s2 = getResources().getString(R.string.Request_add_buddy_failure);
                            Toast.makeText(getApplicationContext(), s2 + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }*/

    public void back(View v) {
        finish();
    }
}
