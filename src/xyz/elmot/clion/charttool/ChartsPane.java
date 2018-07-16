package xyz.elmot.clion.charttool;

import com.intellij.openapi.project.Project;
import com.intellij.ui.SortedListModel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.xdebugger.*;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointListener;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.frame.XStackFrame;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.debugger.CidrEvaluator;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import xyz.elmot.clion.openocd.OpenOcdLauncher;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static javax.swing.border.BevelBorder.LOWERED;

public class ChartsPane extends JBTabbedPane implements XDebugSessionListener, XDebuggerManagerListener, XBreakpointListener<XBreakpoint<?>> {

    private final SortedListModel<XLineBreakpoint> breakpoints = SortedListModel.create(null/*todo*/);
    private final JTextPane breakPointText;
    private final JBCheckBox enableBP;
    private final Project project;

    private CheckBox keep;
    private Button reset;
    private LineChart<Number, Number> lineChart;
    private Map<String, XYChart.Series<Number, Number>> seriesByName = new HashMap<>();
    private boolean initialized = false;
    private JFXPanel fxPanel;

    /* todo remove
        public static void main(String[] args) {
            JFrame jFrame = new JFrame();
            jFrame.setSize(1000, 800);
            jFrame.setContentPane(new ChartsPane());
            jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            jFrame.setVisible(true);
        }
    */

    public ChartsPane(Project project) {
        super(JBTabbedPane.TOP);
        this.project = project;
        addAllBreakpoints(project);

        XDebuggerManager.getInstance(project).getBreakpointManager().addBreakpointListener(
                this
        );
        JBPanel<JBPanel> breakPointsTab = new JBPanel<>(new BorderLayout(10, 10));
        new JBList<>(breakpoints).setBorder(BorderFactory.createBevelBorder(LOWERED));
        breakPointText = new JTextPane();
        breakPointText.setSize(300, 300);
        breakPointText.setBorder(BorderFactory.createBevelBorder(LOWERED));
        JBPanel<JBPanel> bpSettingsPanel = new JBPanel<>(new BorderLayout());
        enableBP = new JBCheckBox("Enable");
        bpSettingsPanel.add(enableBP, BorderLayout.NORTH);
        bpSettingsPanel.add(breakPointText, BorderLayout.CENTER);
        breakPointsTab.add(new JBList<>(breakpoints), BorderLayout.WEST);
        breakPointsTab.add(bpSettingsPanel, BorderLayout.CENTER);

        fxPanel = new JFXPanel();
        addTab("Chart", fxPanel);
        addTab("Breakpoints", breakPointsTab);
        setSelectedIndex(0);
        invalidate();
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
            fxPanel.setScene(scene);
            fxPanel.invalidate();
            initialized = true;
        });


    }

    protected void addAllBreakpoints(Project project) {
        @NotNull XBreakpoint<?>[] allBreakpoints = XDebuggerManager.getInstance(project).getBreakpointManager()
                .getAllBreakpoints();
        for (XBreakpoint<?> breakpoint : allBreakpoints) {
//            breakpoints.addElement(breakpoint);
        }
    }

    private void clear() {
        lineChart.getData().clear();
        Platform.runLater(seriesByName::clear);
    }

    @Override
    public void sessionPaused() {
        XDebuggerManager debuggerManager = XDebuggerManager.getInstance(project);
        XDebugSession session = debuggerManager.getCurrentSession();
        if (session == null) {
            return;
        }
        XStackFrame currentStackFrame = session.getCurrentStackFrame();
        if (currentStackFrame == null) {
            return;
        }
        XSourcePosition currentPosition = session.getCurrentPosition();
        if (currentPosition == null) {
            return;
        }
        CidrEvaluator evaluator = (CidrEvaluator) currentStackFrame.getEvaluator();
        if (evaluator == null) {
            return;
        }
        XBreakpointManager breakpointManager = debuggerManager.getBreakpointManager();
        @NotNull XBreakpoint<?>[] allBreakpoints = breakpointManager.getAllBreakpoints();
        for (XBreakpoint<?> breakpoint : allBreakpoints) {
            if (XSourcePosition.isOnTheSameLine(currentPosition, breakpoint.getSourcePosition())) {

                CidrDebugProcess debugProcess = (CidrDebugProcess) session.getDebugProcess();
                Promise<String> sinDataPromise = debugProcess.postCommand(debuggerDriver ->
                        {
                            return ((OpenOcdLauncher.ExtendedGdbDriver) debuggerDriver).extrectValue("p sinData");
                        }
                );
                sinDataPromise.onProcessed(v ->
                        System.out.println("v = " + v)
                );

            }
        }
    }

    @Override
    public void currentSessionChanged(@Nullable XDebugSession previousSession, @Nullable XDebugSession currentSession) {
        if (previousSession != null) {
            previousSession.removeSessionListener(this);
        }
        if (currentSession != null) {
            currentSession.addSessionListener(this);
        } else {
            breakpoints.clear();
        }
    }

    @Override
    public void breakpointAdded(@NotNull XBreakpoint<?> breakpoint) {
        if (breakpoint instanceof XLineBreakpoint) {
            breakpoints.add((XLineBreakpoint) breakpoint);
        }
    }

    @Override
    public void breakpointRemoved(@NotNull XBreakpoint<?> breakpoint) {

        if (breakpoint instanceof XLineBreakpoint) {
            breakpoints.remove((XLineBreakpoint) breakpoint);
        }
    }

    @Override
    public void breakpointChanged(@NotNull XBreakpoint<?> breakpoint) {
        //todo
    }
}
