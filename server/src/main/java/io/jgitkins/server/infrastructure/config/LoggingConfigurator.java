package io.jgitkins.server.infrastructure.config;

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

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ConfigurableEnvironment environment = event.getEnvironment();

        String profile = environment.getProperty("spring.profiles.active", "local");
        boolean isLocal = "local".equalsIgnoreCase(profile);

        // 0. Base Configuration
        loggerContext.reset(); // Clear existing setup

        // Format: %d{yyyy-MM-dd HH:mm:ss} [%highlight(%level)] [%thread]
        // %cyan(%logger{36}) - %msg%n
        PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
        consoleEncoder.setContext(loggerContext);
        consoleEncoder.setPattern(
                "%d{yyyy-MM-dd HH:mm:ss} %highlight(%-5level) [%X{traceId:-}] [%thread] %cyan(%logger{36}) - %msg%n");
        consoleEncoder.start();

        // 1. Console Appender Setup
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setName("CONSOLE");
        consoleAppender.setEncoder(consoleEncoder);
        consoleAppender.start();

        // 2. Async Console Appender Setup
        AsyncAppender asyncConsoleAppender = new AsyncAppender();
        asyncConsoleAppender.setContext(loggerContext);
        asyncConsoleAppender.setName("ASYNC_CONSOLE");
        asyncConsoleAppender.addAppender(consoleAppender);
        asyncConsoleAppender.setQueueSize(512);
        asyncConsoleAppender.start();

        // Root Logger
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(isLocal ? Level.INFO : Level.WARN); // changed level from OFF
        rootLogger.addAppender(asyncConsoleAppender);

        // ==========================================
        // 3. Logger Level Configurations
        // ==========================================
        configureCommonLoggers(loggerContext);

        if (isLocal) {
            configureLocalLoggers(loggerContext);
        } else {
            configureNonLocalLoggers(loggerContext);

            // 4. File Appender Setup for Non-Local environments
            // 3. File Appender Setup for Non-Local environments
            String userHome = System.getProperty("user.home");
            String logPath = environment.getProperty("jgitkins.logging.path", userHome + "/notificator/logs");

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

            // Format (No Color): %d{yyyy-MM-dd HH:mm:ss} %-5level [%X{traceId:-}] [%thread]
            // %logger{36} - %msg%n
            PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
            fileEncoder.setContext(loggerContext);
            fileEncoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} %-5level [%X{traceId:-}] [%thread] %logger{36} - %msg%n");
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

            rootLogger.addAppender(asyncFileAppender);
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
        setLogger(loggerContext, "org.apache.ibatis", Level.INFO);
        setLogger(loggerContext, "org.aspectj", Level.INFO);
        setLogger(loggerContext, "org.mybatis.spring", Level.INFO);
        setLogger(loggerContext, "com.zaxxer.hikari", Level.INFO);
        setLogger(loggerContext, "org.hibernate", Level.INFO);
        setLogger(loggerContext, "javax.management", Level.WARN);
        setLogger(loggerContext, "jdk.event.security", Level.INFO);

        setLogger(loggerContext, "jdbc", Level.OFF);
        setLogger(loggerContext, "jdbc.sqlonly", Level.WARN);
        setLogger(loggerContext, "jdbc.resultsettable", Level.WARN);
        setLogger(loggerContext, "jdbc.sqltiming", Level.WARN);
        setLogger(loggerContext, "io.jgitkins.server.infrastructure.persistence.mapper", Level.WARN);

        setLogger(loggerContext, "org.springframework.cache", Level.TRACE);
        setLogger(loggerContext, "io.jgitkins", Level.DEBUG);
    }

    private void configureLocalLoggers(LoggerContext loggerContext) {
        setLogger(loggerContext, "logging.level.org.springframework.security", Level.DEBUG);
        // 로컬 환경에는 필요에 따라 여기에 추가 로거 설정을 기입할 수 있습니다.
    }

    private void configureNonLocalLoggers(LoggerContext loggerContext) {
        // (예시) Stg나 Prd 등에서는 io.jgitkins를 INFO로 격상하는 등의 설정을 여기에 추가합니다.
        // setLogger(loggerContext, "io.jgitkins", Level.INFO);
    }

    private void setLogger(LoggerContext context, String name, Level level) {
        context.getLogger(name).setLevel(level);
    }
}
