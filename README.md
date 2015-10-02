# Tanimoto Solr Plugin 

[![Build Status](https://travis-ci.org/NCBI-Hackathons/solr-tanimoto.svg)](https://travis-ci.org/NCBI-Hackathons/solr-tanimoto)


##HOWTOs: 
 
 * Step1: add queryparser in solrconfig.xml
 
        <queryparser class="org.apache.lucene.analysis.tanimoto.TanimotoQParserPlugin" name="tanimoto" />
 
 sample solconfig.xml can be found at *testdata/solr/collection1/conf*
 
 * Step2: add similarity schema in schema.xml
  
        <similarity class="org.apache.lucene.analysis.tanimoto.OverlapSimilarity"/>
        
 sample schema.xml can be found at *testdata/solr/collection1/conf*
 
 * Step3: index and perform a query
 
         {!tanimoto bf="matstringlength"  } 1 2 3 4
         
 More examples can be found at the unit testing cases in *TestTanimotoQParsersPlugins*

##Further Reading about the Tanimoto Score
 
1. [http://en.wikipedia.org/wiki/Jaccard_index](http://en.wikipedia.org/wiki/Jaccard_index)
2. [http://www.daylight.com/dayhtml/doc/theory/theory.finger.html](http://www.daylight.com/dayhtml/doc/theory/theory.finger.html)
 