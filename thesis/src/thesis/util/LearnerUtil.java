package thesis.util;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Learner.AbstractLearner;
import Learner.AdaptivePLT;
import Learner.PLTAdaptiveEnsemble;
import Learner.PLTEnsembleBoosted;
import Learner.PLTEnsembleBoostedWithThreshold;
import net.lingala.zip4j.exception.ZipException;
import util.AdaptivePLTInitConfiguration;
import util.LearnerInitConfiguration;
import util.PLTAdaptiveEnsembleInitConfiguration;
import util.PLTEnsembleBoostedInitConfiguration;
import util.PLTEnsembleBoostedWithThresholdInitConfiguration;

public class LearnerUtil {
	static Logger logger = LogManager.getLogger(LearnerUtil.class);
	public static String basePath = "../learners/";
	public static String bkpPath = "../learnersBkp/";
	static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss_nnnnnnnnn");

	public static void serializeAndZip(UUID id, AbstractLearner learner) {
		try {
			File file = JsonSerializer.writeToFile(basePath + getJsonFileName(id), learner);
			Archiver.zip(getZipFileName(id), file, getBackupZipFileName(id));
			file.delete();
		} catch (IOException e) {
			throw new RuntimeException("serialize and/or zip failed for learnerId: " + id, e);
		} catch (ZipException e) {
			throw new RuntimeException("serialize and/or zip failed for learnerId: " + id, e);
		}
	}

	public static <T> T unzipAndDeserialize(UUID id, Class<T> learnerType) {
		T retVal = null;
		File file = null;
		try {
			file = Archiver.unzip(getZipFileName(id), basePath, getJsonFileName(id))
					.get(0);

			retVal = JsonSerializer.readFromFile(file, learnerType);

		} catch (IOException e) {
			// try {
			// file = Archiver.unzip(getBackupZipFileName(id), basePath,
			// getJsonFileName(id))
			// .get(0);
			//
			// retVal = JsonSerializer.readFromFile(file, learnerType);
			// } catch (IOException e1) {
			// throw new RuntimeException("deserialize and/or unzip failed for
			// learnerId: " + id, e1);
			// } catch (ZipException e1) {
			// throw new RuntimeException("deserialize and/or unzip failed for
			// learnerId: " + id, e);
			// }

			throw new RuntimeException("deserialize and/or unzip failed for learnerId: " + id, e);

		} catch (ZipException e) {
			throw new RuntimeException("deserialize and/or unzip failed for learnerId: " + id, e);
		} finally {
			if (file != null)
				file.delete();
		}
		return retVal;
	}

	public static void cleanupBackup(UUID id) {
		try {
			FileUtils.deleteDirectory(new File(bkpPath + id));
		} catch (IOException e) {
			logger.error("Unable to delete backup for " + id + ". Reason: " + e.getMessage(), e);
		}
	}

	private static String getJsonFileName(UUID id) {
		return id + ".json";
	}

	private static String getZipFileName(UUID id) {
		return basePath + id + ".zip";
	}

	private static String getBackupZipFileName(UUID id) {
		return bkpPath + id + "/" + LocalDateTime.now()
				.format(formatter) + ".zip";
	}

	public static SimpleEntry<Class<? extends AbstractLearner>, ? extends LearnerInitConfiguration> getLearnerClassAndConfig(
			String config) {

		LearnerInitConfiguration learnerConfig;

		if ((learnerConfig = JsonSerializer.tryDeserializeSimple(config,
				AdaptivePLTInitConfiguration.class)) != null)
			return new SimpleEntry<>(AdaptivePLT.class, learnerConfig);

		if ((learnerConfig = JsonSerializer.tryDeserializeSimple(config,
				PLTEnsembleBoostedInitConfiguration.class)) != null)
			return new SimpleEntry<>(PLTEnsembleBoosted.class, learnerConfig);

		if ((learnerConfig = JsonSerializer.tryDeserializeSimple(config,
				PLTEnsembleBoostedWithThresholdInitConfiguration.class)) != null)
			return new SimpleEntry<>(PLTEnsembleBoostedWithThreshold.class, learnerConfig);

		if ((learnerConfig = JsonSerializer.tryDeserializeSimple(config,
				PLTAdaptiveEnsembleInitConfiguration.class)) != null)
			return new SimpleEntry<>(PLTAdaptiveEnsemble.class, learnerConfig);

		return null;
	}
}