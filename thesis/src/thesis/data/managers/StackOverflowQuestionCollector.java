package thesis.data.managers;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.code.stackexchange.client.query.StackExchangeApiQueryFactory;
import com.google.code.stackexchange.common.PagedList;
import com.google.code.stackexchange.schema.Paging;
import com.google.code.stackexchange.schema.StackExchangeSite;
import com.google.code.stackexchange.schema.TimePeriod;

import thesis.data.entities.Question;
import thesis.data.entities.Tag;
import thesis.data.repositories.QuestionRepository;
import thesis.data.repositories.TagRepository;
import thesis.data.repositoryinterfaces.ILabelRepository;
import thesis.data.repositoryinterfaces.IQuestionRepository;
import thesis.util.HibernateManager;
import thesis.util.adapters.QuestionAdapter;

public class StackOverflowQuestionCollector /*extends DataManager implements IFmeasureObserver*/ {
	private static Logger logger = LogManager.getLogger(StackOverflowQuestionCollector.class);
	private static final int pageSize = 100;

	private ILabelRepository<Tag> labelRepository;
	private IQuestionRepository questionRepository;
	private StackExchangeApiQueryFactory queryFactory;

	private final Date hardEndDate;

	int maxFeatureIndex, maxLabelIndex;
	private Calendar calendar;
	private PagedList<com.google.code.stackexchange.schema.Question> seQuestions;
	private List<Question> questions;

	public StackOverflowQuestionCollector(
			ILabelRepository<Tag> labelRepository,
			IQuestionRepository questionRepository,
			StackExchangeApiQueryFactory queryFactory) {

		if (labelRepository == null || questionRepository == null || queryFactory == null)
			throw new IllegalArgumentException();

		this.labelRepository = labelRepository;
		this.questionRepository = questionRepository;
		this.queryFactory = queryFactory;

		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.set(2017, 0, 1, 0, 0, 0);
		hardEndDate = calendar.getTime();
	}

	public void run() {

		calendar.clear();

		calendar.setTime(questionRepository.getMaxSeCreationDateTime());
		calendar.add(Calendar.SECOND, 1);
		Date startDate = calendar.getTime();

		calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
				calendar.get(Calendar.DATE), 23, 59, 59);
		Date endDate = calendar.getTime();

		int pageNumber = 1;
		boolean hasMore = false, rangeChanged = false;

		while (hardEndDate.after(startDate)) {

			rangeChanged = false;

			logger.info("Trying to fetch page " + pageNumber + " in the range (" + startDate + " - " + endDate + ")");
			hasMore = fetchAndStoreNextChunkOfQuestions(pageNumber, startDate, endDate);

			if (hasMore) {
				pageNumber += 10; // fetch every 10th page.
			} else {
				startDate = endDate;
				calendar.clear();
				calendar.setTime(endDate);
				calendar.add(Calendar.DATE, 1);
				Date tempEndDate = calendar.getTime();
				endDate = tempEndDate.after(hardEndDate) ? hardEndDate : tempEndDate;
				pageNumber = 1;
				rangeChanged = true;
			}

			// end of stream
			if (!rangeChanged && !hasMore)
				break;
		}
	}

	public boolean fetchAndStoreNextChunkOfQuestions(int pageNumber, Date startDate, Date endDate) {

		fetchStackOverflowQuestions(pageNumber, startDate, endDate);

		if (!seQuestions.isEmpty()) {
			questions = QuestionAdapter.convertStackOverflowQuestion(seQuestions);

			// convert/map the tags to data store tags.
			List<String> tags = seQuestions.stream()
					.flatMap(q -> q.getTags()
							.stream())
					.distinct()
					.collect(Collectors.toList());
			HashMap<String, Tag> tagMap = labelRepository.fetchOrStoreByName(tags);

			// add tags to the question
			questions.stream()
					.forEach(q -> q.getTags()
							.addAll(q.getTagNames()
									.stream()
									.map(tagName -> tagMap.get(tagName))
									.collect(Collectors.toSet())));

			// add the new questions to data store
			questionRepository.create(questions);
		}

		boolean hasMore = seQuestions.hasMore();
		seQuestions.clear();
		if (questions != null)
			questions.clear();
		return hasMore;
	}

	private void fetchStackOverflowQuestions(int pageNumber, Date startDate, Date endDate) {

		seQuestions = queryFactory.newQuestionApiQuery()
				.withPaging(new Paging(pageNumber, pageSize))
				.withFilter("withbody")
				.withTimePeriod(new TimePeriod(startDate, endDate))
				.withSort(com.google.code.stackexchange.schema.Question.SortOrder.LEAST_RECENTLY_CREATED)
				.list();

		try {
			int backOff = seQuestions.getBackoff();
			if (backOff > 0) {
				logger.info("backoff for " + backOff + " seconds.");
				Thread.sleep(backOff * 1000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		try {
			StackOverflowQuestionCollector collector = new StackOverflowQuestionCollector(new TagRepository(),
					new QuestionRepository(), StackExchangeApiQueryFactory.newInstance("pj9WKZ7g4MJ6XKsKWuxKmA((",
							StackExchangeSite.STACK_OVERFLOW));
			collector.run();
		} catch (Exception e) {
			logger.error("Exception occurred.", e);
		} finally {
			HibernateManager.shutdown();
		}
	}
}