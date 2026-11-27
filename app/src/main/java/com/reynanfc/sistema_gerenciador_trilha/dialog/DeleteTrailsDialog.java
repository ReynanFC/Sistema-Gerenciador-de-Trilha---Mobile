package com.reynanfc.sistema_gerenciador_trilha.dialog;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.reynanfc.sistema_gerenciador_trilha.R;
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
    private final View content;

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
        // Pai null: o diálogo vira o pai quando a view é colocada nele
        this.content = LayoutInflater.from(context).inflate(R.layout.dialog_delete_trails, null);
        this.mode = selected != null ? Mode.SELECTED : Mode.RANGE;
    }

    private void show(int totalCount) {
        View optionSelected = content.findViewById(R.id.option_delete_selected);
        if (selected != null) {
            ((TextView) content.findViewById(R.id.text_delete_selected)).setText(context.getString(
                    R.string.delete_selected_description, selected.getName(), Formatters.date(selected.getStartTime())));
        } else {
            optionSelected.setVisibility(View.GONE);
        }
        ((TextView) content.findViewById(R.id.text_delete_all)).setText(context.getResources()
                .getQuantityString(R.plurals.delete_all_description, totalCount, totalCount));

        optionSelected.setOnClickListener(v -> select(Mode.SELECTED));
        content.findViewById(R.id.option_delete_range).setOnClickListener(v -> select(Mode.RANGE));
        content.findViewById(R.id.option_delete_all).setOnClickListener(v -> select(Mode.ALL));
        content.findViewById(R.id.edit_range_from).setOnClickListener(v -> pickDate(true));
        content.findViewById(R.id.edit_range_to).setOnClickListener(v -> pickDate(false));
        select(mode);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context).setView(content).show();
        content.findViewById(R.id.button_delete_cancel).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.button_delete_confirm).setOnClickListener(v -> {
            if (confirm()) {
                dialog.dismiss();
            }
        });
    }

    private void select(Mode newMode) {
        mode = newMode;
        setChecked(R.id.option_delete_selected, R.id.radio_delete_selected, mode == Mode.SELECTED);
        setChecked(R.id.option_delete_range, R.id.radio_delete_range, mode == Mode.RANGE);
        setChecked(R.id.option_delete_all, R.id.radio_delete_all, mode == Mode.ALL);
        content.findViewById(R.id.layout_range_fields).setVisibility(mode == Mode.RANGE ? View.VISIBLE : View.GONE);
    }

    private void setChecked(int cardId, int radioId, boolean checked) {
        ((MaterialCardView) content.findViewById(cardId)).setChecked(checked);
        ((MaterialRadioButton) content.findViewById(radioId)).setChecked(checked);
    }

    private void pickDate(boolean isFrom) {
        LocalDate initial = isFrom ? fromDate : toDate;
        if (initial == null) {
            initial = LocalDate.now();
        }
        // No DatePickerDialog os meses começam em 0; no LocalDate, em 1
        new DatePickerDialog(context, (picker, year, month, day) -> {
            LocalDate picked = LocalDate.of(year, month + 1, day);
            int fieldId = isFrom ? R.id.edit_range_from : R.id.edit_range_to;
            if (isFrom) {
                fromDate = picked;
            } else {
                toDate = picked;
            }
            ((TextInputEditText) content.findViewById(fieldId)).setText(picked.format(DATE_FORMAT));
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
        TextInputLayout layoutFrom = content.findViewById(R.id.layout_range_from);
        TextInputLayout layoutTo = content.findViewById(R.id.layout_range_to);
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
        ((TextInputLayout) content.findViewById(R.id.layout_range_from)).setError(null);
        ((TextInputLayout) content.findViewById(R.id.layout_range_to)).setError(null);
    }
}
