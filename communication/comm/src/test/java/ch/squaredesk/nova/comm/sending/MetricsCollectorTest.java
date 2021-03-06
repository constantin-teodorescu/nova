/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.sending;

import ch.squaredesk.nova.metrics.Metrics;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetricsCollectorTest {
    private Metrics metrics = new Metrics();
    private MetricsCollector sut = new MetricsCollector("test", metrics);

    @Test
    void instanceCannotBeCreatedWithoutMetrics() {
        Throwable t = assertThrows(NullPointerException.class,
                () ->new MetricsCollector("test", null));
        assertThat(t.getMessage(), containsString("metrics"));
    }

    @Test
    void messageSentCanBeInvokedWithNull() {
        sut.messageSent(null);

        assertThat(metrics.getMeter("test", "messageSender", "sent", "total").getCount(), is(1L));
        assertThat(metrics.getMeter("test", "messageSender", "sent", "null").getCount(), is(1L));
    }

    @Test
    void messageSending() {
        sut.messageSent("destination1");

        assertThat(metrics.getMeter("test","messageSender", "sent", "total").getCount(), is(1L));
        assertThat(metrics.getMeter("test", "messageSender", "sent", "destination1").getCount(), is(1L));
    }

}