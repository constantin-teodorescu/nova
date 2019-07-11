/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.service;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.metrics.MetricsDump;
import ch.squaredesk.nova.service.annotation.LifecycleBeanProcessor;
import ch.squaredesk.nova.tuples.Pair;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class NovaService {
    private List<Pair<String, String>> additionalInfoForMetricsDump;

    private Lifeline lifeline = new Lifeline();

    private boolean started = false;

    protected final Logger logger;

    @Qualifier(NovaServiceConfiguration.BeanIdentifiers.REGISTER_SHUTDOWN_HOOK)
    boolean registerShutdownHook;

    @Autowired
    LifecycleBeanProcessor lifecycleBeanProcessor;
    @Autowired
    protected Nova nova;
    @Autowired
    @Qualifier(NovaServiceConfiguration.BeanIdentifiers.INSTANCE_IDENTIFIER)
    protected String instanceId;
    @Autowired(required = false)
    @Qualifier(NovaServiceConfiguration.BeanIdentifiers.NAME)
    protected String serviceName;


    protected NovaService() {
        this.logger = LoggerFactory.getLogger(getClass());
    }

    @PostConstruct
    public void initServiceName() {
        if (serviceName == null) {
            serviceName = getClass().getSimpleName();
            logger.info("The service name was not provided, so we derived it from the class name: {} ", serviceName);
        }

        Pair<String, String> hostName;
        Pair<String, String> hostAddress;
        try {
            InetAddress myInetAddress = InetAddress.getLocalHost();
            hostName = Pair.create("hostName", myInetAddress.getHostName());
            hostAddress = Pair.create("hostAddress", myInetAddress.getHostAddress());
        } catch (Exception ex) {
            logger.warn("Unable to determine my IP address. MetricDumps will be lacking this information.");
            hostName = Pair.create("hostName", "n/a");
            hostAddress = Pair.create("hostAddress", "n/a");
        }
        additionalInfoForMetricsDump = Arrays.asList(
            hostName,
            hostAddress,
            Pair.create("serviceName", serviceName),
            Pair.create("serviceInstanceId", instanceId),
            Pair.create("serviceInstanceName", serviceName + "." + instanceId)
        );
    }

    private void doInit() {
        Objects.requireNonNull(nova);

        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread(()->shutdown()));
        }
        lifecycleBeanProcessor.invokeInitHandlers();
    }

    public void start() {
        if (started) {
            throw new IllegalStateException("service " + serviceName + "/" + instanceId + " already started");
        }

        lifecycleBeanProcessor.invokeStartupHandlers();

        lifeline.start();
        started = true;
        logger.info("Service {}, instance {} up and running.", serviceName, instanceId);
    }

    public void shutdown() {
        if (started) {
            logger.info("Service {}, instance {} is shutting down...", serviceName, instanceId);
            try {
                lifecycleBeanProcessor.invokeShutdownHandlers();
            } catch (Exception e) {
                logger.warn("Error in shutdown procedure of instance " + instanceId,e);
            }

            lifeline.cut();
            lifeline = new Lifeline();
            started = false;

            logger.info("Shutdown procedure completed for service {}, instance {}.", serviceName, instanceId);
        }
    }

    public boolean isStarted() {
        return started;
    }

    public Observable<MetricsDump> dumpMetricsContinuously (long interval, TimeUnit timeUnit) {
        return nova.metrics.dumpContinuously(interval, timeUnit, additionalInfoForMetricsDump);
    }

    private class Lifeline extends Thread {
        private final CountDownLatch shutdownLatch = new CountDownLatch(1);
        private Lifeline() {
            super("Lifeline");
        }

        @Override
        public void run() {
            try {
                shutdownLatch.await();
            } catch (InterruptedException e) {
                // noop
            }
        }

        void cut() {
            shutdownLatch.countDown();
        }
    }

    public static <T extends NovaService> T createInstance(Class<T> serviceClass, Class<?> ...configurationClasses) {

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

        ctx.register(NovaServiceConfiguration.class);
        ctx.register(configurationClasses);
        ctx.refresh();

        T service = ctx.getBean(serviceClass);
        ((NovaService)service).doInit();
        return service;
    }
}
