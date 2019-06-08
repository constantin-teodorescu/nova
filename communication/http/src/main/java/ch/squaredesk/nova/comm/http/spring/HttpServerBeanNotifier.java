/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.http.spring;

import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Map;

public class HttpServerBeanNotifier implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(HttpServerBeanNotifier.class);

    private final boolean autoNotifyOnContextRefresh;

    public HttpServerBeanNotifier(boolean autoNotifyOnContextRefresh) {
        this.autoNotifyOnContextRefresh = autoNotifyOnContextRefresh;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!autoNotifyOnContextRefresh) {
            return;
        }

        Object httpServer = event.getApplicationContext().getBean(HttpServerProvidingConfiguration.BeanIdentifiers.SERVER);
        if (httpServer instanceof HttpServer) { // can also be a NullInstance
            notifyHttpServerAvailableInContext((HttpServer)httpServer, event.getApplicationContext());
        }
    }

    public static void notifyHttpServerAvailableInContext(HttpServer httpServer, ApplicationContext context) {
        Map<String, HttpServerBeanListener> beans = context.getBeansOfType(HttpServerBeanListener.class);
        if (beans != null && ! beans.isEmpty()) {
            beans.entrySet().forEach(entry -> {
                try {
                    entry.getValue().httpServerAvailableInContext(httpServer);
                } catch (Exception e) {
                    logger.error("An error occurred trying to notify bean {} about HttpServer instance", entry.getKey(), e);
                }
            });
        }
    }


}