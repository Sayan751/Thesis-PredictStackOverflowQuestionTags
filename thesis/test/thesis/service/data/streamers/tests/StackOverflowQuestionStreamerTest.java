package thesis.service.data.streamers.tests;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.code.stackexchange.client.query.QuestionApiQuery;
import com.google.code.stackexchange.client.query.StackExchangeApiQueryFactory;
import com.google.code.stackexchange.common.PagedArrayList;
import com.google.code.stackexchange.common.PagedList;

import Data.Instance;
import de.svenjacobs.loremipsum.LoremIpsum;
import thesis.data.entities.Learner;
import thesis.data.entities.Question;
import thesis.data.entities.QuestionLearnerDetails;
import thesis.data.entities.QuestionTermFrequency;
import thesis.data.entities.Tag;
import thesis.data.entities.Term;
import thesis.data.managers.StackOverflowQuestionManager;
import thesis.data.repositories.QuestionTermFrequencyRepository;
import thesis.data.repositoryinterfaces.ILabelRepository;
import thesis.data.repositoryinterfaces.IQuestionLearnerDetailsRepository;
import thesis.data.repositoryinterfaces.IQuestionRepository;
import thesis.data.repositoryinterfaces.ITagRepository;
import thesis.data.repositoryinterfaces.ITermRepository;

/**
 * @author Sayan
 *
 */
public class StackOverflowQuestionStreamerTest {

	StackOverflowQuestionManager target;
	ILabelRepository<Tag> labelRepository;
	IQuestionRepository questionRepository;
	QuestionTermFrequencyRepository questionTermFrequencyRepository;
	ITermRepository termRepository;
	IQuestionLearnerDetailsRepository questionLearnerDetailsRepository;
	StackExchangeApiQueryFactory queryFactory;

	Tag java, csharp, javascript;
	List<Tag> allTags;

	Question javaQuestion, csharpQuestion, javascriptQuestion;
	List<Question> allQuestions;

	Learner learner;
	List<Learner> allLearners;

	List<Term> allTerms;
	List<QuestionTermFrequency> allQuestionTermFrequencies = new ArrayList<QuestionTermFrequency>();

	@Before
	public void arrange() {
		learner = new Learner();
		learner.setId(UUID.randomUUID());
		allLearners = new ArrayList<Learner>();
		allLearners.add(learner);

		mockLabelRepository();
		mockQuestionRepository();
		mockQuestionTermFrequencyRepository();
		mockTermRepository();
		mockQuestionLearnerDetailsRepository();
		mockStackExchangeApiQueryFactory();
	}

	private void mockQuestionLearnerDetailsRepository() {
		questionLearnerDetailsRepository = mock(IQuestionLearnerDetailsRepository.class);
		when(questionLearnerDetailsRepository.create(ArgumentMatchers.anyList()))
				.thenAnswer(
						new Answer<List<QuestionLearnerDetails>>() {

							@SuppressWarnings("unchecked")
							@Override
							public List<QuestionLearnerDetails> answer(InvocationOnMock invocation) throws Throwable {
								Object[] args = invocation.getArguments();
								List<QuestionLearnerDetails> entities = (List<QuestionLearnerDetails>) args[0];
								entities.stream()
										.forEach(e -> questionLearnerDetailsRepository.create(e));
								return entities;
							}
						});
		when(questionLearnerDetailsRepository.create(ArgumentMatchers.any(QuestionLearnerDetails.class)))
				.thenAnswer(new Answer<QuestionLearnerDetails>() {

					@Override
					public QuestionLearnerDetails answer(InvocationOnMock invocation) throws Throwable {

						QuestionLearnerDetails entity = invocation.getArgument(0);

						if (entity.getQuestion() == null)
							entity.setQuestion(allQuestions.stream()
									.filter(q -> q.getId()
											.equals(entity.getQuestionId()))
									.findFirst()
									.get());

						entity.getQuestion()
								.getQuestionLearnerDetails()
								.add(entity);
						if (entity.getLearner() == null)
							entity.setLearner(allLearners
									.stream()
									.filter(l -> l.getId()
											.equals(entity.getLearnerId()))
									.findFirst()
									.get());
						entity.getLearner()
								.getQuestionLearnerDetails()
								.add(entity);
						return entity;
					}
				});
	}

