package xyz.elmot.clion.charttool;

import com.intellij.openapi.util.Key;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.impl.frame.XDebugViewSessionListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ChartDataExtractor extends JBTabbedPane {
    private XDebugSession debugSession;

    private XDebugSessionListener xDebugViewSessionListener;

    private static final Key<List<ChartExpr>> CHART_DATA_KEY = Key.create(ChartDataExtractor.class.getName() + "#breakpoint");

    public ChartDataExtractor(XDebugSession debugSession) {
        this.debugSession = debugSession;
    }

    @NotNull
    public List<ChartExpr> getChartData(XBreakpoint<?> breakpoint) {
        List<ChartExpr> chartExprs = breakpoint.getUserData(CHART_DATA_KEY);
        if(chartExprs ==null) {
            chartExprs = new ArrayList<>();
            breakpoint.putUserData(CHART_DATA_KEY, chartExprs);
        }
        return chartExprs;
    }

    public XBreakpoint<?>[] getBreakPoints() {
        return XDebuggerManager.getInstance(debugSession.getProject()).getBreakpointManager().getAllBreakpoints();
    }

    /* TODO persistence */
    public static class ChartExpr {
        public boolean accumulate;
        public String expression;
    }

//    @Override
    public void sessionPaused() {

    }
}
