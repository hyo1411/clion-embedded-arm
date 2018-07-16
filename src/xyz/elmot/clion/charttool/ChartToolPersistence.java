package xyz.elmot.clion.charttool;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponentWithModificationTracker;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "charttool")
public class ChartToolPersistence implements PersistentStateComponentWithModificationTracker<ChartToolState> {
    private final Project project;
    private long modificationsCount = 0;
    private Runnable changeListener;

    public ChartToolPersistence(Project project) {
        this.project = project;
    }

    @Override
    public long getStateModificationCount() {
        return modificationsCount;
    }

    @Nullable
    @Override
    public ChartToolState getState() {
        ChartToolState state = new ChartToolState();

        for (XLineBreakpoint<?> breakpoint : ChartsPane.getAllXLineBreakpoints(project)) {
            ChartExpr chartData = breakpoint.getUserData(ChartsPane.CHART_EXPR_KEY);
            if (chartData != null) {
                state.addChartBreakPoint(breakpoint.getFileUrl(), breakpoint.getLine(), chartData);
            }
        }
        return state;
    }

    @Override
    public void loadState(@NotNull ChartToolState state) {
        ApplicationManager.getApplication().runReadAction(() -> {

            for (XLineBreakpoint<?> breakpoint : ChartsPane.getAllXLineBreakpoints(project)) {
                ChartExpr chartExpr = state.expressions
                        .get(new ChartToolState.Location(breakpoint.getFileUrl(), breakpoint.getLine()));
                breakpoint.putUserData(ChartsPane.CHART_EXPR_KEY, chartExpr);
            }
            if (changeListener != null) {
                changeListener.run();
            }
        });
    }

    public void registerChange() {
        modificationsCount++;
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener;
    }

    protected XBreakpointManager getBreakpointManager() {
        return XDebuggerManager.getInstance(project).getBreakpointManager();
    }

}
