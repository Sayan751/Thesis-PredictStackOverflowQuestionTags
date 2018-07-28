package thesis.simulators;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import Data.AVPair;
import Learner.PLT;
import thesis.data.managers.ArffDataManager;
import thesis.data.repositories.LearnerRepository;
import thesis.util.JsonSerializer;
import thesis.util.TextFileWriter;
import threshold.ThresholdTunerInitOption;
import threshold.ThresholdTuners;
import util.PLTInitConfiguration;

public class PLTArffRunner {
	private static final String simulationResultExt = ".txt";
	private static final String configurationExt = ".json";
	private static final String configurationPath = "../learnerConfigurations/";
	private static final String simulationResultPath = "../simulationResults/PLTArff/";
	private static final String texPath = "../simulationResults/pltArffData.tex";
	private static final String iterationsArg = "-itr";
	private static final String batchSizeArg = "-bs";
	private static final String hdArg = "-hd";
	private static final String resumeArg = "-r";
	private static final String writeResultArg = "-wres";
	private static final String arffFilePathArg = "-data";
	private static final String labelPrefixArg = "-lprfx";
	private static final String fmeasureResultFilePathArg = "-result";
	private static final String resultRelationNameArg = "-resrel";
	private static final String pathToPropFileArg = "-prop";
	private static final String propKeyArg = "-key";
	private static final String branchingFactorArg = "-k";

	private static Logger logger;
	static {
		System.setProperty("initiator", PLTArffRunner.class.getSimpleName());
		System.setProperty("current.date", (new SimpleDateFormat("yyyy-MM-dd HHmmss")).format(new Date()));
		System.setProperty("log4j.configurationFile", "META-INF/log4j.xml");
		logger = LogManager.getLogger(PLTArffRunner.class);
	}

	LearnerRepository learnerRepository;
	ArffDataManager soDataManager;

	public PLT plt;
	private static String arffFilePath;
	private static String labelPrefix;
	private static String fmeasureResultFilePath;
	private static String resultRelationName = "Result";
	private static String pathToPropFile = null;
	private static String propKey = null;
	static ObjectMapper simpleObjectMapper = new ObjectMapper();
	private static boolean toWriteResult;
	// private static int datasetSize;
	// private static int numFeatures;
	// private static int numLabels;

	public PLTArffRunner(PLTInitConfiguration config) throws IOException {
		initRepositories();
		initSODataManager();

		config.fmeasureObserver = this.soDataManager;
		plt = new PLT(config);
		UUID learnerId = learnerRepository.create(plt);
		JsonSerializer.writeToFile(getConfigurationFilePath(learnerId), config);

		if (propKey != null && !propKey.isEmpty() && pathToPropFile != null && !pathToPropFile.isEmpty()) {
			Properties prop = new Properties();
			prop.load(new FileInputStream(pathToPropFile));
			prop.setProperty(propKey, learnerId.toString());
			prop.store(new FileOutputStream(pathToPropFile), null);
		}
	}

	public PLTArffRunner(UUID learnerId) {
		initRepositories();
		initSODataManager();

		plt = learnerRepository.read(learnerId, PLT.class);
		if (plt.fmeasureObserverAvailable) {
			plt.fmeasureObserver = soDataManager;
			plt.addInstanceProcessedListener(soDataManager);
			plt.addInstanceTestedListener(soDataManager);
		}
	}

