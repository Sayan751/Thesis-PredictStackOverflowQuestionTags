package thesis.data.repositoryinterfaces;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import thesis.data.entities.Question;

public interface IQuestionRepository extends IRepository<Question> {

	/**
	 * Returns all questions that are not yet processed by the given learner.
	 * 
	 * @param learnerId
	 *            Unique id of the learner.
	 * @param count
	 *            Number of questions to return
	 * @return List of unprocessed questions.
	 */
	List<Question> getAllUnprocessedQuestion(UUID learnerId, int count);

	Date getMaxSeCreationDateTime();
}