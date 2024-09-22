package org.wiremock.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Enables creating WireMock servers.
 *
 * @author Maciej Walkowiak
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(org.wiremock.spring.internal.WireMockSpringExtension.class)
public @interface EnableWireMock {
  /**
   * A list of {@link com.github.tomakehurst.wiremock.WireMockServer} configurations. For each
   * configuration a separate instance of {@link com.github.tomakehurst.wiremock.WireMockServer} is
   * created.
   *
   * @return an array of configurations
   */
  ConfigureWireMock[] value() default {};
}
