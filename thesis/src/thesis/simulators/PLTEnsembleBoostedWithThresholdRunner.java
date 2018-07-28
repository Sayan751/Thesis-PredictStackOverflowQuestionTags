package thesis.simulators;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.prefs.Preferences;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.code.stackexchange.client.query.StackExchangeApiQueryFactory;
import com.google.code.stackexchange.schema.StackExchangeSite;

import Learner.PLTEnsembleBoostedWithThreshold;
import thesis.data.entities.Learner;
import thesis.data.entities.Tag;
import thesis.data.managers.StackOverflowQuestionManager;
import thesis.data.repositories.LearnerRepository;
import thesis.data.repositories.QuestionLearnerDetailsRepository;
import thesis.data.repositories.QuestionRepository;
import thesis.data.repositories.QuestionTermFrequencyRepository;
import thesis.data.repositories.TagRepository;
import thesis.data.repositories.TermRepository;
import thesis.data.repositoryinterfaces.ILabelRepository;
import thesis.data.repositoryinterfaces.IQuestionLearnerDetailsRepository;
import thesis.data.repositoryinterfaces.IQuestionRepository;
import thesis.data.repositoryinterfaces.IQuestionTermFrequencyRepository;
import thesis.data.repositoryinterfaces.ITermRepository;
import thesis.util.HibernateManager;
import thesis.util.JsonSerializer;
import thesis.util.TextFileWriter;
import threshold.ThresholdTunerInitOption;
import threshold.ThresholdTuners;
import util.AdaptivePLTInitConfiguration;
import util.Constants.PLTEnsembleBoostedWithThresholdDefaultValues;
import util.PLTEnsembleBoostedWithThresholdInitConfiguration;

public class PLTEnsembleBoostedWithThresholdRunner {
	private static final String simulationResultExt = ".txt";
	private static final String simulationResultPath = "../simulationResults/PLTEnsembleBoostedWithThreshold/";
	private static final String texPath = "../simulationResults/ensembleBoostingWithThresholdData.tex";
	private static final String iterationsArg = "-itr";
	private static final String batchSizeArg = "-bs";
	private static final String hdArg = "-hd";
	private static final String ensembleSizeArg = "-es";
	private static final String majorityVoteArg = "-mv";
	private static final String lambdaCWArg = "-lcw";
	private static final String resumeArg = "-r";
	private static final String writeRegistryArg = "-wreg";
	private static final String writeResultArg = "-wres";
	private static final String minEpochsArg = "-me";
	private static final String kslckArg = "-kslck";
	private static Logger logger;
	static {
		System.setProperty("initiator", PLTEnsembleBoostedWithThresholdRunner.class.getSimpleName());
		System.setProperty("current.date", (new SimpleDateFormat("yyyy-MM-dd HHmmss")).format(new Date()));
		System.setProperty("log4j.configurationFile", "META-INF/log4j.xml");
		logger = LogManager.getLogger(PLTEnsembleBoostedWithThresholdRunner.class);
	}
	LearnerRepository learnerRepository;
	private ILabelRepository<Tag> labelRepository;
	private IQuestionRepository questionRepository;
	private IQuestionTermFrequencyRepository questionTermFrequencyRepository;
	private ITermRepository termRepository;
	private IQuestionLearnerDetailsRepository questionLearnerDetailsRepository;

	private StackExchangeApiQueryFactory queryFactory;
	StackOverflowQuestionManager soDataManager;
	private static boolean toWriteRegistry = false;
	private static boolean toWriteResult = false;
	private static final String regKeyFormat = "boostedEnsembleWithThreshold-{0,number,#}-{1}-{2}-{3}-{4}";

	PLTEnsembleBoostedWithThreshold pltEnsembleBoostedWithThreshold;
	static ObjectMapper simpleObjectMapper = new ObjectMapper();

	public PLTEnsembleBoostedWithThresholdRunner(ObjectNode runConfig) throws JsonProcessingException {

		initRepositories();
		initSODataManage();
		PLTEnsembleBoostedWithThresholdInitConfiguration properties = getLearnerConfiguration(runConfig);

		pltEnsembleBoostedWithThreshold = new PLTEnsembleBoostedWithThreshold(properties);

		Learner learner = learnerRepository.create(new Learner(runConfig.toString()), pltEnsembleBoostedWithThreshold);
		final UUID learnerId = learner.getId();
		pltEnsembleBoostedWithThreshold.setId(learnerId);

		if (toWriteRegistry)
			Preferences.userRoot()
					.node("thesis")
					.put(MessageFormat.format(regKeyFormat, properties.individualPLTConfiguration.getHd(),
							properties.getEnsembleSize(), properties.getMinEpochs(),
							properties.isToAggregateByLambdaCW(), properties.isToAggregateByMajorityVote())
							.toLowerCase(), learnerId.toString());
	}

