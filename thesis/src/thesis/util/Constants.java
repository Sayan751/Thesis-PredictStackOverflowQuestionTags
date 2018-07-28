package thesis.util;

/**
 * Class defining all constants.
 * 
 * @author Sayan
 *
 */
public class Constants {
	/**
	 * Exposes names of all tables.
	 * 
	 * @author Sayan
	 *
	 */
	public class Tables {
		public static final String Tags = "Tags";
		public static final String Terms = "Terms";
		public static final String Learners = "Learners";
		public static final String Questions = "Questions";
		public static final String QuestionTagAssignments = "QuestionsTagAssignments";
		public static final String QuestionLearnerDetails = "QuestionLearnerDetails";
		public static final String QuestionTermFrequencies = "QuestionTermFrequencies";
	}

	/**
	 * Exposes names of all entities.
	 * 
	 * @author Sayan
	 *
	 */
	public class Entities {
		public static final String Tag = "Tag";
		public static final String Term = "Term";
		public static final String Learner = "Learner";
		public static final String Question = "Question";
		public static final String QuestionTermFrequency = "QuestionTermFrequency";
		public static final String QuestionLearnerDetails = "QuestionLearnerDetails";
	}

	public class Ui {
		public class Messages {
			public static final String learnerNotFound = "Learner is not found.";
			public static final String unrecognizedLearner = "Learner is not recognized.";
			public static final String unknownException = "Something went wrong.";
			public static final String cantFetchQuestion = "Can not fetch question; something went wrong.";
		}

		public class Seperators {
			public static final String tagSeparator = ", ";
		}
	}
}
