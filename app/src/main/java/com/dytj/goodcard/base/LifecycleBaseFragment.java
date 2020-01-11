package com.dytj.goodcard.base;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.dytj.goodcard.AppConfig;
import com.dytj.goodcard.MyApplication;
import com.dytj.goodcard.api.UserNetWork;
import com.dytj.goodcard.mvpBase.BasePresenter;
import com.dytj.goodcard.mvpBase.BaseView;
import com.dytj.goodcard.utils.Event;
import com.dytj.goodcard.utils.EventBusUtil;
import com.dytj.goodcard.utils.MyToast;
import com.dytj.goodcard.utils.PreferenceHelper;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Field;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.LifecycleRegistryOwner;

/**
 * Created by zeng on 2017/7/17.
 * Introduction:
 */

public abstract class LifecycleBaseFragment<P extends BasePresenter> extends Fragment implements LifecycleRegistryOwner, BaseView {
    protected LayoutInflater inflater;
    private View contentView;
    private Context context;
    private ViewGroup container;
    private Activity activity;
    public UserNetWork userNetWork;
    protected P presenter;

    public Context getContext() {
        if (activity == null) {
            return MyApplication.getInstance();
        }
        return activity;
    }

    public LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

    @Override
    public LifecycleRegistry getLifecycle() {
        return mLifecycleRegistry;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity().getApplicationContext();
    }

    public abstract P initPresenter();

    //子类通过重写onCreateView，调用setOnContentView进行布局设置，否则contentView==null，返回null
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.inflater = inflater;
        this.container = container;
        onCreateView(savedInstanceState);
        if (contentView == null) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }
        if (userNetWork == null) {
            userNetWork = new UserNetWork();
        }
        presenter = initPresenter();
        if (isRegisterEventBus()) {
            EventBusUtil.register(this);
        }
        return contentView;

    }

    protected void onCreateView(Bundle savedInstanceState) {

    }

    /**
     * 是否注册事件分发
     *
     * @return true绑定EventBus事件分发，默认不绑定，子类需要绑定的话复写此方法返回true.
     */
    protected boolean isRegisterEventBus() {
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        contentView = null;
        container = null;
        inflater = null;
    }

    public Context getApplicationContext() {
        return context;
    }

    public void setContentView(int layoutResID) {
        setContentView((ViewGroup) inflater.inflate(layoutResID, container, false));
    }

    public void setContentView(View view) {
        contentView = view;
    }

    public View getContentView() {
        return contentView;
    }

    public View findViewById(int id) {
        if (contentView != null)
            return contentView.findViewById(id);
        return null;
    }

    /**
     * 获取token  非登录状态下为空字符串
     *
     * @return token
     */
    public String getAccess_token() {
        String token = "";
        try {
            token = PreferenceHelper.readString(PreferenceHelper.DEFAULT_FILE_NAME, AppConfig.PREFER_TOKEN_TAG, "");
        } catch (Exception e) {
            e.printStackTrace();
            token = "";
        }
        return token;
    }

    //获得当前登录的userId
    public String getUserId() {
        try {
            return PreferenceHelper.readString(PreferenceHelper.DEFAULT_FILE_NAME, AppConfig.PREFER_USERID_TAG, "");//userid保存
        } catch (Exception e) {
            Log.e("error_userid", e.getMessage() + "");
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 获取最近使用手机号
     *
     * @return phone  在登录状态下返回的是当前登录的手机号
     */
    /*public String getLatestPhone() {
        String phone = "";
        try {
            phone = PreferenceHelper.readString(PreferenceHelper.DEFAULT_FILE_NAME, AppConfig.PREFER_LTPHONE_TAG, "");
        } catch (Exception e) {
            e.printStackTrace();
            phone = "";
        }
        return phone;
    }*/
    // http://stackoverflow.com/questions/15207305/getting-the-error-java-lang-illegalstateexception-activity-has-been-destroyed
    @Override
    public void onDetach() {
        Log.d("TAG", "onDetach() : ");
        super.onDetach();
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    //添加使用全局Toast1的提示(在底部)
    public void showShortToastBottom(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        MyToast.showMyToast(context, message, Toast.LENGTH_SHORT);
    }

    //添加使用全局Toast2的提示（居中）
    public void showShortToastCenter(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        MyToast.showMyToast2(context, message, Toast.LENGTH_SHORT);
    }

    //添加使用全局Toast2的提示（带图标的toast--成功）
    public void showShortToastIconOk(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        MyToast.showMyToastIconOk(getApplicationContext(), message, Toast.LENGTH_SHORT);
    }

    //添加使用全局Toast2的提示（带图标的toast--失败）
    public void showShortToastIconError(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        MyToast.showMyToastIconError(getApplicationContext(), message, Toast.LENGTH_SHORT);
    }


    @Override
    public void showLoadingDialog(String msg) {

    }

    @Override
    public void dismissLoadingDialog() {

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventBusCome(Event event) {
        if (event != null) {
            receiveEvent(event);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onStickyEventBusCome(Event event) {
        if (event != null) {
            receiveStickyEvent(event);
        }
    }

    /**
     * 接收到分发到事件
     *
     * @param event 事件
     */
    protected void receiveEvent(Event event) {

    }

    /**
     * 接受到分发的粘性事件
     *
     * @param event 粘性事件
     */
    protected void receiveStickyEvent(Event event) {

    }
}
