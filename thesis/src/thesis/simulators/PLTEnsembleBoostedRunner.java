package thesis.simulators;

import java.io.File;
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

import Learner.PLTEnsembleBoosted;
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
import util.PLTEnsembleBoostedInitConfiguration;

public class PLTEnsembleBoostedRunner {
	private static final String simulationResultExt = ".txt";
	private static final String simulationResultPath = "../simulationResults/PLTEnsembleBoosted/";
	private static final String texPath = "../simulationResults/ensembleBoostingData.tex";
	private static final String iterationsArg = "-itr";
	private static final String batchSizeArg = "-bs";
	private static final String hdArg = "-hd";
	private static final String ensembleSizeArg = "-es";
	private static final String majorityVoteArg = "-mv";
	private static final String minEpochArg = "-me";
	private static final String resumeArg = "-r";
	private static final String writeRegistryArg = "-wreg";
	private static final String writeResultArg = "-wres";
	private static final String kslckArg = "-kslck";
	private static Logger logger;
	static {
		System.setProperty("initiator", PLTEnsembleBoostedRunner.class.getSimpleName());
		System.setProperty("current.date", (new SimpleDateFormat("yyyy-MM-dd HHmmss")).format(new Date()));
		System.setProperty("log4j.configurationFile", "META-INF/log4j.xml");
		logger = LogManager.getLogger(PLTEnsembleBoostedRunner.class);
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
	private static final String regKeyFormat = "boostedEnsemble-{0,number,#}-{1}-{2}-{3}";

	PLTEnsembleBoosted pltEnsembleBoosted;
	static ObjectMapper simpleObjectMapper = new ObjectMapper();

	public PLTEnsembleBoostedRunner(ObjectNode runConfig) throws JsonProcessingException {

		initRepositories();
		initSODataManage();
		PLTEnsembleBoostedInitConfiguration properties = getLearnerConfiguration(runConfig);

		pltEnsembleBoosted = new PLTEnsembleBoosted(properties);

		Learner learner = learnerRepository.create(new Learner(runConfig.toString()), pltEnsembleBoosted);
		final UUID learnerId = learner.getId();
		pltEnsembleBoosted.setId(learnerId);

		if (toWriteRegistry)
			Preferences.userRoot()
					.node("thesis")
					.put(MessageFormat.format(regKeyFormat, properties.individualPLTConfiguration.getHd(),
							properties.getEnsembleSize(), properties.isToAggregateByMajorityVote(),
							properties.getMinEpochs())
							.toLowerCase(), learnerId.toString());
	}

	public PLTEnsembleBoostedRunner(PLTEnsembleBoostedInitConfiguration config) throws JsonProcessingException {

		initRepositories();
		initSODataManage();

		config.fmeasureObserver = this.soDataManager;
		config.learnerRepository = this.learnerRepository;

		pltEnsembleBoosted = new PLTEnsembleBoosted(config);

		Learner learner = learnerRepository.create(new Learner(simpleObjectMapper.writeValueAsString(config)),
				pltEnsembleBoosted);
		final UUID learnerId = learner.getId();
		pltEnsembleBoosted.setId(learnerId);

		if (toWriteRegistry)
			Preferences.userRoot()
					.node("thesis")
					.put(MessageFormat.format(regKeyFormat, config.individualPLTConfiguration.getHd(),
							config.getEnsembleSize(), config.isToAggregateByMajorityVote(),
							config.getMinEpochs())
							.toLowerCase(), learnerId.toString());

	}

	public PLTEnsembleBoostedRunner(UUID learnerId) {

		initRepositories();
		initSODataManage();

		pltEnsembleBoosted = learnerRepository.read(learnerId, PLTEnsembleBoosted.class);
		if (pltEnsembleBoosted.fmeasureObserverAvailable) {
			pltEnsembleBoosted.fmeasureObserver = soDataManager;
			pltEnsembleBoosted.addInstanceProcessedListener(soDataManager);
			pltEnsembleBoosted.addInstanceTestedListener(soDataManager);
		}
		pltEnsembleBoosted.setLearnerRepository(learnerRepository);
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

				if (!(configMap.keySet()
						.containsAll(Arrays.asList(iterationsArg, hdArg, ensembleSizeArg, majorityVoteArg, minEpochArg))
						|| configMap.keySet()
								.containsAll(Arrays.asList(iterationsArg, resumeArg))))
					throw new IllegalArgumentException(
							"One of two following sets of arguments are needed:\n\t 1: iterations (-itr), hashed dimension (-hd), ensemble size (-es), aggregate by majority voting (-mv), minimum epochs(-me)\n\t 2: iterations (-itr), learner to resume (-r)");

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
			PLTEnsembleBoostedRunner runner = null;

			if (config != null || configMap != null) {
				if (configMap == null)
					runSimulation(iterations, batchSize, rbinom,
							(new ObjectMapper()).treeToValue(config, PLTEnsembleBoostedInitConfiguration.class), null,
							runner);
				else {
					if (configMap.containsKey(resumeArg)) {
						UUID learnerId = UUID.fromString(configMap.get(resumeArg));
						logger.info("Resuming for learnerId:" + learnerId);
						resumeSimulation(iterations, batchSize, rbinom, runner, learnerId);
					} else {
						final int hd = Integer.parseInt(configMap.get(hdArg));
						final int ensembleSize = Integer.parseInt(configMap.get(ensembleSizeArg));
						final int minEpoch = Integer.parseInt(configMap.get(minEpochArg));
						final boolean aggrPref = Boolean.parseBoolean(configMap.get(majorityVoteArg));

						AdaptivePLTInitConfiguration pltConfig = getConstantPLTConfig();
						PLTEnsembleBoostedInitConfiguration ensembleConfig = getConstantEnsembleConfig();
						
						if (configMap.containsKey(kslckArg))
							ensembleConfig.setkSlack(Integer.parseInt(configMap.get(kslckArg)));
						
						ensembleConfig.individualPLTConfiguration = pltConfig;

						pltConfig.setHd(hd);

						ensembleConfig.setEnsembleSize(ensembleSize);
						ensembleConfig.setToAggregateByMajorityVote(aggrPref);
						ensembleConfig.setMinEpochs(minEpoch);

						runSimulation(iterations, batchSize, rbinom, ensembleConfig,
								MessageFormat.format(
										"PLTEnsembleBoosted_{0,number,#}_{1,number,#}_{2,number,#}_{3}_{4}_{5}",
										iterations,
										batchSize, hd, ensembleSize, aggrPref, minEpoch),
								runner);
					}
				}
			} else {
				// run series of simulations
				final int[] hds = { 16384, 32768 };
				final int[] ensembleSizes = { 10, 20 };
				final boolean[] aggregateByMajorityVote = { true, false };
				final int[] minEpochs = { 10, 30 };

				File texFile = new File(texPath);
				if (texFile.exists())
					texFile.delete();

				AdaptivePLTInitConfiguration pltConfig = getConstantPLTConfig();
				PLTEnsembleBoostedInitConfiguration ensembleConfig = getConstantEnsembleConfig();
				ensembleConfig.individualPLTConfiguration = pltConfig;

				for (int hd : hds) {
					for (int ensembleSize : ensembleSizes) {
						for (boolean aggrPref : aggregateByMajorityVote) {
							for (int minEpoch : minEpochs) {

								pltConfig.setHd(hd);

								ensembleConfig.setEnsembleSize(ensembleSize);
								ensembleConfig.setToAggregateByMajorityVote(aggrPref);
								ensembleConfig.setMinEpochs(minEpoch);

								runSimulation(iterations, batchSize, rbinom,
										ensembleConfig, MessageFormat.format(
												"PLTEnsembleBoosted_{0,number,#}_{1,number,#}_{2,number,#}_{3}_{4}_{5}",
												iterations, batchSize, hd,
												ensembleSize, aggrPref, minEpoch),
										runner);
							}
						}
					}
				}
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			HibernateManager.shutdown();
		}
	}

	private static PLTEnsembleBoostedInitConfiguration getConstantEnsembleConfig() {
		PLTEnsembleBoostedInitConfiguration ensembleConfig = new PLTEnsembleBoostedInitConfiguration();
		ensembleConfig.setMaxBranchingFactor(10);
		ensembleConfig.tunerInitOption = new ThresholdTunerInitOption(1, 2);
		ensembleConfig.setfZero(0.001);
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
			PLTEnsembleBoostedRunner runner, UUID learnerId)
			throws JsonParseException, JsonMappingException, IOException {
		runner = new PLTEnsembleBoostedRunner(learnerId);
		Learner learner = runner.learnerRepository.read(learnerId);
		PLTEnsembleBoostedInitConfiguration config = simpleObjectMapper.readValue(learner.getLearnerDetails(),
				PLTEnsembleBoostedInitConfiguration.class);

		runSimulation(iterations, batchSize, rbinom, config,
				MessageFormat.format("PLTEnsembleBoosted_{0,number,#}_{1,number,#}_{2,number,#}_{3}_{4}_{5}",
						iterations, batchSize, config.individualPLTConfiguration.getHd(), config.getEnsembleSize(),
						config.isToAggregateByMajorityVote(), config.getMinEpochs()),
				runner, runner.pltEnsembleBoosted.getnTrain());
	}

	private static void runSimulation(int iterations, int batchSize, BinomialDistribution rbinom,
			PLTEnsembleBoostedInitConfiguration config, String fileName, PLTEnsembleBoostedRunner runner)
			throws JsonProcessingException {
		runSimulation(iterations, batchSize, rbinom, config, fileName, runner, 0);
	}

	private static void runSimulation(int iterations, int batchSize, BinomialDistribution rbinom,
			PLTEnsembleBoostedInitConfiguration config, String fileName, PLTEnsembleBoostedRunner runner,
			int startIndex)
			throws JsonProcessingException {
		if (runner == null || startIndex == 0) {
			runner = new PLTEnsembleBoostedRunner(config);
			runner.soDataManager.loadNext(batchSize, runner.pltEnsembleBoosted.getId());
			runner.pltEnsembleBoosted.allocateClassifiers(runner.soDataManager);
		}
		// boolean toTrain = true;

		for (int index = startIndex; index < iterations; index++) {
			// toTrain = index == 0 || rbinom.sample() == 1;

			if (index > 0)
				runner.soDataManager.loadNext(batchSize, runner.pltEnsembleBoosted.getId());

			// if (toTrain)
			runner.pltEnsembleBoosted.train(runner.soDataManager);
			// else
			// runner.pltEnsembleBoosted.test(runner.soDataManager);

			runner.learnerRepository.update(runner.pltEnsembleBoosted.getId(), runner.pltEnsembleBoosted);

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
			PLTEnsembleBoostedInitConfiguration config, PLTEnsembleBoostedRunner runner, StringBuilder verbose,
			StringBuilder tex)
			throws JsonProcessingException {

		double trainMacroFm = runner.pltEnsembleBoosted.getMacroFmeasure();
		double trainAvgTime = runner.pltEnsembleBoosted.getAverageTrainTime();

		double prequestialAvgFm = runner.pltEnsembleBoosted.getAverageFmeasure(true, false);
		double prequestialAvgFmTopk = runner.pltEnsembleBoosted.getAverageFmeasure(true, true);
		double prequestialEvalAvgTime = runner.pltEnsembleBoosted.getAverageEvaluationTime(true, false);
		double prequestialEvalAvgTimeTopk = runner.pltEnsembleBoosted.getAverageEvaluationTime(true, true);

		double avgFm = runner.pltEnsembleBoosted.getAverageFmeasure(false, false);
		double avgFmTopk = runner.pltEnsembleBoosted.getAverageFmeasure(false, true);
		double evalAvgTime = runner.pltEnsembleBoosted.getAverageEvaluationTime(false, false);
		double evalAvgTimeTopk = runner.pltEnsembleBoosted.getAverageEvaluationTime(false, true);

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
		tex.append(config.isToAggregateByMajorityVote() ? "MV" : "FM");
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

	private PLTEnsembleBoostedInitConfiguration getLearnerConfiguration(ObjectNode runConfig)
			throws JsonProcessingException {

		PLTEnsembleBoostedInitConfiguration config = (new ObjectMapper()).treeToValue(
				runConfig.get("ensembleBoostedProperties"),
				PLTEnsembleBoostedInitConfiguration.class);

		config.fmeasureObserver = this.soDataManager;
		config.learnerRepository = this.learnerRepository;

		return config;
	}
}
