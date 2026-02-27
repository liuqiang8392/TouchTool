package top.bogey.touch_tool.ui.blueprint.selecter.select_action;

import android.content.Context;
import android.graphics.Point;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.divider.MaterialDivider;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionInfo;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.task.CustomStartAction;
import top.bogey.touch_tool.bean.action.task.ExecuteTaskAction;
import top.bogey.touch_tool.bean.action.variable.GetOrSetVariableAction;
import top.bogey.touch_tool.bean.action.variable.GetVariableAction;
import top.bogey.touch_tool.bean.action.variable.SetVariableAction;
import top.bogey.touch_tool.bean.other.Usage;
import top.bogey.touch_tool.bean.pin.PinInfo;
import top.bogey.touch_tool.bean.pin.pin_objects.PinType;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.save.variable.VariableSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.bean.task.Variable;
import top.bogey.touch_tool.databinding.DialogSelectActionNormalItemBinding;
import top.bogey.touch_tool.databinding.DialogSelectActionTaskItemBinding;
import top.bogey.touch_tool.databinding.DialogSelectActionVariableItemBinding;
import top.bogey.touch_tool.ui.blueprint.BlueprintView;
import top.bogey.touch_tool.ui.blueprint.card.ActionCard;
import top.bogey.touch_tool.ui.custom.EditTaskDialog;
import top.bogey.touch_tool.ui.custom.EditVariableDialog;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.callback.BooleanResultCallback;
import top.bogey.touch_tool.utils.callback.ResultCallback;

public class SelectActionItemRecyclerViewAdapter extends RecyclerView.Adapter<SelectActionItemRecyclerViewAdapter.ViewHolder> {
    private final static Map<PinType, List<PinInfo>> PIN_INFO_MAP = PinInfo.getCustomPinInfoMap();

    private final SelectActionDialog dialog;
    private final ResultCallback<Action> callback;
    private final boolean canSelectAll;

    private List<Object> data = new ArrayList<>();

    public SelectActionItemRecyclerViewAdapter(SelectActionDialog dialog, ResultCallback<Action> callback, boolean canSelectAll) {
        this.dialog = dialog;
        this.callback = callback;
        this.canSelectAll = canSelectAll;
    }

