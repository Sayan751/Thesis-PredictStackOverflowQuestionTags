package thesis.webapi.services;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.code.stackexchange.client.query.StackExchangeApiQueryFactory;
import com.google.code.stackexchange.schema.StackExchangeSite;
import com.google.common.primitives.Ints;

import Data.Instance;
import Learner.AbstractLearner;
import Learner.AdaptivePLT;
import Learner.PLTAdaptiveEnsemble;
import Learner.PLTEnsembleBoosted;
import Learner.PLTEnsembleBoostedWithThreshold;
import event.args.InstanceProcessedEventArgs;
import event.listeners.IInstanceProcessedListener;
import thesis.data.entities.Learner;
import thesis.data.entities.Question;
import thesis.data.managers.StackOverflowQuestionManager;
import thesis.data.repositories.LearnerRepository;
import thesis.data.repositories.QuestionLearnerDetailsRepository;
import thesis.data.repositories.QuestionRepository;
import thesis.data.repositories.QuestionTermFrequencyRepository;
import thesis.data.repositories.TagRepository;
import thesis.data.repositories.TermRepository;
import thesis.data.repositoryinterfaces.IQuestionLearnerDetailsRepository;
import thesis.data.repositoryinterfaces.IQuestionRepository;
import thesis.data.repositoryinterfaces.IQuestionTermFrequencyRepository;
import thesis.data.repositoryinterfaces.ITagRepository;
import thesis.data.repositoryinterfaces.ITermRepository;
import thesis.util.LearnerUtil;
import thesis.webapi.adapters.LearnerAdapter;
import thesis.webapi.adapters.QuestionAdapter;
import thesis.webapi.adapters.TagAdapter;
import thesis.webapi.dto.FmeasuresInfo;
import thesis.webapi.dto.LearnerConfigDto;
import thesis.webapi.dto.LearnerFmeasuresDto;
import thesis.webapi.dto.LearnerInfoDto;
import thesis.webapi.dto.PredictionDto;
import thesis.webapi.dto.QuestionDto;
import thesis.webapi.dto.TagDto;
import threshold.ThresholdTunerInitOption;
import threshold.ThresholdTuners;
import util.AdaptivePLTInitConfiguration;
import util.Constants.LearnerDefaultValues;
import util.LearnerInitConfiguration;
import util.PLTAdaptiveEnsembleInitConfiguration;
import util.PLTEnsembleBoostedInitConfiguration;
import util.PLTEnsembleBoostedWithThresholdInitConfiguration;
import util.PLTInitConfiguration;

@Path("learner")
public class LearnerService implements IInstanceProcessedListener {
	static Logger logger = LogManager.getLogger(LearnerService.class);

	private LearnerRepository learnerRepository;
	private ITagRepository labelRepository;
	private IQuestionRepository questionRepository;
	private IQuestionTermFrequencyRepository questionTermFrequencyRepository;
	private ITermRepository termRepository;
	private IQuestionLearnerDetailsRepository questionLearnerDetailsRepository;

	private StackExchangeApiQueryFactory queryFactory;
	private StackOverflowQuestionManager soDataManager;

	private Class<? extends AbstractLearner> currentLearnerClass;
	private LearnerInitConfiguration currentLearnerConfig;
	private AbstractLearner currentLearner;
	private Learner currentLearnerEntity;

	private FmeasuresInfo currentPrequentialFmeasures, currentPostTrainFmeasures;

