package thesis.webapi.dto;

import java.util.Date;
import java.util.Set;

public class QuestionDto extends BaseDto {

	public long seQuestionId;
	public String title;
	public String body;
	public Date seCreationDateTime;
	public Set<TagDto> tags;
}