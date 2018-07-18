package xyz.elmot.clion.charttool;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerManagerListener;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.Nullable;
import xyz.elmot.clion.charttool.ui.BreakpointList;
import xyz.elmot.clion.charttool.ui.ExprList;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.swing.border.BevelBorder.LOWERED;

public class SignalSources extends JBPanel<SignalSources> implements XDebuggerManagerListener {

    /*todo select first */
    /*todo different model*/
    //todo better breakpoints renderer
    //todo checkbox to breakpoints renderer?
    //todo keep flag

    private final Project project;
    private final BreakpointList bpList;
    private final ChartToolPersistence persistence;
    private final DebugListener debugListener;

    public SignalSources(Project project, DebugListener debugListener, ChartToolPersistence persistence) {
        setLayout(new BorderLayout(10, 10));
        setBorder(JBUI.Borders.empty(15));
        this.project = project;
        this.debugListener = debugListener;
        this.persistence = persistence;
        this.persistence.setChangeListener(this::setAllBreakpoints);
        bpList = new BreakpointList(persistence);
        bpList.setBorder(IdeBorderFactory.createTitledBorder("Breakpoints", true, JBUI.insets(10)));
        setAllBreakpoints();
        ExprList exprList = new ExprList(persistence);
        exprList.setBorder(IdeBorderFactory.createTitledBorder("Expressions"));
        add(bpList, BorderLayout.EAST);
        add(exprList, BorderLayout.CENTER);
        XDebuggerManager.getInstance(project).getBreakpointManager().addBreakpointListener(bpList);

        invalidate();
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

    protected void setAllBreakpoints() {
        bpList.setAllBreakpoints(getAllXLineBreakpoints(project));
    }

    @Override
    public void currentSessionChanged(@Nullable XDebugSession previousSession, @Nullable XDebugSession currentSession) {
        if (previousSession != null) {
            previousSession.removeSessionListener(debugListener);
        }
        if (currentSession != null) {
            currentSession.addSessionListener(debugListener);
        } else {
            bpList.setAllBreakpoints(null);
        }
    }


}
