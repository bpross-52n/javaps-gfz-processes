/*
 * Copyright (C) 2007 - 2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *       • Apache License, version 2.0
 *       • Apache Software License, version 1.0
 *       • GNU Lesser General Public License, version 3
 *       • Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       • Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * As an exception to the terms of the GPL, you may copy, modify,
 * propagate, and distribute a work formed by combining 52°North WPS
 * GeoTools Modules with the Eclipse Libraries, or a work derivative of
 * such a combination, even if such copying, modification, propagation, or
 * distribution would otherwise violate the terms of the GPL. Nothing in
 * this exception exempts you from complying with the GPL in all respects
 * for all of the code used other than the Eclipse Libraries. You may
 * include this exception and its grant of permissions when you distribute
 * 52°North WPS GeoTools Modules. Inclusion of this notice with such a
 * distribution constitutes a grant of such permissions. If you do not wish
 * to grant these permissions, remove this paragraph from your
 * distribution. "52°North WPS GeoTools Modules" means the 52°North WPS
 * modules using GeoTools functionality - software licensed under version 2
 * or any later version of the GPL, or a work based on such software and
 * licensed under the GPL. "Eclipse Libraries" means Eclipse Modeling
 * Framework Project and XML Schema Definition software distributed by the
 * Eclipse Foundation and licensed under the Eclipse Public License Version
 * 1.0 ("EPL"), or a work based on such software and licensed under the EPL.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.wps.python.data.quakeml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.n52.javaps.annotation.Properties;
import org.n52.javaps.description.TypedProcessInputDescription;
import org.n52.javaps.gt.io.GTHelper;
import org.n52.javaps.gt.io.data.GenericFileDataWithGT;
import org.n52.javaps.gt.io.data.binding.complex.GTVectorDataBinding;
import org.n52.javaps.gt.io.datahandler.parser.GeoJSONParser;
import org.n52.javaps.io.AbstractPropertiesInputOutputHandler;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.DecodingException;
import org.n52.javaps.io.InputHandler;
import org.n52.shetland.ogc.wps.Format;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * This parser parses XML data in the USGS shakemap format
 *
 * @author Benjamin Pross
 *
 */
@Properties(defaultPropertyFileName = "quakeml.properties")
public class QuakeMLParser extends AbstractPropertiesInputOutputHandler implements InputHandler {

    private static Logger LOGGER = LoggerFactory
            .getLogger(QuakeMLParser.class);

    private GTHelper gtHelper;
    private QuakeMLGenerator quakeMLGenerator;
    private GeoJSONParser geoJSONParser;

    @Inject
    public void setGTHelper(GTHelper gtHelper){
        this.gtHelper = gtHelper;
    }

    @Inject
    public void setQuakeMLGenerator(QuakeMLGenerator quakeMLGenerator){
        this.quakeMLGenerator = quakeMLGenerator;
    }

    @Inject
    public void setGeoJSONParser(GeoJSONParser geoJSONParser){
        this.geoJSONParser = geoJSONParser;
    }

    public QuakeMLParser() {
        super();
        addSupportedBinding(QuakeMLDataBinding.class);
    }

    public Data<?> parse(TypedProcessInputDescription<?> description, InputStream input, Format format) throws IOException, DecodingException {

        String mimeType = format.getMimeType().get();

        if(mimeType.equals(QuakeMLGenerator.mimeTypeQuakeML)){
            GenericFileDataWithGT theData = new GenericFileDataWithGT(input, mimeType);

            return new QuakeMLDataBinding(theData);
        }

        Data<?> eventFeatures = geoJSONParser.parse(description, input, format);

        if(eventFeatures instanceof GTVectorDataBinding){

            try {

              InputStream in = quakeMLGenerator.generateQuakeML((SimpleFeatureCollection) ((GTVectorDataBinding)eventFeatures).getPayload());

              File quakeMLFile = File.createTempFile("quakeml", ".xml");

              FileOutputStream out = new FileOutputStream(quakeMLFile);

              IOUtils.copy(in, out);

              GenericFileDataWithGT theData = new GenericFileDataWithGT(quakeMLFile, mimeType);

              return new QuakeMLDataBinding(theData);

            } catch (IOException e) {
                // TODO Auto-generated catch block
                LOGGER.error("");
            }
        }

        return null;
    }

