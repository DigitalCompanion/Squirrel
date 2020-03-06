package org.dice_research.squirrel.analyzer.impl.data_portals.utils;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.dice_research.squirrel.analyzer.impl.html.scraper.HtmlScraper;
import org.dice_research.squirrel.data.uri.CrawleableUri;
import org.hobbit.utils.test.ModelComparisonHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstract test class that loads two RDF model and compares the actual result
 * with the expected result of a given URI.
 * An actual model contains the triples generated by the HTML scraper
 * and an expected model contains the expected triples.
 *
 */

public abstract class AbstractDataPortalTest {

    private CrawleableUri uriToCrawl;
    private File fileToScrape;
    /**
     * Name of the resource from which the expected result is loaded.
     */
    private ModelCom expectedModel;
    private HtmlScraper htmlScraper;

    @Before
    public void prepareGeneral() {
        File configurationFile = new File("src/test/resources/html_scraper_analyzer/yaml");
        this.htmlScraper = new HtmlScraper(configurationFile);
    }
    /**
     * Constructor is executed by parametrized test case of the given URI to create the expected model
     */
    public AbstractDataPortalTest(CrawleableUri uri, File fileToScrape, ModelCom expectedModel) {
        super();
        this.uriToCrawl = uri;
        this.fileToScrape = fileToScrape;
        this.expectedModel = expectedModel;
    }
    /**
     * Compares the actual model which contains the triples generated by the HTML scraper .
     */
    @Test
    public void test() throws Exception{
        //Scrapes the given URI and generates a list of triples
        List<Triple> listTriples = new ArrayList<Triple>();
        listTriples.addAll(htmlScraper.scrape(uriToCrawl.getUri().toString(), fileToScrape));
        // Load an actual model with the list of triples generated
        ModelCom actualModel = (ModelCom) ModelFactory.createDefaultModel();
        for (Triple curTriple: listTriples){
            Statement tempStmt = StatementImpl.toStatement(curTriple, actualModel);
            actualModel.add(tempStmt);
        }
        // Compare models
        // Check the precision and recall
        Set<Statement> missingStatements = ModelComparisonHelper.getMissingStatements(expectedModel, actualModel);
        Set<Statement> unexpectedStatements = ModelComparisonHelper.getMissingStatements(actualModel, expectedModel);

        StringBuilder builder = new StringBuilder();
        if (missingStatements.size() != 0) {
            builder.append("The result does not contain the expected statements:\n\n"
                + missingStatements.stream().map(Object::toString).collect(Collectors.joining("\n"))
                + "\n\nExpected model:\n\n" + expectedModel.toString() + "\n\nResult model:\n\n" + actualModel.toString()
                + "\n");
        }

        if (unexpectedStatements.size() != 0) {
            builder.append("The result contains the unexpected statements:\n\n"
                + unexpectedStatements.stream().map(Object::toString).collect(Collectors.joining("\n"))
                + "\n\nExpected model:\n\n" + expectedModel.toString() + "\nResult model:\n\n" + actualModel.toString()
                + "\n");
        }

        Assert.assertTrue(builder.toString(), missingStatements.size() == 0 &&
            unexpectedStatements.size() == 0);
    }
}