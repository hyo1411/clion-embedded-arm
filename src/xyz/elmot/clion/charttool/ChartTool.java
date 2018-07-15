package xyz.elmot.clion.charttool;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTabbedPane;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import xyz.elmot.clion.openocd.ChartFilter;

import java.util.*;


public class ChartTool implements ToolWindowFactory, ChartFilter.DataListener {
    static {
        Platform.setImplicitExit(false);
    }

    //    private static final com.intellij.openapi.diagnostic.Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(OpenOcdComponent.class);
    private CheckBox keep;
    private Button reset;
    private LineChart<Number, Number> lineChart;
    private Map<String, XYChart.Series<Number, Number>> seriesByName = new HashMap<>();
    private boolean initialized = false;
    private JFXPanel fxPanel;

    public ChartTool() {
    }

    private void clear() {
        lineChart.getData().clear();
        seriesByName.clear();
    }

    @Override
    public void dataDetected(String name, Collection<Number> data) {
        if (!initialized) return;
        Platform.runLater(() -> {
            XYChart.Series<Number, Number> series;
            if (keep.isSelected()) {
                String newName;
                for (int i = 1; true; i++) {
                    newName = name + " " + i;
                    if (!seriesByName.containsKey(newName)) break;
                }
                series = newSeries(newName);
            } else {
                series = seriesByName.get(name);
                if (series == null) {
                    series = newSeries(name);
                }
            }
            List<XYChart.Data<Number, Number>> dataPoints = new ArrayList<>(data.size());
            for (Number datum : data) {
                dataPoints.add(new XYChart.Data<>(dataPoints.size(), datum));
            }
            series.getData().setAll(dataPoints);
        });
    }

    private XYChart.Series<Number, Number> newSeries(String name) {
        XYChart.Series<Number, Number> series;
        series = new XYChart.Series<>();
        series.setName(name);
        lineChart.getData().add(series);
        seriesByName.put(name, series);
        return series;
    }


    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        project.getMessageBus().connect().subscribe(ChartFilter.CHART_DATA_FLOW, this);
        fxPanel = new JFXPanel();
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
            VBox controls = new VBox(keep, reset);

            HBox hBox = new HBox(controls, lineChart);
            Scene scene = new Scene(hBox, 800, 600);

            lineChart.setAnimated(false);
            lineChart.setBackground(Background.EMPTY);

            fxPanel.setScene(scene);
            fxPanel.invalidate();
            initialized = true;
        });
        toolWindow.getComponent().add(fxPanel);
    }
}
