package xyz.elmot.clion.charttool.ui;

import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.ui.AddEditRemovePanel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.panels.VerticalLayout;
import org.jdesktop.swingx.JXRadioGroup;
import org.jetbrains.annotations.Nullable;
import xyz.elmot.clion.charttool.ChartToolPersistence;
import xyz.elmot.clion.charttool.state.ChartExpr;
import xyz.elmot.clion.charttool.state.ExpressionState;

import java.util.stream.Stream;

public class ExprList extends AddEditRemovePanel<ChartExpr> {

    private final ChartToolPersistence persistence;

    public ExprList(ChartToolPersistence persistence) {
        super(createModel(), persistence.getExprs());
        this.persistence = persistence;
    }

    private static TableModel<ChartExpr> createModel() {
        return new TableModel<ChartExpr>() {
            @Override
            public int getColumnCount() {
                return 3;
            }

            @Override
            public String getColumnName(int columnIndex) {
                return new String[]{"Name", "State", "Expression"}[columnIndex];
            }

            @Override
            public Object getField(ChartExpr chartExpr, int columnIndex) {
                switch (columnIndex) {
                    case 0:
                        return chartExpr.getName();
                    case 1:
                        return chartExpr.getState();
                    default:
                        return chartExpr.getExpression();
                }
            }
        };
    }

    @Override
    protected ChartExpr addItem() {
        persistence.registerChange();
        return doEdit(new ChartExpr());
    }

    @Override
    protected boolean removeItem(ChartExpr chartExpr) {
        persistence.registerChange();
        return true;
    }

    @Nullable
    @Override
    protected ChartExpr editItem(ChartExpr chartExpr) {

        return doEdit(chartExpr);

    }

    @Nullable
    private ChartExpr doEdit(ChartExpr chartExpr) {
        JBTextField jbTextField = new JBTextField(chartExpr.getExpression());
        JXRadioGroup<ExpressionState> stateGroup = new JXRadioGroup<>(ExpressionState.values());
        Stream.of(ExpressionState.values()).forEach(v->stateGroup.getChildButton(v).setToolTipText(v.hint));
        stateGroup.setSelectedValue(chartExpr.getState());
        JBPanel<JBPanel> dataPanel = new JBPanel<>(new VerticalLayout(20));
        dataPanel.add(jbTextField);
        dataPanel.add(stateGroup);
        DialogBuilder dialogBuilder = new DialogBuilder(this)
                .centerPanel(dataPanel)
                .title("Edit expression");
        dialogBuilder.addOkAction();
        if(dialogBuilder.showAndGet())
        {
            chartExpr.setExpression(jbTextField.getText());
            chartExpr.setState(stateGroup.getSelectedValue());
            persistence.registerChange();
            return chartExpr;
        }
        return null;
    }
}