	private void mockStackExchangeApiQueryFactory() {
		queryFactory = mock(StackExchangeApiQueryFactory.class);
		QuestionApiQuery questionApiQuery = mock(QuestionApiQuery.class);
		when(questionApiQuery.withPaging(ArgumentMatchers.any())).thenReturn(questionApiQuery);
		when(questionApiQuery.withFilter(ArgumentMatchers.any())).thenReturn(questionApiQuery);
		when(questionApiQuery
				.withSort(ArgumentMatchers.any(com.google.code.stackexchange.schema.Question.SortOrder.class)))
						.thenReturn(questionApiQuery);
		when(questionApiQuery.list())
				.thenAnswer(new Answer<PagedList<com.google.code.stackexchange.schema.Question>>() {

					private LoremIpsum loremIpsum = new LoremIpsum();

					@Override
					public PagedList<com.google.code.stackexchange.schema.Question> answer(InvocationOnMock invocation)
							throws Throwable {

						return IntStream.rangeClosed(1, 100)
								.boxed()
								// .parallel()
								.map(indx -> {
									com.google.code.stackexchange.schema.Question seQuestion = new com.google.code.stackexchange.schema.Question();
									seQuestion.setTitle(loremIpsum.getWords());
									seQuestion.setBody(
											String.format("<html><p> %s </p></html>", loremIpsum.getParagraphs()));
									seQuestion.setCreationDate(new Date(System.currentTimeMillis()));
									seQuestion.setTags(Arrays.asList(loremIpsum.getWords(5)
											.split(" ")));
									seQuestion.setQuestionId(System.currentTimeMillis());
									return seQuestion;
								})
								.sorted((q1, q2) -> q1.getCreationDate()
										.compareTo(q2.getCreationDate()))
								.collect(Collectors.toCollection(
										PagedArrayList<com.google.code.stackexchange.schema.Question>::new));
					}
				});
		when(queryFactory.newQuestionApiQuery()).thenReturn(questionApiQuery);
	}

	private void mockTermRepository() {
		allTerms = new ArrayList<Term>();

		termRepository = mock(ITermRepository.class);
		when(termRepository.all()).thenReturn(allTerms);
		when(termRepository.getTotalCount()).thenAnswer(new Answer<Long>() {
			@Override
			public Long answer(InvocationOnMock invocation) throws Throwable {
				return (long) allTerms.size();
			}
		});
		when(termRepository.fetchOrStoreByName(ArgumentMatchers.anyList()))
				.thenAnswer(new Answer<HashMap<String, Term>>() {

					@Override
					public HashMap<String, Term> answer(InvocationOnMock invocation) throws Throwable {
						Object[] args = invocation.getArguments();
						@SuppressWarnings("unchecked")
						List<String> names = (List<String>) args[0];
						// Stream<String> namesStream = names.stream()
						// .distinct();

						names.stream()
								.distinct()
								.forEach(name -> {
									if (allTerms.stream()
											.noneMatch(term -> term.getName()
													.equals(name))) {
										Term term = new Term();
										term.setId(UUID.randomUUID());
										term.setName(name);
										term.setIndex(allTerms.size());
										allTerms.add(term);
									}
								});

						return names.stream()
								.distinct()
								.collect(HashMap<String, Term>::new,
										(m, name) -> m.put(name, allTerms.stream()
												.filter(tag -> tag.getName()
														.equals(name))
												.findFirst()
												.get()),
										(m, u) -> {
										});
					}

				});
	}

