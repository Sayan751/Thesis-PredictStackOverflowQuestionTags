package thesis.data.repositories;

import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;

import thesis.data.entities.BaseEntity;
import thesis.data.repositoryinterfaces.IRepository;
import thesis.util.HibernateManager;

public abstract class GenericRepository<T extends BaseEntity> implements IRepository<T> {

	protected Class<T> persistentClass;
	public final EntityManager entityManager;
	protected final String basicEntityAlias = "e";
	protected String basicQuery;
	protected String entityName;

	@SuppressWarnings("unchecked")
	protected GenericRepository(String entityName) {
		if (entityName != null) {
			persistentClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
					.getActualTypeArguments()[0];
			// session = HibernateUtil.getSessionFactory()
			// .openSession();
			entityManager = HibernateManager.getEntityManager();
			this.entityName = entityName;
			basicQuery = String.format("select %2$s from %1$s %2$s", entityName, basicEntityAlias);
		} else {
			entityManager = null;
		}
	}

	@Override
	public T create(T entity) {
		entityManager.getTransaction()
				.begin();
		if (entity.getCreatedOn() == null)
			entity.setCreatedOn(Instant.now());
		// saving objects to session
		entityManager.persist(entity);
		entityManager.getTransaction()
				.commit();
		return entity;
	}

	@Override
	public List<T> create(List<T> entities) {
		entityManager.getTransaction()
				.begin();
		entities.forEach(entity -> {
			if (entity.getCreatedOn() == null)
				entity.setCreatedOn(Instant.now());
			entityManager.persist(entity);
		});
		entityManager.getTransaction()
				.commit();
		return entities;
	}

	@Override
	public T read(UUID id) {
		// entityManager.getTransaction()
		// .begin();
		try {
			return entityManager.find(persistentClass, id);

		} finally {
			// entityManager.getTransaction()
			// .commit();
		}
	}

	@Override
	public List<T> all() {
		return entityManager.createQuery(basicQuery, persistentClass)
				.getResultList();
	}

	@Override
	public T update(T entity) {
		entityManager.getTransaction()
				.begin();
		// saving objects to session
		entityManager.merge(entity);
		entityManager.getTransaction()
				.commit();
		return entity;
	}

	@Override
	public T delete(T entity) {
		entityManager.getTransaction()
				.begin();
		// saving objects to session
		entityManager.remove(entity);
		entityManager.getTransaction()
				.commit();
		return entity;
	}

	@Override
	public T delete(UUID id) {
		return delete(read(id));
	}

	@Override
	public long getTotalCount() {
		entityManager.getTransaction()
				.begin();
		long retVal = 0;
		try {
			List<?> result = entityManager
					.createQuery(
							"SELECT COUNT(" + basicEntityAlias + ".id) FROM " + entityName + " " + basicEntityAlias)
					.getResultList();

			if (result.size() > 0) {
				Object count = result.get(0);
				if (count != null)
					retVal = (long) count;
			}

		} finally {
			entityManager.getTransaction()
					.commit();
		}
		return retVal;
	}
}