package com.reynanfc.sistema_gerenciador_trilha.activities;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.database.TrailDao;
import com.reynanfc.sistema_gerenciador_trilha.databinding.ActivityTrailDetailBinding;
import com.reynanfc.sistema_gerenciador_trilha.dialog.RenameTrailDialog;
import com.reynanfc.sistema_gerenciador_trilha.model.Trail;
import com.reynanfc.sistema_gerenciador_trilha.model.TrackPoint;
import com.reynanfc.sistema_gerenciador_trilha.util.Async;
import com.reynanfc.sistema_gerenciador_trilha.util.Formatters;
import com.reynanfc.sistema_gerenciador_trilha.util.MapIcons;
import com.reynanfc.sistema_gerenciador_trilha.util.SettingsPrefs;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela Visualizar Trilha: carrega uma trilha com seus pontos pelo id, desenha o percurso com os
 * pinos de início e fim, enquadra a câmera nele e mostra o resumo. O nome também pode ser editado aqui.
 * Fluxo e diagramas: docs/visualizar-trilha.md
 */
public class TrailDetailActivity extends AppCompatActivity {

    /** Id da trilha a exibir (long). Só o id vai no Intent; a trilha é carregada do banco */
    public static final String EXTRA_TRAIL_ID = "com.reynanfc.sistema_gerenciador_trilha.EXTRA_TRAIL_ID";

    private static final long NO_ID = -1;
    private static final float SINGLE_POINT_ZOOM = 17f;

    private ActivityTrailDetailBinding binding;
    private long trailId;
    private SettingsPrefs settingsPrefs;
    private TrailDao trailDao;
    @Nullable
    private GoogleMap googleMap;
    @Nullable
    private Trail trail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        trailId = getIntent().getLongExtra(EXTRA_TRAIL_ID, NO_ID);
        if (trailId == NO_ID) {
            closeWithError();
            return;
        }

        EdgeToEdge.enable(this);
        binding = ActivityTrailDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        settingsPrefs = new SettingsPrefs(this);
        trailDao = new TrailDao(this);

