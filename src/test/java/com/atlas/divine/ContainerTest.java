package com.atlas.divine;

import com.atlas.divine.provider.AnnotationProvider;
import com.atlas.divine.runtime.lifecycle.AfterInitialized;
import com.atlas.divine.tree.ContainerInstance;
import com.atlas.divine.tree.ContainerRegistry;
import com.atlas.divine.exception.UnknownDependencyException;
import com.atlas.divine.descriptor.factory.Factory;
import com.atlas.divine.descriptor.generic.Inject;
import com.atlas.divine.descriptor.generic.Service;
import com.atlas.divine.descriptor.generic.ServiceScope;
import com.atlas.divine.descriptor.property.NoProperties;
import com.atlas.divine.descriptor.property.PropertyProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContainerTest {
    @Test
    void test_singleton_Container_from_root_context() {
        @Service(scope = ServiceScope.SINGLETON)
        class MyService {
            private int value;

            public void increment() {
                value++;
            }

            public int get() {
                return value;
            }
        }

        for (int i = 1; i <= 3; i++) {
            MyService service = Container.get(MyService.class);
            service.increment();
            assertEquals(i, service.get());
        }
    }

    @Test
    void test_singleton_Container_from_multiple_context() {
        @Service(scope = ServiceScope.SINGLETON)
        class MyService {
            private boolean set;
        }

        ContainerRegistry foo = Container.of("foo");
        ContainerRegistry bar = Container.of("bar");

        foo.get(MyService.class).set = true;
        assertTrue(bar.get(MyService.class).set);
    }

    @Test
    void test_container_only_Container_from_multiple_context() {
        @Service(scope = ServiceScope.CONTAINER)
        class MyService {
            private boolean set;
        }

        ContainerRegistry foo = Container.of("foo");
        ContainerRegistry bar = Container.of("bar");

        foo.get(MyService.class).set = true;
        assertFalse(bar.get(MyService.class).set);
    }

    @Test
    void test_transient_Container() {
        @Service(scope = ServiceScope.TRANSIENT)
        class MyService {
            private int value;

            public void increment() {
                value++;
            }

            public int get() {
                return value;
            }
        }

        for (int i = 0; i < 3; i++) {
            MyService service = Container.get(MyService.class);
            service.increment();
            assertEquals(1, service.get());
        }
    }

    private static class FactoryWithoutProperties implements Factory<ServiceWithFactoryWithoutProperty, NoProperties> {
        @Override
        public @NotNull ServiceWithFactoryWithoutProperty create(
            @NotNull Service descriptor, @NotNull Class<? extends ServiceWithFactoryWithoutProperty> type,
            @NotNull Class<?> context, @Nullable NoProperties properties
        ) {
            return () -> 123;
        }
    }

    @Service(factory = FactoryWithoutProperties.class)
    interface ServiceWithFactoryWithoutProperty {
        int get();
    }

    @Test
    public void test_Container_initialization_without_Factory_properties() {
        ServiceWithFactoryWithoutProperty service = Container.get(ServiceWithFactoryWithoutProperty.class);
        assertEquals(123, service.get());
    }

    private static class FactoryWithProperties implements Factory<
        ServiceWithFactoryWithProperties, ServiceWithFactoryWithPropertiesProperties
    > {
        @Override
        public @NotNull ServiceWithFactoryWithProperties create(
            @NotNull Service descriptor, @NotNull Class<? extends ServiceWithFactoryWithProperties> type,
            @NotNull Class<?> context, @Nullable ServiceWithFactoryWithPropertiesProperties properties
        ) {
            if (properties == null)
                throw new IllegalArgumentException("Properties cannot be null");

            switch (properties) {
                case FIRST:
                    return () -> "First service implementation";
                case SECOND:
                    return () -> "Second service implementation";
                default:
                    throw new IllegalArgumentException("Unknown property: " + properties);
            }
        }
    }

    public enum ServiceWithFactoryWithPropertiesProperties {
        FIRST,
        SECOND
    }

    @Service(factory = FactoryWithProperties.class, scope = ServiceScope.TRANSIENT)
    interface ServiceWithFactoryWithProperties {
        String value();
    }

    @Test
    public void test_Container_initialization_with_Factory_properties() {
        ServiceWithFactoryWithProperties firstService = Container.get(
            ServiceWithFactoryWithProperties.class,
            ServiceWithFactoryWithPropertiesProperties.FIRST
        );
        assertEquals("First service implementation", firstService.value());

        ServiceWithFactoryWithProperties secondService = Container.get(
            ServiceWithFactoryWithProperties.class,
            ServiceWithFactoryWithPropertiesProperties.SECOND
        );
        assertEquals("Second service implementation", secondService.value());
    }

    @Service
    @interface MyAnnotation {
    }

    @Service
    interface MyService {
    }

    @Service
    enum MyEnum {
        FIRST,
        SECOND
    }

    @Test
    public void test_Container_error_on_interface_or_enum_without_Factory() {
        assertThrows(UnknownDependencyException.class, () -> Container.get(MyService.class));
        assertThrows(UnknownDependencyException.class, () -> Container.get(MyEnum.class));
        assertThrows(UnknownDependencyException.class, () -> Container.get(MyAnnotation.class));
    }

    static class DynamicServiceFactory implements Factory<MyDynamicService, String> {
        @Override
        public @NotNull MyDynamicService create(
            @NotNull Service descriptor, @NotNull Class<? extends MyDynamicService> type, @NotNull Class<?> context,
            @Nullable String properties
        ) {
            if (properties == null)
                throw new IllegalArgumentException("Properties cannot be null");

            switch (properties) {
                case "first":
                    return () -> "First implementation";
                case "second":
                    return () -> "Second implementation";
                default:
                    return () -> "Default implementation";
            }
        }
    }

    @Service(factory = DynamicServiceFactory.class, scope = ServiceScope.TRANSIENT)
    interface MyDynamicService {
        String get();
    }

    static class MyPropertyProvider implements PropertyProvider<MyDynamicServiceComponent, String> {
        @Override
        public String provide(
            @NotNull Service descriptor, @NotNull Class<MyDynamicServiceComponent> type, @NotNull Class<?> context
        ) {
            return "second";
        }
    }

    @Service
    static class MyDynamicServiceComponent {
        @Inject(properties = "first")
        public MyDynamicService firstService;

        @Inject(provider = MyPropertyProvider.class)
        public MyDynamicService secondService;
    }

    @Test
    public void test_dynamic_inject_dependency_with_properties() {
        MyDynamicServiceComponent component = Container.get(MyDynamicServiceComponent.class);
        assertEquals("First implementation", component.firstService.get());
        assertEquals("Second implementation", component.secondService.get());
    }

    @Test
    public void test_property_injection_with_token() {
        Container.set("MY_VALUE", "Hello, World");

        @Service
        class MyService {
            @Inject(token = "MY_VALUE")
            public String value;
        }

        MyService service = Container.get(MyService.class);
        assertEquals("Hello, World", service.value);
    }

    @Service(implementation = MyServiceImplementation.class)
    interface MyServiceWithImplementation {
        int get();
    }

    @Service
    static class MyServiceImplementation implements MyServiceWithImplementation {
        @Override
        public int get() {
            return 100;
        }
    }

    @Test
    public void test_service_implementation() {
        MyServiceWithImplementation service = Container.get(MyServiceWithImplementation.class);
        assertEquals(100, service.get());
    }

    @Test
    public void test_service_after_initialized() {
        @Service
        class MyService {
            private int value;

            @AfterInitialized
            public void init() {
                value = 1337;
            }
        }

        MyService service = Container.get(MyService.class);
        assertEquals(1337, service.value);
    }

    interface MyCustomService {
        int get();
    }

    static class MyCustomServiceImpl implements MyCustomService {
        @Override
        public int get() {
            return 123;
        }
    }

    static class MyProvider implements AnnotationProvider<MyCustomServiceImpl> {
        @Override
        public @NotNull MyCustomServiceImpl provide(@NotNull Object target, @NotNull ContainerInstance container) {
            return new MyCustomServiceImpl();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface MyCustomAnnotation {
    }

    @Service
    static class MyProvidedService {
        @MyCustomAnnotation
        public MyCustomService service;
    }

    @Test
    public void test_custom_annotation_injection() {
        Container.addProvider(MyCustomAnnotation.class, new MyProvider());
        MyProvidedService service = Container.get(MyProvidedService.class);
        assertEquals(123, service.service.get());
    }

    @Service
    interface MyMultipleService {
        int get();
    }

    @Service(id = "my-multiple-service", multiple = true)
    static class MyFirstMultipleService implements MyMultipleService {
        @Override
        public int get() {
            return 100;
        }
    }

    @Service(id = "my-multiple-service", multiple = true)
    static class MySecondMultipleService implements MyMultipleService {
        @Override
        public int get() {
            return 200;
        }
    }

    @Test
    public void test_multiple_services() {
        Container.insert(MyFirstMultipleService.class, MySecondMultipleService.class);
        List<MyMultipleService> services = Container.getMany("my-multiple-service");
        assertEquals(2, services.size());
        assertEquals(100, services.get(0).get());
        assertEquals(200, services.get(1).get());
    }

    // TODO add more cases, such as the implementation class does not implement the service interface,
    //  or the implementation class is not a @Service, etc
}
