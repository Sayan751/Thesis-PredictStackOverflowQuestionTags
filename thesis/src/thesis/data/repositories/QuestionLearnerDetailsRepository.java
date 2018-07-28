/**
 * 
 */
package thesis.data.repositories;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import javax.persistence.Query;

import thesis.data.entities.QuestionLearnerDetails;
import thesis.data.repositoryinterfaces.IQuestionLearnerDetailsRepository;
import thesis.util.Constants;

/**
 * @author Sayan
 *
 */
public class QuestionLearnerDetailsRepository extends GenericRepository<QuestionLearnerDetails>
		implements IQuestionLearnerDetailsRepository {

	private static class Parameters {
		static final String learnerId = "learnerId";
		static final String isPrequential = "isPrequential";
		static final String isTopk = "isTopk";
	}

	private static class Queries {
		static final String averageFmeasure = "SELECT AVG({0}.fMeasure) FROM {1} {0} WHERE {0}.learnerId = :learnerId and {0}.test = false and {0}.prequential = :isPrequential and {0}.topk = :isTopk";
		static final String averageTestFmeasure = "SELECT AVG({0}.fMeasure) FROM {1} {0} WHERE {0}.learnerId = :learnerId and {0}.test = true and {0}.topk = :isTopk";
	}

	public QuestionLearnerDetailsRepository() {
		super(Constants.Entities.QuestionLearnerDetails);
	}

	private Query averageFmeasureQuery = entityManager
			.createQuery(MessageFormat.format(Queries.averageFmeasure, basicEntityAlias, entityName));

	private Query averageTestFmeasureQuery = entityManager
			.createQuery(MessageFormat.format(Queries.averageTestFmeasure, basicEntityAlias, entityName));

	@Override
	public double getAverageFmeasure(UUID learnerId, boolean isPrequential, boolean isTopk) {
		entityManager.getTransaction()
				.begin();
		double retVal = 0;
		try {
			List<?> result = averageFmeasureQuery
					.setParameter(Parameters.learnerId, learnerId)
					.setParameter(Parameters.isPrequential, isPrequential)
					.setParameter(Parameters.isTopk, isTopk)
					.getResultList();

			if (result.size() > 0) {
				Object avg = result.get(0);
				if (avg != null)
					retVal = (double) avg;
			}
		} finally {
			entityManager.getTransaction()
					.commit();
		}
		return retVal;
	}

	@Override
	public double getTestAverageFmeasure(UUID learnerId, boolean isTopk) {
		entityManager.getTransaction()
				.begin();
		double retVal = 0;
		try {
			List<?> result = averageTestFmeasureQuery
					.setParameter(Parameters.learnerId, learnerId)
					.setParameter(Parameters.isTopk, isTopk)
					.getResultList();

			if (result.size() > 0) {
				Object avg = result.get(0);
				if (avg != null)
					retVal = (double) avg;
			}
		} finally {
			entityManager.getTransaction()
					.commit();
		}
		return retVal;
	}
}
