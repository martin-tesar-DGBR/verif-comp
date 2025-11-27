package ast;

import lexer.LocatedString;

import java.util.List;
import java.util.ListIterator;

public class BlockNode extends ASTNode {
	public List<ASTNode> children;

	BlockNode(LocatedString lexeme) {
		super(lexeme);
	}

	@Override
	public void acceptVisitor(ASTVisitor visitor) {
		visitor.visitEnter(this);
		switch (visitor.getTraversalOrder()) {
			case FORWARDS -> {
				for (ASTNode child : children) {
					child.acceptVisitor(visitor);
				}
			}
			case BACKWARDS -> {
				ListIterator<ASTNode> li = children.listIterator(children.size());
				while (li.hasPrevious()) {
					ASTNode child = li.previous();
					child.acceptVisitor(visitor);
				}
			}
		}
		visitor.visitExit(this);
	}
}
