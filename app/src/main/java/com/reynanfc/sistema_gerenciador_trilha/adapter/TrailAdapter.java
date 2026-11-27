package com.reynanfc.sistema_gerenciador_trilha.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.databinding.ItemTrailBinding;
import com.reynanfc.sistema_gerenciador_trilha.model.Trail;
import com.reynanfc.sistema_gerenciador_trilha.util.Formatters;

import java.util.ArrayList;
import java.util.List;

/**
 * Mostra as trilhas como cartões. O adapter só liga os dados e repassa os cliques: abrir, renomear
 * e apagar são decididos pela Activity através do {@link Listener}.
 */
public class TrailAdapter extends RecyclerView.Adapter<TrailAdapter.TrailViewHolder> {

    public interface Listener {
        void onOpen(Trail trail);

        void onRename(Trail trail);

        void onDelete(Trail trail);
    }

    private final Listener listener;
    private final List<Trail> trails = new ArrayList<>();

    public TrailAdapter(Listener listener) {
        this.listener = listener;
    }

    @SuppressWarnings("NotifyDataSetChanged") // a lista inteira é recarregada do banco
    public void submit(List<Trail> newTrails) {
        trails.clear();
        trails.addAll(newTrails);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTrailBinding itemBinding = ItemTrailBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TrailViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull TrailViewHolder holder, int position) {
        holder.bind(trails.get(position));
    }

    @Override
    public int getItemCount() {
        return trails.size();
    }

    // O binding do item é criado uma vez por cartão e reaproveitado na rolagem
    class TrailViewHolder extends RecyclerView.ViewHolder {

        private final ItemTrailBinding itemBinding;

        TrailViewHolder(ItemTrailBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        void bind(Trail trail) {
            itemBinding.textTrailName.setText(trail.getName());
            itemBinding.textTrailDate.setText(Formatters.dateTime(trail.getStartTime()));
            itemBinding.textTrailDistance.setText(itemView.getContext().getString(
                    R.string.value_km, Formatters.distanceKm(trail.getDistance())));
            itemBinding.textTrailDuration.setText(Formatters.shortDuration(trail.getDurationMillis()));

            itemView.setOnClickListener(v -> listener.onOpen(trail));
            itemBinding.buttonRename.setOnClickListener(v -> listener.onRename(trail));
            itemBinding.buttonDelete.setOnClickListener(v -> listener.onDelete(trail));
        }
    }
}
