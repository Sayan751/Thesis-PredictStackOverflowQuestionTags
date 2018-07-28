package thesis.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import thesis.data.common.Terms;

/**
 * This is a general utility class to extract features from text.
 * 
 * @author Sayan
 */
public class FeatureExtractor {

	public static HashMap<String, Integer> GetTermFrequencies(Terms terms) {
		if (terms == null)
			throw new IllegalArgumentException("terms is null");

		HashMap<String, Integer> retVal = new HashMap<String, Integer>();
		terms.forEach(term -> {
			if (!retVal.containsKey(term))
				retVal.put(term, 1);
			else
				retVal.put(term, retVal.get(term) + 1);
		});
		return retVal;
	}

	public static List<HashMap<String, Integer>> GetTermFrequencies(List<Terms> termsMatrix) {
		if (termsMatrix == null)
			throw new IllegalArgumentException("termsMatrix is null");

		return termsMatrix.stream()
				.map(FeatureExtractor::GetTermFrequencies)
				.collect(Collectors.toList());// Collectors.toCollection(ArrayList<HashMap<String,
												// Integer>>::new)
	}

	/**
	 * Extracts tokens from a string, after stemming, and removing stop words in
	 * English.
	 * 
	 * @param documentContent
	 *            A string containing the content of the document.
	 * @return A list of token extracted from {@code documentContent}.
	 * @throws IOException
	 */
	public static Terms GetTokensFromString(String documentContent) throws IOException {

		if (documentContent == null)
			throw new IllegalArgumentException("documentContent is null");

		Tokenizer tokenizer = new StandardTokenizer();
		tokenizer.setReader(new StringReader(documentContent.toLowerCase()));

		TokenStream tokenStream = new StopFilter(tokenizer, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
		tokenStream = new PorterStemFilter(tokenStream);

		return GetTokensFromTokenStrem(tokenStream);
	}

	/**
	 * Converts a list of strings to a matrix of tokens extracted from the
	 * strings. The matrix has one row per item in documentContents.
	 * 
	 * @param documentContents
	 *            List of document contents.
	 * @return A 2D list of extracted tokens, one row per document. Might return
	 *         an empty list for a document in case any problem during token
	 *         extraction from that document.
	 */
	public static List<Terms> GetTokensFromString(List<String> documentContents) {

		if (documentContents == null)
			throw new IllegalArgumentException("documentContents is null");

		return documentContents.stream()
				.map(str -> {
					Terms retVal = new Terms();
					try {
						retVal = GetTokensFromString(str);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					return retVal;
				})
				.collect(Collectors.toList());// Collectors.toCollection(ArrayList<Terms>::new)
	}

	/**
	 * @param tokenStream
	 * @return List of tokens/terms present in the stream
	 * @throws IOException
	 */
	public static Terms GetTokensFromTokenStrem(TokenStream tokenStream) throws IOException {
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
		Terms tokens = new Terms();

		tokenStream.reset();
		while (tokenStream.incrementToken()) {
			tokens.add(charTermAttribute.toString());
		}

		return tokens;
	}
}