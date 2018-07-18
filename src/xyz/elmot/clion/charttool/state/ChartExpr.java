package xyz.elmot.clion.charttool.state;

import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChartExpr {

    @NotNull
    private String expression = "";

    @Nullable
    private String name = "";

    private double xScale = 1;//todo support
    private double yScale = 1;//todo support

    private double xBase = 0;//todo support
    private double yBase = 0;//todo support

    @NotNull
    private ExpressionState state = ExpressionState.SAMPLE_ONCE;

    @Transient
    @NotNull
    private String expressionTrim = expression;

    public String getName() {

        return (name == null || "".equals(name)) ? expression : name;
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
