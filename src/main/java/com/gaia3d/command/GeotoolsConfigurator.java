package com.gaia3d.command;

import lombok.extern.slf4j.Slf4j;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.factory.PropertyAuthorityFactory;
import org.geotools.referencing.factory.ReferencingFactoryContainer;
import org.geotools.util.factory.Hints;
import org.opengis.referencing.crs.CRSAuthorityFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

@Slf4j
public class GeotoolsConfigurator {
    public void setEpsg() throws IOException {
        Hints hints = new Hints(Hints.CRS_AUTHORITY_FACTORY, PropertyAuthorityFactory.class);
        log.info("[CONFIG]=================={}==================}", ReferencingFactoryFinder.getCRSAuthorityFactories(hints).size());

        URL epsg = Thread.currentThread().getContextClassLoader().getResource("epsg.properties");
        if (epsg != null) {
            ReferencingFactoryContainer referencingFactoryContainer = ReferencingFactoryContainer.instance(hints);
            PropertyAuthorityFactory factory = new PropertyAuthorityFactory(referencingFactoryContainer, Citations.fromName("EPSG"), epsg);

            ReferencingFactoryFinder.scanForPlugins();

            Set<CRSAuthorityFactory> factories = ReferencingFactoryFinder.getCRSAuthorityFactories(hints);
            log.info("[CONFIG]=================={}==================}", factories.size());
            factories.forEach(f -> {
                log.info("{}", f);
            });
        }
    }
}
