package com.simulator.gui;

import com.simulator.config.SimulationConfig;
import com.simulator.core.IterativeRunner;
import com.simulator.core.PooledSimulation;
import com.simulator.core.RawSimulation;
import com.simulator.logging.SimulationLogger;
import com.simulator.metrics.MetricsCollector;
import com.simulator.model.SimulationReport;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

/**
 * Controlador JavaFX de la ventana principal.
 *
 * Corre las simulaciones en un Task<Void> de fondo para no
 * bloquear el hilo UI, y actualiza los controles con
 * Platform.runLater() al terminar cada muestra.
 */
public class SimulationController {

    // ── Controles FXML ────────────────────────────────────────────────────────
    @FXML
    private Label lblEstado;
    @FXML
    private ProgressBar barProgreso;
    @FXML
    private TextArea logArea;
    @FXML
    private Button btnIniciar;
    @FXML
    private Button btnDetener;
    @FXML
    private CheckBox chkIterativo;
    @FXML
    private TextField txtPasos;
    @FXML
    private Label lblPoolInfo;

    // Tabla comparativa
    @FXML
    private TableView<MetricaFila> tablaResult;
    @FXML
    private TableColumn<MetricaFila, String> colMetrica;
    @FXML
    private TableColumn<MetricaFila, String> colRaw;
    @FXML
    private TableColumn<MetricaFila, String> colPooled;

    private SimulationConfig config;
    private Task<Void> tareaActual;

