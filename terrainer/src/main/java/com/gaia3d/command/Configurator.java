package com.gaia3d.command;

import com.gaia3d.process.ProcessOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.factory.PropertyAuthorityFactory;
import org.geotools.referencing.factory.ReferencingFactoryContainer;
import org.geotools.util.factory.Hints;
import org.opengis.referencing.crs.CRSAuthorityFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;

@Slf4j
public class Configurator {
    public static final Level LEVEL = Level.ALL;
    private static final String DEFAULT_PATTERN = "%message%n";

    public static void initConsoleLogger() {
        initConsoleLogger(null);
    }

    public static void initConsoleLogger(String pattern) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

        removeAllAppender(loggerConfig);
        if (pattern == null) {
            pattern = DEFAULT_PATTERN;
        }
        PatternLayout layout = createPatternLayout(pattern);
        ConsoleAppender consoleAppender = createConsoleAppender(layout);

        loggerConfig.setLevel(LEVEL);
        loggerConfig.addAppender(consoleAppender, LEVEL, null);
        ctx.updateLoggers();

        consoleAppender.start();
    }

    public static void initFileLogger(String pattern, String path) throws IOException {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

        if (pattern == null) {
            pattern = DEFAULT_PATTERN;
        }
        PatternLayout layout = createPatternLayout(pattern);
        FileAppender fileAppender = createRollingFileAppender(layout, path);

        loggerConfig.setLevel(LEVEL);
        loggerConfig.addAppender(fileAppender, LEVEL, null);
        ctx.updateLoggers();
        fileAppender.start();
    }

    public static void destroyLogger() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

        Appender appender = loggerConfig.getAppenders().get("FileLogger");
        if (appender != null) {
            appender.stop();
        }

        removeAllAppender(loggerConfig);
        ctx.updateLoggers();
    }

    public static Options createOptions() {
        Options options = new Options();
        for (ProcessOptions processOptions : ProcessOptions.values()) {
            options.addOption(processOptions.getShortName(), processOptions.getLongName(), processOptions.isArgRequired(), processOptions.getDescription());
        }
        return options;
    }

    public static void setLevel(Level level) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(level);
        ctx.updateLoggers();
    }

    private static PatternLayout createPatternLayout(String pattern) {
        return PatternLayout.newBuilder().withPattern(pattern).withCharset(StandardCharsets.UTF_8).build();
    }

    private static FileAppender createRollingFileAppender(PatternLayout layout, String path) throws IOException {
        if (path == null) {
            path = "logs/gaia3d-mesher.log";
        }
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            File backup = new File(path + "_" + file.lastModified());
            Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return FileAppender.newBuilder().setName("FileLogger").withFileName(path).withAppend(true).withImmediateFlush(true).withBufferedIo(true).withBufferSize(8192).setLayout(layout).build();
    }

    private static ConsoleAppender createConsoleAppender(PatternLayout layout) {
        return ConsoleAppender.newBuilder().setName("Console").setTarget(ConsoleAppender.Target.SYSTEM_OUT).setLayout(layout).build();
    }

    private static void removeAllAppender(LoggerConfig loggerConfig) {
        loggerConfig.getAppenders().forEach((key, value) -> loggerConfig.removeAppender(key));
    }

    public static void setEpsg() throws IOException {
        Hints hints = new Hints(Hints.CRS_AUTHORITY_FACTORY, PropertyAuthorityFactory.class);
        log.debug("[Geotools]=================={}==================}", ReferencingFactoryFinder.getCRSAuthorityFactories(hints).size());

        URL epsg = Thread.currentThread().getContextClassLoader().getResource("epsg.properties");
        if (epsg != null) {
            ReferencingFactoryContainer referencingFactoryContainer = ReferencingFactoryContainer.instance(hints);
            ReferencingFactoryFinder.scanForPlugins();
            Set<CRSAuthorityFactory> factories = ReferencingFactoryFinder.getCRSAuthorityFactories(hints);
            log.debug("[Geotools]=================={}==================}", factories.size());
            factories.forEach(f -> {
                log.debug("{}", f);
            });
        }
    }
}
