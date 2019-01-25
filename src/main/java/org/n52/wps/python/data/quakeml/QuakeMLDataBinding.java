package org.n52.wps.python.data.quakeml;

import org.n52.javaps.gt.io.data.GenericFileDataWithGT;
import org.n52.javaps.io.complex.ComplexData;

public class QuakeMLDataBinding implements ComplexData<GenericFileDataWithGT> {

    /**
     *
     */
    private static final long serialVersionUID = 5635195859106892797L;

    protected GenericFileDataWithGT payload;

    public QuakeMLDataBinding(GenericFileDataWithGT fileData){
        this.payload = fileData;
    }

    public GenericFileDataWithGT getPayload() {
        return payload;
    }

    public Class<GenericFileDataWithGT> getSupportedClass() {
        return GenericFileDataWithGT.class;
    }
}