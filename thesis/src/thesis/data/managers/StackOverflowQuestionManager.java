package thesis.data.managers;

import static java.util.stream.Stream.concat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.code.stackexchange.client.query.StackExchangeApiQueryFactory;
import com.google.code.stackexchange.common.PagedList;
import com.google.code.stackexchange.schema.Paging;
import com.google.common.collect.Iterables;

import Data.AVPair;
import Data.Instance;
import IO.DataManager;
import Learner.AbstractLearner;
import event.args.InstanceProcessedEventArgs;
import event.args.InstanceTestedEventArgs;
import interfaces.IFmeasureObserver;
import thesis.data.entities.Question;
import thesis.data.entities.QuestionLearnerDetails;
import thesis.data.entities.Tag;
import thesis.data.entities.Term;
import thesis.data.repositoryinterfaces.ILabelRepository;
import thesis.data.repositoryinterfaces.IQuestionLearnerDetailsRepository;
import thesis.data.repositoryinterfaces.IQuestionRepository;
import thesis.data.repositoryinterfaces.IQuestionTermFrequencyRepository;
import thesis.data.repositoryinterfaces.ITermRepository;
import thesis.util.FeatureExtractor;
import thesis.util.adapters.QuestionAdapter;

public class StackOverflowQuestionManager extends DataManager implements IFmeasureObserver {
	private static Logger logger = LogManager.getLogger(StackOverflowQuestionManager.class);
	private static final int pageSize = 100;

	private List<SimpleEntry<Instance, UUID>> buffer;
	// private Map<Instance, Map<UUID, Double>> questionLearnerDetails = new
	// HashMap<Instance, Map<UUID, Double>>();

	private ILabelRepository<Tag> labelRepository;
	private IQuestionRepository questionRepository;
	private ITermRepository termRepository;
	private IQuestionTermFrequencyRepository questionTermFrequencyRepository;
	private IQuestionLearnerDetailsRepository questionLearnerDetailsRepository;
	private StackExchangeApiQueryFactory queryFactory;
	private int bufferIndex = 0;

	int maxFeatureIndex, maxLabelIndex;
	/**
	 * Temp storage for loadNext(); variable pulled out as a optimization step
	 * to avoid multiple redfinition.
	 */
	private transient List<Question> questions;

	public StackOverflowQuestionManager(
			ILabelRepository<Tag> labelRepository,
			IQuestionRepository questionRepository,
			IQuestionTermFrequencyRepository questionTermFrequencyRepository,
			ITermRepository termRepository,
			StackExchangeApiQueryFactory queryFactory,
			IQuestionLearnerDetailsRepository questionLearnerDetailsRepository) {

		if (labelRepository == null || questionRepository == null
				|| questionTermFrequencyRepository == null || termRepository == null || queryFactory == null
				|| questionLearnerDetailsRepository == null)
			throw new IllegalArgumentException();

		this.labelRepository = labelRepository;
		this.questionRepository = questionRepository;
		this.questionTermFrequencyRepository = questionTermFrequencyRepository;
		this.termRepository = termRepository;
		this.queryFactory = queryFactory;
		this.questionLearnerDetailsRepository = questionLearnerDetailsRepository;
		// queryFactory = StackExchangeApiQueryFactory.newInstance(null,
		// StackExchangeSite.STACK_OVERFLOW);

		buffer = new ArrayList<>();
	}

