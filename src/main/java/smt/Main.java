package smt;

import ast.*;
import com.microsoft.z3.*;
import lexer.*;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        Lexer lexer;
        try {
            lexer = Lexer.make(args[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Parser parser = new Parser(lexer);
        ASTNode root = parser.parseProgram();
        PrintVisitor visitor = new PrintVisitor();
        root.acceptVisitor(visitor);
    }
}
