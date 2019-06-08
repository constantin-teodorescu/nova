/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.spring;


import ch.squaredesk.nova.Nova;
import io.reactivex.BackpressureStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class NovaProvidingConfiguration {
    public interface BeanIdentifiers {
        String NOVA = "NOVA.INSTANCE";
        String IDENTIFIER = "NOVA.ID";
        String DEFAULT_BACKPRESSURE_STRATEGY = "NOVA.EVENTS.DEFAULT_BACKPRESSURE_STRATEGY";
        String WARN_ON_UNHANDLED_EVENTS = "NOVA.EVENTS.WARN_ON_UNHANDLED";
        String CAPTURE_VM_METRICS = "NOVA.METRICS.CAPTURE_VM_METRICS";
    }

    @Bean(BeanIdentifiers.NOVA)
    public Nova nova(@Qualifier(BeanIdentifiers.IDENTIFIER) String identifier,
                     @Qualifier(BeanIdentifiers.DEFAULT_BACKPRESSURE_STRATEGY) BackpressureStrategy defaultBackpressureStrategy,
                     @Qualifier(BeanIdentifiers.WARN_ON_UNHANDLED_EVENTS) boolean warnOnUnhandledEvent,
                     @Qualifier(BeanIdentifiers.CAPTURE_VM_METRICS) boolean captureJvmMetrics) {
        return Nova.builder()
                .setIdentifier(identifier)
                .setDefaultBackpressureStrategy(defaultBackpressureStrategy)
                .setWarnOnUnhandledEvent(warnOnUnhandledEvent)
                .captureJvmMetrics(captureJvmMetrics)
                .build();
    }

    @Bean(BeanIdentifiers.IDENTIFIER)
    public String identifier(Environment environment) {
        return environment.getProperty(BeanIdentifiers.IDENTIFIER, "");
    }

    @Bean(BeanIdentifiers.WARN_ON_UNHANDLED_EVENTS)
    public Boolean warnOnUnhandledEvent(Environment environment) {
        return environment.getProperty(BeanIdentifiers.WARN_ON_UNHANDLED_EVENTS, Boolean.class, false);
    }

    @Bean(BeanIdentifiers.CAPTURE_VM_METRICS)
    public Boolean captureJvmMetrics(Environment environment) {
        return environment.getProperty(BeanIdentifiers.CAPTURE_VM_METRICS, Boolean.class, true);
    }

    @Bean(BeanIdentifiers.DEFAULT_BACKPRESSURE_STRATEGY)
    public BackpressureStrategy defaultBackpressureStrategy(Environment environment) {
        String strategyAsString = environment.getProperty(
                BeanIdentifiers.DEFAULT_BACKPRESSURE_STRATEGY,
                String.class,
                BackpressureStrategy.BUFFER.toString());
        return BackpressureStrategy.valueOf(strategyAsString.toUpperCase());
    }

}