package org.n52.wps.python.data.quakeml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.n52.javaps.annotation.Properties;
import org.n52.javaps.description.TypedProcessOutputDescription;
import org.n52.javaps.gt.io.data.binding.complex.GTVectorDataBinding;
import org.n52.javaps.gt.io.datahandler.generator.GeoJSONGenerator;
import org.n52.javaps.io.AbstractPropertiesInputOutputHandler;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.EncodingException;
import org.n52.javaps.io.OutputHandler;
import org.n52.shetland.ogc.wps.Format;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

@Properties(defaultPropertyFileName = "quakeml.properties")
public class QuakeMLGenerator extends AbstractPropertiesInputOutputHandler implements OutputHandler {

    private static Logger LOGGER = LoggerFactory
            .getLogger(QuakeMLGenerator.class);

    public static String mimeTypeQuakeML = "text/xml";
    public static String mimeTypeGeoJSON = "application/vnd.geo+json";

    private QuakeMLParser quakeMLParser;

    private GeoJSONGenerator geoJSONGenerator;

    @Inject
    public void setQuakeMLParser(QuakeMLParser quakeMLParser){
        this.quakeMLParser = quakeMLParser;
    }

    @Inject
    public void setGeoJSONGenerator(GeoJSONGenerator geoJSONGenerator){
        this.geoJSONGenerator = geoJSONGenerator;
    }

    public QuakeMLGenerator() {
        super();
        addSupportedBinding(QuakeMLDataBinding.class);
    }

    @Override
    public InputStream generate(TypedProcessOutputDescription<?> description,
            Data<?> data,
            Format format) throws IOException, EncodingException {

        String mimeType = format.getMimeType().get();

        if(mimeType.equals(QuakeMLGenerator.mimeTypeGeoJSON)){

            SimpleFeatureCollection featureCollection = quakeMLParser.parseQuakeMLToFeatureCollection(((QuakeMLDataBinding)data).getPayload().getDataStream());

            return geoJSONGenerator.generate(description, new GTVectorDataBinding(featureCollection), format);

        } else {
            return ((QuakeMLDataBinding)data).getPayload().getDataStream();
        }
    }

