package org.rapla.plugin.export2ical.server;

import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.framework.Configuration;
import org.rapla.framework.PluginDescriptor;
import org.rapla.framework.RaplaContextException;
import org.rapla.plugin.export2ical.Export2iCalPlugin;
import org.rapla.plugin.export2ical.ICalExport;
import org.rapla.plugin.export2ical.ICalTimezones;
import org.rapla.server.ServerServiceContainer;


public class Export2iCalServerPlugin implements PluginDescriptor<ServerServiceContainer> {

	public void provideServices(ServerServiceContainer container, Configuration config) throws RaplaContextException {
		container.addRemoteMethodFactory(ICalTimezones.class, RaplaICalTimezones.class, config);
		if (!config.getAttributeAsBoolean("enabled", Export2iCalPlugin.ENABLE_BY_DEFAULT))
			return;

		container.addContainerProvidedComponent(Export2iCalPlugin.RESOURCE_FILE, I18nBundleImpl.class, I18nBundleImpl.createConfig(Export2iCalPlugin.RESOURCE_FILE.getId()));
		container.addRemoteMethodFactory(ICalExport.class, RaplaICalExport.class, config);
        container.addWebpage(Export2iCalPlugin.GENERATOR,Export2iCalServlet.class, config);
	}

}