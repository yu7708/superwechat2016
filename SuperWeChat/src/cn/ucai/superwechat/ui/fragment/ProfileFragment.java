package cn.ucai.superwechat.ui.fragment;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.utils.EaseUserUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatDemoHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

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
    }

    private void initData() {
        //得到名字和对象
        String username = EMClient.getInstance().getCurrentUser();
        tvProfileUsername.setText(username);
        EaseUserUtils.setAppUserNick(username,tvProfileNickname);
        EaseUserUtils.setAppUserAvatar(getContext(),username,ivProfileAvatar);
    }
}
