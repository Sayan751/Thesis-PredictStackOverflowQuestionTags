package thesis.data.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import thesis.util.Constants;

@Entity
@Table(name = Constants.Tables.QuestionTermFrequencies)
public class QuestionTermFrequency extends BaseEntity {

	private Question question;
	private Term term;
	private int frequency;

	/**
	 * @return the question
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "QuestionId", nullable = false)
	public Question getQuestion() {
		return question;
	}

	/**
	 * @param question
	 *            the question to set
	 */
	public void setQuestion(Question question) {
		this.question = question;
	}

	/**
	 * @return the term
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "TermId", nullable = false)
	public Term getTerm() {
		return term;
	}

	/**
	 * @param term
	 *            the term to set
	 */
	public void setTerm(Term term) {
		this.term = term;
	}

	/**
	 * @return the frequency
	 */
	@Column(name = "Frequency", nullable = false)
	public int getFrequency() {
		return frequency;
	}

	/**
	 * @param ferquency
	 *            the frequency to set
	 */
	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}
}
