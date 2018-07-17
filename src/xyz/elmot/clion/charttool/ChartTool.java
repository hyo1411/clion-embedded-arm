package xyz.elmot.clion.charttool;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.xdebugger.XDebuggerManager;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;


public class ChartTool implements ToolWindowFactory {
    public static final Key<ChartExpr> CHART_EXPR_KEY = Key.create(ChartTool.class.getName() + "#breakpoint");

    static {
        Platform.setImplicitExit(false);
    }


    public ChartTool() {
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        ContentFactory factory = contentManager.getFactory();
        ChartsPanel chartsPanel = new ChartsPanel();
        contentManager.addContent(
                factory.createContent(chartsPanel, "Chart", true)
        );

        DebugListener debugListener = new DebugListener(project, chartsPanel);
        SignalSources sources = new SignalSources(project, debugListener);
        contentManager.addContent(
                factory.createContent(sources, "Breakpoints", true)
        );

        XDebuggerManager.getInstance(project).getBreakpointManager().addBreakpointListener(sources);
        project.getMessageBus().connect().subscribe(XDebuggerManager.TOPIC, sources);

    }
}
