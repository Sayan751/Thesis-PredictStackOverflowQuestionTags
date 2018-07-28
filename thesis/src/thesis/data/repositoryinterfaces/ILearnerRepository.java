package thesis.data.repositoryinterfaces;

import Learner.AbstractLearner;
import thesis.data.entities.Learner;

public interface ILearnerRepository extends IRepository<Learner> {

	Learner create(Learner entity, AbstractLearner learner);
	Learner update(Learner entity, AbstractLearner learner);
}