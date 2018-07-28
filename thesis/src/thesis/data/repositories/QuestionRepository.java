package thesis.data.repositories;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.TypedQuery;

import thesis.data.entities.Question;
import thesis.data.repositoryinterfaces.IQuestionRepository;
import thesis.util.Constants;

public class QuestionRepository extends GenericRepository<Question> implements IQuestionRepository {

	private static class Parameters {
		static final String learnerId = "learnerId";
		static final String seQuestionId = "seQuestionId";
	}

	private static class Queries {
		static final String unprocessedQuestions = "select q from Question q left join q.questionLearnerDetails d with d.learnerId = :"
				+ Parameters.learnerId + " where d.id = null order by q.seQuestionId";// q.seCreationDateTime
	}

	private TypedQuery<Question> unprocessedQuestionsQuery = entityManager
			.createQuery(Queries.unprocessedQuestions, persistentClass);

	private TypedQuery<Question> getBySeIdQuery = entityManager.createQuery(
			String.format("%1$s where %2$s.seQuestionId = :seQuestionId", basicQuery, basicEntityAlias),
			persistentClass);

	public QuestionRepository() {
		super(Constants.Entities.Question);
	}

	@Override
	public List<Question> getAllUnprocessedQuestion(UUID learnerId, int count) {
		return unprocessedQuestionsQuery
				.setParameter(Parameters.learnerId, learnerId)
				.setMaxResults(count)
				.getResultList();
	}

	@Override
	public Date getMaxSeCreationDateTime() {

		String hql = "select max(q.seCreationDateTime) from Question q";

		List<Date> retVal = entityManager
				.createQuery(hql, Date.class)
				.getResultList();

		if (retVal == null || retVal.isEmpty())
			new Date(0); // 1970.01.01 00:00:00

		return retVal.get(0);
	}

	public Question getQuestionBySeId(long seQuestionId) {
		List<Question> questions = getBySeIdQuery.setParameter(Parameters.seQuestionId, seQuestionId)
				.getResultList();
		return questions.isEmpty() ? null : questions.get(0);
	}
}