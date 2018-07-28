package thesis.data.entities;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.ColumnDefault;

import thesis.util.Constants;

@Entity
@Table(name = Constants.Tables.QuestionLearnerDetails)
public class QuestionLearnerDetails extends BaseEntity {
	private UUID questionId;
	private Question question;
	private UUID learnerId;
	private Learner learner;
	private double fMeasure;
	private boolean test = false;
	private boolean topk = false;
	private boolean prequential = false;

	public QuestionLearnerDetails() {
	}

	public QuestionLearnerDetails(UUID learnerId, UUID questionId, double fMeasure, boolean isPrequential,
			boolean isTopk, boolean isTest) {
		setLearnerId(learnerId);
		setQuestionId(questionId);
		setfMeasure(fMeasure);
		setPrequential(isPrequential);
		setTest(isTest);
		setTopk(isTopk);
	}

	/**
	 * @return the question
	 */
	@ManyToOne(fetch = FetchType.LAZY, targetEntity = Question.class)
	@JoinColumn(name = "QuestionId", insertable = false, updatable = false)
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
	 * @return the learner
	 */
	@ManyToOne(fetch = FetchType.LAZY, targetEntity = Learner.class)
	@JoinColumn(name = "LearnerId", insertable = false, updatable = false)
	public Learner getLearner() {
		return learner;
	}

	/**
	 * @param learner
	 *            the learner to set
	 */
	public void setLearner(Learner learner) {
		this.learner = learner;
	}

	/**
	 * @return the fMeasure
	 */
	@Column(name = "Fmeasure")
	public double getfMeasure() {
		return fMeasure;
	}

	/**
	 * @param fMeasure
	 *            the fMeasure to set
	 */
	public void setfMeasure(double fMeasure) {
		this.fMeasure = fMeasure;
	}

	/**
	 * @return the learnerId
	 */
	@Column(name = "LearnerId", nullable = false, updatable = false) //, columnDefinition = "uniqueidentifier"
	public UUID getLearnerId() {
		return learnerId;
	}

	/**
	 * @param learnerId
	 *            the learnerId to set
	 */
	public void setLearnerId(UUID learnerId) {
		this.learnerId = learnerId;
	}

	/**
	 * @return the questionId
	 */
	@Column(name = "QuestionId", nullable = false, updatable = false) //, columnDefinition = "uniqueidentifier"
	public UUID getQuestionId() {
		return questionId;
	}

	/**
	 * @param questionId
	 *            the questionId to set
	 */
	public void setQuestionId(UUID questionId) {
		this.questionId = questionId;
	}

	/**
	 * @return the isTest
	 */
	@Column(name = "IsTest", nullable = false, updatable = false)
	@ColumnDefault("0")
	public boolean isTest() {
		return test;
	}

	/**
	 * @param isTest
	 *            the isTest to set
	 */
	public void setTest(boolean isTest) {
		this.test = isTest;
	}

	/**
	 * @return the isTopk
	 */
	@Column(name = "IsTopk", nullable = false, updatable = false)
	@ColumnDefault("0")
	public boolean isTopk() {
		return topk;
	}

	/**
	 * @param isTopk
	 *            the isTopk to set
	 */
	public void setTopk(boolean isTopk) {
		this.topk = isTopk;
	}

	/**
	 * @return the prequential
	 */
	@Column(name = "IsPrequential", nullable = false, updatable = false)
	@ColumnDefault("0")
	public boolean isPrequential() {
		return prequential;
	}

	/**
	 * @param prequential
	 *            the prequential to set
	 */
	public void setPrequential(boolean prequential) {
		this.prequential = prequential;
	}
}
