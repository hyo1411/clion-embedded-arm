package xyz.elmot.clion.charttool;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.ui.DocumentAdapter;
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
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.debugger.CidrEvaluator;
import javafx.scene.chart.XYChart;
import org.jdesktop.swingx.renderer.DefaultListRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import xyz.elmot.clion.openocd.OpenOcdLauncher;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.swing.border.BevelBorder.LOWERED;

public class ChartsPane extends JBTabbedPane implements XDebugSessionListener, XDebuggerManagerListener, XBreakpointListener<XBreakpoint<?>> {

    /* TODO fix read access */
    /* TODO refactor*/
    //todo better breakpoints renderer
    //todo checkbox to breakpoints renderer?
    //todo better var error handling
    //todo keep flag

    public static final Key<ChartExpr> CHART_EXPR_KEY = Key.create(ChartsPane.class.getName() + "#breakpoint");
    private final SortedListModel<XLineBreakpoint<?>> breakpoints = SortedListModel
            .create(XLineBreakpointComparator.COMPARATOR);
    private final JBCheckBox enableBP;
    private final Project project;
    private final JTextArea breakpointText;
    private final JBList<XLineBreakpoint<?>> bpList;
    private final ChartToolPersistence persistence;
    boolean innerUpdate = false;
    private ChartsPanel chartsPanel;

