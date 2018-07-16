package xyz.elmot.clion.charttool;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChartsPanel extends JFXPanel {
    private boolean initialized = false;

    private CheckBox keep;
    private Button reset;
    private LineChart<Number, Number> lineChart;
    private Map<String, XYChart.Series<Number, Number>> seriesByName = new ConcurrentHashMap<>();

    public ChartsPanel() {
        Platform.runLater(() -> {

            keep = new CheckBox("Keep Old Series");

            reset = new Button("Clear");
            reset.setOnAction(e -> clear());
            //defining the axes
            final NumberAxis xAxis = new NumberAxis();
            final NumberAxis yAxis = new NumberAxis();
            //creating the chart
            lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setCreateSymbols(false);
            VBox controls = new VBox(10, keep, reset);

            HBox hBox = new HBox(10, controls, lineChart);
            hBox.setPadding(new Insets(10));
            Scene scene = new Scene(hBox);

            lineChart.setAnimated(false);
            hBox.setFillHeight(true);
            HBox.setHgrow(lineChart, Priority.ALWAYS);
            lineChart.setScaleShape(true);
            setScene(scene);
            invalidate();
            initialized = true;
        });
    }

    private void clear() {
        lineChart.getData().clear();
        Platform.runLater(seriesByName::clear);
    }

    public void series(String name, List<XYChart.Data<Number, Number>> data) {
        if (!initialized) {
            return;
        }

        if (keep.isSelected()) {

            String realName;
            for (int i = 1; seriesByName.containsKey(realName = name + "#" + i); ) {
                i++;
            }
            newSeries(realName, data);

        } else {
            XYChart.Series<Number, Number> oldSeries = seriesByName.get(name);
            if (oldSeries != null) {
                Platform.runLater(() -> {
                    oldSeries.getData().setAll(data);
                });
            } else {
                newSeries(name, data);
            }
        }
    }

    protected void newSeries(String name, List<XYChart.Data<Number, Number>> data) {
        Platform.runLater(() -> {

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(name);
            series.getData().setAll(data);
            lineChart.getData().add(series);
            seriesByName.put(name, series);
        });
    }
}
