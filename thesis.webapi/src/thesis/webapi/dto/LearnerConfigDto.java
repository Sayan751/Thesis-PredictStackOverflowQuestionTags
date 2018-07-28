package thesis.webapi.dto;

/**
 * DTO expected at learner creation service.
 * 
 * @author Sayan
 *
 */
public class LearnerConfigDto {
	public String learnerClass;
	public String configJson;

	@Override
	public String toString() {
		return "class: " + learnerClass + ", config: " + configJson;
	}
}
