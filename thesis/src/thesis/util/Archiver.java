package thesis.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.io.Files;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class Archiver {
	private static final int maxBackUps = 20;
	private static final int bufferSize = 1024;
	static Logger logger = LogManager.getLogger(Archiver.class);
	private static ZipParameters parameters;
	static {
		parameters = new ZipParameters();
		parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
		parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
	}

	public static void zip_old(String zipName, File sourceFile) throws FileNotFoundException, IOException {
		zip_old(zipName, sourceFile, null);
	}

	public static void zip_old(String zipName, File sourceFile, String backupZipFile)
			throws FileNotFoundException, IOException {

		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipName));
		zos.setLevel(Deflater.BEST_COMPRESSION);

		ZipEntry ze = new ZipEntry(sourceFile.getName());
		zos.putNextEntry(ze);

		FileInputStream in = new FileInputStream(sourceFile);

		int len;
		byte[] buffer = new byte[bufferSize];

		while ((len = in.read(buffer)) > 0) {
			zos.write(buffer, 0, len);
		}

		in.close();
		zos.closeEntry();
		zos.close();

		if (backupZipFile != null && !backupZipFile.isEmpty()) {
			manageBackUp(zipName, backupZipFile);
		}
	}

	public static List<File> unzip_old(String zipName, String outputFolder, String... lookupFileNames)
			throws FileNotFoundException, IOException {

		List<File> files = new ArrayList<File>();

		ZipInputStream zis = new ZipInputStream(new FileInputStream(zipName));

		ZipEntry ze = zis.getNextEntry();

		byte[] buffer = new byte[bufferSize];
		List<String> targetFiles = Arrays.asList(lookupFileNames);

		while (ze != null) {

			String fileName = ze.getName();
			if (targetFiles == null || targetFiles.size() < 1 || targetFiles.contains(fileName)) {
				File newFile = new File(outputFolder + File.separator + fileName);

				// create all non exists folders
				// else you will hit FileNotFoundException for compressed folder
				new File(newFile.getParent()).mkdirs();

				FileOutputStream fos = new FileOutputStream(newFile);

				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}

				fos.close();
				files.add(newFile);
			}

			if (files.size() >= targetFiles.size())
				break;
			ze = zis.getNextEntry();
		}

		zis.closeEntry();
		zis.close();

		return files;
	}

	public static void zip(String zipName, File sourceFile) throws ZipException, IOException {
		zip(zipName, sourceFile, null);
	}

	public static void zip(String zipName, File sourceFile, String backupZipFile) throws ZipException, IOException {

		ZipFile zipFile = new ZipFile(zipName);

		if (sourceFile.isFile()) {
			zipFile.addFile(sourceFile, parameters);
		} else if (sourceFile.isDirectory()) {
			zipFile.addFolder(sourceFile, parameters);
		}

		if (backupZipFile != null && !backupZipFile.isEmpty()) {
			manageBackUp(zipName, backupZipFile);
		}
	}

	public static List<File> unzip(String zipName, String outputFolder,
			String... lookupFileNames)
			throws ZipException {
		List<File> files = new ArrayList<File>();

		ZipFile zipFile = new ZipFile(zipName);
		for (String lookupFileName : lookupFileNames) {
			zipFile.extractFile(lookupFileName, outputFolder);
			files.add(new File(outputFolder + File.separator + lookupFileName));
		}

		return files;
	}

	private static void manageBackUp(String zipName, String backupZipFile) throws IOException {
		final File from = new File(zipName);
		final File to = new File(backupZipFile);
		if (to.exists())
			throw new IOException("Backup file " + backupZipFile + " exists.");
		Files.createParentDirs(from);
		Files.createParentDirs(to);
		Files.copy(from, to);

		final File backupDir = to.getParentFile();
		if (backupDir != null) {
			File[] files = backupDir.listFiles();
			int filesToDelete = files.length - maxBackUps;
			if (filesToDelete > 0) {
				Arrays.sort(files, (f1, f2) -> Long.valueOf(f1.lastModified())
						.compareTo(f2.lastModified()));

				for (int i = 0; i < filesToDelete; i++)
					files[i].delete();
			}
		}
	}
}
