package com.simulator.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * EXTRA OPCIONAL — App de Escritorio JavaFX
 *
 * Punto de entrada de la GUI. Para ejecutarla:
 * mvn javafx:run
 *
 * La ventana ofrece:
 * - Panel de configuración (muestras, modo iterativo)
 * - Progreso en tiempo real con ProgressBar + log en vivo
 * - Tabla comparativa RAW vs POOLED al finalizar
 */
public class SimulatorApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/simulator/gui/SimulatorView.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Simulador de Pool de Conexiones — PostgreSQL + JDBC");
        primaryStage.setScene(new Scene(root, 900, 660));
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
