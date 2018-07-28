package thesis.webapi.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LearnerFmeasuresDto extends BaseDto {

	public List<Double> preqGenFm;
	public List<Double> preqTopkFm;
	public List<Double> postTrainGenFm;
	public List<Double> postTrainTopkFm;

	public LearnerFmeasuresDto() {
		preqGenFm = new ArrayList<>();
		preqTopkFm = new ArrayList<>();
		postTrainGenFm = new ArrayList<>();
		postTrainTopkFm = new ArrayList<>();
	}

	public LearnerFmeasuresDto(UUID learnerId) {
		this();
		this.id = learnerId;
	}
}