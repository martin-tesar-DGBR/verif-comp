package ast;

import lexer.LocatedString;
import lexer.StaticToken;
import lexer.StaticTokenImpl;

public class BoolOperatorNode extends ASTNode {

	public enum Operator {
		OR,
		AND,
		NOT,
	}

	public Operator op;
	public ASTNode left;
	public ASTNode right;

	BoolOperatorNode(StaticTokenImpl token, ASTNode left, ASTNode right) {
		super(token.getLexeme());
		switch (token.token) {
			case OR -> {
				this.op = Operator.OR;
			}
			case AND -> {
				this.op = Operator.AND;
			}
			case NOT -> {
				this.op = Operator.NOT;
			}
			default -> {
				assert false;
			}
		}
		this.left = left;
		this.right = right;
	}

	public BoolOperatorNode(StaticToken token, ASTNode left, ASTNode right) {
		super(new LocatedString("",-1,-1));
		switch (token) {
			case OR -> {
				this.op = Operator.OR;
			}
			case AND -> {
				this.op = Operator.AND;
			}
			case NOT -> {
				this.op = Operator.NOT;
			}
			default -> {
				assert false;
			}
		}
		this.left = left;
		this.right = right;
	}

	@Override
	public void acceptVisitor(ASTVisitor visitor) {
		visitor.visitEnter(this);
		left.acceptVisitor(visitor);
		if (right != null) {
			right.acceptVisitor(visitor);
		}
		visitor.visitExit(this);
	}
}
