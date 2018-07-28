package thesis.data.managers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;

import Data.AVPair;
import Data.Instance;
import IO.DataManager;
import Learner.AbstractLearner;
import event.args.InstanceProcessedEventArgs;
import event.args.InstanceTestedEventArgs;
import interfaces.IFmeasureObserver;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ConverterUtils.DataSource;

public class ArffDataManager extends DataManager implements IFmeasureObserver {

	private class ResultFields {
		public static final String questionId = "QuestionId";
		public static final String createdOn = "CreatedOn";
		public static final String fmeasure = "Fmeasure";
		public static final String topkFmeasure = "TopkFmeasure";
		public static final String isTest = "IsTest";
		public static final String isPrequential = "IsPrequential";
	}

	private static Logger logger = LogManager.getLogger(ArffDataManager.class);
	private static final String dateFormat = "yyyy-MM-dd HH:mm:ss.SSS";
	private static final SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

	private Instances data;
	private int numLabels;
	private int numFeatures;
	private int numInstances;

	private List<SimpleEntry<Instance, Long>> buffer = new ArrayList<>();

	private Instances result;
	private String fmeasureResultFilePath;

	private int currentRowIndex = -1;
	private int bufferIndex = 0;
	private String basePathForEnsembleResults = null;
	private boolean isEnsmble = false;
	// private ArffSaver saver = new ArffSaver();

	public ArffDataManager(final String arffFilePath, final String labelPrefix,
			final String basePathForEnsembleResults) {
		try {
			initializeData(arffFilePath, labelPrefix);
			this.basePathForEnsembleResults = basePathForEnsembleResults;

			isEnsmble = this.basePathForEnsembleResults != null && !this.basePathForEnsembleResults.isEmpty();
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Failed to initialize the ArffDataManger. For root cause check wrapped exception.", e);
		}
	}

