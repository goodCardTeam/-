package com.dytj.leekbox.base;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.dytj.leekbox.AppConfig;
import com.dytj.leekbox.AppManager;
import com.dytj.leekbox.MyApplication;
import com.dytj.leekbox.R;
import com.dytj.leekbox.api.UserNetWork;
import com.dytj.leekbox.mvpBase.BasePresenter;
import com.dytj.leekbox.mvpBase.BaseView;
import com.dytj.leekbox.utils.Event;
import com.dytj.leekbox.utils.EventBusUtil;
import com.dytj.leekbox.utils.MyToast;
import com.dytj.leekbox.utils.PreferenceHelper;
import com.dytj.leekbox.utils.StatusBarUtil;
import com.dytj.leekbox.utils.SystemToolUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.LifecycleRegistryOwner;

public abstract class LifecycleBaseActivity<P extends BasePresenter> extends CheckPermissionsActivity
        implements LifecycleRegistryOwner, BaseView {
    // 是否允许全屏
    //  private boolean mAllowFullScreen = true;
    private final LifecycleRegistry mRegistry = new LifecycleRegistry(this);
    private long time;
    public boolean wasBackground = false;    //声明一个布尔变量,记录当前的活动背景
    public UserNetWork userNetWork;
    protected P presenter;
    private int sTheme;

    @Override
    public LifecycleRegistry getLifecycle() {
        return this.mRegistry;
    }

    public LifecycleRegistry getmRegistry() {
        return mRegistry;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setStatusBar();
        switch (0) {
            case 0:
                sTheme = R.style.AppThemeLightBlue;
                break;
            case 1:
                sTheme = R.style.AppThemeDeepBlue;
                break;
            case 2:
                sTheme = R.style.AppThemeGreen;
                break;
            case 3:
                sTheme = R.style.AppThemePink;
                break;
            case 4:
                break;

        }
        setTheme(sTheme);
        AppManager.getAppManager(this).addActivity(this);
        if (userNetWork == null) {
            userNetWork = new UserNetWork();
        }
        presenter = initPresenter();

        if (isRegisterEventBus()) {
            EventBusUtil.register(this);
        }
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
    protected void onPause() {
        super.onPause();
        if (isApplicationBroughtToBackground()) {
            //Logger.i("task: 从前台进入后台");
            wasBackground = true;
            doForAndBack(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    /*if (isBackground(this)&&JUCApplication.getInstance().isRestart){
      System.out.println("hotfix回调-点击home键调取了-回到后台");
      SophixManager.getInstance().killProcessSafely();
      JUCApplication.getInstance().setRestart(false);
    }*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppManager.getAppManager(this).finishActivity(this);
        if (presenter != null) {
            presenter.detach();//在presenter中解绑释放view
            presenter = null;
        }

        if (isRegisterEventBus()) {
            EventBusUtil.unregister(this);
        }
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        if (wasBackground) {
            //Logger.i("task: 从后台回到前台");
            wasBackground = false;
            doForAndBack(this);
        }
        // isBackground(this);
    }

    /**
     * 在子类中初始化对应的presenter
     *
     * @return 相应的presenter
     */
    public abstract P initPresenter();

    public void setStatusNoBar() {
//    StatusBarUtil.setColor(this, getResources().getColor(R.color.colorPrimary),0);
        StatusBarUtil.setColor(this, getResources().getColor(R.color.red_6729), 45);
    }

    public void setStatusYesBar() {
        StatusBarUtil.setTranslucent(this, 40);
        StatusBarUtil.setDarkStatusIcon(this, true);
    }


    //判断当前的应用程序是不是在运行
    //需要申请GETTask权限
    private boolean isApplicationBroughtToBackground() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            String packageName = topActivity.getPackageName();
            if (!topActivity.getPackageName().equals(getPackageName())) {
                return true;
            }
        }
        return false;
    }


    private void doForAndBack(Context context) {
        if (wasBackground) {
            //进入后台操作
            //String str = this.getClass().toString().trim();
            time = System.currentTimeMillis();
        } else {
            //进入前台操作
            long time2 = System.currentTimeMillis();
            Log.e(time + "", time2 + "////" + (time - time2));
            if (time != -1 && time2 - time > 120 * 1000) {
                checkFirstOpen();
                time = -1;
            }
        }
    }

    private void checkFirstOpen() {
        if (!TextUtils.isEmpty(getAccess_token())) {
            //设置过密码
            boolean isFingerPrintEnable = PreferenceHelper.readBoolean(getUserId(), AppConfig.IS_PREFER_FINGERPRINT_ENABLE, false);
            if (!TextUtils.isEmpty(getGestureKey()) || isFingerPrintEnable) {
                boolean isPreferGestureEnable = PreferenceHelper.readBoolean(getUserId(), AppConfig.IS_PREFER_GESTURE_ENABLE, false);
            }
        }
    }

    //找到指定的Activity
    public Activity getActivity(Class className) {

        return AppManager.getAppManager().findActivity(className);
    }

    /**
     * 设置添加屏幕的背景透明度
     *
     * @param bgAlpha
     */
    public void backgroundAlpha(float bgAlpha) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = bgAlpha; //0.0-1.0
        getWindow().setAttributes(lp);
    }

    //结束指定的Activity
    public void endActivity(Class className) {
        AppManager.getAppManager().finishActivity(className);
    }

    //添加使用全局Toast1的提示(在底部)
    public void showShortToastBottom(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        MyToast.showMyToast(getApplicationContext(), message, Toast.LENGTH_SHORT);
    }

    //添加使用全局Toast2的提示（居中）
    public void showShortToastCenter(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        MyToast.showMyToast2(getApplicationContext(), message, Toast.LENGTH_SHORT);
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

    /**
     * 当前登录状况下token，未登录字符串为空
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

    //获取手势密码
    public String getGestureKey() {
        try {
            String lockKey = PreferenceHelper.readString(getUserId(), AppConfig.PREFER_GESTUREKEY_TAG, "");
            return lockKey;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 获取最近使用手机号
     *
     * @return phone  在登录状态下返回的是当前登录的手机号
     */
    public String getLatestPhone() {
        String phone = "";
        try {
            phone = PreferenceHelper.readString(PreferenceHelper.DEFAULT_FILE_NAME, AppConfig.PREFER_LTPHONE_TAG, "");
        } catch (Exception e) {
            e.printStackTrace();
            phone = "";
        }
        return phone;
    }

    /**
     * 是否实名认证
     *
     * @return
     */
    public boolean isAuthentify() {
        boolean isAuthentify = false;
        String userId = getUserId();
        if (!TextUtils.isEmpty(userId)) {
            isAuthentify = PreferenceHelper.readBoolean(userId, AppConfig.PREFER_IS_REALNAME, false);
        }
        return isAuthentify;
    }

    //得到注册渠道编号
    public String getRegisterChannelCode() {
        String umChannelCode = "";//注册渠道编号
    /*
       应用宝 097
      OPPO 096
      华为 095
      小米 094
      Vivo 093
      百度 092
      360 091
     */
        //友盟渠道名
        String umChannelStr = SystemToolUtils.getChannel(this, "UMENG_CHANNEL");
        if (umChannelStr == null) {
            umChannelCode = "";
            return umChannelCode;
        }
        switch (umChannelStr) {
            case "1_应用宝":
                umChannelCode = "097";
                break;
            case "15_oppo软件商店":
                umChannelCode = "096";
                break;
            case "7_华为":
                umChannelCode = "095";
                break;
            case "2_小米应用市场":
                umChannelCode = "094";
                break;
            case "19_vivo应用市场":
                umChannelCode = "093";
                break;
            case "9_百度":
                umChannelCode = "092";
                break;
            case "18_360平台":
                umChannelCode = "091";
                break;
        }
        if (TextUtils.isEmpty(umChannelCode)) {
            //注册来源 001:前台注册 002：后台添加 003：手机APP 004:手机WAP
            umChannelCode = "003";
        }
        return umChannelCode;
    }

    /**
     * 重写 getResource 方法，防止系统字体影响
     *
     * @return
     */
    @Override
    public Resources getResources() {//禁止app字体大小跟随系统字体大小调节
        Resources resources = super.getResources();
        if (resources != null && resources.getConfiguration().fontScale != 1.0f) {
            android.content.res.Configuration configuration = resources.getConfiguration();
            configuration.fontScale = 1.0f;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }
        return resources;
    }

    @Override
    public void dismissLoadingDialog() {

    }

    @Override
    public void showLoadingDialog(String msg) {

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

//    protected void initSystemBarTint() {
//
//        Window window = getWindow();
//        if (translucentStatusBar()) {
//            // 沉浸式状态栏
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
//                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//                window.setStatusBarColor(Color.BLACK);
//            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
//            }
//            return;
//        }
//    }
}