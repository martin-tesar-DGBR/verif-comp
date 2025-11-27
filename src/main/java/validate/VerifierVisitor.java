package validate;

import ast.*;
import com.microsoft.z3.*;
import lexer.LocatedString;
import logging.*;

import java.util.*;

public class VerifierVisitor extends ASTVisitor.Default {
    
    private final Context ctx;
    private final Logger logger;
    
    // Map from variable name to its current Z3 expression
    // Uses SSA-style: each assignment creates a new version
    private Map<String, IntExpr> variables;
    
    // Map from variable name to its assignment constraint (var == expr)
    // This tracks all assignments we've seen
    private List<BoolExpr> assignmentConstraints;
    
    // Stack of path conditions (boolean formulas that must be true at current point)
    private Stack<BoolExpr> pathConditions;
    
    // Stack to save variable states when entering if statements
    private Stack<Map<String, IntExpr>> variableStateStack;
    
    // Counter for generating fresh variable names in SSA
    private Map<String, Integer> varVersion;
    
    public VerifierVisitor() {
        this.ctx = new Context();
        this.logger = Logger.get(LogType.VERIFIER);
        this.variables = new HashMap<>();
        this.assignmentConstraints = new ArrayList<>();
        this.pathConditions = new Stack<>();
        this.variableStateStack = new Stack<>();
        this.varVersion = new HashMap<>();
    }
    
    private IntExpr getVariable(String name) {
        return variables.get(name);
    }
    
    private void setVariable(String name, IntExpr expr) {
        variables.put(name, expr);
    }
    
    private IntExpr createFreshVariable(String name) {
        int version = varVersion.getOrDefault(name, 0);
        varVersion.put(name, version + 1);
        String freshName = version == 0 ? name : name + "_" + version;
        return ctx.mkIntConst(freshName);
    }
    
    private void pushPathCondition(BoolExpr condition) {
        pathConditions.push(condition);
    }
    
    private BoolExpr popPathCondition() {
        return pathConditions.isEmpty() ? null : pathConditions.pop();
    }
    
    private BoolExpr getCurrentPathCondition() {
        if (pathConditions.isEmpty()) {
            return ctx.mkTrue();
        }
        BoolExpr[] conditions = pathConditions.toArray(new BoolExpr[0]);
        return ctx.mkAnd(conditions);
    }
    
    @Override
    public void visitEnter(AssignmentNode node) {
    }
    
    @Override
    public void visitExit(AssignmentNode node) {
        // Evaluate the RHS expression
        IntExpr rhsExpr = translateIntExpr(node.rhs);
        
        // Create a fresh variable for SSA
        String varName = node.lhs.s;
        IntExpr freshVar = createFreshVariable(varName);
        
        // Create assignment constraint: freshVar == rhsExpr
        // This constraint holds under the current path condition
        BoolExpr assignment = ctx.mkEq(freshVar, rhsExpr);
        BoolExpr pathCond = getCurrentPathCondition();
        BoolExpr constraint = ctx.mkImplies(pathCond, assignment);
        
        // Store the constraint
        assignmentConstraints.add(constraint);
        
        // Update variable mapping
        setVariable(varName, freshVar);
        
        // Log the assignment
        logger.log(LogLevel.DEBUG, "Assignment: " + varName + " := " + rhsExpr + " (under path: " + pathCond + ")");
    }
    
    // Track if we're currently processing branches of an if statement
    // 0 = not in if, 1 = processing then branch, 2 = processing else branch
    private int ifBranchState = 0;
    private BoolExpr currentIfCondition = null;
    
    @Override
    public void visitEnter(IfNode node) {
        // Save current variable state before entering if
        variableStateStack.push(new HashMap<>(variables));
        
        // Evaluate the condition
        BoolExpr condExpr = translateBoolExpr(node.cond);
        currentIfCondition = condExpr;
        
        // Mark that we're entering then branch
        ifBranchState = 1;
        
        // Push condition for then branch
        pushPathCondition(condExpr);
    }
    
    @Override
    public void visitExit(IfNode node) {
        // Both branches have been processed
        // Pop both path conditions (then and else)
        if (!pathConditions.isEmpty()) {
            popPathCondition(); // Remove else branch condition
        }
        if (!pathConditions.isEmpty()) {
            popPathCondition(); // Remove then branch condition
        }
        
        // Restore variable state from before the if
        // Variables assigned in branches are still accessible but constrained by path conditions
        if (!variableStateStack.isEmpty()) {
            variables = variableStateStack.pop();
        }
        
        ifBranchState = 0;
        currentIfCondition = null;
    }
    
    @Override
    public void visitEnter(BlockNode node) {
        // If we just finished the then branch (ifBranchState == 1),
        // the next block is the else branch
        if (ifBranchState == 1 && currentIfCondition != null) {
            // We're now entering the else branch
            ifBranchState = 2;
            // Pop the then condition and push the else condition
            if (!pathConditions.isEmpty()) {
                popPathCondition(); // Remove then condition
            }
            pushPathCondition(ctx.mkNot(currentIfCondition)); // Push else condition
        }
    }
    
    @Override
    public void visitExit(BlockNode node) {
    }
    
