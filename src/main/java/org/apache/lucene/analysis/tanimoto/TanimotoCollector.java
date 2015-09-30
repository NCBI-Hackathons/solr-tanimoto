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
 * Created by hanl on 3/20/2015.
 */
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.ScoreCachingWrappingScorer;
import org.apache.lucene.search.Scorer;
import org.apache.solr.search.DelegatingCollector;

import java.io.IOException;

public class TanimotoCollector extends DelegatingCollector /*Collector*/ {


    private float threshold;
    int maxdoc;

    public TanimotoCollector(float threshold) {
        this.threshold = threshold;
    }

    public TanimotoCollector(Collector delegate) {
        this.setDelegate(delegate);
        this.threshold = 0.0f;
    }

    public TanimotoCollector(Collector delegate, float threshold) {
        this.setDelegate(delegate);
        this.threshold = threshold;
    }

    @Override
    public void collect(int doc) throws IOException {
        if (doc< maxdoc && scorer.score() >= this.threshold) {
            delegate.collect(doc);
        }
    }

    @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
        maxdoc = context.reader().maxDoc();
        super.setNextReader(context);
    }

    @Override
    public void setScorer(Scorer scorer) throws IOException {
        // Set a ScoreCachingWrappingScorer in case the wrapped Collector will call
        // score() also.
        this.scorer = new ScoreCachingWrappingScorer(scorer);
        delegate.setScorer(this.scorer);
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
        return this.delegate.acceptsDocsOutOfOrder();
    }

}
