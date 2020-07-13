package com.ruokki.parser;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.CapitalizationFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.util.Arrays;
import java.util.List;

public class MyCustomAnalyzer extends Analyzer {

    private static final CharArraySet JAVA_WORD_SET;

    static {
        final List<String> stopWords = Arrays.asList(
                "import", "class", "final", "abstract", "new", "static",
                "public", "private", "protected", "List", "Arrays", "package", "static", "@Override"
        );
        final CharArraySet stopSet = new CharArraySet(stopWords, true);
        JAVA_WORD_SET = CharArraySet.unmodifiableSet(stopSet);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final StandardTokenizer src = new StandardTokenizer();
        TokenStream result = new StandardFilter(src);
        result = new LowerCaseFilter(result);
        result = new StopFilter(result, JAVA_WORD_SET);
        result = new PorterStemFilter(result);
        result = new CapitalizationFilter(result);
        return new TokenStreamComponents(src, result);
    }

}
