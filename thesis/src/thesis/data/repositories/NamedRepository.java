package thesis.data.repositories;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.TypedQuery;

import thesis.data.entities.NamedEntity;
import thesis.data.repositoryinterfaces.INamedRepository;

public abstract class NamedRepository<T extends NamedEntity> extends GenericRepository<T>
		implements INamedRepository<T> {
	
	private static class Parameters {
		static final String names = "names";
	}

	/**
	 * To avoid sending too many parameters to database.
	 */
	private static final int MaxChunkSize = 2000;

	protected NamedRepository(String entityName) {
		super(entityName);
	}

	public HashMap<String, T> fetchOrStoreByName(List<String> names) {
		HashMap<String, T> retVal = new HashMap<String, T>();

		int size = names.size();
		int chunks = (names.size() / MaxChunkSize) + 1;

		for (int i = 0; i < chunks; i++) {
			getFetchOrStoreQuery()
					.setParameter(Parameters.names, names.subList(i * MaxChunkSize, Math.min((i + 1) * MaxChunkSize, size)))
					.getResultList()
					.forEach(tag -> retVal.put(tag.getName(), tag));
		}

		// Collect the rest.
		HashSet<String> nameSet = new HashSet<String>(names);
		nameSet.removeAll(retVal.keySet());

		// Create the rest and add to the main set
		
		create(nameSet
				.stream()
				.map(label -> createFromName(label))
				.collect(Collectors.toList()))
						.forEach(tag -> retVal.put(tag.getName(), tag));

		return retVal;
	}

	protected abstract TypedQuery<T> getFetchOrStoreQuery();
}
