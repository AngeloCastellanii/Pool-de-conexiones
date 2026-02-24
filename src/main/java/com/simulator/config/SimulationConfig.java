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

        this.dbUrl      = require(props, "db.url");
        this.dbUser     = require(props, "db.user");
        this.dbPassword = require(props, "db.password");

        String rawQueries = require(props, "simulation.queries");
        this.queries = Arrays.stream(rawQueries.split("\\|"))
                             .map(String::trim)
                             .filter(q -> !q.isEmpty())
                             .toList();

        this.samples        = Integer.parseInt(require(props, "simulation.samples"));
        this.maxRetries     = Integer.parseInt(require(props, "simulation.maxRetries"));
        this.timeoutSeconds = Integer.parseInt(require(props, "simulation.timeoutSeconds"));

        this.poolMinSize             = Integer.parseInt(require(props, "pool.minSize"));
        this.poolMaxSize             = Integer.parseInt(require(props, "pool.maxSize"));
        this.poolScaleUpThresholdMs  = Long.parseLong(require(props, "pool.scaleUpThresholdMs"));
        this.poolScaleDownThresholdMs= Long.parseLong(require(props, "pool.scaleDownThresholdMs"));
        this.poolAcquireTimeoutMs    = Long.parseLong(require(props, "pool.acquireTimeoutMs"));
    }

    private String require(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new RuntimeException("Propiedad requerida no encontrada: " + key);
        }
        return value.trim();
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public String getDbUrl()      { return dbUrl; }
    public String getDbUser()     { return dbUser; }
    public String getDbPassword() { return dbPassword; }

    public List<String> getQueries()    { return queries; }
    public int getSamples()             { return samples; }
    public int getMaxRetries()          { return maxRetries; }
    public int getTimeoutSeconds()      { return timeoutSeconds; }

    public int  getPoolMinSize()              { return poolMinSize; }
    public int  getPoolMaxSize()              { return poolMaxSize; }
    public long getPoolScaleUpThresholdMs()   { return poolScaleUpThresholdMs; }
    public long getPoolScaleDownThresholdMs() { return poolScaleDownThresholdMs; }
    public long getPoolAcquireTimeoutMs()     { return poolAcquireTimeoutMs; }

    @Override
    public String toString() {
        return String.format(
            "[Config] BD=%s | Muestras=%d | Reintentos=%d | Timeout=%ds | Pool=%d-%d",
            dbUrl, samples, maxRetries, timeoutSeconds, poolMinSize, poolMaxSize
        );
    }
}