    public SimpleFeatureCollection parseQuakeMLToFeatureCollection(InputStream stream){

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(stream);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            Element eventParameters = null;
            if (root.getTagName().equals("eventParameters")) {
                LOGGER.info("parsing invalid QuakeML");
                eventParameters = root;
            } else if (root.getTagName().equals("q:quakeml")) {
                LOGGER.info("parsing valid QuakeML");
                NodeList nList = root.getElementsByTagName("eventParameters");
                Node nNode = nList.item(0);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    eventParameters = (Element) nNode;
                }
            } else {
                LOGGER.error("Could not parse input stream.");
                throw new Exception("Could not parse input stream.");
            }
            if (eventParameters == null) {
                LOGGER.error("Could not parse input stream.");
                throw new Exception("Could not parse input stream.");
            }

            NodeList events = eventParameters.getElementsByTagName("event");
            eventParameters.getAttributes();
            if (events == null) {
                LOGGER.error("Could not parse input stream.");
                throw new Exception("Could not parse input stream.");
            }
            SimpleFeatureType sft = createFeatureType();
            // iterate events:
            for (int i = 0; i < events.getLength(); i++) {
                Node event = events.item(i);
                SimpleFeature feature = getFeatureFromEvent(event, sft);
                setFeatureProperties(feature, event);
                if (feature != null) {
                    featureCollection.add(feature);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Could not parse input stream: " + ex);
        }
        return featureCollection;

    }

