package com.reynanfc.sistema_gerenciador_trilha.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Executa o trabalho de banco fora da UI thread e entrega o resultado de volta nela.
 * Uma única thread de fundo mantém as gravações em ordem (nunca duas transações ao mesmo tempo).
 */
public final class Async {

    private static final ExecutorService BACKGROUND = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Async() {
    }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Exception> onError) {
        BACKGROUND.execute(() -> {
            try {
                T result = work.call();
                MAIN.post(() -> onSuccess.accept(result));
            } catch (Exception e) {
                MAIN.post(() -> onError.accept(e));
            }
        });
    }
}