	public LearnerService() {
		initRepositories();
		initSODataManager();
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public LearnerInfoDto getLearnerInfo(@PathParam("id") UUID learnerId) {
		LearnerInfoDto retVal = null;
		setCurrentLearnerById(learnerId);

		/*non-null currentLearner ensures that other required objects below are also non null.*/
		if (currentLearner != null)
			retVal = LearnerAdapter.toLearnerInfo(learnerId, currentLearnerEntity.getCreatedOn(), currentLearnerConfig,
					currentLearnerClass, currentLearner);
		return retVal;
	}

	@GET
	@Path("/{id}/fm")
	@Produces(MediaType.APPLICATION_JSON)
	public LearnerFmeasuresDto getLearnerFmeasures(@PathParam("id") UUID learnerId) {
		currentLearnerEntity = learnerRepository.read(learnerId);
		if (currentLearnerEntity != null) {
			final LearnerFmeasuresDto retVal = new LearnerFmeasuresDto(learnerId);

			currentLearnerEntity.getQuestionLearnerDetails()
					.stream()
					.sorted((e1, e2) -> e1.getCreatedOn()
							.compareTo(e2.getCreatedOn()))
					.forEach(qld -> {
						if (qld.isPrequential()) {
							if (qld.isTopk()) {
								retVal.preqTopkFm.add(qld.getfMeasure());
							} else {
								retVal.preqGenFm.add(qld.getfMeasure());
							}
						} else {
							if (qld.isTopk()) {
								retVal.postTrainTopkFm.add(qld.getfMeasure());
							} else {
								retVal.postTrainGenFm.add(qld.getfMeasure());
							}
						}
					});
			return retVal;
		}
		return null;
	}

	@GET
	@Path("/{id}/trainNext")
	@Produces(MediaType.APPLICATION_JSON)
	public LearnerInfoDto trainOnNextInstance(@PathParam("id") UUID learnerId) {
		LearnerInfoDto retVal = null;
		setCurrentLearnerById(learnerId);

		if (currentLearner != null) {
			soDataManager.loadNext(1, learnerId);
			currentLearner.train(soDataManager);
			learnerRepository.update(learnerId, currentLearner);

			retVal = LearnerAdapter.buildPostTrainingLearnerInfo(currentLearner, currentPrequentialFmeasures,
					currentPostTrainFmeasures);
		}
		return retVal;
	}

	@GET
	@Path("/{id}/train/{qid}")
	@Produces(MediaType.APPLICATION_JSON)
	public LearnerInfoDto trainOnQuestion(@PathParam("id") UUID learnerId, @PathParam("qid") UUID questionId)
			throws IOException {
		LearnerInfoDto retVal = null;
		setCurrentLearnerById(learnerId);

		if (currentLearner != null) {
			soDataManager.load(questionId);
			currentLearner.train(soDataManager);
			learnerRepository.update(learnerId, currentLearner);

			retVal = LearnerAdapter.buildPostTrainingLearnerInfo(currentLearner, currentPrequentialFmeasures,
					currentPostTrainFmeasures);
		}
		return retVal;
	}

	@GET
	@Path("/{id}/predict/{qid}")
	@Produces(MediaType.APPLICATION_JSON)
	public PredictionDto predictOnQuestion(@PathParam("id") UUID learnerId, @PathParam("qid") UUID questionId)
			throws IOException {
		PredictionDto retVal = null;
		setCurrentLearnerById(learnerId);

		if (currentLearner != null) {
			soDataManager.load(questionId);
			retVal = buildPredictionForInstance(learnerId, questionId, soDataManager.getNextInstance());
		}
		return retVal;
	}

	@GET
	@Path("/{id}/nextQuestion")
	@Produces(MediaType.APPLICATION_JSON)
	public QuestionDto getNextUnprocessedQuestion(@PathParam("id") UUID learnerId) {
		// skipped checking for learner, bit 'unsecure', but ok.
		soDataManager.loadNext(1, learnerId);
		UUID questionId = soDataManager.getBufferedQuestionIdForInstance(soDataManager.getNextInstance());
		return QuestionAdapter.toDto(questionRepository.read(questionId));
	}

	@POST
	@Path("/create")
	// @Consumes("application/json")
	public UUID createLearner(LearnerConfigDto config) {
		UUID learnerId = null;
		System.out.println(config);

		ObjectMapper mapper = new ObjectMapper();

		Class<? extends LearnerInitConfiguration> learnerInitClass = null;
		try {
			learnerInitClass = setLearnerClassAndConfig(config, mapper);

			currentLearner = currentLearnerClass.getConstructor(learnerInitClass)
					.newInstance(currentLearnerConfig);

			learnerId = learnerRepository
					.create(new Learner(mapper.writeValueAsString(currentLearnerConfig)), currentLearner)
					.getId();

			currentLearner.setId(learnerId);
			soDataManager.loadNext(1, learnerId);
			currentLearner.allocateClassifiers(soDataManager);
			learnerRepository.update(learnerId, currentLearner);

			System.out.println(learnerId);

		} catch (IOException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			logger.error(e.getMessage(), e);
		}

		return learnerId;
	}

	@POST
	@Path("/{id}/trainCustom")
	public LearnerInfoDto train(@PathParam("id") UUID learnerId, QuestionDto question) throws IOException {

		LearnerInfoDto retVal = null;
		setCurrentLearnerById(learnerId, true);

		if (currentLearner != null) {

			Question questionEntity = QuestionAdapter.createWithTitleAndBody(question);
			questionEntity.setId(UUID.randomUUID());
			questionEntity.setTags(new HashSet<>(labelRepository.fetchOrStoreByName(question.tags.stream()
					.map(t -> t.name)
					.collect(Collectors.toList()))
					.values()));

			soDataManager.load(questionEntity, true);
			currentLearner.train(soDataManager);
			learnerRepository.update(learnerId, currentLearner);

			retVal = LearnerAdapter.buildPostTrainingLearnerInfo(currentLearner,
					currentPrequentialFmeasures,
					currentPostTrainFmeasures);
		}
		return retVal;
	}

	@POST
	@Path("/{id}/predictCustom")
	public PredictionDto predict(@PathParam("id") UUID learnerId, QuestionDto question) throws IOException {

		PredictionDto retVal = null;
		setCurrentLearnerById(learnerId, true);

		if (currentLearner != null) {

			final UUID questionId = UUID.randomUUID();
			Question questionEntity = QuestionAdapter.createWithTitleAndBody(question);
			questionEntity.setId(questionId);
			questionEntity.setTags(new HashSet<>(labelRepository.fetchOrStoreByName(question.tags.stream()
					.map(t -> t.name)
					.collect(Collectors.toList()))
					.values()));

			soDataManager.load(questionEntity, true);
			retVal = buildPredictionForInstance(learnerId, questionId, soDataManager.getNextInstance());

		}
		return retVal;
	}

	private Class<? extends LearnerInitConfiguration> setLearnerClassAndConfig(LearnerConfigDto config,
			ObjectMapper mapper)
			throws IOException, JsonProcessingException {

		Class<? extends LearnerInitConfiguration> learnerInitClass = null;
		if (config.learnerClass.equals("AdaptivePLT")) {

			currentLearnerClass = AdaptivePLT.class;
			learnerInitClass = AdaptivePLTInitConfiguration.class;
			setAdaptivePltConfig(config.configJson, mapper);

		} else if (config.learnerClass.equals("PLTEnsembleBoosted")) {

			currentLearnerClass = PLTEnsembleBoosted.class;
			learnerInitClass = PLTEnsembleBoostedInitConfiguration.class;
			setBoostedEnsembleConfig(config.configJson, mapper);

		} else if (config.learnerClass.equals("PLTEnsembleBoostedWithThreshold")) {

			currentLearnerClass = PLTEnsembleBoostedWithThreshold.class;
			learnerInitClass = PLTEnsembleBoostedWithThresholdInitConfiguration.class;
			setBoostedEnsembleWithThresholdConfig(config.configJson, mapper);

		} else if (config.learnerClass.equals("PLTAdaptiveEnsemble")) {

			currentLearnerClass = PLTAdaptiveEnsemble.class;
			learnerInitClass = PLTAdaptiveEnsembleInitConfiguration.class;
			setAdaptiveEnsembleConfig(config.configJson, mapper);
		}

		currentLearnerConfig.fmeasureObserver = soDataManager;
		return learnerInitClass;
	}

	private void setAdaptivePltConfig(String json, ObjectMapper mapper)
			throws IOException, JsonParseException, JsonMappingException {
		AdaptivePLTInitConfiguration adaptivePltConfig = mapper.readValue(json, AdaptivePLTInitConfiguration.class);

		// set the other required fields that are not provided by client.
		adaptivePltConfig.tunerInitOption = new ThresholdTunerInitOption(1, 2);
		adaptivePltConfig.setHasher("AdaptiveMurmur");
		adaptivePltConfig.tunerType = ThresholdTuners.AdaptiveOfoFast;

		currentLearnerConfig = adaptivePltConfig;
	}

	private void setBoostedEnsembleConfig(String json, ObjectMapper mapper)
			throws IOException, JsonParseException, JsonMappingException {

		PLTEnsembleBoostedInitConfiguration config = mapper.readValue(json, PLTEnsembleBoostedInitConfiguration.class);

		// set the other required fields that are not provided by client.
		config.tunerInitOption = new ThresholdTunerInitOption(1, 2);
		if (config.individualPLTConfiguration == null)
			config.individualPLTConfiguration = new AdaptivePLTInitConfiguration();
		config.individualPLTConfiguration.tunerInitOption = new ThresholdTunerInitOption(1, 2);
		config.individualPLTConfiguration.setHasher("AdaptiveMurmur");
		config.individualPLTConfiguration.tunerType = ThresholdTuners.AdaptiveOfoFast;
		config.learnerRepository = learnerRepository;

		currentLearnerConfig = config;

	}

	private void setBoostedEnsembleWithThresholdConfig(String json, ObjectMapper mapper)
			throws IOException, JsonParseException, JsonMappingException {

		PLTEnsembleBoostedWithThresholdInitConfiguration config = mapper.readValue(json,
				PLTEnsembleBoostedWithThresholdInitConfiguration.class);

		// set the other required fields that are not provided by client.
		config.tunerInitOption = new ThresholdTunerInitOption(1, 2);
		if (config.individualPLTConfiguration == null)
			config.individualPLTConfiguration = new AdaptivePLTInitConfiguration();
		config.individualPLTConfiguration.tunerInitOption = new ThresholdTunerInitOption(1, 2);
		config.individualPLTConfiguration.setHasher("AdaptiveMurmur");
		config.individualPLTConfiguration.tunerType = ThresholdTuners.AdaptiveOfoFast;
		config.learnerRepository = learnerRepository;

		currentLearnerConfig = config;

	}

	private void setAdaptiveEnsembleConfig(String json, ObjectMapper mapper)
			throws IOException, JsonParseException, JsonMappingException {

		PLTAdaptiveEnsembleInitConfiguration config = mapper.readValue(json,
				PLTAdaptiveEnsembleInitConfiguration.class);

		// set the other required fields that are not provided by client.
		config.tunerInitOption = new ThresholdTunerInitOption(1, 2);
		if (config.individualPLTProperties == null)
			config.individualPLTProperties = new PLTInitConfiguration();
		config.individualPLTProperties.tunerInitOption = new ThresholdTunerInitOption(1, 2);
		config.individualPLTProperties.setHasher("AdaptiveMurmur");
		config.individualPLTProperties.tunerType = ThresholdTuners.AdaptiveOfoFast;
		config.learnerRepository = learnerRepository;

		currentLearnerConfig = config;

	}

	private void setCurrentLearnerById(UUID learnerId) {
		setCurrentLearnerById(learnerId, false);
	}

	private void setCurrentLearnerById(UUID learnerId, boolean isCustom) {
		currentLearnerEntity = learnerRepository.read(learnerId);
		if (currentLearnerEntity != null) {

			SimpleEntry<Class<? extends AbstractLearner>, ? extends LearnerInitConfiguration> classAndConfig = LearnerUtil
					.getLearnerClassAndConfig(currentLearnerEntity.getLearnerDetails());
			currentLearnerClass = classAndConfig.getKey();
			currentLearnerConfig = classAndConfig.getValue();

			if (currentLearnerClass != null) {
				currentLearner = learnerRepository.read(learnerId, currentLearnerClass);

				if (currentLearner != null) {
					currentLearner.addInstanceProcessedListener(this);
					if (currentLearner.fmeasureObserverAvailable) {
						currentLearner.fmeasureObserver = soDataManager;
						if (!isCustom) {
							currentLearner.addInstanceProcessedListener(soDataManager);
							currentLearner.addInstanceTestedListener(soDataManager);
						}
					}
					if (currentLearnerClass == PLTEnsembleBoosted.class) {
						((PLTEnsembleBoosted) currentLearner).setLearnerRepository(learnerRepository);
					} else if (currentLearnerClass == PLTEnsembleBoostedWithThreshold.class) {
						((PLTEnsembleBoostedWithThreshold) currentLearner).setLearnerRepository(learnerRepository);
					} else if (currentLearnerClass == PLTAdaptiveEnsemble.class) {
						((PLTAdaptiveEnsemble) currentLearner).setLearnerRepository(learnerRepository);
						// TODO:
						// pltEnsemble.addPLTCreatedListener(this);
						// pltEnsemble.addPLTDiscardedListener(this.learnerRepository);
					}

				}
			}
		}
	}

	private PredictionDto buildPredictionForInstance(UUID learnerId, UUID questionId,
			Instance instance) {

		Set<Integer> predictedPositives = currentLearner.getPositiveLabels(instance.x);
		int[] topkPredictions = currentLearner.getTopkLabels(instance.x, LearnerDefaultValues.defaultK);

		Set<Integer> allPredictedTags = new HashSet<>(predictedPositives);
		allPredictedTags.addAll(Ints.asList(topkPredictions));

		if (!allPredictedTags.isEmpty()) {
			Collection<TagDto> tags = TagAdapter.toDto(labelRepository.getByIndices(allPredictedTags));

			final PredictionDto retVal = new PredictionDto(learnerId, questionId, topkPredictions.length);

			List<Integer> topKList = Ints.asList(topkPredictions);

			tags.forEach(tag -> {
				try {
					final int tagIndex = tag.index;

					if (predictedPositives.contains(tagIndex))
						retVal.general.add(tag);

					// done to maintain the order
					int topkIndex = topKList.indexOf(tagIndex);
					if (topkIndex > -1)
						retVal.topk.set(topkIndex, tag);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			return retVal;
		}
		return null;
	}

	private void initRepositories() {
		learnerRepository = new LearnerRepository();
		labelRepository = new TagRepository();
		questionRepository = new QuestionRepository();
		questionTermFrequencyRepository = new QuestionTermFrequencyRepository();
		termRepository = new TermRepository();
		questionLearnerDetailsRepository = new QuestionLearnerDetailsRepository();
	}

	private void initSODataManager() {
		queryFactory = StackExchangeApiQueryFactory.newInstance("pj9WKZ7g4MJ6XKsKWuxKmA((",
				StackExchangeSite.STACK_OVERFLOW);
		soDataManager = new StackOverflowQuestionManager(labelRepository, questionRepository,
				questionTermFrequencyRepository,
				termRepository, queryFactory, questionLearnerDetailsRepository);
	}

	@Override
	public void onInstanceProcessed(AbstractLearner learner, InstanceProcessedEventArgs args) {
		if (learner.getId()
				.equals(currentLearner.getId())) {
			FmeasuresInfo fm = new FmeasuresInfo();
			fm.general = args.fmeasure;
			fm.topk = args.topkFmeasure;
			if (args.isPrequential)
				currentPrequentialFmeasures = fm;
			else
				currentPostTrainFmeasures = fm;
		}

	}
}
