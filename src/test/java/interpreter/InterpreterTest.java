package interpreter;

import ast.ASTNode;
import ast.Parser;
import interpret.Interpreter;
import lexer.Lexer;
import logging.LogLevel;
import logging.LogType;
import logging.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class InterpreterTest {

   
    private String interpretFile(String filename) throws IOException {
        return interpretFileWithStdoutAndStderr(filename)[0];
    }

    private String[] interpretFileWithStdoutAndStderr(String filename) throws IOException {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        ByteArrayOutputStream outBaos = new ByteArrayOutputStream();
        ByteArrayOutputStream errBaos = new ByteArrayOutputStream();
        PrintStream outPs = new PrintStream(outBaos);
        PrintStream errPs = new PrintStream(errBaos);

        System.setOut(outPs);
        System.setErr(errPs);

        try {
            Lexer lexer = Lexer.make(filename);
            Parser parser = new Parser(lexer);
            ASTNode program = parser.parseProgram();

            boolean parsedOk = lexer.dumpLogs() && program != null;

            Assert.assertTrue("Program " + filename + " failed parsing.", parsedOk);

            Interpreter interpreter = new Interpreter();
            interpreter.run(program);

        } finally {
            System.out.flush();
            System.err.flush();
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        String out = outBaos.toString().replace("\r\n", "\n");
        String err = errBaos.toString().replace("\r\n", "\n");
        return new String[]{out, err};
    }

    // ---------- existing PASS tests ----------

    @Test
    public void simpleAssignAndPrint() throws IOException {
        String out = interpretFile("src/test/java/interpreter/pass/test1.txt");
        Assert.assertEquals("1\n2\n", out);
    }

    @Test
    public void ifElseExecution() throws IOException {
        String out = interpretFile("src/test/java/interpreter/pass/test2.txt");
        Assert.assertEquals("8\n", out);
    }

    // ---------- existing FAIL test ----------

    @Test
    public void useBeforeAssignFailsAtRuntime() throws IOException {
        try {
            interpretFile("src/test/java/interpreter/fail/test1.txt");
            Assert.fail("Program should have failed at runtime (use-before-assign).");
        } catch (RuntimeException e) {
        }
    }

    @Test
    public void checkPasses_NoErrorPrinted() throws IOException {
        String[] outErr = interpretFileWithStdoutAndStderr(
                "src/test/java/interpreter/pass/test3.txt");
        Assert.assertEquals("5\n", outErr[0]);       
        Assert.assertEquals("", outErr[1]);          
    }

    @Test
    public void checkFails_ErrorOnStderr() throws IOException {
        String[] outErr = interpretFileWithStdoutAndStderr(
                "src/test/java/interpreter/pass/test4.txt");
        Assert.assertEquals("10\n", outErr[0]);                     
        Assert.assertTrue(outErr[1].contains("Runtime check failed"));
    }
}
