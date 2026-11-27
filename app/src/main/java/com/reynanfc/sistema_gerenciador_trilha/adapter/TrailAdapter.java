package com.reynanfc.sistema_gerenciador_trilha.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.model.Trail;
import com.reynanfc.sistema_gerenciador_trilha.util.Formatters;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the trails as cards. The adapter only binds data and forwards clicks: opening, renaming
 * and deleting are decided by the Activity through {@link Listener}.
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

    @SuppressWarnings("NotifyDataSetChanged") // the whole list is reloaded from the database
    public void submit(List<Trail> newTrails) {
        trails.clear();
        trails.addAll(newTrails);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trail, parent, false);
        return new TrailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrailViewHolder holder, int position) {
        holder.bind(trails.get(position));
    }

    @Override
    public int getItemCount() {
        return trails.size();
    }

    // Holds the item views so findViewById runs once per card, not on every scroll
    class TrailViewHolder extends RecyclerView.ViewHolder {

        private final TextView textName;
        private final TextView textDate;
        private final TextView textDistance;
        private final TextView textDuration;

        TrailViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_trail_name);
            textDate = itemView.findViewById(R.id.text_trail_date);
            textDistance = itemView.findViewById(R.id.text_trail_distance);
            textDuration = itemView.findViewById(R.id.text_trail_duration);
        }

        void bind(Trail trail) {
            textName.setText(trail.getName());
            textDate.setText(Formatters.dateTime(trail.getStartTime()));
            textDistance.setText(itemView.getContext().getString(
                    R.string.value_km, Formatters.distanceKm(trail.getDistance())));
            textDuration.setText(Formatters.shortDuration(trail.getDurationMillis()));

            itemView.setOnClickListener(v -> listener.onOpen(trail));
            itemView.findViewById(R.id.button_rename).setOnClickListener(v -> listener.onRename(trail));
            itemView.findViewById(R.id.button_delete).setOnClickListener(v -> listener.onDelete(trail));
        }
    }
}
