package com.zst.ynh.widget.repayment.repaymentfragment;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.alibaba.android.arouter.launcher.ARouter;
import com.blankj.utilcode.constant.TimeConstants;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.constant.RefreshState;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.zst.ynh.R;
import com.zst.ynh.adapter.RepayOrderAdapter;
import com.zst.ynh.bean.CalendarBean;
import com.zst.ynh.bean.OtherPlatformRepayInfoBean;
import com.zst.ynh.bean.PaymentStyleBean;
import com.zst.ynh.bean.HistoryOrderInfoBean;
import com.zst.ynh.bean.RepayItemBean;
import com.zst.ynh.bean.YnhRepayInfoBean;
import com.zst.ynh.config.ArouterUtil;
import com.zst.ynh.config.BundleKey;
import com.zst.ynh.utils.CalendarManager;
import com.zst.ynh_base.adapter.recycleview.MultiItemTypeAdapter;
import com.zst.ynh_base.mvp.view.BaseFragment;
import com.zst.ynh_base.util.Layout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;

import static com.zst.ynh.config.SPkey.PREF_DATE_LIST;
import static com.zst.ynh.utils.CalendarManager.YNH_TITLE;

@Layout(R.layout.fragment_repaymentlist)
public class RepaymentListFragment extends BaseFragment implements IRepaymentView {

    private static final String tag = RepaymentListFragment.class.getSimpleName();
    @BindView(R.id.multiply_freshlayout)
    SmartRefreshLayout smartRefreshLayout;
    @BindView(R.id.recycleView_repayment)
    RecyclerView repaymentRecyclerView;
    private RepayOrderAdapter repaymentInfoAdapter;
    private List<HistoryOrderInfoBean.OrderItem> repaymentInfoBeanList = new ArrayList<>();
    private RepaymentPresent repaymentPresent;

    private int LIST_TYPE = ListType.YNH_REPAYMENT;


    public void setLIST_TYPE(int LIST_TYPE) {
        this.LIST_TYPE = LIST_TYPE;
    }

    public int getLIST_TYPE() {
        return LIST_TYPE;
    }

    public void autoFresh() {
        if (smartRefreshLayout != null) {
            onLazyLoad();
        }
    }