    @Override
    public void visitEnter(CheckNode node) {
    }
    
    @Override
    public void visitExit(CheckNode node) {
        // Translate the check expression to Z3
        BoolExpr checkExpr = translateBoolExpr(node.expr);
        
        // Get current path condition
        BoolExpr pathCond = getCurrentPathCondition();
        
        // Build the formula to verify:
        // (all assignment constraints) AND pathCond -> checkExpr
        // This means: under the current path and all assignments, the check must hold
        
        // Collect all relevant constraints
        List<BoolExpr> allConstraints = new ArrayList<>(assignmentConstraints);
        allConstraints.add(pathCond);
        
        // Verify: (constraints AND pathCond) -> checkExpr
        BoolExpr antecedent = ctx.mkAnd(allConstraints.toArray(new BoolExpr[0]));
        BoolExpr toVerify = ctx.mkImplies(antecedent, checkExpr);
        
        // Check validity by checking if negation is unsatisfiable
        Solver solver = ctx.mkSolver();
        BoolExpr negation = ctx.mkNot(toVerify);
        solver.add(negation);
        
        Status status = solver.check();
        
        LocatedString loc = node.lexeme;
        if (status == Status.UNSATISFIABLE) {
            // Check always holds
            logger.log(LogLevel.DEBUG, 
                "Check verified at line " + loc.line + ", column " + loc.col);
        } else if (status == Status.SATISFIABLE) {
            // Check can fail - found counterexample
            Model model = solver.getModel();
            logger.log(LogLevel.WARNING,
                "Check may fail at line " + loc.line + ", column " + loc.col +
                " (counterexample found)");
            logger.log(LogLevel.DEBUG, "Model: " + model);
        } else {
            // Unknown
            logger.log(LogLevel.WARNING,
                "Check verification unknown at line " + loc.line + ", column " + loc.col);
        }
    }
    
    // Translate an integer expression AST node to a Z3 IntExpr.
    private IntExpr translateIntExpr(ASTNode node) {
        if (node instanceof IntConstantNode) {
            IntConstantNode c = (IntConstantNode) node;
            int value = Integer.parseInt(c.lexeme.s);
            return ctx.mkInt(value);
        } else if (node instanceof LabelNode) {
            LabelNode l = (LabelNode) node;
            String name = l.label.s;
            IntExpr var = getVariable(name);
            if (var == null) {
                // Variable not assigned yet - create a fresh unconstrained variable
                var = createFreshVariable(name);
                setVariable(name, var);
                logger.log(LogLevel.DEBUG, "Using uninitialized variable: " + name);
            }
            return var;
        } else if (node instanceof IntOperatorNode) {
            return translateIntOperator((IntOperatorNode) node);
        } else {
            throw new IllegalStateException(
                "Unexpected node type in integer expression: " + node.getClass());
        }
    }
    
    private IntExpr translateIntOperator(IntOperatorNode node) {
        IntOperatorNode.Operator op = node.op;
        
        if (op == IntOperatorNode.Operator.NEGATE) {
            IntExpr operand = translateIntExpr(node.left);
            return (IntExpr) ctx.mkUnaryMinus(operand);
        }
        
        IntExpr left = translateIntExpr(node.left);
        IntExpr right = translateIntExpr(node.right);
        
        switch (op) {
            case ADD:
                return (IntExpr) ctx.mkAdd(left, right);
            case SUB:
                return (IntExpr) ctx.mkSub(left, right);
            case MUL:
                return (IntExpr) ctx.mkMul(left, right);
            default:
                throw new IllegalStateException("Unexpected int operator: " + op);
        }
    }
    
    private BoolExpr translateBoolExpr(ASTNode node) {
        if (node instanceof BoolCompareNode) {
            return translateBoolCompare((BoolCompareNode) node);
        } else if (node instanceof BoolOperatorNode) {
            return translateBoolOperator((BoolOperatorNode) node);
        } else {
            throw new IllegalStateException(
                "Unexpected node type in boolean expression: " + node.getClass());
        }
    }
    
    private BoolExpr translateBoolCompare(BoolCompareNode node) {
        IntExpr left = translateIntExpr(node.left);
        IntExpr right = translateIntExpr(node.right);
        
        switch (node.cmp) {
            case GREATER:
                return ctx.mkGt(left, right);
            case LESSER:
                return ctx.mkLt(left, right);
            case EQUAL:
                return ctx.mkEq(left, right);
            default:
                throw new IllegalStateException("Unexpected comparison: " + node.cmp);
        }
    }
    
    private BoolExpr translateBoolOperator(BoolOperatorNode node) {
        BoolOperatorNode.Operator op = node.op;
        
        if (op == BoolOperatorNode.Operator.NOT) {
            BoolExpr operand = translateBoolExpr(node.left);
            return ctx.mkNot(operand);
        }
        
        BoolExpr left = translateBoolExpr(node.left);
        BoolExpr right = translateBoolExpr(node.right);
        
        switch (op) {
            case AND:
                return ctx.mkAnd(left, right);
            case OR:
                return ctx.mkOr(left, right);
            default:
                throw new IllegalStateException("Unexpected boolean operator: " + op);
        }
    }
    
    public void close() {
        ctx.close();
    }
}

