Neo4j Websockets Client/Server Framework
========================================
This framework allows to connect clients with Neo4j servers through websockets. Furthermore it offers high availability and load balancing to the clients when using Neo4j in a cluster.

Spring Framework is used for dependency injection.

Jar files for the framework are available from Maven Central Repository:
[maven.org - neo4j-websockets](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22de.oliverwetterau.neo4j%22)

An example project showing the use of client and server part of the framework can be found here:
[github - neo4j-websockets-example](https://github.com/owetterau/neo4j-websockets-example)

Server
------
### Configuration and Startup
This framework is a plugin for the server side. Hence, when using the provided example pom for maven, a fat Jar will be created that can be copied into the `plugins` directory of a Neo4j (2.3) server (community or enterprise).

*An example-pom.xml that will create such a Jar can be found in the doc directory.*

To be able to pickup the functionality added by yourself, you must let the framework know which packages need to be scanned for Spring beans. To do this, simply add a line like the following to ```conf/neo4j.properties```

```bash
# replace com.example.project with your root package name
websocket_packages=com.example.project
```

There are three more settings in ```conf/neo4j.properties``` that you can add and change:

```bash
# hostname and port the websockets server shall listen to
# defaults to 0.0.0.0:8765 if not set
websocket_host=192.168.1.10:9999

# path for management connections
# defaults to /ws/management if not set
websocket_management_path=/websockets/management-connection

# path for data connections
# defaults to /ws/data if not set
websocket_data_path=/websockets/data-connection
```

### Websockets Message Handling

All incoming data message will be passed to controllers based on controller and method name. To make controllers and methods known to the framework, annotations are being used.

#### Annotations
There are two annotations: `@MessageController("name")` and `@MessageMethod`.

`@MessageController("abc")` defines a class to be the handler of all messages marked with the service name "abc".

`@MessageMethod` defines a method to deal with messages marked with the method's name. The signature of methods annotated with `@MessageMethod` must always be:

```java
public Result methodName(JsonNode jsonNode)
```

For example:

```java
@MessageController("system")
public class SystemController {
    private final SchemaService schemaService;

    @Autowired
    public SystemController(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @MessageMethod
    public Result<Boolean> initWithFakeData(JsonNode jsonNode) {
        try {
            schemaService.initWithFakeData();
        }
        catch (Exception e) {
            return new Result<>(e);
        }

        return new Result<>(true);
    }
}
```

All data to be used by these methods is being passed by a `JsonNode`.

#### Result, Error, JsonObjectMapper and ThreadLocale
These classes are tied closely together. `Result` is the expected format on the client side when receiving answers to a websocket message. It can contain any data (`Result` is generic) or errors. Hence, `Error` is the expected format of error messages on the client side.

The Jackson library is used for serialization and deserialization of the messages sent between client and server using binary json (SMILE). To allow for the addition of custom serializers, `JsonObjectMapper` has to be initialized with an instance of *JsonObjectSerializers* (or with null). *Result* and *Error* will use *JsonObjectMapper* to serialize themselves. Furthermore, all incoming messages on the server side will be deserialzed with *JsonObjectMapper*.

As each message coming from the client also contains a *Locale* defining the language that shall be used an instance of *ThreadLocale* is being used by *JsonObjectMapper* for serialization with the correct language settings. A class implementing *ThreadLocale* therefore must be created and made available as a *@Service*. Here is an example implementation:

```java
@Service
public class LanguageSettings implements ThreadLocale {
    protected static ThreadLocal<ISOLanguage> isoLanguageThreadLocal = new ThreadLocal<>();

    public static void setLanguage(final ISOLanguage isoLanguage) {
        isoLanguageThreadLocal.set(isoLanguage);
    }

    public static ISOLanguage getLanguage() {
        ISOLanguage isoLanguage = isoLanguageThreadLocal.get();
        return (isoLanguage == null) ? ISOLanguage.EN_US : isoLanguage;
    }

    @Override
    public void setLocale(final Locale locale) {
        setLanguage(ISOLanguage.getByLocale(locale));
    }

    @Override
    public Locale getLocale() {
        return getLanguage().getLocale();
    }
}
```

Client
------
### Configuration and Startup

#### Application Settings
You can configure the websocket connections to the server using the ApplicationSettings path.

##### Servers
The minimum information required to use the framework is the list of servers that the framework shall connect to. Setting this information is as easy as follows:

```java
String[] uris = new String[] { "ws://192.168.1.10:8765", "ws://192.168.1.11:8765", "ws://192.168.1.12:8765"};
ApplicationSettings.setServerURIs(uris);
```

##### Paths (optional)
The paths used on the server must be same for all servers as it is not possible to configure paths per server (yet).

Obviously, paths defined on the client must be in sync with paths defined on the servers. Setting them is easy:

```java
ApplicationSettings.setManagementPath("/websockets/management-connection");
ApplicationSettings.setManagementPath("/websockets/data-connection");
```

It is not necessary to set these paths. Default values "/ws/management" and "/ws/data" will be used, if you do not manually set them.

##### Protocol (optional)
As a default a binary protocol (Jackson Smile) will be used for the data connection between client and server. If you want to change that to a clear text communication, you can do it like this:

```java
ApplicationSettings.setBinaryCommunication(false);
```

#### Startup
Similar to the server, it is important add this framework's package to the list of packages being scanned by Spring to
activate it's functionality:

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.example", "de.oliverwetterau.neo4j.websockets"})
public class Application {
    public static void main(String[] arguments) throws Exception {
        ApplicationSettings.setServerURIs(System.getProperty("database.server.uris").split(","));
        SpringApplication.run(Application.class, arguments);
    }
}
```

That's all to connect the client to the servers...

Sending and Retrieving
----------------------
`DatabaseService` basically offers two different methods of communication with the servers: `getData` and `writeDataWithResult` whereas the first is being used for pure read access and the latter should be used for all messages that may lead to writes to Neo4j.

These method have four different signatures:

```java
Result<JsonNode> getData(String service, String method)
Result<JsonNode> getData(String service, String method, JsonNode parameters)
Result<JsonNode> getData(String service, String method, Locale locale)
Result<JsonNode> getData(String service, String method, JsonNode parameters, Locale locale)
```


```java
Result<JsonNode> writeDataWithResult(String service, String method)
Result<JsonNode> writeDataWithResult(String service, String method, JsonNode parameters)
Result<JsonNode> writeDataWithResult(String service, String method, Locale locale)
Result<JsonNode> writeDataWithResult(String service, String method, JsonNode parameters, Locale locale)
```

These methods are being used as follows:

* `service` is the name of the service on the server which must be annotated with `@MessageController("service")`.
* `method` is the name of the method of the corresponding service which must be annotated with `@MessageMethod`.
* `parameters` is a json node that can be used to pass any data to the called method.
* `locale` is the locale that shall be used on the server side when serializing the answer.

How to Use it
=============
You can find a description of how to use this framework and an example project here:

[How to - Server](https://github.com/owetterau/neo4j-websockets-example/blob/master/server/README.md)

[How to - Client](https://github.com/owetterau/neo4j-websockets-example/blob/master/client/README.md)

[Example Project](https://github.com/owetterau/neo4j-websockets-example)

### Example Project
An example project using this framework can be found here:

https://github.com/owetterau/neo4j-websockets-example

Some Final Thoughts
-------------------
I know that this description is not the most detailed one, yet. As work on this framework continues and maybe a few remarks come in I hopefully will be able to make it better...
