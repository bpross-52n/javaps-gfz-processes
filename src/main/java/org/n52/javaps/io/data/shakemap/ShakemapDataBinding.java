package org.n52.javaps.io.data.shakemap;

import org.n52.javaps.io.complex.ComplexData;

import gov.usgs.earthquake.eqcenter.shakemap.ShakemapGridDocument;

public class ShakemapDataBinding implements ComplexData<ShakemapGridDocument> {

    /**
     *
     */
    private static final long serialVersionUID = 5635195859106892797L;

    private ShakemapGridDocument shakemapGrid;

    public ShakemapDataBinding(ShakemapGridDocument shakemapGrid) {
        this.shakemapGrid = shakemapGrid;
    }

    @Override
    public ShakemapGridDocument getPayload() {
        return shakemapGrid;
    }

    @Override
    public Class<ShakemapGridDocument> getSupportedClass() {
        return ShakemapGridDocument.class;
    }

}
