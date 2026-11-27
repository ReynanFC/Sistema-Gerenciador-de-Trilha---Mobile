package com.reynanfc.sistema_gerenciador_trilha.location;

import android.location.Location;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.reynanfc.sistema_gerenciador_trilha.model.TrackPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado da trilha em gravação, independente de qualquer View: tempo decorrido com pausas,
 * velocidades, distância e os pontos aceitos.
 * Fica dentro de um ViewModel para sobreviver à rotação de tela.
 *
 * O tempo usa SystemClock.elapsedRealtime(): ele só anda para frente, mesmo se o usuário mudar
 * o relógio do celular. O relógio do sistema (System.currentTimeMillis) é usado só para as datas
 * de início e fim salvas no banco.
 */
public class TrailRecorder {

    private static final long NOT_RUNNING = -1;

    /** Leituras menos precisas que isto (metros) não contam para velocidade máxima, distância nem pontos */
    public static final float MAX_ACCEPTED_ACCURACY = 25f;

    /** Menor deslocamento (metros) que conta como distância, para a oscilação do GPS não somar com o usuário parado */
    private static final float MIN_STEP_METERS = 5f;

    private boolean started;
    private long startTime;
    private long accumulatedMillis;
    private long runningSince = NOT_RUNNING;
    private float currentSpeed;
    private float maxSpeed;
    private double distance;
    private final List<TrackPoint> points = new ArrayList<>();
    @Nullable
    private Location lastAccepted;

    public boolean isStarted() {
        return started;
    }

    public boolean isRunning() {
        return runningSince != NOT_RUNNING;
    }

    public void start() {
        if (started) {
            return;
        }
        started = true;
        startTime = System.currentTimeMillis();
        runningSince = SystemClock.elapsedRealtime();
    }

    public void pause() {
        if (!isRunning()) {
            return;
        }
        accumulatedMillis += SystemClock.elapsedRealtime() - runningSince;
        runningSince = NOT_RUNNING;
    }

    public void resume() {
        if (!started || isRunning()) {
            return;
        }
        runningSince = SystemClock.elapsedRealtime();
        // O que o usuário andou durante a pausa não pode contar: a próxima posição começa um novo trecho
        lastAccepted = null;
    }

    /** Alimenta a gravação com uma posição do GPS. Durante a pausa, só a velocidade atual é atualizada. */
    public void addLocation(Location location) {
        // Nem toda posição traz velocidade (ex.: localização pela rede); nesse caso considera parado
        currentSpeed = location.hasSpeed() ? location.getSpeed() : 0f;

        if (!isRunning() || location.getAccuracy() > MAX_ACCEPTED_ACCURACY) {
            return;
        }
        // Leituras imprecisas são descartadas acima porque geram picos de velocidade e distância falsa
        maxSpeed = Math.max(maxSpeed, currentSpeed);

        if (lastAccepted == null) {
            acceptPoint(location);
            return;
        }

        // Uma posição só conta depois de andar mais que a própria margem de erro (e pelo menos MIN_STEP),
        // senão, parado, a oscilação do GPS iria somando distância aos poucos
        float step = lastAccepted.distanceTo(location);
        if (step >= Math.max(MIN_STEP_METERS, location.getAccuracy())) {
            distance += step;
            acceptPoint(location);
        }
    }

    private void acceptPoint(Location location) {
        points.add(new TrackPoint(location.getLatitude(), location.getLongitude(),
                currentSpeed, location.getAccuracy(), location.getTime()));
        lastAccepted = location;
    }

    /** Metros */
    public double getDistance() {
        return distance;
    }

    /** Pontos aceitos na ordem da gravação, para serem salvos com a trilha */
    public List<TrackPoint> getPoints() {
        return points;
    }

    /** m/s */
    public float getCurrentSpeed() {
        return currentSpeed;
    }

    /** m/s */
    public float getMaxSpeed() {
        return maxSpeed;
    }

    /** Tempo gravado, sem as pausas */
    public long getElapsedMillis() {
        long current = isRunning() ? SystemClock.elapsedRealtime() - runningSince : 0;
        return accumulatedMillis + current;
    }

    /** Data/hora do início pelo relógio do celular, em epoch millis */
    public long getStartTime() {
        return startTime;
    }
}
