package thesis.data.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.persistence.TypedQuery;

import thesis.data.entities.Tag;
import thesis.data.repositoryinterfaces.ILabelRepository;
import thesis.data.repositoryinterfaces.ITagRepository;
import thesis.util.Constants;

public class TagRepository extends NamedRepository<Tag> implements ILabelRepository<Tag>, ITagRepository {

	private static class Parameters {
		static final String indices = "indices";
	}

	private TypedQuery<Tag> fetchOrStoreQuery;
	private TypedQuery<Tag> getByIndicesQuery;

	public TagRepository() {
		super(Constants.Entities.Tag);
		fetchOrStoreQuery = entityManager.createQuery(
				String.format("%1$s where %2$s.name in :names", basicQuery, basicEntityAlias),
				persistentClass);
		getByIndicesQuery = entityManager.createQuery(
				String.format("%1$s where %2$s.index in :indices", basicQuery, basicEntityAlias),
				persistentClass);
	}

	@Override
	public Tag create(Tag entity) {
		entity.setIndex(getTotalNumberOfLabels());
		return super.create(entity);
	}

	@Override
	public List<Tag> create(List<Tag> entities) {
		final int totalCount = getTotalNumberOfLabels();
		final int count = entities.size();
		for (int index = 0; index < count; index++)
			entities.get(index)
					.setIndex(totalCount + index);
		return super.create(entities);
	}

	@Override
	public int getTotalNumberOfLabels() {
		return Math.toIntExact(getTotalCount());
	}

	@Override
	public Tag createFromName(String name) {
		Tag tag = new Tag();
		tag.setName(name);
		tag.setCreatedOn(Instant.now());
		return tag;
	}

	@Override
	protected TypedQuery<Tag> getFetchOrStoreQuery() {
		return fetchOrStoreQuery;
	}

	@Override
	public List<Tag> getByIndices(Set<Integer> indices) {
		return getByIndicesQuery.setParameter(Parameters.indices, indices)
				.getResultList();
	}
}