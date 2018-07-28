package thesis._playgrounds;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import thesis.data.common.Terms;
import thesis.data.entities.Question;
import thesis.data.entities.Term;
import thesis.data.repositories.QuestionRepository;
import thesis.data.repositories.TermRepository;
import thesis.util.FeatureExtractor;
import thesis.util.HibernateManager;

public class Playground {

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		QuestionRepository questionRepository = new QuestionRepository();
		TermRepository termRepository = new TermRepository();

		List<String> ids = Arrays.asList(
				"A0380716-1692-4ACD-A47B-17604DF09D30",
				"31F22BA3-1D78-4065-8552-22F91B89693D",
				"D3584267-8BCB-4C94-B99A-37C3BA1A58E3",
				"3BC2EFE8-DA8C-48AF-B8BB-3FE8AEECFA33",
				"0EB78259-4B59-465E-A9CD-422522B675E7",
				"C7094D7A-F726-4652-B7EF-8BB741455556",
				"F985D8CF-0F8A-4B6A-8660-B6ACBA4ABB4F",
				"64990C1D-EF37-4050-8384-CBABF43540DD",
				"12272F18-6EB2-467A-90FD-DAB67FBBED87",
				"DF64EA03-9BDD-41FC-A24C-E2E66BA5E30B",
				"B07DE5DE-B675-4411-9E82-C17A92ACABE6",
				"EC37777A-04AC-4491-B0AD-ED3A6CD547F1");

		List<Question> questions = ids.stream()
				.map(id -> questionRepository.read(UUID.fromString(id)))
				.collect(Collectors.toList());

		// Question q =
		// questionRepository.read(UUID.fromString("AFAC73EC-ED0C-44DA-A312-93D18842813A"));
		//
		// List<Terms> terms =
		// FeatureExtractor.GetTokensFromString(Arrays.asList(q.getTitleAndBodyCombinedText()));
		List<Terms> terms = FeatureExtractor.GetTokensFromString(questions.stream()
				.map(q -> q.getTitleAndBodyCombinedText())
				.collect(Collectors.toList()));

		List<HashMap<String, Integer>> termFrequencies = FeatureExtractor.GetTermFrequencies(terms);

		List<String> questionTerms = termFrequencies.stream()
				.flatMap(mp -> mp.keySet()
						.stream())
				.distinct()
				.collect(Collectors.toList());

		boolean containsBeta = questionTerms.contains("ß");
		boolean containsSS = questionTerms.contains("ss");

		HashMap<String, Term> termMap = questionTerms.size() > 0
				? termRepository.fetchOrStoreByName(questionTerms)
				: new HashMap<String, Term>();

		HibernateManager.shutdown();
	}
}
