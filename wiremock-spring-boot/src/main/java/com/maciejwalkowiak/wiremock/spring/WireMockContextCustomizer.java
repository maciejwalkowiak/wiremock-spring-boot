package com.maciejwalkowiak.wiremock.spring;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Attaches properties with urls pointing to {@link WireMockServer} instances to the Spring {@link Environment}.
 *
 * @author Maciej Walkowiak
 */
public class WireMockContextCustomizer implements ContextCustomizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockContextCustomizer.class);

    private final List<ConfigureWireMock> configuration;

    /**
     * Creates an instance of {@link WireMockContextCustomizer}.
     *
     * @param configurations the configurations
     */
    public WireMockContextCustomizer(List<ConfigureWireMock> configurations) {
        this.configuration = configurations;
    }

    /**
     * Creates an instance of {@link WireMockContextCustomizer}.
     *
     * @param configurations the configurations
     */
    public WireMockContextCustomizer(ConfigureWireMock[] configurations) {
        this(Arrays.asList(configurations));
    }

    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
        for (ConfigureWireMock configureWiremock : configuration) {
            resolveOrCreateWireMockServer(context, configureWiremock);
        }
    }

    private void resolveOrCreateWireMockServer(ConfigurableApplicationContext context, ConfigureWireMock options) {
        WireMockServer wireMockServer = Store.INSTANCE.findWireMockInstance(context, options.name());

        if (wireMockServer == null) {
            // create & start wiremock server
            WireMockConfiguration serverOptions = options()
                    .usingFilesUnderClasspath(resolveStubLocation(options))
                    .port(options.port())
                    .notifier(new Slf4jNotifier(true));

            if (options.extensions().length > 0) {
                serverOptions.extensions(options.extensions());
            }

            applyCustomizers(options, serverOptions);

            LOGGER.info("Configuring WireMockServer with name '{}' on port: {}", options.name(), serverOptions.portNumber());

            WireMockServer newServer = new WireMockServer(serverOptions);
            newServer.start();

            LOGGER.info("Started WireMockServer with name '{}':{}", options.name(), newServer.baseUrl());

            // save server to store
            Store.INSTANCE.store(context, options.name(), newServer);

            // add shutdown hook
            context.addApplicationListener(event -> {
                if (event instanceof ContextClosedEvent) {
                    LOGGER.info("Stopping WireMockServer with name '{}'", options.name());
                    newServer.stop();
                }
            });

            // configure Spring environment property
            Arrays.stream(options.property()).forEach(propertyName -> {
                String property =  propertyName + "=" + newServer.baseUrl();
                LOGGER.debug("Adding property '{}' to Spring application context", property);
                TestPropertyValues.of(property).applyTo(context.getEnvironment());
            });
        } else {
            LOGGER.info("WireMockServer with name '{}' is already configured", options.name());
        }
    }

    private static void applyCustomizers(ConfigureWireMock options, WireMockConfiguration serverOptions) {
        for (Class<? extends WireMockConfigurationCustomizer> customizer : options.configurationCustomizers()) {
            try {
                ReflectionUtils.newInstance(customizer).customize(serverOptions, options);
            } catch (Exception e) {
                if (e instanceof NoSuchMethodException) {
                    LOGGER.error("Customizer {} must have a no-arg constructor", customizer, e);
                }
                throw e;
            }
        }
    }

    private String resolveStubLocation(ConfigureWireMock options) {
        return StringUtils.isBlank(options.stubLocation()) ? "wiremock/" + options.name() : options.stubLocation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        WireMockContextCustomizer that = (WireMockContextCustomizer) o;
        return Objects.equals(configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configuration);
    }

    // ported from: https://github.com/spring-cloud/spring-cloud-contract/commit/44c634d0e9e82515d2fba66343530eb7d2ba8223
    static class Slf4jNotifier implements Notifier {

        private static final Logger log = LoggerFactory.getLogger("WireMock");

        private final boolean verbose;

        Slf4jNotifier(boolean verbose) {
            this.verbose = verbose;
        }

        @Override
        public void info(String message) {
            if (verbose) {
                log.info(message);
            }
        }

        @Override
        public void error(String message) {
            log.error(message);
        }

        @Override
        public void error(String message, Throwable t) {
            log.error(message, t);
        }

    }
}
