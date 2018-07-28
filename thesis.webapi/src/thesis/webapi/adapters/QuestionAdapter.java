package thesis.webapi.adapters;

import java.util.HashSet;

import thesis.data.entities.Question;
import thesis.webapi.dto.QuestionDto;

public class QuestionAdapter {
	public static QuestionDto toDto(Question q) {
		QuestionDto retVal = new QuestionDto();
		retVal.id = q.getId();
		retVal.createdOn =  q.getCreatedOn().toString();
		retVal.seQuestionId = q.getSeQuestionId();
		retVal.seCreationDateTime = q.getSeCreationDateTime();
		retVal.title = q.getTitle();
		retVal.body = q.getBody();
		retVal.tags = new HashSet<>(TagAdapter.toDto(q.getTags()));
		return retVal;
	}
	
	public static Question createWithTitleAndBody(QuestionDto q) {
		Question retVal = new Question();
		retVal.setTitle(q.title);
		retVal.setBody(q.body);		
		return retVal;
	}
}