    @FXML
    public void initialize() {
        // Cargar config al abrir la ventana
        try {
            config = new SimulationConfig("config.properties");
            log("✅ Configuración cargada: " + config);
            chkIterativo.setSelected(config.isIterative());
            txtPasos.setText(String.join(",",
                    config.getIterativeSteps().stream().map(Object::toString).toList()));
        } catch (Exception e) {
            log("❌ Error cargando config.properties: " + e.getMessage());
            btnIniciar.setDisable(true);
        }

        // Inicializar tabla
        colMetrica.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().metrica));
        colRaw.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().raw));
        colPooled.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().pooled));

        colMetrica.setCellFactory(column -> createReadableCell());
        colRaw.setCellFactory(column -> createReadableCell());
        colPooled.setCellFactory(column -> createReadableCell());

        if (lblPoolInfo != null && config != null) {
            lblPoolInfo.setText(String.format(
                "Nota: los pasos iterativos cambian hilos/muestras, no el pool. Límite actual: %d-%d conexiones.",
                config.getPoolMinSize(), config.getPoolMaxSize()));
        }

        barProgreso.setProgress(0);
        lblEstado.setText("Listo");
        btnDetener.setDisable(true);
    }

    @FXML
    private void onIniciar() {
        if (tareaActual != null && tareaActual.isRunning()) {
            return;
        }

        boolean modoIterativo = chkIterativo.isSelected();
        SimulationConfig configEjecucion = config;
        List<Integer> pasosIterativos = null;

        if (modoIterativo) {
            try {
                pasosIterativos = parseIterativeSteps(txtPasos.getText());
                configEjecucion = config.withIterativeSteps(pasosIterativos);
            } catch (IllegalArgumentException e) {
                lblEstado.setText("❌ Pasos inválidos");
                log("❌ " + e.getMessage());
                return;
            }
        }

        btnIniciar.setDisable(true);
        btnDetener.setDisable(false);
        logArea.clear();
        tablaResult.getItems().clear();
        barProgreso.setProgress(-1); // modo indeterminado
        lblEstado.setText("Simulando...");

        final SimulationConfig cfgFinal = configEjecucion;
        final List<Integer> pasosFinal = pasosIterativos;

        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (SimulationLogger logger = new SimulationLogger()) {

                    appendLog("📋 Logger iniciado → " + logger.getLogFilePath());

                    if (modoIterativo) {
                        appendLog("🔁 Modo ITERATIVO activado — pasos: " + pasosFinal);
                        IterativeRunner runner = new IterativeRunner(cfgFinal, logger);
                        List<IterativeRunner.StepResult> pasos = runner.run(this::isCancelled);

                        if (isCancelled()) {
                            throw new CancellationException("Simulación cancelada por el usuario");
                        }

                        appendLog("✅ Iterativo completado — " + pasos.size() + " pasos procesados.");
                        Platform.runLater(() -> {
                            barProgreso.setProgress(1.0);
                            lblEstado.setText("✅ Iterativo completado");
                            rellenarTablaIterativa(pasos);
                        });
                    } else {
                        // RAW
                        appendLog("⚙️  Iniciando simulación RAW...");
                        RawSimulation raw = new RawSimulation(cfgFinal, logger);
                        SimulationReport rawReport = raw.run(this::isCancelled);

                        if (isCancelled()) {
                            throw new CancellationException("Simulación cancelada por el usuario");
                        }

                        appendLog(String.format("✅ RAW completada — %d/%d exitosas | Tiempo: %dms",
                                rawReport.getExitosas(), rawReport.getTotalMuestras(),
                                rawReport.getTiempoTotalMs()));

                        Platform.runLater(() -> barProgreso.setProgress(0.5));
                        Thread.sleep(2000);

                        if (isCancelled()) {
                            throw new CancellationException("Simulación cancelada por el usuario");
                        }

                        // POOLED
                        appendLog("⚙️  Iniciando simulación POOLED...");
                        PooledSimulation pooled = new PooledSimulation(cfgFinal, logger);
                        SimulationReport pooledReport = pooled.run(this::isCancelled);

                        if (isCancelled()) {
                            throw new CancellationException("Simulación cancelada por el usuario");
                        }

                        appendLog(String.format("✅ POOLED completada — %d/%d exitosas | Tiempo: %dms",
                                pooledReport.getExitosas(), pooledReport.getTotalMuestras(),
                                pooledReport.getTiempoTotalMs()));

                        // Actualizar tabla
                        Platform.runLater(() -> {
                            barProgreso.setProgress(1.0);
                            lblEstado.setText("✅ Simulación completada");
                            rellenarTabla(rawReport, pooledReport);
                        });

                        MetricsCollector.imprimirComparativa(rawReport, pooledReport, logger);
                    }

                } catch (CancellationException e) {
                    appendLog("🛑 Simulación detenida manualmente.");
                    Platform.runLater(() -> {
                        barProgreso.setProgress(0);
                        lblEstado.setText("🛑 Detenida");
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    appendLog("🛑 Simulación detenida manualmente.");
                    Platform.runLater(() -> {
                        barProgreso.setProgress(0);
                        lblEstado.setText("🛑 Detenida");
                    });
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    appendLog("❌ ERROR: " + e.getMessage() + "\n" + sw);
                    Platform.runLater(() -> lblEstado.setText("❌ Error"));
                }
                return null;
            }
        };

        tareaActual = tarea;

        tarea.setOnSucceeded(e -> {
            btnIniciar.setDisable(false);
            btnDetener.setDisable(true);
        });
        tarea.setOnCancelled(e -> {
            btnIniciar.setDisable(false);
            btnDetener.setDisable(true);
            barProgreso.setProgress(0);
            lblEstado.setText("🛑 Detenida");
        });
        tarea.setOnFailed(e -> {
            btnIniciar.setDisable(false);
            btnDetener.setDisable(true);
            lblEstado.setText("❌ Falló inesperadamente");
        });

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    private void onDetener() {
        if (tareaActual != null && tareaActual.isRunning()) {
            appendLog("🛑 Freno manual solicitado. Deteniendo simulación...");
            lblEstado.setText("Deteniendo...");
            tareaActual.cancel(true);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void appendLog(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    private void log(String msg) {
        logArea.appendText(msg + "\n");
    }

    private TableCell<MetricaFila, String> createReadableCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setTextFill(Color.web("#0f172a"));
                    setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
                }
            }
        };
    }

    private List<Integer> parseIterativeSteps(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Debes indicar pasos iterativos, por ejemplo: 100,500,1000");
        }

        List<Integer> pasos = new ArrayList<>();
        String[] partes = raw.split(",");
        for (String parte : partes) {
            String limpio = parte.trim();
            if (limpio.isEmpty()) {
                throw new IllegalArgumentException("Formato inválido en pasos iterativos. Usa enteros separados por coma.");
            }
            int valor;
            try {
                valor = Integer.parseInt(limpio);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Paso inválido: '" + limpio + "'. Usa solo números enteros positivos.");
            }
            if (valor <= 0) {
                throw new IllegalArgumentException("Cada paso debe ser mayor que 0.");
            }
            pasos.add(valor);
        }

        if (pasos.isEmpty()) {
            throw new IllegalArgumentException("Debes indicar al menos un paso iterativo.");
        }

        return pasos;
    }

    private void rellenarTablaIterativa(List<IterativeRunner.StepResult> pasos) {
        for (IterativeRunner.StepResult r : pasos) {
            long rawMs = r.rawReport.getTiempoTotalMs();
            long pooledMs = r.pooledReport.getTiempoTotalMs();
            double mejora = rawMs > 0 ? ((rawMs - pooledMs) * 100.0 / rawMs) : 0;
            String ganador = pooledMs < rawMs ? "POOLED ✓" : "RAW ✓";
            tablaResult.getItems().add(new MetricaFila(
                    r.n + " hilos",
                    rawMs + "ms (" + r.rawReport.getExitosas() + "/" + r.rawReport.getTotalMuestras() + ")",
                    pooledMs + "ms  " + String.format("%.1f%% mejor  ", mejora) + ganador));
        }
    }

    private void rellenarTabla(SimulationReport raw, SimulationReport pooled) {
        boolean pooledGana = pooled.getTiempoTotalMs() < raw.getTiempoTotalMs();

        tablaResult.getItems().addAll(
                new MetricaFila("Tiempo total (ms)",
                        raw.getTiempoTotalMs() + "ms",
                        pooled.getTiempoTotalMs() + "ms " + (pooledGana ? "✓" : "")),
                new MetricaFila("Total muestras",
                        String.valueOf(raw.getTotalMuestras()),
                        String.valueOf(pooled.getTotalMuestras())),
                new MetricaFila("Exitosas",
                        raw.getExitosas() + " (" + String.format("%.1f%%", raw.getPorcentajeExito()) + ")",
                        pooled.getExitosas() + " (" + String.format("%.1f%%", pooled.getPorcentajeExito()) + ")"),
                new MetricaFila("Fallidas",
                        raw.getFallidas() + " (" + String.format("%.1f%%", 100 - raw.getPorcentajeExito()) + ")",
                        pooled.getFallidas() + " (" + String.format("%.1f%%", 100 - pooled.getPorcentajeExito()) + ")"),
                new MetricaFila("Prom. reintentos",
                        String.format("%.2f", raw.getPromedioReintentos()),
                        String.format("%.2f", pooled.getPromedioReintentos())),
                new MetricaFila("GANADOR",
                        pooledGana ? "" : "RAW ✓",
                        pooledGana ? "POOLED ✓" : ""));
    }

    // ── Modelo de fila de tabla ───────────────────────────────────────────────

    public static class MetricaFila {
        final String metrica, raw, pooled;

        MetricaFila(String m, String r, String p) {
            metrica = m;
            raw = r;
            pooled = p;
        }
    }
}
