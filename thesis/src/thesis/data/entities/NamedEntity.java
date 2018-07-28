package thesis.data.entities;

import javax.persistence.Column;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class NamedEntity extends BaseEntity {
	private String name;

	/**
	 * @return the name
	 */
	@Column(name = "Name", unique = true, nullable = false, columnDefinition = "nvarchar(255)")
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

}