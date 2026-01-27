package top.bogey.touch_tool.service;

import static top.bogey.touch_tool.common.StaticValues.YOLO_APP_PACKAGE;
import static top.bogey.touch_tool.common.StaticValues.YOLO_APP_SERVICE;
import static top.bogey.touch_tool.service.TaskInfoSummary.OCR_SERVICE_ACTION;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.hardware.HardwareBuffer;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import top.bogey.ocr.IOcr;
import top.bogey.ocr.IOcrCallback;
import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.start.StartAction;
import top.bogey.touch_tool.bean.action.start.TimeStartAction;
import top.bogey.touch_tool.bean.other.log.LogInfo;
import top.bogey.touch_tool.bean.other.log.NormalLog;
import top.bogey.touch_tool.bean.save.SettingSaver;
import top.bogey.touch_tool.bean.save.log.LogSaver;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.capture.CaptureService;
import top.bogey.touch_tool.service.receiver.SystemEventReceiver;
import top.bogey.touch_tool.service.super_user.SuperUser;
import top.bogey.touch_tool.ui.InstantActivity;
import top.bogey.touch_tool.ui.PermissionActivity;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.ThreadUtil;
import top.bogey.touch_tool.utils.callback.BooleanResultCallback;
import top.bogey.touch_tool.utils.callback.ResultCallback;
import top.bogey.yolo.IYolo;
import top.bogey.yolo.IYoloCallback;

@SuppressLint("AccessibilityPolicy")
public class MainAccessibilityService extends AccessibilityService {
    static {
        System.loadLibrary("native");
    }

    public static final MutableLiveData<Boolean> enabled = new MutableLiveData<>(false);
    public static final MutableLiveData<Boolean> connected = new MutableLiveData<>(false);

    private SystemEventReceiver systemEventReceiver;
    private final TaskInfoSummary taskInfoSummary = TaskInfoSummary.getInstance();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        if (event.getClassName() == null) return;

