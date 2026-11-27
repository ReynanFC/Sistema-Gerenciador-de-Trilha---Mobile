package com.reynanfc.sistema_gerenciador_trilha.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.model.Trail;
import com.reynanfc.sistema_gerenciador_trilha.util.Formatters;

import java.util.function.Consumer;

/**
 * "Renomear trilha" dialog, shared by the list and the detail screen.
 * It only collects and validates the new name; saving it is up to the caller (the DAO is not used here).
 */
public final class RenameTrailDialog {

    private RenameTrailDialog() {
    }

    public static void show(Context context, Trail trail, Consumer<String> onRenamed) {
        // null parent: the dialog becomes the parent when the view is set on it
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_rename_trail, null);
        TextView textDate = content.findViewById(R.id.text_rename_date);
        TextInputLayout layoutName = content.findViewById(R.id.layout_rename);
        TextInputEditText editName = content.findViewById(R.id.edit_rename);

        textDate.setText(context.getString(R.string.rename_recorded_at, Formatters.dateTime(trail.getStartTime())));
        editName.setText(trail.getName());
        editName.setSelection(trail.getName().length());

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(content)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, null)
                .show();

        // Set after show(): the default positive button always closes the dialog, even with an empty name
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = editName.getText() == null ? "" : editName.getText().toString().trim();
            if (name.isEmpty()) {
                layoutName.setError(context.getString(R.string.rename_name_required));
                return;
            }
            dialog.dismiss();
            if (!name.equals(trail.getName())) {
                onRenamed.accept(name);
            }
        });
    }
}