    public SelectActionItemRecyclerViewAdapter(SelectActionDialog dialog, ResultCallback<Action> callback) {
        this(dialog, callback, false);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return switch (viewType) {
            case 1 -> new ViewHolder(DialogSelectActionTaskItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            case 2 -> new ViewHolder(DialogSelectActionVariableItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            default -> new ViewHolder(DialogSelectActionNormalItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        };
    }

    @Override
    public int getItemViewType(int position) {
        Object object = data.get(position);
        if (object instanceof Task) return 1;
        if (object instanceof Variable) return 2;
        return 0;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.refresh(data.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static String getObjectTitle(Object object) {
        if (object instanceof Task task) {
            return task.getTitle();
        }
        if (object instanceof Variable var) {
            return var.getTitle();
        }
        if (object instanceof ActionType actionType) {
            ActionInfo info = ActionInfo.getActionInfo(actionType);
            if (info != null) {
                return info.getTitle();
            }
        }
        if (object instanceof ActionCard card) return card.getAction().getTitle();
        return "";
    }

    public static String getObjectDesc(Object object) {
        if (object instanceof Task task) return task.getDescription();
        if (object instanceof Variable var) return var.getDescription();
        if (object instanceof ActionType actionType) {
            ActionInfo info = ActionInfo.getActionInfo(actionType);
            if (info != null) {
                return info.getDescription();
            }
        }
        if (object instanceof ActionCard card) return card.getAction().getDescription();
        return "";
    }

    @DrawableRes
    public static int getObjectIcon(Object object) {
        if (object instanceof Task) return R.drawable.icon_assignment;
        if (object instanceof Variable) return R.drawable.icon_note_stack;
        if (object instanceof ActionType actionType) {
            ActionInfo info = ActionInfo.getActionInfo(actionType);
            if (info != null) {
                return info.getIcon();
            }
        }
        if (object instanceof ActionCard card) return getObjectIcon(card.getAction().getType());
        return 0;
    }

    public static String getUsageTitlePath(Usage usage) {
        StringBuilder builder = new StringBuilder();
        Task task = usage.task();
        while (task != null) {
            builder.insert(0, task.getTitle()).insert(0, " > ");
            task = task.getParent();
        }
        for (Point point : usage.points()) {
            builder.append(" (").append(point.x).append(",").append(point.y).append(")");
        }
        return builder.toString().substring(1);
    }

    public static LinearLayout getTipsLinearLayout(Context context, List<Usage> usages, @StringRes int tipString) {
        LinearLayout linearLayout = new LinearLayout(context);

        MaterialTextView tips = new MaterialTextView(context);
        tips.setText(context.getString(tipString, usages.size()));
        linearLayout.addView(tips);
        MaterialDivider divider = new MaterialDivider(context);
        DisplayUtil.setViewWidth(divider, ViewGroup.LayoutParams.MATCH_PARENT);
        DisplayUtil.setViewHeight(divider, (int) DisplayUtil.dp2px(context, 1));
        linearLayout.addView(divider);

        linearLayout.setOrientation(LinearLayout.VERTICAL);
        usages.forEach(usage -> {
            MaterialTextView title = new MaterialTextView(context);
            title.setTextSize(12);
            title.setText(usage.task().getTitle());
            linearLayout.addView(title);

            MaterialTextView path = new MaterialTextView(context);
            path.setTextSize(8);
            path.setText(getUsageTitlePath(usage));
            path.setSingleLine(false);
            linearLayout.addView(path);

            MaterialDivider div = new MaterialDivider(context);
            DisplayUtil.setViewWidth(div, ViewGroup.LayoutParams.MATCH_PARENT);
            DisplayUtil.setViewHeight(div, (int) DisplayUtil.dp2px(context, 1));
            linearLayout.addView(div);
        });
        return linearLayout;
    }

    public void setData(List<Object> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public void addData(Object object) {
        data.add(object);
        notifyItemInserted(data.size() - 1);
    }

    private void deleteSameObject(Object object) {
        dialog.deleteSameObject(object);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private DialogSelectActionNormalItemBinding normalBinding;
        private DialogSelectActionTaskItemBinding taskBinding;
        private DialogSelectActionVariableItemBinding variableBinding;
        private final Context context;
        private boolean needDelete = false;

        public ViewHolder(@NonNull DialogSelectActionNormalItemBinding binding) {
            super(binding.getRoot());

            this.normalBinding = binding;
            context = binding.getRoot().getContext();

            binding.getRoot().setOnClickListener(v -> {
                int index = getBindingAdapterPosition();
                Object object = data.get(index);
                Action action = null;
                if (object instanceof ActionType actionType) {
                    ActionInfo info = ActionInfo.getActionInfo(actionType);
                    if (info != null) action = info.newInstance();
                }
                if (callback != null) callback.onResult(action);
            });
        }

        public ViewHolder(@NonNull DialogSelectActionTaskItemBinding binding) {
            super(binding.getRoot());
            this.taskBinding = binding;
            context = binding.getRoot().getContext();

            binding.settingButton.setOnClickListener(v -> {
                int index = getBindingAdapterPosition();
                Object object = data.get(index);

                if (object instanceof Task task) {
                    BlueprintView.tryPushStack(task);
                    dialog.dismiss();
                }
            });

            binding.editButton.setOnClickListener(v -> {
                int index = getBindingAdapterPosition();
                Object object = data.get(index);

                if (object instanceof Task editTask) {
                    EditTaskDialog dialog = new EditTaskDialog(context, editTask);
                    dialog.setTitle(R.string.task_update);
                    dialog.setCallback(result -> {
                        if (result) {
                            editTask.save();
                            notifyItemChanged(index);
                        }
                    });
                    dialog.show();
                }
            });

            binding.copyButton.setOnClickListener(v -> {
                int index = getBindingAdapterPosition();
                Object object = data.get(index);
                if (object instanceof Task task) {
                    Task copy = task.newCopy();
                    copy.setTitle(context.getString(R.string.copy_title, task.getTitle()));
                    dialog.setCopyObject(copy);
                }
            });

            binding.deleteButton.setOnClickListener(v -> {
                if (needDelete) {
                    int index = getBindingAdapterPosition();
                    Object object = data.get(index);

                    if (object instanceof Task task) {
                        deleteTask(task, result -> {
                            Task parent = task.getParent();
                            if (parent == null) {
                                TaskSaver.getInstance().removeTask(task.getId());
                            } else {
                                parent.removeTask(task.getId());
                            }
                            data.remove(index);
                            notifyItemRemoved(index);
                            deleteSameObject(object);
                        });
                    }
                } else {
                    binding.deleteButton.setChecked(true);
                    needDelete = true;
                    binding.deleteButton.postDelayed(() -> {
                        binding.deleteButton.setChecked(false);
                        needDelete = false;
                    }, 1500);
                }
            });

            binding.getRoot().setOnClickListener(v -> {
                int index = getBindingAdapterPosition();
                Object object = data.get(index);
                Action action = null;

                if (object instanceof Task task) {
                    ExecuteTaskAction executeTaskAction = new ExecuteTaskAction();
                    executeTaskAction.setTask(task);
                    action = executeTaskAction;
                }

                if (callback != null) callback.onResult(action);
            });
        }

        private ViewHolder(@NonNull DialogSelectActionVariableItemBinding binding) {
            super(binding.getRoot());
            this.variableBinding = binding;
            context = binding.getRoot().getContext();

            binding.keySlot.setOnClickListener(v -> {
                ListPopupWindow popup = new ListPopupWindow(context);
                List<PinInfo> pinInfoList = new ArrayList<>();
                ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.widget_textview_item);
                PIN_INFO_MAP.forEach((pinType, infoList) -> infoList.forEach(info -> {
                    adapter.add(info.getTitle());
                    pinInfoList.add(info);
                }));
                popup.setAdapter(adapter);
                popup.setAnchorView(binding.keySlot);
                popup.setModal(true);
                popup.setWidth(DisplayUtil.measureArrayAdapterContentWidth(context, adapter));
                popup.setOnItemClickListener((parent, view, position, id) -> {
                    PinInfo pinInfo = pinInfoList.get(position);
                    binding.keySlot.setText(pinInfo.getTitle());
                    int index = getBindingAdapterPosition();
                    Variable var = (Variable) data.get(index);
                    var.setKeyPinInfo(pinInfo);
                    var.save();
                    popup.dismiss();
                });
                popup.show();
            });

            binding.typeSpinner.setOnClickListener(v -> {
                ListPopupWindow popup = new ListPopupWindow(context);
                String[] array = context.getResources().getStringArray(R.array.pin_simple_type);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.widget_textview_item, array);
                popup.setAdapter(adapter);
                popup.setAnchorView(binding.typeSpinner);
                popup.setModal(true);
                popup.setWidth(DisplayUtil.measureArrayAdapterContentWidth(context, adapter));
                popup.setOnItemClickListener((parent, view, position, id) -> {
                    binding.typeSpinner.setText(array[position]);
                    int index = getBindingAdapterPosition();
                    Variable var = (Variable) data.get(index);
                    if (var.setType(Variable.VariableType.values()[position])) {
                        var.save();
                        notifyItemChanged(index);
                    }
                    popup.dismiss();
                });
                popup.show();
            });

            binding.valueSlot.setOnClickListener(v -> {
                ListPopupWindow popup = new ListPopupWindow(context);
                List<PinInfo> pinInfoList = new ArrayList<>();
                ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.widget_textview_item);
                PIN_INFO_MAP.forEach((pinType, infoList) -> infoList.forEach(info -> {
                    adapter.add(info.getTitle());
                    pinInfoList.add(info);
                }));
                popup.setAdapter(adapter);
                popup.setAnchorView(binding.valueSlot);
                popup.setModal(true);
                popup.setWidth(DisplayUtil.measureArrayAdapterContentWidth(context, adapter));
                popup.setOnItemClickListener((parent, view, position, id) -> {
                    PinInfo pinInfo = pinInfoList.get(position);
                    binding.valueSlot.setText(pinInfo.getTitle());
                    int index = getBindingAdapterPosition();
                    Variable var = (Variable) data.get(index);
                    var.setValuePinInfo(pinInfo);
                    var.save();
                    popup.dismiss();
                });
                popup.show();
            });

            binding.editButton.setOnClickListener(v -> {
                int index = getBindingAdapterPosition();
                Object object = data.get(index);

                if (object instanceof Variable var) {
                    EditVariableDialog dialog = new EditVariableDialog(context, var);
                    dialog.setTitle(R.string.variable_update);
                    dialog.setCallback(result -> {
                        if (result) {
                            var.save();
                            notifyItemChanged(index);
                        }
                    });
                    dialog.show();
                }
            });

            binding.copyButton.setOnClickListener(v -> {
                int index = getBindingAdapterPosition();
                Object object = data.get(index);

                if (object instanceof Variable var) {
                    Variable copy = var.newCopy();
                    copy.setTitle(context.getString(R.string.copy_title, var.getTitle()));
                    dialog.setCopyObject(copy);
                }
            });

            binding.deleteButton.setOnClickListener(v -> {
                if (needDelete) {
                    int index = getBindingAdapterPosition();
                    Object object = data.get(index);

                    if (object instanceof Variable var) {
                        deleteVariable(var, result -> {
                            if (result) {
                                Task parent = var.getParent();
                                if (parent == null) {
                                    VariableSaver.getInstance().removeVar(var.getId());
                                } else {
                                    parent.removeVariable(var.getId());
                                }
                                data.remove(index);
                                notifyItemRemoved(index);
                                deleteSameObject(object);
                            }
                        });
                    }
                } else {
                    binding.deleteButton.setChecked(true);
                    needDelete = true;
                    binding.deleteButton.postDelayed(() -> {
                        binding.deleteButton.setChecked(false);
                        needDelete = false;
                    }, 1500);
                }
            });

            binding.getRoot().setOnClickListener(v -> {
                int index = getBindingAdapterPosition();
                Object object = data.get(index);
                Action action = null;

                if (object instanceof Variable var) {
                    action = new GetVariableAction(var);
//                    action = new GetOrSetVariableAction(var);
                }

                if (callback != null) callback.onResult(action);
            });
        }

        public void refresh(Object object) {
            String desc = getObjectDesc(object);

            if (normalBinding != null) {
                normalBinding.taskName.setText(getObjectTitle(object));
                normalBinding.icon.setImageResource(getObjectIcon(object));

                if (desc != null && !desc.isEmpty()) {
                    normalBinding.taskDesc.setVisibility(View.VISIBLE);
                    normalBinding.taskDesc.setText(desc);
                } else {
                    normalBinding.taskDesc.setVisibility(View.GONE);
                }
            } else if (taskBinding != null && object instanceof Task task) {
                taskBinding.taskName.setText(getObjectTitle(task));

                if (task.getParent() == null) {
                    taskBinding.icon.setImageResource(R.drawable.icon_globe);
                } else {
                    taskBinding.icon.setImageResource(R.drawable.icon_assignment);
                }

                if (desc != null && !desc.isEmpty()) {
                    taskBinding.taskDesc.setVisibility(View.VISIBLE);
                    taskBinding.taskDesc.setText(desc);
                } else {
                    taskBinding.taskDesc.setVisibility(View.GONE);
                }

                Task parentTask = dialog.task.upFindTask(task.getId());
                boolean isUsable = task.equals(parentTask) || task.getParent() == null;

                if (task.equals(dialog.task) || dialog.task.isMyParent(task.getId())) {
                    taskBinding.deleteButton.setVisibility(View.GONE);
                } else {
                    taskBinding.deleteButton.setVisibility(View.VISIBLE);
                }

                if (!canSelectAll && (task.getActions(CustomStartAction.class).isEmpty() || !isUsable)) {
                    taskBinding.getRoot().setEnabled(false);
                    taskBinding.getRoot().setAlpha(0.5f);
                } else {
                    taskBinding.getRoot().setEnabled(true);
                    taskBinding.getRoot().setAlpha(1f);
                }
            } else if (variableBinding != null && object instanceof Variable var) {
                variableBinding.taskName.setText(getObjectTitle(var));

                if (var.getParent() == null) {
                    variableBinding.icon.setImageResource(R.drawable.icon_globe);
                } else {
                    variableBinding.icon.setImageResource(R.drawable.icon_note_stack);
                }

                if (desc != null && !desc.isEmpty()) {
                    variableBinding.taskDesc.setVisibility(View.VISIBLE);
                    variableBinding.taskDesc.setText(desc);
                } else {
                    variableBinding.taskDesc.setVisibility(View.GONE);
                }

                Variable variable = dialog.task.upFindVariable(var.getId());
                boolean isUsable = var.equals(variable) || var.getParent() == null;
                if (!isUsable) {
                    variableBinding.getRoot().setEnabled(false);
                    variableBinding.getRoot().setAlpha(0.5f);
                } else {
                    variableBinding.getRoot().setEnabled(true);
                    variableBinding.getRoot().setAlpha(1f);
                }

                PinInfo keyPinInfo = var.getKeyPinInfo();
                if (keyPinInfo != null) variableBinding.keySlot.setText(keyPinInfo.getTitle());

                int[] array = new int[]{R.drawable.icon_variables, R.drawable.icon_data_array, R.drawable.icon_map};
                variableBinding.typeSpinner.setIconResource(array[var.getType().ordinal()]);

                if (var.getType() == Variable.VariableType.MAP) {
                    variableBinding.valueSlot.setVisibility(View.VISIBLE);
                    PinInfo valuePinInfo = var.getValuePinInfo();
                    if (valuePinInfo != null) variableBinding.valueSlot.setText(valuePinInfo.getTitle());
                } else {
                    variableBinding.valueSlot.setVisibility(View.GONE);
                }
            }
        }

        private void deleteTask(Task task, BooleanResultCallback callback) {
            List<Usage> usages;
            Task parent = task.getParent();
            if (parent == null) {
                usages = TaskSaver.getInstance().getTaskUses(task.getId());
            } else {
                usages = parent.getTaskUses(task.getId());
            }

            if (!usages.isEmpty()) {
                LinearLayout linearLayout = getTipsLinearLayout(context, usages, R.string.task_delete_tips);
                new MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.remove_task)
                        .setView(linearLayout)
                        .setPositiveButton(R.string.enter, null)
                        .setNegativeButton(R.string.force_delete, (dialog, which) -> callback.onResult(true))
                        .show();

                int px = (int) DisplayUtil.dp2px(context, 32);
                DisplayUtil.setViewMargin(linearLayout, px, px / 2, px, px / 2);
            } else {
                callback.onResult(true);
            }
        }

        private void deleteVariable(Variable var, BooleanResultCallback callback) {
            List<Usage> usages;
            Task parent = var.getParent();
            if (parent == null) {
                usages = VariableSaver.getInstance().getVarUses(var.getId());
            } else {
                usages = parent.getVariableUses(var.getId());
            }

            if (!usages.isEmpty()) {
                LinearLayout linearLayout = getTipsLinearLayout(context, usages, R.string.variable_delete_tips);
                new MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.remove_task)
                        .setView(linearLayout)
                        .setPositiveButton(R.string.enter, null)
//                        .setNegativeButton(R.string.force_delete, (dialog, which) -> callback.onResult(true))
                        .show();

                int px = (int) DisplayUtil.dp2px(context, 32);
                DisplayUtil.setViewMargin(linearLayout, px, px / 2, px, px / 2);
            } else {
                callback.onResult(true);
            }
        }
    }
}
