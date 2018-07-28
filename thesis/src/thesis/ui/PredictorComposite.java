package thesis.ui;

import static thesis.util.Constants.Ui.Messages.cantFetchQuestion;
import static thesis.util.Constants.Ui.Messages.learnerNotFound;
import static thesis.util.Constants.Ui.Messages.unknownException;
import static thesis.util.Constants.Ui.Messages.unrecognizedLearner;
import static thesis.util.Constants.Ui.Seperators.tagSeparator;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.code.stackexchange.client.query.StackExchangeApiQueryFactory;
import com.google.code.stackexchange.schema.StackExchangeSite;
import com.google.common.base.Joiner;
import com.google.common.primitives.Ints;

import Data.Instance;
import Learner.AbstractLearner;
import Learner.AdaptivePLT;
import Learner.PLTAdaptiveEnsemble;
import Learner.PLTEnsembleBoosted;
import Learner.PLTEnsembleBoostedWithThreshold;
import thesis.data.entities.Learner;
import thesis.data.entities.Question;
import thesis.data.entities.Tag;
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
import threshold.ThresholdTunerInitOption;
import threshold.ThresholdTuners;
import util.AdaptivePLTInitConfiguration;
import util.Constants.LearnerDefaultValues;
import util.LearnerInitConfiguration;
import util.PLTAdaptiveEnsembleInitConfiguration;
import util.PLTEnsembleBoostedInitConfiguration;
import util.PLTEnsembleBoostedWithThresholdInitConfiguration;
import util.PLTInitConfiguration;

public class PredictorComposite extends Composite {

	private static Logger logger = LogManager.getLogger(PredictorComposite.class);

	private LearnerRepository learnerRepository;
	private ITagRepository labelRepository;
	private IQuestionRepository questionRepository;
	private IQuestionTermFrequencyRepository questionTermFrequencyRepository;
	private ITermRepository termRepository;
	private IQuestionLearnerDetailsRepository questionLearnerDetailsRepository;

	private StackExchangeApiQueryFactory queryFactory;
	private StackOverflowQuestionManager soDataManager;

	private Text textLearnerId;
	private Text textLearnerDetails, textTruePositives, textTopKPredictions, textPredictions;
	private Link linkTitle;
	private Browser browserQuestionBody;
	private Group grpLearnerDetails, grpCreateNewLearner;
	private Button btnPickQuestion, buttonPredictTags, buttonTrain, btnGetLearner, btnAdaptivePlt, btnBoostedEnsemble,
			btnBoostedWithThreshold, btnAdaptiveEnsemble, chkCreateNewLearner;
	private ProgressBar progressBar;

	private ObjectMapper mapper;
	private UUID learnerId;
	private Class<? extends AbstractLearner> learnerClass;
	private Class<? extends LearnerInitConfiguration> initConfigClass;
	private LearnerInitConfiguration learnerConfig = null;
	private AbstractLearner learner;
	private Question question;
	private String questionLink;
	private final String questionUrlFormat = "http://stackoverflow.com/questions/{0,number,#}";
	private final String questionTitleLinkFormat = "<a href=\"{0}\">{1}</a>";
	private Button btnTrainOnNext;

	public PredictorComposite(Composite parent, int style) {
		super(parent, style);
		mapper = new ObjectMapper();
		initRepositories();
		initSODataManager();
		initUi();
	}

	private void initUi() {
		setLayout(new GridLayout(4, false));
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);

		Group grpLearner = new Group(this, SWT.NONE);
		grpLearner.setFont(SWTResourceManager.getFont("Segoe UI", 13, SWT.NORMAL));
		grpLearner.setLayout(new GridLayout(7, false));
		GridData gd_grpLearner = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd_grpLearner.heightHint = 621;
		gd_grpLearner.widthHint = 515;
		grpLearner.setLayoutData(gd_grpLearner);
		grpLearner.setText("Learner");

