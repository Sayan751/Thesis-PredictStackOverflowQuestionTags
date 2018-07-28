package thesis._playgrounds;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.code.stackexchange.client.query.StackExchangeApiQueryFactory;
import com.google.code.stackexchange.schema.StackExchangeSite;

import Data.Instance;
import Learner.AbstractLearner;
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

public class SODataManagerPlayground {

	@SuppressWarnings("unused")
	private static Logger logger;
	static {
		System.setProperty("initiator", SODataManagerPlayground.class.getSimpleName());
		System.setProperty("current.date", (new SimpleDateFormat("yyyy-MM-dd HHmmss")).format(new Date()));
		System.setProperty("log4j.configurationFile", "resources/log4j.xml");
		logger = LogManager.getLogger(SODataManagerPlayground.class);
	}
	@SuppressWarnings("unused")
	private LearnerRepository learnerRepository;
	private ILabelRepository<Tag> labelRepository;
	private IQuestionRepository questionRepository;
	private IQuestionTermFrequencyRepository questionTermFrequencyRepository;
	private ITermRepository termRepository;
	private IQuestionLearnerDetailsRepository questionLearnerDetailsRepository;

	private StackExchangeApiQueryFactory queryFactory;
	private StackOverflowQuestionManager soDataManager;

	Map<Instance, Map<AbstractLearner, Double>> questionLearnerDetails = new HashMap<Instance, Map<AbstractLearner, Double>>();

	public SODataManagerPlayground() {

		initRepositories();
		initSODataManage();
	}

	public static void main(String[] args) {

		SODataManagerPlayground playground = new SODataManagerPlayground();
		try {
			while (playground.questionRepository.getTotalCount() <= 1000000) {
				if (playground.soDataManager.fetchAndStoreNextChunkOfQuestions(1)
						.size() == 0)
					break;
			}
			// playground.soDataManager.loadNext(1000000, learner.getId());

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Question count: " + playground.questionRepository.getTotalCount());
		HibernateManager.shutdown();

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
}
