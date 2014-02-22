package de.matrixweb.osgi.jetty.api;

import java.util.Map;

public interface ServletContext {

	String[] getConnectorNames();
	
	String[] getVirtualHosts();
	
	String getContextPath();
	
	Map<String, String> getInitParameters();
	
}