        String packageName = event.getPackageName().toString();
        String className = event.getClassName().toString();
        if (packageName.isEmpty() || className.isEmpty()) return;
        Log.d("TAG", "onAccessibilityEvent: " + event);
        Log.d("TAG", "onAccessibilityEvent: " + packageName + "/" + className);

        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // 桌面应用需要检查实际界面与事件是否匹配
            if (taskInfoSummary.isLauncherApp(packageName)) {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null && root.getPackageName() != null) {
                    if (!Objects.equals(packageName, root.getPackageName().toString())) {
                        // 界面不匹配直接退出
                        return;
                    }
                }
            }
            taskInfoSummary.enterActivity(packageName, className);
        } else if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            Map<String, String> content = new HashMap<>();
            List<CharSequence> textList = event.getText();

            if (className.contains(Notification.class.getSimpleName())) {
                if (SettingSaver.getInstance().getNotificationType() != 0) return;

                Notification notification = (Notification) event.getParcelableData();
                if (notification != null && notification.extras != null) {
                    Bundle extras = notification.extras;
                    for (String key : extras.keySet()) {
                        Object value = extras.get(key);
                        content.put(key, String.valueOf(value));
                    }
                } else if (!textList.isEmpty()) {
                    String s = textList.get(0).toString();
                    Pattern pattern = AppUtil.getPattern("^(.+?): (.+)$");
                    if (pattern != null) {
                        Matcher matcher = pattern.matcher(s);
                        if (matcher.find()) {
                            content.put(Notification.EXTRA_TITLE, matcher.group(1));
                            content.put(Notification.EXTRA_TEXT, matcher.group(2));
                        }
                    }
                }

                taskInfoSummary.setNotification(TaskInfoSummary.NotificationType.NOTIFICATION, packageName, content);
            } else if (className.contains(Toast.class.getSimpleName())) {
                if (textList.isEmpty()) return;
                String text = textList.get(0).toString();
                content.put(Notification.EXTRA_TEXT, text);
                taskInfoSummary.setNotification(TaskInfoSummary.NotificationType.TOAST, packageName, content);
            }
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        connected.setValue(true);
        setEnabled(SettingSaver.getInstance().isServiceEnabled());
    }

    @Override
    public boolean onUnbind(Intent intent) {
        connected.setValue(false);
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MainApplication.getInstance().setService(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        connected.setValue(false);
        setEnabled(false);
        MainApplication.getInstance().setService(null);
    }

    public boolean isConnected() {
        return Boolean.TRUE.equals(connected.getValue());
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled.getValue()) && isConnected();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void setEnabled(boolean bool) {
        enabled.setValue(bool);

        if (isEnabled()) {
            systemEventReceiver = new SystemEventReceiver(this);
            systemEventReceiver.register();

            resetAllAlarm();
            SuperUser.getInstance().tryInit();
            Intent intent = new Intent(this, InstantActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            if (systemEventReceiver != null) systemEventReceiver.unregister();
            systemEventReceiver = null;

            if (tts != null) tts.shutdown();
            tts = null;

            stopAllTask();
            stopCapture();
            stopOcrService();
            stopYoloService();
            stopSound(null);
            cancelAllAlarm();
            SuperUser.getInstance().exit();
        }
    }

    // 任务 ----------------------------------------------------------------------------- start
    private final Set<TaskRunnable> tasks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<ITaskListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void addListener(ITaskListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ITaskListener listener) {
        listeners.remove(listener);
    }

    public void runTask(Task task, StartAction startAction) {
        runTask(task, startAction, null);
    }

    public TaskRunnable runTask(Task task, StartAction startAction, ITaskListener listener) {
        if (task == null || startAction == null) return null;
        if (!isEnabled()) return null;

        if (startAction.getRestartType() == StartAction.RestartType.CANCEL) {
            if (isTaskRunning(task, startAction)) return null;
        }

        TaskRunnable runnable = new TaskRunnable(task, startAction);
        if (listener != null) runnable.addListener(listener);

        runnable.addListener(new TaskListener() {
            @Override
            public void onFinish(TaskRunnable runnable) {
                tasks.remove(runnable);
            }
        });

        // 直接加入到任务列表。如果在开始里加入，需要等待线程启动，当同时有多条开始进来时，重入策略会失效
        if (startAction.getRestartType() == StartAction.RestartType.RESTART) {
            stopTask(runnable.getStartTask());
        }
        tasks.add(runnable);

        listeners.stream().filter(Objects::nonNull).forEach(runnable::addListener);
        Future<?> future = ThreadUtil.submitTask(runnable);
        runnable.setFuture(future);
        return runnable;
    }

    public boolean isTaskRunning(Task task) {
        if (task == null) return false;
        TaskRunnable runnable = getTaskRunnable(task.getId(), null);
        return runnable != null && !runnable.isInterrupt();
    }

    public boolean isTaskRunning(Task task, Action action) {
        if (task == null || action == null) return false;
        TaskRunnable runnable = getTaskRunnable(task.getId(), action.getId());
        return runnable != null && !runnable.isInterrupt();
    }

    public TaskRunnable getTaskRunnable(String taskId, String actionId) {
        for (TaskRunnable runnable : tasks) {
            if (runnable.isInterrupt()) continue;

            Task startTask = runnable.getStartTask();
            if (taskId.equals(startTask.getId())) {
                if (actionId != null) {
                    Action action = runnable.getStartAction();
                    if (actionId.equals(action.getId())) {
                        return runnable;
                    }
                } else {
                    return runnable;
                }
            }
        }
        return null;
    }

    public void stopTask(Task task) {
        if (task == null) return;
        for (TaskRunnable runnable : tasks) {
            if (runnable.isInterrupt()) continue;

            Task startTask = runnable.getStartTask();
            if (task.equals(startTask)) {
                runnable.stop();
            }
        }
    }

    public void stopAllTask() {
        for (TaskRunnable runnable : tasks) {
            if (runnable.isInterrupt()) continue;
            runnable.stop();
        }
    }

    public void stopAllTask(Task exclude) {
        for (TaskRunnable runnable : tasks) {
            if (runnable.isInterrupt()) continue;
            if (runnable.getStartTask().equals(exclude)) continue;
            runnable.stop();
        }
    }
    // 任务 ----------------------------------------------------------------------------- end

    // 定时 ----------------------------------------------------------------------------- start
    private PendingIntent getAlarmPendingIntent(String taskId, String actionId) {
        Intent intent = new Intent(InstantActivity.INTENT_KEY_DO_ACTION);
        intent.putExtra(InstantActivity.TASK_ID, taskId);
        intent.putExtra(InstantActivity.ACTION_ID, actionId);

        return PendingIntent.getBroadcast(this, Objects.hashCode(taskId + actionId), intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void resetAllAlarm() {
        List<Task> tasks = TaskSaver.getInstance().getTasks(TimeStartAction.class);
        for (Task task : tasks) {
            for (Action action : task.getActions(TimeStartAction.class)) {
                TimeStartAction timeStartAction = (TimeStartAction) action;
                cancelAlarm(task, timeStartAction);
                if (timeStartAction.isEnable()) {
                    addAlarm(task, timeStartAction);
                }
            }
        }
    }

    public void cancelAllAlarm() {
        List<Task> tasks = TaskSaver.getInstance().getTasks(TimeStartAction.class);
        for (Task task : tasks) {
            for (Action action : task.getActions(TimeStartAction.class)) {
                TimeStartAction timeStartAction = (TimeStartAction) action;
                cancelAlarm(task, timeStartAction);
            }
        }
    }

    public void cancelAlarm(Task task, TimeStartAction timeStartAction) {
        if (task == null || timeStartAction == null) return;
        PendingIntent intent = getAlarmPendingIntent(task.getId(), timeStartAction.getId());
        if (intent == null) return;
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(intent);
    }

    public void addAlarm(Task task, TimeStartAction timeStartAction) {
        if (task == null || timeStartAction == null) return;
        if (!timeStartAction.isEnable()) return;
        if (!isEnabled()) return;
        if (!SettingSaver.getInstance().isExactAlarmEnabled()) return;

        PendingIntent pendingIntent = getAlarmPendingIntent(task.getId(), timeStartAction.getId());
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        long timeMillis = System.currentTimeMillis();
        long startTime = timeStartAction.getStartTime();
        long periodic = timeStartAction.getPeriodic();

        long nextStartTime = startTime;
        if (periodic > 0) {
            long l = timeMillis - startTime;
            // 当前时间没达到定时时间，下次执行时间就是开始时间
            if (l > 0) {
                // 当前时间大于开始时间，需要计算下次开始的时间，防止定时任务刚设定就执行了
                int loop = (int) Math.ceil(l * 1f / periodic);
                nextStartTime = startTime + loop * periodic;
                // 如果算出的下个开始时间没有大于现在时间30s，应该是算错了，下个开始时间再加个间隔
                if (nextStartTime - timeMillis < 30 * 1000) {
                    nextStartTime += periodic;
                }
            }
        }
        if (nextStartTime < timeMillis) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (manager.canScheduleExactAlarms()) {
                AlarmManager.AlarmClockInfo clockInfo = new AlarmManager.AlarmClockInfo(nextStartTime, null);
                manager.setAlarmClock(clockInfo, pendingIntent);
//                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextStartTime, pendingIntent);
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextStartTime, pendingIntent);
            }
        } else {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextStartTime, pendingIntent);
        }
        LogSaver.getInstance().addLog(task.getId(), new LogInfo(new NormalLog(getString(R.string.time_start_action_set_tips, AppUtil.formatDateTime(this, nextStartTime, false, true)))), true);
    }

    public void replaceAlarm(Task task) {
        if (task == null) return;
        Task originTask = TaskSaver.getInstance().getOriginTask(task.getId());
        List<Action> actions = task.getActions(TimeStartAction.class);

        if (originTask != null) {
            originTask.getActions(TimeStartAction.class).forEach(startAction -> {
                TimeStartAction timeStartAction = (TimeStartAction) startAction;
                if (!timeStartAction.isEnable()) return;

                boolean isExist = false;
                for (Action action : actions) {
                    if (action.getId().equals(timeStartAction.getId())) {
                        isExist = true;
                        break;
                    }
                }
                if (!isExist) {
                    cancelAlarm(task, timeStartAction);
                }
            });
        }

        actions.forEach(startAction -> {
            TimeStartAction timeStartAction = (TimeStartAction) startAction;
            if (timeStartAction.isEnable()) {
                addAlarm(task, timeStartAction);
            } else {
                cancelAlarm(task, timeStartAction);
            }
        });
    }

    // 定时 ----------------------------------------------------------------------------- end

    // 录屏 ----------------------------------------------------------------------------- start
    private BooleanResultCallback captureCallback;
    private ServiceConnection captureConnection = null;
    private CaptureService.CaptureBinder captureBinder = null;

    public boolean isCaptureEnabled() {
        if (isEnabled()) {
            return captureBinder != null;
        }
        return false;
    }

    public boolean startCapture(BooleanResultCallback callback) {
        if (captureBinder == null) {
            captureCallback = callback;
            Intent intent = new Intent(this, PermissionActivity.class);
            intent.putExtra(PermissionActivity.CAPTURE_PERMISSION, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } else {
            if (callback != null) callback.onResult(true);
            return false;
        }
    }

    public void stopCapture() {
        if (captureConnection != null) {
            unbindService(captureConnection);
            captureConnection = null;
            stopService(new Intent(this, CaptureService.class));
        }
        captureBinder = null;
    }

    public void bindCapture(boolean result, Intent data) {
        if (result) {
            captureConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    captureBinder = (CaptureService.CaptureBinder) service;
                    callCaptureCallback(true);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    stopCapture();
                }
            };

            Intent intent = new Intent(this, CaptureService.class);
            intent.putExtra(CaptureService.DATA, data);
            if (!bindService(intent, captureConnection, Context.BIND_AUTO_CREATE)) callCaptureCallback(false);
        } else {
            callCaptureCallback(false);
        }
    }

    private void callCaptureCallback(boolean result) {
        if (captureCallback != null) {
            captureCallback.onResult(result);
            captureCallback = null;
        }
    }

    // 录屏 ----------------------------------------------------------------------------- end

    // 截图 ----------------------------------------------------------------------------- start

    private WeakReference<Bitmap> screenShot = new WeakReference<>(null);

    public synchronized Bitmap getScreenShotByCapture() {
        if (captureBinder == null) return screenShot.get();
        Bitmap bitmap = captureBinder.getScreenShot();
        screenShot = new WeakReference<>(bitmap);
        return bitmap;
    }

    // 不能递归调用，会锁死
    public Bitmap getScreenShotByAccessibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            CompletableFuture<Bitmap> future = new CompletableFuture<>();
            takeScreenshot(0, ThreadUtil.getExecutorService(), new TakeScreenshotCallback() {
                @Override
                public void onSuccess(@NonNull ScreenshotResult result) {
                    try (HardwareBuffer hardwareBuffer = result.getHardwareBuffer()) {
                        Bitmap bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, result.getColorSpace());
                        if (bitmap != null) {
                            Bitmap copy = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                            screenShot = new WeakReference<>(copy);
                            bitmap.recycle();
                            future.complete(copy);
                        }
                    } catch (Exception e) {
                        future.complete(screenShot.get());
                    }
                }

                @Override
                public void onFailure(int errorCode) {
                    future.complete(screenShot.get());
                }
            });
            return future.join();
        } else {
            return getScreenShotByCapture();
        }
    }

    public Bitmap tryGetScreenShot() {
        if (isCaptureEnabled()) {
            return getScreenShotByCapture();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return getScreenShotByAccessibility();
        }

        return null;
    }

    // 截图 ----------------------------------------------------------------------------- end

    // Ocr ----------------------------------------------------------------------------- start
    private final Map<String, IOcr> ocrBinderMap = new HashMap<>();
    private final Map<String, ServiceConnection> ocrConnectionMap = new HashMap<>();

    private synchronized void initOcrService(String packageName, BooleanResultCallback callback) {
        IOcr iOcr = ocrBinderMap.get(packageName);
        if (iOcr == null || !iOcr.asBinder().isBinderAlive()) {
            ServiceConnection connection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    IOcr iOcr = IOcr.Stub.asInterface(service);
                    ocrBinderMap.put(packageName, iOcr);
                    callback.onResult(true);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    ocrBinderMap.remove(packageName);
                    ocrConnectionMap.remove(packageName);
                }
            };

            Intent intent = new Intent(OCR_SERVICE_ACTION);
            intent.setPackage(packageName);
            if (bindService(intent, connection, Context.BIND_AUTO_CREATE)) {

                ocrConnectionMap.put(packageName, connection);
            } else {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(this, R.string.bind_service_failed_tips, Toast.LENGTH_SHORT).show());
                callback.onResult(false);
            }
        } else {
            callback.onResult(true);
        }
    }

    public synchronized void runOcr(Bitmap bitmap, String packageName, ResultCallback<List<OcrResult>> callback) {
        if (bitmap == null || bitmap.isRecycled()) {
            callback.onResult(new ArrayList<>());
            return;
        }

        initOcrService(packageName, result -> {
            if (result) {
                IOcr iOcr = ocrBinderMap.get(packageName);
                if (iOcr != null && iOcr.asBinder().isBinderAlive()) {
                    try {
                        iOcr.runOcr(bitmap, new IOcrCallback.Stub() {
                            @Override
                            public void onResult(List<OcrResult> result) {
                                callback.onResult(result);
                            }
                        });
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        callback.onResult(new ArrayList<>());
                    }
                } else {
                    callback.onResult(new ArrayList<>());
                }
            }
        });
    }

    private void stopOcrService() {
        for (String packageName : ocrConnectionMap.keySet()) {
            ServiceConnection connection = ocrConnectionMap.get(packageName);
            if (connection != null) {
                unbindService(connection);
            }
        }
        ocrBinderMap.clear();
        ocrConnectionMap.clear();
    }

    // Ocr ----------------------------------------------------------------------------- end

    // Yolo ----------------------------------------------------------------------------- start
    private IYolo yolo;
    private ServiceConnection yoloConnection;

    private void initYoloService(BooleanResultCallback callback) {
        if (yolo == null || !yolo.asBinder().isBinderAlive()) {
            yoloConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    yolo = IYolo.Stub.asInterface(service);
                    callback.onResult(true);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    yolo = null;
                    yoloConnection = null;
                }
            };

            Intent intent = new Intent();
            intent.setComponent(new ComponentName(YOLO_APP_PACKAGE, YOLO_APP_SERVICE));
            if (!bindService(intent, yoloConnection, Context.BIND_AUTO_CREATE)) {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(this, R.string.bind_service_failed_tips, Toast.LENGTH_SHORT).show());
                callback.onResult(false);
            }
        } else {
            callback.onResult(true);
        }
    }

    public synchronized void runYolo(Bitmap bitmap, String modelName, int similarity, ResultCallback<List<YoloResult>> callback) {
        if (bitmap == null || bitmap.isRecycled()) {
            callback.onResult(new ArrayList<>());
            return;
        }

        initYoloService(result -> {
            if (result) {
                try {
                    yolo.runYolo(bitmap, modelName, similarity / 100f, new IYoloCallback.Stub() {
                        @Override
                        public void onResult(List<YoloResult> result) {
                            callback.onResult(result);
                        }
                    });
                } catch (RemoteException e) {
                    e.printStackTrace();
                    callback.onResult(new ArrayList<>());
                }
            }
        });
    }

    public synchronized void getYoloModelList(ResultCallback<List<String>> callback) {
        initYoloService(result -> {
            if (result) {
                try {
                    callback.onResult(yolo.getModelList());
                } catch (RemoteException e) {
                    callback.onResult(new ArrayList<>());
                }
            }
        });
    }

    private void stopYoloService() {
        if (yoloConnection != null) {
            unbindService(yoloConnection);
            yoloConnection = null;
        }
        yolo = null;
    }

    // Yolo ----------------------------------------------------------------------------- end

    // 按键 ----------------------------------------------------------------------------- start
    private Handler handler;

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (!SettingSaver.getInstance().isVolumeButtonExit()) return super.onKeyEvent(event);
        if (handler == null) handler = new Handler();

        if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                handler.postDelayed(() -> {
                    Toast.makeText(this, isEnabled() ? R.string.app_setting_disable : R.string.app_setting_enabled, Toast.LENGTH_SHORT).show();
                    setEnabled(!isEnabled());
                }, 2000);
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                handler.removeCallbacksAndMessages(null);
                if (isEnabled()) stopAllTask();
            }
        }
        return super.onKeyEvent(event);
    }
    // 按键 ----------------------------------------------------------------------------- end

    // 手势 ----------------------------------------------------------------------------- start
    public void runGesture(int x, int y, int time, BooleanResultCallback callback) {
        if (x >= 0 && y >= 0 && time > 0) {
            Path path = new Path();
            path.moveTo(x, y);
            runGesture(Collections.singleton(new GestureDescription.StrokeDescription(path, 0, time)), callback);
            return;
        }
        if (callback != null) callback.onResult(false);
    }

    public void runGesture(Set<GestureDescription.StrokeDescription> strokeDescriptions, BooleanResultCallback callback) {
        if (strokeDescriptions == null || strokeDescriptions.isEmpty()) {
            if (callback != null) callback.onResult(false);
            return;
        }

        GestureDescription.Builder builder = new GestureDescription.Builder();
        for (GestureDescription.StrokeDescription strokeDescription : strokeDescriptions) {
            builder.addStroke(strokeDescription);
        }
        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                if (callback != null) callback.onResult(true);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                if (callback != null) callback.onResult(false);
            }
        }, null);
    }

    public void runGesture(List<Set<GestureDescription.StrokeDescription>> strokeDescriptions, BooleanResultCallback callback) {
        if (strokeDescriptions == null || strokeDescriptions.isEmpty()) {
            if (callback != null) callback.onResult(false);
            return;
        }

        GestureHandler gestureHandler = new GestureHandler(strokeDescriptions, callback);
        gestureHandler.dispatchGesture();
    }

    private class GestureHandler {
        private final List<Set<GestureDescription.StrokeDescription>> strokeList;
        private final BooleanResultCallback callback;

        public GestureHandler(List<Set<GestureDescription.StrokeDescription>> strokeList, BooleanResultCallback callback) {
            this.strokeList = strokeList;
            this.callback = callback;
        }

        private void dispatchGesture() {
            if (strokeList.isEmpty()) {
                if (callback != null) callback.onResult(true);
                return;
            }
            runGesture(strokeList.remove(0), result -> {
                if (result) {
                    dispatchGesture();
                } else {
                    if (callback != null) callback.onResult(false);
                }
            });
        }
    }
    // 手势 ----------------------------------------------------------------------------- end

    // 播放声音 ----------------------------------------------------------------------------- start
    private final Map<String, MediaPlayer> playerMap = new HashMap<>();

    public void playSound(String path, int index) {
        stopSound(path);
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, Uri.parse(path));
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(index == 0 ? AudioAttributes.USAGE_MEDIA : AudioAttributes.USAGE_NOTIFICATION_RINGTONE).setContentType(index == 0 ? AudioAttributes.CONTENT_TYPE_MUSIC : AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            mediaPlayer.setOnPreparedListener(mp -> {
                playerMap.put(path, mp);
                mp.start();
                int duration = mp.getDuration();
                new Handler(Looper.getMainLooper()).postDelayed(() -> stopSound(path), duration);
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopSound(String path) {
        if (path == null || path.isEmpty()) {
            playerMap.forEach((s, mediaPlayer) -> {
                mediaPlayer.stop();
                mediaPlayer.release();
            });
            playerMap.clear();
            return;
        }

        MediaPlayer mediaPlayer = playerMap.remove(path);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    private TextToSpeech tts;

    public void speak(String text) {
        if (tts == null) {
            tts = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.CHINA);
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            });
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // 播放声音 ----------------------------------------------------------------------------- end

    // 震动 ----------------------------------------------------------------------------- start
    public void vibrate(long duration) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(duration);
        }
    }
    // 震动 ----------------------------------------------------------------------------- end
}
