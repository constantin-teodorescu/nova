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

package ch.squaredesk.nova.events.consumers;


import io.reactivex.functions.Consumer;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static ch.squaredesk.nova.events.consumers.ParamHelper.elementAtIndex;

@FunctionalInterface
public interface TenParameterConsumer<
        P1,
        P2,
        P3,
        P4,
        P5,
        P6,
        P7,
        P8,
        P9,
        P10>  extends Consumer<Object[]> {

    void accept(P1 param1, P2 param2, P3 param3, P4 param4,
                P5 param5, P6 param6, P7 param7, P8 param8,
                P9 param9, P10 param10);

    @SuppressWarnings("unchecked")
    default void accept(Object... data) {
        try {
            accept(
                    (P1) elementAtIndex(0, data),
                    (P2) elementAtIndex(1, data),
                    (P3) elementAtIndex(2, data),
                    (P4) elementAtIndex(3, data),
                    (P5) elementAtIndex(4, data),
                    (P6) elementAtIndex(5, data),
                    (P7) elementAtIndex(6, data),
                    (P8) elementAtIndex(7, data),
                    (P9) elementAtIndex(8, data),
                    (P10) elementAtIndex(9, data)
            );
        } catch (Exception e) {
            LoggerFactory
                    .getLogger("ch.squaredesk.nova.event.consumers")
                    .error("Error, trying to consume event with parameters {}", Arrays.toString(data), e);
        }
    }
}
