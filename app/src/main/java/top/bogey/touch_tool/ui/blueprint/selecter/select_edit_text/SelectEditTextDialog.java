package top.bogey.touch_tool.ui.blueprint.selecter.select_edit_text;

import android.content.Context;
import android.text.Editable;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.databinding.DialogFullScreenEditBinding;
import top.bogey.touch_tool.ui.MainActivity;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.callback.StringResultCallback;
import top.bogey.touch_tool.utils.listener.TextChangedListener;

public class SelectEditTextDialog extends BottomSheetDialog {

    public SelectEditTextDialog(@NonNull Context context, TextInputEditText from, StringResultCallback callback) {
        super(context);
        DialogFullScreenEditBinding binding = DialogFullScreenEditBinding.inflate(LayoutInflater.from(context));
        setContentView(binding.getRoot());

        binding.editText.setText(from.getText());
        binding.editText.setEnabled(from.isEnabled());
        if (from.getMaxLines() <= 1) binding.editText.setSingleLine();
        else binding.editText.setSingleLine(false);
        binding.editText.setInputType(from.getInputType());

        binding.editText.addTextChangedListener(new TextChangedListener() {
            @Override
            public void afterTextChanged(Editable s) {
                callback.onResult(s.toString());
            }
        });

        binding.editText.requestFocus();

        Size size = DisplayUtil.getScreenSize(context);
        DisplayUtil.setViewHeight(binding.getRoot(), (int) (size.getHeight() * 0.7f));
        DisplayUtil.setViewWidth(binding.getRoot(), ViewGroup.LayoutParams.MATCH_PARENT);

        MainActivity activity = MainApplication.getInstance().getActivity();
        View decorView = activity.getWindow().getDecorView();
        int width = decorView.getWidth();
        int height = decorView.getHeight();

        BottomSheetBehavior<FrameLayout> behavior = getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        boolean portrait = DisplayUtil.isPortrait(context);
        if (portrait) {
            DisplayUtil.setViewHeight(binding.getRoot(), (int) (height * 0.7f));
            DisplayUtil.setViewWidth(binding.getRoot(), ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            DisplayUtil.setViewHeight(binding.getRoot(), (int) (height * 0.8f));
            behavior.setMaxWidth(width);
        }
    }
}
