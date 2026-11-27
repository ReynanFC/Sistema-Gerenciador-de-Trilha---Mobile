package com.reynanfc.sistema_gerenciador_trilha.dialog;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.databinding.DialogRenameTrailBinding;
import com.reynanfc.sistema_gerenciador_trilha.model.Trail;
import com.reynanfc.sistema_gerenciador_trilha.util.Formatters;

import java.util.function.Consumer;

/**
 * Diálogo "Renomear trilha", compartilhado pela lista e pela tela Visualizar.
 * Só coleta e valida o novo nome; salvar fica a cargo de quem chama (o DAO não é usado aqui).
 */
public final class RenameTrailDialog {

    private RenameTrailDialog() {
    }

    public static void show(Context context, Trail trail, Consumer<String> onRenamed) {
        DialogRenameTrailBinding binding = DialogRenameTrailBinding.inflate(LayoutInflater.from(context));
        TextInputLayout layoutName = binding.layoutRename;
        TextInputEditText editName = binding.editRename;

        binding.textRenameDate.setText(context.getString(R.string.rename_recorded_at, Formatters.dateTime(trail.getStartTime())));
        editName.setText(trail.getName());
        editName.setSelection(trail.getName().length());

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(binding.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, null)
                .show();

        // Definido depois do show(): o botão positivo padrão sempre fecha o diálogo, mesmo com o nome vazio
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
