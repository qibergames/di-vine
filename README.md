# The DiVine Dependency Injection Tool
DiVine is an advanced [dependency injection](https://en.wikipedia.org/wiki/Dependency_injection) tool for Java, 
that is inspired by the design of the TypeScript [typedi](https://github.com/typestack/typedi) library.
<p>
It is designed to be simple to use, and to provide a powerful and flexible way to manage dependencies in your Java applications.

## Automate initializations
One of the main purposes behind a dependency injection tool, is to minimize the need of manual code initialization.
Rather than spending time on developing the business logic of applications, you have to do a lot of work of making,
passing and deleting instances throughout your entire codebase.
<p>
This is where DiVine comes into place. It minimizes the code for service registration and dependency requests, so you
can have your focus kept on implementing actual logic.

## Basic usage
Note that, DiVine requires services to have a `@Service` annotation, called the service descriptor. This tells the
dependency injector, how the annotated service behaves.
<p>
The following code shows an example of a simple service, that is field-injected in another service.

```java
@Service
class Logger {
    public void log(String message) {
        System.out.println("LOG: " + message);
    }
}

@Service
class Application {
    @Inject
    private Logger logger;
    
    public void logic() {
        logger.log("Starting application");
    } 
}
```

You can also store shared values in the container.

```java
void initConfig() {
    // store shared values in the container
    Container.set("SECRET_KEY", "abc123");
}

@Service
class ApiController {
    // inject shared value into the parent service
    @Inject(token = "SECRET_KEY")
    private String secretKey;
    
    public void authenticate() {
        // you can also manually request values from the container
        requestToken(secretKey, Container.get("OTHER_VALUE"));
    }
}
```

## Improve testing

Using a dependency injector can improve the way that you can test your applications. By using interfaces as services,
you can easily use a mock implementation.
<p>
You can register mock implementations in your test files, therefore you do not need to modify anything in your
application code for testing.

```java
@Service(implementation = MongoDBUserController.class)
interface UserController {
    User getUser(String userId);
}

@Service
class MongoDBUserController implements UserController {
    @Override
    public User getUser(String userId) {
        return userCollection.find(new Document("userId", userId)).first();
    }
}
```

Inside your application, you can request the `UserController` dependency as such:

```java
void myUserHandler() {
    UserController controller = Container.get(UserController.class);
    User user = controller.getUser("test");
    System.out.println("Welcome user, " + user.getName());
}
```

Inside your test files you could do the following code:

```java
@Service
class UserControllerMock implements UserController {
    @Override
    public User getUser(String userId) {
        return createUserMock(userId);
    }
}

@BeforeAll
public void mockUserController() {
    Container.implement(UserController.class, UserControllerMock.class);
}

@Test
public void test_UserController_getUser() {
    UserController controller = Container.implement(UserController.class);
    User user = controller.getUser("test");
    asserEquals("ExampleUser", user.getName());
}
```

## Advanced usage

DiVine features a vast range of features that ensure that you have the best experience while using the dependency injector.
The following codes showcase some of the most important utilities, that you can use to improve your code.

### Service scopes

DiVine uses a hierarchy of containers, called the `container tree`, that includes the `root container` and `sub-containers`.
You may use sub-containers, when separate parts of your code needs separate dependencies.
<p>
By default, service scopes are `CONTAINER`, which means that a unique instance is associated for each container, that requests the dependency.
If you want a service to be unique in the whole container tree, consider using `@Service(scope=SINGLETON)` to indicate,
that no matter what container is the code requesting the dependency from, the same instance should be retrieved.
<p>
You can use the following methods to request various containers depending on the call context.

```java
void testContainers() {
    Container.ofGlobal(); // will return the root container if the container tree
    
    Container.ofGlobal("my-container"); // will return a sub-container called `my-container`, 
    // that is a child of the root container
    
    Container.ofContext(); // will return a unique container for the call context, that is called 
    // `container-%container_id_increment%`
    
    Container.ofContext("other-container"); // will return a unique sub-container of `Container.ofContext()`, 
    // which is called `other-container`
}
```

If you want to learn more about container contexts, check out the `Container contexts` part in `Advanced usage`.

The following code is an example, in which container-scoped services come in handy.

```java
@Service
class PlayerManager {
    public List<Player> getPlayers() { /* player list logic*/ }
}

@Service
class MinigameArena { 
    @Inject
    private PlayerManager playerManager;
    
    public void start() {
        for (Player player : playerManager.getPlayers()) {
            player.message("Arena is starting...");
        }
    }
}

void manageArenas() {
    // the following code will resolve the dependency tree for two separate minigame arenas,
    // where each arena will request their own instance of dependencies
    MinigameArena foo = fooArenaContainer.get(MinigameArena.class);
    MinigameArena bar = barArenaContainer.get(MinigameArena.class);
    assert foo != bar;
}
```

### Service factories

When requesting dependencies, the implementations may differ for various contexts. The following code showcases a simple
way of requesting different implementations for a service.

```java
import java.awt.*;

@Service(
    // use the `CarFactory` class to create new instances for the `Car` type
    factory = CarFactory.class,
    // use `TRANSIENT` scope, to create a new car instance, each time a car is requested
    scope = ServiceScope.TRANSIENT 
)
interface Car {
    void drive();
}

enum CarType {
    MERCEDES,
    BMW,
    FERRARI
}

class CarFactory implements Factory<Car, CarType> {
    @Override
    public @NotNull Car create(
        @NotNull Service descriptor, @NotNull Class<? extends Car> type, 
        @NotNull Class<?> context, @Nullable CarType carType
    ) {
        return switch (carType) {
            case MERCEDES -> new MercedesCar();
            case BMW -> new BMWCar();
            case FERRARI -> new FerrariCar();
        };
    }
}

@Service
class CarDealership() {
    public Car orderCar(CarType type) {
        return Container.get(Car.class, type);
    }
}

void orderCars() {
    CarDealership dealership = Container.get(CarDealership.class);
    assert dealership.orderCar(CarType.MERCEDES) instanceof MercedesCar;
    assert dealership.orderCar(CarType.BMW) instanceof BMWCar;
    assert dealership.orderCar(CarType.TERRARY) instanceof FerraryCar;
}
```

### Service implementations
In case, you want to use a single implementation of your service interface, throughout your entire application,
you can use the following code.

```java
@Service(implementation = RedisSessionManager.class)
interface SessionManager {
    Session createSession();
}

@Service
class RedisSessionManager implements SessionManager {
    @Override
    public Session createSession() {
        return createMySession();
    }
}

void handleAuthentication() {
    SessionManager sessionManager = Container.get(SessionManager.class);
    
    if (authorized)
        user.setSession(sessionManager.createSession());
}
``` 

You can also manually implement a service interface, using the following code.

```java
@Service
interface DatabaseConnector {
    void connect();
}

@Service
class MySQLConnector implements DatabaseConnector {
    @Override
    public void connect() {
        requestMySQLConnection();
    }
}

void initDatabase() {
    Container.implement(DatabaseConnector.class, MySQLConnector.class);
}

void useDatabase() {
    DatabaseConnector connector = Container.get(DatabaseConnector.class);
    assert connector instanceof MySQLConnector;
}
```

### Implementation permissions

You can create a rule for the service interface, that specifies, which classes may implement the interface.

```java
@Service(permits = { MongoUserService.class, MySQLUserService.class })
interface UserService {
}

@Service
class MongoUserService implements UserService {
}

@Service
class MySQLUserService implements UserService {
}

@Service
class PostgresUserService implements UserService {
}

void initUserService() {
    Container.implement(UserService.class, MongoUserService.class);
    Container.implement(UserService.class, MySQLUSerService.class);
    // both should work fine
    
    Container.implement(UserService.class, PostgresUserService.class); 
    // will throw an `InvalidServiceAccessException`
}
```

### Custom annotations

If you have shared dependencies, that you want to easily access throughout your entire applications, you might
want to consider using custom annotations, so you do not need to specify `@Inject(...long properties)` every time.

```java
@Service
class MyLogger {
    @Override
    public void log(String message) {
        System.out.println("LOG: " + message);
    }
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface Logger {
}

void initLogger() {
    Container.registerProvider(Logger.class, (target, container) -> new MyLogger());
}

@Service
class UserController {
    @Logger
    MyLogger logger;

    public void init() {
        logger.log("UserController has been initialized!");
    }
}
```

### Circular Dependencies
A circular dependency problem occurs, when two or more services depend on each other. By default, the dependency injector
cannot resolve this issue, because the resolving would end up in an infinite loop, as the dependencies would keep requesting each other.
<p>
For most cases, it is recommended to avoid services referencing to each other.
<p>
The following code will throw a `CircularDependencyException`.

```java
@Service
class ServiceA {
    @Inject
    ServiceB serviceB;
}

@Service
class ServiceB {
    @Inject
    ServiceA serviceA;
}

void init() {
    assertThrows(CircularDependencyException.class, () -> Container.get(ServiceA.class));
}
```

#### Using referenced dependency access
One workaround for circular dependency access, is to use `Ref`s. A `Ref<T>` features lazy access to a dependency.

```java
@Service
class ServiceA {
    Ref<ServiceB> serviceB;
    
    void foo() {
        serviceB.get().baz();
    }
    
    void bar() {
        System.out.println("Hello!");
    }
}

@Service
class ServiceB {
    Ref<ServiceA> serviceA;
    
    void baz() {
        serviceA.get().bar();
    }
}

void useCircularRefs() {
    Container.get(ServiceA.class).foo(); // will print `Hello!`
}
```

#### Using lazy dependency access
Fields annotated with`@Inject(lazy=true)` will be resolved after the whole dependency tree was resolved.
This way, when injecting these fields, each of the required dependencies are already resolved.
<p>
Note that, these fields will be `null` in the constructor. If you want to use these fields after initialization,
check out the `Service lifecycles` section in the `Advanced usage` category.

```java
@Service
class ServiceA {
    @Inject(lazy = true)
    ServiceB serviceB;
}

@Service
class ServiceB {
    @Inject(lazy = ture)
    ServiceA serviceA;
}
```

### Service lifecycles
DiVine features a set of events, that are called for a service during runtime, when it reaches a certain lifecycle.
<p>
Lifecycles make your code easier, as you don't have to define a public initialization or clean up method, and call
it from various parts of your application - which is often untraceable.
<p>
You can register listeners for the following lifecycles:

#### Initialization lifecycle
The `@AfterInitialized` method is called right after the service is instantiated, and each field is injected.
<p>
This feature is useful, when you have a bunch of dependencies of your service, and you don't want to use a constructor,
because the initialization would be too robust.

```java
@Service
class CloudController {
    @Inject
    private CloudConnection connection;
    
    @AfterInitialized
    private void init() {
        connection.sendHandshake();
    }
}
```

#### Lazy initialization lifecycle
The `@AfterInitialized(lazy = true)` method is called after the whole dependency tree is resolved, therefore here
you can already access each dependency of the service, that was lazily injected.

```java
@Service
class AuthService {
    @Inject(lazy = true)
    private SessionManager sessionManager;
    
    @AfterInitialized(lazy = true)
    private void init() {
        System.out.println("Restored " + sessionManager.getSessions().size() + " sessions.");
    }
}
```

#### Termination lifecycle
Your services may initialize components, that must be closed/terminated. You could add a public method to clean up these
resources, however you may forget to call these outside the service, before unregistering the service.

```java
@Service
class UserManager {
    @Inject
    private DatabaseConnection connection;

    @AfterInitialized 
    private void init() {
        connection.connect();
    }
    
    @BeforeTerminate
    private void shutdown() {
        connection.close();
    }
}

void handleTermination() {
    myContainer.unset(UserManager.class); // you may manually remove the dependency from the container
    
    myContainer.reset(); // you may manually reset the entire container
    
    // both of these cases would normally open up bugs here, if you don't call
    // explicitly a clean-up method
    // 
    // luckily, the dependency injector will call the termination method for your registered 
    // dependencies, as specified
}
```

### Dealing with multiple constructors
When you declare multiple constructors for your service, by default, the dependency injector cannot decide which one
to use to initialize the service with.
<p>

In order to fix this problem, annotate the desired constructor with the `@ConstructWith` annotation, to tell the
dependency injector, which constructor to use.

```java
@Service
class ImageProcessor {
    private final int quality;
    private final boolean resize;
    
    // by default, the dependency injector will use this method to 
    // instantiate the `ImageProcessor` class
    @ConstructWith
    public ImageProcessor(ImageOptions options) {
        quality = options.getQuality();
        resize = options.getResize();
    }
    
    // you may have various constructors, as you wish
    public ImageProcessor(int quality, boolean resize) {
        this.quality = quality;
        this.resize = resize;
    }
}
```