	@Override
	public void loadNext(int count, UUID learnerId) {
		maxFeatureIndex = Integer.MIN_VALUE;
		maxLabelIndex = Integer.MIN_VALUE;

		questions = questionRepository.getAllUnprocessedQuestion(learnerId, count);
		int questionsCount = questions.size();

		// if needed get more question from stackoverflow.
		while (questionsCount < count) {
			questions.addAll(fetchAndStoreNextChunkOfQuestions(count -
					questions.size()));

			// if there is no questions, then stop
			int tempCount = questions.size();
			if (questionsCount < tempCount)
				questionsCount = tempCount;
			else
				break;
		}

		// construct feature matrix (extracting features)...
		AVPair[][] featureMatrix = buildFeatureMatrix(questions);

		// ...and label matrix
		int[][] labelMatrix = questions.stream()
				.map(q -> buildLabelVector(q))
				.toArray(int[][]::new);

		if (!buffer.isEmpty())
			buffer.clear();
		questionsCount = questions.size();
		for (int index = 0; index < questionsCount; index++) {
			buffer.add(new SimpleEntry<Instance, UUID>(new Instance(featureMatrix[index], labelMatrix[index]),
					questions.get(index)
							.getId()));
		}

		// buffer = IntStream.range(0, questions.size())
		// .boxed()
		// .map(index -> {
		// return new SimpleEntry<Instance, UUID>(
		// new Instance(featureMatrix[index], labelMatrix[index]),
		// questions.get(index)
		// .getId());
		// })
		// .collect(Collectors.toList());

		maxFeatureIndex = Arrays.stream(featureMatrix)
				.flatMap(arr -> Arrays.stream(arr))
				.mapToInt(p -> p.index)
				.distinct()
				.max()
				.getAsInt();

		maxLabelIndex = Arrays.stream(labelMatrix)
				.flatMapToInt(arr -> Arrays.stream(arr))
				.distinct()
				.max()
				.getAsInt();

		reset();
		questions.clear();
	}

	public boolean markProcessed(Instance instance, UUID learnerId, double fMeasure, double topkFMeasure,
			boolean isPrequential, boolean isTest) {

		UUID questionId = getBufferedQuestionIdForInstance(instance);

		QuestionLearnerDetails details = new QuestionLearnerDetails(learnerId, questionId, fMeasure, isPrequential,
				false, isTest);
		QuestionLearnerDetails topKdetails = new QuestionLearnerDetails(learnerId, questionId, topkFMeasure,
				isPrequential, true, isTest);

		logger.info((isTest ? "(Test data) " : "") + (isPrequential ? "Prequential fmeasures " : "Fmeasures ")
				+ " for learner: " + learnerId + " and questionId: " + questionId + " are " + fMeasure + ", and "
				+ topkFMeasure + "(topk)");

		List<QuestionLearnerDetails> createdDetails = questionLearnerDetailsRepository
				.create(Arrays.asList(details, topKdetails));
		return createdDetails != null && !createdDetails.isEmpty();

	}

	public UUID getBufferedQuestionIdForInstance(Instance instance) {
		return Iterables.find(buffer, pair -> pair.getKey()
				.equals(instance), null)
				.getValue();
	}

	private int[] buildLabelVector(Question q) {
		return q.getTags()
				.stream()
				.mapToInt(tag -> tag.getIndex())
				.toArray();
	}

	private AVPair[][] buildFeatureMatrix(List<Question> questions) {

		Map<Boolean, List<Question>> groups = questions.stream()
				.collect(Collectors.partitioningBy(q -> q.getQuestionTermFrequencies()
						.size() <= 0));
		List<Question> unProcessedQuestion = groups.get(true);
		List<Question> processedQuestion = groups.get(false);

		List<HashMap<String, Integer>> termFrequencies = FeatureExtractor.GetTermFrequencies(
				FeatureExtractor
						.GetTokensFromString(unProcessedQuestion.stream()
								.map(q -> q.getTitleAndBodyCombinedText())
								.collect(Collectors.toList())));

		List<String> questionTerms = termFrequencies.stream()
				.flatMap(mp -> mp.keySet()
						.stream())
				.distinct()
				.collect(Collectors.toList());

		HashMap<String, Term> termMap = questionTerms.size() > 0
				? termRepository.fetchOrStoreByName(questionTerms)
				: new HashMap<String, Term>();

		return concat(
				IntStream.range(0, termFrequencies.size())
						.boxed()
						.map(index -> questionTermFrequencyRepository
								.createQuestionTermFrequencies(termMap, unProcessedQuestion.get(index),
										termFrequencies.get(index))),

				processedQuestion.stream()
						.map(q -> q.getQuestionTermFrequencies())

		)
				.map(questionTermFrequencies -> questionTermFrequencies
						.stream()
						.map(questionTermFrequency -> new AVPair(
								questionTermFrequency.getTerm()
										.getIndex(),
								questionTermFrequency.getFrequency()))
						.toArray(AVPair[]::new))
				.toArray(AVPair[][]::new);
	}