		grpCreateNewLearner = new Group(grpLearner, SWT.NONE);
		grpCreateNewLearner.setLayout(new GridLayout(4, false));
		GridData gd_grpCreateNewLearner = new GridData(SWT.LEFT, SWT.CENTER, false, false, 7, 1);
		gd_grpCreateNewLearner.widthHint = 485;
		gd_grpCreateNewLearner.heightHint = 94;
		grpCreateNewLearner.setLayoutData(gd_grpCreateNewLearner);
		grpCreateNewLearner.setText("Create new learner");
		grpCreateNewLearner.setFont(SWTResourceManager.getFont("Segoe UI", 13, SWT.NORMAL));

		chkCreateNewLearner = new Button(grpCreateNewLearner, SWT.CHECK);
		chkCreateNewLearner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				chkCreateNewLearnerChanged();
			}
		});
		chkCreateNewLearner.setFont(SWTResourceManager.getFont("Segoe UI", 11, SWT.NORMAL));
		chkCreateNewLearner.setText("Create new ");
		new Label(grpCreateNewLearner, SWT.NONE);
		new Label(grpCreateNewLearner, SWT.NONE);
		new Label(grpCreateNewLearner, SWT.NONE);

		btnBoostedEnsemble = new Button(grpCreateNewLearner, SWT.RADIO);
		btnBoostedEnsemble.setEnabled(false);
		btnBoostedEnsemble.setFont(SWTResourceManager.getFont("Segoe UI", 11, SWT.NORMAL));
		btnBoostedEnsemble.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				PLTEnsembleBoostedInitConfiguration config = new PLTEnsembleBoostedInitConfiguration();
				config.tunerInitOption = new ThresholdTunerInitOption(1, 2);
				config.individualPLTConfiguration = new AdaptivePLTInitConfiguration();
				config.individualPLTConfiguration.tunerInitOption = new ThresholdTunerInitOption(1, 2);
				config.individualPLTConfiguration.setHasher("AdaptiveMurmur");
				config.individualPLTConfiguration.tunerType = ThresholdTuners.AdaptiveOfoFast;
				config.individualPLTConfiguration.setHd(32768);

				learnerConfig = config;
				initConfigClass = PLTEnsembleBoostedInitConfiguration.class;
				learnerClass = PLTEnsembleBoosted.class;
				setLearnerConfiguration();
			}
		});
		btnBoostedEnsemble.setText("Boosted");

		btnBoostedWithThreshold = new Button(grpCreateNewLearner, SWT.RADIO);
		btnBoostedWithThreshold.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {

				PLTEnsembleBoostedWithThresholdInitConfiguration config = new PLTEnsembleBoostedWithThresholdInitConfiguration();
				config.tunerInitOption = new ThresholdTunerInitOption(1, 2);
				config.individualPLTConfiguration = new AdaptivePLTInitConfiguration();
				config.individualPLTConfiguration.tunerInitOption = new ThresholdTunerInitOption(1, 2);
				config.individualPLTConfiguration.setHasher("AdaptiveMurmur");
				config.individualPLTConfiguration.tunerType = ThresholdTuners.AdaptiveOfoFast;
				config.individualPLTConfiguration.setHd(32768);

				learnerConfig = config;
				initConfigClass = PLTEnsembleBoostedWithThresholdInitConfiguration.class;
				learnerClass = PLTEnsembleBoostedWithThreshold.class;
				setLearnerConfiguration();
			}
		});
		btnBoostedWithThreshold.setEnabled(false);
		btnBoostedWithThreshold.setFont(SWTResourceManager.getFont("Segoe UI", 11, SWT.NORMAL));
		btnBoostedWithThreshold.setText("Boosted with threshold");
		new Label(grpCreateNewLearner, SWT.NONE);

		Button btnNewButton = new Button(grpCreateNewLearner, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				createNewLearner(btnNewButton);
				chkCreateNewLearner.setSelection(false);
				chkCreateNewLearner.notifyListeners(SWT.Selection, null);
				textLearnerId.setText(learnerId.toString());
				btnGetLearner.notifyListeners(SWT.MouseUp, null);
			}
		});
		btnNewButton.setEnabled(false);
		btnNewButton.setFont(SWTResourceManager.getFont("Segoe UI", 13, SWT.NORMAL));
		btnNewButton.setText("Create");

		btnAdaptivePlt = new Button(grpCreateNewLearner, SWT.RADIO);
		btnAdaptivePlt.setEnabled(false);
		btnAdaptivePlt.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				AdaptivePLTInitConfiguration config = new AdaptivePLTInitConfiguration();
				config.tunerInitOption = new ThresholdTunerInitOption(1, 2);
				config.setHasher("AdaptiveMurmur");
				config.tunerType = ThresholdTuners.AdaptiveOfoFast;
				config.setHd(32768);

				learnerConfig = config;
				initConfigClass = AdaptivePLTInitConfiguration.class;
				learnerClass = AdaptivePLT.class;
				setLearnerConfiguration();
			}
		});
		btnAdaptivePlt.setFont(SWTResourceManager.getFont("Segoe UI", 11, SWT.NORMAL));
		btnAdaptivePlt.setText("Adaptive PLT");

		btnAdaptiveEnsemble = new Button(grpCreateNewLearner, SWT.RADIO);
		btnAdaptiveEnsemble.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				PLTAdaptiveEnsembleInitConfiguration config = new PLTAdaptiveEnsembleInitConfiguration();
				config.tunerInitOption = new ThresholdTunerInitOption(1, 2);
				config.individualPLTProperties = new PLTInitConfiguration();
				config.individualPLTProperties.tunerInitOption = new ThresholdTunerInitOption(1, 2);
				config.individualPLTProperties.setHasher("AdaptiveMurmur");
				config.individualPLTProperties.tunerType = ThresholdTuners.AdaptiveOfoFast;
				config.individualPLTProperties.setHd(32768);

				learnerConfig = config;
				initConfigClass = PLTAdaptiveEnsembleInitConfiguration.class;
				learnerClass = PLTAdaptiveEnsemble.class;
				setLearnerConfiguration();
			}
		});
		btnAdaptiveEnsemble.setEnabled(false);
		btnAdaptiveEnsemble.setFont(SWTResourceManager.getFont("Segoe UI", 11, SWT.NORMAL));
		btnAdaptiveEnsemble.setText("Adaptive Ensemble");
		new Label(grpCreateNewLearner, SWT.NONE);
		new Label(grpCreateNewLearner, SWT.NONE);

		Label lblLearnerId = new Label(grpLearner, SWT.NONE);
		lblLearnerId.setFont(SWTResourceManager.getFont("Segoe UI", 13, SWT.NORMAL));
		lblLearnerId.setText("Learner Id");

		textLearnerId = new Text(grpLearner, SWT.BORDER);
		GridData gd_textLearnerId = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_textLearnerId.widthHint = 287;
		textLearnerId.setLayoutData(gd_textLearnerId);
		textLearnerId.setFont(SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));

		btnGetLearner = new Button(grpLearner, SWT.NONE);
		btnGetLearner.setFont(SWTResourceManager.getFont("Segoe UI", 13, SWT.NORMAL));
		btnGetLearner.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent arg0) {
				fetchLearner();
			}
		});
		btnGetLearner.setText("Get Learner");
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);

		grpLearnerDetails = new Group(grpLearner, SWT.NONE);
		grpLearnerDetails.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 5));
		grpLearnerDetails.setFont(SWTResourceManager.getFont("Segoe UI", 13, SWT.NORMAL));
		grpLearnerDetails.setText("Learner Details");

		textLearnerDetails = new Text(grpLearnerDetails,
				SWT.BORDER | SWT.READ_ONLY | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		textLearnerDetails.setFont(SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));
		textLearnerDetails.setBounds(10, 23, 358, 409);
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);

		btnPickQuestion = new Button(grpLearner, SWT.NONE);
		btnPickQuestion.setEnabled(false);
		btnPickQuestion.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent arg0) {
				setQuestion(null);
				fetchQuestion();
				if (question == null)
					setQuestionUnavailibilityDetails();
			}
		});
		btnPickQuestion.setFont(SWTResourceManager.getFont("Segoe UI", 13, SWT.NORMAL));
		btnPickQuestion.setText("Pick Question");
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);

		buttonPredictTags = new Button(grpLearner, SWT.NONE);
		buttonPredictTags.setEnabled(false);
		buttonPredictTags.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent arg0) {
				getPredictions();
			}
		});
		buttonPredictTags.setFont(SWTResourceManager.getFont("Segoe UI", 13, SWT.NORMAL));
		buttonPredictTags.setText("Predict Tags");
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);

		buttonTrain = new Button(grpLearner, SWT.NONE);
		buttonTrain.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		buttonTrain.setEnabled(false);
		buttonTrain.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent arg0) {
				train();
			}
		});
		buttonTrain.setFont(SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));
		buttonTrain.setText("Train on current");
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);

		btnTrainOnNext = new Button(grpLearner, SWT.NONE);
		btnTrainOnNext.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent arg0) {
				trainOnNextNInstances(10);
			}
		});
		btnTrainOnNext.setEnabled(false);
		btnTrainOnNext.setFont(SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));
		btnTrainOnNext.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		btnTrainOnNext.setText("Train on next 10");
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);
		new Label(grpLearner, SWT.NONE);

		Group grpQuestion = new Group(this, SWT.NONE);
		grpQuestion.setFont(SWTResourceManager.getFont("Segoe UI", 13, SWT.NORMAL));
		GridData gd_grpQuestion = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_grpQuestion.widthHint = 807;
		gd_grpQuestion.heightHint = 649;
		grpQuestion.setLayoutData(gd_grpQuestion);
		grpQuestion.setText("Question");

		Label lblTitle = new Label(grpQuestion, SWT.NONE);
		lblTitle.setFont(SWTResourceManager.getFont("Segoe UI", 13, SWT.NORMAL));
		lblTitle.setBounds(10, 26, 55, 23);
		lblTitle.setText("Title");

		linkTitle = new Link(grpQuestion, SWT.NONE);
		linkTitle.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent arg0) {
				navigateToQuestion();
			}
		});
		linkTitle.setFont(SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));
		linkTitle.setBounds(77, 26, 431, 23);

		Label lblBody = new Label(grpQuestion, SWT.NONE);
		lblBody.setFont(SWTResourceManager.getFont("Segoe UI", 13, SWT.NORMAL));
		lblBody.setBounds(10, 62, 55, 23);
		lblBody.setText("Body");

		Group group = new Group(grpQuestion, SWT.NONE);
		group.setFont(SWTResourceManager.getFont("Segoe UI", 13, SWT.NORMAL));
		group.setText("Tags");
		group.setBounds(10, 426, 212, 142);

		textTruePositives = new Text(group,
				SWT.BORDER | SWT.READ_ONLY | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL);
		textTruePositives.setFont(SWTResourceManager.getFont("Segoe UI", 11, SWT.NORMAL));
		textTruePositives.setBounds(10, 29, 192, 103);

		Label label_1 = new Label(grpQuestion, SWT.SEPARATOR | SWT.VERTICAL);
		label_1.setBounds(228, 446, 12, 158);

		Group group_1 = new Group(grpQuestion, SWT.NONE);
		group_1.setFont(SWTResourceManager.getFont("Segoe UI", 13, SWT.NORMAL));
		group_1.setBounds(246, 514, 554, 108);
		group_1.setText("Genral predictions");

		textPredictions = new Text(group_1,
				SWT.BORDER | SWT.READ_ONLY | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL);
		textPredictions.setFont(SWTResourceManager.getFont("Segoe UI", 11, SWT.NORMAL));
		textPredictions.setBounds(10, 28, 534, 70);

		progressBar = new ProgressBar(grpQuestion, SWT.NONE);
		progressBar.setBounds(38, 224, 202, 29);
		progressBar.setVisible(false);

		Group group_2 = new Group(grpQuestion, SWT.NONE);
		group_2.setFont(SWTResourceManager.getFont("Segoe UI", 13, SWT.NORMAL));
		group_2.setText("Top K predictions");
		group_2.setBounds(241, 426, 554, 82);

		textTopKPredictions = new Text(group_2,
				SWT.BORDER | SWT.READ_ONLY | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL);
		textTopKPredictions.setFont(SWTResourceManager.getFont("Segoe UI", 11, SWT.NORMAL));
		textTopKPredictions.setBounds(10, 26, 534, 46);

		browserQuestionBody = new Browser(grpQuestion, SWT.NONE);
		browserQuestionBody.setFont(SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));
		browserQuestionBody.setBounds(10, 91, 785, 329);
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
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

	private void fetchLearner() {
		setLearner(null);
		setQuestion(null);

		String strLearnerId = textLearnerId.getText()
				.trim();

		if (strLearnerId != null && !strLearnerId.isEmpty()) {

			learnerId = UUID.fromString(strLearnerId);
			Learner learnerEntity = learnerRepository.read(learnerId);

			if (learnerEntity != null) {

				// setLearnerType(learnerEntity.getLearnerDetails());
				SimpleEntry<Class<? extends AbstractLearner>, ? extends LearnerInitConfiguration> classAndConfig = LearnerUtil
						.getLearnerClassAndConfig(learnerEntity.getLearnerDetails());
				learnerClass = classAndConfig.getKey();
				learnerConfig = classAndConfig.getValue();
				if (learnerClass == null) {
					textLearnerDetails.setText(unrecognizedLearner);
				}
				setLearner(learnerRepository.read(learnerId, learnerClass));

				if (learner == null)
					textLearnerDetails.setText(learnerNotFound);
				else {
					if (learner.fmeasureObserverAvailable) {
						learner.fmeasureObserver = soDataManager;
						learner.addInstanceProcessedListener(soDataManager);
						learner.addInstanceTestedListener(soDataManager);
					}
					if (learner instanceof PLTEnsembleBoosted) {

						((PLTEnsembleBoosted) learner).setLearnerRepository(learnerRepository);

					} else if (learner instanceof PLTEnsembleBoostedWithThreshold) {

						((PLTEnsembleBoostedWithThreshold) learner).setLearnerRepository(learnerRepository);

					} else if (learner instanceof PLTAdaptiveEnsemble) {

						((PLTAdaptiveEnsemble) learner).setLearnerRepository(learnerRepository);
						// TODO:
						// pltEnsemble.addPLTCreatedListener(this);
						// pltEnsemble.addPLTDiscardedListener(this.learnerRepository);
					}
					try {
						setLearnerDetailsText();
					} catch (JsonProcessingException e) {
						textLearnerDetails.setText(unknownException);
					}
				}
			} else {
				textLearnerDetails.setText(learnerNotFound);
			}
		}
	}

	private void setLearnerDetailsText() throws JsonProcessingException {
		StringBuilder sb = new StringBuilder();

		sb.append("Type: ");
		sb.append(learnerClass);

		sb.append("\n\n========================\n");

		sb.append("\nMacro f-measure: ");
		sb.append(learner.getMacroFmeasure() * 100);

		sb.append("\n\nAverage f-measure:");
		sb.append("\n\tPrequential: ");
		sb.append(MessageFormat.format("{0,number,0.00}", learner.getAverageFmeasure(true, false) * 100));
		sb.append(", ");
		sb.append(MessageFormat.format("{0,number,0.00}", learner.getAverageFmeasure(true, true) * 100));
		sb.append(", (top k)");

		sb.append("\n\tPost-training: ");
		sb.append(MessageFormat.format("{0,number,0.00}", learner.getAverageFmeasure(false, false) * 100));
		sb.append(", ");
		sb.append(MessageFormat.format("{0,number,0.00}", learner.getAverageFmeasure(false, true) * 100));
		sb.append(", (top k)");

		sb.append("\n\n========================\n\n");
		sb.append("Configuration: \n");

		sb.append(mapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(learnerConfig));
		textLearnerDetails.setText(sb.toString());
	}

	private void fetchQuestion() {
		soDataManager.loadNext(1, learnerId);
		UUID questionId = soDataManager.getBufferedQuestionIdForInstance(soDataManager.getNextInstance());
		soDataManager.reset();
		setQuestion(questionRepository.read(questionId));
	}

	private void setQuestionUnavailibilityDetails() {
		linkTitle.setText(cantFetchQuestion);
		browserQuestionBody.setText(cantFetchQuestion);
		textTruePositives.setText("");
	}

	private void navigateToQuestion() {
		if (questionLink != null && !questionLink.isEmpty()) {
			browserQuestionBody.setUrl(questionLink);
		}
	}

	private void getPredictions() {
		Instance instance = soDataManager.getNextInstance();
		soDataManager.reset();

		Set<Integer> predictedPositives = learner.getPositiveLabels(instance.x);
		int[] topkPredictions = learner.getTopkLabels(instance.x, LearnerDefaultValues.defaultK);

		Set<Integer> allPredictedTags = new HashSet<>(predictedPositives);
		allPredictedTags.addAll(Ints.asList(topkPredictions));

		if (!allPredictedTags.isEmpty()) {
			List<Tag> tags = labelRepository.getByIndices(allPredictedTags);

			List<String> predictedPositivesNames = new ArrayList<>();
			List<String> topKNames = new ArrayList<>(Collections.nCopies(topkPredictions.length, ""));
			List<Integer> topKList = Ints.asList(topkPredictions);

			tags.forEach(tag -> {
				try {
					final int tagIndex = tag.getIndex();
					final String tagName = tag.getName();

					if (predictedPositives.contains(tagIndex))
						predictedPositivesNames.add(tagName);

					// done to maintain the order
					int topkIndex = topKList.indexOf(tagIndex);
					if (topkIndex > -1)
						topKNames.set(topkIndex, tagName);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});

			textPredictions.setText(Joiner.on(tagSeparator)
					.join(predictedPositivesNames));
			textTopKPredictions.setText(Joiner.on(tagSeparator)
					.join(topKNames));
		}
	}

	private void train() {
		progressBar.setVisible(true);
		progressBar.setSelection(10);

		enableAllButtons(false);
		progressBar.setSelection(30);

		learner.train(soDataManager);
		progressBar.setSelection(95);

		enableAllButtons(true);

		try {
			setLearnerDetailsText();
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
		}
		progressBar.setSelection(100);
		progressBar.setVisible(false);
	}

	private void enableAllButtons(boolean enable) {
		btnGetLearner.setEnabled(enable);
		btnPickQuestion.setEnabled(enable);
		buttonTrain.setEnabled(enable);
		buttonPredictTags.setEnabled(enable);
		btnTrainOnNext.setEnabled(enable);
	}

	private void setLearnerConfiguration() {
		try {
			textLearnerDetails.setText(mapper.writerWithDefaultPrettyPrinter()
					.writeValueAsString(learnerConfig));
			textLearnerDetails.setEditable(true);
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			textLearnerDetails.setText("Something went wrong, please try again");
		}
	}

	private void createNewLearner(Button btnNewButton) {
		try {
			progressBar.setVisible(true);

			setNewLearnerConfig();
			progressBar.setSelection(10);

			setLearner(learnerClass.getConstructor(initConfigClass)
					.newInstance(learnerConfig));
			progressBar.setSelection(20);

			learnerId = learnerRepository.create(new Learner(mapper.writeValueAsString(learnerConfig)), learner)
					.getId();
			learner.setId(learnerId);
			learner.addInstanceProcessedListener(soDataManager);
			learner.addInstanceTestedListener(soDataManager);
			progressBar.setSelection(30);

			soDataManager.loadNext(1, learnerId);
			progressBar.setSelection(50);

			learner.allocateClassifiers(soDataManager);
			progressBar.setSelection(95);

			learnerRepository.update(learnerId, learner);
			progressBar.setSelection(100);
			progressBar.setVisible(false);

		} catch (IOException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			logger.error(e.getMessage(), e);
			String msg = "Something went wrong";
			if (e instanceof InvocationTargetException) {
				Throwable ex = ((InvocationTargetException) e).getTargetException();
				if (ex instanceof IllegalArgumentException) {
					msg = ex.getMessage();
				}
			}
			MessageBox info = new MessageBox(btnNewButton.getShell(), SWT.ICON_ERROR | SWT.OK);
			info.setText("Error");
			info.setMessage(msg);
			info.open();
		}
	}

	private void setNewLearnerConfig() throws IOException, JsonParseException, JsonMappingException {
		learnerConfig = mapper.readValue(textLearnerDetails.getText(), initConfigClass);
		learnerConfig.fmeasureObserver = soDataManager;

		if (learnerConfig instanceof PLTEnsembleBoostedInitConfiguration) {
			((PLTEnsembleBoostedInitConfiguration) learnerConfig).learnerRepository = learnerRepository;
		} else if (learnerConfig instanceof PLTEnsembleBoostedWithThresholdInitConfiguration) {
			((PLTEnsembleBoostedWithThresholdInitConfiguration) learnerConfig).learnerRepository = learnerRepository;
		} else if (learnerConfig instanceof PLTAdaptiveEnsembleInitConfiguration) {
			((PLTAdaptiveEnsembleInitConfiguration) learnerConfig).learnerRepository = learnerRepository;
		}
	}

	private void chkCreateNewLearnerChanged() {
		setQuestion(null);
		boolean isSelected = chkCreateNewLearner.getSelection();

		Arrays.stream(grpCreateNewLearner.getChildren())
				.forEach(child -> {
					child.setEnabled(isSelected);
					if (child instanceof Button) {
						((Button) child).setSelection(false);
					}
				});
		chkCreateNewLearner.setEnabled(true);
		chkCreateNewLearner.setSelection(isSelected);

		// cleanup
		if (isSelected) {
			textLearnerId.setText("");
			textLearnerDetails.setText("");
			linkTitle.setText("");
			browserQuestionBody.setText("");
			textTruePositives.setText("");
			textPredictions.setText("");
			textTopKPredictions.setText("");
		}

		textLearnerId.setEnabled(!isSelected);
		btnGetLearner.setEnabled(!isSelected);
	}

	/**
	 * @param question
	 *            the question to set
	 */
	public void setQuestion(Question question) {
		this.question = question;
		final boolean questionAvailable = this.question != null;

		buttonPredictTags.setEnabled(questionAvailable && learner != null && learner.getnTrain() > 0);
		buttonTrain.setEnabled(questionAvailable);

		if (questionAvailable) {
			questionLink = MessageFormat.format(questionUrlFormat, question.getSeQuestionId());
			linkTitle.setText(MessageFormat.format(questionTitleLinkFormat, questionLink, question.getTitle()));
			browserQuestionBody.setText(question.getBody());
			textTruePositives.setText(Joiner.on(tagSeparator)
					.join(question.getTagNames()));
		} else {
			textTruePositives.setText("");
			textPredictions.setText("");
			textTopKPredictions.setText("");
			linkTitle.setText("");
			browserQuestionBody.setText("");
		}
	}

	/**
	 * @param learner
	 *            the learner to set
	 */
	private void setLearner(AbstractLearner learner) {
		this.learner = learner;
		final boolean learnerAvailable = this.learner != null;
		btnPickQuestion.setEnabled(learnerAvailable);
		btnTrainOnNext.setEnabled(learnerAvailable);
	}

	private void trainOnNextNInstances(int n) {
		setQuestion(null);
		btnPickQuestion.setEnabled(false);
		btnTrainOnNext.setEnabled(false);

		progressBar.setVisible(true);
		progressBar.setState(SWT.NORMAL);

		try {
			for (int i = 1; i <= n; i++) {
				soDataManager.loadNext(1, learnerId);
				learner.train(soDataManager);
				learnerRepository.update(learnerId, learner);
				progressBar.setSelection((100 * i) / n);
			}

			setLearnerDetailsText();
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
		} finally {
			btnPickQuestion.setEnabled(true);
			btnTrainOnNext.setEnabled(true);
			progressBar.setVisible(false);
		}
	}

}