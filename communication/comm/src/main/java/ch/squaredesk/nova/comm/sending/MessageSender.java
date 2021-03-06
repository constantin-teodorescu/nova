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
import io.reactivex.Single;
import io.reactivex.functions.Function;

import static java.util.Objects.requireNonNull;


public abstract class MessageSender<
        DestinationType,
        TransportMessageType,
        MetaDataType extends OutgoingMessageMetaData<DestinationType, ?>> {

    protected final MetricsCollector metricsCollector;

    protected MessageSender(Metrics metrics) {
        this(null, metrics);
    }

    protected MessageSender(String identifier, Metrics metrics) {
        requireNonNull(metrics, "metrics must not be null");
        this.metricsCollector = new MetricsCollector(identifier, metrics);
    }

    public abstract Single<MetaDataType> send(TransportMessageType message, MetaDataType sendingInfo);

    public <T> Single<MetaDataType> send(T message, MetaDataType metaData, Function<T, TransportMessageType> transcriber) {
        TransportMessageType transportMessage = null;
        try {
            transportMessage = transcriber.apply(message);
        } catch (Exception e) {
            return Single.error(e);
        }
        return send(transportMessage, metaData);
    }

}

