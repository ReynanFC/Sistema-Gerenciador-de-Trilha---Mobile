package com.reynanfc.sistema_gerenciador_trilha.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.adapter.TrailAdapter;
import com.reynanfc.sistema_gerenciador_trilha.database.TrailDao;
import com.reynanfc.sistema_gerenciador_trilha.model.Trail;
import com.reynanfc.sistema_gerenciador_trilha.util.Async;
import com.reynanfc.sistema_gerenciador_trilha.util.Formatters;

import java.util.ArrayList;
import java.util.List;

public class TrailListActivity extends AppCompatActivity implements TrailAdapter.Listener {

    private TrailDao trailDao;
    private TrailAdapter adapter;
    private TextView textTrailCount;
    private TextView textTotalDistance;
    private View layoutList;
    private View layoutEmpty;
    private List<Trail> trails = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trail_list);

        trailDao = new TrailDao(this);
        textTrailCount = findViewById(R.id.text_trail_count);
        textTotalDistance = findViewById(R.id.text_total_distance);
        layoutList = findViewById(R.id.layout_list);
        layoutEmpty = findViewById(R.id.layout_empty);

        setupToolbar();
        setupList();
        findViewById(R.id.fab_new_trail).setOnClickListener(v ->
                startActivity(new Intent(this, RecordTrailActivity.class)));
    }

    // onResume (not onCreate): coming back from recording or viewing a trail must show the changes
    @Override
    protected void onResume() {
        super.onResume();
        loadTrails();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete_trails) {
                onDeleteMany();
                return true;
            }
            return false;
        });
    }

    private void setupList() {
        adapter = new TrailAdapter(this);
        RecyclerView recycler = findViewById(R.id.recycler_trails);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
    }

    private void loadTrails() {
        Async.run(trailDao::findAll, this::showTrails,
                error -> Toast.makeText(this, R.string.list_load_error, Toast.LENGTH_LONG).show());
    }

    private void showTrails(List<Trail> loaded) {
        trails = loaded;
        adapter.submit(loaded);

        boolean empty = loaded.isEmpty();
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        layoutList.setVisibility(empty ? View.GONE : View.VISIBLE);

        double totalMeters = 0;
        for (Trail trail : loaded) {
            totalMeters += trail.getDistance();
        }
        textTrailCount.setText(getResources().getQuantityString(
                R.plurals.list_trail_count, loaded.size(), loaded.size()));
        textTotalDistance.setText(getString(R.string.list_total_distance, Formatters.distanceKm(totalMeters)));
    }

    @Override
    public void onOpen(Trail trail) {
    }

    @Override
    public void onRename(Trail trail) {
    }

    @Override
    public void onDelete(Trail trail) {
    }

    private void onDeleteMany() {
    }
}
