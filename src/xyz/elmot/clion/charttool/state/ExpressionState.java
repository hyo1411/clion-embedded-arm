package xyz.elmot.clion.charttool.state;

public enum ExpressionState {
    DISABLED("Disabled", "N"),
    SAMPLE_ONCE("Sample once after clear data", "1"),
    ALWAYS_REFRESH("Refresh on breakpoint", "R"),
    ACCUMULATE("Keep All Series", "A");
    public final String buttonLabel;
    public final String hint;

    ExpressionState(String hint, String buttonLabel) {
        this.hint = hint;
        this.buttonLabel = buttonLabel;
    }

    @Override
    public String toString() {
        return buttonLabel;
    }
}
