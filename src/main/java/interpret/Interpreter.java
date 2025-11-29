package interpret;

import ast.*;
import lexer.LocatedString;

import java.math.BigInteger;
import java.util.*;


public class Interpreter {

    // Single environment mapping variable name -> integer value
    private final Map<String, BigInteger> env = new HashMap<>();
    Input input;

    public Interpreter(Input input) {
        this.input = input;
    }

    /**
     * Entry point: interpret the entire program.
     */
    public void run(ASTNode root) {
        if (!(root instanceof BlockNode)) {
            throw new IllegalArgumentException("Program root must be a BlockNode");
        }
        executeBlock((BlockNode) root);
    }

    // =========================
    //   STATEMENTS
    // =========================

    private void executeBlock(BlockNode block) {
        List<ASTNode> statements = block.children;
        if (statements == null) return;
        for (ASTNode stmt : statements) {
            executeStatement(stmt);
        }
    }

    private void executeStatement(ASTNode stmt) {
        if (stmt instanceof AssignmentNode) {
            executeAssignment((AssignmentNode) stmt);
        }
        else if (stmt instanceof InputNode) {
            executeInput((InputNode) stmt);
        }
        else if (stmt instanceof IfNode) {
            executeIf((IfNode) stmt);
        }
        else if (stmt instanceof PrintNode) {
            executePrint((PrintNode) stmt);
        }
        else if (stmt instanceof CheckNode) {
            // do nothing; validated at compile time
        }
        else {
            throw new IllegalStateException("Unexpected statement node type: " + stmt.getClass());
        }
    }

    private void executeAssignment(AssignmentNode node) {
        String varName = node.lhs.s;
        BigInteger value = evalInt(node.rhs);
        env.put(varName, value);
    }

    private void executeInput(InputNode node) {
        String varName = node.lhs.s;
        int line = node.lhs.line;
        BigInteger value = null;
        boolean valid = false;
        String input;
        Scanner sc = new Scanner(System.in);
        do {
            System.out.println("Enter value for " + varName + " on line " + line + ":");
            input = sc.nextLine();
            try {
                value = new BigInteger(input);
                valid = true;
            } catch (NumberFormatException e) {
                System.out.println("Invalid value: " + input);
            }
        } while (!valid);
        env.put(varName, value);
    }

    private void executeIf(IfNode node) {
        boolean cond = evalBool(node.cond);
        if (cond) {
            executeBlock(node.branchThen);
        }
        else {
            executeBlock(node.branchElse);
        }
    }

    private void executePrint(PrintNode node) {
        String varName = node.variable.s;
        BigInteger value = env.get(varName);
        if (value == null) {
            LocatedString loc = node.lexeme;
            throw new RuntimeException(
                "Variable \"" + varName + "\" used before assigned at line "
                + loc.line + ", column " + loc.col
            );
        }
        System.out.println(varName + ": " + value);
    }

    // =========================
    //   EXPRESSIONS – INT
    // =========================

    private BigInteger evalInt(ASTNode node) {
        if (node instanceof IntConstantNode) {
            IntConstantNode c = (IntConstantNode) node;
            return new BigInteger(c.lexeme.s);
        }
        else if (node instanceof LabelNode) {
            LabelNode l = (LabelNode) node;
            String name = l.label.s;
            BigInteger value = env.get(name);
            if (value == null) {
                LocatedString loc = l.label;
                throw new RuntimeException(
                    "Variable \"" + name + "\" used before assigned at line "
                    + loc.line + ", column " + loc.col
                );
            }
            return value;
        }
        else if (node instanceof IntOperatorNode) {
            return evalIntOperator((IntOperatorNode) node);
        }
        else {
            throw new IllegalStateException(
                "Expected integer expression, got " + node.getClass()
            );
        }
    }

    private BigInteger evalIntOperator(IntOperatorNode node) {
        switch (node.op) {
            case ADD -> {
                BigInteger left = evalInt(node.left);
                BigInteger right = evalInt(node.right);
                return left.add(right);
            }
            case SUB -> {
                BigInteger left = evalInt(node.left);
                BigInteger right = evalInt(node.right);
                return left.subtract(right);
            }
            case NEGATE -> {
                BigInteger v = evalInt(node.left);
                return v.negate();
            }
            default -> {
                throw new IllegalStateException("Unexpected int operator: " + node.op);
            }
        }
    }

    // =========================
    //   EXPRESSIONS – BOOL
    // =========================

    private boolean evalBool(ASTNode node) {
        if (node instanceof BoolCompareNode) {
            return evalBoolCompare((BoolCompareNode) node);
        }
        else if (node instanceof BoolOperatorNode) {
            return evalBoolOperator((BoolOperatorNode) node);
        }
        else {
            throw new IllegalStateException("Expected boolean expression, got " + node.getClass());
        }
    }

    private boolean evalBoolCompare(BoolCompareNode node) {
        BigInteger left = evalInt(node.left);
        BigInteger right = evalInt(node.right);

        int cmp = left.compareTo(right);

        switch (node.cmp) {
            case GREATER:
                return cmp > 0;
            case LESSER:
                return cmp < 0;
            case EQUAL:
                return cmp == 0;
            default:
                throw new IllegalStateException("Unexpected comparison: " + node.cmp);
        }
    }

    private boolean evalBoolOperator(BoolOperatorNode node) {
        BoolOperatorNode.Operator op = node.op;

        if (op == BoolOperatorNode.Operator.NOT) {
            // unary NOT, right is null
            return !evalBool(node.left);
        }

        // binary operators (short-circuit)
        if (op == BoolOperatorNode.Operator.AND) {
            boolean left = evalBool(node.left);
            if (!left) return false;
            return evalBool(node.right);
        } else if (op == BoolOperatorNode.Operator.OR) {
            boolean left = evalBool(node.left);
            if (left) return true;
            return evalBool(node.right);
        } else {
            throw new IllegalStateException("Unexpected boolean operator: " + op);
        }
    }
}
