package de.matrixweb.osgi.jetty.api;

import java.util.HashMap;
import java.util.Map;

public class DefaultServletContext implements ServletContext {

	private String[] connectorNames;

	private String[] virtualHosts;

	private String contextPath;

	private Map<String, String> initParameters = new HashMap<String, String>();

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

	@Override
	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	@Override
	public Map<String, String> getInitParameters() {
		return initParameters;
	}

}
