package de.matrixweb.osgi.jetty.api;


public interface ServletContext {

	String[] getConnectorNames();
	
	String[] getVirtualHosts();
	
}
