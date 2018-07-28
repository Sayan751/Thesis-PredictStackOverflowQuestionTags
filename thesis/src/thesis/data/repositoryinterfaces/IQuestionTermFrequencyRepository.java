package thesis.data.repositoryinterfaces;

import java.util.HashMap;
import java.util.List;

import thesis.data.entities.Question;
import thesis.data.entities.QuestionTermFrequency;
import thesis.data.entities.Term;

public interface IQuestionTermFrequencyRepository extends IRepository<QuestionTermFrequency> {

	List<QuestionTermFrequency> createQuestionTermFrequencies(HashMap<String, Term> termMap, Question question,
			HashMap<String, Integer> questionTermFreqMap);
}
