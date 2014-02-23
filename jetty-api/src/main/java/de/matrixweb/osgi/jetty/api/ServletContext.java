package de.matrixweb.osgi.jetty.api;

public interface ServletContext {

  String CONTEXT_ID = "contextId";

  String NAME = "name";

  String ALIAS = "alias";

  String[] getConnectorNames();

  String[] getVirtualHosts();

}
