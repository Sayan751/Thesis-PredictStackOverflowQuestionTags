package thesis.webapi.dto;

import util.LearnerInitConfiguration;

public class LearnerInfoDto extends BaseDto {

	public String learnerClass;
	public double macroFmeasure;
	public FmeasuresInfo avgPrequentialFmeasures;
	public FmeasuresInfo avgPostTrainingFmeasures;
	public FmeasuresInfo prequentialFmsCurrentInstance;
	public FmeasuresInfo postTrainingFmsCurrentInstance;
	public LearnerInitConfiguration learnerConfig;

	public LearnerInfoDto() {
		avgPostTrainingFmeasures = new FmeasuresInfo();
		avgPrequentialFmeasures = new FmeasuresInfo();
	}
}
