package thesis.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.google.common.io.Files;

public class TextFileWriter {

	public static void writeToFile(String fileName, String content) {
		writeToFile(fileName, content, false);
	}

	public static void writeToFile(String fileName, String content, boolean append) {
		try {
			File file = new File(fileName);
			Files.createParentDirs(file);

			try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, append))) {
				bw.write(content);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
