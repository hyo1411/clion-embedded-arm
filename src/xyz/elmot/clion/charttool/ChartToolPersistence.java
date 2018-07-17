package xyz.elmot.clion.charttool;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponentWithModificationTracker;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static xyz.elmot.clion.charttool.ChartTool.CHART_EXPR_KEY;

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

        for (XLineBreakpoint<?> breakpoint : SignalSources.getAllXLineBreakpoints(project)) {
            ChartExpr chartData = breakpoint.getUserData(CHART_EXPR_KEY);
            if (chartData != null) {
                state.addChartBreakPoint(breakpoint.getFileUrl(), breakpoint.getLine(), chartData);
            }
        }
        return state;
    }

    @Override
    public void loadState(@NotNull ChartToolState state) {
        ApplicationManager.getApplication().runReadAction(() -> {

            for (XLineBreakpoint<?> breakpoint : SignalSources.getAllXLineBreakpoints(project)) {
                ChartExpr chartExpr = state.expressions
                        .get(new ChartToolState.Location(breakpoint.getFileUrl(), breakpoint.getLine()));
                breakpoint.putUserData(CHART_EXPR_KEY, chartExpr);
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

}
