package com.gaia3d.command;

import lombok.extern.slf4j.Slf4j;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.factory.PropertyAuthorityFactory;
import org.geotools.referencing.factory.ReferencingFactoryContainer;
import org.geotools.util.factory.Hints;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

@Slf4j
public class GeotoolsConfigurator {
    public void setEpsg() throws IOException {
        Hints hints = new Hints(Hints.CRS_AUTHORITY_FACTORY, PropertyAuthorityFactory.class);
        log.info("[CONFIG]=================={}==================}", ReferencingFactoryFinder.getCRSAuthorityFactories(hints).size());

        URL epsg = Thread.currentThread().getContextClassLoader().getResource("epsg.properties");

        CoordinateReferenceSystem crs = null;
        try {
            crs = CRS.decode("EPSG:6737");
        } catch (FactoryException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        }

        if (epsg != null) {
            ReferencingFactoryContainer referencingFactoryContainer = ReferencingFactoryContainer.instance(hints);
            PropertyAuthorityFactory factory = new PropertyAuthorityFactory(referencingFactoryContainer, Citations.fromName("EPSG"), epsg);

            ReferencingFactoryFinder.scanForPlugins();

            Set<CRSAuthorityFactory> factories = ReferencingFactoryFinder.getCRSAuthorityFactories(hints);
            log.info("[CONFIG]=================={}==================}", factories.size());
            factories.forEach(f -> {
                log.info("{}", f);
            });

            crs = null;
            try {
                crs = CRS.decode("EPSG:6737");
            } catch (FactoryException e) {
                log.error("Error : {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
}
