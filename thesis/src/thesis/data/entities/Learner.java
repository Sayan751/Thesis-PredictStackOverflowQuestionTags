package thesis.data.entities;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import thesis.util.Constants;

/**
 * Represents a learner.
 * 
 * @author Sayan
 *
 */
@Entity
@Table(name = Constants.Tables.Learners)
public class Learner extends BaseEntity {

	private String learnerDetails;
	private UUID parentId;
	private Learner parent;
	private boolean isActive;

	private Set<Learner> children;
	private Set<QuestionLearnerDetails> questionLearnerDetails;

	public Learner() {
		this((Learner) null, null);
	}

	public Learner(String learnerDetails) {
		this((Learner) null, learnerDetails);
	}

	public Learner(Learner parent, String learnerDetails) {
		setParent(parent);
		initialize(learnerDetails);
	}

	public Learner(UUID parentId, String learnerDetails) {
		setParentId(parentId);
		initialize(learnerDetails);
	}

	public Learner(UUID id) {
		this(id, null);
	}

	private void initialize(String learnerDetails) {
		setQuestionLearnerDetails(new HashSet<QuestionLearnerDetails>());
		setLearnerDetails(learnerDetails);
		setActive(true);
		setChildren(new HashSet<Learner>());
	}

	/**
	 * @return the questionLearnerDetails
	 */
	@OneToMany(mappedBy = "learner", fetch = FetchType.LAZY)
	public Set<QuestionLearnerDetails> getQuestionLearnerDetails() {
		return questionLearnerDetails;
	}

	/**
	 * @param questionLearnerDetails
	 *            the questionLearnerDetails to set
	 */
	protected void setQuestionLearnerDetails(Set<QuestionLearnerDetails> questionLearnerDetails) {
		this.questionLearnerDetails = questionLearnerDetails;
	}

	/**
	 * @return the serialized learner details in JSON. As each learner can have
	 *         different types properties, it is better to have a serialized
	 *         JSON string holding the state of the learner.
	 */
	@Column(name = "LearnerDetails", columnDefinition = "VARCHAR(MAX)")
	public String getLearnerDetails() {
		return learnerDetails;
	}

	/**
	 * @param learnerDetails
	 *            the serialized learner details in JSON to set.As each learner
	 *            can have different types properties, it is better to have a
	 *            serialized JSON string holding the state of the learner.
	 */
	public void setLearnerDetails(String learnerDetails) {
		if (learnerDetails != null)
			this.learnerDetails = learnerDetails;
	}

	/**
	 * @return the parent
	 */
	@JoinColumn(name = "ParentId", nullable = true, insertable = false, updatable = false, columnDefinition = "uniqueidentifier")
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	public Learner getParent() {
		return parent;
	}

	/**
	 * @param parent
	 *            the parent to set
	 */
	protected void setParent(Learner parent) {
		this.parent = parent;
		if (parent != null)
			setParentId(parent.getId());
	}

	/**
	 * @return the isActive
	 */
	@Column(name = "IsActive")
	public boolean isActive() {
		return isActive;
	}

	/**
	 * @param isActive
	 *            the isActive to set
	 */
	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	/**
	 * @return the children
	 */
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "parent")
	public Set<Learner> getChildren() {
		return children;
	}

	/**
	 * @param children
	 *            the children to set
	 */
	protected void setChildren(Set<Learner> children) {
		this.children = children;
	}

	/**
	 * @return the parentId
	 */
	@Column(name = "ParentId", updatable = false, nullable = true)//, columnDefinition = "uniqueidentifier"
	public UUID getParentId() {
		return parentId;
	}

	/**
	 * @param parentId
	 *            the parentId to set
	 */
	public void setParentId(UUID parentId) {
		this.parentId = parentId;
	}
}