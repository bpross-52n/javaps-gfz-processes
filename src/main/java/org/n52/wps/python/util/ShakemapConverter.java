package org.n52.wps.python.util;

import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.media.jai.JAI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.earthquake.eqcenter.shakemap.GridFieldType;
import gov.usgs.earthquake.eqcenter.shakemap.GridSpecificationType;
import gov.usgs.earthquake.eqcenter.shakemap.ShakemapGridDocument.ShakemapGrid;

public class ShakemapConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShakemapConverter.class);

    private final String lonFieldName = "LON";
    private final String latFieldName = "LAT";
    private final String pgaFieldName = "PGA";
    private int gridFieldsCount;
    private int latIndex;
    private int lonIndex;
    private int pgaIndex;
    private int width;
    private int height;
    private double minx;
    private double miny;
    private double maxx;
    private double maxy;
    private double a;
    private double c;
    private double b;
    private double d;

    public ShakemapConverter(ShakemapGrid shakemapGrid) {

        GridSpecificationType gridSpecification = shakemapGrid.getGridSpecification();

        width  = Integer.parseInt(gridSpecification.getNlon());
        height = Integer.parseInt(gridSpecification.getNlat());

        miny = Double.parseDouble(gridSpecification.getLatMin());
        minx = Double.parseDouble(gridSpecification.getLonMin());
        maxy = Double.parseDouble(gridSpecification.getLatMax());
        maxx = Double.parseDouble(gridSpecification.getLonMax());

        a = width / (maxx - minx);
        c = height / (miny - maxy);

        b = - (a * minx);
        d = - (c * maxy);

        GridFieldType[] gridFieldArray = shakemapGrid.getGridFieldArray();

        gridFieldsCount = gridFieldArray.length;

        //TODO maybe create Map<String, int> for other grid fields

        //try to find indexes in grid fields
        for (int i = 0; i < gridFieldArray.length; i++) {

            GridFieldType gridField = gridFieldArray[i];

            if(gridField.getName().equalsIgnoreCase(lonFieldName)){
                lonIndex = gridField.getIndex();
            } else if(gridField.getName().equalsIgnoreCase(latFieldName)){
                latIndex = gridField.getIndex();
            } else if(gridField.getName().equalsIgnoreCase(pgaFieldName)){
                pgaIndex = gridField.getIndex();
            }

        }
    }

    public void convertGridToRaster(WritableRaster raster, InputStream in) throws NumberFormatException, IOException{

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));

        String line = "";

        while((line = bufferedReader.readLine()) != null){

            if(line.isEmpty()){
                continue;
            }

            double lat = 0;
            double lon = 0;
            double pga = 0;

            for (int i = 0; i < gridFieldsCount ; i++) {

                //gidData is separated by blanks
                int blankIndex = line.indexOf(" ");

                //check if no blanks are left. another criteria for breaking the loop
                if(blankIndex < 0){
                    break;
                }

                String part = line.substring(0, blankIndex);

                if(i == latIndex-1){
                    lat = Double.parseDouble(part);
                } else if(i == lonIndex-1){
                    lon = Double.parseDouble(part);
                } else if(i == pgaIndex-1){
                    pga = Double.parseDouble(part);
                }

                line = line.substring(blankIndex).trim();
            }

            if((lat == 0) || (lon == 0)){
                break;
            }

            //calculate image coordinates
            double u = Math.ceil(a * lon + b);
            double v = Math.ceil(c * lat + d);

            if(!((u > width) || (v > height))){
                raster.setSample((int)u, (int)v, 0, pga);
            }else{
                LOGGER.warn("Image coordinates out of bounds: u: " + u + " v: " + v + " width: " + width + " height: " + height);
            }
        }
    }

    //see https://usgs.github.io/shakemap/manual3_5/tg_intensity.html
    private int getIntensity(double pga) {

        int intensity = 0;

        if(pga < 0.05){
            intensity = 0;
        } else if((0.05 <= pga) && (pga < 0.3)){
            intensity = 1;
        } else if((0.3 <= pga) && (pga < 2.8)){
            intensity = 2;
        } else if((2.8 <= pga) && (pga < 6.2)){
            intensity = 3;
        } else if((6.2 <= pga) && (pga < 12)){
            intensity = 4;
        } else if((12 <= pga) && (pga < 22)){
            intensity = 5;
        } else if((22 <= pga) && (pga < 40)){
            intensity = 6;
        } else if((40 <= pga) && (pga < 75)){
            intensity = 7;
        } else if((75 <= pga) && (pga < 139)){
            intensity = 8;
        } else if(pga > 139){
            intensity = 9;
        }

        return intensity;
    }

    public void writeGeotiff(GeoTiffWriter geoTiffWriter, GridCoverage coverage){
        GeoTiffFormat format = new GeoTiffFormat();

        GeoTiffWriteParams wp = new GeoTiffWriteParams();

        wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
        wp.setCompressionType("LZW");
        wp.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
        int width = ((GridCoverage2D) coverage).getRenderedImage().getWidth();
        int tileWidth = 1024;
        if(width<2048){
            tileWidth = new Double(Math.sqrt(width)).intValue();
        }
        wp.setTiling(tileWidth, tileWidth);
        ParameterValueGroup paramWrite = format.getWriteParameters();
        paramWrite.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(256*1024*1024);

        try {
            geoTiffWriter.write(coverage, (GeneralParameterValue[])paramWrite.values().toArray(new
                    GeneralParameterValue[1]));
        } catch (IllegalArgumentException e1) {
            LOGGER.error(e1.getMessage(), e1);
            throw new RuntimeException(e1);
        } catch (IndexOutOfBoundsException e1) {
            LOGGER.error(e1.getMessage(), e1);
            throw new RuntimeException(e1);
        } catch (IOException e1) {LOGGER.error(e1.getMessage(), e1);
            throw new RuntimeException(e1);
        }
    }

}
