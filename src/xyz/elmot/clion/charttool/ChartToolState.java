package xyz.elmot.clion.charttool;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ChartToolState {

    public final Map<Location, ChartExpr> expressions = new HashMap<>();

    public void addChartBreakPoint(@NotNull String fileUrl, int line, @NotNull ChartExpr chartData) {
        expressions.put(new Location(fileUrl, line), chartData);
    }

    public static class Location {
        @NotNull
        public String fileUrl = "";
        public int lineNo;

        public Location(@NotNull String fileUrl, int lineNo) {
            this.fileUrl = fileUrl;
            this.lineNo = lineNo;
        }

        @SuppressWarnings("unused")
        public Location() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Location location = (Location) o;
            return lineNo == location.lineNo &&
                    Objects.equals(fileUrl, location.fileUrl);
        }

        @Override
        public int hashCode() {

            return Objects.hash(fileUrl, lineNo);
        }
    }

}
