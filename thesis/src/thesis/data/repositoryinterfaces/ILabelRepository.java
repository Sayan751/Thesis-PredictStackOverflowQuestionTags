/**
 * 
 */
package thesis.data.repositoryinterfaces;

import thesis.data.entities.NamedEntity;

/**
 * @author Sayan
 *
 */
public interface ILabelRepository<T extends NamedEntity> extends INamedRepository<T> {
	int getTotalNumberOfLabels();
}
