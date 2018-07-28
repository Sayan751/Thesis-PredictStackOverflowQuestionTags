/**
 * 
 */
package thesis.data.repositoryinterfaces;

import java.util.UUID;

import thesis.data.entities.QuestionLearnerDetails;

/**
 * @author Sayan
 *
 */
public interface IQuestionLearnerDetailsRepository extends IRepository<QuestionLearnerDetails> {

	public double getAverageFmeasure(UUID learnerId, boolean isPrequential, boolean isTopk);

	public double getTestAverageFmeasure(UUID learnerId, boolean isTopk);
}
