package smt;

import ast.*;
import interpret.Interpreter;
import lexer.*;
import logging.*;
import validate.UsageVisitor;
import verifier.VerificationVisitor;

import java.io.IOException;

public class Main {

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

        Interpreter interpreter = new Interpreter();
        interpreter.run(root);
    }
}
