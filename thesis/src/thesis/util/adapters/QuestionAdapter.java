package thesis.util.adapters;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import thesis.data.entities.Question;

/**
 * Converts stack overflow questions to {@link Question}
 * 
 * @author Sayan
 *
 */
public class QuestionAdapter {

	public static List<Question> convertStackOverflowQuestion(
			List<com.google.code.stackexchange.schema.Question> seQuestions) {

		return seQuestions.stream()
				.map(QuestionAdapter::convertStackOverflowQuestion)
				.collect(Collectors.toList());
	}

	public static Question convertStackOverflowQuestion(com.google.code.stackexchange.schema.Question seQuestion) {

		Question question = new Question();

		question.setSeQuestionId(seQuestion.getQuestionId());
		question.setTitle(seQuestion.getTitle());
		question.setBody(seQuestion.getBody());
		question.setSeCreationDateTime(seQuestion.getCreationDate());
		question.setTagNames(new HashSet<String>(seQuestion.getTags()));

		return question;
	}
}