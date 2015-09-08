Neo4j Websockets Client/Server Framework
========================================
This framework allows to connect clients with embedded Neo4j servers through websockets. Furthermore it offers high
availability and load balancing to the clients when using Neo4j in a cluster.

Spring Framework is used for dependency injection and websocket communication.

Server
------
### Configuration and startup

#### Starter
The *Starter* class is being used to configure the embedded Neo4j instance and to start that instance. This is
especially helpful for running unit tests where you most likely would like to use an in-memory database.

```java
import com.example.data.database.ExampleEmbeddedNeo4j;
import com.example.data.model.schema.Schema;
import de.oliverwetterau.neo4j.websockets.server.neo4j.Configurer;
import de.oliverwetterau.neo4j.websockets.server.neo4j.EmbeddedNeo4j;
import de.oliverwetterau.neo4j.websockets.server.neo4j.Starter;
import org.neo4j.graphdb.config.Setting;

import java.util.Map;

public class ExampleStarter extends Starter {
    public ExampleStarter(Class embeddedNeo4jClass, Configurer configurer, Map<Setting,String> settings) throws Exception{
        super(embeddedNeo4jClass, configurer, settings);

        Schema.readSchemaAnnotations();
    }

    @Override
    public void before(Class embeddedNeo4j) {
    }

    @Override
    public void after(EmbeddedNeo4j embeddedNeo4j) {
        ((ExampleEmbeddedNeo4j) embeddedNeo4j).warmUp();
    }
}
```

The *Starter* used in the example application extends the default starter logic with reading annotations and warming
up the database after creation.

A call of the derived starter may look like this (it is important to add the framework to the list of packages being
scanned for annotations by Spring):

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.example.data", "de.oliverwetterau.neo4j.websockets"})
public class Application extends SpringBootServletInitializer {
    public static void main(final String[] args) throws Throwable {
        Map<Setting,String> neo4jSettings = getNeo4jSettings(args);

        new ExampleStarter(ExampleEmbeddedNeo4j.class, new ExampleNeo4jConfigurer(), neo4jSettings).run();

        SpringApplication.run(Application.class, args);
    }
}
```

#### EmbeddedNeo4j
To have additional functionality, *EmbeddedNeo4j* can be derived (as seen in the examples before) so that you e.g. can get access to spatial
functionality.

```java
public class ExampleEmbeddedNeo4j extends EmbeddedNeo4j {
    protected static final String SPATIAL_LAYER_NAME = "spatial";

    /** spatial database instance */
    protected static SpatialDatabaseService spatialDatabase = null;

    public void startSchemaCreation(GraphDatabaseService databaseService) {
        try (Transaction tx = databaseService.beginTx()) {
            IndexManager indexManager = databaseService.index();

            indexManager.forNodes(
                    SPATIAL_LAYER_NAME,
                    Collections.unmodifiableMap(MapUtil.stringMap(
                            "provider", "spatial",
                            "geometry_type", "point",
                            "lat", "Latitude",
                            "lon", "Longitude"
                    ))
            );

            tx.success();
        }
    }

    public SpatialDatabaseService getSpatialDatabase() {
        if (spatialDatabase == null) {
            spatialDatabase = new SpatialDatabaseService(getDatabase());
        }

        return spatialDatabase;
    }

    @Transactional
    public void warmUp() {
        final int batchSize = 1000000;

        int nodesCount = 0, relationshipsCount = 0;

        for (Node node : GlobalGraphOperations.at(getDatabase()).getAllNodes()) {
            if ((++nodesCount + relationshipsCount) % batchSize == 0) {
                logger.debug("[warmUp] n = {}, r = {}", String.format("%,d", nodesCount), String.format("%,d", relationshipsCount));
            }

            for (String key : node.getPropertyKeys()) {
                //noinspection ResultOfMethodCallIgnored
                node.getProperty(key).toString();
            }

            node.getLabels();

            for (Relationship relationship : node.getRelationships(Direction.OUTGOING)) {
                relationship.getOtherNode(node);
                relationship.getType();

                for (String key : relationship.getPropertyKeys()) {
                    //noinspection ResultOfMethodCallIgnored
                    relationship.getProperty(key).toString();
                }
            }
        }
    }
}
```

#### Configurer
For the actual configuration, a class implementing the *Configurer* interface must be created as it needs to be passed
to the *Starter*.

```java
public class ExampleNeo4jConfigurer implements Configurer {
    /** Neo4j database instance */
    protected static HighlyAvailableGraphDatabase database = null;

