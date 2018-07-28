package thesis.data.repositoryinterfaces;

import java.util.List;
import java.util.Set;

import thesis.data.entities.Tag;

public interface ITagRepository extends ILabelRepository<Tag> {
	List<Tag> getByIndices(Set<Integer> indices);
}