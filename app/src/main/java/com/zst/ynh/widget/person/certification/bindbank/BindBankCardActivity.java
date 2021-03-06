package com.zst.ynh.widget.person.certification.bindbank;

import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.blankj.utilcode.util.KeyboardUtils;
import com.blankj.utilcode.util.RegexUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.zst.ynh.R;
import com.zst.ynh.base.UMBaseActivity;
import com.zst.ynh.bean.BankBean;
import com.zst.ynh.bean.DepositOpenInfoVBean;
import com.zst.ynh.config.ArouterUtil;
import com.zst.ynh.config.BundleKey;
import com.zst.ynh.config.Constant;
import com.zst.ynh.config.SPkey;
import com.zst.ynh.config.UMClicEventID;
import com.zst.ynh.config.UMClickEvent;
import com.zst.ynh.event.StringEvent;
import com.zst.ynh.utils.DialogUtil;
import com.zst.ynh.utils.KeyboardUtil;
import com.zst.ynh.view.BankListDialog;
import com.zst.ynh.view.CardEditText;
import com.zst.ynh.view.keyboard.KeyboardNumberUtil;
import com.zst.ynh_base.mvp.view.BaseActivity;
import com.zst.ynh_base.util.Layout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.OnClick;

/*
 * 绑定银行卡
 * */
@Route(path = ArouterUtil.BIND_BANK_CARD)
@Layout(R.layout.activity_bind_bank_card_layout)
public class BindBankCardActivity extends UMBaseActivity implements IBindBankCardView {
    @BindView(R.id.tv_user_name)
    TextView tvUserName;
    @BindView(R.id.tv_bank_name)
    TextView tvBankName;
    @BindView(R.id.et_bankcard_num)
    CardEditText etBankcardNum;
    @BindView(R.id.et_phone_num)
    EditText etPhoneNum;
    @BindView(R.id.et_verify_code)
    EditText etVerifyCode;
    @BindView(R.id.tv_send_code)
    TextView tvSendCode;
    @BindView(R.id.btn_submit)
    Button btnSubmit;
    @BindView(R.id.llCustomerKb)
    View llCustomerKb;

    @Autowired(name = BundleKey.ISCHANGE)
    boolean isChange;
    private BindBankCardPresent bindBankCardPresent;
    private CountDownTimer timer;
    private BankListDialog bankListDialog;
    private BankBean bankBean;
    //这是判断是否从认证页面来的 true:下方按钮显示下一步； false:下方按钮显示保存
    private boolean isFromToCertification;
    private int bankID;

    @Override
    protected boolean isUseEventBus() {
        return true;
    }

    @Override
    public void sendSMSSuccess() {
        tvSendCode.setEnabled(false);
        ToastUtils.showShort("验证码已发送");
        countTime();
    }


    @Override
    public void loadContent() {
        loadContentView();
    }

    @Override
    public void loadLoading() {
        loadLoadingView();
    }

    @Override
    public void loadError() {
        loadErrorView();
    }

    @Override
    public void getBankListData(BankBean response) {
        this.bankBean = response;
    }

    @Override
    public void addBankCardSuccess() {
        UMClickEvent.getInstance().onClick(this,UMClicEventID.UM_EVENT_BANK_INFORMATION_NEXT,"绑定银行卡下一步");
        if (isFromToCertification) {
            ARouter.getInstance().build(ArouterUtil.SIMPLE_WEB).withString(BundleKey.URL, Constant.getTargetUrl()).withBoolean(BundleKey.WEB_SET_SESSION, true).navigation();
            finish();
        } else {
            ToastUtils.showShort("绑定银行卡成功");
            finish();
        }
    }