    protected static Map<Setting,String> settings;
    /** storage path of the database files */
    protected static String storagePath = null;
    /** internal high availibity id in a cluster */
    protected static Integer highAvailabilityId = null;

    /** http port for the web gui */
    protected static Integer httpPort = null;
    /** https port for the web gui */
    protected static Integer httpsPort = null;

    @Override
    public void init(final Map<Setting,String> settings) {
        ExampleNeo4jConfigurer.settings = settings;

        highAvailabilityId = Integer.parseInt(settings.get(ClusterSettings.server_id));
        storagePath = settings.get(GraphDatabaseSettings.store_dir);

        httpPort = Integer.parseInt(settings.get(ServerSettings.webserver_port));
        httpsPort = Integer.parseInt(settings.get(ServerSettings.webserver_https_port));
    }

    @Override
    public GraphDatabaseService getGraphDatabase() {
        if (database == null) {
            GraphDatabaseBuilder graphDatabaseBuilder = new HighlyAvailableGraphDatabaseFactory()
                        .newEmbeddedDatabaseBuilder(settings.get(GraphDatabaseSettings.store_dir));

            for (Map.Entry<Setting,String> parameter : settings.entrySet()) {
                if (parameter.getKey().equals(GraphDatabaseSettings.store_dir)) continue;

                graphDatabaseBuilder.setConfig(parameter.getKey(), parameter.getValue());
            }

            database = (HighlyAvailableGraphDatabase) graphDatabaseBuilder.newGraphDatabase();
        }

        return database;
    }

    @Override
    public void dropGraphDatabase() {
        database.shutdown();
        database = null;
    }

    @Override
    public ServerConfigurator getServerConfigurator(final HighlyAvailableGraphDatabase databaseService) {
        ServerConfigurator configurator = new ServerConfigurator(databaseService);
        configurator.configuration().addProperty(ServerConfigurator.WEBSERVER_PORT_PROPERTY_KEY, httpPort);
        configurator.configuration().addProperty(ServerConfigurator.WEBSERVER_HTTPS_PORT_PROPERTY_KEY, httpsPort);
        configurator.configuration().addProperty(ServerConfigurator.WEBSERVER_ADDRESS_PROPERTY_KEY, "0.0.0.0");

        return configurator;
    }

    @Override
    public Integer getHighAvailabilityId() {
        return highAvailabilityId;
    }

    @Override
    public boolean isProductionDatabase() {
        return true;
    }
}
```

Probably the most important part of the *Configurer* class is the *getGraphDatabase* method as this is the place where
the Neo4j instance is being created.

### Websocket messages handling

#### Annotations
To make it easy to route messages that come through a websocket to the correct service that can deal with the message,
there are two annotations: *@MessageController("name")* and *@MessageMethod*.

*@MessageController("name")* defines a class to be the handler of all messages marked with the service name "name".

*@MessageMethod* defines a method to deal with messages marked with the method name "name". The signature of methods
annotated with *@MessageMethod* must always be: `public Result methodName(JsonNode jsonNode)`.

```java
@Service
@MessageController("system")
public class SystemController {
    protected final SchemaService schemaService;

    @Autowired
    public SystemController(SchemaService schemaService)
    {
        this.schemaService = schemaService;
    }

