package parser;

import ast.*;
import lexer.Lexer;
import logging.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ParsingTest {
	void testPass(String filename) {
		try {
			Lexer lexer = Lexer.make(filename);
			Parser parser = new Parser(lexer);
			ASTNode program = parser.parseProgram();
			boolean pass = Logger.get(LogType.LEXER).dump() == LogLevel.DEBUG && Logger.get(LogType.PARSER).dump() == LogLevel.DEBUG;
			Assert.assertTrue("Program " + filename + " failed parsing.", pass);
		} catch (IOException e) {
			Assert.fail("Could not open file " + filename);
		}
		Logger.clearLogs();
	}

	void testFail(String filename) {
		try {
			Lexer lexer = Lexer.make(filename);
			Parser parser = new Parser(lexer);
			ASTNode program = parser.parseProgram();
			boolean pass = Logger.get(LogType.LEXER).dump() == LogLevel.DEBUG && Logger.get(LogType.PARSER).dump() == LogLevel.DEBUG;
			Assert.assertFalse("Program " + filename + " passed parsing.", pass);
		} catch (IOException e) {
			Assert.fail("Could not open file " + filename);
		}
		Logger.clearLogs();
	}

	@Test
	public void pass() {
		testPass("src/test/java/parser/pass/test1.txt");
		testPass("src/test/java/parser/pass/test2.txt");
	}

	@Test
	public void fail() {
		testFail("src/test/java/parser/fail/test1.txt");
		testFail("src/test/java/parser/fail/test2.txt");
		testFail("src/test/java/parser/fail/test3.txt");
	}
}
