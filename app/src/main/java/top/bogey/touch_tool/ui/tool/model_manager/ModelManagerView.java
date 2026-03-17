package top.bogey.touch_tool.ui.tool.model_manager;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.save.model.ModelSaver;
import top.bogey.touch_tool.databinding.ViewToolModelManagerBinding;
import top.bogey.touch_tool.ui.MainActivity;
import top.bogey.touch_tool.utils.ThreadUtil;

public class ModelManagerView extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) requireActivity();

        ViewToolModelManagerBinding binding = ViewToolModelManagerBinding.inflate(inflater, container, false);
        binding.toolBar.setNavigationOnClickListener(v -> activity.getOnBackPressedDispatcher().onBackPressed());

        ModelManagerAdapter adapter = new ModelManagerAdapter();
        binding.models.setAdapter(adapter);
        adapter.refresh();

        binding.addButton.setOnClickListener(v -> activity.launcherOpenDocument((code, intent) -> {
            if (code == Activity.RESULT_OK) {
                Toast.makeText(activity, R.string.model_manager_importing, Toast.LENGTH_SHORT).show();
                ThreadUtil.execute(() -> {
                    boolean result = ModelSaver.getInstance().importModel(activity, intent.getData());
                    binding.addButton.post(() -> {
                        if (result) {
                            Toast.makeText(activity, R.string.model_manager_import_success, Toast.LENGTH_SHORT).show();
                            adapter.refresh();
                        } else {
                            Toast.makeText(activity, R.string.model_manager_import_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        }, "application/zip"));

        return binding.getRoot();
    }
}
