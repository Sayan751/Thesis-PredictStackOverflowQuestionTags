package thesis.simulators;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.code.stackexchange.client.query.StackExchangeApiQueryFactory;
import com.google.code.stackexchange.schema.StackExchangeSite;

import Learner.AdaptivePLT;
import Learner.PLT;
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
import util.AdaptivePLTInitConfiguration;
import util.PLTInitConfiguration;

public class PLTRunner {
	private static final String simulationResultExt = ".txt";
	private static final String simulationResultPath = "../simulationResults/AdaptivePLT/";
	private static final String texPath = "../simulationResults/adaptivePLTData.tex";
	private static Logger logger;
	static {
		System.setProperty("initiator", PLTRunner.class.getSimpleName());
		System.setProperty("current.date", (new SimpleDateFormat("yyyy-MM-dd HHmmss")).format(new Date()));
		System.setProperty("log4j.configurationFile", "resources/log4j2.xml");
		logger = LogManager.getLogger(PLTRunner.class);
	}

	LearnerRepository learnerRepository;
	private ILabelRepository<Tag> labelRepository;
	private IQuestionRepository questionRepository;
	private IQuestionTermFrequencyRepository questionTermFrequencyRepository;
	private ITermRepository termRepository;
	private IQuestionLearnerDetailsRepository questionLearnerDetailsRepository;

	private StackExchangeApiQueryFactory queryFactory;
	StackOverflowQuestionManager soDataManager;

	public PLT plt;
	static ObjectMapper simpleObjectMapper = new ObjectMapper();

	public PLTRunner(ObjectNode runConfig) throws JsonProcessingException {
		initRepositories();
		initSODataManage();

		AdaptivePLTInitConfiguration config = getLearnerConfiguration(runConfig);

		plt = new AdaptivePLT(config);

		Learner learner = learnerRepository.create(new Learner(runConfig.toString()), plt);
		plt.setId(learner.getId());
	}

	public PLTRunner(PLTInitConfiguration config) throws JsonProcessingException {
		initRepositories();
		initSODataManage();

		config.fmeasureObserver = this.soDataManager;
		plt = new PLT(config);

		Learner learner = learnerRepository.create(new Learner(simpleObjectMapper.writeValueAsString(config)),
				plt);
		plt.setId(learner.getId());
	}

