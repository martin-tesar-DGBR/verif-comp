package usage;

import ast.ASTNode;
import ast.Parser;
import lexer.Lexer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class UsageTest {

	boolean testUsage(String filename) {
		ASTNode program = null;
		try {
			Lexer lexer = Lexer.make(filename);
			Parser parser = new Parser(lexer);
			program = parser.parseProgram();
			if (!lexer.dumpLogs() || program == null) {
				return false;
			}
		} catch (IOException e) {
			Assert.fail("Could not open file " + filename);
		}
		Assert.assertNotNull(program);
		UsageVisitor visitor = new UsageVisitor();
		program.acceptVisitor(visitor);
		return visitor.isUsageOk();
	}

	@Test
	public void invalidUsage() {
		Assert.assertTrue(testUsage("src/test/java/usage/pass/test1.txt"));
		Assert.assertTrue(testUsage("src/test/java/usage/pass/test2.txt"));

		Assert.assertFalse(testUsage("src/test/java/usage/fail/test1.txt"));
		Assert.assertFalse(testUsage("src/test/java/usage/fail/test2.txt"));
		Assert.assertFalse(testUsage("src/test/java/usage/fail/test3.txt"));
		Assert.assertFalse(testUsage("src/test/java/usage/fail/test4.txt"));
	}
}