        setupWindowInsets();
        binding.buttonDetailBack.setOnClickListener(v -> finish());
        binding.buttonDetailEdit.setOnClickListener(v -> renameTrail());
        showSettingsSummary();
        setupMap();
        loadTrail();
    }

    private void setupMap() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(map -> {
                googleMap = map;
                googleMap.setMapType(settingsPrefs.getMapType() == SettingsPrefs.MapType.SATELLITE
                        ? GoogleMap.MAP_TYPE_SATELLITE : GoogleMap.MAP_TYPE_NORMAL);
                googleMap.getUiSettings().setMapToolbarEnabled(false);
                drawRouteWhenReady();
            });
        }
    }

    private void loadTrail() {
        Async.run(() -> trailDao.findById(trailId),
                loaded -> {
                    if (loaded == null) {
                        closeWithError();
                        return;
                    }
                    trail = loaded;
                    showInfo(loaded);
                    drawRouteWhenReady();
                },
                error -> closeWithError());
    }

    private void showInfo(Trail trail) {
        binding.textDetailName.setText(trail.getName());
        binding.textDetailStart.setText(Formatters.dateTime(trail.getStartTime()));
        binding.textDetailDistance.setText(
                getString(R.string.value_km, Formatters.distanceKm(trail.getDistance())));
        binding.textDetailDuration.setText(Formatters.shortDuration(trail.getDurationMillis()));
        binding.textDetailAverageSpeed.setText(
                getString(R.string.value_kmh, Formatters.speedKmh(trail.getAverageSpeed())));
        binding.textDetailMaxSpeed.setText(
                getString(R.string.value_kmh, Formatters.speedKmh(trail.getMaxSpeed())));
    }

    private void showSettingsSummary() {
        binding.textDetailSettings.setText(getString(R.string.record_settings_summary,
                getString(settingsPrefs.getMapType().labelResId),
                getString(settingsPrefs.getNavigationMode().labelResId)));

        // A linha da legenda recebe a mesma cor do percurso
        Drawable legendLine = ContextCompat.getDrawable(this, R.drawable.legend_line_route);
        if (legendLine != null) {
            legendLine = legendLine.mutate();
            legendLine.setTint(ContextCompat.getColor(this, routeColorRes()));
            binding.textLegendRoute
                    .setCompoundDrawablesRelativeWithIntrinsicBounds(legendLine, null, null, null);
        }
    }

    // Mesmo diálogo da lista; o novo nome é salvo aqui e aparece na hora
    private void renameTrail() {
        if (trail == null) {
            return;
        }
        Trail current = trail;
        RenameTrailDialog.show(this, current, newName ->
                Async.run(() -> trailDao.rename(current.getId(), newName),
                        renamed -> {
                            current.setName(newName);
                            binding.textDetailName.setText(newName);
                            Toast.makeText(this, R.string.rename_saved, Toast.LENGTH_SHORT).show();
                        },
                        error -> Toast.makeText(this, R.string.rename_error, Toast.LENGTH_LONG).show()));
    }

    // O mapa e a trilha chegam de forma assíncrona e em qualquer ordem: desenha quando os dois existirem
    private void drawRouteWhenReady() {
        if (googleMap == null || trail == null) {
            return;
        }
        List<TrackPoint> points = trail.getPoints();
        if (points.isEmpty()) {
            Toast.makeText(this, R.string.detail_no_points, Toast.LENGTH_LONG).show();
            return;
        }

        List<LatLng> route = new ArrayList<>(points.size());
        for (TrackPoint point : points) {
            route.add(new LatLng(point.latitude(), point.longitude()));
        }

        googleMap.addPolyline(new PolylineOptions()
                .addAll(route)
                .color(ContextCompat.getColor(this, routeColorRes()))
                .width(getResources().getDimension(R.dimen.route_width))
                .geodesic(true));

        // Com um único ponto os pinos de início e fim ficariam sobrepostos: mostra só o de início
        addPin(route.get(0), R.drawable.ic_pin_start);
        if (route.size() > 1) {
            addPin(route.get(route.size() - 1), R.drawable.ic_pin_end);
        }

        // newLatLngBounds precisa que o mapa já tenha tamanho, então espera ele terminar de carregar
        googleMap.setOnMapLoadedCallback(() -> frameRoute(route));
    }

    /**
     * Enquadra o percurso inteiro na tela. O padding do mapa reserva as áreas cobertas pela barra superior
     * e pelo painel de informações, então o percurso fica na parte visível entre eles.
     */
    private void frameRoute(List<LatLng> route) {
        View overlay = binding.detailOverlay;
        View topCard = binding.cardDetailTop;
        View infoCard = binding.cardDetailInfo;
        googleMap.setPadding(0, topCard.getBottom(), 0, overlay.getHeight() - infoCard.getTop());

        if (route.size() == 1) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(route.get(0), SINGLE_POINT_ZOOM));
            return;
        }
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        for (LatLng position : route) {
            bounds.include(position);
        }
        int padding = getResources().getDimensionPixelSize(R.dimen.detail_bounds_padding);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), padding));
    }

    // Azul aparece bem no mapa vetorial claro; na imagem escura do satélite o amarelo se destaca
    private int routeColorRes() {
        return settingsPrefs.getMapType() == SettingsPrefs.MapType.SATELLITE
                ? R.color.route_satellite : R.color.route_vector;
    }

    private void addPin(LatLng position, int drawableResId) {
        googleMap.addMarker(new MarkerOptions()
                .position(position)
                .icon(MapIcons.fromVector(this, drawableResId))
                .anchor(0.5f, 0.5f));
    }

    private void closeWithError() {
        Toast.makeText(this, R.string.detail_not_found, Toast.LENGTH_LONG).show();
        finish();
    }

    private void setupWindowInsets() {
        View overlay = binding.detailOverlay;

        int initialLeft = overlay.getPaddingLeft();
        int initialTop = overlay.getPaddingTop();
        int initialRight = overlay.getPaddingRight();
        int initialBottom = overlay.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(overlay, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    initialLeft + systemBars.left,
                    initialTop + systemBars.top,
                    initialRight + systemBars.right,
                    initialBottom + systemBars.bottom
            );
            return insets;
        });
    }
}