    @MessageMethod
    public Result initWithFakeData(JsonNode jsonNode) {
        try {
            schemaService.initWithFakeData();
        }
        catch (Exception e) {
            return new Result(e);
        }

        return new Result<>(true);
    }
}
```

The *Result* class will be explained later...

#### CommandHandler
Instances of the annotated classes must be made available to the *CommandHandler* class so that the messages can be
routed to them. Hence, you must derive a class from *CommandHandler*, make it a *@Service* and add all instances to the
*CommandHandler* by calling *setControllerInstance*:

```java
@Service
public class ExampleCommandHandler extends CommandHandler {
    @Autowired
    public ExampleCommandHandler(JsonObjectMapper jsonObjectMapper, ThreadLocale threadLocale,
                                 SystemController systemController, ClientsController clientsController)
            throws Exception
    {
        super(ExampleCommandHandler.class.getPackage().getName(), jsonObjectMapper, threadLocale, true);

        setControllerInstance(systemController);
        setControllerInstance(clientsController);
    }
}
```

Again, the *JsonObjectMapper* and *ThreadLocale* classes will be explained later..

#### Result, Error, JsonObjectMapper and ThreadLocale
These classes are tied closely together. *Result* is the expected format on the client side when receiving answers to
a websocket message. It can contain any data (*Result* is generic) or *Errors*. Hence, *Error* is the expected format
of error messages on the client side.

The Jackson library is used for serialization and deserialization of the messages sent between client and server using
binary json (SMILE). To allow for the addition of custom serializers, *JsonObjectMapper* has to be initialized with
an instance of *JsonObjectSerializers* (or with null). *Result* and *Error* will use *JsonObjectMapper* to serialize
themselves. Furthermore, all incoming messages on the server side will be deserialzed with *JsonObjectMapper*.

As each message coming from the client also contains a *Locale* defining the language that shall be used an instance of
*ThreadLocale* is being used by *JsonObjectMapper* for serialization with the correct language settings. A class
implementing *ThreadLocale* therefore must be created and made available as a *@Service*. Here is an example
implementation:

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
### Configuration and start up

#### ServerUri
The most important information regarding the clients is the list of available servers. To pass that list to the
framework a class implementing the *ServerUri* interface and annotated with *@Service* must be created. The following
example will read that data from an environment variable called "database.server.uris":

```java
@Service
public class DatabaseUri implements ServerUri {
    protected String[] uris;

    @Autowired
    public DatabaseUri(@Value("${database.server.uris}") String uri) {
        uris = uri.split(",");
    }

    public String[] getServerUris() {
        return uris;
    }
}
```

#### Startup
Similar to the server, it is important add this framework's package to the list of packages being scanned by Spring to
activate it's functionality:

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.example", "de.oliverwetterau.neo4j.websockets"})
public class Application {
    public static void main(String[] arguments) throws Exception {
        SpringApplication.run(Application.class, arguments);
    }
}
```

That's all to connect the client to the servers...

Sending and Retrieving
----------------------
*DatabaseService* basically offers to different methods of communication with the servers: *getData* and
*writeDataWithResult* whereas the first is being used for pure read access and the latter should be used for all
messages that may lead to writes to Neo4j.

`These method have four different signatures (signatures for *writeDataWithResult* are the same):
* `Result<JsonNode> getData(String service, String method)`
* `Result<JsonNode> getData(String service, String method, Locale locale)`
* `Result<JsonNode> getData(String service, String method, JsonNode parameters)`
* `Result<JsonNode> getData(String service, String method, JsonNode parameters, Locale locale)`

The parameters are being used as follows:
* `service` is the name of the service on the server which must be annotated with *@MessageController("service")*.
* `method` is the name of the method of the corresponding service which must be annotated with *@MessageMethod*.
* `parameters` is a json node that can be used to pass any data to the called method.
* `locale` is the locale that shall be used on the server side when serializing the answer.

Some Final Thoughts
-------------------
I know that this description is not the most detailed one, yet. As work on this framework continues and maybe a few
remarks come in I hopefully will be able to make it better...
