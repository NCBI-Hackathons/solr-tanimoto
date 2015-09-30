package org.apache.lucene.analysis.tanimoto;

/**
 * Created by hanl.
 * <queryparser class="org.apache.lucene.analysis.tanimoto.tanimotoCutoffPostFilterPlugin" name="cutoff" />
 * {!cutoff threshold=0.5}
 *
 */


import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class tanimotoCutoffPostFilterPlugin extends QParserPlugin {

    private final static Logger logger = LoggerFactory.getLogger(tanimotoCutoffPostFilterPlugin.class);

    public void init(NamedList namedList) {
    }

    public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest request) {
        return new ExportQParser(qstr, localParams, params, request);
    }

    public class ExportQParser extends QParser {

        public ExportQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest request) {
            super(qstr, localParams, params, request);

        }

        public Query parse() throws SyntaxError {
            try {
                return new TanimotoPostQuery(localParams, params, req);
            } catch (Exception e) {
                throw new SyntaxError(e.getMessage(), e);
            }
        }
    }

    public class TanimotoPostQuery extends ExtendedQueryBase implements PostFilter {
        private float _threshold;

        public boolean getCache() {
            return false;
        }

        public int hashCode() {
            return -1;
        }

        public boolean equals(Object o) {
            return false;
        }

        public String toString(String s) {
            return s;
        }

        public TanimotoPostQuery(SolrParams localParams, SolrParams params, SolrQueryRequest request) throws IOException {
            setCost(localParams.getInt("cost", 200));
            _threshold = localParams.getFloat("at", 0.0f);
            //System.out.println(_threshold);
        }

        public DelegatingCollector getFilterCollector(IndexSearcher indexSearcher) {
            return new TanimotoCollector(_threshold);
        }
    }
}