package ast;

import lexer.LocatedString;

public class InputNode extends ASTNode {
	public LocatedString lhs;

	InputNode(LocatedString lexeme, LocatedString lhs) {
		super(lexeme);
		this.lhs = lhs;
	}

	@Override
	public void acceptVisitor(ASTVisitor visitor) {
		visitor.visit(this);
	}
}
