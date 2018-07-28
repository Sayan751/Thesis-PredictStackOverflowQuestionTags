package thesis.simulators;

import java.io.File;
import java.io.FileInputStream;
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
import thesis.data.managers.ArffDataManager;
import thesis.data.repositories.LearnerRepository;
import thesis.util.JsonSerializer;
import thesis.util.TextFileWriter;
import threshold.ThresholdTunerInitOption;
import threshold.ThresholdTuners;
import util.AdaptivePLTInitConfiguration;

public class AdaptivePLTArffRunner {
	private static final String simulationResultExt = ".txt";
	private static final String configurationExt = ".json";
	private static final String configurationPath = "../learnerConfigurations/";
	private static final String simulationResultPath = "../simulationResults/AdaptivePLT/";
	private static final String texPath = "../simulationResults/adaptivePLTData.tex";
	private static final String iterationsArg = "-itr";
	private static final String batchSizeArg = "-bs";
	private static final String hdArg = "-hd";
	private static final String alphaArg = "-alpha";
	private static final String hpArg = "-hp";
	private static final String resumeArg = "-r";
	private static final String writeResultArg = "-wres";
	private static final String branchingFactorArg = "-k";
	private static final String arffFilePathArg = "-data";
	private static final String labelPrefixArg = "-lprfx";
	private static final String fmeasureResultFilePathArg = "-result";
	private static final String resultRelationNameArg = "-resrel";
	private static final String pathToPropFileArg = "-prop";
	private static final String propKeyArg = "-key";

	private static Logger logger;
	static {
		System.setProperty("initiator", AdaptivePLTArffRunner.class.getSimpleName());
		System.setProperty("current.date", (new SimpleDateFormat("yyyy-MM-dd HHmmss")).format(new Date()));
		System.setProperty("log4j.configurationFile", "META-INF/log4j.xml");
		logger = LogManager.getLogger(AdaptivePLTArffRunner.class);
	}

	LearnerRepository learnerRepository;
	ArffDataManager soDataManager;
	private static boolean toWriteResult = false;

	public AdaptivePLT adaptivePlt;
	private static String arffFilePath;
	private static String labelPrefix;
	private static String fmeasureResultFilePath;
	private static String resultRelationName = "Result";
	private static String pathToPropFile = null;
	private static String propKey = null;
	static ObjectMapper simpleObjectMapper = new ObjectMapper();
	// private static int datasetSize;
	// private static int numFeatures;
	// private static int numLabels;

	public AdaptivePLTArffRunner(ObjectNode runConfig) throws IOException {
		initRepositories();
		initSODataManage();

		AdaptivePLTInitConfiguration config = getLearnerConfiguration(runConfig);

		adaptivePlt = new AdaptivePLT(config);
		UUID learnerId = learnerRepository.create(adaptivePlt);
		JsonSerializer.writeToFile(getConfigurationFilePath(learnerId), config);

		if (propKey != null && !propKey.isEmpty() && pathToPropFile != null && !pathToPropFile.isEmpty()) {
			Properties prop = new Properties();
			prop.load(new FileInputStream(pathToPropFile));
			prop.setProperty(propKey, learnerId.toString());
			prop.store(new FileOutputStream(pathToPropFile), null);
		}
	}

	public AdaptivePLTArffRunner(AdaptivePLTInitConfiguration config) throws IOException {
		initRepositories();
		initSODataManage();

		config.fmeasureObserver = this.soDataManager;
		adaptivePlt = new AdaptivePLT(config);
		UUID learnerId = learnerRepository.create(adaptivePlt);
		JsonSerializer.writeToFile(getConfigurationFilePath(learnerId), config);

		if (propKey != null && !propKey.isEmpty() && pathToPropFile != null && !pathToPropFile.isEmpty()) {
			Properties prop = new Properties();
			prop.load(new FileInputStream(pathToPropFile));
			prop.setProperty(propKey, learnerId.toString());
			prop.store(new FileOutputStream(pathToPropFile), null);
		}
	}

	public AdaptivePLTArffRunner(UUID learnerId) {
		initRepositories();
		initSODataManage();

		adaptivePlt = learnerRepository.read(learnerId, AdaptivePLT.class);
		if (adaptivePlt.fmeasureObserverAvailable) {
			adaptivePlt.fmeasureObserver = soDataManager;
			adaptivePlt.addInstanceProcessedListener(soDataManager);
			adaptivePlt.addInstanceTestedListener(soDataManager);
		}
	}

