package org.n52.wps.python.algorithm;

import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.media.jai.RasterFactory;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.n52.javaps.algorithm.AbstractSelfDescribingAlgorithm;
import org.n52.javaps.algorithm.ExecutionException;
import org.n52.javaps.annotation.ConfigurableClass;
import org.n52.javaps.annotation.Properties;
import org.n52.javaps.engine.ProcessExecutionContext;
import org.n52.javaps.gt.io.data.GenericFileDataWithGT;
import org.n52.javaps.gt.io.data.binding.complex.GenericFileDataWithGTBinding;
import org.n52.shetland.ogc.ows.OwsCode;
import org.n52.shetland.ogc.wps.Format;
import org.n52.wps.python.util.JavaProcessStreamReader;
import org.n52.wps.python.util.ShakemapConverter;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

import gov.usgs.earthquake.eqcenter.shakemap.GridSpecificationType;
import gov.usgs.earthquake.eqcenter.shakemap.ShakemapGridDocument;
import gov.usgs.earthquake.eqcenter.shakemap.ShakemapGridDocument.ShakemapGrid;

@Properties(defaultPropertyFileName = "shakemapprocess.properties")
public class ShakemapProcess extends AbstractSelfDescribingAlgorithm implements ConfigurableClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShakemapProcess.class);

    private String workspacePath;

    public ShakemapProcess() {
        setTitle("ShakemapProcess");
        setAbstract("Produces a shakemap grid based on a QuakeML input");
        setVersion("1.0.0");
        setStatusSupported(true);
        setStoreSupported(true);

        Format defaultShakemapFormat = new Format("text/xml", Charsets.UTF_8, "http://quakeml.org/xmlns/quakeml/1.2/QuakeML-1.2.xsd");
        Format defaultQuakeMLFormat = new Format("text/xml", Charsets.UTF_8, "http://quakeml.org/xmlns/quakeml/1.2/QuakeML-1.2.xsd");

        Set<Format> supportedQuakeMLFormats = new HashSet<>();

        supportedQuakeMLFormats.add(new Format("text/xml", Charsets.UTF_8, "http://quakeml.org/xmlns/quakeml/1.2/QuakeML-1.2.xsd"));
        supportedQuakeMLFormats.add(new Format("application/vnd.geo+json"));

        Set<Format> supportedShakemapFormats = new HashSet<>();

        supportedShakemapFormats.add(new Format("text/xml", Charsets.UTF_8, "http://earthquake.usgs.gov/eqcenter/shakemap/xml/schemas/shakemap.xsd"));
        supportedShakemapFormats.add(new Format("application/wms"));
        supportedShakemapFormats.add(new Format("image/tiff"));
        supportedShakemapFormats.add(new Format("image/tiff", "base64"));

        addComplexInputDescription("quakeml-input", supportedQuakeMLFormats, GenericFileDataWithGTBinding.class);
        addComplexOutputDescription("shakemap-output", "shakemap-output", "shakemap-output", supportedShakemapFormats, GenericFileDataWithGTBinding.class);
    }

    @Override
    public void execute(ProcessExecutionContext context) throws ExecutionException {

        try {

            workspacePath = getProperties().path("workspacepath").asText() + "shakyground/";

            Runtime rt = Runtime.getRuntime();

            GenericFileDataWithGTBinding data = (GenericFileDataWithGTBinding) context.getInputs().get(new OwsCode("quakeml-input")).get(0);

            File quakeMLFile = data.getPayload().getBaseFile(false);

            File newQuakeMLFile = File.createTempFile("quakeml", ".xml");

            BufferedReader bufferedReader = new BufferedReader(new FileReader(quakeMLFile));

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(newQuakeMLFile));

            String line = "";

            while((line = bufferedReader.readLine()) != null){
                if(line.contains("<?xml version='1.0' encoding='UTF-8'?>")){
                    line = line.replace("<?xml version='1.0' encoding='UTF-8'?>", "");
                }if(line.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")){
                    line = line.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
                }
                bufferedWriter.write(line);
            }

            bufferedReader.close();
            bufferedWriter.close();

            LOGGER.debug("Quakeml file: " + newQuakeMLFile.getAbsolutePath());

            File outputShakemap = File.createTempFile("shakemap", ".xml");

            String pythonScriptName = "service.py";

            String command = "python3 " +  workspacePath + File.separatorChar + pythonScriptName  + " " + newQuakeMLFile.getAbsolutePath();

            LOGGER.debug("Command: " + command);

            Process proc = rt.exec(command, new String[]{"HOME=" + workspacePath}, new File(workspacePath));

            PipedOutputStream pipedOut = new PipedOutputStream();

            PipedInputStream pipedIn = new PipedInputStream(pipedOut);

            FileOutputStream fileOutputStream = new FileOutputStream(new File("/tmp/log" + System.currentTimeMillis() + ".log"));

            // attach error stream reader
            JavaProcessStreamReader errorStreamReader =
                    new JavaProcessStreamReader(proc.getErrorStream(), "ERROR", fileOutputStream);

            // attach output stream reader
            JavaProcessStreamReader outputStreamReader = new JavaProcessStreamReader(proc.getInputStream(), "OUTPUT", pipedOut);

            // start them
            errorStreamReader.start();
            outputStreamReader.start();

            // fetch output
            String output = "";
            try (BufferedReader ouptutReader = new BufferedReader(new InputStreamReader(pipedIn));) {
                String line2 = "";

                while ((line2 = ouptutReader.readLine()) != null) {
                    output = output.concat(line2 + "\n");
                }
            }

            try {
                proc.waitFor();
            } catch (InterruptedException e1) {
                LOGGER.error("Java process was interrupted.", e1);
            } finally {
                proc.destroy();
            }

            BufferedWriter shakemapWriter = new BufferedWriter(new FileWriter(outputShakemap));

            shakemapWriter.write(output);

            shakemapWriter.close();

            GenericFileDataWithGT genericFileDataWithGT = null;

            String mimeType = context.getOutputDefinition("shakemap-output").get().getFormat().getMimeType().get();

            if (mimeType.equals("text/xml")) {
                genericFileDataWithGT = new GenericFileDataWithGT(outputShakemap, mimeType);
            } else if(mimeType.equals("application/WMS") || mimeType.contains("tif")){
                ShakemapGridDocument shakemapGridDocument = ShakemapGridDocument.Factory.parse(outputShakemap);

                ShakemapGrid shakemapGrid = shakemapGridDocument.getShakemapGrid();

                ShakemapConverter shakemapConverter = new ShakemapConverter(shakemapGrid);

                GridSpecificationType gridSpecification = shakemapGrid.getGridSpecification();

                final int width = Integer.parseInt(gridSpecification.getNlon());
                final int height = Integer.parseInt(gridSpecification.getNlat());

                double minx = Double.parseDouble(gridSpecification.getLonMin());
                double miny = Double.parseDouble(gridSpecification.getLatMin());
                double maxx = Double.parseDouble(gridSpecification.getLonMax());
                double maxy = Double.parseDouble(gridSpecification.getLatMax());
                //
                String gridData = shakemapGrid.getGridData();

                WritableRaster raster =
                        RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, width + 1, height + 1, 1, null);

                ByteArrayInputStream in = new ByteArrayInputStream(gridData.getBytes());

                shakemapConverter.convertGridToRaster(raster, in);

                CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
                Envelope envelope = new ReferencedEnvelope(minx, maxx, miny, maxy, crs);

                GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
                GridCoverage gc = factory.create("Shakemap", raster, envelope);

                GeoTiffWriter geoTiffWriter = null;
                String tmpDirPath = System.getProperty("java.io.tmpdir");
                String fileName = tmpDirPath + File.separatorChar + "temp" + UUID.randomUUID() + ".tif";
                File outputTiff = new File(fileName);
                try {
                    geoTiffWriter = new GeoTiffWriter(outputTiff);
                    shakemapConverter.writeGeotiff(geoTiffWriter, gc);
                    geoTiffWriter.dispose();
                    genericFileDataWithGT = new GenericFileDataWithGT(outputTiff, "image/tiff");
                } catch (IOException e) {
                    // LOGGER.error(e.getMessage());
                    throw new IOException("Could not create output due to an IO error");
                }
            }

           context.getOutputs().put(new OwsCode("shakemap-output"), new GenericFileDataWithGTBinding(genericFileDataWithGT));

        } catch (Exception e) {
            LOGGER.error("Exception occurred while trying to execute python script.", e);
//            throw new ExceptionReport("Exception occurred while trying to execute python script.", ExceptionReport.NO_APPLICABLE_CODE);
        }

    }


    @Override
    public List<String> getInputIdentifiers() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public List<String> getOutputIdentifiers() {
        // TODO Auto-generated method stub
        return null;
    }

}
