package thesis.simulators;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Files;

import Learner.AdaptivePLT;
import Learner.PLT;
import Learner.PLTEnsembleBoostedGeneric;
import thesis.data.managers.ArffDataManager;
import thesis.data.repositories.LearnerRepository;
import thesis.util.JsonSerializer;
import thesis.util.TextFileWriter;
import threshold.ThresholdTunerInitOption;
import threshold.ThresholdTuners;
import util.AdaptivePLTInitConfiguration;
import util.BoostingStrategy;
import util.Constants.PLTEnsembleBoostedGenericDefaultValues;
import util.PLTEnsembleBoostedGenericInitConfiguration;
import util.PLTInitConfiguration;

public class PLTEnsembleBoostedGenericArffRunner {
	private static final String simulationResultExt = ".txt";
	private static final String configurationExt = ".json";
	private static final String configurationPath = "../learnerConfigurations/";
	private static final String basePathForEnsembleResults = "../results/";
	private static final String iterationsArg = "-itr";
	private static final String batchSizeArg = "-bs";
	private static final String hdArg = "-hd";
	private static final String ensembleSizeArg = "-es";
	private static final String majorityVoteArg = "-mv";
	private static final String lambdaCWArg = "-lcw";
	private static final String resumeArg = "-r";
	private static final String writeResultArg = "-wres";
	private static final String minEpochsArg = "-me";
	private static final String boostingStrategyArg = "-bst";
	private static final String kslckArg = "-kslck";
	private static final String arffFilePathArg = "-data";
	private static final String labelPrefixArg = "-lprfx";
	private static final String pathToPropFileArg = "-prop";
	private static final String propKeyArg = "-key";
	private static final String simulationResultPathArg = "-resPath";
	private static final String texPathArg = "-texPath";
	private static final String pltTypeArg = "-pt";
	private static Logger logger;
	static {
		System.setProperty("initiator", PLTEnsembleBoostedGenericArffRunner.class.getSimpleName());
		System.setProperty("current.date", (new SimpleDateFormat("yyyy-MM-dd HHmmss")).format(new Date()));
		System.setProperty("log4j.configurationFile", "META-INF/log4j.xml");
		logger = LogManager.getLogger(PLTEnsembleBoostedGenericArffRunner.class);
	}
	LearnerRepository learnerRepository;
	ArffDataManager soDataManager;

	private static boolean toWriteResult = false;

	PLTEnsembleBoostedGeneric<?> pltEnsembleBoostedGeneric;
	private static String arffFilePath;
	private static String labelPrefix;
	private static String pathToPropFile = null;
	private static String propKey = null;
	static ObjectMapper simpleObjectMapper = new ObjectMapper();
	// private static int datasetSize;
	// private static int numFeatures;
	// private static int numLabels;
	private static String simulationResultPath;
	private static String texPath;
	private static PLTType pltType;

	public PLTEnsembleBoostedGenericArffRunner(ObjectNode runConfig) throws FileNotFoundException, IOException {

		initRepositories();
		initSODataManage();
		PLTEnsembleBoostedGenericInitConfiguration properties = getLearnerConfiguration(runConfig);

		switch (pltType) {
		case plt:
			pltEnsembleBoostedGeneric = new PLTEnsembleBoostedGeneric<PLT>(properties);
			break;
		case aplt:
			pltEnsembleBoostedGeneric = new PLTEnsembleBoostedGeneric<AdaptivePLT>(properties);
			break;
		}

		final UUID learnerId = learnerRepository.create(pltEnsembleBoostedGeneric);

		JsonSerializer.writeToFile(getConfigurationFilePath(learnerId), properties);

		if (propKey != null && !propKey.isEmpty() && pathToPropFile != null && !pathToPropFile.isEmpty()) {
			Properties prop = new Properties();
			prop.load(new FileInputStream(pathToPropFile));
			prop.setProperty(propKey, learnerId.toString());
			prop.store(new FileOutputStream(pathToPropFile), null);
		}
	}

