package smt;

import ast.*;
import interpret.Input;
import interpret.Interpreter;
import lexer.*;
import usage.UsageVisitor;
import verifier.VerificationVisitor;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    static class ConsoleInput implements Input {
        Scanner sc = new Scanner(System.in);

        @Override
        public String getLine() {
            return sc.nextLine();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: verif-comp.jar <input file>");
            return;
        }

        String filename = args[0];

        Lexer lexer;
        try {
            lexer = Lexer.make(filename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Parser parser = new Parser(lexer);
        ASTNode root = parser.parseProgram();
        if (!lexer.dumpLogs() || root == null) {
            return;
        }

        UsageVisitor usageVisitor = new UsageVisitor();
        root.acceptVisitor(usageVisitor);
        if (!usageVisitor.isUsageOk()) {
            return;
        }

        VerificationVisitor verificationVisitor = new VerificationVisitor();
        root.acceptVisitor(verificationVisitor);
        if (!verificationVisitor.verifyCondition()) {
            System.out.println("Could not verify program: " + filename);
            return;
        }

        Interpreter interpreter = new Interpreter(new ConsoleInput());
        interpreter.run(root);
    }
}
