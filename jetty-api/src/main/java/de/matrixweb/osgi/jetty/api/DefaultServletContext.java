package de.matrixweb.osgi.jetty.api;

/**
 * @author markusw
 */
public class DefaultServletContext implements ServletContext {

  private String[] connectorNames;

  private String[] virtualHosts;

  @Override
  public String[] getConnectorNames() {
    return this.connectorNames;
  }

  /**
   * @param connectorNames
   */
  public void setConnectorNames(final String[] connectorNames) {
    this.connectorNames = connectorNames;
  }

  @Override
  public String[] getVirtualHosts() {
    return this.virtualHosts;
  }

  /**
   * @param virtualHosts
   */
  public void setVirtualHosts(final String[] virtualHosts) {
    this.virtualHosts = virtualHosts;
  }

}
