package com.zst.ynh.widget.person.certification.mohe;

import android.graphics.Color;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.View;

import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.zst.ynh.R;
import com.zst.ynh.config.ApiUrl;
import com.zst.ynh.config.ArouterUtil;
import com.zst.ynh.config.BundleKey;
import com.zst.ynh.config.SPkey;
import com.zst.ynh_base.mvp.view.BaseActivity;
import com.zst.ynh_base.net.BaseParams;
import com.zst.ynh_base.net.HttpManager;
import com.zst.ynh_base.util.Layout;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.fraudmetrix.octopus.aspirit.bean.OctopusParam;
import cn.fraudmetrix.octopus.aspirit.main.OctopusManager;
import cn.fraudmetrix.octopus.aspirit.main.OctopusTaskCallBack;

@Route(path = ArouterUtil.MAGIC_BOX)
@Layout(R.layout.activity_magic_box_layout)
public class MagicBoxActivity extends BaseActivity {
    @BindView(R.id.group_layout)
    ConstraintLayout groupLayout;
    private HttpManager httpManager;
    @Autowired(name = BundleKey.IS_FROM_INCERTIFICATION)
    boolean isFromInCertification = false;

    public void skipToMain() {
        if (!isFromInCertification)
            ARouter.getInstance().build(ArouterUtil.MAIN).withString(BundleKey.MAIN_SELECTED,BundleKey.MAIN_LOAN).navigation();
        else
            finish();
    }

    @Override
    public void onRetry() {

    }

    @Override
    public void initView() {
        mTitleBar.setVisibility(View.GONE);
        httpManager = new HttpManager(this);

        OctopusManager.getInstance().setNavImgResId(R.drawable.system_back);//设置导航返回图标

        OctopusManager.getInstance().setPrimaryColorResId(R.color.white);//设置导航背景

        OctopusManager.getInstance().setTitleColorResId(Color.BLACK);//设置title字体颜色

        OctopusManager.getInstance().setTitleSize(14);//sp 设置title字体大小
        OctopusManager.getInstance().setShowWarnDialog(true);//强制对话框是否显示
        OctopusManager.getInstance().setStatusBarBg(R.color.white);//设置状态栏背景


        OctopusParam param = new OctopusParam();
        OctopusManager.getInstance().getChannel(this, "005003", param, new OctopusTaskCallBack() {
            @Override
            public void onCallBack(int code, final String taskId) {
                if (code == 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            saveMagicBox(taskId, SPUtils.getInstance().getString(SPkey.UID));
                        }
                    });


                }
            }
        });
    }

    private void saveMagicBox(String taskId, String userId) {
        Map<String, String> map = BaseParams.getBaseParams();
        map.put("taskId", taskId);
        map.put("userId", userId);
        httpManager.executePostString(ApiUrl.SAVE_MOHE, map, new HttpManager.ResponseCallBack<String>() {

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(int code, String errorMSG) {
                ToastErrorMessage(errorMSG);
            }

            @Override
            public void onSuccess(String response) {
                skipToMain();
            }
        });
    }

    public void ToastErrorMessage(String msg) {
        ToastUtils.showShort(msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (groupLayout.getChildCount() == 0) {
            finish();
        }
    }

}
