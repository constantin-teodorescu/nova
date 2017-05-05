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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

public class NovaServiceTest {

    @Test
    void serviceCannotBeStartedWithoutConfig() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.refresh();

        assertThrows(
                NoSuchBeanDefinitionException.class,
                () -> ctx.getBean(MyService.class));
    }

    @Test
    void serviceCannotBeCreatedWithConfigThatDoesntReturnNovaInstance() {
        assertThrows(
                BeanCreationException.class,
                () -> MyService.createInstance(MyService.class, MyCrippledConfig.class));
    }

    @Test
    void serviceCannotBeCreatedWithConfigThatIsntAnnotatedWithConfiguration() {
        Throwable t = assertThrows(IllegalArgumentException.class,
                () -> MyService.createInstance(MyService.class, MyConfigWithoutConfigurationAnnotation.class));
        assertThat(t.getMessage(), containsString("must be annotated with @Configuration"));
    }

    @Test
    void startedServiceCannotBeRestartedWithoutShutdown() {
        MyService sut = MyService.createInstance(MyService.class, MyConfig.class);
        sut.start();
        assertTrue(sut.isStarted());

        assertThrows(IllegalStateException.class, sut::start);
    }

    @Test
    void notStartedServiceCanBeShutdown() {
        MyService sut = MyService.createInstance(MyService.class, MyConfig.class);

        assertFalse(sut.isStarted());
        sut.shutdown();
        assertFalse(sut.isStarted());
        assertThat(sut.onStartInvocations,is(0));
        assertThat(sut.onShutdownInvocations,is(1));
    }

    @Test
    void exceptionInOnStartPreventsServiceStart() {
        MyBrokenStartService sut = MyBrokenStartService
                .createInstance(MyBrokenStartService.class, MyConfigForBrokenStartService.class);
        Throwable t = assertThrows(RuntimeException.class, sut::start);
        assertThat(t.getMessage(), containsString("unable to start"));
    }

    @Test
    void exceptionInOnInitPreventsServiceCreation() {
        assertThrows(BeanCreationException.class,
                () -> MyBrokenInitService.createInstance(MyBrokenInitService.class, MyConfigForBrokenInitService.class));
    }

    @Test
    void earlyExceptionForServiceConfigurationThatIsNotProperlyAnnotated() {
        Throwable t = assertThrows(IllegalArgumentException.class,
                () -> MyService.createInstance(MyService.class, MyConfigWithoutBeanAnnotation.class));
        assertThat(t.getMessage(), containsString("must be annotated with @Bean"));
    }

    @Test
    void startedServiceCanBeRestartedAfterShutdown() {
        MyService sut = MyService.createInstance(MyService.class, MyConfig.class);

        assertFalse(sut.isStarted());

        sut.start();
        assertTrue(sut.isStarted());
        assertThat(sut.onStartInvocations,is(1));

        sut.shutdown();
        assertFalse(sut.isStarted());
        assertThat(sut.onStartInvocations,is(1));
        assertThat(sut.onShutdownInvocations,is(1));

        sut.start();
        assertTrue(sut.isStarted());
        assertThat(sut.onStartInvocations,is(2));
    }

    @Test
    void lifecycleCallbacksAreBeingInvoked() {
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext();

        ctx.register(MyConfig.class);
        ctx.refresh();

        MyService sut = ctx.getBean(MyService.class);

        assertFalse(sut.isStarted());
        assertThat(sut.onInitInvocations,is(1));
        assertThat(sut.onStartInvocations,is(0));
        assertThat(sut.onShutdownInvocations,is(0));

        sut.start();
        assertTrue(sut.isStarted());
        assertThat(sut.onInitInvocations,is(1));
        assertThat(sut.onStartInvocations,is(1));
        assertThat(sut.onShutdownInvocations,is(0));

        sut.shutdown();
        assertFalse(sut.isStarted());
        assertThat(sut.onStartInvocations,is(1));
        assertThat(sut.onShutdownInvocations,is(1));
        assertThat(sut.onShutdownInvocations,is(1));
    }

    @Component
    public static class MyBrokenInitService extends NovaService {
        @Override
        protected void onInit() {
            throw new RuntimeException("for test");
        }
    }

    @Component
    public static class MyBrokenStartService extends NovaService {
        @Override
        protected void onStart() {
            throw new RuntimeException("for test");
        }
    }

    @Component
    public static class MyService extends NovaService {
        private int onInitInvocations = 0;
        private int onStartInvocations = 0;
        private int onShutdownInvocations = 0;

        @Override
        protected void onInit() {
            onInitInvocations++;
        }

        @Override
        protected void onStart() {
            onStartInvocations++;
        }

        @Override
        protected void onShutdown() {
            onShutdownInvocations++;
        }
    }


    @Configuration
    public static class MyCrippledConfig extends NovaServiceConfiguration {
        @Override
        @Bean
        public Nova getNova() {
            return null;
        }

        @Bean
        public Object createServiceInstance() {
            return new MyService();
        }
    }

    @Configuration
    public static class MyConfig extends NovaServiceConfiguration<MyService> {
        @Bean
        public MyService createServiceInstance() {
            return new MyService();
        }
    }

    @Configuration
    public static class MyConfigForBrokenStartService extends NovaServiceConfiguration<MyBrokenStartService> {
        @Bean
        public MyBrokenStartService createServiceInstance() {
            return new MyBrokenStartService();
        }
    }

    @Configuration
    public static class MyConfigForBrokenInitService extends NovaServiceConfiguration<MyBrokenInitService> {
        @Bean
        public MyBrokenInitService createServiceInstance() {
            return new MyBrokenInitService();
        }
    }

    @Configuration
    public static class MyConfigWithoutBeanAnnotation extends NovaServiceConfiguration<MyService> {
        public MyService createServiceInstance() {
            return new MyService();
        }
    }

    public static class MyConfigWithoutConfigurationAnnotation extends NovaServiceConfiguration<MyService> {
        @Bean
        public MyService createServiceInstance() {
            return new MyService();
        }
    }
}