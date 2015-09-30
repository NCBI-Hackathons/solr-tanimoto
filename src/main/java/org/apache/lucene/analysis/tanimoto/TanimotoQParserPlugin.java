package org.apache.lucene.analysis.tanimoto;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by hanl.
 *
 *
 * Implementation of Tanimoto Query Plugin
 *
 * <p>Usage:</p>
 *    <queryparser class="org.apache.lucene.analysis.tanimoto.TanimotoQParserPlugin" name="tanimoto" />
 * <p/>
 *
 * <p>
 *     {!tanimoto bf="matstringlength"  } 1 2 3 4
 * </p>
 *
 * <p>Suggested Reading:</p>
 * <p>
 *     1) http://en.wikipedia.org/wiki/Jaccard_index
 *     2) http://www.daylight.com/dayhtml/doc/theory/theory.finger.html
 * </p>
 */

import org.apache.lucene.search.Query;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SyntaxError;

import java.util.Iterator;


public class TanimotoQParserPlugin extends QParserPlugin {

    public void init(NamedList args) {
        SolrParams params = SolrParams.toSolrParams(args);
        // handle configuration parameters
        // passed through solrconfig.xml
    }

    @Override
    public QParser createParser(String queryString,
                                SolrParams localParams, SolrParams params, SolrQueryRequest req) {
        return new TanimotoQParser(queryString, localParams, params, req);
    }

    private static class TanimotoQParser extends QParser {

        private Query innerQuery;
        private SolrParams localParams;


        public TanimotoQParser(String queryString, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
            super(queryString, localParams, params, req);
            try {
                QParser parser = getParser(queryString, "edismax", getReq());
                //this won't update the default mm
                // parser.setLocalParams(this.getCustomParams(parser.getLocalParams()));
                this.innerQuery = parser.parse();
                this.localParams = localParams;
            } catch (SyntaxError ex) {
                throw new RuntimeException("error parsing query", ex);
            }
        }

        /**
         * add default mm value as 30% unless it's specified explicitly
         *
         * @param params --SolrParams
         * @return
         */
        protected SolrParams getCustomParams(SolrParams params) {
            ModifiableSolrParams result = new ModifiableSolrParams();
            Iterator<String> iter = params.getParameterNamesIterator();

            while (iter.hasNext()) {
                String nxt = iter.next();
                result.add(nxt, params.getParams(nxt));

            }
            result.set("mm", result.get("mm", "100%"));
            return result;
        }

        @Override
        public Query parse() throws SyntaxError {
            return new TanimotoQuery(localParams, innerQuery);
        }
    }
}

