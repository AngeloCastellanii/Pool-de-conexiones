package com.simulator.config;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Carga y expone todos los parámetros del archivo config.properties.
 * Ningún valor de configuración está acoplado al código Java.
 */
public class SimulationConfig {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    private final List<String> queries;
    private final int samples;
    private final int maxRetries;
    private final int timeoutSeconds;

    private final int poolMinSize;
    private final int poolMaxSize;
    private final long poolScaleUpThresholdMs;
    private final long poolScaleDownThresholdMs;
    private final long poolAcquireTimeoutMs;

    // Modo iterativo
    private final boolean iterative;
    private final List<Integer> iterativeSteps;

    public SimulationConfig(String propertiesFile) {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(propertiesFile)) {
            if (is == null) {
                throw new RuntimeException("No se encontró el archivo: " + propertiesFile);
            }
            props.load(is);
        } catch (Exception e) {
            throw new RuntimeException("Error al cargar la configuración: " + e.getMessage(), e);
        }

        this.dbUrl = require(props, "db.url");
        this.dbUser = require(props, "db.user");
        this.dbPassword = require(props, "db.password");

        String rawQueries = require(props, "simulation.queries");
        this.queries = Arrays.stream(rawQueries.split("\\|"))
                .map(String::trim)
                .filter(q -> !q.isEmpty())
                .toList();

        this.samples = Integer.parseInt(require(props, "simulation.samples"));
        this.maxRetries = Integer.parseInt(require(props, "simulation.maxRetries"));
        this.timeoutSeconds = Integer.parseInt(require(props, "simulation.timeoutSeconds"));

        this.poolMinSize = Integer.parseInt(require(props, "pool.minSize"));
        this.poolMaxSize = Integer.parseInt(require(props, "pool.maxSize"));
        this.poolScaleUpThresholdMs = Long.parseLong(require(props, "pool.scaleUpThresholdMs"));
        this.poolScaleDownThresholdMs = Long.parseLong(require(props, "pool.scaleDownThresholdMs"));
        this.poolAcquireTimeoutMs = Long.parseLong(require(props, "pool.acquireTimeoutMs"));

        this.iterative = Boolean.parseBoolean(props.getProperty("simulation.iterative", "false"));
        String stepsRaw = props.getProperty("simulation.iterativeSteps", "50,100,200");
        this.iterativeSteps = Arrays.stream(stepsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList();
    }

    private String require(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new RuntimeException("Propiedad requerida no encontrada: " + key);
        }
        return value.trim();
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public List<String> getQueries() {
        return queries;
    }

    public int getSamples() {
        return samples;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getPoolMinSize() {
        return poolMinSize;
    }

    public int getPoolMaxSize() {
        return poolMaxSize;
    }

    public long getPoolScaleUpThresholdMs() {
        return poolScaleUpThresholdMs;
    }

    public long getPoolScaleDownThresholdMs() {
        return poolScaleDownThresholdMs;
    }

    public long getPoolAcquireTimeoutMs() {
        return poolAcquireTimeoutMs;
    }

    public boolean isIterative() {
        return iterative;
    }

    public List<Integer> getIterativeSteps() {
        return iterativeSteps;
    }

    /**
     * Devuelve una copia de esta config con un número de muestras diferente.
     * Útil para el modo iterativo: withSamples(100), withSamples(200), etc.
     */
    public SimulationConfig withSamples(int n) {
        return new SimulationConfig(this, n);
    }

    // Constructor privado para clonación con diferente N
    private SimulationConfig(SimulationConfig base, int newSamples) {
        this.dbUrl = base.dbUrl;
        this.dbUser = base.dbUser;
        this.dbPassword = base.dbPassword;
        this.queries = base.queries;
        this.samples = newSamples;
        this.maxRetries = base.maxRetries;
        this.timeoutSeconds = base.timeoutSeconds;
        this.poolMinSize = base.poolMinSize;
        this.poolMaxSize = base.poolMaxSize;
        this.poolScaleUpThresholdMs = base.poolScaleUpThresholdMs;
        this.poolScaleDownThresholdMs = base.poolScaleDownThresholdMs;
        this.poolAcquireTimeoutMs = base.poolAcquireTimeoutMs;
        this.iterative = base.iterative;
        this.iterativeSteps = base.iterativeSteps;
    }

    @Override
    public String toString() {
        return String.format(
                "[Config] BD=%s | Muestras=%d | Reintentos=%d | Timeout=%ds | Pool=%d-%d",
                dbUrl, samples, maxRetries, timeoutSeconds, poolMinSize, poolMaxSize);
    }
}