	public PLTEnsembleBoostedGenericArffRunner(PLTEnsembleBoostedGenericInitConfiguration config)
			throws FileNotFoundException, IOException {

		initRepositories();
		initSODataManage();

		config.fmeasureObserver = this.soDataManager;
		config.learnerRepository = this.learnerRepository;

		switch (pltType) {
		case plt:
			pltEnsembleBoostedGeneric = new PLTEnsembleBoostedGeneric<PLT>(config);
			break;
		case aplt:
			pltEnsembleBoostedGeneric = new PLTEnsembleBoostedGeneric<AdaptivePLT>(config);
			break;
		}

		UUID learnerId = learnerRepository.create(pltEnsembleBoostedGeneric);

		JsonSerializer.writeToFile(getConfigurationFilePath(learnerId), config);

		if (propKey != null && !propKey.isEmpty() && pathToPropFile != null && !pathToPropFile.isEmpty()) {
			Properties prop = new Properties();
			prop.load(new FileInputStream(pathToPropFile));
			prop.setProperty(propKey, learnerId.toString());
			prop.store(new FileOutputStream(pathToPropFile), null);
		}
	}

	public PLTEnsembleBoostedGenericArffRunner(UUID learnerId) {

		initRepositories();
		initSODataManage();

		pltEnsembleBoostedGeneric = learnerRepository.read(learnerId, PLTEnsembleBoostedGeneric.class);
		if (pltEnsembleBoostedGeneric.fmeasureObserverAvailable) {
			pltEnsembleBoostedGeneric.fmeasureObserver = soDataManager;
			pltEnsembleBoostedGeneric.addInstanceProcessedListener(soDataManager);
			pltEnsembleBoostedGeneric.addInstanceTestedListener(soDataManager);
		}
		pltEnsembleBoostedGeneric.setLearnerRepository(learnerRepository);
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
						.containsAll(Arrays.asList(arffFilePathArg, labelPrefixArg, iterationsArg, hdArg,
								ensembleSizeArg, minEpochsArg, pltTypeArg, boostingStrategyArg))
						&&
						(configMap.keySet()
								.contains(majorityVoteArg) || configMap.keySet()
										.contains(lambdaCWArg)))
						|| configMap.keySet()
								.containsAll(Arrays.asList(arffFilePathArg, labelPrefixArg, iterationsArg, resumeArg))))
					throw new IllegalArgumentException(
							"One of two following sets of arguments are needed:"
									+ "\n\t 1: path to arff data file (-data), label prefix in data file (-lprfx), plt type (-pt, allowed values are plt, and aplt), boosting strategy (-bst, allowed values are fluent, threshold)"
									+ "\n\t\t iterations (-itr), hashed dimension (-hd), ensemble size (-es), number of minimum epochs (-me), "
									+ "\n\t\t aggregation options: by majority voting (-mv) or by aggregate by lambda cw (-lcw)"
									+ "\n\t 2: path to arff data file (-data), label prefix in data file (-lprfx), iterations (-itr), learner to resume (-r)");
				logger.info("Input arguments: " + configMap);

				pltType = PLTType.valueOf(configMap.get(pltTypeArg));
				if (pltType == null)
					throw new IllegalArgumentException("Invalid plt type");

				arffFilePath = configMap.get(arffFilePathArg);
				labelPrefix = configMap.get(labelPrefixArg);
				iterations = Integer.parseInt(configMap.get(iterationsArg));

				if (configMap.containsKey(batchSizeArg))
					batchSize = Integer.parseInt(configMap.get(batchSizeArg));
				else
					logger.info("Batch size not provided, continuing with default batch size of 1");

				if (configMap.containsKey(pathToPropFileArg))
					pathToPropFile = configMap.get(pathToPropFileArg);
				if (configMap.containsKey(propKeyArg))
					propKey = configMap.get(propKeyArg);

				if (configMap.containsKey(writeResultArg))
					toWriteResult = Boolean.parseBoolean(configMap.get(writeResultArg));

				if (toWriteResult) {
					if (!configMap.containsKey(simulationResultPathArg) || !configMap.containsKey(texPathArg)) {
						throw new IllegalArgumentException(
								"To write result, provide value for following to parameters: path to write simulation results (-resPath), path to tex file (-texPath)");
					}
					simulationResultPath = configMap.get(simulationResultPathArg);
					texPath = configMap.get(texPathArg);

					// String[] nameParts =
					// Files.getNameWithoutExtension(arffFilePath)
					// .split("_");
					// if (nameParts.length >= 4) {
					// datasetSize = Integer.parseInt(nameParts[1]);
					// numFeatures = Integer.parseInt(nameParts[2]);
					// numLabels = Integer.parseInt(nameParts[3]);
					// }
				}
			}
			PLTEnsembleBoostedGenericArffRunner runner = null;

			if (config == null && configMap == null)
				throw new IllegalArgumentException("Invalid run configuration");

			if (configMap == null)
				runSimulation(iterations, batchSize, rbinom,
						(new ObjectMapper()).treeToValue(config,
								PLTEnsembleBoostedGenericInitConfiguration.class),
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
							: PLTEnsembleBoostedGenericDefaultValues.isToAggregateByMajorityVote;
					final boolean aggrLambdaCW = configMap.containsKey(majorityVoteArg)
							? Boolean.parseBoolean(configMap.get(lambdaCWArg))
							: PLTEnsembleBoostedGenericDefaultValues.isToAggregateByLambdaCW;

					PLTInitConfiguration pltConfig = getConstantPLTConfig();
					PLTEnsembleBoostedGenericInitConfiguration ensembleConfig = getConstantEnsembleConfig();

					if (configMap.containsKey(kslckArg))
						ensembleConfig.setkSlack(Integer.parseInt(configMap.get(kslckArg)));

					ensembleConfig.individualPLTConfiguration = pltConfig;

					pltConfig.setHd(hd);

					ensembleConfig.setBaseLearnerClass(getPLTType());
					ensembleConfig.setBoostingStrategy(BoostingStrategy.valueOf(configMap.get(boostingStrategyArg)));
					ensembleConfig.setMinEpochs(minEpoch);
					ensembleConfig.setEnsembleSize(ensembleSize);
					ensembleConfig.setToAggregateByMajorityVote(aggrPref);
					ensembleConfig.setToAggregateByLambdaCW(aggrLambdaCW);

					runSimulation(iterations, batchSize, rbinom, ensembleConfig,
							MessageFormat.format(
									"PLTEnsembleBoostedGeneric_{0,number,#}_{1,number,#}_{2,number,#}_{3}_{4}_{5}",
									iterations, batchSize, hd, ensembleSize,
									aggrPref ? "MV" : (aggrLambdaCW ? "LCW" : "FM"), minEpoch),
							runner);
				}
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private static PLTEnsembleBoostedGenericInitConfiguration getConstantEnsembleConfig() {
		PLTEnsembleBoostedGenericInitConfiguration ensembleConfig = new PLTEnsembleBoostedGenericInitConfiguration();
		ensembleConfig.setMaxBranchingFactor(10);
		ensembleConfig.tunerInitOption = new ThresholdTunerInitOption(1, 2);
		ensembleConfig.setMeasureTime(true);
		return ensembleConfig;
	}

	private static PLTInitConfiguration getConstantPLTConfig() {
		PLTInitConfiguration pltConfig = null;
		switch (pltType) {
		case plt:
			pltConfig = new PLTInitConfiguration();
			pltConfig.setHasher("AdaptiveMurmur");
			pltConfig.tunerType = ThresholdTuners.AdaptiveOfoFast;
			pltConfig.tunerInitOption = new ThresholdTunerInitOption(1, 2);
			pltConfig.setLambda(1);
			pltConfig.setGamma(1);
			break;

		case aplt:
			pltConfig = new AdaptivePLTInitConfiguration();
			((AdaptivePLTInitConfiguration) pltConfig).setToPreferHighestProbLeaf(true);
			pltConfig.setHasher("AdaptiveMurmur");
			pltConfig.tunerType = ThresholdTuners.AdaptiveOfoFast;
			pltConfig.tunerInitOption = new ThresholdTunerInitOption(1, 2);
			pltConfig.setLambda(1);
			pltConfig.setGamma(1);
			break;
		}
		return pltConfig;
	}

	private static void resumeSimulation(int iterations, int batchSize, BinomialDistribution rbinom,
			PLTEnsembleBoostedGenericArffRunner runner, UUID learnerId)
			throws JsonParseException, JsonMappingException, IOException {
		runner = new PLTEnsembleBoostedGenericArffRunner(learnerId);

		PLTEnsembleBoostedGenericInitConfiguration config = JsonSerializer.readFromFile(
				getConfigurationFilePath(learnerId), PLTEnsembleBoostedGenericInitConfiguration.class);

		runSimulation(iterations, batchSize, rbinom, config,
				MessageFormat.format(
						"PLTEnsembleBoostedGeneric_{0,number,#}_{1,number,#}_{2,number,#}_{3}_{4}_{5}",
						iterations, batchSize, config.individualPLTConfiguration.getHd(), config.getEnsembleSize(),
						config.isToAggregateByMajorityVote() ? "MV" : (config.isToAggregateByLambdaCW() ? "LCW" : "FM"),
						config.getMinEpochs()),
				runner, runner.pltEnsembleBoostedGeneric.getnTrain());
	}

	private static void runSimulation(int iterations, int batchSize, BinomialDistribution rbinom,
			PLTEnsembleBoostedGenericInitConfiguration config, String fileName,
			PLTEnsembleBoostedGenericArffRunner runner)
			throws FileNotFoundException, IOException {
		runSimulation(iterations, batchSize, rbinom, config, fileName, runner, 0);
	}

	private static void runSimulation(int iterations, int batchSize, BinomialDistribution rbinom,
			PLTEnsembleBoostedGenericInitConfiguration config, String fileName,
			PLTEnsembleBoostedGenericArffRunner runner,
			int startIndex)
			throws FileNotFoundException, IOException {
		if (runner == null || startIndex == 0) {
			runner = new PLTEnsembleBoostedGenericArffRunner(config);
			runner.soDataManager.loadNext(batchSize);
			runner.pltEnsembleBoostedGeneric.allocateClassifiers(runner.soDataManager);
		}
		// boolean toTrain = true;
		if (startIndex > 0) {
			runner.soDataManager.setCurrentRowIndex(startIndex - 1);
		}
		for (int index = startIndex; index < iterations; index++) {
			// toTrain = index == 0 || rbinom.sample() == 1;

			if (index > 0)
				runner.soDataManager.loadNext(batchSize);

			// if (toTrain)
			runner.pltEnsembleBoostedGeneric.train(runner.soDataManager);
			// else
			// runner.pltEnsembleBoosted.test(runner.soDataManager);

			runner.learnerRepository.update(runner.pltEnsembleBoostedGeneric.getId(),
					runner.pltEnsembleBoostedGeneric);

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
			PLTEnsembleBoostedGenericInitConfiguration config, PLTEnsembleBoostedGenericArffRunner runner,
			StringBuilder verbose,
			StringBuilder tex)
			throws JsonProcessingException {

		double trainMacroFm = runner.pltEnsembleBoostedGeneric.getMacroFmeasure();
		double trainAvgTime = runner.pltEnsembleBoostedGeneric.getAverageTrainTime();

		double prequestialAvgFm = runner.pltEnsembleBoostedGeneric.getAverageFmeasure(true, false);
		double prequestialAvgFmTopk = runner.pltEnsembleBoostedGeneric.getAverageFmeasure(true, true);
		double prequestialEvalAvgTime = runner.pltEnsembleBoostedGeneric.getAverageEvaluationTime(true, false);
		double prequestialEvalAvgTimeTopk = runner.pltEnsembleBoostedGeneric.getAverageEvaluationTime(true, true);

		double avgFm = runner.pltEnsembleBoostedGeneric.getAverageFmeasure(false, false);
		double avgFmTopk = runner.pltEnsembleBoostedGeneric.getAverageFmeasure(false, true);
		double evalAvgTime = runner.pltEnsembleBoostedGeneric.getAverageEvaluationTime(false, false);
		double evalAvgTimeTopk = runner.pltEnsembleBoostedGeneric.getAverageEvaluationTime(false, true);

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

		// tex.append(datasetSize);
		// tex.append(" & ");
		// tex.append(numFeatures);
		// tex.append(" & ");
		// tex.append(numLabels);
		// tex.append(" & ");
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
		learnerRepository = new LearnerRepository(true);
	}

	private void initSODataManage() {
		soDataManager = new ArffDataManager(arffFilePath, labelPrefix, basePathForEnsembleResults);
	}

	private PLTEnsembleBoostedGenericInitConfiguration getLearnerConfiguration(ObjectNode runConfig)
			throws JsonProcessingException {

		PLTEnsembleBoostedGenericInitConfiguration config = (new ObjectMapper()).treeToValue(
				runConfig.get("ensembleBoostedProperties"),
				PLTEnsembleBoostedGenericInitConfiguration.class);

		config.fmeasureObserver = this.soDataManager;
		config.learnerRepository = this.learnerRepository;

		return config;
	}

	private static String getConfigurationFilePath(UUID learnerId) {
		return configurationPath + learnerId.toString() + configurationExt;
	}

	private static Class<?> getPLTType() {
		switch (pltType) {
		case plt:
			return PLT.class;
		case aplt:
			return AdaptivePLT.class;
		default:
			throw new IllegalArgumentException("Invalid plt type");
		}
	}

	enum PLTType {
		plt, aplt
	}
}
