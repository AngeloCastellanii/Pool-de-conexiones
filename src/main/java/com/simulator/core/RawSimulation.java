package com.simulator.core;

import com.simulator.config.SimulationConfig;
import com.simulator.logging.SimulationLogger;
import com.simulator.model.SampleResult;
import com.simulator.model.SimulationReport;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * PARTE 4 — Simulación RAW
 *
 * Abre una conexión directa por cada hilo (sin pool).
 * Todos los hilos arrancan al mismo tiempo via CountDownLatch.
 * Incluye freno manual (watchdog) que cancela todo si se supera el timeout.
 */
public class RawSimulation {

    private final SimulationConfig config;
    private final SimulationLogger logger;

    public RawSimulation(SimulationConfig config, SimulationLogger logger) {
        this.config = config;
        this.logger = logger;
    }

    public SimulationReport run() throws InterruptedException {
        return run(() -> false);
    }

    public SimulationReport run(BooleanSupplier stopRequested) throws InterruptedException {
        int n = config.getSamples();
        List<SampleResult> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch startLatch = new CountDownLatch(1); // pistola de salida
        CountDownLatch doneLatch = new CountDownLatch(n); // esperar que terminen todos
        ExecutorService executor = Executors.newFixedThreadPool(n);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        Random random = new Random();
        List<String> queries = config.getQueries();

        // ── Freno manual (watchdog) ───────────────────────────────────────────
        ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
        watchdog.schedule(() -> {
            cancelled.set(true);
            logger.logError("FRENO MANUAL activado — timeout de "
                    + config.getTimeoutSeconds() + "s alcanzado. Cancelando hilos...");
            executor.shutdownNow();
        }, config.getTimeoutSeconds(), TimeUnit.SECONDS);

        logger.logSection("SIMULACION RAW — " + n + " hilos concurrentes");
        long startTime = System.currentTimeMillis();

        // ── Enviar hilos al pool de threads ───────────────────────────────────
        for (int i = 1; i <= n; i++) {
            final int sampleId = i;
            final String query = queries.get(random.nextInt(queries.size()));

            executor.submit(() -> {
                try {
                    if (stopRequested.getAsBoolean()) {
                        cancelled.set(true);
                        throw new InterruptedException("Cancelación solicitada");
                    }
                    startLatch.await(); // esperar pistola de salida
                    SampleResult result = ejecutarMuestra(sampleId, query, cancelled, stopRequested);
                    results.add(result);
                    logger.logSample("RAW", result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    SampleResult fallida = new SampleResult(
                            sampleId, false, 0, 0, query, "Hilo interrumpido");
                    results.add(fallida);
                    logger.logSample("RAW", fallida);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // ¡todos arrancan ya!
        boolean completed;
        try {
            completed = doneLatch.await(config.getTimeoutSeconds() + 5L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            cancelled.set(true);
            executor.shutdownNow();
            throw e;
        } finally {
            watchdog.shutdownNow();
            executor.shutdownNow();
        }

        if (!completed) {
            cancelled.set(true);
            logger.logError("RAW no terminó dentro del tiempo de espera.");
        }

        long totalMs = System.currentTimeMillis() - startTime;

        logger.logInfo("RAW finalizada. Tiempo total: " + totalMs + "ms");
        return new SimulationReport("RAW", new ArrayList<>(results), totalMs);
    }

    // ── Lógica de 1 muestra con reintentos ────────────────────────────────────

    private SampleResult ejecutarMuestra(int id, String query, AtomicBoolean cancelled, BooleanSupplier stopRequested) {
        long inicio = System.currentTimeMillis();
        int reintentos = 0;

        while (reintentos <= config.getMaxRetries()) {
            if (cancelled.get() || stopRequested.getAsBoolean() || Thread.currentThread().isInterrupted()) {
                return new SampleResult(id, false,
                        System.currentTimeMillis() - inicio,
                        reintentos, query, "Cancelado por freno manual");
            }

            try (Connection conn = DriverManager.getConnection(
                    config.getDbUrl(), config.getDbUser(), config.getDbPassword());
                    Statement stmt = conn.createStatement()) {

                stmt.execute(query);
                return new SampleResult(id, true,
                        System.currentTimeMillis() - inicio,
                        reintentos, query, null);

            } catch (SQLException e) {
                reintentos++;
                if (reintentos > config.getMaxRetries()) {
                    return new SampleResult(id, false,
                            System.currentTimeMillis() - inicio,
                            reintentos - 1, query, e.getMessage());
                }
                // Backoff exponencial entre reintentos
                try {
                    Thread.sleep(100L * reintentos);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new SampleResult(id, false,
                            System.currentTimeMillis() - inicio,
                            reintentos, query, "Interrumpido durante reintento");
                }
            }
        }

        return new SampleResult(id, false,
                System.currentTimeMillis() - inicio,
                reintentos, query, "Max reintentos alcanzado");
    }
}
