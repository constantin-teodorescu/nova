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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class CurrentThreadEventEmitter extends EventEmitter {
	private static final Logger LOGGER = LoggerFactory.getLogger(CurrentThreadEventEmitter.class);

	public CurrentThreadEventEmitter(EventMetricsCollector eventMetricsCollector, boolean warnOnUnhandledEvents) {
		super(eventMetricsCollector, warnOnUnhandledEvents);
	}

	@Override
	void dispatchEventAndDataToListeners(List<EventListener> listenerList, Object event, Object... data) {
		Object[] dataToPass = data.length == 0 ? null : data;
		listenerList.forEach(listener -> {
			try {
				listener.handle(dataToPass);
				metricsCollector.eventDispatched(event);
			} catch (Exception e) {
				LOGGER.error("Uncaught exception while invoking eventListener " + listener, e);
				LOGGER.error("\tparamters: " + Arrays.toString(data));
			}
		});
	}

}
