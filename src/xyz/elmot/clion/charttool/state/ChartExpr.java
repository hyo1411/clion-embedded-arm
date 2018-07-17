package xyz.elmot.clion.charttool.state;

import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;

public class ChartExpr {

    @NotNull
    private String expression = "";

    @NotNull
    private ExpressionState state = ExpressionState.SAMPLE_ONCE;

    @Transient
    @NotNull
    private String expressionTrim = expression;

    public String getName() {//todo separate from expression
        return expression;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(@NotNull String expression) {
        this.expression = expression;
        expressionTrim = expression.trim();
    }

    @NotNull
    public String getExpressionTrim() {
        return expressionTrim;
    }

    @NotNull
    public ExpressionState getState() {
        return state;
    }

    public void setState(@NotNull ExpressionState state) {
        this.state = state;
    }
}