	public List<Question> fetchAndStoreNextChunkOfQuestions(int count) {
		// fetch data using stack overflow API.
		List<com.google.code.stackexchange.schema.Question> seQuestions = fetchStackOverflowQuestions(
				(Math.toIntExact(questionRepository.getTotalCount()) / pageSize) + 1);
		List<Question> questions = QuestionAdapter.convertStackOverflowQuestion(seQuestions);

		// convert/map the tags to data store tags.
		List<String> tags = seQuestions.stream()
				.flatMap(q -> q.getTags()
						.stream())
				.distinct()
				.collect(Collectors.toList());
		HashMap<String, Tag> tagMap = labelRepository.fetchOrStoreByName(tags);

		// add tags to the question
		questions.forEach(q -> q.getTags()
				.addAll(q.getTagNames()
						.stream()
						.map(tagName -> tagMap.get(tagName))
						.collect(Collectors.toSet())));

		// add the new questions to data store
		questionRepository.create(questions);

		return questions.subList(0, Math.min(count, pageSize));
	}

	private List<com.google.code.stackexchange.schema.Question> fetchStackOverflowQuestions(int pageNumber) {

		PagedList<com.google.code.stackexchange.schema.Question> questions = queryFactory.newQuestionApiQuery()
				.withPaging(new Paging(pageNumber, pageSize))
				.withFilter("withbody")// "!9YdnSIN18";//"default";
				.withSort(com.google.code.stackexchange.schema.Question.SortOrder.LEAST_RECENTLY_CREATED)
				// .withTags(tag)
				.list();

		try {
			int backOff = questions.getBackoff();
			if (backOff > 0) {
				logger.info("backoff for " + backOff + " seconds.");
				Thread.sleep(backOff * 1000);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		return questions;
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
		return buffer == null
				? Math.toIntExact(termRepository.getTotalCount())
				: maxFeatureIndex + 1;
	}

	@Override
	public int getNumberOfLabels() {
		return buffer == null
				? labelRepository.getTotalNumberOfLabels()
				: maxLabelIndex + 1;
	}

	@Override
	public void reset() {
		bufferIndex = 0;
	}

	@Override
	public void onInstanceProcessed(AbstractLearner learner, InstanceProcessedEventArgs args) {
		markProcessed(args.instance, learner.getId(), args.fmeasure, args.topkFmeasure, args.isPrequential, false);
	}

	@Override
	public double getAverageFmeasure(AbstractLearner learner, boolean isPrequential, boolean isTopk) {
		return this.questionLearnerDetailsRepository.getAverageFmeasure((UUID) learner.getId(),
				isPrequential, isTopk);
	}

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
	public void loadNext(int count) {
		throw new RuntimeException(
				"This method is not implemented for StackOverflowDatamanager. Use the loadNext(int, UUID) varient");
	}

	@Override
	public void onInstanceTested(AbstractLearner learner, InstanceTestedEventArgs args) {
		markProcessed(args.instance, learner.getId(), args.fmeasure, args.topkFmeasure, false, true);
	}

	@Override
	public double getTestAverageFmeasure(AbstractLearner learner, boolean isTopk) {
		return questionLearnerDetailsRepository.getTestAverageFmeasure(learner.getId(), isTopk);
	}

	public void load(UUID questionId) throws IOException {
		load(questionRepository.read(questionId), false);
	}

	public void load(Question question, boolean isCustomQuestion) throws IOException {
		AVPair[] x = !isCustomQuestion ? buildFeatureMatrix(Arrays.asList(question))[0]
				: buildFeatureVectorForCustomQuestion(question);
		int[] y = buildLabelVector(question);

		if (!buffer.isEmpty())
			buffer.clear();

		buffer.add(new SimpleEntry<Instance, UUID>(new Instance(x, y), question.getId()));
		reset();
	}

	private AVPair[] buildFeatureVectorForCustomQuestion(Question question) throws IOException {

		HashMap<String, Integer> termFrequencies = FeatureExtractor
				.GetTermFrequencies(FeatureExtractor.GetTokensFromString(question.getTitleAndBodyCombinedText()));

		HashMap<String, Term> termMap = termRepository.fetchOrStoreByName(termFrequencies.keySet()
				.stream()
				.distinct()
				.collect(Collectors.toList()));

		return termMap.entrySet()
				.stream()
				.map(entry -> new AVPair(entry.getValue()
						.getIndex(), termFrequencies.get(entry.getKey())))
				.toArray(AVPair[]::new);
	}
}