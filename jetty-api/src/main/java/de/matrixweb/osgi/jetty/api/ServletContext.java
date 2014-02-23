package de.matrixweb.osgi.jetty.api;

/**
 * This is a servlet context description. Every exposed service using this
 * interface will be created as a new servlet context in the jetty webserver.
 * 
 * @author markusw
 */
public interface ServletContext {

  /**
   * The id of the context. This should be set to the required context-path
   * (e.g. '/').
   */
  String CONTEXT_ID = "contextId";

  /**
   * This is a naming constant. It could be used for naming filters and
   * servlets.
   */
  String NAME = "name";

  /**
   * The alias constant. It is used for filters and servlets to define the
   * mapping.
   */
  String ALIAS = "alias";

  /**
   * @return Returns either 'hostname:port' or 'connectorname' and defines to
   *         which connector this context should be bound to.
   */
  String[] getConnectorNames();

  /**
   * @return Returns a list of virtual host names. Whildcards like '*.' are
   *         allowed. <code>null</code> means every virtual host.
   */
  String[] getVirtualHosts();

}
