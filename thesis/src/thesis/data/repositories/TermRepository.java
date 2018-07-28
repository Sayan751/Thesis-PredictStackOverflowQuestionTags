package thesis.data.repositories;

import java.time.Instant;
import java.util.List;

import javax.persistence.TypedQuery;

import thesis.data.entities.Term;
import thesis.data.repositoryinterfaces.ITermRepository;
import thesis.util.Constants;

public class TermRepository extends NamedRepository<Term> implements ITermRepository {

	private TypedQuery<Term> fetchOrStoreQuery;

	public TermRepository() {
		super(Constants.Entities.Term);
		fetchOrStoreQuery = entityManager.createQuery(
				String.format("%1$s where %2$s.name in :names", basicQuery, basicEntityAlias),
				persistentClass);
	}

	@Override
	public Term create(Term entity) {
		entity.setIndex(Math.toIntExact(getTotalCount()));
		return super.create(entity);
	}

	@Override
	public List<Term> create(List<Term> entities) {
		final int totalCount = Math.toIntExact(getTotalCount());
		final int count = entities.size();
		for (int index = 0; index < count; index++)
			entities.get(index)
					.setIndex(totalCount + index);
		return super.create(entities);
	}

	@Override
	public Term createFromName(String name) {
		Term term = new Term();
		term.setName(name);
		term.setCreatedOn(Instant.now());
		return term;
	}

	@Override
	protected TypedQuery<Term> getFetchOrStoreQuery() {
		return fetchOrStoreQuery;
	}
}