	public static void main(String[] args) {
		try {
			if ((args.length & 1) != 0)
				throw new IllegalArgumentException("Invalid number of arguments provided");

			Map<String, String> configMap = new HashMap<>();
			for (int i = 0; i < args.length; i += 2) {
				configMap.put(args[i], args[i + 1]);
			}

			if (!(configMap.keySet()
					.containsAll(Arrays
							.asList(arffFilePathArg, labelPrefixArg, fmeasureResultFilePathArg, iterationsArg, hdArg))
					|| configMap.keySet()
							.containsAll(Arrays.asList(arffFilePathArg, labelPrefixArg, fmeasureResultFilePathArg,
									iterationsArg, resumeArg))))
				throw new IllegalArgumentException(
						"One of two following sets of arguments are needed:\n\t "
								+ "1: path to arff data file (-data), label prefix in data file (-lprfx), path to the arff file to store the fmeasure results (-result), iterations (-itr), hashed dimension (-hd)\n\t "
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

			int iterations = Integer.parseInt(configMap.get(iterationsArg));
			int batchSize = 1;
			if (configMap.containsKey(batchSizeArg))
				batchSize = Integer.parseInt(configMap.get(batchSizeArg));
			else
				logger.info("Batch size not provided, continuing with default batch size of 1");

			if (configMap.containsKey(writeResultArg))
				toWriteResult = Boolean.parseBoolean(configMap.get(writeResultArg));

			// if (toWriteResult) {
			// // File dataFile = new File(arffFilePath);
			// // String[] nameParts = dataFile.getName().split("_");
			// String[] nameParts = Files.getNameWithoutExtension(arffFilePath)
			// .split("_");
			// if (nameParts.length >= 4) {
			// datasetSize = Integer.parseInt(nameParts[1]);
			// numFeatures = Integer.parseInt(nameParts[2]);
			// numLabels = Integer.parseInt(nameParts[3]);
			// }
			// }

			PLTArffRunner runner = null;

			if (configMap.containsKey(resumeArg)) {
				UUID learnerId = UUID.fromString(configMap.get(resumeArg));
				logger.info("Resuming for learnerId:" + learnerId);
				resumeSimulation(iterations, batchSize, runner, learnerId);
			} else {
				PLTInitConfiguration simulationConfig = getConstantConfig();
				final int hd = Integer.parseInt(configMap.get(hdArg));
				simulationConfig.setHd(hd);

				if (configMap.containsKey(branchingFactorArg))
					simulationConfig.setK(Integer.parseInt(configMap.get(branchingFactorArg)));

				runSimulation(iterations, batchSize, simulationConfig,
						MessageFormat.format("PLT_{0,number,#}_{1,number,#}_{2,number,#}_{3,number,#}", iterations,
								batchSize, hd, simulationConfig.getK()),
						runner);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private static PLTInitConfiguration getConstantConfig() {
		PLTInitConfiguration simulationConfig = new PLTInitConfiguration();
		simulationConfig.tunerType = ThresholdTuners.OfoFast;
		simulationConfig.tunerInitOption = new ThresholdTunerInitOption(1, 2);
		simulationConfig.setLambda(1);
		simulationConfig.setGamma(1);
		simulationConfig.setEpochs(30);
		simulationConfig.setDefaultK(5);
		simulationConfig.setK(2);
		simulationConfig.setMeasureTime(true);
		simulationConfig.setHasher("Murmur");
		return simulationConfig;
	}

	private static void resumeSimulation(int iterations, int batchSize, PLTArffRunner runner, UUID learnerId)
			throws JsonParseException, JsonMappingException, IOException {

		runner = new PLTArffRunner(learnerId);

		PLTInitConfiguration config = JsonSerializer.readFromFile(getConfigurationFilePath(learnerId),
				PLTInitConfiguration.class);
		runSimulation(iterations, batchSize, config,
				MessageFormat.format("PLT_{0,number,#}_{1,number,#}_{2,number,#}_{3,number,#}", iterations, batchSize,
						config.getHd(), config.getK()),
				runner, runner.plt.getnTrain());
	}

	private static void runSimulation(int iterations, int batchSize, PLTInitConfiguration config, String fileName,
			PLTArffRunner runner) throws IOException {
		runSimulation(iterations, batchSize, config, fileName, runner, 0);
	}

	private static void runSimulation(int iterations, int batchSize, PLTInitConfiguration config, String fileName,
			PLTArffRunner runner, int startIndex) throws IOException {

		if (runner == null || startIndex == 0) {
			runner = new PLTArffRunner(config);
			runner.soDataManager.loadNext(batchSize);
			runner.plt.allocateClassifiers(runner.soDataManager);
		}
		if (startIndex > 0) {
			runner.soDataManager.setCurrentRowIndex(startIndex - 1);
		}

		for (int index = startIndex; index < iterations; index++) {
			if (index > 0)
				runner.soDataManager.loadNext(batchSize);

			runner.plt.train(runner.soDataManager);
			
			System.out.println(runner.plt.getTopKEstimatesComplete(runner.soDataManager.getNextInstance().x, 10000));
			
			runner.learnerRepository.update(runner.plt.getId(), runner.plt);
			logger.info("End of iteration " + (index + 1) + " of " + iterations);
		}

		if (toWriteResult) {
			StringBuilder verbose = new StringBuilder(), tex = new StringBuilder();
			buildSimulationInfo(iterations, batchSize, config, runner, verbose, tex);

			System.out.println(verbose.toString());
			System.out.println(tex.toString());
			if (fileName != null && !fileName.isEmpty()) {
				TextFileWriter.writeToFile(simulationResultPath + fileName + simulationResultExt, verbose.toString());
				TextFileWriter.writeToFile(texPath, tex.toString(), true);
				logger.info("End of simulation: " + fileName);
			} else
				logger.info(verbose.toString());
		}
	}

	private static void buildSimulationInfo(int iterations, int batchSize,
			PLTInitConfiguration config, PLTArffRunner runner, StringBuilder verbose, StringBuilder tex)
			throws JsonProcessingException {
		double trainMacroFm = runner.plt.getMacroFmeasure();
		double trainAvgTime = runner.plt.getAverageTrainTime();

		double prequestialAvgFm = runner.plt.getAverageFmeasure(true, false);
		double prequestialAvgFmTopk = runner.plt.getAverageFmeasure(true, true);
		double prequestialEvalAvgTime = runner.plt.getAverageEvaluationTime(true, false);
		double prequestialEvalAvgTimeTopk = runner.plt.getAverageEvaluationTime(true, true);

		double avgFm = runner.plt.getAverageFmeasure(false, false);
		double avgFmTopk = runner.plt.getAverageFmeasure(false, true);
		double evalAvgTime = runner.plt.getAverageEvaluationTime(false, false);
		double evalAvgTimeTopk = runner.plt.getAverageEvaluationTime(false, true);

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
		tex.append(config.getHd());
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

		tex.append(" \\\\ \\hline \n");
	}

	private static String getConfigurationFilePath(UUID learnerId) {
		return configurationPath + learnerId.toString() + configurationExt;
	}

	private void initRepositories() {
		learnerRepository = new LearnerRepository(true);
	}

	private void initSODataManager() {
		soDataManager = new ArffDataManager(arffFilePath, labelPrefix, fmeasureResultFilePath, resultRelationName);
	}
}
