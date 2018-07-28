package thesis.webapi.services;

import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.code.stackexchange.client.query.StackExchangeApiQueryFactory;
import com.google.code.stackexchange.common.PagedList;
import com.google.code.stackexchange.schema.StackExchangeSite;

import thesis.data.entities.Question;
import thesis.data.entities.Tag;
import thesis.data.repositories.QuestionRepository;
import thesis.data.repositories.TagRepository;
import thesis.webapi.adapters.QuestionAdapter;
import thesis.webapi.dto.QuestionDto;

@Path("question")
public class QuestionService {

	private QuestionRepository questionRepository;

	public QuestionService() {
		questionRepository = new QuestionRepository();
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public QuestionDto GetQuestion(@PathParam("id") UUID id) {
		Question q = questionRepository.read(id);
		QuestionDto retVal = QuestionAdapter.toDto(q);
		return retVal;
	}

	@GET
	@Path("/soid/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public QuestionDto GetStackOverflowQuestion(@PathParam("id") long id) {
		Question question = questionRepository.getQuestionBySeId(id);

		if (question == null) {
			question = fetchFromStackOverflow(id);
		}

		return question == null ? null : QuestionAdapter.toDto(question);
	}

	private Question fetchFromStackOverflow(long id) {

		Question question = null;
		StackExchangeApiQueryFactory queryFactory = StackExchangeApiQueryFactory.newInstance("pj9WKZ7g4MJ6XKsKWuxKmA((",
				StackExchangeSite.STACK_OVERFLOW);

		PagedList<com.google.code.stackexchange.schema.Question> seQuestions = queryFactory.newQuestionApiQuery()
				.withFilter("withbody")
				.withQuestionIds(id)
				.list();

		if (!seQuestions.isEmpty()) {
			question = persistQuestionData(seQuestions.get(0));
		}
		return question;
	}

	private Question persistQuestionData(final com.google.code.stackexchange.schema.Question seQuestion) {
		Question question;
		question = thesis.util.adapters.QuestionAdapter.convertStackOverflowQuestion(seQuestion);

		TagRepository labelRepository = new TagRepository();
		HashMap<String, Tag> tagMap = labelRepository.fetchOrStoreByName(seQuestion.getTags());

		// add tags to the question
		question.getTags()
				.addAll(question.getTagNames()
						.stream()
						.map(tagName -> tagMap.get(tagName))
						.collect(Collectors.toSet()));

		// add the new questions to data store
		questionRepository.create(question);
		return question;
	}

}
