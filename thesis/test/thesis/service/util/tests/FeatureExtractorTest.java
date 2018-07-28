/**
 * 
 */
package thesis.service.util.tests;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.junit.Test;

import thesis.data.common.Terms;
import thesis.util.FeatureExtractor;

/**
 * @author Sayan
 *
 */
public class FeatureExtractorTest {

	@Test(expected = IllegalArgumentException.class)
	public void GetTokensFromStringTest_NullDocumentContentShouldCauseException() throws Exception {
		String documentContent = null;
		FeatureExtractor.GetTokensFromString(documentContent);
	}

	@Test()
	public void GetTokensFromStringTest_EmptyDocumentContentShouldReturnEmptyTerms() throws Exception {
		String documentContent = "";
		Terms actual = FeatureExtractor.GetTokensFromString(documentContent);
		assertEquals(0, actual.size());
	}

	@Test()
	public void GetTokensFromStringTest_TermsReturnedShouldBeInLowerCaseAndStemmedAndContainNoStopWord()
			throws Exception {
		String documentContent = "This is the First testing Document";

		Terms actual = FeatureExtractor.GetTokensFromString(documentContent);

		Set<String> expectedSet = new HashSet<String>(
				new ArrayList<String>(Arrays.asList("first", "document", "test")));

		assertEquals(3, actual.size());
		assertEquals(expectedSet, new HashSet<String>(actual));
	}

	@Test(expected = IllegalArgumentException.class)
	public void GetTokensFromStringTest_NullDocumentContentsShouldCauseException() throws Exception {
		ArrayList<String> documentContents = null;
		FeatureExtractor.GetTokensFromString(documentContents);
	}

	@Test()
	public void GetTokensFromStringTest_EmptyDocumentContentsShouldReturnEmptyMatrix() throws Exception {
		ArrayList<String> documentContents = new ArrayList<String>();
		List<Terms> actual = FeatureExtractor.GetTokensFromString(documentContents);
		assertEquals(0, actual.size());
	}

	@Test()
	public void GetTokensFromStringTest_MatrixReturnedShouldBeInLowerCaseAndStemmedAndContainNoStopWord()
			throws Exception {
		ArrayList<String> documentContents = new ArrayList<String>(
				Arrays.asList("This is the First testing Document", "This is the Second testing Document"));
		List<Terms> actual = FeatureExtractor.GetTokensFromString(documentContents);

		assertEquals(2, actual.size());
		assert (actual
				.stream()
				.allMatch(terms -> terms.stream()
						.allMatch(term -> term.chars()
								.allMatch(c -> !Character.isUpperCase(c)))));

		assert (actual
				.stream()
				.allMatch(terms -> !terms.contains("testing")));
		assert (actual
				.stream()
				.allMatch(terms -> !terms.contains(StopAnalyzer.ENGLISH_STOP_WORDS_SET)));

	}

	@Test(expected = IllegalArgumentException.class)
	public void GetTermFrequenciesTest_NullTermsShouldCauseException() {
		Terms terms = null;
		FeatureExtractor.GetTermFrequencies(terms);
	}

	@Test
	public void GetTermFrequenciesTest_EmptyTermsShouldReturnEmptyMap() {
		Terms terms = new Terms();
		HashMap<String, Integer> actual = FeatureExtractor.GetTermFrequencies(terms);
		assertEquals(0, actual.size());
	}

	@Test
	public void GetTermFrequenciesTest_ValidInputShouldReturnValidHashMap() throws Exception {
		String documentContent = "This is the First testing Document. The document is for testing purpose.";
		HashMap<String, Integer> actual = FeatureExtractor
				.GetTermFrequencies(FeatureExtractor.GetTokensFromString(documentContent));

		HashMap<String, Integer> expected = new HashMap<String, Integer>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			{
				put("first", 1);
				put("document", 2);
				put("test", 2);
				put("purpos", 1);
			}
		};
		assertEquals(expected, actual);
	}
}