    public InputStream generateQuakeML(SimpleFeatureCollection featureCollection){
        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder;

            docBuilder = docFactory.newDocumentBuilder();

            // eventParameters
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("eventParameters");
            Attr namespace = doc.createAttribute("namespace");
            namespace.setValue("http://quakeml.org/xmlns/quakeml/1.2");
            rootElement.setAttributeNode(namespace);

            // events
            SimpleFeatureIterator features = featureCollection.features();
            while (features.hasNext()) {
                Feature feature = features.next();
                Element event = doc.createElement("event");
                Attr publicID = doc.createAttribute("publicID");
                publicID.setValue(feature.getIdentifier().getID());
                event.setAttributeNode(publicID);

                Collection<Property> eventProperties = feature.getProperties();
                for (Property prop : eventProperties) {
                    if (prop.getValue() == null) {
                        LOGGER.debug(prop.toString());
                    } else {
                        switch (prop.getName().toString()) {
                            case "the_geom":
                                createLatLon((Point) prop.getValue(), doc, event);
                                break;
                            case "geometry":
                                createLatLon((Point) prop.getValue(), doc, event);
                                break;
                            case "preferredOriginID":
                                Element preferredOriginID = doc.createElement("preferredOriginID");
                                preferredOriginID.setTextContent(prop.getValue().toString());
                                event.appendChild(preferredOriginID);
                                break;
                            case "preferredMagnitudeID":
                                Element preferredMagnitudeID = doc.createElement("preferredMagnitudeID");
                                preferredMagnitudeID.setTextContent(prop.getValue().toString());
                                event.appendChild(preferredMagnitudeID);
                                break;
                            case "type":
                                Element type = doc.createElement("type");
                                type.setTextContent(prop.getValue().toString());
                                event.appendChild(type);
                                break;
                            case "description.text":
                                createElement(doc, event, "description.text", prop.getValue().toString());
                                break;
                            case "origin.publicID":
                                createAttribut(doc, event, "origin.publicID", prop.getValue().toString());
                                break;
                            case "origin.time.value":
                                createElement(doc, event, "origin.time.value", prop.getValue().toString());
                                break;
                            case "origin.time.uncertainty":
                                createElement(doc, event, "origin.time.uncertainty", prop.getValue().toString());
                                break;
                            case "origin.depth.value":
                                createElement(doc, event, "origin.depth.value", prop.getValue().toString());
                                break;
                            case "origin.depth.uncertainty":
                                createElement(doc, event, "origin.depth.uncertainty", prop.getValue().toString());
                                break;
                            case "origin.depthType":
                                createElement(doc, event, "origin.depthType", prop.getValue().toString());
                                break;
                            case "origin.timeFixed":
                                createElement(doc, event, "origin.timeFixed", prop.getValue().toString());
                                break;
                            case "origin.epicenterFixed":
                                createElement(doc, event, "origin.epicenterFixed", prop.getValue().toString());
                                break;
                            case "origin.referenceSystemID":
                                createElement(doc, event, "origin.referenceSystemID", prop.getValue().toString());
                                break;
                            case "origin.type":
                                createElement(doc, event, "origin.type", prop.getValue().toString());
                                break;
                            case "origin.creationInfo.value":
                                createElement(doc, event, "origin.creationInfo.value", prop.getValue().toString());
                                break;
                            case "origin.quality.azimuthalGap":
                                createElement(doc, event, "origin.quality.azimuthalGap", prop.getValue().toString());
                                break;
                            case "origin.quality.minimumDistance":
                                createElement(doc, event, "origin.quality.minimumDistance", prop.getValue().toString());
                                break;
                            case "origin.quality.maximumDistance":
                                createElement(doc, event, "origin.quality.maximumDistance", prop.getValue().toString());
                                break;
                            case "origin.quality.usedPhaseCount":
                                createElement(doc, event, "origin.quality.usedPhaseCount", prop.getValue().toString());
                                break;
                            case "origin.quality.usedStationCount":
                                createElement(doc, event, "origin.quality.usedStationCount", prop.getValue().toString());
                                break;
                            case "origin.quality.standardError":
                                createElement(doc, event, "origin.quality.standardError", prop.getValue().toString());
                                break;
                            case "origin.evaluationMode":
                                createElement(doc, event, "origin.evaluationMode", prop.getValue().toString());
                                break;
                            case "origin.evaluationStatus":
                                createElement(doc, event, "origin.evaluationStatus", prop.getValue().toString());
                                break;
                            case "originUncertainty.horizontalUncertainty":
                                createElement(doc, event, "originUncertainty.horizontalUncertainty", prop.getValue().toString());
                                break;
                            case "originUncertainty.minHorizontalUncertainty":
                                createElement(doc, event, "originUncertainty.minHorizontalUncertainty", prop.getValue().toString());
                                break;
                            case "originUncertainty.maxHorizontalUncertainty":
                                createElement(doc, event, "originUncertainty.maxHorizontalUncertainty", prop.getValue().toString());
                                break;
                            case "originUncertainty.azimuthMaxHorizontalUncertainty":
                                createElement(doc, event, "originUncertainty.azimuthMaxHorizontalUncertainty", prop.getValue().toString());
                                break;
                            case "magnitude.publicID":
                                createAttribut(doc, event, "magnitude.publicID", prop.getValue().toString());
                                break;
                            case "magnitude.mag.value":
                                createElement(doc, event, "magnitude.mag.value", prop.getValue().toString());
                                break;
                            case "magnitude.mag.uncertainty":
                                createElement(doc, event, "magnitude.mag.uncertainty", prop.getValue().toString());
                                break;
                            case "magnitude.type":
                                createElement(doc, event, "magnitude.type", prop.getValue().toString());
                                break;
                            case "magnitude.evaluationStatus":
                                createElement(doc, event, "magnitude.evaluationStatus", prop.getValue().toString());
                                break;
                            case "magnitude.originID":
                                createElement(doc, event, "magnitude.originID", prop.getValue().toString());
                                break;
                            case "magnitude.stationCount":
                                createElement(doc, event, "magnitude.stationCount", prop.getValue().toString());
                                break;
                            case "magnitude.creationInfo.value":
                                createElement(doc, event, "magnitude.creationInfo.value", prop.getValue().toString());
                                break;
                            case "focalMechanism.publicID":
                                createAttribut(doc, event, "focalMechanism.publicID", prop.getValue().toString());
                                break;
                            case "focalMechanism.nodalPlanes.nodalPlane1.strike.value":
                                createElement(doc, event, "focalMechanism.nodalPlanes.nodalPlane1.strike.value", prop.getValue().toString());
                                break;
                            case "focalMechanism.nodalPlanes.nodalPlane1.strike.uncertainty":
                                createElement(doc, event, "focalMechanism.nodalPlanes.nodalPlane1.strike.uncertainty", prop.getValue().toString());
                                break;
                            case "focalMechanism.nodalPlanes.nodalPlane1.dip.value":
                                createElement(doc, event, "focalMechanism.nodalPlanes.nodalPlane1.dip.value", prop.getValue().toString());
                                break;
                            case "focalMechanism.nodalPlanes.nodalPlane1.dip.uncertainty":
                                createElement(doc, event, "focalMechanism.nodalPlanes.nodalPlane1.dip.uncertainty", prop.getValue().toString());
                                break;
                            case "focalMechanism.nodalPlanes.nodalPlane1.rake.value":
                                createElement(doc, event, "focalMechanism.nodalPlanes.nodalPlane1.rake.value", prop.getValue().toString());
                                break;
                            case "focalMechanism.nodalPlanes.nodalPlane1.rake.uncertainty":
                                createElement(doc, event, "focalMechanism.nodalPlanes.nodalPlane1.rake.uncertainty", prop.getValue().toString());
                                break;
                            case "focalMechanism.nodalPlanes.preferredPlane":
                                createElement(doc, event, "focalMechanism.nodalPlanes.preferredPlane", prop.getValue().toString());
                                break;
                            case "amplitude.publicID":
                                createAttribut(doc, event, "amplitude.publicID", prop.getValue().toString());
                                break;
                            case "amplitude.type":
                                createElement(doc, event, "amplitude.type", prop.getValue().toString());
                                break;
                            case "amplitude.genericAmplitude.value":
                                createElement(doc, event, "amplitude.genericAmplitude.value", prop.getValue().toString());
                                break;
                        }
                    }
                }

                rootElement.appendChild(event);
            }

            doc.appendChild(rootElement);

            // generate InputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Source xmlSource = new DOMSource(doc);
            Result outputTarget = new StreamResult(outputStream);
            TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
            return new ByteArrayInputStream(outputStream.toByteArray());
        } catch (Exception e) {
            LOGGER.error("Could not generate Inputstream. Returning null. " + e);
            return null;
        }
    }

    private void createLatLon(Point p, Document doc, Element event) {
        Coordinate c = p.getCoordinate();
        // latitude:
        createElement(doc, event, "origin.latitude.value", c.y + "");
        // longitude:
        createElement(doc, event, "origin.longitude.value", c.x + "");
    }

    private void createAttribut(Document doc, Element event, String id, String value) {
        if (value == null) {
            return;
        }
        String[] path = id.split("\\.");
        if (path.length == 1) {
            Attr attributeKey = doc.createAttribute(path[0]);
            attributeKey.setValue(value);
            event.setAttributeNode(attributeKey);
        } else {
            NodeList nList = event.getElementsByTagName(path[0]);
            String restID = id.substring(id.indexOf(".") + 1);
            if (nList.getLength() > 0) {
                Element elem = (Element) nList.item(0);
                createAttribut(doc, elem, restID, value);
            } else {
                Element newElement = doc.createElement(path[0]);
                event.appendChild(newElement);
                createAttribut(doc, newElement, restID, value);
            }
        }
    }

    private void createElement(Document doc, Element event, String id, String value) {
        if (value == null) {
            return;
        }
        String[] path = id.split("\\.");
        if (path.length == 1) {
            Element newElement = doc.createElement(id);
            newElement.setTextContent(value);
            event.appendChild(newElement);
        } else {
            NodeList nList = event.getElementsByTagName(path[0]);
            String restID = id.substring(id.indexOf(".") + 1);
            if (nList.getLength() > 0) {
                Element elem = (Element) nList.item(0);
                createElement(doc, elem, restID, value);
            } else {
                Element newElement = doc.createElement(path[0]);
                event.appendChild(newElement);
                createElement(doc, newElement, restID, value);
            }
        }
    }

}
