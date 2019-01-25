package org.n52.python.test;

import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.media.jai.RasterFactory;

import org.apache.xmlbeans.XmlException;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.n52.wps.python.util.ShakemapConverter;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.earthquake.eqcenter.shakemap.GridSpecificationType;
import gov.usgs.earthquake.eqcenter.shakemap.ShakemapGridDocument;
import gov.usgs.earthquake.eqcenter.shakemap.ShakemapGridDocument.ShakemapGrid;


public class ShakemapConverterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShakemapConverterTest.class);

    private final String lonFieldName = "LON";
    private final String latFieldName = "LAT";
    private final String pgaFieldName = "PGA";

    @Test
    public void testConvertShakemap() throws XmlException, IOException, NoSuchAuthorityCodeException, FactoryException{

        //read grid

        //normalize coordinates (world to image)

        ShakemapGridDocument shakemapGridDocument = ShakemapGridDocument.Factory.parse(getClass().getResourceAsStream("shakemap.xml"));

        ShakemapGrid shakemapGrid = shakemapGridDocument.getShakemapGrid();

        ShakemapConverter shakemapConverter = new ShakemapConverter(shakemapGrid);

        GridSpecificationType gridSpecification = shakemapGrid.getGridSpecification();
//
        final int width  = Integer.parseInt(gridSpecification.getNlon());
        final int height = Integer.parseInt(gridSpecification.getNlat());
//
        double minx = Double.parseDouble(gridSpecification.getLonMin());
        double miny = Double.parseDouble(gridSpecification.getLatMin());
        double maxx = Double.parseDouble(gridSpecification.getLonMax());
        double maxy = Double.parseDouble(gridSpecification.getLatMax());
//
//        double a = width / (maxx - minx);
//        double c = height / (miny - maxy);
//
//        double b = - (a * minx);
//        double d = - (c * maxy);
//
//        GridFieldType[] gridFieldArray = shakemapGrid.getGridFieldArray();
//
//        int gridFieldsCount = gridFieldArray.length;
//
//        //indexes of necessary gridfields, set default ones
//        int lonIndex = 0;
//        int latIndex = 1;
//        int pgaIndex = 2;
//
//        //TODO maybe create Map<String, int> for other grid fields
//
//        //try to find indexes in grid fields
//        for (int i = 0; i < gridFieldArray.length; i++) {
//
//            GridFieldType gridField = gridFieldArray[i];
//
//            if(gridField.getName().equalsIgnoreCase(lonFieldName)){
//                lonIndex = gridField.getIndex();
//            } else if(gridField.getName().equalsIgnoreCase(latFieldName)){
//                latIndex = gridField.getIndex();
//            } else if(gridField.getName().equalsIgnoreCase(pgaFieldName)){
//                pgaIndex = gridField.getIndex();
//            }
//
//        }
//
        String gridData = shakemapGrid.getGridData();
//
//        //TODO just for robustness, maybe remove this
//        gridData = gridData.replace("  ", " ");
//        gridData = gridData.replace("\n", " ");
//        gridData = gridData.trim();
//
//        //use this as guarantee that the loop will finish
//        int previousGridLength = 0;

        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT,
                                                                 width+1, height+1, 1, null);

        ByteArrayInputStream in = new ByteArrayInputStream(gridData.getBytes());

        shakemapConverter.convertGridToRaster(raster, in);

//        while(!(gridData.length() == 0)){
//
//            previousGridLength = gridData.length();
//
//            double lat = 0;
//            double lon = 0;
//            double pga = 0;
//
//            for (int i = 0; i < gridFieldsCount ; i++) {
//
//                //gidData is separated by blanks
//                int blankIndex = gridData.indexOf(" ");
//
//                //check if no blanks are left. another criteria for breaking the loop
//                if(blankIndex < 0){
//                    break;
//                }
//
//                String part = gridData.substring(0, blankIndex);
//
//                if(i == latIndex-1){
//                    lat = Double.parseDouble(part);
//                }else if(i == lonIndex-1){
//                    lon = Double.parseDouble(part);
//                } else if(i == pgaIndex-1){
//                    pga = Double.parseDouble(part);
//                }
//
//                gridData = gridData.substring(blankIndex).trim();
//
//            }
//
//            if((lat == 0) || (lon == 0)){
//                break;
//            }
//
//            //calculate image coordinates
//            long u = Math.round(a * lat + b);
//            long v = Math.round(c * lon + d);
//
//            System.out.println(u + " " + v);
////            System.out.println((a * lat + b) + " " + (c * lon + d));
////            System.out.println(Math.ceil(a * lat + b) + " " + Math.ceil(c * lon + d));
////            System.out.println(Math.round(a * lat + b) + " " + Math.round(c * lon + d));
//
//            raster.setSample((int)u, (int)v, 0, pga);
//
//            if(previousGridLength == gridData.length()){
//                break;
//            }
//        }

//        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
//        Envelope envelope = new Envelope2D(crs, 0, 0, width, height);
        Envelope envelope = new ReferencedEnvelope(minx, maxx, miny, maxy, crs);

        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
        GridCoverage gc = factory.create("Shakemap", raster, envelope);

        GeoTiffWriter geoTiffWriter = null;
        String tmpDirPath = System.getProperty("java.io.tmpdir");
        String fileName = tmpDirPath + File.separatorChar + "temp" + UUID.randomUUID() + ".tif";
        File outputFile = new File(fileName);

        try {
            geoTiffWriter = new GeoTiffWriter(outputFile);
            shakemapConverter.writeGeotiff(geoTiffWriter, gc);
            geoTiffWriter.dispose();

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new IOException("Could not create output due to an IO error");
        }

        System.out.println(outputFile.getAbsolutePath());

//        Color[] colors = new Color[] {Color.BLUE, Color.CYAN, Color.WHITE, Color.YELLOW, Color.RED};
//        gc = factory.create("My colored coverage", raster, envelope,
//                            null, null, null, new Color[][] {colors}, null);

    }

}
