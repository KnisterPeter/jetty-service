package de.matrixweb.osgi.jetty.api;

public class DefaultServletContext implements ServletContext {

  private String[] connectorNames;

  private String[] virtualHosts;

  @Override
  public String[] getConnectorNames() {
    return this.connectorNames;
  }

  public void setConnectorNames(final String[] connectorNames) {
    this.connectorNames = connectorNames;
  }

  @Override
  public String[] getVirtualHosts() {
    return this.virtualHosts;
  }

  public void setVirtualHosts(final String[] virtualHosts) {
    this.virtualHosts = virtualHosts;
  }

}
