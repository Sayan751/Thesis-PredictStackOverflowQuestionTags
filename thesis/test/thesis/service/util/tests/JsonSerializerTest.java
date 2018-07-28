package thesis.service.util.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import thesis.util.JsonSerializer;
import thesis.util.TreeNodeMixin;
import util.AdaptiveTree;
import util.CompleteTree;
import util.TreeNode;

public class JsonSerializerTest {

	File file;

	@After
	public void tearDown() {
		file.delete();
	}

	@Test
	public void canSerializeAdaptableTreeCorrectly() throws Exception {

		// arrange
		JsonSerializer.mapper.addMixIn(TreeNode.class, TreeNodeMixin.class);
		CompleteTree tree = new CompleteTree(2, 7);
		AdaptiveTree T = new AdaptiveTree(tree, CompleteTree.name, false);

		// act
		file = JsonSerializer.writeToFile("testTree.json", T);

		// assert
		assertNotNull(file);
	}

	@Test
	public void canDeserializeAdaptableTreeCorrectly() throws Exception {

		// arrange
		JsonSerializer.mapper.addMixIn(TreeNode.class, TreeNodeMixin.class);
		CompleteTree tree = new CompleteTree(2, 7);
		AdaptiveTree T = new AdaptiveTree(tree, CompleteTree.name, false);
		file = JsonSerializer.writeToFile("testTree.json", T);

		// act
		AdaptiveTree T1 = JsonSerializer.readFromFile(file, AdaptiveTree.class);

		// assert
		assertNotNull(T1);
		assertEquals(0, T1.getParent(T1.getChildNodes(0)
				.get(0)));
	}

}
