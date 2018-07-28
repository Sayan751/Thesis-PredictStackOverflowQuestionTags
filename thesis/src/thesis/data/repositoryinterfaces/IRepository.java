package thesis.data.repositoryinterfaces;

import java.util.List;
import java.util.UUID;

import thesis.data.entities.BaseEntity;

/**
 * Generic repository interface for basic CRUD.
 * 
 * @author Sayan
 *
 * @param <T>
 */
public interface IRepository<T extends BaseEntity> {
	/**
	 * Creates a new {@code entity}.
	 * 
	 * @param entity
	 *            Object to insert in data store.
	 * @return Created object.
	 */
	T create(T entity);

	/**
	 * Facilitates bulk insert.
	 * 
	 * @param entities
	 *            Entity objects to save.
	 * @return
	 */
	List<T> create(List<T> entities);

	/**
	 * Loads the entity object using the primary key {@code id}.
	 * 
	 * @param id
	 *            Primary key of the entity object.
	 * @return Loaded entity object
	 */
	T read(UUID id);

	/**
	 * Returns all entities of type {@code T}
	 * 
	 * @return
	 */
	List<T> all();

	/**
	 * Updates {@code entity} in data store.
	 * 
	 * @param entity
	 *            Entity object to update.
	 * @return Updated entity.
	 */
	T update(T entity);

	/**
	 * Deletes {@code entity} from data store.
	 * 
	 * @param entity
	 *            Entity object to delete.
	 * @return Deleted entity.
	 */
	T delete(T entity);

	/**
	 * Deletes entity from data store by primary key {@code id}.
	 * 
	 * @param id
	 *            Primary key of the entity object.
	 * @return Deleted object.
	 */
	T delete(UUID id);

	long getTotalCount();
}