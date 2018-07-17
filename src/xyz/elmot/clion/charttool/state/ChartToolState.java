package xyz.elmot.clion.charttool.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartToolState {

    private final Map<Location, LineState> locations = new HashMap<>();
    private final List<ChartExpr> exprs = new ArrayList<>();

    public Map<Location, LineState> getLocations() {
        return locations;
    }

    public List<ChartExpr> getExprs() {
        return exprs;
    }
}