	public PLTEnsembleBoostedWithThresholdRunner(PLTEnsembleBoostedWithThresholdInitConfiguration config)
			throws JsonProcessingException {

		initRepositories();
		initSODataManage();

		config.fmeasureObserver = this.soDataManager;
		config.learnerRepository = this.learnerRepository;

		pltEnsembleBoostedWithThreshold = new PLTEnsembleBoostedWithThreshold(config);

		Learner learner = learnerRepository.create(new Learner(simpleObjectMapper.writeValueAsString(config)),
				pltEnsembleBoostedWithThreshold);
		final UUID learnerId = learner.getId();
		pltEnsembleBoostedWithThreshold.setId(learnerId);

		if (toWriteRegistry)
			Preferences.userRoot()
					.node("thesis")
					.put(MessageFormat.format(regKeyFormat, config.individualPLTConfiguration.getHd(),
							config.getEnsembleSize(), config.getMinEpochs(), config.isToAggregateByLambdaCW(),
							config.isToAggregateByMajorityVote())
							.toLowerCase(), learnerId.toString());
	}

	public PLTEnsembleBoostedWithThresholdRunner(UUID learnerId) {

		initRepositories();
		initSODataManage();

		pltEnsembleBoostedWithThreshold = learnerRepository.read(learnerId, PLTEnsembleBoostedWithThreshold.class);
		if (pltEnsembleBoostedWithThreshold.fmeasureObserverAvailable) {
			pltEnsembleBoostedWithThreshold.fmeasureObserver = soDataManager;
			pltEnsembleBoostedWithThreshold.addInstanceProcessedListener(soDataManager);
			pltEnsembleBoostedWithThreshold.addInstanceTestedListener(soDataManager);
		}
		pltEnsembleBoostedWithThreshold.setLearnerRepository(learnerRepository);
	}

