package xyz.elmot.clion.charttool;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import xyz.elmot.clion.charttool.state.ChartExpr;
import xyz.elmot.clion.charttool.state.ExpressionState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChartsPanel extends JFXPanel {
    private boolean initialized = false;
    public static final int MAX_SERIES = 50;

    private Button reset;
    private LineChart<Number, Number> lineChart;
    private Map<String, ChartExpressionData> seriesByName = new ConcurrentHashMap<>();

    public ChartsPanel() {
        Platform.runLater(() -> {

            reset = new Button("Clear");
            reset.setOnAction(e -> clear());
            //defining the axes
            final NumberAxis xAxis = new NumberAxis();
            final NumberAxis yAxis = new NumberAxis();
            //creating the chart
            lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setCreateSymbols(false);

            VBox vBox = new VBox(10, lineChart, reset);
            vBox.setPadding(new Insets(10));
            Scene scene = new Scene(vBox);

            lineChart.setAnimated(false);
            vBox.setFillWidth(true);
            VBox.setVgrow(lineChart, Priority.ALWAYS);
            lineChart.setScaleShape(true);
            setScene(scene);
            invalidate();
            initialized = true;
        });
    }

    public void clear() {
        seriesByName.clear();
        Platform.runLater(lineChart.getData()::clear);
    }

    public void series(ChartExpr chartExpr, List<Number> numbers) {
        ChartExpressionData data = seriesByName
                .computeIfAbsent(chartExpr.getName(), a -> new ChartExpressionData());
        String name;
        if (chartExpr.getState() == ExpressionState.ACCUMULATE) {
            int index = data.currentIndex.getAndUpdate(i -> (i + 1) % MAX_SERIES);
            if (data.data.size() <= index) {
                data.data.add(numbers);
            } else {
                data.data.set(index, numbers);
            }
            name = chartExpr.getName() + " #" + (index + 1);
        } else {
            data.data.clear();
            data.currentIndex.set(0);
            data.data.add(numbers);
            name = chartExpr.getName();
        }


        ObservableList<XYChart.Data<Number, Number>> lineData = calcLineData(chartExpr, numbers);
        Platform.runLater(() -> {
            ObservableList<XYChart.Series<Number, Number>> chartData = lineChart.getData();
            Optional<XYChart.Series<Number, Number>> foundSeries = chartData
                    .stream()
                    .filter(series -> name.equals(series.getName()))
                    .findFirst();
            if (foundSeries.isPresent()) {
                foundSeries.get().setData(lineData);
            } else {
                chartData.add(new XYChart.Series<>(name, lineData));
            }
        });
    }

    @NotNull
    protected ObservableList<XYChart.Data<Number, Number>> calcLineData(ChartExpr chartExpr, List<Number> numbers) {
        return FXCollections
                .observableArrayList(IntStream.range(0, numbers.size()).mapToObj(
                        i -> {
                            double x = chartExpr.getXBase() + chartExpr.getXScale() * i;
                            double y = chartExpr.getYBase() + chartExpr.getYScale() * numbers.get(i).doubleValue();
                            return new XYChart.Data<>((Number) x, (Number) y);
                        }
                ).collect(Collectors.toList()));
    }

    public boolean isSampled(String name) {
        return lineChart.getData().parallelStream().map(XYChart.Series::getName).anyMatch(name::equals);
    }

    private static class ChartExpressionData {
        private final List<List<Number>> data = new ArrayList<>(MAX_SERIES);
        private final AtomicInteger currentIndex = new AtomicInteger();
    }
}