	@SuppressWarnings("unchecked")
	private void mockQuestionTermFrequencyRepository() {
		questionTermFrequencyRepository = mock(QuestionTermFrequencyRepository.class);
		when(questionTermFrequencyRepository.all()).thenReturn(allQuestionTermFrequencies);
		when(questionTermFrequencyRepository.create(ArgumentMatchers.anyList()))
				.thenAnswer(new Answer<List<QuestionTermFrequency>>() {

					@Override
					public List<QuestionTermFrequency> answer(InvocationOnMock invocation) throws Throwable {
						Object[] args = invocation.getArguments();
						List<QuestionTermFrequency> entities = (List<QuestionTermFrequency>) args[0];
						allQuestionTermFrequencies.addAll(entities);
						entities.stream()
								.forEach(e -> {
									e.getQuestion()
											.getQuestionTermFrequencies()
											.add(e);
								});
						return entities;
					}
				});
		when(questionTermFrequencyRepository.createQuestionTermFrequencies(ArgumentMatchers.any(HashMap.class),
				ArgumentMatchers.any(Question.class), ArgumentMatchers.any(HashMap.class))).thenCallRealMethod();
	}

	private void mockQuestionRepository() {
		javaQuestion = new Question();
		javaQuestion.setId(UUID.randomUUID());
		javaQuestion.setTitle("A java question title");
		javaQuestion.setBody("A java question body");
		javaQuestion.setCreatedOn(Instant.now());
		javaQuestion.getTags()
				.addAll(new HashSet<Tag>(Arrays.asList(java)));

		csharpQuestion = new Question();
		csharpQuestion.setId(UUID.randomUUID());
		csharpQuestion.setTitle("A chsarp question title");
		csharpQuestion.setBody("A chsarp question body");
		csharpQuestion.setCreatedOn(Instant.now());
		csharpQuestion.getTags()
				.addAll(new HashSet<Tag>(Arrays.asList(csharp)));

		javascriptQuestion = new Question();
		javascriptQuestion.setId(UUID.randomUUID());
		javascriptQuestion.setTitle("A chsarp javascript title");
		javascriptQuestion.setBody("A chsarp javascript body");
		javascriptQuestion.setCreatedOn(Instant.now());
		javascriptQuestion.getTags()
				.addAll(new HashSet<Tag>(Arrays.asList(javascript)));

		allQuestions = new ArrayList<Question>(Arrays.asList(javaQuestion, csharpQuestion, javascriptQuestion));

		questionRepository = mock(IQuestionRepository.class);
		when(questionRepository.all()).thenReturn(allQuestions);
		when(questionRepository.getTotalCount()).thenReturn((long) allQuestions.size());
		when(questionRepository.read(ArgumentMatchers.any(UUID.class))).thenAnswer(new Answer<Question>() {

			@Override
			public Question answer(InvocationOnMock invocation) throws Throwable {
				UUID id = invocation.getArgument(0);
				return allQuestions.stream()
						.filter(q -> q.getId()
								.equals(id))
						.findFirst()
						.get();
			}

		});
		when(questionRepository.getAllUnprocessedQuestion(ArgumentMatchers.eq(learner.getId()),
				ArgumentMatchers.anyInt()))
						.thenAnswer(new Answer<List<Question>>() {

							@Override
							public List<Question> answer(InvocationOnMock invocation) throws Throwable {
								Object[] args = invocation.getArguments();

								int count = Integer.parseInt(args[1].toString());

								HashSet<Question> questionSet = new HashSet<Question>(allQuestions);
								questionSet.removeAll(learner.getQuestionLearnerDetails()
										.stream()
										.map(d -> d.getQuestion())
										.collect(Collectors.toSet()));

								return questionSet.stream()
										.sorted((f1, f2) -> f1.getCreatedOn()
												.compareTo(f2.getCreatedOn()))
										.limit((long) count)
										.collect(Collectors.toList());
							}

						});
		when(questionRepository.create(ArgumentMatchers.anyList())).then(new Answer<List<Question>>() {

			@Override
			public List<Question> answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				@SuppressWarnings("unchecked")
				List<Question> questions = (List<Question>) args[0];
				allQuestions.addAll(questions);
				return questions;
			}
		});