    private void setFeatureProperties(SimpleFeature feature, Node event) {
        Element eventElem = (Element) event;
        NodeList eventChilds = eventElem.getChildNodes();
        for (int i = 0; i < eventChilds.getLength(); i++) {
            Node child = eventChilds.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElem = (Element) child;
                switch (childElem.getTagName()) {
                    case "preferredOriginID":
                        feature.setAttribute("preferredOriginID", childElem.getTextContent());
                        break;
                    case "preferredMagnitudeID":
                        feature.setAttribute("preferredMagnitudeID", childElem.getTextContent());
                        break;
                    case "type":
                        feature.setAttribute("type", childElem.getTextContent());
                        break;
                    case "description":
                        String descrText = getChildNodeValueById(childElem, "text");
                        if (descrText != null) {
                            feature.setAttribute("description.text", descrText);
                        }
                        break;
                    case "origin":
                        String origPubID = childElem.getAttribute("publicID");
                        feature.setAttribute("origin.publicID", origPubID);
                        String origPubTime = getChildNodeValueById(childElem, "time.value");
                        if (origPubTime != null) {
                            feature.setAttribute("origin.time.value", origPubTime);
                        }
                        String origPubTimeU = getChildNodeValueById(childElem, "time.uncertainty");
                        if (origPubTimeU != null) {
                            feature.setAttribute("origin.time.uncertainty", origPubTimeU);
                        }
                        String origDepV = getChildNodeValueById(childElem, "depth.value");
                        if (origDepV != null) {
                            feature.setAttribute("origin.depth.value", origDepV);
                        }
                        String origDepU = getChildNodeValueById(childElem, "depth.uncertainty");
                        if (origDepU != null) {
                            feature.setAttribute("origin.depth.uncertainty", origDepU);
                        }
                        String origDepT = getChildNodeValueById(childElem, "depthType");
                        if (origDepT != null) {
                            feature.setAttribute("origin.depthType", origDepT);
                        }
                        String origDepTf = getChildNodeValueById(childElem, "timeFixed");
                        if (origDepTf != null) {
                            feature.setAttribute("origin.timeFixed", origDepTf);
                        }
                        String origef = getChildNodeValueById(childElem, "epicenterFixed");
                        if (origef != null) {
                            feature.setAttribute("origin.epicenterFixed", origef);
                        }
                        String origrsid = getChildNodeValueById(childElem, "referenceSystemID");
                        if (origrsid != null) {
                            feature.setAttribute("origin.referenceSystemID", origrsid);
                        }
                        String origT = getChildNodeValueById(childElem, "type");
                        if (origT != null) {
                            feature.setAttribute("origin.type", origT);
                        }
                        String creaInfoV = getChildNodeValueById(childElem, "creationInfo.value");
                        if (creaInfoV != null) {
                            feature.setAttribute("origin.creationInfo.value", creaInfoV);
                        }
                        String qualityAzi = getChildNodeValueById(childElem, "quality.azimuthalGap");
                        if (qualityAzi != null) {
                            feature.setAttribute("origin.quality.azimuthalGap", qualityAzi);
                        }
                        String minDis = getChildNodeValueById(childElem, "quality.minimumDistance");
                        if (minDis != null) {
                            feature.setAttribute("origin.quality.minimumDistance", minDis);
                        }
                        String maxDis = getChildNodeValueById(childElem, "quality.maximumDistance");
                        if (maxDis != null) {
                            feature.setAttribute("origin.quality.maximumDistance", maxDis);
                        }
                        String uPC = getChildNodeValueById(childElem, "quality.usedPhaseCount");
                        if (uPC != null) {
                            feature.setAttribute("origin.quality.usedPhaseCount", uPC);
                        }
                        String uSC = getChildNodeValueById(childElem, "quality.usedStationCount");
                        if (uSC != null) {
                            feature.setAttribute("origin.quality.usedStationCount", uSC);
                        }
                        String usE = getChildNodeValueById(childElem, "quality.standardError");
                        if (usE != null) {
                            feature.setAttribute("origin.quality.standardError", usE);
                        }
                        String eM = getChildNodeValueById(childElem, "evaluationMode");
                        if (eM != null) {
                            feature.setAttribute("origin.evaluationMode", eM);
                        }
                        String eS = getChildNodeValueById(childElem, "evaluationStatus");
                        if (eS != null) {
                            feature.setAttribute("origin.evaluationStatus", eS);
                        }
                        break;
                    case "originUncertainty":
                        String oUh = getChildNodeValueById(childElem, "horizontalUncertainty");
                        if (oUh != null) {
                            feature.setAttribute("originUncertainty.horizontalUncertainty", oUh);
                        }
                        String mhu = getChildNodeValueById(childElem, "minHorizontalUncertainty");
                        if (mhu != null) {
                            feature.setAttribute("originUncertainty.minHorizontalUncertainty", mhu);
                        }
                        String maxhu = getChildNodeValueById(childElem, "maxHorizontalUncertainty");
                        if (maxhu != null) {
                            feature.setAttribute("originUncertainty.maxHorizontalUncertainty", maxhu);
                        }
                        String amhu = getChildNodeValueById(childElem, "azimuthMaxHorizontalUncertainty");
                        if (amhu != null) {
                            feature.setAttribute("originUncertainty.azimuthMaxHorizontalUncertainty", amhu);
                        }
                        break;
                    case "magnitude":
                        String magPubID = childElem.getAttribute("publicID");
                        feature.setAttribute("magnitude.publicID", magPubID);
                        String mV = getChildNodeValueById(childElem, "mag.value");
                        if (mV != null) {
                            feature.setAttribute("magnitude.mag.value", mV);
                        }
                        String muC = getChildNodeValueById(childElem, "mag.uncertainty");
                        if (muC != null) {
                            feature.setAttribute("magnitude.mag.uncertainty", muC);
                        }
                        String mT = getChildNodeValueById(childElem, "type");
                        if (mT != null) {
                            feature.setAttribute("magnitude.type", mT);
                        }
                        String meS = getChildNodeValueById(childElem, "evaluationStatus");
                        if (meS != null) {
                            feature.setAttribute("magnitude.evaluationStatus", meS);
                        }
                        String moI = getChildNodeValueById(childElem, "originID");
                        if (moI != null) {
                            feature.setAttribute("magnitude.originID", moI);
                        }
                        String msC = getChildNodeValueById(childElem, "stationCount");
                        if (msC != null) {
                            feature.setAttribute("magnitude.stationCount", msC);
                        }
                        String mci = getChildNodeValueById(childElem, "creationInfo.value");
                        if (mci != null) {
                            feature.setAttribute("magnitude.creationInfo.value", mci);
                        }
                        break;
                    case "focalMechanism":
                        String focMechID = childElem.getAttribute("publicID");
                        feature.setAttribute("focalMechanism.publicID", focMechID);
                        String fMv = getChildNodeValueById(childElem, "nodalPlanes.nodalPlane1.strike.value");
                        if (fMv != null) {
                            feature.setAttribute("focalMechanism.nodalPlanes.nodalPlane1.strike.value", fMv);
                        }
                        String fMu = getChildNodeValueById(childElem, "nodalPlanes.nodalPlane1.strike.uncertainty");
                        if (fMu != null) {
                            feature.setAttribute("focalMechanism.nodalPlanes.nodalPlane1.strike.uncertainty", fMu);
                        }
                        String dipV = getChildNodeValueById(childElem, "nodalPlanes.nodalPlane1.dip.value");
                        if (dipV != null) {
                            feature.setAttribute("focalMechanism.nodalPlanes.nodalPlane1.dip.value", dipV);
                        }
                        String dipU = getChildNodeValueById(childElem, "nodalPlanes.nodalPlane1.dip.uncertainty");
                        if (dipU != null) {
                            feature.setAttribute("focalMechanism.nodalPlanes.nodalPlane1.dip.uncertainty", dipU);
                        }
                        String rakV = getChildNodeValueById(childElem, "nodalPlanes.nodalPlane1.rake.value");
                        if (rakV != null) {
                            feature.setAttribute("focalMechanism.nodalPlanes.nodalPlane1.rake.value", rakV);
                        }
                        String rakU = getChildNodeValueById(childElem, "nodalPlanes.nodalPlane1.rake.uncertainty");
                        if (rakU != null) {
                            feature.setAttribute("focalMechanism.nodalPlanes.nodalPlane1.rake.uncertainty", rakU);
                        }
                        String pP = getChildNodeValueById(childElem, "nodalPlanes.preferredPlane");
                        if (pP != null) {
                            feature.setAttribute("focalMechanism.nodalPlanes.preferredPlane", pP);
                        }
                        break;
                    case "amplitude":
                        String ampPubID = childElem.getAttribute("publicID");
                        feature.setAttribute("amplitude.publicID", ampPubID);
                        String ampType = getChildNodeValueById(childElem, "type");
                        if (ampType != null) {
                            feature.setAttribute("amplitude.type", ampType);
                        }
                        String ampV = getChildNodeValueById(childElem, "genericAmplitude.value");
                        if (ampV != null) {
                            feature.setAttribute("amplitude.genericAmplitude.value", ampV);
                        }
                        break;
                }
            }
        }
    }

    private String getChildNodeValueById(Element child, String id) {
        String[] nestedChilds = id.split("\\.");
        if (nestedChilds.length == 1) {
            NodeList nList = child.getElementsByTagName(nestedChilds[0]);
            if (nList.getLength() > 0) {
                Node nChild = nList.item(0);
                if (nChild.getNodeType() == Node.ELEMENT_NODE) {
                    Element eChild = (Element) nChild;
                    return eChild.getTextContent();
                }
            }
        } else {
            NodeList nList = child.getElementsByTagName(nestedChilds[0]);
            String restID = id.substring(id.indexOf(".")+1);
            if (nList.getLength() > 0) {
                Node nChild = nList.item(0);
                if (nChild.getNodeType() == Node.ELEMENT_NODE) {
                    Element eChild = (Element) nChild;
                    return getChildNodeValueById(
                            eChild, restID);
                }
            }
        }
        return null;
    }

    private Coordinate getCoordinate(Node event) {
        // get origin:
        Element eventElem = (Element) event;
        Element origin = (Element) eventElem.getElementsByTagName("origin").item(0);
        // get latitude:
        Element latitude = (Element) origin.getElementsByTagName("latitude").item(0);
        String lat = latitude.getElementsByTagName("value").item(0).getTextContent();
        // get longitude:
        Element longitude = (Element) origin.getElementsByTagName("longitude").item(0);
        String lng = longitude.getElementsByTagName("value").item(0).getTextContent();

        return new Coordinate(
                Double.parseDouble(lng),
                Double.parseDouble(lat)
        );
    }

    private SimpleFeature getFeatureFromEvent(Node event, SimpleFeatureType sft) {
        String id = ((Element) event).getAttribute("publicID");
        Point point = new GeometryFactory().createPoint(getCoordinate(event));
        SimpleFeature createdFeature = (SimpleFeature) gtHelper.createFeature(id, point, sft);
        return createdFeature;
//        return null;
    }

    private SimpleFeatureType createFeatureType() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        String id = UUID.randomUUID().toString().substring(0, 5);
        String namespace = "http://www.52north.org/" + id;
        Name name = new NameImpl(namespace, "Feature-" + id);
        builder.setName(name);
        builder.setCRS(DefaultGeographicCRS.WGS84);
        builder.add("the_geom", Point.class);
        builder.add("preferredOriginID", String.class);
        builder.add("preferredMagnitudeID", String.class);
        builder.add("type", String.class);
        builder.add("description.text", String.class);
        builder.add("origin.publicID", String.class);
        builder.add("origin.time.value", String.class);
        builder.add("origin.time.uncertainty", String.class);
        builder.add("origin.depth.value", String.class);
        builder.add("origin.depth.uncertainty", String.class);
        builder.add("origin.depthType", String.class);
        builder.add("origin.timeFixed", String.class);
        builder.add("origin.epicenterFixed", String.class);
        builder.add("origin.referenceSystemID", String.class);
        builder.add("origin.type", String.class);
        builder.add("origin.creationInfo.value", String.class);
        builder.add("origin.quality.azimuthalGap", String.class);
        builder.add("origin.quality.minimumDistance", String.class);
        builder.add("origin.quality.maximumDistance", String.class);
        builder.add("origin.quality.usedPhaseCount", String.class);
        builder.add("origin.quality.usedStationCount", String.class);
        builder.add("origin.quality.standardError", String.class);
        builder.add("origin.evaluationMode", String.class);
        builder.add("origin.evaluationStatus", String.class);
        builder.add("originUncertainty.horizontalUncertainty", String.class);
        builder.add("originUncertainty.minHorizontalUncertainty", String.class);
        builder.add("originUncertainty.maxHorizontalUncertainty", String.class);
        builder.add("originUncertainty.azimuthMaxHorizontalUncertainty", String.class);
        builder.add("magnitude.publicID", String.class);
        builder.add("magnitude.mag.value", String.class);
        builder.add("magnitude.mag.uncertainty", String.class);
        builder.add("magnitude.type", String.class);
        builder.add("magnitude.evaluationStatus", String.class);
        builder.add("magnitude.originID", String.class);
        builder.add("magnitude.stationCount", String.class);
        builder.add("magnitude.creationInfo.value", String.class);
        builder.add("focalMechanism.publicID", String.class);
        builder.add("focalMechanism.nodalPlanes.nodalPlane1.strike.value", String.class);
        builder.add("focalMechanism.nodalPlanes.nodalPlane1.strike.uncertainty", String.class);
        builder.add("focalMechanism.nodalPlanes.nodalPlane1.dip.value", String.class);
        builder.add("focalMechanism.nodalPlanes.nodalPlane1.dip.uncertainty", String.class);
        builder.add("focalMechanism.nodalPlanes.nodalPlane1.rake.value", String.class);
        builder.add("focalMechanism.nodalPlanes.nodalPlane1.rake.uncertainty", String.class);
        builder.add("focalMechanism.nodalPlanes.preferredPlane", String.class);
        builder.add("amplitude.publicID", String.class);
        builder.add("amplitude.type", String.class);
        builder.add("amplitude.genericAmplitude.value", String.class);
        return builder.buildFeatureType();
    }

}
