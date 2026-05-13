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
import top.bogey.touch_tool.bean.save.setting.SettingSaver;
import top.bogey.touch_tool.databinding.ViewSettingBinding;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.service.notification.NotificationService;
import top.bogey.touch_tool.service.super_user.ISuperUser;
import top.bogey.touch_tool.service.super_user.SuperUser;
import top.bogey.touch_tool.service.super_user.root.RootSuperUser;
import top.bogey.touch_tool.service.super_user.shizuku.ShizukuSuperUser;
import top.bogey.touch_tool.ui.MainActivity;
import top.bogey.touch_tool.ui.blueprint.picker.ColorPickerPreview;
import top.bogey.touch_tool.ui.tool.task_manager.ExportTaskDialog;
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
            } else if (menuItem.getItemId() == R.id.helpDoc) {
                AppUtil.gotoUrl(requireContext(), getString(R.string.setting_help_doc_url));
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
                    if (SettingSaver.SHOW_APP_SERVICE_ENABLE_TIPS.get()) {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    } else {
                        AppUtil.showDialog(activity, getString(R.string.app_setting_enable_tips, getString(R.string.app_name)), result -> {
                            if (result) {
                                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                            }
                        });
                        SettingSaver.SHOW_APP_SERVICE_ENABLE_TIPS.set(true);
                    }
                    binding.enableSwitch.setChecked(false);
                }
            } else {
                if (service != null) {
                    service.setEnabled(false);
                }
            }
            SettingSaver.APP_SERVICE.set(enable);
        });

        MainAccessibilityService serv = MainApplication.getInstance().getService();
        binding.enableSwitch.setChecked(serv != null && serv.isEnabled());

        // 重启服务
        binding.reloadService.setOnButtonClickListener(v -> {
            binding.reloadService.setButtonText(getString(R.string.app_setting_reload_button_running));
            binding.reloadService.setEnabled(false);

            if (activity.stopAccessibilityServiceBySecurePermission()) {
                binding.getRoot().postDelayed(() -> {
                    SettingSaver.APP_SERVICE.set(true);
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
        binding.hideBackSwitch.setOnSwitchClickListener(v -> SettingSaver.APP_HIDE_ACTIVITY_BACKGROUND.set(activity, binding.hideBackSwitch.isChecked()));
        binding.hideBackSwitch.setChecked(SettingSaver.APP_HIDE_ACTIVITY_BACKGROUND.get());

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
        binding.forgeServiceSwitch.setOnSwitchClickListener(v -> SettingSaver.APP_KEEP_ALIVE_SERVICE.set(activity, binding.forgeServiceSwitch.isChecked()));
        binding.forgeServiceSwitch.setChecked(SettingSaver.APP_KEEP_ALIVE_SERVICE.get());

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
                        binding.cleanCacheButton.setDescription(getString(R.string.app_setting_clean_cache_desc, AppUtil.getCacheDirsSizeString(activity)));
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .setMultiChoiceItems(dirNames, isChecked, (dialog, which, checked) -> isChecked[which] = checked)
                    .show();
        });
        binding.cleanCacheButton.setDescription(getString(R.string.app_setting_clean_cache_desc, AppUtil.getCacheDirsSizeString(activity)));
        if (!AppUtil.isRelease(activity)) {
            binding.cleanCacheButton.setOnClickListener(v -> AppUtil.crashTest());
        }

        // 超级用户
        binding.superUserSelect.setOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                View view = group.findViewById(checkedId);
                int index = group.indexOfChild(view);
                SettingSaver.PERMISSION_SUPER_USER.set(index);

                // 尝试超级用户
                ISuperUser instance = SuperUser.getInstance();
                if (instance != null) {
                    instance.init(result -> {
                        if (!result) {
                            SettingSaver.PERMISSION_SUPER_USER.set(0);
                            binding.superUserSelect.checkIndex(0);

                            if (instance instanceof ShizukuSuperUser) Toast.makeText(activity, R.string.permission_setting_super_user_no_shizuku, Toast.LENGTH_SHORT).show();
                            else if (instance instanceof RootSuperUser) Toast.makeText(activity, R.string.permission_setting_super_user_no_root, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                refreshNotificationCmd();
                refreshAutoReloadService();
            }
        });
        binding.superUserSelect.checkIndex(SettingSaver.PERMISSION_SUPER_USER.get());

        // 通知来源
        binding.notificationTypeSelect.checkIndex(SettingSaver.PERMISSION_NOTIFICATION.get());
        binding.notificationTypeSelect.setOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                View view = group.findViewById(checkedId);
                int index = group.indexOfChild(view);
                if (index == 0) {
                    SettingSaver.PERMISSION_NOTIFICATION.set(0);
                    activity.stopService(new Intent(activity, NotificationService.class));
                } else {
                    if (NotificationService.isEnabled()) {
                        SettingSaver.PERMISSION_NOTIFICATION.set(1);
                    } else {
                        NotificationService.requestConnect(activity);
                        SettingSaver.PERMISSION_NOTIFICATION.set(0);
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
                        SettingSaver.PERMISSION_EXACT_ALARM.set(true);
                    } else {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        startActivity(intent);
                        binding.alarmSwitch.setChecked(false);
                    }
                } else {
                    binding.alarmSwitch.setChecked(true);
                }
            } else {
                SettingSaver.PERMISSION_EXACT_ALARM.set(false);
            }
            if (binding.alarmSwitch.isChecked()) {
                MainAccessibilityService service = MainApplication.getInstance().getService();
                if (service != null && service.isEnabled()) {
                    service.resetAllAlarm();
                }
            }
        });
        binding.alarmSwitch.setChecked(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() && SettingSaver.PERMISSION_EXACT_ALARM.get());
        binding.alarmSwitch.setVisibility(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? View.VISIBLE : View.GONE);

        // 蓝牙监听
        binding.bluetoothSwitch.setOnSwitchClickListener(v -> {
            if (binding.bluetoothSwitch.isChecked()) {
                binding.bluetoothSwitch.setChecked(false);
                activity.launcherBluetooth((code, data) -> {
                    boolean enable = code == Activity.RESULT_OK;
                    SettingSaver.PERMISSION_BLUETOOTH.set(enable);
                    binding.bluetoothSwitch.setChecked(enable);
                });
            } else {
                SettingSaver.PERMISSION_BLUETOOTH.set(false);
            }
        });
        binding.bluetoothSwitch.setChecked(SettingSaver.PERMISSION_BLUETOOTH.get());

        // 定位
        binding.locationSwitch.setOnSwitchClickListener(v -> {
            if (binding.locationSwitch.isChecked()) {
                binding.locationSwitch.setChecked(false);
                activity.launcherFineLocation((code, data) -> {
                    boolean enable = code == Activity.RESULT_OK;
                    enable &= activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                    SettingSaver.PERMISSION_LOCATION.set(enable);
                    binding.locationSwitch.setChecked(enable);
                    if (enable) {
                        TaskInfoSummary.getInstance().setWifiInfo(AppUtil.getWifiInfo(activity));
                    }
                });
            } else {
                SettingSaver.PERMISSION_LOCATION.set(false);
            }
        });
        binding.locationSwitch.setChecked(SettingSaver.PERMISSION_LOCATION.get());


        // 自动备份
        binding.autoBackupSelect.setOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                View view = group.findViewById(checkedId);
                int index = group.indexOfChild(view);
                SettingSaver.TASK_AUTO_BACKUP.set(index);
                MainAccessibilityService service = MainApplication.getInstance().getService();
                if (service != null && service.isEnabled()) {
                    service.addAlarm();
                }
            }
        });
        binding.autoBackupSelect.checkIndex(SettingSaver.TASK_AUTO_BACKUP.get());
        binding.autoBackupSelect.setOnClickListener(v -> {
            ExportTaskDialog.autoBackup(activity);
            Toast.makeText(activity, R.string.app_setting_auto_backup_tips, Toast.LENGTH_SHORT).show();
        });
        // 手势轨迹
        binding.showTouchSwitch.setOnSwitchClickListener(v -> SettingSaver.TASK_GESTURE_TRACE.set(binding.showTouchSwitch.isChecked()));
        binding.showTouchSwitch.setChecked(SettingSaver.TASK_GESTURE_TRACE.get());

        // 标记目标区域
        binding.showTargetAreaSwitch.setOnSwitchClickListener(v -> SettingSaver.TASK_TARGET_MARK.set(binding.showTargetAreaSwitch.isChecked()));
        binding.showTargetAreaSwitch.setChecked(SettingSaver.TASK_TARGET_MARK.get());

        // 任务提示
        binding.taskTipsSwitch.setOnSwitchClickListener(v -> SettingSaver.TASK_RUNNING_TIPS.set(binding.taskTipsSwitch.isChecked()));
        binding.taskTipsSwitch.setChecked(SettingSaver.TASK_RUNNING_TIPS.get());

        // 详细日志
        binding.detailLogSwitch.setOnSwitchClickListener(v -> SettingSaver.TASK_DETAIL_LOG.set(binding.detailLogSwitch.isChecked()));
        binding.detailLogSwitch.setChecked(SettingSaver.TASK_DETAIL_LOG.get());

        // 日志重置
        binding.resetLogSwitch.setOnSwitchClickListener(v -> SettingSaver.TASK_RESET_DETAIL_LOG.set(binding.resetLogSwitch.isChecked()));
        binding.resetLogSwitch.setChecked(SettingSaver.TASK_RESET_DETAIL_LOG.get());

        // 音量键退出
        binding.volumeButtonExitSwitch.setOnSwitchClickListener(v -> SettingSaver.TASK_VOLUME_KEY_STOP.set(binding.volumeButtonExitSwitch.isChecked()));
        binding.volumeButtonExitSwitch.setChecked(SettingSaver.TASK_VOLUME_KEY_STOP.get());


        // 卡片默认展开状态
        binding.cardTypeSelect.setOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                View view = group.findViewById(checkedId);
                int index = group.indexOfChild(view);
                SettingSaver.BLUEPRINT_CARD_EXPAND_STATE.set(index);
            }
        });
        binding.cardTypeSelect.checkIndex(SettingSaver.BLUEPRINT_CARD_EXPAND_STATE.get());

        // 卡片整理时的间隔
        binding.arrangeCardOffset.setSliderOnChangeListener((slider, value, fromUser) -> SettingSaver.BLUEPRINT_CARD_ARRANGE_PADDING.set((int) value));
        binding.arrangeCardOffset.setValue(SettingSaver.BLUEPRINT_CARD_ARRANGE_PADDING.get());


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
        binding.supportFreeFormSwitch.setOnSwitchClickListener(v -> SettingSaver.FREE_FORM_OPTIMIZE.set(binding.supportFreeFormSwitch.isChecked()));
        binding.supportFreeFormSwitch.setChecked(SettingSaver.FREE_FORM_OPTIMIZE.get());

        // 暗色模式
        binding.themeSelect.setOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                View view = group.findViewById(checkedId);
                int index = group.indexOfChild(view);
                SettingSaver.NIGHT_MODE.set(index);
            }
        });
        binding.themeSelect.checkIndex(SettingSaver.NIGHT_MODE.get());

        // 动态颜色
        binding.dynamicColorSwitch.setOnSwitchClickListener(v -> SettingSaver.DYNAMIC_COLOR.set(activity, binding.dynamicColorSwitch.isChecked()));
        binding.dynamicColorSwitch.setOnClickListener(v -> ColorPickerPreview.showPicker(result -> SettingSaver.DYNAMIC_COLOR_OPTIONS.set(activity, result.getColor()), SettingSaver.DYNAMIC_COLOR_OPTIONS.get()));
        binding.dynamicColorSwitch.setChecked(SettingSaver.DYNAMIC_COLOR.get());

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
        int notificationType = SettingSaver.PERMISSION_NOTIFICATION.get();
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
