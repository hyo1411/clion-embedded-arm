package xyz.elmot.clion.charttool;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SortedListModel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerManagerListener;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointListener;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase;
import org.jdesktop.swingx.JXRadioGroup;
import org.jdesktop.swingx.renderer.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.elmot.clion.charttool.state.ChartExpr;
import xyz.elmot.clion.charttool.state.LineState;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.swing.border.BevelBorder.LOWERED;
import static xyz.elmot.clion.charttool.ChartTool.CHART_EXPR_KEY;

public class SignalSources extends JBPanel<SignalSources> implements XDebuggerManagerListener, XBreakpointListener<XBreakpoint<?>> {

    /*todo select first */
    /*todo different model*/
    //todo better breakpoints renderer
    //todo checkbox to breakpoints renderer?
    //todo keep flag

    private final SortedListModel<XLineBreakpoint<?>> breakpoints = SortedListModel
            .create(XLineBreakpointComparator.COMPARATOR);
    private final JBCheckBox enableBP;
    private final Project project;
    private final JTextArea breakpointText;
    private final JBList<XLineBreakpoint<?>> bpList;
    private final ChartToolPersistence persistence;
    private final DebugListener debugListener;
    boolean innerUpdate = false;

    public SignalSources(Project project, DebugListener debugListener, ChartToolPersistence persistence) {
        super(new BorderLayout(10, 10));
        this.project = project;
        this.debugListener = debugListener;
        this.persistence = persistence;
        this.persistence.setChangeListener(this::setAllBreakpoints);
        setAllBreakpoints();
        breakpointText = new JTextArea();
        breakpointText.setBorder(BorderFactory.createBevelBorder(LOWERED));
        breakpointText.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                saveBreakpointData();
            }
        });
        bpList = createBpList();

        add(bpList, BorderLayout.WEST);
        add(breakpointText, BorderLayout.CENTER);

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
                ChartExpr chartExpr = breakpoint.getUserData(CHART_EXPR_KEY);
                if (chartExpr == null) {
                    chartExpr = new ChartExpr();
                    breakpoint.putUserData(CHART_EXPR_KEY, chartExpr);
                }
                ChartExpr chartData = chartExpr;
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
        bpList.setCellRenderer(
                new DefaultListRenderer(new ComponentProvider<JRendererPanel>() {
                    @Override
                    protected void format(CellContext cellContext) {

                    }

                    @Override
                    protected void configureState(CellContext cellContext) {
                        cellContext.getComponent().setBackground(cellContext.getB);
                    }

                    @Override
                    protected JRendererPanel createRendererComponent() {
                        JRendererPanel panel = new JRendererPanel();
                        panel.add(new JRendererLabel());
                        panel.add(new JXRadioGroup<>(LineState.values()));
                        return panel;
                    }
                })
        new DefaultListRenderer(SignalSources::getBreakpointName, SignalSources::breakpointIcon)
        )


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
                ChartExpr chartData = selected.getUserData(CHART_EXPR_KEY);
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
            previousSession.removeSessionListener(debugListener);
        }
        if (currentSession != null) {
            currentSession.addSessionListener(debugListener);
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
        bpList.repaint();
    }

}
