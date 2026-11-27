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
import com.reynanfc.sistema_gerenciador_trilha.dialog.DeleteTrailsDialog;
import com.reynanfc.sistema_gerenciador_trilha.dialog.RenameTrailDialog;
import com.reynanfc.sistema_gerenciador_trilha.model.Trail;
import com.reynanfc.sistema_gerenciador_trilha.util.Async;
import com.reynanfc.sistema_gerenciador_trilha.util.Formatters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Trail list screen: loads every trail from the database (newest first) and lets the user open,
 * rename or delete them (one, a date range or all). Reloads in onResume to reflect changes.
 * Flow and diagrams: docs/consultar-trilhas.md
 */
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
        Intent intent = new Intent(this, TrailDetailActivity.class);
        intent.putExtra(TrailDetailActivity.EXTRA_TRAIL_ID, trail.getId());
        startActivity(intent);
    }

    @Override
    public void onRename(Trail trail) {
        RenameTrailDialog.show(this, trail, newName ->
                Async.run(() -> trailDao.rename(trail.getId(), newName),
                        renamed -> {
                            Toast.makeText(this, R.string.rename_saved, Toast.LENGTH_SHORT).show();
                            loadTrails();
                        },
                        error -> Toast.makeText(this, R.string.rename_error, Toast.LENGTH_LONG).show()));
    }

    @Override
    public void onDelete(Trail trail) {
        DeleteTrailsDialog.show(this, trail, trails.size(), deleteCallbacks);
    }

    private void onDeleteMany() {
        if (trails.isEmpty()) {
            Toast.makeText(this, R.string.delete_nothing, Toast.LENGTH_SHORT).show();
            return;
        }
        DeleteTrailsDialog.show(this, null, trails.size(), deleteCallbacks);
    }

    // Each mode maps to one DAO method; the points go with the trails through ON DELETE CASCADE
    private final DeleteTrailsDialog.Callbacks deleteCallbacks = new DeleteTrailsDialog.Callbacks() {
        @Override
        public void deleteOne(Trail trail) {
            runDelete(() -> trailDao.delete(trail.getId()));
        }

        @Override
        public void deleteBetween(long fromMillis, long toMillis) {
            runDelete(() -> trailDao.deleteBetween(fromMillis, toMillis));
        }

        @Override
        public void deleteAll() {
            runDelete(trailDao::deleteAll);
        }
    };

    private void runDelete(Callable<Integer> delete) {
        Async.run(delete,
                count -> {
                    Toast.makeText(this, getResources().getQuantityString(
                            R.plurals.delete_done, count, count), Toast.LENGTH_SHORT).show();
                    loadTrails();
                },
                error -> Toast.makeText(this, R.string.delete_error, Toast.LENGTH_LONG).show());
    }
}
