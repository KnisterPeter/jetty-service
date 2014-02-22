package de.matrixweb.osgi.jetty.api;


public class DefaultServletContext implements ServletContext {

	private String[] connectorNames;

	private String[] virtualHosts;

	@Override
	public String[] getConnectorNames() {
		return connectorNames;
	}

	public void setConnectorNames(String[] connectorNames) {
		this.connectorNames = connectorNames;
	}

	@Override
	public String[] getVirtualHosts() {
		return virtualHosts;
	}

	public void setVirtualHosts(String[] virtualHosts) {
		this.virtualHosts = virtualHosts;
	}

}
