package thesis.simulators;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.math.Stats;

import thesis.util.HibernateManager;
import thesis.util.JsonSerializer;

public class Comparer {

	private static Logger logger;
	static {
		System.setProperty("initiator", Comparer.class.getSimpleName());
		System.setProperty("current.date", (new SimpleDateFormat("yyyy-MM-dd HHmmss")).format(new Date()));
		System.setProperty("log4j.configurationFile", "resources/log4j.xml");
		logger = LogManager.getLogger(Comparer.class);
	}

	public static void main(String[] args) {
		try {
			String configFileName = "learnerConfigurations\\combined\\config-i1000s1.json";
			ObjectNode runConfig = JsonSerializer.readFromFile(configFileName, ObjectNode.class);

			int rounds = runConfig.get("rounds")
					.asInt();
			int iterations = runConfig.get("iterations")
					.asInt();
			int batchSize = runConfig.get("batchSize")
					.asInt(1);

			ArrayList<Double> adaptivePLTPerfs = new ArrayList<Double>();
			ArrayList<Double> ensemblePLTPerfs = new ArrayList<Double>();
			ArrayList<Double> ensembleBoostedPLTPerfs = new ArrayList<Double>();

			IntStream.range(0, rounds)
					.forEach(i -> {
						AdaptivePLTRunner adaptivePLTrunner;
						try {
							adaptivePLTrunner = runAdaptivePLT(runConfig, iterations, batchSize, i);

							double averageFmeasure = adaptivePLTrunner.adaptivePlt.getAverageFmeasure(false, true);
							logger.info(
									"After training on "
											+ adaptivePLTrunner.adaptivePlt.getnTrain()
											+ " instances, the (topk) average Fmeasure of the adaptive PLT is "
											+ averageFmeasure);

							adaptivePLTPerfs.add(averageFmeasure);

							PLTAdaptiveEnsembleRunner pltEnsembleRunner = runPLTEnsemble(runConfig, iterations, batchSize, i);

							averageFmeasure = pltEnsembleRunner.pltEnsemble.getAverageFmeasure(false, true);
							logger.info(
									"After training on "
											+ pltEnsembleRunner.pltEnsemble.getnTrain()
											+ " instances, the (topk) average Fmeasure of the PLTEnsemble is "
											+ averageFmeasure);
							ensemblePLTPerfs.add(averageFmeasure);

							PLTEnsembleBoostedRunner pltEnsembleBoostedRunner = runPLTEnsembleBoosted(runConfig,
									iterations,
									batchSize, i);

							averageFmeasure = pltEnsembleBoostedRunner.pltEnsembleBoosted.getAverageFmeasure(false, true);
							logger.info(
									"After training on "
											+ pltEnsembleBoostedRunner.pltEnsembleBoosted
													.getnTrain()
											+ " instances, the (topk) average Fmeasure of the PLTEnsembleBoosted is "
											+ averageFmeasure);
							ensembleBoostedPLTPerfs.add(averageFmeasure);
							logger.info("End of round " + (i + 1) + " of " + rounds);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					});

			logger.info("Average fmeasure (topk) of adaptivePLT is " + Stats.meanOf(adaptivePLTPerfs));
			logger.info("Average fmeasure (topk) of ensemblePLT is " + Stats.meanOf(ensemblePLTPerfs));
			logger.info("Average fmeasure (topk) of pltEnsembleBoosted is " + Stats.meanOf(ensembleBoostedPLTPerfs));

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			HibernateManager.shutdown();
		}

	}

	private static PLTAdaptiveEnsembleRunner runPLTEnsemble(ObjectNode runConfig, int iterations, int batchSize, int i) throws JsonProcessingException {
		PLTAdaptiveEnsembleRunner pltEnsembleRunner = new PLTAdaptiveEnsembleRunner(runConfig);

		IntStream.range(0, iterations)
				.forEach(index -> {
					try {
						pltEnsembleRunner.soDataManager.loadNext(batchSize, pltEnsembleRunner.pltEnsemble.getId());						

						pltEnsembleRunner.pltEnsemble.train(pltEnsembleRunner.soDataManager);

						logger.info("End of iteration " + index + " of " + iterations + " (round: "
								+ (i + 1)
								+ ")");
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
		pltEnsembleRunner.learnerRepository.update(
				pltEnsembleRunner.learnerRepository.read((UUID) pltEnsembleRunner.pltEnsemble.getId()),
				pltEnsembleRunner.pltEnsemble);
		return pltEnsembleRunner;
	}

	private static AdaptivePLTRunner runAdaptivePLT(ObjectNode runConfig, int iterations, int batchSize, int i)
			throws JsonProcessingException {
		AdaptivePLTRunner adaptivePLTrunner = new AdaptivePLTRunner(runConfig);

		adaptivePLTrunner.soDataManager.loadNext(batchSize, adaptivePLTrunner.adaptivePlt.getId());
		adaptivePLTrunner.adaptivePlt.allocateClassifiers(adaptivePLTrunner.soDataManager);
		IntStream.range(0, iterations)
				.forEach(index -> {
					if (index > 0)
						adaptivePLTrunner.soDataManager.loadNext(batchSize, adaptivePLTrunner.adaptivePlt.getId());
					adaptivePLTrunner.adaptivePlt.train(adaptivePLTrunner.soDataManager);
					logger.info("End of iteration " + (index + 1) + " of " + iterations + " (round: "
							+ (i + 1)
							+ ")");
				});

		adaptivePLTrunner.learnerRepository.update(
				adaptivePLTrunner.learnerRepository.read((UUID) adaptivePLTrunner.adaptivePlt.getId()),
				adaptivePLTrunner.adaptivePlt);
		return adaptivePLTrunner;
	}

	private static PLTEnsembleBoostedRunner runPLTEnsembleBoosted(ObjectNode runConfig, int iterations, int batchSize,
			int i) throws JsonProcessingException {
		PLTEnsembleBoostedRunner runner = new PLTEnsembleBoostedRunner(runConfig);

		runner.soDataManager.loadNext(batchSize, runner.pltEnsembleBoosted.getId());
		runner.pltEnsembleBoosted.allocateClassifiers(runner.soDataManager);

		IntStream.range(0, iterations)
				.forEach(index -> {
					if (index > 0)
						runner.soDataManager.loadNext(batchSize, runner.pltEnsembleBoosted.getId());
					runner.pltEnsembleBoosted.train(runner.soDataManager);
					logger.info("End of iteration " + index + " of " + iterations + " (round: "
							+ (i + 1)
							+ ")");
				});
		runner.learnerRepository.update(
				runner.learnerRepository.read((UUID) runner.pltEnsembleBoosted.getId()),
				runner.pltEnsembleBoosted);
		return runner;
	}
}