	public ArffDataManager(final String arffFilePath, final String labelPrefix, final String fmeasureResultFilePath,
			final String resultRelationName) {
		try {
			initializeData(arffFilePath, labelPrefix);

			if (fmeasureResultFilePath != null) {
				this.fmeasureResultFilePath = fmeasureResultFilePath;

				if (new File(this.fmeasureResultFilePath).exists())
					loadExistingResultsDataset().validate();
				else
					initializeResultsDataset(resultRelationName).saveResultsToDisk();
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Failed to initialize the ArffDataManger. For root cause check wrapped exception.", e);
		}
		isEnsmble = this.basePathForEnsembleResults != null && !this.basePathForEnsembleResults.isEmpty();
	}

	@Override
	public void loadNext(int count) {
		final int endIndex = currentRowIndex + count;
		if (endIndex + 1 > numInstances)
			throw new IllegalArgumentException("Not enough new instances to load; requested: " + count + ", has:"
					+ ((endIndex + 1) - numInstances));

		buffer.clear();
		while (currentRowIndex < endIndex) {
			currentRowIndex++;
			weka.core.Instance instance = data.get(currentRowIndex);
			final double qId = instance.valueSparse(0);
			buffer.add(new SimpleEntry<Instance, Long>(convertToXmlcInstance((SparseInstance) instance),
					(long) qId));
		}
		reset();
	}

	@Override
	public boolean hasNext() {
		return buffer.size() > bufferIndex;
	}

	@Override
	public Instance getNextInstance() {
		return buffer.get(bufferIndex++)
				.getKey();
	}

	@Override
	public int getNumberOfFeatures() {
		// the 0th attribute is not a feature//TODO: fix it later; low priority.
		return numFeatures - 1;
	}

	@Override
	public int getNumberOfLabels() {
		return numLabels;
	}

	@Override
	public void reset() {
		bufferIndex = 0;
	}

	/**
	 * @return the currentRowIndex
	 */
	public int getCurrentRowIndex() {
		return currentRowIndex;
	}

	/**
	 * @param currentRowIndex
	 *            the currentRowIndex to set
	 */
	public void setCurrentRowIndex(int currentRowIndex) {
		if (currentRowIndex >= this.numInstances)
			throw new IllegalArgumentException("Index is greater than the number of instances available.");
		this.currentRowIndex = currentRowIndex;
	}

	private void initializeData(final String arffFilePath, final String labelPrefix) throws Exception {
		data = new DataSource(arffFilePath).getDataSet();
		/* TODO: validate that the first attribute is SO questionid; low priority */
		numLabels = (int) Collections.list(data.enumerateAttributes())
				.stream()
				.filter(attr -> attr.name()
						.startsWith(labelPrefix))
				.count();
		numFeatures = data.numAttributes() - numLabels;
		numInstances = data.numInstances();
	}

	private void validate() {
		Attribute attribute = result.attribute(0);
		if (!attribute.name()
				.equals(ResultFields.questionId) || !attribute.isNumeric())
			throw new IllegalArgumentException(
					"The first attribute of the result must be numeric and named as '" + ResultFields.questionId + "'");

		attribute = result.attribute(1);
		if (!attribute.name()
				.equals(ResultFields.createdOn) || !attribute.isDate() || !attribute.getDateFormat()
						.equals(dateFormat))
			throw new IllegalArgumentException("The second attribute of the result must be datetime (with format '"
					+ dateFormat + "') and named as '" + ResultFields.createdOn + "'");

		attribute = result.attribute(2);
		if (!attribute.name()
				.equals(ResultFields.fmeasure) || !attribute.isNumeric())
			throw new IllegalArgumentException(
					"The third attribute of the result must be numeric and named as '" + ResultFields.fmeasure + "'");

		attribute = result.attribute(3);
		if (!attribute.name()
				.equals(ResultFields.topkFmeasure) || !attribute.isNumeric())
			throw new IllegalArgumentException(
					"The fourth attribute of the result must be numeric and named as '" + ResultFields.topkFmeasure
							+ "'");

		attribute = result.attribute(4);
		if (!attribute.name()
				.equals(ResultFields.isTest) || !attribute.isNominal()
				|| !(new HashSet<>(Collections.list(attribute.enumerateValues()))
						.equals(new HashSet<>(Arrays.asList("0", "1")))))
			throw new IllegalArgumentException(
					"The fifth attribute of the result must be numinal (allowed values are 0, and 1) and named as '"
							+ ResultFields.isTest + "'");

		attribute = result.attribute(5);
		if (!attribute.name()
				.equals(ResultFields.isPrequential) || !attribute.isNominal()
				|| !(new HashSet<>(Collections.list(attribute.enumerateValues()))
						.equals(new HashSet<>(Arrays.asList("0", "1")))))
			throw new IllegalArgumentException(
					"The sixth attribute of the result must be numinal (allowed values are 0, and 1) and named as '"
							+ ResultFields.isPrequential + "'");
	}

	private ArffDataManager loadExistingResultsDataset() throws Exception {
		result = new DataSource(this.fmeasureResultFilePath).getDataSet();
		return this;
	}

	private void saveResultsToDisk() throws IOException {
		final File file = new File(fmeasureResultFilePath);
		Files.createParentDirs(file);

		BufferedWriter writer = new BufferedWriter(new FileWriter(fmeasureResultFilePath));
		writer.write(result.toString());
		writer.flush();
		writer.close();

		// saver.setInstances(result);
		// System.out.println(fmeasureResultFilePath);
		// saver.setFile(file);
		// // saver.setDestination(file);
		// saver.writeBatch();
	}

	private void appendResult(String result) throws IOException {

		BufferedWriter writer = new BufferedWriter(new FileWriter(fmeasureResultFilePath, true));
		writer.write(result);
		writer.flush();
		writer.close();
	}

	private ArffDataManager initializeResultsDataset(final String resultRelationName) {
		final ArrayList<Attribute> attInfo = new ArrayList<>();
		attInfo.add(new Attribute(ResultFields.questionId));
		attInfo.add(new Attribute(ResultFields.createdOn, dateFormat));
		attInfo.add(new Attribute(ResultFields.fmeasure));
		attInfo.add(new Attribute(ResultFields.topkFmeasure));
		attInfo.add(new Attribute(ResultFields.isTest, new ArrayList<>(Arrays.asList("0", "1"))));
		attInfo.add(new Attribute(ResultFields.isPrequential, new ArrayList<>(Arrays.asList("0", "1"))));

		result = new Instances(
				(resultRelationName != null && !resultRelationName.isEmpty()) ? resultRelationName : "Result",
				attInfo, 0);
		return this;
	}

	private Instance convertToXmlcInstance(SparseInstance sInstance) {
		List<AVPair> x = new ArrayList<>();
		List<Integer> y = new ArrayList<>();

		// index starts from 1 as the 0th index contains the question id, which
		// is not a feature.
		for (int i = 1; i < sInstance.numValues(); i++) {
			final int attributeIndex = sInstance.index(i);
			if (attributeIndex < numFeatures)
				x.add(new AVPair(attributeIndex, sInstance.valueSparse(i)));
			else
				y.add(attributeIndex - numFeatures);
		}
		return new Data.Instance(x.toArray(new AVPair[x.size()]), Ints.toArray(y));
	}

	/***
	 * 
	 * Members of IFmeasureObserver
	 * 
	 ***/
	@Override
	public void onInstanceProcessed(AbstractLearner learner, InstanceProcessedEventArgs args) {
		try {
			final UUID learnerId = learner.getId();
			if (isEnsmble) {
				final String strLearnerId = learnerId.toString();
				setFmeasuresResultPath(strLearnerId).createResultsFileIfNotExists(strLearnerId);
			}

			markProcessed(args.instance, learnerId, args.fmeasure, args.topkFmeasure, args.isPrequential, false);

			if (isEnsmble)
				result = null;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void createResultsFileIfNotExists(final String strLearnerId) throws IOException, Exception {
		if (!(new File(fmeasureResultFilePath).exists())) {
			this.initializeResultsDataset("Result" + strLearnerId);
			saveResultsToDisk();
		}
	}

	private void loadResultsFile() throws IOException, Exception {
		result = new DataSource(fmeasureResultFilePath).getDataSet();
	}

	private ArffDataManager setFmeasuresResultPath(final String strLearnerId) {
		fmeasureResultFilePath = this.basePathForEnsembleResults + strLearnerId + ".arff";
		return this;
	}

	@Override
	public void onInstanceTested(AbstractLearner learner, InstanceTestedEventArgs args) {
		try {
			final UUID learnerId = learner.getId();
			if (isEnsmble) {
				final String strLearnerId = learnerId.toString();
				setFmeasuresResultPath(strLearnerId).createResultsFileIfNotExists(strLearnerId);
			}

			markProcessed(args.instance, learnerId, args.fmeasure, args.topkFmeasure, false, true);

			if (isEnsmble)
				result = null;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public double getAverageFmeasure(AbstractLearner learner, boolean isPrequential, boolean isTopk) {
		double retVal = 0;
		try {
			if (isEnsmble) {
				final String strLearnerId = learner.getId()
						.toString();
				setFmeasuresResultPath(strLearnerId);
			}
			loadResultsFile();
			
			retVal = result.stream()
					.filter(instance -> instance.value(result.attribute(ResultFields.isTest)) == 0
							&& instance.value(result.attribute(ResultFields.isPrequential)) == (isPrequential ? 1 : 0))
					.mapToDouble(instance -> isTopk ? instance.value(result.attribute(ResultFields.topkFmeasure))
							: instance.value(result.attribute(ResultFields.fmeasure)))
					.average()
					.getAsDouble();

			if (isEnsmble)
				result = null;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return retVal;
	}

	@Override
	public double getTestAverageFmeasure(AbstractLearner learner, boolean isTopk) {
		double retVal = 0;
		try {
			if (isEnsmble) {
				final String strLearnerId = learner.getId()
						.toString();
				setFmeasuresResultPath(strLearnerId).loadResultsFile();
			}

			retVal = result.stream()
					.filter(instance -> instance.value(result.attribute(ResultFields.isTest)) == 1)
					.mapToDouble(instance -> isTopk ? instance.value(result.attribute(ResultFields.topkFmeasure))
							: instance.value(result.attribute(ResultFields.fmeasure)))
					.average()
					.getAsDouble();

			if (isEnsmble)
				result = null;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return retVal;
	}

	public Long getBufferedQuestionIdForInstance(Instance instance) {
		return Iterables.find(buffer, pair -> pair.getKey()
				.equals(instance), null)
				.getValue();
	}

	public void markProcessed(Instance instance, UUID learnerId, double fMeasure,
			double topkFMeasure, boolean isPrequential, boolean isTest) throws IOException {

		Long questionId = getBufferedQuestionIdForInstance(instance);

		logger.info((isTest ? "(Test data) " : "") + (isPrequential ? "Prequential fmeasures " : "Fmeasures ")
				+ " for learnerId:" + learnerId + ", and questionId: " + questionId + " are " + fMeasure + ", and "
				+ topkFMeasure + "(topk)");

		// QuestionId, CreatedOn, Fmeasure, TopkFmeasure, IsTest, IsPrequential
		// result.add(new DenseInstance(1.0, new double[] {
		// questionId, System.currentTimeMillis(), fMeasure, topkFMeasure,
		// isTest ? 1 : 0, isPrequential ? 1 : 0
		// }));

		StringBuilder sb = new StringBuilder(System.lineSeparator());
		sb.append(questionId.toString());
		sb.append(",'");
		sb.append(sdf.format(new Date(System.currentTimeMillis())));
		sb.append("',");
		sb.append(fMeasure);
		sb.append(",");
		sb.append(topkFMeasure);
		sb.append(",");
		sb.append(isTest ? 1 : 0);
		sb.append(",");
		sb.append(isPrequential ? 1 : 0);

		// System.out.println(sb.toString());

		// saveResultsToDisk();
		appendResult(sb.toString());
	}

	/***
	 * 
	 * Not implmented methods
	 * 
	 ***/
	@Override
	public void setInputStream(InputStreamReader input) {
		// Not Applicable
	}

	@Override
	public DataManager getCopy() {
		// Not Applicable as of now.
		return null;
	}

	@Override
	public void loadNext(int count, UUID learnerId) {
		throw new RuntimeException(
				"This method is not implemented for ArffDataManager. Use the loadNext(int) varient");
	}
}
