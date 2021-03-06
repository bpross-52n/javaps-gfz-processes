package org.n52.wps.python.algorithm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.n52.javaps.algorithm.annotation.Algorithm;
import org.n52.javaps.algorithm.annotation.ComplexOutput;
import org.n52.javaps.algorithm.annotation.Execute;
import org.n52.javaps.algorithm.annotation.LiteralInput;
import org.n52.javaps.annotation.ConfigurableClass;
import org.n52.javaps.annotation.Properties;
import org.n52.javaps.gt.io.data.GenericFileDataWithGT;
import org.n52.wps.python.data.quakeml.QuakeMLDataBinding;
import org.n52.wps.python.util.JavaProcessStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Properties(defaultPropertyFileName = "quakemlprocess.properties")
@Algorithm(version = "1.0.0")
public class QuakeMLProcess implements ConfigurableClass{

    private static Logger LOGGER = LoggerFactory.getLogger(QuakeMLProcess.class);

    private final String lineSeparator = System.getProperty("line.separator");

    @LiteralInput(
            identifier = "lonmin", defaultValue = "288")
    public double lonmin;

    @LiteralInput(
            identifier = "lonmax", defaultValue = "292")
    public double lonmax;

    @LiteralInput(
            identifier = "latmin", defaultValue = "-70")
    public double latmin;

    @LiteralInput(
            identifier = "latmax", defaultValue = "-10")
    public double latmax;

    @LiteralInput(
            identifier = "mmin", defaultValue = "6.6")
    public double mmin;

    @LiteralInput(
            identifier = "mmax", defaultValue = "8.5")
    public double mmax;

    @LiteralInput(
            identifier = "zmin", defaultValue = "5")
    public double zmin;

    @LiteralInput(
            identifier = "zmax", defaultValue = "140")
    public double zmax;

    @LiteralInput(
            identifier = "p", defaultValue = "0.1")
    public double p;

    @LiteralInput(
            identifier = "etype", allowedValues = { "observed", "deaggregation", "stochastic", "expert" }, defaultValue="deaggregation")
    public String etype;

    @LiteralInput(
            identifier = "tlon", defaultValue = "-71.5730623712764")
    public double tlon;

    @LiteralInput(
            identifier = "tlat", defaultValue = "-33.1299174879672")
    public double tlat;

    private GenericFileDataWithGT selectedRows;

    private String outputFileName;

    private String workspacePath;

    public QuakeMLProcess() {
        // TODO Get from script
        outputFileName = "test.xml";

//        PythonAlgorithmRepositoryCM repositoryCM = (PythonAlgorithmRepositoryCM) WPSConfig.getInstance().getConfigurationModuleForClass(PythonAlgorithmRepository.class.getName(), ConfigurationCategory.REPOSITORY);

        workspacePath = getProperties().path("workspacepath").asText() + "quakeledger/";
    }

    @ComplexOutput(identifier = "selected-rows", binding = QuakeMLDataBinding.class)
    public GenericFileDataWithGT getResult() {
        return selectedRows;
    }

    @Execute
    public void runScript() {
        LOGGER.info("Executing python script.");

        try {

            Runtime rt = Runtime.getRuntime();

            String command = getCommand();

            Process proc = rt.exec(command, new String[]{}, new File(workspacePath));

            PipedOutputStream pipedOut = new PipedOutputStream();

            PipedInputStream pipedIn = new PipedInputStream(pipedOut);

            // attach error stream reader
            JavaProcessStreamReader errorStreamReader =
                    new JavaProcessStreamReader(proc.getErrorStream(), "ERROR", pipedOut);

            // attach output stream reader
            JavaProcessStreamReader outputStreamReader = new JavaProcessStreamReader(proc.getInputStream(), "OUTPUT");

            // start them
            errorStreamReader.start();
            outputStreamReader.start();

            // fetch errors if there are any
            String errors = "";
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(pipedIn));) {
                String line = errorReader.readLine();

                while (line != null) {
                    errors = errors.concat(line + lineSeparator);
                    line = errorReader.readLine();
                }
            }

            try {
                proc.waitFor();
            } catch (InterruptedException e1) {
                LOGGER.error("Java process was interrupted.", e1);
            } finally {
                proc.destroy();
            }

            LOGGER.info(errors);

        } catch (Exception e) {
            LOGGER.error("Exception occurred while trying to execute python script.", e);
        }

        File selectedRowCVSFile = new File(workspacePath + "/" + outputFileName);

        try {
            selectedRows = new GenericFileDataWithGT(selectedRowCVSFile, "text/xml");
        } catch (IOException e) {
            LOGGER.error("Could not create GenericFileData.", e);
        }
    }

    private String getCommand() {

        String pythonScriptName = "eventquery.py";

        return "python3 " + workspacePath + File.separatorChar + pythonScriptName + " " + lonmin + " " + lonmax + " " + latmin + " " + latmax + " " + mmin + " " + mmax
                + " " + zmin + " " + zmax + " " + p + " " + etype + " " + tlon + " " + tlat;
    }

//    private void setInputs(Map<String, List<Data<?>>> inputData) {
//        lonmin = getInput(inputData, "lonmin");
//        lonmax = getInput(inputData, "lonmax");
//        latmin = getInput(inputData, "latmin");
//        latmax = getInput(inputData, "latmax");
//        mmin = getInput(inputData, "mmin");
//        mmax = getInput(inputData, "mmax");
//        zmin = getInput(inputData, "zmin");
//        zmax = getInput(inputData, "zmax");
//        p = getInput(inputData, "p");
//        etype = getInputString(inputData, "etype");
//        tlon = getInput(inputData, "tlon");
//        tlat = getInput(inputData, "tlat");
//    }

//    private String getInputString(Map<String, List<Data<?>>> inputData,
//            String inputID) {
//        List<Data<?>> dataList = inputData.get(inputID);
//
//        Data<?> data = dataList.get(0);
//
//        if(data instanceof LiteralStringBinding){
//            return ((LiteralStringBinding)data).getPayload();
//        }
//
//        throw new IllegalArgumentException("Could not get input: " + inputID);
//    }
//
//    private Double getInput(Map<String, List<Data<?>>> inputData, String inputID){
//
//        List<Data<?>> dataList = inputData.get(inputID);
//
//        Data<?> data = dataList.get(0);
//
//        if(data instanceof LiteralDoubleBinding){
//            return ((LiteralDoubleBinding)data).getPayload();
//        }
//
//        throw new IllegalArgumentException("Could not get input: " + inputID);
//    }

}
