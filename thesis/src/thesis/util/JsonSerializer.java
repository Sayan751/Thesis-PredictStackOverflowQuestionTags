package thesis.util;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.common.io.Files;

import util.TreeNode;

public class JsonSerializer {
	static Logger logger = LogManager.getLogger(JsonSerializer.class);
	public static ObjectMapper mapper;
	private static ObjectMapper simpleMapper;

	static {
		simpleMapper = new ObjectMapper();
		// default configuration for jackson
		mapper = new ObjectMapper();
		mapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);
		mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		// inject type information in json
		mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

		mapper.addMixIn(TreeNode.class, TreeNodeMixin.class);

	}

	public static File writeToFile(String fileName, Object source) throws IOException {
		File file = new File(fileName);
		Files.createParentDirs(file);
		try {
			mapper.writeValue(file, source);
		} finally {
		}
		return file;
	}

	public static <T> T readFromFile(String fileName, Class<T> valueType) throws IOException {
		return readFromFile(new File(fileName), valueType);
	}

	public static <T> T readFromFile(File file, Class<T> valueType) throws IOException {
		T retVal = null;
		try {
			retVal = mapper.readValue(file, valueType);
		} finally {
		}
		return retVal;
	}

	/**
	 * Try to deserilize the {@code jsonString} as per provided {@code type}. In
	 * case, the provided json string is not of provided type, it fails silently
	 * (only logs), and returns null.
	 * 
	 * @param jsonString
	 *            JSON string to deserialize.
	 * @param type
	 *            Target type.
	 * @return
	 */
	public static <T> T tryDeserializeSimple(String jsonString, Class<T> type) {
		T t = null;
		try {
			t = simpleMapper.readValue(jsonString, type);
		} catch (UnrecognizedPropertyException e) {
			logger.warn("Provided json string is not of type: " + type);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return t;
	}
}
