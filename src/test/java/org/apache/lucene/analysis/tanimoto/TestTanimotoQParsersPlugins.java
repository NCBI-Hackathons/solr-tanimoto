package org.apache.lucene.analysis.tanimoto;

/**
 * Created by hanl.
 */

import junit.framework.TestCase;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TestTanimotoQParsersPlugins extends TestCase {
    private final static Logger logger = LoggerFactory.getLogger(TestTanimotoQParsersPlugins.class);
    EmbeddedSolrServer server;
    CoreContainer container;

    protected void loadTestingData() throws Exception {
        // ** Let's index a document into our embedded server

        String[] docs = new String[]{
                "star1 star2 star3 star4 star5 star6",
                "star1 ",
                "star1 star2",
                "star1 star2 star3",
                "star1 star2 star3 star4"};
        for (int i = 0; i < docs.length; i++) {
            String[] parts = docs[i].split(" ");
            SolrInputDocument newDoc = new SolrInputDocument();
            newDoc.addField("id", i + 1);
            //newDoc.addField("text", docs[i]);
            newDoc.addField("title", docs[i]);
            newDoc.addField("matchstring", docs[i]);
            newDoc.addField("matchstringLength", parts.length);
            server.add(newDoc);
        }
        server.commit();

    }

    protected void setUp() throws Exception {
        // If indexing a brand-new index, you might want to delete the data directory first
        // FileUtilities.deleteDirectory("testdata/solr/collection1/data");

        container = new CoreContainer("testdata/solr");
        container.load();

        server = new EmbeddedSolrServer(container, "collection1");
        super.setUp();
        this.loadTestingData();
    }

    protected void tearDown() throws Exception {
        server.shutdown();
        super.tearDown();
    }

    public void testRegularTextQuery() throws SolrServerException, IOException {
        ModifiableSolrParams params = new ModifiableSolrParams();

        // ** And now let's query for it

        params.set("q", "title:star1");
        QueryResponse qResp = server.query(params);
        SolrDocumentList docList = qResp.getResults();

        SolrDocument doc = docList.get(0);
//        System.out.println("Num docs: " + docList.getNumFound());
//        System.out.println("Title: " + doc.getFirstValue("title").toString());
        assert (docList.getNumFound() == 5);
        assert (doc.getFirstValue("title").toString().length() >= 5);

    }

    public void testOverlapScore() throws SolrServerException, IOException {
        ModifiableSolrParams params = new ModifiableSolrParams();

        // ** And now let's query for it

        params.set("q", "{!edismax mm=2 qf=matchstring}(star1 star2)");
        params.set("fl", "*,score");
        QueryResponse qResp = server.query(params);
        SolrDocumentList docList = qResp.getResults();
        for (SolrDocument doc : docList) {
            logger.info((String) doc.getFieldValue("title"));
            logger.info(doc.getFieldValue("score").toString());
            //all matching score should be 2
            assert (Float.parseFloat(doc.getFieldValue("score").toString()) == 2.0f);
        }
    }

    public void testOverlapScoreAndCutoff() throws SolrServerException, IOException {
        ModifiableSolrParams params = new ModifiableSolrParams();

        // ** And now let's query for it

        params.set("q", "{!edismax mm=1 qf=matchstring}(star1 star2 star3)");
        params.set("fl", "*,score");
        params.set("fq", "{!cutoff at=2}");
        QueryResponse qResp = server.query(params);
        SolrDocumentList docList = qResp.getResults();
        for (SolrDocument doc : docList) {
            logger.info((String) doc.getFieldValue("title"));
            logger.info(doc.getFieldValue("score").toString());
            //all matching score should be >= 2
            assert (Float.parseFloat(doc.getFieldValue("score").toString()) >= 2.0f);
        }

    }

    public void testTanimotoScoreAndCutoff() throws SolrServerException, IOException {
        ModifiableSolrParams params = new ModifiableSolrParams();

        // ** And now let's query for it

        params.set("q", "{!tanimoto bf=matchstringLength v=$qq}");
        params.set("qq", "{!edismax mm=2 qf=matchstring}(star1 star2 star3)");
        params.set("fl", "*,score");
        params.set("fq", "{!cutoff at=0.4}");
        QueryResponse qResp = server.query(params);
        SolrDocumentList docList = qResp.getResults();
        for (SolrDocument doc : docList) {
            logger.info((String) doc.getFieldValue("title"));
            logger.info(doc.getFieldValue("score").toString());
            //all matching score should be >= 0.4
            assert (Float.parseFloat(doc.getFieldValue("score").toString()) >= 0.4f);
            assert (Float.parseFloat(doc.getFieldValue("score").toString()) <= 1.0f);
        }
    }

    public void testTanimotoScoreAndCutoffDefaultMM() throws SolrServerException, IOException {
        ModifiableSolrParams params = new ModifiableSolrParams();

        // ** And now let's query for it
        params.set("q", "{!tanimoto bf=matchstringLength v=$qq}");
        params.set("qq", "{!edismax mm=60% qf=matchstring}(star1 star2 star3)");
        params.set("fl", "*,score");
        params.set("fq", "{!cutoff at=0.6}");
        QueryResponse qResp = server.query(params);
        SolrDocumentList docList = qResp.getResults();
        for (SolrDocument doc : docList) {
            logger.info((String) doc.getFieldValue("title"));
            logger.info(doc.getFieldValue("score").toString());
            //all matching score should be [0.6,1]
            assert (Float.parseFloat(doc.getFieldValue("score").toString()) >= 0.6f);
            assert (Float.parseFloat(doc.getFieldValue("score").toString()) <= 1.0f);
        }


    }


}