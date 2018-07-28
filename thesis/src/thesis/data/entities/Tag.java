package thesis.data.entities;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import thesis.util.Constants;

@Entity
@Table(name = Constants.Tables.Tags)
public class Tag extends NamedEntity {
	private int index;
	private Set<Question> questions;

	public Tag() {
		setQuestions(new HashSet<Question>());
	}

	/**
	 * @return the questions
	 */
	@ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "tags")
	public Set<Question> getQuestions() {
		return questions;
	}

	/**
	 * @param questions
	 *            the questions to set
	 */
	public void setQuestions(Set<Question> questions) {
		this.questions = questions;
	}

	/**
	 * @return the index
	 */
	@Column(name = "TagIndex", unique = true, nullable = false, updatable = false)
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
}
