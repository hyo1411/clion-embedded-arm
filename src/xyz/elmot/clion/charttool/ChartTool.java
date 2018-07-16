package xyz.elmot.clion.charttool;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.xdebugger.XDebuggerManager;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;


public class ChartTool implements ToolWindowFactory {
    static {
        Platform.setImplicitExit(false);
    }


    public ChartTool() {
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
//todo use standard tabbing
        ChartsPane chartsPane = new ChartsPane(project);
        project.getMessageBus().connect().subscribe(XDebuggerManager.TOPIC, chartsPane);
        toolWindow.getComponent().add(chartsPane);
    }
}
