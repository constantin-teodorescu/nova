/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.examples.metrics;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.events.EventEmitter;
import ch.squaredesk.nova.events.wrappers.NoParameterEventListener;
import org.apache.log4j.BasicConfigurator;

/**
 * This simple example shows how to use the Nove event queue.
 *
 * It creates a source JFrame and translates that Frame's MouseEvents to 2 target JFrames.
 *
 * The translation is done by the source frame putting all MouseEvents onto the Nove event queue, and register one Nova event queue listener
 * per target frame, which takes the MouseEvents and applies
 * them to the target frame.
 *
 */
public class MetricsExample {
	public static void main(String[] args) throws Exception {
		// init logging
		BasicConfigurator.configure();

		/**
		 * <pre>
		 * *********************************************************************** *
		 * *********************************************************************** *
		 * ***                                                                 *** *
		 * *** 1st step:                                                       *** *
		 * *** Initilize Nova by creating a new instance of Nova *** *
		 * ***                                                                 *** *
		 * *********************************************************************** *
		 * *********************************************************************** *
		 */
		final Nova nova = new Nova.Builder().build();

		/**
		 * <pre>
		 * ******************************************************************* *
		 * ******************************************************************* *
		 * ***                                                             *** *
		 * *** 2nd step:                                                   *** *
		 * *** Specify that metrics should regularily be dumped to logfile *** *
		 * ***                                                             *** *
		 * ******************************************************************* *
		 * ******************************************************************* *
		 */
		nova.metrics.dumpContinuouslyToLog(10, TimeUnit.SECONDS);

		/**
		 * <pre>
		 * *************************************************************************** *
		 * *************************************************************************** *
		 * ***                                                                     *** *
		 * *** 3rd step:                                                           *** *
		 * *** register dummy listeners and create events until <ENTER> is pressed *** *
		 * ***                                                                     *** *
		 * *************************************************************************** *
		 * *************************************************************************** *
		 */
		nova.eventEmitter.on("Event", (NoParameterEventListener) () -> {
		});
		nova.eventEmitter.on("Event2", (NoParameterEventListener) () -> {
		});
		startEventCreation(nova.eventEmitter);

		new BufferedReader(new InputStreamReader(System.in)).readLine();
	}

	private static void startEventCreation(final EventEmitter eventEmitter) {
		Thread t = new Thread() {
			@Override
			public void run() {
				Random rand = new Random();
				for (;;) {
					long sleepTimeMillis = (long) (rand.nextDouble() * 1000);
					try {
						sleep(sleepTimeMillis);
					} catch (InterruptedException e) {
					}
					eventEmitter.emit("Event");
					eventEmitter.emit("Event2");
					eventEmitter.emit("Unhandled");
					eventEmitter.emit("Unhandled2");
				}
			}
		};
		t.setDaemon(true);
		t.start();

	}

}
