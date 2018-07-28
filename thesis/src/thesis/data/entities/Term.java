package thesis.data.entities;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import thesis.util.Constants;

/**
 * Represents a term from the bag-of-words model.
 * 
 * @author Sayan
 *
 */
@Entity
@Table(name = Constants.Tables.Terms)
public class Term extends NamedEntity {
	private int index;
	private List<QuestionTermFrequency> questionTermFrequencies;

	/**
	 * @return the index
	 */
	@Column(name = "TermIndex", unique = true, nullable = false, updatable = false)
	public int getIndex() {
		return index;
	}

	/**
	 * @param index
	 *            the index to set
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * @return the questionTermFrequencies
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "term")
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
}
