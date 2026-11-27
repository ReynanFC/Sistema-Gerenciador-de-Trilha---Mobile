package com.reynanfc.sistema_gerenciador_trilha.dialog;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputLayout;
import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.databinding.DialogDeleteTrailsBinding;
import com.reynanfc.sistema_gerenciador_trilha.model.Trail;
import com.reynanfc.sistema_gerenciador_trilha.util.Formatters;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Diálogo "Apagar trilhas" com três modos: a trilha selecionada, um intervalo de datas ou todas.
 * Como o diálogo de renomear, ele só coleta a escolha; quem chama executa o DAO.
 */
public final class DeleteTrailsDialog {

    public interface Callbacks {
        void deleteOne(Trail trail);

        /** Os dois limites incluídos, em epoch millis: do início do primeiro dia ao fim do último */
        void deleteBetween(long fromMillis, long toMillis);

        void deleteAll();
    }

    private enum Mode { SELECTED, RANGE, ALL }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Context context;
    @Nullable
    private final Trail selected;
    private final Callbacks callbacks;
    private final DialogDeleteTrailsBinding binding;

    private Mode mode;
    @Nullable
    private LocalDate fromDate;
    @Nullable
    private LocalDate toDate;

    /**
     * @param selected a trilha cuja lixeira foi tocada, ou null quando aberto pela app bar
     */
    public static void show(Context context, @Nullable Trail selected, int totalCount, Callbacks callbacks) {
        new DeleteTrailsDialog(context, selected, callbacks).show(totalCount);
    }

    private DeleteTrailsDialog(Context context, @Nullable Trail selected, Callbacks callbacks) {
        this.context = context;
        this.selected = selected;
        this.callbacks = callbacks;
        this.binding = DialogDeleteTrailsBinding.inflate(LayoutInflater.from(context));
        this.mode = selected != null ? Mode.SELECTED : Mode.RANGE;
    }

    private void show(int totalCount) {
        if (selected != null) {
            binding.textDeleteSelected.setText(context.getString(
                    R.string.delete_selected_description, selected.getName(), Formatters.date(selected.getStartTime())));
        } else {
            binding.optionDeleteSelected.setVisibility(View.GONE);
        }
        binding.textDeleteAll.setText(context.getResources()
                .getQuantityString(R.plurals.delete_all_description, totalCount, totalCount));

        binding.optionDeleteSelected.setOnClickListener(v -> select(Mode.SELECTED));
        binding.optionDeleteRange.setOnClickListener(v -> select(Mode.RANGE));
        binding.optionDeleteAll.setOnClickListener(v -> select(Mode.ALL));
        binding.editRangeFrom.setOnClickListener(v -> pickDate(true));
        binding.editRangeTo.setOnClickListener(v -> pickDate(false));
        select(mode);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context).setView(binding.getRoot()).show();
        binding.buttonDeleteCancel.setOnClickListener(v -> dialog.dismiss());
        binding.buttonDeleteConfirm.setOnClickListener(v -> {
            if (confirm()) {
                dialog.dismiss();
            }
        });
    }

    private void select(Mode newMode) {
        mode = newMode;
        setChecked(binding.optionDeleteSelected, binding.radioDeleteSelected, mode == Mode.SELECTED);
        setChecked(binding.optionDeleteRange, binding.radioDeleteRange, mode == Mode.RANGE);
        setChecked(binding.optionDeleteAll, binding.radioDeleteAll, mode == Mode.ALL);
        binding.layoutRangeFields.setVisibility(mode == Mode.RANGE ? View.VISIBLE : View.GONE);
    }

    private void setChecked(MaterialCardView card, MaterialRadioButton radio, boolean checked) {
        card.setChecked(checked);
        radio.setChecked(checked);
    }

    private void pickDate(boolean isFrom) {
        LocalDate initial = isFrom ? fromDate : toDate;
        if (initial == null) {
            initial = LocalDate.now();
        }
        // No DatePickerDialog os meses começam em 0; no LocalDate, em 1
        new DatePickerDialog(context, (picker, year, month, day) -> {
            LocalDate picked = LocalDate.of(year, month + 1, day);
            if (isFrom) {
                fromDate = picked;
                binding.editRangeFrom.setText(picked.format(DATE_FORMAT));
            } else {
                toDate = picked;
                binding.editRangeTo.setText(picked.format(DATE_FORMAT));
            }
            clearRangeErrors();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    /** @return false quando o intervalo é inválido, mantendo o diálogo aberto */
    private boolean confirm() {
        switch (mode) {
            case SELECTED:
                callbacks.deleteOne(selected);
                return true;
            case ALL:
                callbacks.deleteAll();
                return true;
            case RANGE:
            default:
                return confirmRange();
        }
    }

    private boolean confirmRange() {
        TextInputLayout layoutFrom = binding.layoutRangeFrom;
        TextInputLayout layoutTo = binding.layoutRangeTo;
        if (fromDate == null || toDate == null) {
            (fromDate == null ? layoutFrom : layoutTo).setError(context.getString(R.string.delete_pick_dates));
            return false;
        }
        if (fromDate.isAfter(toDate)) {
            layoutTo.setError(context.getString(R.string.delete_inverted_range));
            return false;
        }

        // Dias inteiros no fuso do celular: de 00:00:00.000 do primeiro dia a 23:59:59.999 do último
        ZoneId zone = ZoneId.systemDefault();
        long fromMillis = fromDate.atStartOfDay(zone).toInstant().toEpochMilli();
        long toMillis = toDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1;
        callbacks.deleteBetween(fromMillis, toMillis);
        return true;
    }

    private void clearRangeErrors() {
        binding.layoutRangeFrom.setError(null);
        binding.layoutRangeTo.setError(null);
    }
}
