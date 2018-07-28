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

import Data.Instance;
import Learner.AbstractLearner;
import Learner.PLTAdaptiveEnsemble;
import event.args.PLTCreationEventArgs;
import event.listeners.IPLTCreatedListener;
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
import util.PLTAdaptiveEnsembleAgeFunctions;
import util.PLTAdaptiveEnsembleInitConfiguration;
import util.PLTAdaptiveEnsemblePenalizingStrategies;
import util.PLTInitConfiguration;

public class PLTAdaptiveEnsembleRunner implements IPLTCreatedListener/*, IPLTDiscardedListener*/ {
	private static final String simulationResultExt = ".txt";
	private static final String simulationResultPath = "../simulationResults/PLTAdaptiveEnsemble/";
	private static final String texPath = "../simulationResults/adaptiveEnsembleData.tex";
	private static final String iterationsArg = "-itr";
	private static final String batchSizeArg = "-bs";
	private static final String ageFunctionArg = "-af";
	private static final String epsilonArg = "-ep";
	private static final String nMinArg = "-nm";
	private static final String retainmentArg = "-ret";
	private static final String resumeArg = "-r";
	private static final String writeRegistryArg = "-wreg";
	private static final String writeResultArg = "-wres";
	private static Logger logger;
	static {
		System.setProperty("initiator", PLTAdaptiveEnsembleRunner.class.getSimpleName());
		System.setProperty("current.date", (new SimpleDateFormat("yyyy-MM-dd HHmmss")).format(new Date()));
		System.setProperty("log4j.configurationFile", "META-INF/log4j.xml");
		logger = LogManager.getLogger(PLTAdaptiveEnsembleRunner.class);
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
	private static final String regKeyFormat = "adaptiveEnsemble-{0}-{1,number,0.000000}-{2}-{3,number,0.0}";

	PLTAdaptiveEnsemble pltEnsemble;
	Map<Instance, Map<AbstractLearner, Double>> questionLearnerDetails = new HashMap<Instance, Map<AbstractLearner, Double>>();
	static ObjectMapper simpleObjectMapper = new ObjectMapper();

	public PLTAdaptiveEnsembleRunner(ObjectNode runConfig) throws JsonProcessingException {

		initRepositories();
		initSODataManage();
		PLTAdaptiveEnsembleInitConfiguration configuration = getLearnerConfiguration(runConfig);

		pltEnsemble = new PLTAdaptiveEnsemble(configuration);
		pltEnsemble.addPLTCreatedListener(this);
		pltEnsemble.addPLTDiscardedListener(this.learnerRepository);

		Learner learner = learnerRepository.create(new Learner(runConfig.toString()), pltEnsemble);
		final UUID learnerId = learner.getId();
		pltEnsemble.setId(learnerId);

		if (toWriteRegistry)
			Preferences.userRoot()
					.node("thesis")
					.put(MessageFormat.format(regKeyFormat, configuration.getAgeFunction(), configuration.getEpsilon(),
							configuration.getMinTraingInstances(), configuration.getRetainmentFraction())
							.toLowerCase(), learnerId.toString());
	}

	public PLTAdaptiveEnsembleRunner(PLTAdaptiveEnsembleInitConfiguration config) throws JsonProcessingException {

		initRepositories();
		initSODataManage();

		config.fmeasureObserver = this.soDataManager;
		config.learnerRepository = this.learnerRepository;

		pltEnsemble = new PLTAdaptiveEnsemble(config);
		pltEnsemble.addPLTCreatedListener(this);
		pltEnsemble.addPLTDiscardedListener(this.learnerRepository);

		Learner learner = learnerRepository.create(new Learner(simpleObjectMapper.writeValueAsString(config)),
				pltEnsemble);
		final UUID learnerId = learner.getId();
		pltEnsemble.setId(learnerId);

		if (toWriteRegistry)
			Preferences.userRoot()
					.node("thesis")
					.put(MessageFormat.format(regKeyFormat, config.getAgeFunction(), config.getEpsilon(),
							config.getMinTraingInstances(), config.getRetainmentFraction())
							.toLowerCase(), learnerId.toString());
	}

	public PLTAdaptiveEnsembleRunner(UUID learnerId) {

		initRepositories();
		initSODataManage();

		pltEnsemble = learnerRepository.read(learnerId, PLTAdaptiveEnsemble.class);
		if (pltEnsemble.fmeasureObserverAvailable) {
			pltEnsemble.fmeasureObserver = soDataManager;
			pltEnsemble.addInstanceProcessedListener(soDataManager);
			pltEnsemble.addInstanceTestedListener(soDataManager);
		}
		pltEnsemble.setLearnerRepository(learnerRepository);
		pltEnsemble.addPLTCreatedListener(this);
		pltEnsemble.addPLTDiscardedListener(this.learnerRepository);
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
				config = runConfig.get("ensembleProperties");
			} else {
				if ((args.length & 1) != 0)
					throw new IllegalArgumentException("Invalid number of arguments provided");
				configMap = new HashMap<>();
				for (int i = 0; i < args.length; i += 2) {
					configMap.put(args[i], args[i + 1]);
				}

				if (!(configMap.keySet()
						.containsAll(Arrays.asList(iterationsArg, ageFunctionArg, epsilonArg, nMinArg, retainmentArg))
						|| configMap.keySet()
								.containsAll(Arrays.asList(iterationsArg, resumeArg))))
					throw new IllegalArgumentException(
							"One of two following sets of arguments are needed:\n\t 1: iterations (-itr), age function (-af), epsilon (-ep), N_min (-nm), retainment fraction(-ret)\n\t 2: iterations (-itr), learner to resume (-r)");

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
			PLTAdaptiveEnsembleRunner runner = null;

			if (config != null || configMap != null) {
				if (configMap == null)
					runSimulation(iterations, batchSize, rbinom,
							simpleObjectMapper.treeToValue(config, PLTAdaptiveEnsembleInitConfiguration.class), null,
							runner);
				else {
					if (configMap.containsKey(resumeArg)) {
						UUID learnerId = UUID.fromString(configMap.get(resumeArg));
						logger.info("Resuming for learnerId:" + learnerId);
						resumeSimulation(iterations, batchSize, rbinom, runner, learnerId);
					} else {
						final int hd = 16384;
						final double alpha = 0.5;

						final PLTAdaptiveEnsembleAgeFunctions ageFunction = PLTAdaptiveEnsembleAgeFunctions
								.valueOf(configMap.get(ageFunctionArg));
						final double epsilon = Double.parseDouble(configMap.get(epsilonArg));
						final int nMin = Integer.parseInt(configMap.get(nMinArg));
						final double retainmentFraction = Double.parseDouble(configMap.get(retainmentArg));

						PLTInitConfiguration pltConfig = getConstantPLTConfig();
						PLTAdaptiveEnsembleInitConfiguration ensembleConfig = getConstantEnsembleConfig();
						ensembleConfig.individualPLTProperties = pltConfig;

						pltConfig.setHd(hd);
						ensembleConfig.setAgeFunction(ageFunction);
						ensembleConfig.setAlpha(alpha);
						ensembleConfig.setEpsilon(epsilon);
						ensembleConfig.setMinTraingInstances(nMin);
						ensembleConfig.setRetainmentFraction(retainmentFraction);

						runSimulation(iterations, batchSize, rbinom, ensembleConfig,
								MessageFormat.format(
										"PLTAdaptiveEnsemble_{0,number,#}_{1,number,#}_{2,number,#}_{3}_{4}_{5,number,#.########}_{6}_{7}",
										iterations, batchSize, hd, ageFunction, alpha, epsilon, nMin,
										retainmentFraction),
								runner);
					}
				}
			} else {
				// run series of simulations
				final PLTAdaptiveEnsembleAgeFunctions[] ageFunctions = {
						PLTAdaptiveEnsembleAgeFunctions.NumberOfLabelsBased,
						PLTAdaptiveEnsembleAgeFunctions.NumberTrainingInstancesBased };
				final double[] epsilons = { 0.000001, 0.001 };
				final int[] nMins = { 2, 100 };
				final double[] retainmentFractions = { 0.1, 0.2 };
				final int hd = 16384;
				final double alpha = 0.5;

				File texFile = new File(texPath);
				if (texFile.exists())
					texFile.delete();

				PLTInitConfiguration pltConfig = getConstantPLTConfig();
				PLTAdaptiveEnsembleInitConfiguration ensembleConfig = getConstantEnsembleConfig();
				ensembleConfig.individualPLTProperties = pltConfig;

				for (PLTAdaptiveEnsembleAgeFunctions ageFunction : ageFunctions) {
					for (double epsilon : epsilons) {
						for (int nMin : nMins) {
							for (double retainmentFraction : retainmentFractions) {

								pltConfig.setHd(hd);
								ensembleConfig.setAgeFunction(ageFunction);
								ensembleConfig.setAlpha(alpha);
								ensembleConfig.setEpsilon(epsilon);
								ensembleConfig.setMinTraingInstances(nMin);
								ensembleConfig.setRetainmentFraction(retainmentFraction);

								runSimulation(iterations, batchSize, rbinom, ensembleConfig,
										MessageFormat.format(
												"PLTAdaptiveEnsemble_{0,number,#}_{1,number,#}_{2,number,#}_{3}_{4}_{5,number,#.########}_{6}_{7}",
												iterations, batchSize, hd, ageFunction, alpha, epsilon, nMin,
												retainmentFraction),
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

	private static PLTAdaptiveEnsembleInitConfiguration getConstantEnsembleConfig() {
		PLTAdaptiveEnsembleInitConfiguration ensembleConfig = new PLTAdaptiveEnsembleInitConfiguration();
		ensembleConfig
				.setPenalizingStrategy(PLTAdaptiveEnsemblePenalizingStrategies.AgePlusLogOfInverseMacroFm);
		ensembleConfig.setA(3);
		ensembleConfig.setC(100);
		ensembleConfig.tunerInitOption = new ThresholdTunerInitOption(1,2);
		ensembleConfig.setMeasureTime(true);
		ensembleConfig.setPreferMacroFmeasure(false);
		return ensembleConfig;
	}

	private static PLTInitConfiguration getConstantPLTConfig() {
		PLTInitConfiguration pltConfig = new PLTInitConfiguration();
		pltConfig.setHasher("AdaptiveMurmur");
		pltConfig.tunerType = ThresholdTuners.AdaptiveOfoFast;
		pltConfig.tunerInitOption = new ThresholdTunerInitOption(1, 2);
		pltConfig.setLambda(1);
		pltConfig.setGamma(1);
		pltConfig.setK(2);
		return pltConfig;
	}

	private static void resumeSimulation(int iterations, int batchSize, BinomialDistribution rbinom,
			PLTAdaptiveEnsembleRunner runner, UUID learnerId)
			throws JsonParseException, JsonMappingException, IOException {
		runner = new PLTAdaptiveEnsembleRunner(learnerId);
		Learner learner = runner.learnerRepository.read(learnerId);
		PLTAdaptiveEnsembleInitConfiguration config = simpleObjectMapper.readValue(learner.getLearnerDetails(),
				PLTAdaptiveEnsembleInitConfiguration.class);

		runSimulation(iterations, batchSize, rbinom, config,
				MessageFormat.format(
						"PLTAdaptiveEnsemble_{0,number,#}_{1,number,#}_{2,number,#}_{3}_{4}_{5,number,#.########}_{6}_{7}",
						iterations, batchSize, config.individualPLTProperties.getHd(), config.getAgeFunction(),
						config.getAlpha(), config.getEpsilon(), config.getMinTraingInstances(),
						config.getRetainmentFraction()),
				runner, runner.pltEnsemble.getnTrain());
	}

	private static void runSimulation(int iterations, int batchSize, BinomialDistribution rbinom,
			PLTAdaptiveEnsembleInitConfiguration config, String fileName, PLTAdaptiveEnsembleRunner runner)
			throws JsonProcessingException {
		runSimulation(iterations, batchSize, rbinom, config, fileName, runner, 0);
	}

	private static void runSimulation(int iterations, int batchSize, BinomialDistribution rbinom,
			PLTAdaptiveEnsembleInitConfiguration config, String fileName, PLTAdaptiveEnsembleRunner runner,
			int startIndex)
			throws JsonProcessingException {
		if (runner == null || startIndex == 0)
			runner = new PLTAdaptiveEnsembleRunner(config);
		// boolean toTrain = true;

		for (int index = startIndex; index < iterations; index++) {
			// try {
			// toTrain = index == 0 || rbinom.sample() == 1;

			runner.soDataManager.loadNext(batchSize, runner.pltEnsemble.getId());

			// if (toTrain)
			runner.pltEnsemble.train(runner.soDataManager);
			// else
			// runner.pltEnsemble.test(runner.soDataManager);

			runner.learnerRepository.update(runner.pltEnsemble.getId(), runner.pltEnsemble);

			logger.info("End of iteration " + (index + 1) + " of " + iterations);
			// } catch (Exception e) {
			// throw new RuntimeException(e);
			// }
		}

		// runner.learnerRepository.update(runner.learnerRepository.read(runner.pltEnsemble.getId()),
		// runner.pltEnsemble);
		
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
			PLTAdaptiveEnsembleInitConfiguration config, PLTAdaptiveEnsembleRunner runner, StringBuilder verbose,
			StringBuilder tex)
			throws JsonProcessingException {
		double trainMacroFm = runner.pltEnsemble.getMacroFmeasure();
		double trainAvgTime = runner.pltEnsemble.getAverageTrainTime();

		double prequestialAvgFm = runner.pltEnsemble.getAverageFmeasure(true, false);
		double prequestialAvgFmTopk = runner.pltEnsemble.getAverageFmeasure(true, true);
		double prequestialEvalAvgTime = runner.pltEnsemble.getAverageEvaluationTime(true, false);
		double prequestialEvalAvgTimeTopk = runner.pltEnsemble.getAverageEvaluationTime(true, true);

		double avgFm = runner.pltEnsemble.getAverageFmeasure(false, false);
		double avgFmTopk = runner.pltEnsemble.getAverageFmeasure(false, true);
		double evalAvgTime = runner.pltEnsemble.getAverageEvaluationTime(false, false);
		double evalAvgTimeTopk = runner.pltEnsemble.getAverageEvaluationTime(false, true);

		// double testMacroFm = runner.pltEnsemble.getTestMacroFmeasure(false);
		// double testMacroFmTopk =
		// runner.pltEnsemble.getTestMacroFmeasure(true);
		// double testAvgFm = runner.pltEnsemble.getTestAverageFmeasure(false);
		// double testAvgFmTopk =
		// runner.pltEnsemble.getTestAverageFmeasure(true);
		// double testAvgTime = runner.pltEnsemble.getAverageTestTime();

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

		// tex.append(config.individualPLTProperties.getHd());
		// tex.append(" & ");
		tex.append(config.getAgeFunction()
				.equals(PLTAdaptiveEnsembleAgeFunctions.NumberOfLabelsBased) ? "L" : "N");
		tex.append(" & ");
		// tex.append(MessageFormat.format("{0,number,0.00}",
		// config.getAlpha()));
		// tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.0E0}", config.getEpsilon()));
		tex.append(" & ");
		tex.append(config.getMinTraingInstances());
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", config.getRetainmentFraction()));
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
		// tex.append(MessageFormat.format("{0,number,0.0E0}", testAvgTime /
		// 1000.0));
		tex.append(" \\\\ \\hline \n");
	}

	@Override
	public void onPLTCreated(Object source, PLTCreationEventArgs args) {
		logger.info("New PLT created: " + args.plt.getId());
	}

	/*	@Override
		public void onPLTDiscarded(Object source, PLTDiscardedEventArgs args) {
			Learner discardedLearner = learnerRepository.read((UUID) args.discardedPLT.getId());
			discardedLearner.setActive(false);
			// discardedLearner.setLearnerDetails(gson.toJson(args.discardedPLT));
			learnerRepository.update(discardedLearner, args.discardedPLT);
	
			logger.info("Discarded PLT: " + args.discardedPLT.getId());
		}*/

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

	private PLTAdaptiveEnsembleInitConfiguration getLearnerConfiguration(ObjectNode runConfig)
			throws JsonProcessingException {

		PLTAdaptiveEnsembleInitConfiguration config = simpleObjectMapper.treeToValue(
				runConfig.get("ensembleProperties"),
				PLTAdaptiveEnsembleInitConfiguration.class);

		config.fmeasureObserver = this.soDataManager;
		config.learnerRepository = this.learnerRepository;

		return config;
	}
}