    /**
     * 选择的赋值
     *
     * @param messageEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(StringEvent messageEvent) {
        if (!TextUtils.isEmpty(messageEvent.getMessage())) {
            tvBankName.setText(messageEvent.getMessage());
            bankID = messageEvent.getValue();
        }

    }

    @Override
    public void onRetry() {
        bindBankCardPresent.getBankList();
    }

    @Override
    public void initView() {

        ARouter.getInstance().inject(this);
        mTitleBar.setTitle("绑定银行卡");
        bindBankCardPresent = new BindBankCardPresent();
        bindBankCardPresent.attach(this);
        bindBankCardPresent.getBankList();
        tvUserName.setText(SPUtils.getInstance().getString(SPkey.REAL_NAME));
        isFromToCertification = Constant.isIsStep();
        if (isFromToCertification) {
            btnSubmit.setText("下一步");
        }
        KeyboardUtils.hideSoftInput(this);
        etPhoneNum.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (etPhoneNum != null)
                        etPhoneNum.requestFocus();
                    KeyboardUtil.showKeyboard(BindBankCardActivity.this, llCustomerKb, KeyboardNumberUtil.CUSTOMER_KEYBOARD_TYPE.NUMBER, etPhoneNum);
                } else {
                    if (etPhoneNum != null)
                        etPhoneNum.clearFocus();
                    KeyboardUtil.hideKeyboard();
                }
            }
        });

        etBankcardNum.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (etBankcardNum != null)
                        etBankcardNum.requestFocus();
                    KeyboardUtil.showKeyboard(BindBankCardActivity.this, llCustomerKb, KeyboardNumberUtil.CUSTOMER_KEYBOARD_TYPE.NUMBER, etBankcardNum);
                } else {
                    if (etBankcardNum != null)
                        etBankcardNum.clearFocus();
                    KeyboardUtil.hideKeyboard();
                }
            }
        });

    }

    @Override
    public void showLoading() {
        showLoadingView();
    }

    @Override
    public void hideLoading() {
        hideLoadingView();
    }

    @Override
    public void ToastErrorMessage(String msg) {
        ToastUtils.showShort(msg);
    }


    @OnClick({R.id.tv_send_code, R.id.btn_submit, R.id.tv_bank_name,R.id.et_phone_num,R.id.et_bankcard_num})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_send_code:
                if (TextUtils.isEmpty(etPhoneNum.getText().toString().trim())) {
                    ToastUtils.showShort("手机号不能为空");
                } else if (!RegexUtils.isMobileSimple(etPhoneNum.getText().toString().trim())) {
                    ToastUtils.showShort("请输入正确的手机号");
                } else {
                    bindBankCardPresent.sendBankSMS(etPhoneNum.getText().toString().trim(), bankID + "");
                }
                break;
            case R.id.btn_submit:

                if (TextUtils.isEmpty(etPhoneNum.getText().toString().trim())) {
                    ToastUtils.showShort("手机号不能为空");
                } else if (!RegexUtils.isMobileSimple(etPhoneNum.getText().toString().trim())) {
                    ToastUtils.showShort("请输入正确的手机号");
                } else if (TextUtils.isEmpty(etBankcardNum.getText().toString().trim())) {
                    ToastUtils.showShort("银行卡号不能为空");
                } else if (TextUtils.isEmpty(tvBankName.getText().toString().trim())) {
                    ToastUtils.showShort("请选择银行");
                } else if (TextUtils.isEmpty(etVerifyCode.getText().toString().trim())) {
                    ToastUtils.showShort("验证码不能为空");
                } else {
                    if (!isChange)
                        bindBankCardPresent.addBankCard(bankID + "", etBankcardNum.getText().toString().trim().replaceAll(" ", ""),
                                etPhoneNum.getText().toString().trim(), etVerifyCode.getText().toString().trim());
                    else
                        bindBankCardPresent.changeBankCard(bankID + "", etBankcardNum.getText().toString().trim().replaceAll(" ", ""),
                                etPhoneNum.getText().toString().trim(), etVerifyCode.getText().toString().trim());
                }
                break;
            case R.id.tv_bank_name:
                bankListDialog = new BankListDialog.Builder(this, bankBean.item, bankBean.tips).create();
                bankListDialog.show();
                break;

            case R.id.et_phone_num:
                if (!KeyboardUtil.isKeyboardShow()) {
                    KeyboardUtil.showKeyboard(this, llCustomerKb, KeyboardNumberUtil.CUSTOMER_KEYBOARD_TYPE.NUMBER, etPhoneNum);
                }
                break;
            case R.id.et_bankcard_num:
                if (!KeyboardUtil.isKeyboardShow()) {
                    KeyboardUtil.showKeyboard(this, llCustomerKb, KeyboardNumberUtil.CUSTOMER_KEYBOARD_TYPE.NUMBER, etBankcardNum);
                }
                break;

        }
    }

    /**
     * 倒计时
     */
    private void countTime() {
        if(timer!=null) {
            timer.cancel(); //防止new出多个导致时间跳动加速
            timer=null;
        }
        timer = new CountDownTimer(60 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (tvSendCode != null) {
                    tvSendCode.setText(millisUntilFinished / 1000 + "秒");
                }
            }

            @Override
            public void onFinish() {
                if (tvSendCode != null) {
                    tvSendCode.setEnabled(true);
                    tvSendCode.setText("重新发送");
                }
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bindBankCardPresent != null)
            bindBankCardPresent.detach();
        DialogUtil.hideDialog(bankListDialog);
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
