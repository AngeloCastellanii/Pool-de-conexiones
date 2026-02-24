package com.simulator.core;

import com.simulator.config.SimulationConfig;
import com.simulator.logging.SimulationLogger;
import com.simulator.model.SampleResult;
import com.simulator.model.SimulationReport;
import com.simulator.pool.ConnectionPool;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * PARTE 6 — Simulación POOLED
 *
 * Misma estructura que RawSimulation (N hilos, CountDownLatch, freno manual)
 * pero cada hilo toma/devuelve conexiones del ConnectionPool en vez de abrir
 * una nueva.
 */
public class PooledSimulation {

    private final SimulationConfig config;
    private final SimulationLogger logger;

    public PooledSimulation(SimulationConfig config, SimulationLogger logger) {
        this.config = config;
        this.logger = logger;
    }

    public SimulationReport run() throws Exception {
        return run(() -> false);
    }

    public SimulationReport run(BooleanSupplier stopRequested) throws Exception {
        int n = config.getSamples();
        List<SampleResult> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(n);
        ExecutorService executor = Executors.newFixedThreadPool(n);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        Random random = new Random();
        List<String> queries = config.getQueries();

        // Inicializar el pool ANTES de soltar los hilos
        ConnectionPool pool = new ConnectionPool(config, logger);

        // ── Freno manual (watchdog) ───────────────────────────────────────────
        ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
        watchdog.schedule(() -> {
            cancelled.set(true);
            logger.logError("FRENO MANUAL activado — timeout de "
                    + config.getTimeoutSeconds() + "s alcanzado. Cancelando hilos...");
            executor.shutdownNow();
        }, config.getTimeoutSeconds(), TimeUnit.SECONDS);

        logger.logSection("SIMULACION POOLED — " + n + " hilos concurrentes | Pool: "
                + config.getPoolMinSize() + "-" + config.getPoolMaxSize() + " conexiones");

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
                    SampleResult result = ejecutarMuestra(sampleId, query, pool, cancelled, stopRequested);
                    results.add(result);
                    logger.logSample("POOLED", result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    SampleResult fallida = new SampleResult(
                            sampleId, false, 0, 0, query, "Hilo interrumpido");
                    results.add(fallida);
                    logger.logSample("POOLED", fallida);
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
            pool.shutdown();
        }

        if (!completed) {
            cancelled.set(true);
            logger.logError("POOLED no terminó dentro del tiempo de espera.");
        }

        long totalMs = System.currentTimeMillis() - startTime;

        logger.logInfo("POOLED finalizada. Tiempo total: " + totalMs + "ms");
        return new SimulationReport("POOLED", new ArrayList<>(results), totalMs);
    }

    // ── Lógica de 1 muestra con reintentos usando el pool ─────────────────────

    private SampleResult ejecutarMuestra(int id, String query,
            ConnectionPool pool, AtomicBoolean cancelled, BooleanSupplier stopRequested) {
        long inicio = System.currentTimeMillis();
        int reintentos = 0;

        while (reintentos <= config.getMaxRetries()) {
            if (cancelled.get() || stopRequested.getAsBoolean() || Thread.currentThread().isInterrupted()) {
                return new SampleResult(id, false,
                        System.currentTimeMillis() - inicio,
                        reintentos, query, "Cancelado por freno manual");
            }

            Connection conn = null;
            try {
                conn = pool.acquire(); // tomar del pool

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(query);
                }

                pool.release(conn); // devolver al pool
                return new SampleResult(id, true,
                        System.currentTimeMillis() - inicio,
                        reintentos, query, null);

            } catch (Exception e) {
                if (conn != null)
                    pool.release(conn);
                reintentos++;
                if (reintentos > config.getMaxRetries()) {
                    return new SampleResult(id, false,
                            System.currentTimeMillis() - inicio,
                            reintentos - 1, query, e.getMessage());
                }
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
