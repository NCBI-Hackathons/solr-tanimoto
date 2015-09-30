package org.apache.lucene.analysis.tanimoto;

/**
 * Created by hanl.
 *
 * test case adopted from https://github.com/o19s/lucene-query-example
 */

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class TestOverlapSimilarity extends LuceneTestCase {

    IndexSearcher searcherUnderTest;
    RandomIndexWriter indexWriterUnderTest;
    IndexReader indexReaderUnderTest;
    Directory dirUnderTest;
    Similarity similarity;

    Field newFieldAllOn(String name, String value) {
        FieldType tagsFieldType = new FieldType();
        tagsFieldType.setStored(true);
        tagsFieldType.setIndexed(true);
        tagsFieldType.setOmitNorms(true);
        tagsFieldType.setStoreTermVectors(true);
        tagsFieldType.setStoreTermVectorPositions(true);
        tagsFieldType.setStoreTermVectorPayloads(true);
        return new Field(name, value, tagsFieldType);
    }

    TermQuery newTermQuery(String field, String search) {
        Term t = new Term(field, search);
        return new TermQuery(t);
    }

    @Before
    public void setupIndex() throws IOException {
        dirUnderTest = newDirectory();
        similarity = new OverlapSimilarity();

        IndexWriterConfig iwConf = new IndexWriterConfig(
                Version.LUCENE_44, new WhitespaceAnalyzer(
                Version.LUCENE_44));
        iwConf.setSimilarity(similarity);
        indexWriterUnderTest = new RandomIndexWriter(random(), dirUnderTest, iwConf);

        String[] docs = new String[]{"1 2 10 11 19",
                "1 2 10 11 19 20 284 285 287 309 333 340 342 345 347 353 367 375 407 568 572 618 664",
                "1 2 10 11 66",
                "a b c d",
                "c d",
                "a b d",
                "a c d"};
        for (int i = 0; i < docs.length; i++) {
            Document doc = new Document();
            String idStr = Integer.toString(i);
            doc.add(newFieldAllOn("id", idStr));
            doc.add(newFieldAllOn("tag", docs[i]));
            indexWriterUnderTest.addDocument(doc);
        }
        indexWriterUnderTest.commit();
        indexReaderUnderTest = indexWriterUnderTest.getReader();
        searcherUnderTest = newSearcher(indexReaderUnderTest);
        searcherUnderTest.setSimilarity(similarity);
    }

    @After
    public void closeStuff() throws IOException {
        indexReaderUnderTest.close();
        indexWriterUnderTest.close();
        dirUnderTest.close();
    }

    @Test
    public void testOverlapScoring() throws IOException {


        BooleanQuery q = new BooleanQuery();

        q.add(newTermQuery("tag", "1"), BooleanClause.Occur.SHOULD);
        q.add(newTermQuery("tag", "2"), BooleanClause.Occur.SHOULD);
        q.add(newTermQuery("tag", "10"), BooleanClause.Occur.SHOULD);
        q.add(newTermQuery("tag", "11"), BooleanClause.Occur.SHOULD);
        q.add(newTermQuery("tag", "19"), BooleanClause.Occur.SHOULD);


        q.setMinimumNumberShouldMatch(1);


        TopDocs topDocs = searcherUnderTest.search(q, 10);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        assert (topDocs.totalHits == 3);
        assert (scoreDocs[0].score == 5.0);
        assert (scoreDocs[1].score == 5.0);
        assert (scoreDocs[2].score == 4.0);

//        for (int i = 0; i < scoreDocs.length; ++i) {
//            int docId = scoreDocs[i].doc;
//            float score = scoreDocs[i].score;
//            System.out.println(indexReaderUnderTest.document(docId).get("tag") + " -> " + score);
//            System.out.println(searcherUnderTest.explain(q, docId));
//        }
    }
}