	public static void main(String[] args) {
		try {
			Map<String, String> configMap = null;
			BinomialDistribution rbinom = null;
			int iterations, batchSize = 1;
			JsonNode pltConfig = null;

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
				pltConfig = runConfig.get("PLTProperties");

			} else {
				if ((args.length & 1) != 0)
					throw new IllegalArgumentException("Invalid number of arguments provided");
				configMap = new HashMap<>();
				for (int i = 0; i < args.length; i += 2) {
					configMap.put(args[i], args[i + 1]);
				}

				if (!(configMap.keySet()
						.containsAll(Arrays.asList(arffFilePathArg,
								labelPrefixArg, fmeasureResultFilePathArg, iterationsArg, hdArg, alphaArg, hpArg))
						|| configMap.keySet()
								.containsAll(Arrays.asList(arffFilePathArg, labelPrefixArg, fmeasureResultFilePathArg,
										iterationsArg, resumeArg))))
					throw new IllegalArgumentException(
							"One of two following sets of arguments are needed:\n\t "
									+ "1: path to arff data file (-data), label prefix in data file (-lprfx), path to the arff file to store the fmeasure results (-result), iterations (-itr), hashed dimension (-hd), alpha (-alpha), prefer node with highest probability (-hp)\n\t "
									+ "2: path to arff data file (-data), label prefix in data file (-lprfx), path to the arff file to store the fmeasure results (-result), iterations (-itr), learner to resume (-r)");

				logger.info("Input arguments: " + configMap);
				arffFilePath = configMap.get(arffFilePathArg);
				labelPrefix = configMap.get(labelPrefixArg);
				fmeasureResultFilePath = configMap.get(fmeasureResultFilePathArg);
				if (configMap.containsKey(resultRelationNameArg))
					resultRelationName = configMap.get(resultRelationNameArg);

				if (configMap.containsKey(pathToPropFileArg))
					pathToPropFile = configMap.get(pathToPropFileArg);
				if (configMap.containsKey(propKeyArg))
					propKey = configMap.get(propKeyArg);

				iterations = Integer.parseInt(configMap.get(iterationsArg));
				if (configMap.containsKey(batchSizeArg))
					batchSize = Integer.parseInt(configMap.get(batchSizeArg));
				else
					logger.info("Batch size not provided, continuing with default batch size of 1");

				if (configMap.containsKey(writeResultArg))
					toWriteResult = Boolean.parseBoolean(configMap.get(writeResultArg));

				// if (toWriteResult) {
				// // File dataFile = new File(arffFilePath);
				// // String[] nameParts = dataFile.getName().split("_");
				// String[] nameParts =
				// Files.getNameWithoutExtension(arffFilePath)
				// .split("_");
				// if (nameParts.length >= 4) {
				// datasetSize = Integer.parseInt(nameParts[1]);
				// numFeatures = Integer.parseInt(nameParts[2]);
				// numLabels = Integer.parseInt(nameParts[3]);
				// }
				// }
			}

			AdaptivePLTArffRunner runner = null;

			if (pltConfig != null || configMap != null) {
				if (configMap == null)
					runSimulation(iterations, batchSize, rbinom,
							(new ObjectMapper()).treeToValue(pltConfig, AdaptivePLTInitConfiguration.class), null,
							runner);
				else {
					if (configMap.containsKey(resumeArg)) {
						UUID learnerId = UUID.fromString(configMap.get(resumeArg));
						logger.info("Resuming for learnerId:" + learnerId);
						resumeSimulation(iterations, batchSize, rbinom, runner, learnerId);
					} else {

						final int hd = Integer.parseInt(configMap.get(hdArg));
						final double alpha = Double.parseDouble(configMap.get(alphaArg));
						final boolean probPref = Boolean.parseBoolean(configMap.get(hpArg));

						AdaptivePLTInitConfiguration simulationConfig = getConstantConfig();
						simulationConfig.setHd(hd);
						simulationConfig.setAlpha(alpha);
						simulationConfig.setToPreferHighestProbLeaf(probPref);
						if (configMap.containsKey(branchingFactorArg))
							simulationConfig.setK(Integer.parseInt(configMap.get(branchingFactorArg)));

						runSimulation(iterations, batchSize, rbinom, simulationConfig,
								MessageFormat.format(
										"AdaptivePLT_{0,number,#}_{1,number,#}_{2,number,#}_{3}_{4}_{5,number,#}",
										iterations, batchSize, hd, alpha, probPref, simulationConfig.getK()),
								runner);
					}
				}
			} else {
				// run series of simulations
				final int[] hds = { 16384, 32768 };
				final double[] alphas = { 0.4, 0.5, 0.85, 1 };
				final boolean[] preferNodeWithHighestProbability = { true, false };

				File texFile = new File(texPath);
				if (texFile.exists())
					texFile.delete();

				AdaptivePLTInitConfiguration simulationConfig = getConstantConfig();

				for (int hd : hds) {
					for (double alpha : alphas) {
						for (boolean probPref : preferNodeWithHighestProbability) {

							simulationConfig.setHd(hd);
							simulationConfig.setAlpha(alpha);
							simulationConfig
									.setToPreferHighestProbLeaf(probPref);

							runSimulation(iterations, batchSize, rbinom, simulationConfig,
									MessageFormat.format(
											"AdaptivePLT_{0,number,#}_{1,number,#}_{2,number,#}_{3}_{4}",
											iterations, batchSize, hd, alpha, probPref),
									runner);

							// }
						}
					}
				}
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private static AdaptivePLTInitConfiguration getConstantConfig() {
		AdaptivePLTInitConfiguration simulationConfig = new AdaptivePLTInitConfiguration();
		simulationConfig.tunerType = ThresholdTuners.AdaptiveOfoFast;
		simulationConfig.tunerInitOption = new ThresholdTunerInitOption(1, 2);
		simulationConfig.setLambda(1);
		simulationConfig.setGamma(1);
		simulationConfig.setEpochs(30);
		simulationConfig.setDefaultK(5);
		simulationConfig.setK(2);
		simulationConfig.setMeasureTime(true);
		simulationConfig.setHasher("AdaptiveMurmur");
		return simulationConfig;
	}

	private static void resumeSimulation(int iterations, int batchSize, BinomialDistribution rbinom,
			AdaptivePLTArffRunner runner, UUID learnerId) throws JsonParseException, JsonMappingException, IOException {
		runner = new AdaptivePLTArffRunner(learnerId);

		AdaptivePLTInitConfiguration config = JsonSerializer.readFromFile(getConfigurationFilePath(learnerId),
				AdaptivePLTInitConfiguration.class);
		runSimulation(iterations, batchSize, rbinom, config,
				MessageFormat.format("AdaptivePLT_{0,number,#}_{1,number,#}_{2,number,#}_{3}_{4}_{5,number,#}",
						iterations, batchSize, config.getHd(), config.getAlpha(), config.isToPreferHighestProbLeaf(),
						config.getK()),
				runner, runner.adaptivePlt.getnTrain());
	}

	private static void runSimulation(int iterations, int batchSize, BinomialDistribution rbinom,
			AdaptivePLTInitConfiguration config, String fileName, AdaptivePLTArffRunner runner)
			throws IOException {
		runSimulation(iterations, batchSize, rbinom, config, fileName, runner, 0);
	}

	private static void runSimulation(int iterations, int batchSize, BinomialDistribution rbinom,
			AdaptivePLTInitConfiguration config, String fileName, AdaptivePLTArffRunner runner, int startIndex)
			throws IOException {

		if (runner == null || startIndex == 0) {
			runner = new AdaptivePLTArffRunner(config);
			runner.soDataManager.loadNext(batchSize);
			runner.adaptivePlt.allocateClassifiers(runner.soDataManager);
		}
		if (startIndex > 0) {
			runner.soDataManager.setCurrentRowIndex(startIndex - 1);
		}
		// boolean toTrain = true;

		for (int index = startIndex; index < iterations; index++) {
			// toTrain = index == 0 || rbinom.sample() == 1;

			if (index > 0)
				runner.soDataManager.loadNext(batchSize);

			// if (toTrain)
			runner.adaptivePlt.train(runner.soDataManager);
			// else
			// runner.adaptivePlt.test(runner.soDataManager);
			runner.learnerRepository.update(runner.adaptivePlt.getId(), runner.adaptivePlt);

			logger.info("End of iteration " + (index + 1) + " of " + iterations);
		}

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
			AdaptivePLTInitConfiguration config, AdaptivePLTArffRunner runner, StringBuilder verbose, StringBuilder tex)
			throws JsonProcessingException {

		double trainMacroFm = runner.adaptivePlt.getMacroFmeasure();
		double trainAvgTime = runner.adaptivePlt.getAverageTrainTime();

		double prequestialAvgFm = runner.adaptivePlt.getAverageFmeasure(true, false);
		double prequestialAvgFmTopk = runner.adaptivePlt.getAverageFmeasure(true, true);
		double prequestialEvalAvgTime = runner.adaptivePlt.getAverageEvaluationTime(true, false);
		double prequestialEvalAvgTimeTopk = runner.adaptivePlt.getAverageEvaluationTime(true, true);

		double avgFm = runner.adaptivePlt.getAverageFmeasure(false, false);
		double avgFmTopk = runner.adaptivePlt.getAverageFmeasure(false, true);
		double evalAvgTime = runner.adaptivePlt.getAverageEvaluationTime(false, false);
		double evalAvgTimeTopk = runner.adaptivePlt.getAverageEvaluationTime(false, true);

		// double testMacroFm = runner.adaptivePlt.getTestMacroFmeasure(false);
		// double testMacroFmTopk =
		// runner.adaptivePlt.getTestMacroFmeasure(true);
		// double testAvgFm = runner.adaptivePlt.getTestAverageFmeasure(false);
		// double testAvgFmTopk =
		// runner.adaptivePlt.getTestAverageFmeasure(true);
		// double testAvgTime = runner.adaptivePlt.getAverageTestTime();

		verbose.append("Simulation Info:\n");
		verbose.append("================\n\n");

		verbose.append("Total number of instances: ");
		verbose.append(iterations * batchSize);
		verbose.append("\n");

		// double trainSplit = rbinom.getProbabilityOfSuccess() * 100;
		// verbose.append("Random train/test split: ");
		// verbose.append(trainSplit);
		// verbose.append("/");
		// verbose.append(100 - trainSplit);
		// verbose.append("\n");

		verbose.append("Simulation configuration: ");
		verbose.append(simpleObjectMapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(config));
		verbose.append("\n\n");

		verbose.append("Training info:\n");
		verbose.append("================\n");
		// verbose.append("Total number of training instances: ");
		// verbose.append(runner.adaptivePlt.getnTrain());
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

		// verbose.append("Testing info:\n");
		// verbose.append("================\n");
		// verbose.append("Total number of testing instances: ");
		// verbose.append(runner.adaptivePlt.getnTest());
		// verbose.append("\n");
		// verbose.append("Macro fmeasure: ");
		// verbose.append(testMacroFm);
		// verbose.append("\n");
		// verbose.append("Topk-based macro fmeasure: ");
		// verbose.append(testMacroFmTopk);
		// verbose.append("\n");
		// verbose.append("Average fmeasure: ");
		// verbose.append(testAvgFm);
		// verbose.append("\n");
		// verbose.append("Topk-based average fmeasure: ");
		// verbose.append(testAvgFmTopk);
		// verbose.append("\n");
		// verbose.append("Average testing time: ");
		// verbose.append(testAvgTime);
		// verbose.append(" \u00B5s\n");

		// tex.append(datasetSize);
		// tex.append(" & ");
		// tex.append(numFeatures);
		// tex.append(" & ");
		// tex.append(numLabels);
		// tex.append(" & ");
		tex.append(config.getHd());
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", config.getAlpha()));
		tex.append(" & ");
		tex.append(config.isToPreferHighestProbLeaf() ? "Y" : "N");
		tex.append(" & ");
		tex.append(config.getK());
		tex.append(" & ");

		tex.append(MessageFormat.format("{0,number,0.00}", prequestialAvgFm * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", prequestialEvalAvgTime));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", prequestialAvgFmTopk * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", prequestialEvalAvgTimeTopk));
		tex.append(" & ");

		tex.append(MessageFormat.format("{0,number,0.00}", trainMacroFm * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", trainAvgTime));
		tex.append(" & ");

		tex.append(MessageFormat.format("{0,number,0.00}", avgFm * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", evalAvgTime));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", avgFmTopk * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", evalAvgTimeTopk));

		// tex.append(MessageFormat.format("{0,number,0.00}", testMacroFm *
		// 100));
		// tex.append(" & ");
		// tex.append(MessageFormat.format("{0,number,0.00}", testMacroFmTopk *
		// 100));
		// tex.append(" & ");
		// tex.append(MessageFormat.format("{0,number,0.00}", testAvgFm * 100));
		// tex.append(" & ");
		// tex.append(MessageFormat.format("{0,number,0.00}", testAvgFmTopk *
		// 100));
		// tex.append(" & ");
		// tex.append(MessageFormat.format("{0,number,0.00}", testAvgTime));
		tex.append(" \\\\ \\hline \n");
	}

	private void initRepositories() {
		learnerRepository = new LearnerRepository(true);
	}

	private void initSODataManage() {
		soDataManager = new ArffDataManager(arffFilePath, labelPrefix, fmeasureResultFilePath, resultRelationName);
	}

	private AdaptivePLTInitConfiguration getLearnerConfiguration(ObjectNode runConfig) throws JsonProcessingException {

		AdaptivePLTInitConfiguration config = (new ObjectMapper()).treeToValue(runConfig.get("PLTProperties"),
				AdaptivePLTInitConfiguration.class);
		config.fmeasureObserver = this.soDataManager;

		return config;
	}

	private static String getConfigurationFilePath(UUID learnerId) {
		return configurationPath + learnerId.toString() + configurationExt;
	}
}
