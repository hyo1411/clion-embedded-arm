package xyz.elmot.clion.openocd;

import com.intellij.execution.configuration.ConfigurationFactoryEx;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.jetbrains.cidr.cpp.execution.CMakeRunConfigurationType;
import kotlin.Lazy;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * (c) elmot on 29.9.2017.
 */
public class OpenOcdConfigurationType extends CMakeRunConfigurationType {

    private static final String FACTORY_ID = "elmot.embedded.openocd.conf.factory";
    public static final String TYPE_ID = "elmot.embedded.openocd.conf.type";
    private final ConfigurationFactory factory;

    public OpenOcdConfigurationType() {
        //noinspection ConstantConditions
        super(TYPE_ID,
                FACTORY_ID,
                "OpenOCD Download & Run",
                "Downloads and Runs Embedded Applications using OpenOCD",
                new Lazy<Icon>() {
                    Icon icon;

                    @Override
                    public Icon getValue() {
                        if (icon == null) {
                            icon = getPluginIcon();
                        }
                        return icon;
                    }

                    @Override
                    public boolean isInitialized() {
                        return icon != null;
                    }
                }
        );
        factory = new ConfigurationFactoryEx(this) {
            @NotNull
            @Override
            public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
                return new OpenOcdConfiguration(project, factory, "");
            }

            @Override
            public boolean isConfigurationSingletonByDefault() {
                return true;
            }

            @Override
            public String getId() {
                return FACTORY_ID;
            }
        };
    }

    public static Icon getPluginIcon() {
        return IconLoader.findIcon("ocd_run.png", OpenOcdConfigurationType.class);
    }

    @Override
    public OpenOcdConfigurationEditor createEditor(@NotNull Project project) {
        return new OpenOcdConfigurationEditor(project, getHelper(project));
    }

    @NotNull
    @Override
    protected OpenOcdConfiguration createRunConfiguration(@NotNull Project project,
                                                          @NotNull ConfigurationFactory configurationFactory) {
        return new OpenOcdConfiguration(project, factory, "");
    }
}
