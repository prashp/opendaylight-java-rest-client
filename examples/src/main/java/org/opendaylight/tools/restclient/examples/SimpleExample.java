package org.opendaylight.tools.restclient.examples;


import org.opendaylight.controller.flowprogrammer.northbound.FlowConfigs;
import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.topology.northbound.EdgeProperties;
import org.opendaylight.controller.topology.northbound.Topology;
import org.opendaylight.tools.client.rest.Config;
import org.opendaylight.tools.client.rest.FlowprogrammerHelper;
import org.opendaylight.tools.client.rest.TopologyHelper;
import org.opendaylight.tools.clientgen.GetResponse;

import javax.ws.rs.core.MediaType;

public class SimpleExample {

  public static void main(String[] args) throws Exception {
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

    // disable verbose on underlying connection
    config.setVerbose(false);
    // change underlying transport to XML

    config.setMediaType(MediaType.APPLICATION_XML_TYPE);
    TopologyHelper topoHelper = new TopologyHelper();
    topoHelper.setConfig(config);

    System.out.println("==== dumping topology");
    GetResponse<Topology> r2 = topoHelper.getTopology("default");
    Topology topo = r2.getEntity();
    for (EdgeProperties e : topo.getEdgeProperties()) {
      String from = e.getEdge().getHeadNodeConnector().getNodeConnectorIDString();
      String to = e.getEdge().getTailNodeConnector().getNodeConnectorIDString();
      System.out.println("Edge: " + from + " -> " + to);
    }

  }

}

