package thesis.data.repositoryinterfaces;

import java.util.HashMap;
import java.util.List;

import thesis.data.entities.NamedEntity;

public interface INamedRepository<T extends NamedEntity> extends IRepository<T> {

	/**
	 * Either fetches the already stored entities from data store or create new
	 * ones from {@code names}.
	 * 
	 * @param names
	 *            values of {@code name} property of the objects.
	 * @return A map with {@code {name, object}} pair
	 */
	HashMap<String, T> fetchOrStoreByName(List<String> names);
	
	T createFromName(String name);
}
