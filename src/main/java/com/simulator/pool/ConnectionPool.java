package com.simulator.pool;

import com.simulator.config.SimulationConfig;
import com.simulator.logging.SimulationLogger;

import java.sql.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PARTE 5 — Pool de Conexiones propio
 *
 * Mantiene un conjunto de conexiones reutilizables en una BlockingQueue.
 * Implementa escalado dinámico:
 * - Scale-up: si el tiempo de espera supera scaleUpThresholdMs → agrega
 * conexiones (hasta maxSize)
 * - Scale-down: si el pool tiene más conexiones de las necesarias → las cierra
 * (hasta minSize)
 */
public class ConnectionPool {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    private final int poolMinSize;
    private final int poolMaxSize;
    private final long poolScaleUpThresholdMs;
    private final long poolScaleDownThresholdMs;
    private final long poolAcquireTimeoutMs;

    private final SimulationLogger logger;
    private final BlockingDeque<Connection> pool;
    private final AtomicInteger totalConexiones; // activas (en uso + disponibles)
    private final AtomicLong ultimoTiempoEsperaMs;
    private volatile boolean cerrado = false;

    public ConnectionPool(SimulationConfig config, SimulationLogger logger) throws SQLException {
        this(
                config.getDbUrl(),
                config.getDbUser(),
                config.getDbPassword(),
                config.getPoolMinSize(),
                config.getPoolMaxSize(),
                config.getPoolScaleUpThresholdMs(),
                config.getPoolScaleDownThresholdMs(),
                config.getPoolAcquireTimeoutMs(),
                logger);
    }

    public ConnectionPool(
            String dbUrl,
            String dbUser,
            String dbPassword,
            int poolMinSize,
            int poolMaxSize,
            long poolScaleUpThresholdMs,
            long poolScaleDownThresholdMs,
            long poolAcquireTimeoutMs,
            SimulationLogger logger) throws SQLException {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.poolMinSize = poolMinSize;
        this.poolMaxSize = poolMaxSize;
        this.poolScaleUpThresholdMs = poolScaleUpThresholdMs;
        this.poolScaleDownThresholdMs = poolScaleDownThresholdMs;
        this.poolAcquireTimeoutMs = poolAcquireTimeoutMs;
        this.logger = logger;
        this.pool = new LinkedBlockingDeque<>();
        this.totalConexiones = new AtomicInteger(0);
        this.ultimoTiempoEsperaMs = new AtomicLong(Long.MAX_VALUE);
        inicializar();
    }

    // ── Inicialización ────────────────────────────────────────────────────────

    private void inicializar() throws SQLException {
        logInfo("Inicializando pool con " + poolMinSize + " conexiones...");
        for (int i = 0; i < poolMinSize; i++) {
            pool.offer(crearConexion());
            totalConexiones.incrementAndGet();
        }
        logInfo("Pool listo — " + totalConexiones.get() + " conexiones disponibles.");
    }

    // ── Adquirir conexión ─────────────────────────────────────────────────────

    /**
     * Obtiene una conexión del pool.
     * Si no hay disponibles y hay margen para crecer, hace scale-up.
     * Si se supera el tiempo de espera, lanza SQLException.
     */
    public Connection acquire() throws SQLException, InterruptedException {
        if (cerrado)
            throw new SQLException("El pool está cerrado.");

        long inicio = System.currentTimeMillis();

        // Intentar tomar una conexión disponible
        Connection conn = pool.poll(poolAcquireTimeoutMs, TimeUnit.MILLISECONDS);
        long tiempoEspera = System.currentTimeMillis() - inicio;
        ultimoTiempoEsperaMs.set(tiempoEspera);

        // Si no había disponible → scale-up
        if (conn == null || !esValida(conn)) {
            if (totalConexiones.get() < poolMaxSize) {
                conn = crearConexion();
                totalConexiones.incrementAndGet();
                logInfo("Pool scale-up (sin disponibles): "
                        + totalConexiones.get() + " conexiones totales.");
            } else {
                throw new SQLException(
                        "Pool agotado: no hay conexiones disponibles (max="
                                + poolMaxSize + ")");
            }
        }

        // Si la espera superó el umbral → scale-up preventivo
        if (tiempoEspera > poolScaleUpThresholdMs
                && totalConexiones.get() < poolMaxSize) {
            try {
                pool.offer(crearConexion());
                totalConexiones.incrementAndGet();
                logInfo("Pool scale-up (umbral espera=" + tiempoEspera + "ms): "
                        + totalConexiones.get() + " conexiones totales.");
            } catch (SQLException e) {
                // scale-up best-effort, no crítico
            }
        }

        return conn;
    }

    // ── Liberar conexión ──────────────────────────────────────────────────────

    /**
     * Devuelve una conexión al pool.
     * Si hay exceso de conexiones, aplica scale-down cerrando esta.
     */
    public void release(Connection conn) {
        if (conn == null || cerrado) {
            cerrarConexion(conn);
            return;
        }

        // Scale-down: si la espera reciente fue baja y hay más conexiones de las necesarias,
        // cerrar esta conexión para reducir el pool.
        if (totalConexiones.get() > poolMinSize
            && pool.size() >= poolMinSize
            && ultimoTiempoEsperaMs.get() <= poolScaleDownThresholdMs) {
            cerrarConexion(conn);
            totalConexiones.decrementAndGet();
            logInfo("Pool scale-down (espera=" + ultimoTiempoEsperaMs.get() + "ms): "
                + totalConexiones.get() + " conexiones totales.");
            return;
        }

        // Si la conexión aún es válida, devolverla al pool
        if (esValida(conn)) {
            pool.offerFirst(conn); // devolver al frente para reutilización rápida
        } else {
            // Reemplazar conexión inválida
            cerrarConexion(conn);
            totalConexiones.decrementAndGet();
            try {
                pool.offer(crearConexion());
                totalConexiones.incrementAndGet();
            } catch (SQLException e) {
                logError("No se pudo reemplazar conexión inválida: " + e.getMessage());
            }
        }
    }

    // ── Cierre del pool ───────────────────────────────────────────────────────

    public void shutdown() {
        cerrado = true;
        int cerradas = 0;
        Connection conn;
        while ((conn = pool.poll()) != null) {
            cerrarConexion(conn);
            cerradas++;
        }
        logInfo("Pool cerrado — " + cerradas + " conexiones cerradas.");
        totalConexiones.set(0);
    }

    // ── Métricas del pool ─────────────────────────────────────────────────────

    public int getTotalConexiones() {
        return totalConexiones.get();
    }

    public int getConexionesDisponibles() {
        return pool.size();
    }

    public int getConexionesEnUso() {
        return totalConexiones.get() - pool.size();
    }

    // ── Internos ──────────────────────────────────────────────────────────────

    private Connection crearConexion() throws SQLException {
        return DriverManager.getConnection(
                dbUrl, dbUser, dbPassword);
    }

    private void logInfo(String message) {
        if (logger != null) {
            logger.logInfo(message);
        }
    }

    private void logError(String message) {
        if (logger != null) {
            logger.logError(message);
        }
    }

    private boolean esValida(Connection conn) {
        try {
            return conn != null && !conn.isClosed() && conn.isValid(1);
        } catch (SQLException e) {
            return false;
        }
    }

    private void cerrarConexion(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
