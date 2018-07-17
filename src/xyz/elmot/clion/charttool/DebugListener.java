package xyz.elmot.clion.charttool;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.frame.XStackFrame;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.debugger.CidrEvaluator;
import javafx.scene.chart.XYChart;
import org.jetbrains.concurrency.Promise;
import xyz.elmot.clion.openocd.OpenOcdLauncher;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import static xyz.elmot.clion.charttool.ChartTool.CHART_EXPR_KEY;
import static xyz.elmot.clion.charttool.SignalSources.getAllXLineBreakpoints;

public class DebugListener implements XDebugSessionListener {
    private static final Pattern ARRAY_STRIPPER = Pattern.compile("^[^{]*\\{|\\s+|}$");
    private final Project project;
    private final ChartsPanel chartsPanel;
    private final XDebuggerManager debuggerManager;

    public DebugListener(Project project, ChartsPanel chartsPanel) {
        this.project = project;
        this.chartsPanel = chartsPanel;
        debuggerManager = XDebuggerManager.getInstance(project);
    }

    protected void showError(Throwable rejected, String chartExpression) {
        String message = chartExpression + ": " + rejected.getLocalizedMessage();
        String title = rejected.getClass().getSimpleName();
        ApplicationManager.getApplication().invokeLater(() ->
                com.intellij.openapi.ui.Messages.showErrorDialog(
                        message, title));
    }

    @Override
    public void sessionPaused() {
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
        List<XLineBreakpoint<?>> allXLineBreakpoints = getAllXLineBreakpoints(project);
        for (XLineBreakpoint<?> breakpoint : allXLineBreakpoints) {
            if (XSourcePosition.isOnTheSameLine(currentPosition, breakpoint.getSourcePosition())) {
                ChartExpr chartExpr = breakpoint.getUserData(CHART_EXPR_KEY);

                if (chartExpr != null && chartExpr.enabled) {
                    String chartExpression = chartExpr.expression.trim();
                    if (!"".equals(chartExpression)) {
                        CidrDebugProcess debugProcess = (CidrDebugProcess) session.getDebugProcess();
                        Promise<String> sinDataPromise = debugProcess.postCommand(debuggerDriver ->
                                {
                                    return ((OpenOcdLauncher.ExtendedGdbDriver) debuggerDriver)
                                            .extractValue("p/r " + chartExpression);
                                }
                        );
                        sinDataPromise.onError(e -> showError(e, chartExpression))
                                .onProcessed(s -> processGdbOutput(s, chartExpression));

                    }
                }

            }
        }
    }

    private void processGdbOutput(String v, String chartExpression) {
        if (v != null) {
            try {
                String strippedV = ARRAY_STRIPPER.matcher(v).replaceAll("");
                List<XYChart.Data<Number, Number>> data = new ArrayList<>();
                int i = 1;
                for (StringTokenizer tokenizer = new StringTokenizer(strippedV, ","); tokenizer
                        .hasMoreTokens(); ) {
                    data.add(new XYChart.Data<>(i++, Double.parseDouble(tokenizer.nextToken())));
                }
                chartsPanel.series(chartExpression, data);
            } catch (Throwable e) {
                showError(e, chartExpression);
            }
        }
    }

}
