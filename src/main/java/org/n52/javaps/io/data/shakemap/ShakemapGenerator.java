package org.n52.javaps.io.data.shakemap;

import java.io.IOException;
import java.io.InputStream;

import org.n52.javaps.annotation.Properties;
import org.n52.javaps.description.TypedProcessOutputDescription;
import org.n52.javaps.io.AbstractPropertiesInputOutputHandler;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.OutputHandler;
import org.n52.shetland.ogc.wps.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Properties(defaultPropertyFileName = "shakemap.properties")
public class ShakemapGenerator extends AbstractPropertiesInputOutputHandler implements OutputHandler {

    private static Logger LOGGER = LoggerFactory
            .getLogger(ShakemapGenerator.class);

    public ShakemapGenerator() {
        super();
        addSupportedBinding(ShakemapDataBinding.class);
    }

    @Override
    public InputStream generate(TypedProcessOutputDescription<?> description,
            Data<?> data,
            Format format) throws IOException {

        if(data instanceof ShakemapDataBinding){
            return ((ShakemapDataBinding)data).getPayload().newInputStream();
        }

        LOGGER.error("Data not of type ShakemapDataBinding. Returning null.");

        return null;
    }

}
