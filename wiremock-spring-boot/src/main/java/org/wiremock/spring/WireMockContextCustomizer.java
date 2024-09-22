package org.wiremock.spring;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Attaches properties with urls pointing to {@link WireMockServer} instances to the Spring {@link
 * org.springframework.core.env.Environment}.
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
  public WireMockContextCustomizer(final List<ConfigureWireMock> configurations) {
    this.configuration = configurations;
  }

  /**
   * Creates an instance of {@link WireMockContextCustomizer}.
   *
   * @param configurations the configurations
   */
  public WireMockContextCustomizer(final ConfigureWireMock... configurations) {
    this(Arrays.asList(configurations));
  }

  @Override
  public void customizeContext(
      final ConfigurableApplicationContext context, final MergedContextConfiguration mergedConfig) {
    for (final ConfigureWireMock configureWiremock : this.configuration) {
      final WireMockServer wireMockServer =
          this.resolveOrCreateWireMockServer(context, configureWiremock);
      if (this.configuration.size() == 1) {
        WireMock.configureFor(wireMockServer.port());
      }
    }
  }

  private WireMockServer resolveOrCreateWireMockServer(
      final ConfigurableApplicationContext context, final ConfigureWireMock options) {
    final WireMockServer wireMockServer =
        Store.INSTANCE.findWireMockInstance(context, options.name());

    if (wireMockServer == null) {
      // create & start wiremock server
      final WireMockConfiguration serverOptions =
          options().port(options.port()).notifier(new Slf4jNotifier(true));
      if (options.stubLocationOnClasspath()) {
        serverOptions.usingFilesUnderClasspath(this.resolveStubLocation(options));
      } else {
        serverOptions.usingFilesUnderDirectory(options.stubLocation());
      }

      if (options.extensions().length > 0) {
        serverOptions.extensions(options.extensions());
      }

      applyCustomizers(options, serverOptions);

      LOGGER.info(
          "Configuring WireMockServer with name '{}' on port: {}",
          options.name(),
          serverOptions.portNumber());

      final WireMockServer newServer = new WireMockServer(serverOptions);
      newServer.start();

      LOGGER.info("Started WireMockServer with name '{}':{}", options.name(), newServer.baseUrl());

      // save server to store
      Store.INSTANCE.store(context, options.name(), newServer);

      // add shutdown hook
      context.addApplicationListener(
          event -> {
            if (event instanceof ContextClosedEvent) {
              LOGGER.info("Stopping WireMockServer with name '{}'", options.name());
              newServer.stop();
            }
          });

      Arrays.stream(options.baseUrlProperties())
          .filter(StringUtils::isNotBlank)
          .collect(Collectors.toList())
          .forEach(
              propertyName -> {
                final String property = propertyName + "=" + newServer.baseUrl();
                LOGGER.debug("Adding property '{}' to Spring application context", property);
                TestPropertyValues.of(property).applyTo(context.getEnvironment());
              });

      Arrays.stream(options.portProperties())
          .filter(StringUtils::isNotBlank)
          .collect(Collectors.toList())
          .forEach(
              propertyName -> {
                final String property = propertyName + "=" + newServer.port();
                LOGGER.debug("Adding property '{}' to Spring application context", property);
                TestPropertyValues.of(property).applyTo(context.getEnvironment());
              });

      return newServer;
    } else {
      LOGGER.info("WireMockServer with name '{}' is already configured", options.name());
    }

    return wireMockServer;
  }

  @SuppressFBWarnings
  private static void applyCustomizers(
      final ConfigureWireMock options, final WireMockConfiguration serverOptions) {
    for (final Class<? extends WireMockConfigurationCustomizer> customizer :
        options.configurationCustomizers()) {
      try {
        ReflectionUtils.newInstance(customizer).customize(serverOptions, options);
      } catch (final Exception e) {
        if (e instanceof NoSuchMethodException) { // NOPMD
          LOGGER.error("Customizer {} must have a no-arg constructor", customizer, e);
        }
        throw e;
      }
    }
  }

  private String resolveStubLocation(final ConfigureWireMock options) {
    return StringUtils.isBlank(options.stubLocation())
        ? "wiremock/" + options.name()
        : options.stubLocation();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }
    final WireMockContextCustomizer that = (WireMockContextCustomizer) o;
    return Objects.equals(this.configuration, that.configuration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.configuration);
  }

  // ported from:
  // https://github.com/spring-cloud/spring-cloud-contract/commit/44c634d0e9e82515d2fba66343530eb7d2ba8223
  static class Slf4jNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger("WireMock");

    private final boolean verbose;

    Slf4jNotifier(final boolean verbose) {
      this.verbose = verbose;
    }

    @Override
    public void info(final String message) {
      if (this.verbose) {
        log.info(message);
      }
    }

    @Override
    public void error(final String message) {
      log.error(message);
    }

    @Override
    public void error(final String message, final Throwable t) {
      log.error(message, t);
    }
  }
}