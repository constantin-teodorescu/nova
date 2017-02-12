/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events;

import ch.squaredesk.nova.events.metrics.EventMetricsCollector;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

public class EventBus {
    private final Logger logger = LoggerFactory.getLogger(EventBus.class);

    private final EventBusConfig eventBusConfig;

    // metrics
    private final EventMetricsCollector metricsCollector;

    // the event specific subjects
    private final ConcurrentHashMap<Object,Subject<Object[]>> eventSpecificSubjects;

    public EventBus(String identifier, EventBusConfig eventBusConfig, Metrics metrics) {
        this.eventBusConfig = eventBusConfig;
        this.metricsCollector = new EventMetricsCollector(metrics, identifier);

        if (logger.isDebugEnabled()) {
            logger.debug("Instantiating event loop " + identifier);
            logger.debug("\tdefaultBackpressureStrategy:   " + eventBusConfig.defaultBackpressureStrategy);
            logger.debug("\twarn on unhandled events:      " + eventBusConfig.warnOnUnhandledEvent);
        }

        eventSpecificSubjects = new ConcurrentHashMap<>();
    }

    private Subject<Object[]> getSubjectFor (Object event) {
        return eventSpecificSubjects.computeIfAbsent(event, key -> {
            metricsCollector.eventSubjectAdded(event);
            Subject<Object[]> eventSpecificSubject = PublishSubject
                    .create();
            return eventSpecificSubject.toSerialized();
            // FIXME: remove if last subscriber goes
        });
    }

    public void emit (Object event, Object... data) {
        requireNonNull(event, "event must not be null");
        try {
            Subject<Object[]> subject = getSubjectFor(event);
            if (subject==null) {
                if (eventBusConfig.warnOnUnhandledEvent) {
                    metricsCollector.eventEmittedButNoObservers(event);
                }
            } else {
                subject.onNext(data);
                metricsCollector.eventDispatched(event);
            }
        } catch (Exception e) {
            logger.error("Unable to emit event " + event + " with parameters " + Arrays.toString(data),e);
        }
    }

    public Flowable<Object[]> on(Object event) {
        return on(event,
                eventBusConfig.defaultBackpressureStrategy);
    }


    public Flowable<Object[]> on(Object event, BackpressureStrategy backpressureStrategy) {
        requireNonNull(event, "event must not be null");
        return getSubjectFor(event).toFlowable(backpressureStrategy);
    }

}