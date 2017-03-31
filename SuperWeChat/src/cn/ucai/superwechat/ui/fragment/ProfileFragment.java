package cn.ucai.superwechat.ui.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.easemob.redpacketui.utils.RedPacketUtil;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.utils.EaseUserUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.superwechat.Constant;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatDemoHelper;
import cn.ucai.superwechat.ui.MainActivity;
import cn.ucai.superwechat.ui.UserProfileActivity;
import cn.ucai.superwechat.utils.MFGT;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment implements View.OnClickListener{
    private static final String TAG = "ProfileFragment";
    @BindView(R.id.iv_profile_avatar)
    ImageView ivProfileAvatar;
    @BindView(R.id.tv_profile_nickname)
    TextView tvProfileNickname;
    @BindView(R.id.tv_profile_username)
    TextView tvProfileUsername;
    @BindView(R.id.layout_profile_view)
    RelativeLayout layoutProfileView;
    @BindView(R.id.tv_profile_album)
    TextView tvProfileAlbum;
    @BindView(R.id.tv_profile_collect)
    TextView tvProfileCollect;
    @BindView(R.id.tv_profile_money)
    TextView tvProfileMoney;
    @BindView(R.id.tv_profile_smail)
    TextView tvProfileSmail;
    @BindView(R.id.tv_profile_settings)
    TextView tvProfileSettings;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_profile, container, false);
        ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initData();
        setListener();
    }

    private void setListener() {
        tvProfileMoney.setOnClickListener(this);
        layoutProfileView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
    switch (v.getId()){
        case R.id.tv_profile_money:
            RedPacketUtil.startChangeActivity(getContext());
            break;
        case R.id.layout_profile_view:
            MFGT.gotoUserInfo(getActivity(),true,EMClient.getInstance().getCurrentUser());
            break;
    }
    }

    private void initData() {
        //得到名字和对象
        String username = EMClient.getInstance().getCurrentUser();
        tvProfileUsername.setText(username);
        EaseUserUtils.setAppUserNick(username,tvProfileNickname);
        EaseUserUtils.setAppUserAvatar(getContext(),username,ivProfileAvatar);
    }
    @OnClick(R.id.tv_profile_settings)
    public void setting(){
        MFGT.gotoSetting(getActivity());
    }
    //// FIXME: 2017/3/31 改后的setting里拿出的
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (((MainActivity) getActivity()).isConflict) {
            outState.putBoolean("isConflict", true);
        } else if (((MainActivity) getActivity()).getCurrentAccountRemoved()) {
            outState.putBoolean(Constant.ACCOUNT_REMOVED, true);
        }
    }
    //---f
}
