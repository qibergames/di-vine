package com.qibergames.divine;

import com.qibergames.divine.exception.CircularDependencyException;
import com.qibergames.divine.exception.InvalidServiceAccessException;
import com.qibergames.divine.exception.InvalidServiceException;
import com.qibergames.divine.provider.AnnotationProvider;
import com.qibergames.divine.provider.InjectionHandle;
import com.qibergames.divine.provider.Ref;
import com.qibergames.divine.runtime.lifecycle.AfterInitialized;
import com.qibergames.divine.tree.ContainerInstance;
import com.qibergames.divine.tree.ContainerRegistry;
import com.qibergames.divine.exception.UnknownDependencyException;
import com.qibergames.divine.descriptor.factory.Factory;
import com.qibergames.divine.descriptor.generic.Inject;
import com.qibergames.divine.descriptor.generic.Service;
import com.qibergames.divine.descriptor.generic.ServiceScope;
import com.qibergames.divine.descriptor.property.NoProperties;
import com.qibergames.divine.descriptor.property.PropertyProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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

        ContainerRegistry foo = Container.ofGlobal("foo");
        ContainerRegistry bar = Container.ofGlobal("bar");

        foo.get(MyService.class).set = true;
        assertTrue(bar.get(MyService.class).set);
    }

    @Test
    void test_container_only_Container_from_multiple_context() {
        @Service(scope = ServiceScope.CONTAINER)
        class MyService {
            private boolean set;
        }

        ContainerRegistry foo = Container.ofGlobal("foo");
        ContainerRegistry bar = Container.ofGlobal("bar");

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
        assertThrows(InvalidServiceException.class, () -> Container.get(MyService.class));
        assertThrows(InvalidServiceException.class, () -> Container.get(MyEnum.class));
        assertThrows(InvalidServiceException.class, () -> Container.get(MyAnnotation.class));
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
        public @NotNull String provide(
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

    @Test
    public void test_unknown_token_retrieval() {
        assertThrows(UnknownDependencyException.class, () -> Container.get("UNKNOWN_TOKEN"));
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

    @RequiredArgsConstructor
    static class MyCustomServiceImpl implements MyCustomService {
        private final int val;

        @Override
        public int get() {
            return val;
        }
    }

    static class MyAnnotationProvider implements AnnotationProvider<MyCustomServiceImpl, MyCustomAnnotation> {
        @Override
        public @NotNull MyCustomServiceImpl provide(
            @NotNull Class<?> target, @NotNull MyCustomAnnotation annotation, @NotNull ContainerInstance container,
            @NotNull InjectionHandle handle
        ) {
            return new MyCustomServiceImpl(annotation.val());
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface MyCustomAnnotation {
        int val();
    }

    @Service
    static class MyProvidedService {
        @MyCustomAnnotation(val = 123)
        public MyCustomService service;
    }

    @Test
    public void test_custom_annotation_injection() {
        Container.addProvider(MyCustomAnnotation.class, new MyAnnotationProvider());
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

    @Service
    static class MyParamService {
        private final String value;

        public MyParamService(@Inject(token = "MY_VALUE") String value) {
            this.value = value;
        }
    }

    @Test
    public void test_constructor_token_injection() {
        Container.set("MY_VALUE", "Hello, World");

        MyParamService service = Container.get(MyParamService.class);
        assertEquals("Hello, World", service.value);
    }

    @Service(permits = MyPermittedService.class)
    interface MyPermittingService {
        int test();
    }

    @Service
    static class MyPermittedService implements MyPermittingService {
        @Override
        public int test() {
            return 418;
        }
    }

    @Test
    public void test_service_implementation_permit() {
        MyPermittingService service = Container.implement(MyPermittingService.class, MyPermittedService.class);
        assertEquals(418, service.test());
    }

    @Service
    static class MyNonPermittedService implements MyPermittingService {
        @Override
        public int test() {
            return 1234;
        }
    }

    @Test
    public void test_non_permitted_implementation() {
        assertThrows(InvalidServiceAccessException.class, () -> Container.implement(
            MyPermittingService.class, MyNonPermittedService.class
        ));
    }

    @Service
    static class ServiceA {
        @Inject
        ServiceB serviceB;
    }

    @Service
    static class ServiceB {
        @Inject
        ServiceA serviceA;
    }

    @Test
    public void test_direct_circular_dependency() {
        assertThrows(CircularDependencyException.class, () -> Container.get(ServiceA.class));
    }

    @Service
    static class ServiceFoo {
        @Inject
        Ref<ServiceBar> serviceBar;

        int foo() {
            return serviceBar.get().bar() + 5;
        }

        int init() {
            return 2;
        }
    }

    @Service
    static class ServiceBar {
        @Inject
        Ref<ServiceFoo> serviceFoo;

        int bar() {
            return serviceFoo.get().init() + 10;
        }
    }

    @Test
    public void test_referenced_circular_dependency() {
        ServiceFoo service = Container.get(ServiceFoo.class);
        assertEquals(2 + 10 + 5, service.foo());
    }

    @Test
    public void test_referenced_token() {
        Container.set("MY_VALUE", "Hello, World");

        @Service
        class MyService {
            @Inject(token = "MY_VALUE")
            public Ref<String> value;
        }

        MyService service = Container.get(MyService.class);
        assertEquals("Hello, World", service.value.get());
    }

    @Service
    static class LazyServiceA {
        @Inject(lazy = true)
        LazyServiceB serviceB;

        public int a() {
            return serviceB.b() + 3;
        }

        public int init() {
            return 1;
        }
    }

    @Service
    static class LazyServiceB {
        @Inject(lazy = true)
        LazyServiceA serviceA;

        public int b() {
            return serviceA.init() + 2;
        }
    }

    @Test
    public void test_lazy_referenced_circular_dependency() {
        LazyServiceA service = Container.get(LazyServiceA.class);
        assertEquals(1 + 2 + 3, service.a());
    }

    @Test
    public void test_container_token_resolve() {
        Container.set("SECRET", "1337");
        int value = Container.<String, Integer>resolve("SECRET", Integer::parseInt);
        assertEquals(1337, value);
    }

    @Test
    public void test_lazy_service_init() {
        @Service
        class OtherService {
            int get() {
                return 123;
            }
        }

        @Service
        class MyService {
            @Inject(lazy = true)
            OtherService otherService;

            int val;

            @AfterInitialized
            public void init() {
                assertNull(otherService);
            }

            @AfterInitialized(lazy = true)
            public void lazyInit() {
                assertNotNull(otherService);
                val = otherService.get();
            }
        }

        MyService service = Container.get(MyService.class);
        assertEquals(123, service.val);
    }

    @Test
    public void test_dependency_hook() {
        ContainerRegistry container = Container.ofGlobal("myContainer");

        @Service(scope = ServiceScope.TRANSIENT)
        @Getter
        @Setter
        class MyService {
            private int value = 123;
        }

        container.addHook("MY_HOOK", (dependency, descriptor) -> {
            if (dependency instanceof MyService)
                ((MyService) dependency).setValue(321);
            return dependency;
        });

        MyService service = container.get(MyService.class);
        assertEquals(321, service.getValue());

        container.removeHook("MY_HOOK");
        service = container.get(MyService.class);
        assertEquals(123, service.getValue());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface InspectorTarget {}

    @Service
    static class InspectedService {
        public int value = 100;

        @InspectorTarget
        public void foo(int newValue) {
            value = newValue;
        }
    }

    @Test
    public void test_method_inspector() {
        Container.addInspector(
            InspectorTarget.class,
            (method, annotation, container) -> method.invoke(200)
        );
        InspectedService service = Container.get(InspectedService.class);
        assertEquals(200, service.value);
    }

    @Test
    public void test_inject_into() {
        @Service
        class MyService {
            public final int value = 100;
        }

        @Service
        class MyExternalService {
            @Inject
            private MyService myService;

            public MyExternalService() {
                Container.injectInto(this);
            }
        }

        MyExternalService service = new MyExternalService();
        assertNotNull(service.myService);
        assertEquals(100, service.myService.value);
    }
}
