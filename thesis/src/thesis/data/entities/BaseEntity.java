package thesis.data.entities;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;

/**
 * Abstract base entity defining some common properties.
 * 
 * @author Sayan
 *
 */
@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class BaseEntity {
	protected UUID id;
	protected Instant createdOn;

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue
	@Column(name = "Id") // , columnDefinition = "uniqueidentifier"
	// @GeneratedValue(generator = "uuid")
	// @GenericGenerator(name = "uuid", strategy = "guid")
	public UUID getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(UUID id) {
		this.id = id;
	}

	/**
	 * @return the createdOn
	 */
	@Column(name = "CreatedOn", nullable = false, columnDefinition = "datetime")
	public Instant getCreatedOn() {
		return createdOn;
	}

	/**
	 * @param createdOn
	 *            the createdOn to set
	 */
	public void setCreatedOn(Instant createdOn) {
		this.createdOn = createdOn;
	}
}
