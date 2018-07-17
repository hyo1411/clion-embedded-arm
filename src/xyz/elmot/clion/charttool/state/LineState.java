package xyz.elmot.clion.charttool.state;

public enum LineState {
    DISABLED("No Sampling", "N"),
    SAMPLE("Perform Sample", "S"),
    CLEAR("Clear Chart", "C"),
    CLEAR_AND_SAMPLE("Clear Chart & Sample", "CS");

    public final String buttonLabel;
    public final String hint;

    LineState(String hint, String buttonLabel) {
        this.buttonLabel = buttonLabel;
        this.hint = hint;
    }

    @Override
    public String toString() {
        return buttonLabel;
    }
}
