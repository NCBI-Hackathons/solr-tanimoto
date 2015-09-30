package org.apache.lucene.analysis.tanimoto;


import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.CustomScoreProvider;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by hanl.
 */
public class TanimotoQuery extends CustomScoreQuery {

    private final static Logger logger = LoggerFactory.getLogger(TanimotoQuery.class);
    protected int _queryTermsSize;
    protected String _scoreField;
    protected String _md5hash;


    public TanimotoQuery(String scoreField, Query subQuery, FunctionQuery scoringQuery) {
        super(subQuery, scoringQuery);
        _queryTermsSize = this.getTermsSize(subQuery);
        _scoreField = scoreField;
    }

    public TanimotoQuery(String scoreField, Query subQuery, FunctionQuery... scoringQueries) {
        super(subQuery, scoringQueries);
        _queryTermsSize = this.getTermsSize(subQuery);
        _scoreField = scoreField;
    }

    public TanimotoQuery(SolrParams localParams, Query subQuery, FunctionQuery... scoringQueries) {
        super(subQuery, scoringQueries);
        _queryTermsSize = this.getTermsSize(subQuery);
        _scoreField = localParams.get("bf", "matchstringLength");//score field for size(B)
        _md5hash = localParams.get("hf", "md5");//hash field
//        System.out.println(_queryTermsSize);
//        System.out.println(_scoreField);
//        System.out.println(subQuery.toString());
    }

    private int getTermsSize(Query subQuery) {
        Set<Term> terms = new HashSet<Term>();//ignore duplicates
        subQuery.extractTerms(terms);
        return terms.size();
    }

    @Override
    protected CustomScoreProvider getCustomScoreProvider(AtomicReaderContext context) throws IOException {
        return new MyScoreProvider(context);
    }

    @Override
    public String name() {
        return "TanimotoQuery";
    }

    class MyScoreProvider extends CustomScoreProvider {


        /**
         * MyScoreProvider
         *
         * @param context
         */
        public MyScoreProvider(AtomicReaderContext context) {
            super(context);

        }

        @Override
        public float customScore(int doc, float subQueryScore,
                                 float valSrcScore) throws IOException {
            return customScore(doc, subQueryScore, new float[]{valSrcScore});
        }

        @Override
        public float customScore(int doc, float subQueryScore,
                                 float[] valSrcScores) throws IOException {
            // Method is called for every
            // matching document of the subQuery

            //context.reader().getNumericDocValues(_scoreField).get(doc);
            // plugin external score calculation based on the fields...
            float fieldValue;
            // and return the custom score

            if (context.reader().getNumericDocValues(_scoreField) != null) {
                fieldValue = (float) context.reader().getNumericDocValues(_scoreField).get(doc);
                logger.info("NumericDocValues:" + context.reader().getNumericDocValues(_scoreField).get(doc));
            } else if (context.reader().document(doc).getField(_scoreField) != null) {
                Document d = context.reader().document(doc);
                fieldValue = (float) d.getField(_scoreField).numericValue().longValue();
            } else {
                fieldValue = 0;
                logger.error("not able to read the scoreField by field value or docValues!");
            }
            float score = subQueryScore / (fieldValue + _queryTermsSize - subQueryScore);
            return score;
        }


        private float getTermsVectorCount(int doc) throws IOException {
            IndexReader r = context.reader();
            Terms tv = r.getTermVector(doc, _scoreField);
            TermsEnum termsEnum = null;
            termsEnum = tv.iterator(termsEnum);
            int numTerms = 0;
            while ((termsEnum.next()) != null) {
                numTerms++;
            }
            return (float) (numTerms);
        }

        //@Override

        /**
         * getting the total number of terms for the field
         */
        public float customScore1(int doc, float subQueryScore,
                                  float valSrcScores[]) throws IOException {


            float score = subQueryScore;


            score = this.getTermsVectorCount(doc);

            float[] arr$ = valSrcScores;
            int len$ = valSrcScores.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                float valSrcScore = arr$[i$];
                score *= valSrcScore;
            }
            return score;
        }
    }
}
