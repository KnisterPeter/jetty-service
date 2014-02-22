Jetty Service is an OSGi jetty integration more flexible than osgi http-service
specs. It is possible to add multiple web-components to a webapp-context from
different bundles and share session as well as the servlet-context.
This makes it kind of a multi-bundle war like construct.

Besides the flexibility it does not fully comply to the servlet-spec nor the 
osgi http-service spec.

Configuration
=============

Servlet Context
---------------

The ServletContext describes the URL prefix (context) for which web-elements
could be registered.
This could be seen as the context a war-file creates when it is 
deployed (aka context-path).

    DefaultServletContext servletContext = new DefaultServletContext();
    Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put(ServletContext.CONTEXT_ID, "/");
    // Declared ServetContext parameters (global to the application)
    props.put("init.name", "value");
    regs.add(context.registerService(ServletContext.class, servletContext, props));

Filter
------

    javax.servlet.Filter filter = new Filter() {
      ....
    };
    props = new Hashtable<String, Object>();
    // The context to apply this filter to
    props.put(ServletContext.CONTEXT_ID, "/");
    // The alias or path spec to apply this filter to
    props.put(ServletContext.ALIAS, "/msg");
    // The servlet a filter is 'bound' to
    props.put("servlet.name", "Home");
    // The order in which filters gets registred
    props.put(Constants.SERVICE_RANKING, 1);
    regs.add(context.registerService(Filter.class, filter, props));

Servlet
-------

    javax.servlet.Servlet servlet = new HttpServlet() {
      ...
    };
    props = new Hashtable<String, Object>();
    // The context to apply this servlet to
    props.put(ServletContext.CONTEXT_ID, "/");
    // The alias or path spec to apply this servlet to
    props.put(ServletContext.ALIAS, "/");
    // The servlet name filter could be 'bound' to
    props.put(ServletContext.NAME, "Home");
    regs.add(context.registerService(Servlet.class, servlet, props));

Credits
-------

Thanks to [SinnerSchrader](http://www.sinnerschrader.com/) for their support
and the time to work on this project.
