package com.simulator.logging;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.simulator.model.SampleResult;

/**
 * Escribe cada muestra en un archivo .log con timestamp y estado.
 * También replica todos los mensajes en consola.
 *
 * Formato de cada línea:
 *   [HH:mm:ss.SSS] [TIPO] [ID-XXX] [EXITOSA/FALLIDA] Tiempo: Xms | Reintentos: Y | Error: ...
 */
public class SimulationLogger implements AutoCloseable {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static final DateTimeFormatter FILE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final PrintWriter writer;
    private final String      logFilePath;

    public SimulationLogger() throws IOException {
        Files.createDirectories(Path.of("logs"));
        String filename = "logs/simulacion_" + LocalDateTime.now().format(FILE_FMT) + ".log";
        this.logFilePath = filename;
        this.writer      = new PrintWriter(new FileWriter(filename, true));

        String header = "=".repeat(70);
        writeLine(header);
        writeLine("  SIMULADOR DE POOL DE CONEXIONES - " + LocalDateTime.now());
        writeLine(header);
    }

    /**
     * Registra el resultado de una muestra individual.
     *
     * @param tipo  "RAW" o "POOLED"
     * @param result resultado de la muestra
     */
    public synchronized void logSample(String tipo, SampleResult result) {
        String hora   = result.getTimestamp().format(TIME_FMT);
        String estado = result.isExitosa() ? "EXITOSA" : "FALLIDA";
        String query  = resumirQuery(result.getQueryEjecutada());
        String linea  = String.format("[%s] [%-6s] [ID-%03d] [%s] Tiempo: %dms | Reintentos: %d%s",
                hora, tipo, result.getId(), estado,
                result.getTiempoMs(), result.getReintentos(),
                result.isExitosa() ? "" : " | Error: " + result.getError());

        linea += " | Query: " + query;

        writeLine(linea);
    }

    /**
     * Escribe un encabezado de sección (ej: inicio de RAW o POOLED).
     */
    public synchronized void logSection(String titulo) {
        String sep = "-".repeat(70);
        writeLine("");
        writeLine(sep);
        writeLine("  " + titulo.toUpperCase());
        writeLine(sep);
    }

    /**
     * Escribe un mensaje informativo libre.
     */
    public synchronized void logInfo(String mensaje) {
        String hora  = LocalDateTime.now().format(TIME_FMT);
        writeLine("[" + hora + "] [INFO  ] " + mensaje);
    }

    /**
     * Escribe un mensaje de error libre.
     */
    public synchronized void logError(String mensaje) {
        String hora = LocalDateTime.now().format(TIME_FMT);
        writeLine("[" + hora + "] [ERROR ] " + mensaje);
    }

    /** Devuelve la ruta del archivo de log actual. */
    public String getLogFilePath() { return logFilePath; }

    // ── Interno ────────────────────────────────────────────────────────────────

    private void writeLine(String linea) {
        System.out.println(linea);
        writer.println(linea);
        writer.flush();
    }

    private String resumirQuery(String query) {
        if (query == null || query.isBlank()) {
            return "(vacía)";
        }
        String limpia = query.replaceAll("\\s+", " ").trim();
        int max = 80;
        return limpia.length() <= max ? limpia : limpia.substring(0, max) + "...";
    }

    @Override
    public synchronized void close() {
        writeLine("=".repeat(70));
        writeLine("  FIN DE LA SIMULACION - " + LocalDateTime.now());
        writeLine("=".repeat(70));
        writer.close();
    }
}
