package ast;

import java.util.ArrayList;

public class PrintVisitor extends ASTVisitor.Default {

	ArrayList<Integer> indent;
	boolean newLine;

	public PrintVisitor() {
		this.indent = new ArrayList<>();
		this.newLine = true;
	}

	private void addIndent(int count) {
		this.indent.add(count);
	}

	private void clearIndent() {
		if (this.indent.size() > 0) {
			this.indent.set(this.indent.size() - 1, this.indent.get(this.indent.size() - 1) - 1);
		}
	}

	private void removeIndent() {
		this.indent.remove(this.indent.size() - 1);
	}

	private void printIndentation() {
		for (int i = 0; i < this.indent.size(); i += 1) {
			if (i == this.indent.size() - 1) {
				if (this.indent.get(i) > 0) {
					System.out.print("\u251C "); // '├'
				}
				else {
					System.out.print("\u2514 "); // '└'
				}
			}
			else {
				if (this.indent.get(i) > 0) {
					System.out.print("\u2502 "); // '│'
				}
				else {
					System.out.print("  "); // ' '
				}
			}
		}
	}

	private void print(String s) {
		if (this.newLine) {
			this.printIndentation();
		}
		System.out.print(s);
		this.newLine = false;
	}

	private void println(String s) {
		if (this.newLine) {
			this.printIndentation();
		}
		System.out.println(s);
		this.newLine = true;
	}

	@Override
	public void visitEnter(BlockNode node) {
		this.clearIndent();
		this.println("BLOCK");
		this.addIndent(node.children.size());
	}

	@Override
	public void visitExit(BlockNode node) {
		this.removeIndent();
	}

	@Override
	public void visitEnter(CheckNode node) {
		this.clearIndent();
		this.println("check");
		this.addIndent(1);
	}

	@Override
	public void visitExit(CheckNode node) {
		this.removeIndent();
	}

	@Override
	public void visitEnter(AssignmentNode node) {
		this.clearIndent();
		this.print("ASSIGN: ");
		this.println(node.lhs.s);
		this.addIndent(1);
		this.clearIndent();
	}

	@Override
	public void visitExit(AssignmentNode node) {
		this.removeIndent();
	}

	@Override
	public void visitEnter(IfNode node) {
		this.clearIndent();
		this.println("if");
		this.addIndent(3);
	}

	@Override
	public void visitExit(IfNode node) {
		this.removeIndent();
	}

	@Override
	public void visitEnter(IntOperatorNode node) {
		this.clearIndent();
		this.println(node.op.name());
		if (node.right == null) {
			this.addIndent(1);
		}
		else {
			this.addIndent(2);
		}
	}

	@Override
	public void visitExit(IntOperatorNode node) {
		this.removeIndent();
	}

	@Override
	public void visitEnter(BoolOperatorNode node) {
		this.clearIndent();
		this.println(node.op.name());
		if (node.right == null) {
			this.addIndent(1);
		}
		else {
			this.addIndent(2);
		}
	}

	@Override
	public void visitExit(BoolOperatorNode node) {
		this.removeIndent();
	}

	@Override
	public void visitEnter(BoolCompareNode node) {
		this.clearIndent();
		this.println(node.cmp.name());
		this.addIndent(2);
	}

	@Override
	public void visitExit(BoolCompareNode node) {
		this.removeIndent();
	}

	@Override
	public void visit(LabelNode node) {
		this.clearIndent();
		this.println(node.label.s);
	}

	@Override
	public void visit(IntConstantNode node) {
		this.clearIndent();
		this.println(node.lexeme.s);
	}

	@Override
	public void visit(PrintNode node) {
		this.clearIndent();
		this.println("print: " + node.variable.s);
	}

	@Override
	public void visit(ErrorNode node) {
		this.clearIndent();
		this.println("ERROR");
	}
}
