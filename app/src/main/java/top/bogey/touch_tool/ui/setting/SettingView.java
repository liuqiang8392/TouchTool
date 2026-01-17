package top.bogey.touch_tool.ui.setting;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.save.SettingSaver;
import top.bogey.touch_tool.databinding.ViewSettingBinding;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.notification.NotificationService;
import top.bogey.touch_tool.service.super_user.ISuperUser;
import top.bogey.touch_tool.service.super_user.SuperUser;
import top.bogey.touch_tool.service.super_user.root.RootSuperUser;
import top.bogey.touch_tool.service.super_user.shizuku.ShizukuSuperUser;
import top.bogey.touch_tool.ui.MainActivity;
import top.bogey.touch_tool.ui.blueprint.picker.ColorPickerPreview;
import top.bogey.touch_tool.utils.AppUtil;

public class SettingView extends Fragment {
    private ViewSettingBinding binding;

    private final MenuProvider menuProvider = new MenuProvider() {

        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.menu_setting, menu);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.autoGiveCapturePermission) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.setting_auto_give_capture_permission)
                        .setMessage(R.string.setting_auto_give_capture_permission_des);
                String cmd = String.format("appops set %s PROJECT_MEDIA allow", requireActivity().getPackageName());
                if (SuperUser.getInstance().isValid()) {
                    builder.setPositiveButton(R.string.setting_execute_shell, (dialog, which) -> SuperUser.getInstance().runCommand(cmd))
                            .setNegativeButton(R.string.setting_copy_shell, (dialog, which) -> AppUtil.copyToClipboard(requireContext(), cmd))
                            .setNeutralButton(R.string.cancel, null)
                            .show();
                } else {
                    builder.setPositiveButton(R.string.setting_copy_shell, (dialog, which) -> AppUtil.copyToClipboard(requireContext(), cmd))
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                }
                return true;
            }
            return false;
        }
    };

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getParentFragmentManager().beginTransaction().detach(this).commitAllowingStateLoss();
        getParentFragmentManager().beginTransaction().attach(this).commitAllowingStateLoss();
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) requireActivity();

        binding = ViewSettingBinding.inflate(inflater, container, false);

        binding.toolBar.addMenuProvider(menuProvider, getViewLifecycleOwner());

        // 功能启用
        binding.enableSwitch.setOnSwitchClickListener(v -> {
            MainAccessibilityService service = MainApplication.getInstance().getService();
            boolean enable = false;
            if (binding.enableSwitch.isChecked()) {
                if (AppUtil.isAccessibilityServiceEnabled(activity)) {
                    if (service != null) {
                        enable = true;
                        service.setEnabled(true);
                    } else {
                        binding.enableSwitch.setChecked(false);
                    }
                } else {
                    AppUtil.showDialog(activity, getString(R.string.app_setting_enable_tips, getString(R.string.app_name)), result -> {
                        if (result) {
                            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                        }
                    });
                    binding.enableSwitch.setChecked(false);
                }
            } else {
                if (service != null) {
                    service.setEnabled(false);
                }
            }

            SettingSaver.getInstance().setServiceEnabled(enable);
        });

        MainAccessibilityService serv = MainApplication.getInstance().getService();
        binding.enableSwitch.setChecked(serv != null && serv.isEnabled());

        // 重启服务
        binding.reloadService.setOnButtonClickListener(v -> {
            binding.reloadService.setButtonText(getString(R.string.app_setting_reload_button_running));
            binding.reloadService.setEnabled(false);

            if (activity.stopAccessibilityServiceBySecurePermission()) {
                binding.getRoot().postDelayed(() -> {
                    SettingSaver.getInstance().setServiceEnabled(true);
                    binding.enableSwitch.setChecked(true);
                    activity.restartAccessibilityServiceBySecurePermission();
                    Toast.makeText(activity, R.string.app_setting_reload_success, Toast.LENGTH_SHORT).show();
                    binding.reloadService.setEnabled(true);
                    binding.reloadService.setButtonText(getString(R.string.app_setting_reload_button_text));
                }, 1000);
            } else {
                Toast.makeText(activity, R.string.app_setting_reload_error, Toast.LENGTH_SHORT).show();
                binding.reloadService.setEnabled(true);
                binding.reloadService.setButtonText(getString(R.string.app_setting_reload_button_text));
            }
        });
        refreshReloadService();

        // 自动重启服务
        binding.autoReloadService.setOnButtonClickListener(v -> {
            String cmd = String.format("pm grant %s %s", requireActivity().getPackageName(), Manifest.permission.WRITE_SECURE_SETTINGS);
            if (SuperUser.getInstance().isValid()) {
                SuperUser.getInstance().runCommand(cmd);
                binding.getRoot().postDelayed(() -> {
                    refreshReloadService();
                    refreshAutoReloadService();
                }, 500);
            } else {
                AppUtil.copyToClipboard(activity, cmd);
                Toast.makeText(activity, R.string.copy_tips, Toast.LENGTH_SHORT).show();
            }
        });
        refreshAutoReloadService();

        // 隐藏后台
        binding.hideBackSwitch.setOnSwitchClickListener(v -> SettingSaver.getInstance().setHideAppBackground(activity, binding.hideBackSwitch.isChecked()));
        binding.hideBackSwitch.setChecked(SettingSaver.getInstance().isHideAppBackground());

        // 忽略电量限制
        binding.ignoreBatterySwitch.setOnSwitchClickListener(v -> {
            if (binding.ignoreBatterySwitch.isChecked()) {
                if (!AppUtil.isIgnoredBattery(activity)) {
                    AppUtil.gotoIgnoreBattery(activity);
                    binding.ignoreBatterySwitch.setChecked(false);
                }
            } else {
                if (AppUtil.isIgnoredBattery(activity)) {
                    AppUtil.gotoAppDetailView(activity);
                    binding.ignoreBatterySwitch.setChecked(true);
                }
            }
        });
        binding.ignoreBatterySwitch.setChecked(AppUtil.isIgnoredBattery(activity));

        // 前台服务
        binding.forgeServiceSwitch.setOnSwitchClickListener(v -> SettingSaver.getInstance().setKeepAliveForegroundServiceEnabled(activity, binding.forgeServiceSwitch.isChecked()));
        binding.forgeServiceSwitch.setChecked(SettingSaver.getInstance().isKeepAliveForegroundServiceEnabled());

        // 清理缓存
        binding.cleanCacheButton.setOnButtonClickListener(v -> {
            String[] dirs = new String[]{AppUtil.LOG_DIR_NAME, AppUtil.TASK_DIR_NAME, AppUtil.DOCUMENT_DIR_NAME};
            String[] dirNames = getResources().getStringArray(R.array.cache_dir_name);

            File[] files = new File[dirs.length];
            boolean[] isChecked = new boolean[dirs.length];
            for (int i = 0; i < dirs.length; i++) {
                String dir = dirs[i];
                File file = new File(activity.getCacheDir(), dir);
                files[i] = file;
                String sizeString = AppUtil.getFileSizeString(file);
                dirNames[i] += " (" + sizeString + ")";
                isChecked[i] = file.exists();
            }
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.app_setting_clean_cache)
                    .setPositiveButton(R.string.enter, (dialog, which) -> {
                        boolean flag = true;
                        for (int i = 0; i < isChecked.length; i++) {
                            boolean checked = isChecked[i];
                            if (checked) {
                                AppUtil.deleteFile(files[i]);
                            } else {
                                flag = false;
                            }
                        }
                        if (flag) AppUtil.deleteFile(activity.getCacheDir());
                        binding.cleanCacheButton.setDescription(getString(R.string.app_setting_clean_cache_desc, AppUtil.getFileSizeString(activity.getCacheDir())));
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .setMultiChoiceItems(dirNames, isChecked, (dialog, which, checked) -> isChecked[which] = checked)
                    .show();
        });
        binding.cleanCacheButton.setDescription(getString(R.string.app_setting_clean_cache_desc, AppUtil.getFileSizeString(activity.getCacheDir())));
        if (!AppUtil.isRelease(activity)) {
            binding.cleanCacheButton.setOnClickListener(v -> AppUtil.crashTest());
        }

        // 超级用户
        binding.superUserSelect.setOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                View view = group.findViewById(checkedId);
                int index = group.indexOfChild(view);
                SettingSaver.getInstance().setSuperUserType(index);

                // 尝试超级用户
                ISuperUser instance = SuperUser.getInstance();
                if (instance == null || !instance.init()) {
                    SettingSaver.getInstance().setSuperUserType(0);
                    binding.superUserSelect.checkIndex(0);

                    if (instance instanceof ShizukuSuperUser) Toast.makeText(activity, R.string.permission_setting_super_user_no_shizuku, Toast.LENGTH_SHORT).show();
                    else if (instance instanceof RootSuperUser) Toast.makeText(activity, R.string.permission_setting_super_user_no_root, Toast.LENGTH_SHORT).show();
                }
                refreshNotificationCmd();
                refreshAutoReloadService();
            }
        });
        binding.superUserSelect.checkIndex(SettingSaver.getInstance().getSuperUserType());

        // 通知来源
        binding.notificationTypeSelect.checkIndex(SettingSaver.getInstance().getNotificationType());
        binding.notificationTypeSelect.setOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                View view = group.findViewById(checkedId);
                int index = group.indexOfChild(view);
                if (index == 0) {
                    SettingSaver.getInstance().setNotificationType(0);
                    activity.stopService(new Intent(activity, NotificationService.class));
                } else {
                    if (NotificationService.isEnabled()) {
                        SettingSaver.getInstance().setNotificationType(1);
                    } else {
                        NotificationService.requestConnect(activity);
                        SettingSaver.getInstance().setNotificationType(0);
                        binding.notificationTypeSelect.checkIndex(0);
                    }
                }
                refreshNotificationCmd();
            }
        });

        binding.notificationTypeCmd.setOnButtonClickListener(v -> {
            String cmd = String.format("appops set %s RECEIVE_SENSITIVE_NOTIFICATIONS allow", requireActivity().getPackageName());
            if (SuperUser.getInstance().isValid()) {
                SuperUser.getInstance().runCommand(cmd);
            } else {
                AppUtil.copyToClipboard(activity, cmd);
                Toast.makeText(activity, R.string.copy_tips, Toast.LENGTH_SHORT).show();
            }
        });
        refreshNotificationCmd();

        // 精确定时
        AlarmManager alarmManager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        binding.alarmSwitch.setOnSwitchClickListener(v -> {
            if (binding.alarmSwitch.isChecked()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        SettingSaver.getInstance().setExactAlarmEnabled(true);
                    } else {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        startActivity(intent);
                        binding.alarmSwitch.setChecked(false);
                    }
                } else {
                    binding.alarmSwitch.setChecked(true);
                }
            } else {
                SettingSaver.getInstance().setExactAlarmEnabled(false);
            }
            if (binding.alarmSwitch.isChecked()) {
                MainAccessibilityService service = MainApplication.getInstance().getService();
                if (service != null && service.isEnabled()) {
                    service.resetAllAlarm();
                }
            }
        });
        binding.alarmSwitch.setChecked(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() && SettingSaver.getInstance().isExactAlarmEnabled());

        // 蓝牙监听
        binding.bluetoothSwitch.setOnSwitchClickListener(v -> {
            if (binding.bluetoothSwitch.isChecked()) {
                activity.launcherBluetooth((code, data) -> {
                    boolean enable = code == Activity.RESULT_OK;
                    SettingSaver.getInstance().setBluetoothEnabled(enable);
                    binding.bluetoothSwitch.setChecked(enable);
                });
            } else {
                SettingSaver.getInstance().setBluetoothEnabled(false);
            }
        });
        binding.bluetoothSwitch.setChecked(SettingSaver.getInstance().isBluetoothEnabled());


        // 手势轨迹
        binding.showTouchSwitch.setOnSwitchClickListener(v -> SettingSaver.getInstance().setShowGestureTrack(binding.showTouchSwitch.isChecked()));
        binding.showTouchSwitch.setChecked(SettingSaver.getInstance().isShowGestureTrack());

        // 标记目标区域
        binding.showTargetAreaSwitch.setOnSwitchClickListener(v -> SettingSaver.getInstance().setShowNodeArea(binding.showTargetAreaSwitch.isChecked()));
        binding.showTargetAreaSwitch.setChecked(SettingSaver.getInstance().isShowNodeArea());

        // 任务提示
        binding.taskTipsSwitch.setOnSwitchClickListener(v -> SettingSaver.getInstance().setShowTaskStartTips(binding.taskTipsSwitch.isChecked()));
        binding.taskTipsSwitch.setChecked(SettingSaver.getInstance().isShowTaskStartTips());

        // 详细日志
        binding.detailLogSwitch.setOnSwitchClickListener(v -> SettingSaver.getInstance().setDetailLog(binding.detailLogSwitch.isChecked()));
        binding.detailLogSwitch.setChecked(SettingSaver.getInstance().isDetailLog());

        // 日志重置
        binding.resetLogSwitch.setOnSwitchClickListener(v -> SettingSaver.getInstance().setLogResetOnStart(binding.resetLogSwitch.isChecked()));
        binding.resetLogSwitch.setChecked(SettingSaver.getInstance().isLogResetOnStart());

        // 音量键退出
        binding.volumeButtonExitSwitch.setOnSwitchClickListener(v -> SettingSaver.getInstance().setVolumeButtonExit(binding.volumeButtonExitSwitch.isChecked()));
        binding.volumeButtonExitSwitch.setChecked(SettingSaver.getInstance().isVolumeButtonExit());


        // 卡片默认展开状态
        binding.cardTypeSelect.setOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                View view = group.findViewById(checkedId);
                int index = group.indexOfChild(view);
                SettingSaver.getInstance().setDefaultCardExpandType(index);
            }
        });
        binding.cardTypeSelect.checkIndex(SettingSaver.getInstance().getDefaultCardExpandType());

        // 卡片整理时的间隔
        binding.arrangeCardOffset.setSliderOnChangeListener((slider, value, fromUser) -> SettingSaver.getInstance().setArrangeCardOffset((int) value));
        binding.arrangeCardOffset.setValue(SettingSaver.getInstance().getArrangeCardOffset());


        // 手动执行悬浮窗
        binding.manualPlaySetting.setOnClickListener(v -> {
            NavController controller = Navigation.findNavController(MainApplication.getInstance().getActivity(), R.id.conView);
            controller.navigate(SettingViewDirections.actionSettingToSettingPlayView());
        });
        binding.manualPlaySetting.setOnButtonClickListener(v -> {
            NavController controller = Navigation.findNavController(MainApplication.getInstance().getActivity(), R.id.conView);
            controller.navigate(SettingViewDirections.actionSettingToSettingPlayView());
        });

        // 小窗优化
        binding.supportFreeFormSwitch.setOnSwitchClickListener(v -> SettingSaver.getInstance().setSupportFreeForm(binding.supportFreeFormSwitch.isChecked()));
        binding.supportFreeFormSwitch.setChecked(SettingSaver.getInstance().isSupportFreeForm());

        // 夜间模式
        binding.themeSelect.setOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                View view = group.findViewById(checkedId);
                int index = group.indexOfChild(view);
                SettingSaver.getInstance().setNightModeType(index);
            }
        });
        binding.themeSelect.checkIndex(SettingSaver.getInstance().getNightModeType());

        // 动态颜色
        binding.dynamicColorSwitch.setOnSwitchClickListener(v -> SettingSaver.getInstance().setDynamicColorTheme(activity, binding.dynamicColorSwitch.isChecked()));
        binding.dynamicColorSwitch.setOnClickListener(v -> ColorPickerPreview.showPicker(result -> SettingSaver.getInstance().setDynamicColorValue(activity, result.getColor()), SettingSaver.getInstance().getDynamicColorValue()));
        binding.dynamicColorSwitch.setChecked(SettingSaver.getInstance().isDynamicColorTheme());

        PackageManager packageManager = activity.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(activity.getPackageName(), 0);
            binding.versionText.setText(packageInfo.versionName + "(" + packageInfo.versionCode + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return binding.getRoot();
    }

    private void refreshNotificationCmd() {
        int notificationType = SettingSaver.getInstance().getNotificationType();
        boolean version = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM;
        binding.notificationTypeCmd.setVisibility(notificationType == 1 && version ? View.VISIBLE : View.GONE);
        binding.notificationTypeCmd.setButtonText(getString(SuperUser.getInstance().isValid() ? R.string.setting_execute_shell : R.string.setting_copy_shell));
    }

    private void refreshReloadService() {
        boolean granted = requireActivity().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
        binding.reloadService.setVisibility(granted ? View.VISIBLE : View.GONE);
    }

    private void refreshAutoReloadService() {
        boolean granted = requireActivity().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
        binding.autoReloadService.setVisibility(granted ? View.GONE : View.VISIBLE);
        binding.autoReloadService.setButtonText(getString(SuperUser.getInstance().isValid() ? R.string.setting_execute_shell : R.string.setting_copy_shell));
    }
}