		// when(questionRepository.markQuestionAsProcessedForLearner(ArgumentMatchers.eq(learner),
		// ArgumentMatchers.any(UUID.class))).thenAnswer(new Answer<Boolean>() {
		//
		// @Override
		// public Boolean answer(InvocationOnMock invocation) throws Throwable {
		// Object[] args = invocation.getArguments();
		// UUID questionId = (UUID) args[1];
		// Question question = allQuestions.parallelStream()
		// .filter(q -> q.getId()
		// .equals(questionId))
		// .findFirst()
		// .get();
		// learner.getQuestionLearnerDetails()
		// .add(question);
		// question.getLearners()
		// .add(learner);
		// return true;
		// }
		//
		// });
	}

	private void mockLabelRepository() {
		java = new Tag();
		java.setId(UUID.randomUUID());
		java.setName("java");
		java.setIndex(0);

		csharp = new Tag();
		csharp.setId(UUID.randomUUID());
		csharp.setName("csharp");
		csharp.setIndex(1);

		javascript = new Tag();
		javascript.setId(UUID.randomUUID());
		javascript.setName("javascript");
		javascript.setIndex(2);

		allTags = new ArrayList<Tag>(Arrays.asList(java, csharp, javascript));
		labelRepository = mock(ITagRepository.class);
		when(labelRepository.all()).thenReturn(allTags);
		when(labelRepository.fetchOrStoreByName(ArgumentMatchers.anyList()))
				.thenAnswer(new Answer<HashMap<String, Tag>>() {

					@Override
					public HashMap<String, Tag> answer(InvocationOnMock invocation) throws Throwable {
						Object[] args = invocation.getArguments();
						@SuppressWarnings("unchecked")
						List<String> names = (List<String>) args[0];

						names.stream()
								.distinct()
								.forEach(name -> {
									if (allTags.stream()
											.noneMatch(tag -> tag.getName()
													.equals(name))) {
										Tag tag = new Tag();
										tag.setId(UUID.randomUUID());
										tag.setName(name);
										tag.setIndex(allTags.size());
										allTags.add(tag);
									}
								});

						return names.stream()
								.distinct()
								.collect(HashMap<String, Tag>::new,
										(m, name) -> m.put(name, allTags.stream()
												.filter(tag -> tag.getName()
														.equals(name))
												.findFirst()
												.get()),
										(m, u) -> {
										});
					}
				});
		when(labelRepository.getTotalNumberOfLabels()).thenAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				return allTags.size();
			}
		});
	}

	@Test(expected = IllegalArgumentException.class)
	public void ctorTest_InvalidLabelRepositoryShouldThrowException() {
		target = new StackOverflowQuestionManager(null, questionRepository, questionTermFrequencyRepository,
				termRepository, queryFactory, questionLearnerDetailsRepository);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ctorTest_InvalidQuestionRepositoryShouldThrowException() {
		target = new StackOverflowQuestionManager(labelRepository, null, questionTermFrequencyRepository,
				termRepository, queryFactory, questionLearnerDetailsRepository);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ctorTest_InvalidQuestionTermFrequencyRepositoryShouldThrowException() {
		target = new StackOverflowQuestionManager(labelRepository, questionRepository, null,
				termRepository, queryFactory, questionLearnerDetailsRepository);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ctorTest_InvalidTermRepositoryShouldThrowException() {
		target = new StackOverflowQuestionManager(labelRepository, questionRepository, questionTermFrequencyRepository,
				null, queryFactory, questionLearnerDetailsRepository);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ctorTest_InvalidQueryFactoryShouldThrowException() {
		target = new StackOverflowQuestionManager(labelRepository, questionRepository, questionTermFrequencyRepository,
				termRepository, null, questionLearnerDetailsRepository);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ctorTest_InvalidQuestionLearnerDetailsRepositoryShouldThrowException() {
		target = new StackOverflowQuestionManager(labelRepository, questionRepository, questionTermFrequencyRepository,
				termRepository, queryFactory, null);
	}

	@Test
	public void nextTest_ShouldReturnAVTableWithOneInstance() {
		// arrange
		target = new StackOverflowQuestionManager(labelRepository, questionRepository, questionTermFrequencyRepository,
				termRepository, queryFactory, questionLearnerDetailsRepository);
		Instance instance = null;
		target.loadNext(learner.getId());
		boolean hasNextBefore = false, hasNextAfter = true;

		// act
		try {
			hasNextBefore = target.hasNext();
			instance = target.getNextInstance();
			hasNextAfter = target.hasNext();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println(target.getNumberOfFeatures());
		// assert
		assert (instance != null);
		assertEquals(true, hasNextBefore);
		assertEquals(false, hasNextAfter);
		verify(questionRepository, times(1)).getAllUnprocessedQuestion(ArgumentMatchers.eq(learner.getId()),
				ArgumentMatchers.eq(1));
		verify(queryFactory, times(0)).newQuestionApiQuery();
	}

	@Test
	public void nextTest_next5ShouldReturn5Instances() {
		// arrange
		target = new StackOverflowQuestionManager(labelRepository, questionRepository, questionTermFrequencyRepository,
				termRepository, queryFactory, questionLearnerDetailsRepository);
		int n = 5;
		target.loadNext(n, learner.getId());
		int actual = 0;
		Instance instance = null;

		// act
		try {
			while (target.hasNext()) {
				instance = target.getNextInstance();
				actual++;
				assert (instance != null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// assert
		assertEquals(n, actual);
		assertEquals(103, allQuestions.size());
		assertEquals(allTags.size(), target.getNumberOfLabels());
		assertEquals(allTerms.size(), target.getNumberOfFeatures());
		verify(questionRepository, times(1)).getAllUnprocessedQuestion(ArgumentMatchers.eq(learner.getId()),
				ArgumentMatchers.eq(n));
		verify(queryFactory, times(1)).newQuestionApiQuery();

		/*
		 * Note: This assertion will not hold in practice. However, for testing,
		 * as we know that the actual random text has no html element, this
		 * assertion just ensures that prior to extracting the feature, the
		 * question body is cleaned of any html element. In real life, the
		 * question text may contain strings such as html, p, br etc.
		 *
		 */
		assert (allTerms.stream()
				.noneMatch(t -> t.getName()
						.equals("html") || t.getName()
								.equals("p")));
	}

	@Test
	public void nextTest_next100ShouldReturn100Instances() {
		// arrange
		target = new StackOverflowQuestionManager(labelRepository, questionRepository, questionTermFrequencyRepository,
				termRepository, queryFactory, questionLearnerDetailsRepository);
		int n = 100;
		target.loadNext(n, learner.getId());
		int actual = 0;
		Instance instance = null;

		// act
		try {
			while (target.hasNext()) {
				instance = target.getNextInstance();
				actual++;
				assert (instance != null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// assert

		assertEquals(103, allQuestions.size());
		assertEquals(n, actual);
		assertEquals(allTags.size(), target.getNumberOfLabels());
		assertEquals(allTerms.size(), target.getNumberOfFeatures());
		verify(questionRepository, times(1)).getAllUnprocessedQuestion(ArgumentMatchers.eq(learner.getId()),
				ArgumentMatchers.eq(n));
		verify(queryFactory, times(1)).newQuestionApiQuery();
		/*
		 * Note: This assertion will not hold in practice. However, for testing,
		 * as we know that the actual random text has no html element, this
		 * assertion just ensures that prior to extracting the feature, the
		 * question body is cleaned of any html element. In real life, the
		 * question text may contain strings such as html, p, br etc.
		 *
		 */
		assert (allTerms.stream()
				.noneMatch(t -> t.getName()
						.equals("html") || t.getName()
								.equals("p")));
	}

	@Test
	public void nextTest_next110ShouldReturn110Instances() {
		// arrange
		target = new StackOverflowQuestionManager(labelRepository, questionRepository, questionTermFrequencyRepository,
				termRepository, queryFactory, questionLearnerDetailsRepository);

		int n = 110;
		target.loadNext(n, learner.getId());
		int actual = 0;
		Instance instance = null;

		// act
		try {
			while (target.hasNext()) {
				instance = target.getNextInstance();
				actual++;
				assert (instance != null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// assert

		assertEquals(203, allQuestions.size());
		assertEquals(n, actual);
		assertEquals(allTags.size(), target.getNumberOfLabels());
		assertEquals(allTerms.size(), target.getNumberOfFeatures());
		verify(questionRepository, times(1)).getAllUnprocessedQuestion(ArgumentMatchers.eq(learner.getId()),
				ArgumentMatchers.eq(n));
		verify(queryFactory, times(Math.toIntExact((long) Math.ceil(n / 100.0)))).newQuestionApiQuery();
		/*
		 * Note: This assertion will not hold in practice. However, for testing,
		 * as we know that the actual random text has no html element, this
		 * assertion just ensures that prior to extracting the feature, the
		 * question body is cleaned of any html element. In real life, the
		 * question text may contain strings such as html, p, br etc.
		 *
		 */
		assert (allTerms.stream()
				.noneMatch(t -> t.getName()
						.equals("html") || t.getName()
								.equals("p")));
	}

	@Test
	public void markProcessedTest_NotMarkingProcessedShouldReturnSameInstanceAsBefore() {
		// arrange
		target = new StackOverflowQuestionManager(labelRepository, questionRepository,
				questionTermFrequencyRepository,
				termRepository, queryFactory, questionLearnerDetailsRepository);

		try {

			// act
			target.loadNext(learner.getId());
			final Instance instance1 = target.getNextInstance();
			target.loadNext(learner.getId());
			final Instance instance2 = target.getNextInstance();

			// assert
			assert (instance1 != null);
			assert (instance2 != null);
			assert (Arrays.stream(instance1.x)
					.allMatch(pair1 -> Arrays.stream(instance2.x)
							.anyMatch(pair2 -> pair1.index == pair2.index && pair1.value == pair2.value)));

			assert (Arrays.stream(instance1.y)
					.allMatch(i1 -> Arrays.stream(instance2.y)
							.anyMatch(i2 -> i1 == i2)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void markProcessedTest_MarkingProcessedShouldNotReturnSameInstanceAsBefore() {
		// arrange
		target = new StackOverflowQuestionManager(labelRepository, questionRepository,
				questionTermFrequencyRepository,
				termRepository, queryFactory, questionLearnerDetailsRepository);
		try {

			// act
			target.loadNext(learner.getId());
			final Instance instance1 = target.getNextInstance();
			boolean processed = target.markProcessed(instance1, learner.getId(), 0, 0, false, false);
			target.loadNext(learner.getId());
			final Instance instance2 = target.getNextInstance();

			// assert
			assert (instance1 != null);
			assert (instance2 != null);
			assert (processed);
			assert (!Arrays.stream(instance1.x)
					.allMatch(pair1 -> Arrays.stream(instance2.x)
							.anyMatch(pair2 -> pair1.index == pair2.index && pair1.value == pair2.value)));

			assert (!Arrays.stream(instance1.y)
					.allMatch(i1 -> Arrays.stream(instance2.y)
							.anyMatch(i2 -> i1 == i2)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void resetTest_resetingCausesReturnOfSameInstance() {
		// arrange
		target = new StackOverflowQuestionManager(labelRepository, questionRepository,
				questionTermFrequencyRepository,
				termRepository, queryFactory, questionLearnerDetailsRepository);
		target.loadNext(learner.getId());

		try {
			// act
			final Instance instance1 = target.getNextInstance();
			target.reset();
			final Instance instance2 = target.getNextInstance();

			// assert
			assert (instance1 != null);
			assert (instance2 != null);
			assert (instance1.equals(instance2));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
