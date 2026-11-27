package com.reynanfc.sistema_gerenciador_trilha.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.adapter.TrailAdapter;
import com.reynanfc.sistema_gerenciador_trilha.database.TrailDao;
import com.reynanfc.sistema_gerenciador_trilha.databinding.ActivityTrailListBinding;
import com.reynanfc.sistema_gerenciador_trilha.dialog.DeleteTrailsDialog;
import com.reynanfc.sistema_gerenciador_trilha.dialog.RenameTrailDialog;
import com.reynanfc.sistema_gerenciador_trilha.model.Trail;
import com.reynanfc.sistema_gerenciador_trilha.util.Async;
import com.reynanfc.sistema_gerenciador_trilha.util.Formatters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Tela Consultar Trilhas: carrega todas as trilhas do banco (mais recentes primeiro) e permite abrir,
 * renomear ou apagar (uma, por intervalo de datas ou todas). Recarrega no onResume para refletir mudanças.
 * Fluxo e diagramas: docs/consultar-trilhas.md
 */
public class TrailListActivity extends AppCompatActivity implements TrailAdapter.Listener {

    private TrailDao trailDao;
    private TrailAdapter adapter;
    private ActivityTrailListBinding binding;
    private List<Trail> trails = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTrailListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        trailDao = new TrailDao(this);

        setupToolbar();
        setupList();
        binding.fabNewTrail.setOnClickListener(v ->
                startActivity(new Intent(this, RecordTrailActivity.class)));
    }

    // onResume (e não onCreate): ao voltar da gravação ou da visualização, a lista precisa mostrar as mudanças
    @Override
    protected void onResume() {
        super.onResume();
        loadTrails();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete_trails) {
                onDeleteMany();
                return true;
            }
            return false;
        });
    }

    private void setupList() {
        adapter = new TrailAdapter(this);
        binding.recyclerTrails.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerTrails.setAdapter(adapter);
    }

    private void loadTrails() {
        Async.run(trailDao::findAll, this::showTrails,
                error -> Toast.makeText(this, R.string.list_load_error, Toast.LENGTH_LONG).show());
    }

    private void showTrails(List<Trail> loaded) {
        trails = loaded;
        adapter.submit(loaded);

        boolean empty = loaded.isEmpty();
        binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.layoutList.setVisibility(empty ? View.GONE : View.VISIBLE);

        double totalMeters = 0;
        for (Trail trail : loaded) {
            totalMeters += trail.getDistance();
        }
        binding.textTrailCount.setText(getResources().getQuantityString(
                R.plurals.list_trail_count, loaded.size(), loaded.size()));
        binding.textTotalDistance.setText(getString(R.string.list_total_distance, Formatters.distanceKm(totalMeters)));
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

    // Cada modo corresponde a um método do DAO; os pontos vão junto com as trilhas pelo ON DELETE CASCADE
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
