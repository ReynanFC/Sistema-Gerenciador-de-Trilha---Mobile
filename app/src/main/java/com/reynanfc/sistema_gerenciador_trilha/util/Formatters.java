package com.reynanfc.sistema_gerenciador_trilha.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Converte as unidades guardadas (milissegundos, m/s, metros) nos textos mostrados na tela.
 * Os valores só são convertidos aqui, na hora de exibir; o modelo e o banco mantêm unidades SI.
 */
public final class Formatters {

    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final double MPS_TO_KMH = 3.6;

    private Formatters() {
    }

    /** 2_610_000 ms → "00:43:30" */
    public static String duration(long millis) {
        long totalSeconds = Math.max(0, millis) / 1000;
        return String.format(PT_BR, "%02d:%02d:%02d",
                totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60);
    }

    /** Duração compacta para os chips da lista: "42 min" ou "1h 12min" */
    public static String shortDuration(long millis) {
        long totalMinutes = Math.max(0, millis) / 60_000;
        if (totalMinutes < 60) {
            return String.format(PT_BR, "%d min", totalMinutes);
        }
        return String.format(PT_BR, "%dh %02dmin", totalMinutes / 60, totalMinutes % 60);
    }

    /** 1,44 m/s → "5,2" (km/h, unidade mostrada à parte) */
    public static String speedKmh(double metersPerSecond) {
        return String.format(PT_BR, "%.1f", metersPerSecond * MPS_TO_KMH);
    }

    /** 3480 m → "3,48" (km, unidade mostrada à parte) */
    public static String distanceKm(double meters) {
        return String.format(PT_BR, "%.2f", meters / 1000);
    }

    /** → "24/11/2026 · 07:12" */
    public static String dateTime(long epochMillis) {
        return new SimpleDateFormat("dd/MM/yyyy '·' HH:mm", PT_BR).format(new Date(epochMillis));
    }

    /** → "24/11/2026" */
    public static String date(long epochMillis) {
        return new SimpleDateFormat("dd/MM/yyyy", PT_BR).format(new Date(epochMillis));
    }
}
