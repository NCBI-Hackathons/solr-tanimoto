package org.apache.lucene.analysis.tanimoto;

/**
 * Created by hanl.
 *
 * test case adopted from https://github.com/o19s/lucene-query-example
 */

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class TestTanimotoQuery extends LuceneTestCase {

    private final static Logger logger = LoggerFactory.getLogger(TestTanimotoQuery.class);
    IndexSearcher searcherUnderTest;
    RandomIndexWriter indexWriterUnderTest;
    IndexReader indexReaderUnderTest;
    Directory dirUnderTest;

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

    Field newFieldLengthAllOn(String name, int value) {
        FieldType tagsLengthFieldType = new FieldType();
        tagsLengthFieldType.setStored(true);
        tagsLengthFieldType.setIndexed(true);
        tagsLengthFieldType.setOmitNorms(true);
        tagsLengthFieldType.setStoreTermVectors(true);
        tagsLengthFieldType.setStoreTermVectorPositions(true);
        tagsLengthFieldType.setStoreTermVectorPayloads(true);
        tagsLengthFieldType.setNumericType(FieldType.NumericType.INT);
        return new IntField(name, value, tagsLengthFieldType);
    }

    TermQuery newTermQuery(String field, String search) {
        Term t = new Term(field, search);
        return new TermQuery(t);
    }

    @Before
    public void setupIndex() throws IOException {
        dirUnderTest = newDirectory();

        indexWriterUnderTest = new RandomIndexWriter(random(), dirUnderTest);
        String[] docs = new String[]{"star1 star2 star3 star4 star5 star6",
                "star1 ",
                "star1 star2",
                "star1 star2 star3",
                "star1 star2 star3 star4"};
        for (int i = 0; i < docs.length; i++) {
            Document doc = new Document();
            String idStr = Integer.toString(i);
            String[] parts = docs[i].split(" ");
            doc.add(newFieldAllOn("id", idStr));
            doc.add(newFieldAllOn("tag", docs[i]));
            doc.add(newFieldLengthAllOn("tagLength", parts.length));
            doc.add(new NumericDocValuesField("tagLengthDocValues", parts.length));
            indexWriterUnderTest.addDocument(doc);
        }
        indexWriterUnderTest.commit();

        indexReaderUnderTest = indexWriterUnderTest.getReader();
        searcherUnderTest = newSearcher(indexReaderUnderTest);
        Similarity similarity = new OverlapSimilarity();
        searcherUnderTest.setSimilarity(similarity);

    }

    @After
    public void closeStuff() throws IOException {
        indexReaderUnderTest.close();
        indexWriterUnderTest.close();
        dirUnderTest.close();
    }

    @Test
    public void testTanimotoScoring() throws IOException {
        //TermQuery tq = newTermQuery("tag", "star1 start-2");

        BooleanQuery q = new BooleanQuery();
        q.add(newTermQuery("tag", "star1"), BooleanClause.Occur.SHOULD);
        q.add(newTermQuery("tag", "star2"), BooleanClause.Occur.SHOULD);
        q.add(newTermQuery("tag", "star3"), BooleanClause.Occur.SHOULD);
        q.setMinimumNumberShouldMatch(2);


        TanimotoQuery ct = new TanimotoQuery("tagLength", q);


        TopDocs td = searcherUnderTest.search(ct, 10);
        ScoreDoc[] scoreDocs = td.scoreDocs;
        assert (td.totalHits == 4);
        assert (scoreDocs[0].score == 1.0);
        assert (scoreDocs[1].score == 0.75);
        assert (scoreDocs[2].score > 0.66);
        logger.info(td.totalHits + " -> totalhits tanimoto hits without customized collector ");

        TanimotoQuery ct2 = new TanimotoQuery("tagLength", q);
        TopScoreDocCollector topScore = TopScoreDocCollector.create(100, true);
        Collector collector = new TanimotoCollector(topScore, 0.7f);
        searcherUnderTest.search(ct, null, collector);
        TopDocs hitDocs = topScore.topDocs();
        logger.info(hitDocs.totalHits + " -> totalhits tanimoto hits without customized collector");
        ScoreDoc[] scoreDocs2 = hitDocs.scoreDocs;

        assert (hitDocs.totalHits == 2);
        assert (scoreDocs2[0].score == 1.0);
        assert (scoreDocs2[1].score == 0.75);


        TanimotoQuery ct3 = new TanimotoQuery("tagLengthDocValues", q);
        TopScoreDocCollector topScore3 = TopScoreDocCollector.create(100, true);
        Collector collector3 = new TanimotoCollector(topScore3, 0.7f);
        searcherUnderTest.search(ct3, null, collector3);
        TopDocs hitDocs3 = topScore3.topDocs();
        logger.warn(hitDocs3.totalHits + " -> totalhits tanimoto hits without customized collector");
        ScoreDoc[] scoreDocs3 = hitDocs3.scoreDocs;
        assert (hitDocs3.totalHits == 2);
        assert (scoreDocs3[0].score == 1.0);
        assert (scoreDocs3[1].score == 0.75);
//
//        for (int i = 0; i < scoreDocs3.length; ++i) {
//            int docId = scoreDocs3[i].doc;
//            float score = scoreDocs3[i].score;
//            System.out.println(String.valueOf(docId) + ":" + indexReaderUnderTest.document(docId).get("tag") + " -> " + score);
//            System.out.println(String.valueOf(docId) + ":" + searcherUnderTest.explain(ct, docId));
//        }
    }


}