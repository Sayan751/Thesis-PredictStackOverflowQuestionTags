package thesis.data.entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.jsoup.Jsoup;

import thesis.util.Constants;

@Entity
@Table(name = Constants.Tables.Questions)
public class Question extends BaseEntity {

	private long seQuestionId;
	private String title;
	private String body;
	private Date seCreationDateTime;

	@Transient
	private Set<String> tagNames;

	@ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinTable(name = Constants.Tables.QuestionTagAssignments, joinColumns = @JoinColumn(name = "QuestionId"), inverseJoinColumns = @JoinColumn(name = "TagId"))
	private Set<Tag> tags;

	private Set<QuestionLearnerDetails> questionLearnerDetails;
	private List<QuestionTermFrequency> questionTermFrequencies;

	public Question() {
		questionLearnerDetails = new HashSet<QuestionLearnerDetails>();
		setTags(new HashSet<Tag>());
		questionTermFrequencies = new ArrayList<QuestionTermFrequency>();
	}

	/**
	 * @return the seQuestionId
	 */
	@Column(name = "SeQuestionId", nullable = false, unique = true, updatable = false)
	public long getSeQuestionId() {
		return seQuestionId;
	}

	/**
	 * @param seQuestionId
	 *            the seQuestionId to set
	 */
	public void setSeQuestionId(long seQuestionId) {
		this.seQuestionId = seQuestionId;
	}

	/**
	 * @return the title
	 */
	@Column(name = "Title", nullable = false, columnDefinition = "NVARCHAR(4000)")
	public String getTitle() {
		return title;
	}

	/**
	 * @param title
	 *            the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the body
	 */
	@Column(name = "Body", nullable = false, columnDefinition = "NVARCHAR(MAX)")
	public String getBody() {
		return body;
	}

	/**
	 * @param body
	 *            the body to set
	 */
	public void setBody(String body) {
		this.body = body;
	}

	/**
	 * @return the seCreationDateTime
	 */
	@Column(name = "SeCreationDateTime", nullable = false, updatable = false, columnDefinition = "datetime")
	public Date getSeCreationDateTime() {
		return seCreationDateTime;
	}

	/**
	 * @param seCreationDateTime
	 *            the seCreationDateTime to set
	 */
	public void setSeCreationDateTime(Date seCreationDateTime) {
		this.seCreationDateTime = seCreationDateTime;
	}

	/**
	 * @return the tagNames
	 */
	@Transient
	public Set<String> getTagNames() {
		return (tags != null && !tags.isEmpty())
				? tags.stream()
						.map(tag -> tag.getName())
						.collect(Collectors.toSet())
				: tagNames;
	}

	/**
	 * @param tagNames
	 *            the tagNames to set
	 */
	public void setTagNames(Set<String> tagNames) {
		this.tagNames = tagNames;
	}

	/**
	 * @return the tags
	 */
	@ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinTable(name = Constants.Tables.QuestionTagAssignments, joinColumns = @JoinColumn(name = "QuestionId"), inverseJoinColumns = @JoinColumn(name = "TagId"))
	public Set<Tag> getTags() {
		return tags;
	}

	/**
	 * @param tags
	 *            the tags to set
	 */
	public void setTags(Set<Tag> tags) {
		this.tags = tags;
	}

	/**
	 * @return the questionLearnerDetails
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "question")
	public Set<QuestionLearnerDetails> getQuestionLearnerDetails() {
		return questionLearnerDetails;
	}

	/**
	 * @param questionLearnerDetails
	 *            the questionLearnerDetails to set
	 */
	public void setQuestionLearnerDetails(Set<QuestionLearnerDetails> questionLearnerDetails) {
		this.questionLearnerDetails = questionLearnerDetails;
	}

	/**
	 * @return the questionTermFrequencies
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "question")
	public List<QuestionTermFrequency> getQuestionTermFrequencies() {
		return questionTermFrequencies;
	}

	/**
	 * @param questionTermFrequencies
	 *            the questionTermFrequencies to set
	 */
	public void setQuestionTermFrequencies(List<QuestionTermFrequency> questionTermFrequencies) {
		this.questionTermFrequencies = questionTermFrequencies;
	}

	@Transient
	public String getTitleAndBodyCombined() {
		return String.format("%1$s %2$s", title, body);
	}

	@Transient
	public String getTitleAndBodyCombinedText() {
		return String.format("%1$s %2$s", title, Jsoup.parse(body)
				.text());
	}
}
