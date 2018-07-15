package xyz.elmot.clion.openocd;

import com.intellij.execution.filters.Filter;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChartFilter implements Filter {

    private static final Pattern DATA_DETECTOR = Pattern.compile("^0x[0-9A-F]+\\s*<(\\w+)[^>]*>:\\s*(.*)\\s*", Pattern.CASE_INSENSITIVE);
    @Nullable
    private String lastNameDetected;
    private List<Number> acc = new ArrayList<>();

    public static final Topic<DataListener> CHART_DATA_FLOW = new Topic<>("ChartDataFlow", DataListener.class);
    private final DataListener dataListener;

    public ChartFilter(Project theProject) {
        dataListener = theProject.getMessageBus().syncPublisher(CHART_DATA_FLOW);
    }

    @Nullable
    @Override
    public Result applyFilter(String line, int entireLength) {
        if("".equals(line)) return null;
        Matcher matcher = DATA_DETECTOR.matcher(line);
        if (matcher.matches()) {

            String name = matcher.group(1);
            flush(name);
            for (Scanner scanner = new Scanner(matcher.group(2)); scanner.hasNext(); ) {
                String s = scanner.next();
                try {
                    if (s.indexOf('.') >= 0) {
                        acc.add(Double.parseDouble(s));
                    } else {
                        acc.add(Long.parseLong(s));
                    }
                } catch (NumberFormatException e) {
                    acc.add(Double.NaN);
                }
            }
        } else {
            flush(null);
        }
        return null;
    }

    private void flush(String newName) {
        if(Objects.equals(newName,lastNameDetected)) return;
        if(lastNameDetected != null)
        {
            dataListener.dataDetected(lastNameDetected, acc);
            acc = new ArrayList<>();
        }
        lastNameDetected = newName;
    }

    public interface DataListener {
        void dataDetected(String name, Collection<Number> data);
    }
}
