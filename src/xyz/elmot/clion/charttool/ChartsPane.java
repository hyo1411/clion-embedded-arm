package xyz.elmot.clion.charttool;

import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
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

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ChartsPane extends JBTabbedPane {

    private final JBList<String> bpList;
    private final JTextPane breakPointText;

    private CheckBox keep;
    private Button reset;
    private LineChart<Number, Number> lineChart;
    private Map<String, XYChart.Series<Number, Number>> seriesByName = new HashMap<>();
    private boolean initialized = false;
    private JFXPanel fxPanel;


    public static void main(String[] args) {
        JFrame jFrame = new JFrame();
        jFrame.setSize(1000,800);
        jFrame.setContentPane(new ChartsPane());
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setVisible(true);
    }

    public ChartsPane() {
        super(JBTabbedPane.TOP);
        JBPanel<JBPanel> breakPointsTab = new JBPanel<>(new FlowLayout());
        bpList = new JBList<>("One", "Two", "Three", "Five");
        breakPointText = new JTextPane();
        breakPointsTab.setS
        breakPointsTab.add(bpList, breakPointText);

        addTab("Breakpoints",breakPointsTab);

        JFXPanel chartPanel = new JFXPanel();
        addTab("Chart",chartPanel);

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


    }
    private void clear() {
        lineChart.getData().clear();
        Platform.runLater(seriesByName::clear);
    }
}