	public static void main(String[] args) {
		try {
			String configFileName = args[0];
			ObjectNode runConfig = JsonSerializer.readFromFile(configFileName, ObjectNode.class);

			int iterations = runConfig.get("iterations")
					.asInt();
			int batchSize = runConfig.get("batchSize")
					.asInt(1);

			double trainingProbability = runConfig.get("trainingProbability")
					.asDouble(1.0);
			BinomialDistribution rbinom = new BinomialDistribution(1, trainingProbability);

			PLTRunner runner = null;

			JsonNode pltConfig = runConfig.get("PLTProperties");
			if (pltConfig != null)
				runSimulation(iterations, batchSize, rbinom,
						(new ObjectMapper()).treeToValue(pltConfig, PLTInitConfiguration.class), null, runner);
			else {
				// run series of simulations
				// final int[] hds = { 5000, 10000 };
				// final double[] alphas = { 0.4, 0.5, 0.85, 1 };
				// final boolean[] preferNodeWithHighestProbability = { true,
				// false };
				// final String[] hashers = { "AdaptiveMurmur",
				// "AdaptiveMurmur2" };
				//
				// File texFile = new File(texPath);
				// if (texFile.exists())
				// texFile.delete();
				//
				// AdaptivePLTInitConfiguration simulationConfig =
				// getConstantConfig();
				//
				// for (int hd : hds) {
				// for (double alpha : alphas) {
				// for (boolean probPref : preferNodeWithHighestProbability) {
				// for (String hasher : hashers) {
				//
				// simulationConfig.setHd(hd);
				// simulationConfig.setAlpha(alpha);
				// simulationConfig
				// .setToPreferHighestProbLeaf(probPref);
				// simulationConfig.setHasher(hasher);
				//
				// runSimulation(iterations, batchSize, rbinom,
				// simulationConfig,
				// MessageFormat.format(
				// "AdaptivePLT_{0,number,#}_{1,number,#}_{2,number,#}_{3}_{4}_{5}",
				// iterations, batchSize, hd, alpha, probPref, hasher),
				// runner);
				//
				// }
				// }
				// }
				// }
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			HibernateManager.shutdown();
		}
	}

	// private static AdaptivePLTInitConfiguration getConstantConfig() {
	// AdaptivePLTInitConfiguration simulationConfig = new
	// AdaptivePLTInitConfiguration();
	// simulationConfig.tunerType = ThresholdTuners.AdaptiveOfoFast;
	// simulationConfig.tunerInitOption = new ThresholdTunerInitOption(
	// OFODefaultValues.aSeed, OFODefaultValues.bSeed);
	// simulationConfig.setLambda(1);
	// simulationConfig.setGamma(1);
	// simulationConfig.setEpochs(30);
	// simulationConfig.setDefaultK(5);
	// simulationConfig.setK(2);
	// simulationConfig.setMeasureTime(true);
	// return simulationConfig;
	// }

	private static void runSimulation(int iterations, int batchSize, BinomialDistribution rbinom,
			PLTInitConfiguration config, String fileName, PLTRunner runner)
			throws JsonProcessingException {
		runner = new PLTRunner(config);
		runner.soDataManager.loadNext(batchSize * iterations, runner.plt.getId());
		runner.plt.allocateClassifiers(runner.soDataManager);
		runner.plt.train(runner.soDataManager);
		// // boolean toTrain = true;
		//
		// for (int index = 0; index < iterations; index++) {
		// // toTrain = index == 0 || rbinom.sample() == 1;
		//
		// if (index > 0)
		// runner.soDataManager.loadNext(batchSize, runner.plt.getId());
		//
		// // if (toTrain)
		// runner.plt.train(runner.soDataManager);
		// // else
		// // runner.plt.test(runner.soDataManager);
		//
		// logger.info("End of iteration " + (index + 1) + " of " + iterations);
		// }

		runner.learnerRepository.update(
				runner.learnerRepository.read((UUID) runner.plt.getId()),
				runner.plt);

		StringBuilder verbose = new StringBuilder(), tex = new StringBuilder();
		buildSimulationInfo(iterations, batchSize, rbinom, config, runner, verbose, tex);

		if (fileName != null && !fileName.isEmpty()) {
			TextFileWriter.writeToFile(simulationResultPath + fileName + simulationResultExt, verbose.toString());
			TextFileWriter.writeToFile(texPath, tex.toString(), true);
			logger.info("End of simulation: " + fileName);
		} else
			logger.info(verbose.toString());

	}

	private static void buildSimulationInfo(int iterations, int batchSize, BinomialDistribution rbinom,
			PLTInitConfiguration config, PLTRunner runner, StringBuilder verbose, StringBuilder tex)
			throws JsonProcessingException {

		double trainMacroFm = runner.plt.getMacroFmeasure();
		double trainAvgFm = runner.plt.getAverageFmeasure(false, false);
		double trainAvgFmTopk = runner.plt.getAverageFmeasure(false, true);
		double trainAvgTime = runner.plt.getAverageTrainTime();

		double testMacroFm = runner.plt.getTestMacroFmeasure(false);
		double testMacroFmTopk = runner.plt.getTestMacroFmeasure(true);
		double testAvgFm = runner.plt.getTestAverageFmeasure(false);
		double testAvgFmTopk = runner.plt.getTestAverageFmeasure(true);
		double testAvgTime = runner.plt.getAverageTestTime();

		verbose.append("Simulation Info:\n");
		verbose.append("================\n\n");

		verbose.append("Total number of instances: ");
		verbose.append(iterations * batchSize);
		verbose.append("\n");

		double trainSplit = rbinom.getProbabilityOfSuccess() * 100;
		verbose.append("Random train/test split: ");
		verbose.append(trainSplit);
		verbose.append("/");
		verbose.append(100 - trainSplit);
		verbose.append("\n");

		verbose.append("Simulation configuration: ");
		verbose.append(simpleObjectMapper.writeValueAsString(config));
		verbose.append("\n\n");

		verbose.append("Training info:\n");
		verbose.append("================\n");
		verbose.append("Total number of training instances: ");
		verbose.append(runner.plt.getnTrain());
		verbose.append("\n");
		verbose.append("Macro fmeasure: ");
		verbose.append(trainMacroFm);
		verbose.append("\n");
		verbose.append("Average fmeasure: ");
		verbose.append(trainAvgFm);
		verbose.append("\n");
		verbose.append("Topk-based average fmeasure: ");
		verbose.append(trainAvgFmTopk);
		verbose.append("\n");
		verbose.append("Average training time: ");
		verbose.append(trainAvgTime);
		verbose.append(" \u00B5s\n\n");

		verbose.append("Testing info:\n");
		verbose.append("================\n");
		verbose.append("Total number of testing instances: ");
		verbose.append(runner.plt.getnTest());
		verbose.append("\n");
		verbose.append("Macro fmeasure: ");
		verbose.append(testMacroFm);
		verbose.append("\n");
		verbose.append("Topk-based macro fmeasure: ");
		verbose.append(testMacroFmTopk);
		verbose.append("\n");
		verbose.append("Average fmeasure: ");
		verbose.append(testAvgFm);
		verbose.append("\n");
		verbose.append("Topk-based average fmeasure: ");
		verbose.append(testAvgFmTopk);
		verbose.append("\n");
		verbose.append("Average testing time: ");
		verbose.append(testAvgTime);
		verbose.append(" \u00B5s\n");

		tex.append(config.getHd());
		tex.append(" & ");
		// tex.append(MessageFormat.format("{0,number,0.00}",
		// config.getAlpha()));
		// tex.append(" & ");
		// tex.append(config.isToPreferHighestProbLeaf() ? "Y" : "N");
		// tex.append(" & ");
		tex.append(config.getHasher()
				.equals("AdaptiveMurmur") ? "X" : "G");
		tex.append(" & ");

		tex.append(MessageFormat.format("{0,number,0.00}", trainMacroFm * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", trainAvgFm * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", trainAvgFmTopk * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", trainAvgTime));
		tex.append(" & ");

		tex.append(MessageFormat.format("{0,number,0.00}", testMacroFm * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", testMacroFmTopk * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", testAvgFm * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", testAvgFmTopk * 100));
		tex.append(" & ");
		tex.append(MessageFormat.format("{0,number,0.00}", testAvgTime));
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

	private AdaptivePLTInitConfiguration getLearnerConfiguration(ObjectNode runConfig) throws JsonProcessingException {

		AdaptivePLTInitConfiguration config = (new ObjectMapper()).treeToValue(runConfig.get("PLTProperties"),
				AdaptivePLTInitConfiguration.class);
		config.fmeasureObserver = this.soDataManager;

		return config;
	}
}
