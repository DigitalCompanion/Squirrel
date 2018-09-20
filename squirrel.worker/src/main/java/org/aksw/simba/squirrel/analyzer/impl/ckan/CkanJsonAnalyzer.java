package org.aksw.simba.squirrel.analyzer.impl.ckan;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;

import org.aksw.simba.squirrel.Constants;
import org.aksw.simba.squirrel.analyzer.Analyzer;
import org.aksw.simba.squirrel.collect.UriCollector;
import org.aksw.simba.squirrel.data.uri.CrawleableUri;
import org.aksw.simba.squirrel.fetcher.ckan.java.SimpleCkanFetcher;
import org.aksw.simba.squirrel.metadata.ActivityUtil;
import org.aksw.simba.squirrel.sink.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.trentorise.opendata.jackan.JackanModule;
import eu.trentorise.opendata.jackan.model.CkanDataset;

/**
 * This {@link Analyzer} implements the processing of JSON result objects
 * representing CKAN datasets.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class CkanJsonAnalyzer implements Analyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CkanJsonAnalyzer.class);

    private UriCollector collector;
    protected ObjectMapper mapper;

    public CkanJsonAnalyzer(UriCollector collector) {
        this.collector = collector;
        mapper = new ObjectMapper();
        mapper.registerModule(new JackanModule());
    }

    @Override
    public Iterator<byte[]> analyze(CrawleableUri curi, File data, Sink sink) {
        // Make sure that the file contains the CKAN JSON objects we are expecting
        if (SimpleCkanFetcher.CKAN_API_URI_TYPE_VALUE.equals(curi.getData(Constants.URI_HTTP_MIME_TYPE_KEY))) {
            Stream<String> lines = null;
            try {
                lines = Files.lines(data.toPath(), StandardCharsets.UTF_8);
                sink.openSinkForUri(curi);
                CkanDatasetConsumer consumer = new CkanDatasetConsumer(sink, collector, curi);
                lines.map(s -> parseDataset(s)).forEach(consumer);
                sink.closeSinkForUri(curi);
                ActivityUtil.addStep(curi, getClass());
                return collector.getUris(curi);
            } catch (IOException e) {
                LOGGER.error("Error while reading JSON data from file. Returning empty iterator.", e);
                ActivityUtil.addStep(curi, getClass(), e.getMessage());
                return Collections.emptyIterator();
            } finally {
                lines.close();
            }
        }
        return null;
    }

    public CkanDataset parseDataset(String line) {
        try {
            return mapper.readValue(line, CkanDataset.class);
        } catch (IOException e) {
            LOGGER.error("Error while parsing CKAN dataset. returning null.");
            return null;
        }
    }

}