    public ChartsPane(Project project) {
        super(JBTabbedPane.TOP);
        this.project = project;
        persistence = project.getComponent(ChartToolPersistence.class);
        persistence.setChangeListener(this::setAllBreakpoints);
        setAllBreakpoints();
        XDebuggerManager.getInstance(this.project).getBreakpointManager().addBreakpointListener(this);
        JBPanel<JBPanel> breakPointsTab = new JBPanel<>(new BorderLayout(10, 10));
        breakpointText = new JTextArea();
        breakpointText.setSize(300, 300);
        breakpointText.setBorder(BorderFactory.createBevelBorder(LOWERED));
        JBPanel<JBPanel> bpSettingsPanel = new JBPanel<>(new BorderLayout());
        enableBP = new JBCheckBox("Enable");
        enableBP.addItemListener(evt -> saveBreakpointData());
        breakpointText.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                saveBreakpointData();
            }
        });
        bpSettingsPanel.add(enableBP, BorderLayout.NORTH);
        bpSettingsPanel.add(breakpointText, BorderLayout.CENTER);
        bpList = createBpList();

        breakPointsTab.add(bpList, BorderLayout.WEST);
        breakPointsTab.add(bpSettingsPanel, BorderLayout.CENTER);

        chartsPanel = new ChartsPanel();
        addTab("Chart", chartsPanel);
        addTab("Breakpoints", breakPointsTab);
        setSelectedIndex(0);
        invalidate();


    }

    private static String getBreakpointName(Object o) {
        XLineBreakpoint<?> breakpoint = (XLineBreakpoint<?>) o;
        return breakpoint.getShortFilePath() + ":" + breakpoint.getLine();
    }

    public static List<XLineBreakpoint<?>> getAllXLineBreakpoints(Project project) {
        XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
        XBreakpoint<?>[] xBreakpoints = ApplicationManager.getApplication()
                .runReadAction((Computable<XBreakpoint<?>[]>) breakpointManager::getAllBreakpoints);
        return Stream.of(xBreakpoints)
                .filter(bp -> bp instanceof XLineBreakpoint)
                .map(bp -> (XLineBreakpoint<?>) bp)
                .collect(Collectors.toList());
    }

    private static Icon breakpointIcon(Object o) {
        return ((XBreakpointBase) o).getIcon();
    }

    private void saveBreakpointData() {
        if (!innerUpdate) {
            XLineBreakpoint<?> breakpoint = bpList.getSelectedValue();
            if (breakpoint != null) {
                ChartExpr chartData = getOrCreateChartData(breakpoint);
                chartData.enabled = enableBP.isSelected();
                chartData.expression = breakpointText.getText();
                persistence.registerChange();
            }
        }
    }

    @NotNull
    protected JBList<XLineBreakpoint<?>> createBpList() {
        JBList<XLineBreakpoint<?>> bpList = new JBList<>(breakpoints);
        bpList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bpList.setBorder(BorderFactory.createBevelBorder(LOWERED));
        //noinspection unchecked
        bpList.setCellRenderer(new DefaultListRenderer(ChartsPane::getBreakpointName, ChartsPane::breakpointIcon));
        bpList.addListSelectionListener(evt -> {
            XLineBreakpoint<?> selected = bpList.getSelectedValue();
            innerUpdate = true;
            if (selected == null) {
                breakpointText.setText("");
                breakpointText.setEnabled(false);
                enableBP.setSelected(false);
                enableBP.setEnabled(false);
            } else {
                breakpointText.setEnabled(true);
                enableBP.setEnabled(true);
                ChartExpr chartData = getChartData(selected);
                if (chartData == null) {
                    breakpointText.setText("");
                    enableBP.setSelected(false);
                } else {
                    breakpointText.setText(chartData.expression);
                    enableBP.setSelected(chartData.enabled);
                }
            }
            innerUpdate = false;
        });
        bpList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    XLineBreakpoint<?> breakpoint = bpList.getSelectedValue();
                    if (breakpoint != null && breakpoint.getNavigatable() != null) {
                        breakpoint.getNavigatable().navigate(true);
                    }
                }
            }
        });
        return bpList;
    }

    protected void setAllBreakpoints() {
        this.breakpoints.setAll(getAllXLineBreakpoints(project));
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
        List<XLineBreakpoint<?>> allXLineBreakpoints = getAllXLineBreakpoints(project);
        for (XLineBreakpoint<?> breakpoint : allXLineBreakpoints) {
            if (XSourcePosition.isOnTheSameLine(currentPosition, breakpoint.getSourcePosition())) {
                ChartExpr chartExpr = breakpoint.getUserData(CHART_EXPR_KEY);
                if (chartExpr != null && chartExpr.enabled && !"".equals(chartExpr.expression.trim())) {
                    CidrDebugProcess debugProcess = (CidrDebugProcess) session.getDebugProcess();
                    Promise<String> sinDataPromise = debugProcess.postCommand(debuggerDriver ->
                            {
                                return ((OpenOcdLauncher.ExtendedGdbDriver) debuggerDriver)
                                        .extractValue("p " + chartExpr.expression.trim());
                            }
                    );
                    sinDataPromise.onError(this::showError);
                    sinDataPromise.onProcessed(v ->
                            {
                                try {
                                    v = v.replaceAll("^[^{]*\\{|\\s+|\\.{2,}}", "");
                                    List<XYChart.Data<Number, Number>> data = new ArrayList<>();
                                    int i = 1;
                                    for (Scanner scanner = new Scanner(v).useDelimiter(",|\\s|\\{|}(.{2})"); scanner
                                            .hasNext(); ) {
                                        data.add(new XYChart.Data<>(i++, Double.parseDouble(scanner.next())));
                                    }
                                    chartsPanel.series(chartExpr.expression, data);
                                    //todo parse error
                                } catch (Throwable e) {
                                    showError(e);
                                }
                            }
                    );

                }

            }
        }
    }

    protected void showError(Throwable rejected) {
        String message = rejected.getLocalizedMessage();
        String title = rejected.getClass().getSimpleName();
        ApplicationManager.getApplication().invokeLater(() ->
                com.intellij.openapi.ui.Messages.showErrorDialog(
                        message, title));
    }

    @Override
    public void breakpointChanged(@NotNull XBreakpoint<?> breakpoint) {
        bpList.repaint();
    }

    @Nullable
    public ChartExpr getChartData(XBreakpoint<?> breakpoint) {
        return breakpoint.getUserData(CHART_EXPR_KEY);
    }

    @NotNull
    public ChartExpr getOrCreateChartData(XBreakpoint<?> breakpoint) {
        ChartExpr chartExpr = breakpoint.getUserData(CHART_EXPR_KEY);
        if (chartExpr == null) {
            chartExpr = new ChartExpr();
            breakpoint.putUserData(CHART_EXPR_KEY, chartExpr);
        }
        return chartExpr;
    }

}
