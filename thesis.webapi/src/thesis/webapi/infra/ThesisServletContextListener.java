package thesis.webapi.infra;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thesis.util.HibernateManager;
import thesis.util.LearnerUtil;

@WebListener
public class ThesisServletContextListener implements ServletContextListener {

	static Logger logger = LogManager.getLogger(ThesisServletContextListener.class);

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		System.out.println("Service context destroyed.");
		HibernateManager.shutdown();
	}

	// Run this before web application is started
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		logger.info("Service context initialized.");
		Properties prop = new Properties();
		try {
			prop.load(arg0.getServletContext()
					.getResourceAsStream("META-INF/thesis.properties"));
			LearnerUtil.basePath = prop.getProperty("learnersPath", LearnerUtil.basePath);
			LearnerUtil.bkpPath = prop.getProperty("learnersBackupPath", LearnerUtil.bkpPath);

		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

	}
}