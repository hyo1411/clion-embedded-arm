package xyz.elmot.clion.charttool.ui;

import com.intellij.pom.Navigatable;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointListener;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase;
import org.jdesktop.swingx.JXRadioGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.elmot.clion.charttool.ChartToolPersistence;
import xyz.elmot.clion.charttool.state.LineState;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static xyz.elmot.clion.charttool.ChartTool.CHART_EXPR_KEY;

public class BreakpointList extends JBScrollPane implements XBreakpointListener<XBreakpoint<?>> {

    private final JBPanel<JBPanel> panel;
    private final SortedMap<XLineBreakpoint<?>, JBPanel> breakpoints = new TreeMap<>(XLineBreakpointComparator.COMPARATOR);
    private final ChartToolPersistence persistence;

    public BreakpointList(ChartToolPersistence persistence) {
        super(new JBPanel<>(new VerticalLayout(0)));
        //noinspection unchecked
        panel = (JBPanel<JBPanel>) getViewport().getView();
        this.persistence = persistence;
    }

    public void setAllBreakpoints(@Nullable Collection<XLineBreakpoint<?>> breakpoints) {
        this.breakpoints.clear();
        if (breakpoints != null) {
            breakpoints.forEach(this::addBreakpointToMap);
        }
        rebuild();
    }

    private void addBreakpointToMap(@NotNull XLineBreakpoint<?> breakpoint) {
        JBPanel<JBPanel> itemPanel = new JBPanel<>(new BorderLayout(8,0));
        JBLabel label = new JBLabel();
        if (breakpoint instanceof XBreakpointBase) {
            label.setIcon(((XBreakpointBase) breakpoint).getIcon());
        }
        label.setText(breakpoint.getShortFilePath() + ":" + breakpoint.getLine());
        JXRadioGroup<LineState> stateGroup = new JXRadioGroup<>(LineState.values());
        Stream.of(LineState.values()).forEach(v->stateGroup.getChildButton(v).setToolTipText(v.buttonLabel));
        stateGroup.setSelectedValue(CHART_EXPR_KEY.get(breakpoint, LineState.DISABLED));
        stateGroup.setOpaque(false);
        stateGroup.addActionListener(e -> {
            breakpoint.putUserData(CHART_EXPR_KEY, stateGroup.getSelectedValue());
            persistence.registerChange();

        });
        itemPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Navigatable navigatable = breakpoint.getNavigatable();
                if (navigatable != null) {
                    navigatable.navigate(e.getClickCount() > 1);
                }
            }
        });
        itemPanel.add(label,BorderLayout.CENTER);
        itemPanel.add(stateGroup,BorderLayout.EAST);
        breakpoints.put(breakpoint, itemPanel);
    }

    @Override
    public void breakpointAdded(@NotNull XBreakpoint<?> breakpoint) {
        if (breakpoint instanceof XLineBreakpoint) {
            addBreakpointToMap((XLineBreakpoint<?>) breakpoint);
            rebuild();
        }
    }

    @Override
    public void breakpointRemoved(@NotNull XBreakpoint<?> breakpoint) {
        if (breakpoint instanceof XLineBreakpoint) {
            breakpoints.remove(breakpoint);
            rebuild();
        }
    }

    @Override
    public void breakpointChanged(@NotNull XBreakpoint<?> breakpoint) {
        //the key is changed; there is a chance that it's not searchable anymore
        if (breakpoint instanceof XLineBreakpoint) {
            if (breakpoints.remove(breakpoint) == null) {
                breakpoints.keySet().removeIf(b -> b == breakpoint);
            }
            addBreakpointToMap((XLineBreakpoint<?>) breakpoint);
            rebuild();
        }
    }

    private void rebuild() {
        panel.removeAll();
        breakpoints.values().forEach(panel::add);
        panel.revalidate();
        panel.repaint();
    }
}
