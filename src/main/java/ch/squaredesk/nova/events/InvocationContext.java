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

import java.util.Arrays;

class InvocationContext {
	public final Object event;
	public final Object[] data;

	InvocationContext(Object event, Object[] data) {
		this.event = event;
		this.data = data;
	}


	@Override
	public String toString() {
		return "InvocationContext [event=" + event
				+ ", data=" + Arrays.toString(data)
				+ "]";
	}

}
