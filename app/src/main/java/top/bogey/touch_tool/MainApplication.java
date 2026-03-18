package top.bogey.touch_tool;

import android.app.Application;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.tencent.mmkv.MMKV;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;

import top.bogey.touch_tool.bean.save.SettingSaver;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.ui.MainActivity;

public class MainApplication extends Application implements Thread.UncaughtExceptionHandler {
    private static MainApplication instance;

    private WeakReference<MainActivity> activity = new WeakReference<>(null);
    private WeakReference<MainAccessibilityService> service = new WeakReference<>(null);

    public static MainApplication getInstance() {
        return instance;
    }

    private Thread.UncaughtExceptionHandler handler;

    private String version;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        MMKV.initialize(this);
        SettingSaver.getInstance().addRunTimes();
        SettingSaver.getInstance().initColor(this);

        handler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);

        PackageManager packageManager = getPackageManager();
        try {
            version = packageManager.getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
        }
    }

    public void setActivity(MainActivity activity) {
        this.activity = new WeakReference<>(activity);
    }

    public MainActivity getActivity() {
        return activity.get();
    }

    public void setService(MainAccessibilityService service) {
        this.service = new WeakReference<>(service);
    }

    public MainAccessibilityService getService() {
        return service.get();
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        String errorInfo = e.toString();
        try {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            errorInfo = stringWriter.toString();
        } catch (Exception ignored) {
        }

        SettingSaver.getInstance().setRunningError(version + "\n" + errorInfo);
        handler.uncaughtException(t, e);
    }
}
