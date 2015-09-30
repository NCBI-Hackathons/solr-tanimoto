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

import java.io.IOException;

public class TestTokenizerInSolr extends TestCase {
    EmbeddedSolrServer server;
    CoreContainer container;

    protected void setUp() throws Exception {
        // If indexing a brand-new index, you might want to delete the data directory first
        // FileUtilities.deleteDirectory("testdata/solr/collection1/data");

        container = new CoreContainer("testdata/solr");
        container.load();

        server = new EmbeddedSolrServer(container, "collection1");
        super.setUp();
    }

    protected void tearDown() throws Exception {
        server.shutdown();
        super.tearDown();
    }

    public void testTokenizerInSolr() throws SolrServerException, IOException {
        ModifiableSolrParams params = new ModifiableSolrParams();

        // ** Let's index a document into our embedded server

        SolrInputDocument newDoc = new SolrInputDocument();
        newDoc.addField("title", "Test Document 1");
        newDoc.addField("id", 1);
        newDoc.addField("text", "Hello world!");
        server.add(newDoc);
        server.commit();

        // ** And now let's query for it

        params.set("q", "title:test");
        QueryResponse qResp = server.query(params);

        SolrDocumentList docList = qResp.getResults();

        assert (docList.getNumFound() == 1);
        SolrDocument doc = docList.get(0);
        assert (doc.getFirstValue("title").toString().length() > 5);
//
//        System.out.println("Num docs: " + docList.getNumFound());
//        System.out.println("Title: " + doc.getFirstValue("title").toString());
    }
}