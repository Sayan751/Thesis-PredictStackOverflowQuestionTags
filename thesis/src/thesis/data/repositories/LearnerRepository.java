package thesis.data.repositories;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Learner.AbstractLearner;
import event.args.PLTDiscardedEventArgs;
import event.listeners.IPLTDiscardedListener;
import thesis.data.entities.Learner;
import thesis.data.repositoryinterfaces.ILearnerRepository;
import thesis.util.Constants;
import thesis.util.LearnerUtil;

public class LearnerRepository extends GenericRepository<Learner>
		implements ILearnerRepository, interfaces.ILearnerRepository, IPLTDiscardedListener {

	private Logger logger = LogManager.getLogger(LearnerRepository.class);
	private boolean withoutHibernate = false;

	public LearnerRepository() {
		this(false);
	}

	public LearnerRepository(boolean withoutHibernate) {
		super(withoutHibernate ? null : Constants.Entities.Learner);
		this.withoutHibernate = withoutHibernate;
	}

	@Override
	public Learner create(Learner entity, AbstractLearner learner) {
		Learner retVal = create(entity);
		LearnerUtil.serializeAndZip(retVal.getId(), learner);
		return retVal;
	}

	@Override
	public Learner update(Learner entity, AbstractLearner learner) {
		Learner retVal = update(entity);
		LearnerUtil.serializeAndZip(retVal.getId(), learner);
		return retVal;
	}

	@Override
	public UUID create(AbstractLearner learner, UUID parentId) {
		if (withoutHibernate)
			return create(learner);

		Learner entity = create(new Learner(parentId));
		UUID learnerId = entity.getId();
		learner.setId(learnerId);
		LearnerUtil.serializeAndZip(learnerId, learner);
		return learnerId;
	}

	public UUID create(AbstractLearner learner) {
		UUID learnerId = UUID.randomUUID();
		learner.setId(learnerId);
		LearnerUtil.serializeAndZip((UUID) learner.getId(), learner);
		return learnerId;
	}

	@Override
	public <T extends AbstractLearner> T read(Object learnerId, Class<T> learnerType) {
		return LearnerUtil.unzipAndDeserialize((UUID) learnerId, learnerType);
	}

	@Override
	public AbstractLearner update(Object learnerId, AbstractLearner learner) {
		LearnerUtil.serializeAndZip((UUID) learnerId, learner);
		return learner;
	}

	@Override
	public void onPLTDiscarded(Object source, PLTDiscardedEventArgs args) {
		Learner discardedLearner = read(args.pltId);
		discardedLearner.setActive(false);
		// update(discardedLearner, args.discardedPLT);
		update(discardedLearner);
		LearnerUtil.cleanupBackup(args.pltId);
		logger.info("PLT " + args.pltId + " is discarded.");
	}
}