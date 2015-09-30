package org.apache.lucene.analysis.tanimoto;


import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.CustomScoreProvider;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by hanl.
 * <p/>
 * Setting \alpha = \_beta = 1  produces the Tanimoto coefficient; setting \alpha = \_beta = 0.5  produces Dice's coefficient.
 */
public class TverskyQuery extends CustomScoreQuery {

    protected int _queryTermsSize;
    protected String _scoreField;
    protected float _alpha;
    protected float _beta;


    public TverskyQuery(String scoreField, float alpha, float beta, Query subQuery, FunctionQuery scoringQuery) {
        super(subQuery, scoringQuery);
        _queryTermsSize = this.getTermsSize(subQuery);
        _scoreField = scoreField;
        _alpha = alpha;
        _beta = beta;
    }

    public TverskyQuery(String scoreField, float alpha, float beta, Query subQuery, FunctionQuery... scoringQueries) {
        super(subQuery, scoringQueries);
        _queryTermsSize = this.getTermsSize(subQuery);
        _scoreField = scoreField;
        _alpha = alpha;
        _beta = beta;
    }

    public TverskyQuery(SolrParams localParams, Query subQuery, FunctionQuery... scoringQueries) {
        super(subQuery, scoringQueries);
        _queryTermsSize = this.getTermsSize(subQuery);
        _scoreField = localParams.get("bf", "matchstringLength");//score field for size(B)
        _alpha = localParams.getFloat("alpha", 1.0f);//alpha field
        _beta = localParams.getFloat("beta", 1.0f);//beta field
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
        return "TverskyQuery";
    }

    class MyScoreProvider extends CustomScoreProvider {


        /**
         * MyScoreProvider
         *
         * @param context - context
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
            Document d = context.reader().document(doc);
            // plugin external score calculation based on the fields...
            float fieldValue = (float) d.getField(_scoreField).numericValue().longValue();

            // and return the custom score
            return subQueryScore / (subQueryScore + _alpha * (_queryTermsSize - subQueryScore) + _beta * (fieldValue - subQueryScore));

        }
    }
}
