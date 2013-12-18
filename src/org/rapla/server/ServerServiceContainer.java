package org.rapla.server;

import org.rapla.framework.Configuration;
import org.rapla.framework.Container;
import org.rapla.framework.RaplaContextException;
import org.rapla.servletpages.RaplaPageGenerator;

public interface ServerServiceContainer extends Container 
{
    <T> void addRemoteMethodFactory( Class<T> role, Class<? extends RemoteMethodFactory<T>> factory);
    <T> void addRemoteMethodFactory( Class<T> role, Class<? extends RemoteMethodFactory<T>> factory, Configuration config);
    <T> RemoteMethodFactory<T> getWebservice(Class<T> role) throws RaplaContextException;
    /**
     * You can add arbitrary serlvet pages to your rapla webapp.
     *
     * Example that adds a page with the name "my-page-name" and the class
     * "org.rapla.plugin.myplugin.MyPageGenerator". You can call this page with <code>rapla?page=my-page-name</code>
     * <p/>
     * In the provideService Method of your PluginDescriptor do the following
     <pre>
     container.addContainerProvidedComponent( RaplaExtensionPoints.SERVLET_PAGE_EXTENSION, "org.rapla.plugin.myplugin.MyPageGenerator", "my-page-name", config);
     </pre>

    *@see org.rapla.servletpages.RaplaPageGenerator
     */
    <T extends RaplaPageGenerator> void addWebpage(String pagename, Class<T> pageClass);
    <T extends RaplaPageGenerator> void addWebpage(String pagename, Class<T> pageClass, Configuration config);
    /** @return null when the server doesn't have the webpage 
     * @throws RaplaContextException */
	RaplaPageGenerator getWebpage(String page) throws RaplaContextException;

	
}
