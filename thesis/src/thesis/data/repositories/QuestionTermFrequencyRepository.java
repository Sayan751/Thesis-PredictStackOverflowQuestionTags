package thesis.data.repositories;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thesis.data.entities.Question;
import thesis.data.entities.QuestionTermFrequency;
import thesis.data.entities.Term;
import thesis.data.repositoryinterfaces.IQuestionTermFrequencyRepository;
import thesis.util.Constants;

public class QuestionTermFrequencyRepository extends GenericRepository<QuestionTermFrequency>
		implements IQuestionTermFrequencyRepository {
	private static Logger logger = LogManager.getLogger(QuestionTermFrequencyRepository.class);

	public QuestionTermFrequencyRepository() {
		super(Constants.Entities.QuestionTermFrequency);
	}

	public List<QuestionTermFrequency> createQuestionTermFrequencies(HashMap<String, Term> termMap, Question question,
			HashMap<String, Integer> questionTermFreqMap) {

		List<QuestionTermFrequency> questionTermFrequencies = questionTermFreqMap.keySet()
				.stream()
				.map(term -> {
					if (termMap.get(term) == null) {
						logger.warn("************" + term);
					}
					QuestionTermFrequency questionTermFrequency = new QuestionTermFrequency();
					questionTermFrequency.setQuestion(question);
					questionTermFrequency.setTerm(termMap.get(term));
					questionTermFrequency.setFrequency(questionTermFreqMap.get(term));
					return questionTermFrequency;
				})
				.collect(Collectors.toList());

		create(questionTermFrequencies);
		return questionTermFrequencies;
	}
}