	public static void main(String[] args) {

		try {
			Map<String, String> configMap = null;
			BinomialDistribution rbinom = null;
			int iterations, batchSize = 1;
			JsonNode config = null;

			if (args.length == 1) {
				String configFileName = args[0];
				ObjectNode runConfig = JsonSerializer.readFromFile(configFileName, ObjectNode.class);

				iterations = runConfig.get("iterations")
						.asInt();
				batchSize = runConfig.get("batchSize")
						.asInt(1);

				double trainingProbability = runConfig.get("trainingProbability")
						.asDouble(1.0);
				rbinom = new BinomialDistribution(1, trainingProbability);

				config = runConfig.get("ensembleBoostedProperties");
			} else {
				if ((args.length & 1) != 0)
					throw new IllegalArgumentException("Invalid number of arguments provided");

				configMap = new HashMap<>();
				for (int i = 0; i < args.length; i += 2) {
					configMap.put(args[i], args[i + 1]);
				}

				if (!((configMap.keySet()
						.containsAll(Arrays.asList(iterationsArg, hdArg, ensembleSizeArg, minEpochsArg)) &&
						(configMap.keySet()
								.contains(majorityVoteArg) || configMap.keySet()
										.contains(lambdaCWArg)))
						|| configMap.keySet()
								.containsAll(Arrays.asList(iterationsArg, resumeArg))))
					throw new IllegalArgumentException(
							"One of two following sets of arguments are needed:"
									+ "\n\t 1: iterations (-itr), hashed dimension (-hd), ensemble size (-es), number of minimum epochs (-me), "
									+ "\n\t\t aggregation options: by majority voting (-mv) or by aggregate by lambda cw (-lcw)"
									+ "\n\t 2: iterations (-itr), learner to resume (-r)");

				iterations = Integer.parseInt(configMap.get(iterationsArg));
				if (configMap.containsKey(batchSizeArg))
					batchSize = Integer.parseInt(configMap.get(batchSizeArg));
				else
					logger.info("Batch size not provided, continuing with default batch size of 1");

				if (configMap.containsKey(writeRegistryArg))
					toWriteRegistry = Boolean.parseBoolean(configMap.get(writeRegistryArg));
				if (configMap.containsKey(writeResultArg))
					toWriteResult = Boolean.parseBoolean(configMap.get(writeResultArg));
			}
			PLTEnsembleBoostedWithThresholdRunner runner = null;

			if (config == null && configMap == null)
				throw new IllegalArgumentException("Invalid run configuration");

			if (configMap == null)
				runSimulation(iterations, batchSize, rbinom,
						(new ObjectMapper()).treeToValue(config,
								PLTEnsembleBoostedWithThresholdInitConfiguration.class),
						null,
						runner);
			else {
				if (configMap.containsKey(resumeArg)) {
					UUID learnerId = UUID.fromString(configMap.get(resumeArg));
					logger.info("Resuming for learnerId:" + learnerId);
					resumeSimulation(iterations, batchSize, rbinom, runner, learnerId);
				} else {
					final int minEpoch = Integer.parseInt(configMap.get(minEpochsArg));
					final int hd = Integer.parseInt(configMap.get(hdArg));
					final int ensembleSize = Integer.parseInt(configMap.get(ensembleSizeArg));
					final boolean aggrPref = configMap.containsKey(majorityVoteArg)
							? Boolean.parseBoolean(configMap.get(majorityVoteArg))
							: PLTEnsembleBoostedWithThresholdDefaultValues.isToAggregateByMajorityVote;
					final boolean aggrLambdaCW = configMap.containsKey(majorityVoteArg)
							? Boolean.parseBoolean(configMap.get(lambdaCWArg))
							: PLTEnsembleBoostedWithThresholdDefaultValues.isToAggregateByLambdaCW;

					AdaptivePLTInitConfiguration pltConfig = getConstantPLTConfig();
					PLTEnsembleBoostedWithThresholdInitConfiguration ensembleConfig = getConstantEnsembleConfig();

					if (configMap.containsKey(kslckArg))
						ensembleConfig.setkSlack(Integer.parseInt(configMap.get(kslckArg)));

					ensembleConfig.individualPLTConfiguration = pltConfig;

					pltConfig.setHd(hd);

					ensembleConfig.setMinEpochs(minEpoch);
					ensembleConfig.setEnsembleSize(ensembleSize);
					ensembleConfig.setToAggregateByMajorityVote(aggrPref);
					ensembleConfig.setToAggregateByLambdaCW(aggrLambdaCW);

					runSimulation(iterations, batchSize, rbinom, ensembleConfig,
							MessageFormat.format(
									"PLTEnsembleBoostedWithThreshold_{0,number,#}_{1,number,#}_{2,number,#}_{3}_{4}_{5}",
									iterations, batchSize, hd, ensembleSize,
									aggrPref ? "MV" : (aggrLambdaCW ? "LCW" : "FM"), minEpoch),
							runner);
				}
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			HibernateManager.shutdown();
		}
	}

	private static PLTEnsembleBoostedWithThresholdInitConfiguration getConstantEnsembleConfig() {
		PLTEnsembleBoostedWithThresholdInitConfiguration ensembleConfig = new PLTEnsembleBoostedWithThresholdInitConfiguration();
		ensembleConfig.setMaxBranchingFactor(10);
		ensembleConfig.tunerInitOption = new ThresholdTunerInitOption(1, 2);
		ensembleConfig.setMeasureTime(true);
		return ensembleConfig;
	}

	private static AdaptivePLTInitConfiguration getConstantPLTConfig() {
		AdaptivePLTInitConfiguration pltConfig = new AdaptivePLTInitConfiguration();
		pltConfig.setToPreferHighestProbLeaf(true);
		pltConfig.setHasher("AdaptiveMurmur");
		pltConfig.tunerType = ThresholdTuners.AdaptiveOfoFast;
		pltConfig.tunerInitOption = new ThresholdTunerInitOption(1, 2);
		pltConfig.setLambda(1);
		pltConfig.setGamma(1);
		return pltConfig;
	}

	private static void resumeSimulation(int iterations, int batchSize, BinomialDistribution rbinom,
			PLTEnsembleBoostedWithThresholdRunner runner, UUID learnerId)
			throws JsonParseException, JsonMappingException, IOException {
		runner = new PLTEnsembleBoostedWithThresholdRunner(learnerId);
		Learner learner = runner.learnerRepository.read(learnerId);
		PLTEnsembleBoostedWithThresholdInitConfiguration config = simpleObjectMapper.readValue(
				learner.getLearnerDetails(),
				PLTEnsembleBoostedWithThresholdInitConfiguration.class);

		runSimulation(iterations, batchSize, rbinom, config,
				MessageFormat.format(
						"PLTEnsembleBoostedWithThreshold_{0,number,#}_{1,number,#}_{2,number,#}_{3}_{4}_{5}",
						iterations, batchSize, config.individualPLTConfiguration.getHd(), config.getEnsembleSize(),
						config.isToAggregateByMajorityVote() ? "MV" : (config.isToAggregateByLambdaCW() ? "LCW" : "FM"),
						config.getMinEpochs()),
				runner, runner.pltEnsembleBoostedWithThreshold.getnTrain());
	}

	private static void runSimulation(int iterations, int batchSize, BinomialDistribution rbinom,
			PLTEnsembleBoostedWithThresholdInitConfiguration config, String fileName,
			PLTEnsembleBoostedWithThresholdRunner runner)
			throws JsonProcessingException {
		runSimulation(iterations, batchSize, rbinom, config, fileName, runner, 0);
	}

	private static void runSimulation(int iterations, int batchSize, BinomialDistribution rbinom,
			PLTEnsembleBoostedWithThresholdInitConfiguration config, String fileName,
			PLTEnsembleBoostedWithThresholdRunner runner,
			int startIndex)
			throws JsonProcessingException {
		if (runner == null || startIndex == 0) {
			runner = new PLTEnsembleBoostedWithThresholdRunner(config);
			runner.soDataManager.loadNext(batchSize, runner.pltEnsembleBoostedWithThreshold.getId());
			runner.pltEnsembleBoostedWithThreshold.allocateClassifiers(runner.soDataManager);
		}
		// boolean toTrain = true;

		for (int index = startIndex; index < iterations; index++) {
			// toTrain = index == 0 || rbinom.sample() == 1;

			if (index > 0)
				runner.soDataManager.loadNext(batchSize, runner.pltEnsembleBoostedWithThreshold.getId());

			// if (toTrain)
			runner.pltEnsembleBoostedWithThreshold.train(runner.soDataManager);
			// else
			// runner.pltEnsembleBoosted.test(runner.soDataManager);

			runner.learnerRepository.update(runner.pltEnsembleBoostedWithThreshold.getId(),
					runner.pltEnsembleBoostedWithThreshold);

			logger.info("End of iteration " + (index + 1) + " of " + iterations);
		}

		// runner.learnerRepository.update(runner.learnerRepository.read(runner.pltEnsembleBoosted.getId()),
		// runner.pltEnsembleBoosted);
		if (toWriteResult) {
			StringBuilder verbose = new StringBuilder(), tex = new StringBuilder();
			buildSimulationInfo(iterations, batchSize, rbinom, config, runner, verbose, tex);

			if (fileName != null && !fileName.isEmpty()) {
				TextFileWriter.writeToFile(simulationResultPath + fileName + simulationResultExt, verbose.toString());
				TextFileWriter.writeToFile(texPath, tex.toString(), true);
				logger.info("End of simulation: " + fileName);
			} else
				logger.info(verbose.toString());
		}
	}

	private static void buildSimulationInfo(int iterations, int batchSize, BinomialDistribution rbinom,
			PLTEnsembleBoostedWithThresholdInitConfiguration config, PLTEnsembleBoostedWithThresholdRunner runner,
			StringBuilder verbose,
			StringBuilder tex)
			throws JsonProcessingException {

		double trainMacroFm = runner.pltEnsembleBoostedWithThreshold.getMacroFmeasure();
		double trainAvgTime = runner.pltEnsembleBoostedWithThreshold.getAverageTrainTime();

		double prequestialAvgFm = runner.pltEnsembleBoostedWithThreshold.getAverageFmeasure(true, false);
		double prequestialAvgFmTopk = runner.pltEnsembleBoostedWithThreshold.getAverageFmeasure(true, true);
		double prequestialEvalAvgTime = runner.pltEnsembleBoostedWithThreshold.getAverageEvaluationTime(true, false);
		double prequestialEvalAvgTimeTopk = runner.pltEnsembleBoostedWithThreshold.getAverageEvaluationTime(true, true);

		double avgFm = runner.pltEnsembleBoostedWithThreshold.getAverageFmeasure(false, false);
		double avgFmTopk = runner.pltEnsembleBoostedWithThreshold.getAverageFmeasure(false, true);
		double evalAvgTime = runner.pltEnsembleBoostedWithThreshold.getAverageEvaluationTime(false, false);
		double evalAvgTimeTopk = runner.pltEnsembleBoostedWithThreshold.getAverageEvaluationTime(false, true);

		verbose.append("Simulation Info:\n");
		verbose.append("================\n\n");

		verbose.append("Total number of instances: ");
		verbose.append(iterations * batchSize);
		verbose.append("\n");

		verbose.append("Simulation configuration: ");
		verbose.append(simpleObjectMapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(config));
		verbose.append("\n\n");

		verbose.append("Training info:\n");
		verbose.append("================\n");
		verbose.append("\n");
		verbose.append("Macro fmeasure: ");
		verbose.append(trainMacroFm);
		verbose.append("\n");
		verbose.append("Average training time: ");
		verbose.append(trainAvgTime);
		verbose.append(" \u00B5s\n\n");

		verbose.append("Prequential Evaluation info:\n");
		verbose.append("================\n");
		verbose.append("Average fmeasure: ");
		verbose.append(prequestialAvgFm);
		verbose.append("\n");
		verbose.append("Topk-based average fmeasure: ");
		verbose.append(prequestialAvgFmTopk);
		verbose.append("\n");
		verbose.append("Average evaluation time: ");
		verbose.append(prequestialEvalAvgTime);
		verbose.append(" \u00B5s\n");
		verbose.append("Average topk evaluation time: ");
		verbose.append(prequestialEvalAvgTimeTopk);
		verbose.append(" \u00B5s\n\n");

		verbose.append("Evaluation info:\n");
		verbose.append("================\n");
		verbose.append("Average fmeasure: ");
		verbose.append(avgFm);
		verbose.append("\n");
		verbose.append("Topk-based average fmeasure: ");
		verbose.append(avgFmTopk);
		verbose.append("\n");
		verbose.append("Average evaluation time: ");
		verbose.append(evalAvgTime);
		verbose.append(" \u00B5s\n");
		verbose.append("Average topk evaluation time: ");
		verbose.append(evalAvgTimeTopk);
		verbose.append(" \u00B5s\n\n");

		tex.append(config.individualPLTConfiguration.getHd());
		tex.append(" & ");
		tex.append(config.getEnsembleSize());
		tex.append(" & ");
		tex.append(config.getMinEpochs());
		tex.append(" & ");
		tex.append(config.isToAggregateByMajorityVote() ? "MV" : (config.isToAggregateByLambdaCW() ? "LCW" : "FM"));
		tex.append(" & ");

		tex.append(MessageFormat.format("{0,number,0.00}", prequestialAvgFm * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.0E0}", prequestialEvalAvgTime / 1000.0));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", prequestialAvgFmTopk * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.0E0}", prequestialEvalAvgTimeTopk / 1000.0));
		tex.append(" & ");

		tex.append(MessageFormat.format("{0,number,0.00}", trainMacroFm * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.0E0}", trainAvgTime / 1000.0));
		tex.append(" & ");

		tex.append(MessageFormat.format("{0,number,0.00}", avgFm * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.0E0}", evalAvgTime / 1000.0));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", avgFmTopk * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.0E0}", evalAvgTimeTopk / 1000.0));

		tex.append(" \\\\ \\hline \n");
	}

	private void initRepositories() {
		learnerRepository = new LearnerRepository();
		labelRepository = new TagRepository();
		questionRepository = new QuestionRepository();
		questionTermFrequencyRepository = new QuestionTermFrequencyRepository();
		termRepository = new TermRepository();
		questionLearnerDetailsRepository = new QuestionLearnerDetailsRepository();
	}

	private void initSODataManage() {
		queryFactory = StackExchangeApiQueryFactory.newInstance("pj9WKZ7g4MJ6XKsKWuxKmA((",
				StackExchangeSite.STACK_OVERFLOW);
		soDataManager = new StackOverflowQuestionManager(labelRepository, questionRepository,
				questionTermFrequencyRepository,
				termRepository, queryFactory, questionLearnerDetailsRepository);
	}

	private PLTEnsembleBoostedWithThresholdInitConfiguration getLearnerConfiguration(ObjectNode runConfig)
			throws JsonProcessingException {

		PLTEnsembleBoostedWithThresholdInitConfiguration config = (new ObjectMapper()).treeToValue(
				runConfig.get("ensembleBoostedProperties"),
				PLTEnsembleBoostedWithThresholdInitConfiguration.class);

		config.fmeasureObserver = this.soDataManager;
		config.learnerRepository = this.learnerRepository;

		return config;
	}
}
