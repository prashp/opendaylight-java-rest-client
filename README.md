# OpenDayLight Java REST Client

The java client supports all OpenDayLight AD-SAL REST APIs. It generates
convenience wrappers to make developing clients in java much easier.

## Building the Client
To build the client, you need JDK (7.x) and Maven (3.x) installed on your
machine. Just run `mvn clean install` in the root directory to build the
client.

## JavaDoc
Once the build is complete, you can locate the javadoc under
`rest.client/target/apidocs/index.html` for the top level types. For all
dependent types, you can refer to the ODL javadocs for details.

## Changing message type
The binding types are all identical irrespective of the message type being
used. Use `Config.setMediaType()` to change between JSON and XML message types.

## ODL Version
The client is wired to build off the latest ODL version. You can change this
in the `rest.client/pom.xml`

## How to develop a client
The `examples` module includes a couple of examples on how to develop the client.

Here is a simple example on how you can retrieve the flows:

```java

    import org.opendaylight.controller.flowprogrammer.northbound.FlowConfigs;
    import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
    import org.opendaylight.tools.client.rest.Config;
    import org.opendaylight.tools.client.rest.FlowprogrammerHelper;
    import org.opendaylight.tools.clientgen.GetResponse;

    ...
    // setup config
    Config config = new Config();
    config.setUsername("admin");
    config.setPassword("admin");
    config.setAdminUrl("http://localhost:8080");

    // Lookup flows
    System.out.println("==== dumping flows");
    FlowprogrammerHelper flowHelper = new FlowprogrammerHelper();
    flowHelper.setConfig(config);
    GetResponse<FlowConfigs> r1 = flowHelper.getStaticFlows("default");
    for (FlowConfig fc : r1.getEntity().getFlowConfig()) {
      System.out.println(fc.getName());
    }
    ...

```

Look at the generated code under `rest.client/target/generated-sources/src`
which gives an idea of the top level usage of the client API.

## Jython usage

This client can also be used in jython. Add all dependencies to the jython
classpath and import the types. The above can be rewritten as below:

```Python

from org.opendaylight.tools.client.rest import FlowprogrammerHelper
    ...
    // setup config
    config = Config()
    config.setUsername("admin")
    config.setPassword("admin")
    config.setAdminUrl("http://localhost:8080")

    flowHelper = FlowprogrammerHelper()
    flowHelper.setConfig(config)
    response = flowHelper.getStaticFlows("default")
    for fc in r1.getEntity().getFlowConfig()
      print("%s ", %(fc.getName()))
    }
    ...
```
