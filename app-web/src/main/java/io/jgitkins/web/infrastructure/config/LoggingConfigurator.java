package io.jgitkins.web.infrastructure.config;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

public class LoggingConfigurator implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final String DEFAULT_LOG_PATH = System.getProperty("user.home") + "/notificator/logs";
    private static final String CONSOLE_PATTERN =
            "%d{yyyy-MM-dd HH:mm:ss} %highlight(%-5level) [%X{traceId:-}] [%thread] %cyan(%logger{36}) - %msg%n";
    private static final String FILE_PATTERN =
            "%d{yyyy-MM-dd HH:mm:ss} %-5level [%X{traceId:-}] [%thread] %logger{36} - %msg%n";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ConfigurableEnvironment environment = event.getEnvironment();
        boolean isLocal = "local".equalsIgnoreCase(environment.getProperty("spring.profiles.active", "local"));

        loggerContext.reset();

        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(isLocal ? Level.INFO : Level.WARN);
        rootLogger.addAppender(createAsyncConsoleAppender(loggerContext));

        configureCommonLoggers(loggerContext);
        configureProfileLoggers(loggerContext, isLocal);

        if (!isLocal) {
            rootLogger.addAppender(createAsyncFileAppender(loggerContext, environment));
        }
    }

    private void configureCommonLoggers(LoggerContext loggerContext) {
        setLogger(loggerContext, "org.springframework", Level.INFO);
        setLogger(loggerContext,
                "org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver", Level.ERROR);
        setLogger(loggerContext, "org.springframework.beans.factory", Level.WARN);
        setLogger(loggerContext, "org.springframework.jdbc.support", Level.WARN);
        setLogger(loggerContext, "org.apache", Level.INFO);
        setLogger(loggerContext, "net.sf.ehcache", Level.INFO);
        setLogger(loggerContext, "org.aspectj", Level.INFO);
        setLogger(loggerContext, "com.zaxxer.hikari", Level.INFO);
        setLogger(loggerContext, "org.hibernate", Level.INFO);
        setLogger(loggerContext, "javax.management", Level.WARN);
        setLogger(loggerContext, "jdk.event.security", Level.INFO);
    }

    private void configureProfileLoggers(LoggerContext loggerContext, boolean isLocal) {
        setLogger(loggerContext, "org.springframework.security", isLocal ? Level.DEBUG : Level.INFO);
        setLogger(loggerContext, "org.springframework.cache", isLocal ? Level.INFO : Level.WARN);
        setLogger(loggerContext, "io.jgitkins", isLocal ? Level.DEBUG : Level.INFO);
    }

    private AsyncAppender createAsyncConsoleAppender(LoggerContext loggerContext) {
        PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
        consoleEncoder.setContext(loggerContext);
        consoleEncoder.setPattern(CONSOLE_PATTERN);
        consoleEncoder.start();

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setName("CONSOLE");
        consoleAppender.setEncoder(consoleEncoder);
        consoleAppender.start();

        AsyncAppender asyncConsoleAppender = new AsyncAppender();
        asyncConsoleAppender.setContext(loggerContext);
        asyncConsoleAppender.setName("ASYNC_CONSOLE");
        asyncConsoleAppender.addAppender(consoleAppender);
        asyncConsoleAppender.setQueueSize(512);
        asyncConsoleAppender.start();
        return asyncConsoleAppender;
    }

    private AsyncAppender createAsyncFileAppender(LoggerContext loggerContext, ConfigurableEnvironment environment) {
        String logPath = environment.getProperty("jgitkins.logging.path", DEFAULT_LOG_PATH);

        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("FILE");

        TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
        rollingPolicy.setContext(loggerContext);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(logPath + "/notificator-%d{yyyy-MM-dd}.%i.log");

        SizeAndTimeBasedFNATP<ILoggingEvent> fnatp = new SizeAndTimeBasedFNATP<>();
        fnatp.setContext(loggerContext);
        fnatp.setMaxFileSize(ch.qos.logback.core.util.FileSize.valueOf("100MB"));

        rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(fnatp);
        rollingPolicy.start();

        PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
        fileEncoder.setContext(loggerContext);
        fileEncoder.setPattern(FILE_PATTERN);
        fileEncoder.start();

        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.setEncoder(fileEncoder);
        fileAppender.start();

        AsyncAppender asyncFileAppender = new AsyncAppender();
        asyncFileAppender.setContext(loggerContext);
        asyncFileAppender.setName("ASYNC_FILE");
        asyncFileAppender.addAppender(fileAppender);
        asyncFileAppender.setQueueSize(512);
        asyncFileAppender.start();
        return asyncFileAppender;
    }

    private void setLogger(LoggerContext context, String name, Level level) {
        context.getLogger(name).setLevel(level);
    }
}
