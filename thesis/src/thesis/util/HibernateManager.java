package thesis.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HibernateManager {
	private static Logger logger = LogManager.getLogger(HibernateManager.class);

	private static final String persistenceUnitName = "thesis";
	private static EntityManagerFactory entityManagerFactory;
	private static EntityManager entityManager;

	static {
		try {
			logger.info("Trying to create entityManager for persistence-unit: " + persistenceUnitName);
			entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
			entityManager = entityManagerFactory.createEntityManager();
			logger.info("Created entityManager for persistence-unit: " + persistenceUnitName);
		} catch (Exception e) {
			logger.error("Unable to create entityManager for persistence-unit: " + persistenceUnitName, e);			
			throw e;
		}
	}

	/**
	 * @return the entityManagerFactory
	 */
	protected static EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	/**
	 * @return the entityManager
	 */
	public static EntityManager getEntityManager() {
		return entityManager;
	}

	public static void shutdown() {
		entityManager.close();
		entityManagerFactory.close();
	}

	@Override
	protected void finalize() throws Throwable {
		entityManager.close();
		entityManagerFactory.close();
		super.finalize();
	}
}