    public void loadData() {
        switch (LIST_TYPE) {
            case ListType.YNH_REPAYMENT:
                repaymentPresent.getYnhRepaymentInfo();
                break;
            case ListType.OTHER_REPAYMENT:
                repaymentPresent.getOtherRepaymentInfo();
                break;
            case ListType.YNH_ORDERS:
                repaymentPresent.getYnhOrders();
                break;
            case ListType.OTHER_ORDERS:
                repaymentPresent.getOtherOrders();
                break;
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.d(tag, "setUserVisibleHint:" + isVisibleToUser + ";type:" + getLIST_TYPE());
    }

    private void setAdapter() {
        if (repaymentInfoAdapter == null) {
            repaymentInfoAdapter = new RepayOrderAdapter(this.getActivity(), R.layout.item_repayment_info, repaymentInfoBeanList);
            repaymentRecyclerView.setAdapter(repaymentInfoAdapter);
            repaymentInfoAdapter.setOnItemClickListener(new MultiItemTypeAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, RecyclerView.ViewHolder holder, int position) {
                    HistoryOrderInfoBean.OrderItem orderItem = repaymentInfoBeanList.get(position);
                    if (orderItem.text.contains("已还款") || orderItem.is_repay) {
                        ARouter.getInstance().build(ArouterUtil.SIMPLE_WEB).withBoolean(BundleKey.WEB_SET_SESSION, true).withString(BundleKey.URL, orderItem.url).navigation();
                    }
                }

                @Override
                public boolean onItemLongClick(View view, RecyclerView.ViewHolder holder, int position) {
                    return false;
                }
            });
            repaymentInfoAdapter.setRepayBtnClickListener(new RepayOrderAdapter.RepayBtnClickListener() {
                @Override
                public void OnRepayBtnClick(View v, HistoryOrderInfoBean.OrderItem item) {
                    repaymentPresent.getRepayDetail(item.rep_id, item.platform_code);
                }
            });
        } else {
            repaymentInfoAdapter.notifyDataSetChanged();
        }

    }

    @Override
    public void onLazyLoad() {
        Log.d(tag, "onLazyLoad" + LIST_TYPE);
        if (smartRefreshLayout.getState() != RefreshState.None) {
            smartRefreshLayout.finishRefresh();
        }
        smartRefreshLayout.autoRefresh();
    }

    @Override
    protected void onRetry() {
        showLoadingView();
        loadData();
    }

    @Override
    protected void initView() {
        Log.d(tag, "initView" + LIST_TYPE);
        loadContentView();
        repaymentPresent = new RepaymentPresent();
        repaymentPresent.attach(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        repaymentRecyclerView.setLayoutManager(layoutManager);
        smartRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                loadData();
            }
        });
        if (!isLazyload) {
            onLazyLoad();
        }
    }


    @Override
    public void getYnhRepaymentSuccess(YnhRepayInfoBean response) {
        if (response.item.list.size() == 0) {
            CalendarManager.INSTANCE.deleteCalEvent(this.getActivity(), new String[]{YNH_TITLE});
            loadNoDataView(0, "您暂时还无还款订单哦~");
            return;
        }

        RepayItemBean repayItemBean = response.item.list.get(0);

        String today = TimeUtils.getNowString(new SimpleDateFormat("yyyy-MM-dd"));
        long timespan = TimeUtils.getTimeSpan(repayItemBean.repayment_date, today, new SimpleDateFormat("yyyy-MM-dd"), TimeConstants.DAY);
        if (timespan > 0) {
            ArrayList<CalendarBean> dates = new ArrayList<>();
            CalendarBean calendarBean = new CalendarBean();
            calendarBean.date = repayItemBean.repayment_date;
            calendarBean.title = YNH_TITLE;
            dates.add(calendarBean);
            CalendarManager.INSTANCE.requestCalendarPermission(this.getActivity(), dates);
        }

        repaymentInfoBeanList.clear();
        for (RepayItemBean listBean : response.item.list) {
            HistoryOrderInfoBean.OrderItem item = new HistoryOrderInfoBean.OrderItem();
            item.is_repay = true;
            item.logo = listBean.logo;
            item.money = listBean.repay_money;
            item.period = listBean.period;
            item.platform = listBean.platform;
            item.platform_code = listBean.platform_code;
            item.rep_id = listBean.repay_id;
            item.status_text = listBean.status_text;
            item.repayment_date = listBean.repayment_date;
            item.text = listBean.status_zh;
            item.time = listBean.repay_time;
            item.url = response.item.detail_url;
            item.title = "";
            repaymentInfoBeanList.add(item);
        }
        setAdapter();
        loadContentView();
    }

    @Override
    public void getYnhRepaymentError(int code, String errorMSG) {
        loadErrorView();
    }

    @Override
    public void getYnhOrdersSuccess(HistoryOrderInfoBean historyOrderInfoBean) {
        if (historyOrderInfoBean.item.size() == 0) {
            loadNoDataView(0, "您暂时还无还款订单哦~");
            return;
        }

        repaymentInfoBeanList.clear();
        repaymentInfoBeanList.addAll(historyOrderInfoBean.item);
        setAdapter();
        loadContentView();
    }

    @Override
    public void getYnhOrdersError(int code, String errorMsg) {
        loadErrorView();
    }

    @Override
    public void getOtherRepaymentSuccess(OtherPlatformRepayInfoBean otherPlatformRepayInfoBean) {

        if (otherPlatformRepayInfoBean.item.list.size() == 0) {
            Set<String> stringSet = SPUtils.getInstance().getStringSet(PREF_DATE_LIST, null);
            if (stringSet != null && stringSet.size() != 0) {
                for (String title : stringSet) {
                    CalendarManager.INSTANCE.deleteCalEvent(this.getActivity(), new String[]{title});
                }
                stringSet.clear();
                SPUtils.getInstance().put(PREF_DATE_LIST, stringSet);
            }

            loadNoDataView(0, "您暂时还无还款订单哦~");
            return;
        }

        repaymentInfoBeanList.clear();
        ArrayList<CalendarBean> dates = new ArrayList<>();
        for (RepayItemBean listBean : otherPlatformRepayInfoBean.item.list) {
            HistoryOrderInfoBean.OrderItem item = new HistoryOrderInfoBean.OrderItem();
            item.is_repay = true;
            item.logo = listBean.logo;
            item.money = listBean.repay_money;
            item.period = listBean.period;
            item.platform = listBean.platform;
            item.platform_code = listBean.platform_code;
            item.rep_id = listBean.repay_id;
            item.status_text = listBean.status_text;
            item.repayment_date = listBean.repayment_date;
            item.text = listBean.status_zh;
            item.time = listBean.repay_time;
            item.url = listBean.detail_url;
            item.title = "";

            String today = TimeUtils.getNowString(new SimpleDateFormat("yyyy-MM-dd"));
            long timespan = TimeUtils.getTimeSpan(item.repayment_date, today, new SimpleDateFormat("yyyy-MM-dd"), TimeConstants.DAY);
            if (timespan > 0) {//还款日在今天之后
                CalendarBean calendarBean = new CalendarBean();
                calendarBean.date = item.repayment_date;
                calendarBean.title = item.platform + "还款提醒";
                dates.add(calendarBean);
            }

            repaymentInfoBeanList.add(item);
        }
        setAdapter();
        loadContentView();
        if (dates.size() > 0) {
            CalendarManager.INSTANCE.requestCalendarPermission(this.getActivity(), dates);
        }
    }

    @Override
    public void getOtherRepaymentError(int code, String errorMsg) {
        loadErrorView();
    }

    @Override
    public void getOtherOrdersSuccess(HistoryOrderInfoBean historyOrderInfoBean) {
        if (historyOrderInfoBean.item.size() == 0) {
            loadNoDataView(0, "您暂时还无还款订单哦~");
            return;
        }
        repaymentInfoBeanList.clear();
        repaymentInfoBeanList.addAll(historyOrderInfoBean.item);
        setAdapter();
        loadContentView();
    }

    @Override
    public void getOtherOrderError(int code, String errorMsg) {
        loadErrorView();
    }

    @Override
    public void getDetailsSuccess(PaymentStyleBean response) {
        ARouter.getInstance().build(ArouterUtil.PAYMENT_STYLE).withSerializable(BundleKey.PAYMENTSTYLEBEAN, response).navigation();
        this.getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void showLoading() {
        showLoadingView();
    }

    @Override
    public void hideLoading() {
        hideLoadingView();
        smartRefreshLayout.finishRefresh();
    }

    @Override
    public void ToastErrorMessage(String msg) {
        ToastUtils.showShort(msg);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(tag, "onDestroyView");
        if (repaymentPresent != null) {
            repaymentPresent.detach();
        }
    }
}
