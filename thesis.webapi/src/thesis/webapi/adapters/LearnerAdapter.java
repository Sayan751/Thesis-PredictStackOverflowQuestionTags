package thesis.webapi.adapters;

import java.time.Instant;
import java.util.UUID;

import Learner.AbstractLearner;
import thesis.webapi.dto.FmeasuresInfo;
import thesis.webapi.dto.LearnerInfoDto;
import util.LearnerInitConfiguration;

public class LearnerAdapter {

	public static LearnerInfoDto toLearnerInfo(UUID learnerId, Instant createdOn,
			LearnerInitConfiguration learnerConfig,
			Class<? extends AbstractLearner> learnerClass, AbstractLearner learner) {

		LearnerInfoDto retVal = new LearnerInfoDto();
		retVal.id = learnerId;
		retVal.createdOn = createdOn.toString();
		retVal.learnerClass = learnerClass.getSimpleName();
		retVal.macroFmeasure = learner.getMacroFmeasure();
		retVal.avgPrequentialFmeasures.general = learner.getAverageFmeasure(true, false);
		retVal.avgPrequentialFmeasures.topk = learner.getAverageFmeasure(true, true);
		retVal.avgPostTrainingFmeasures.general = learner.getAverageFmeasure(false, false);
		retVal.avgPostTrainingFmeasures.topk = learner.getAverageFmeasure(false, true);
		retVal.learnerConfig = learnerConfig;
		return retVal;
	}

	public static LearnerInfoDto buildPostTrainingLearnerInfo(AbstractLearner learner,
			FmeasuresInfo prequentialFmeasures,
			FmeasuresInfo postTrainFmeasures) {
		LearnerInfoDto retVal;
		retVal = new LearnerInfoDto();
		retVal.id = learner.getId();
		retVal.macroFmeasure = learner.getMacroFmeasure();
		retVal.prequentialFmsCurrentInstance = prequentialFmeasures;
		retVal.postTrainingFmsCurrentInstance = postTrainFmeasures;
		retVal.avgPrequentialFmeasures.general = learner.getAverageFmeasure(true, false);
		retVal.avgPrequentialFmeasures.topk = learner.getAverageFmeasure(true, true);
		retVal.avgPostTrainingFmeasures.general = learner.getAverageFmeasure(false, false);
		retVal.avgPostTrainingFmeasures.topk = learner.getAverageFmeasure(false, true);
		return retVal;
	}

}
