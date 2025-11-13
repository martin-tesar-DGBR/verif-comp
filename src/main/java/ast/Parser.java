package ast;

import lexer.*;
import logging.*;

import java.util.*;

public class Parser {
	public Lexer stream;
	public Logger logger;

	public Parser(Lexer stream) {
		this.stream = stream;
		this.logger = Logger.get(LogType.PARSER);
	}

	public ASTNode parseProgram() {
		ASTNode program = parseBlock();
		return program;
	}

	// EVERY time the stream is read it must have hasNext() called and flag an error if it doesn't.
	// This is encapsulated in checkHasNext() for "dumb" checks and expect() for StaticTokens

	private boolean expect(StaticToken... tokens) {
		if (tokens.length < 1) {
			throw new IllegalArgumentException("There must be at least one argument to expect()");
		}
		if (!stream.hasNext()) {
			StringBuilder builder = new StringBuilder();
			if (tokens.length == 1) {
				builder.append("Expected");
			}
			else {
				builder.append("Expected one of");
			}
			for (StaticToken token : tokens) {
				builder.append(' ')
					.append(token.lexeme)
					.append(',');
			}
			builder.append(" reached end of file");
			logger.log(LogLevel.SEVERE, builder.toString());
			return false;
		}
		Token read = this.stream.peek();
		for (StaticToken token : tokens) {
			if (read instanceof StaticTokenImpl t && t.token == token) {
				this.stream.next();
				return true;
			}
		}
		LocatedString lexeme = read.getLexeme();
		StringBuilder builder = new StringBuilder();
		if (tokens.length == 1) {
			builder.append("Expected");
		}
		else {
			builder.append("Expected one of");
		}
		for (StaticToken token : tokens) {
			builder.append(' ')
				.append(token.lexeme);
		}
		builder.append(", got ")
			.append(lexeme.s)
			.append(" at line ")
			.append(lexeme.line)
			.append(", column ")
			.append(lexeme.col);
		logger.log(LogLevel.SEVERE, builder.toString());
		return false;
	}

	private boolean checkHasNext() {
		if (!stream.hasNext()) {
			logger.log(LogLevel.SEVERE, "Reached end of file while parsing.");
			return false;
		}
		return true;
	}

	private ASTNode parseBlock() {
		Token blockToken = this.stream.peek();
		if (!expect(StaticToken.LEFT_BRACE)) return null;
		List<ASTNode> statements = new ArrayList<>();
		while (startsStatement()) {
			ASTNode statement = parseStatement();
			statements.add(statement);
		}
		if (!expect(StaticToken.RIGHT_BRACE)) return null;
		BlockNode block = new BlockNode(blockToken.getLexeme());
		block.children = statements;
		return block;
	}

	private boolean startsStatement() {
		if (!checkHasNext()) return false;
		Token next = stream.peek();
		if (next instanceof StaticTokenImpl st) {
			return st.token == StaticToken.IF || st.token == StaticToken.CHECK || st.token == StaticToken.PRINT;
		}
		else if (next instanceof LabelToken) {
			return true;
		}
		return false;
	}

	private ASTNode parseStatement() {
		// honestly this can be folded into parseBlock
		if (!checkHasNext()) return null;
		Token next = stream.peek();
		if (next instanceof StaticTokenImpl st) {
			switch (st.token) {
				case IF -> {
					return parseIfStatement();
				}
				case CHECK -> {
					return parseCheckStatement();
				}
				case PRINT -> {
					return parsePrintStatement();
				}
				default -> {
					throw new IllegalStateException("unreachable");
				}
			}
		}
		else if (next instanceof LabelToken) {
			return parseAssignmentStatement();
		}
		throw new IllegalStateException("unreachable");
	}

	private ASTNode parseIfStatement() {
		if (!checkHasNext()) return null;
		Token token = stream.peek();
		if (!expect(StaticToken.IF)) return null;
		ASTNode cond = parseBoolExpr();
		ASTNode branchThen = parseBlock();
		// assert branchThen instanceof BlockNode;
		if (!expect(StaticToken.ELSE)) return null;
		ASTNode branchElse = parseBlock();
		// assert branchElse instanceof BlockNode;

		IfNode node = new IfNode(token.getLexeme(), cond, (BlockNode) branchThen, (BlockNode) branchElse);
		return node;
	}

	private ASTNode parseCheckStatement() {
		if (!checkHasNext()) return null;
		Token token = stream.peek();
		if (!expect(StaticToken.CHECK)) return null;
		if (!expect(StaticToken.LEFT_PAREN)) return null;
		ASTNode expr = parseBoolExpr();
		if (!expect(StaticToken.RIGHT_PAREN)) return null;
		return new CheckNode(token.getLexeme(), expr);
	}

	private ASTNode parsePrintStatement() {
		if (!checkHasNext()) return null;
		Token token = stream.peek();
		if (!expect(StaticToken.PRINT)) return null;
		if (!expect(StaticToken.LEFT_PAREN)) return null;
		if (!checkHasNext()) return null;
		Token label = stream.next();
		if (label instanceof LabelToken l) {
			if (!expect(StaticToken.RIGHT_PAREN)) return null;
			return new PrintNode(token.getLexeme(), l.getLexeme());
		}
		else {
			logger.log(LogLevel.SEVERE, "Argument to print() expected label, got " + label.getLexeme());
			return null;
		}
	}

	private ASTNode parseAssignmentStatement() {
		if (!checkHasNext()) return null;
		Token label = stream.next();
		if (label instanceof LabelToken l) {
			if (!checkHasNext()) return null;
			Token assign = stream.peek();
			if (!expect(StaticToken.ASSIGN)) return null;
			ASTNode expr = parseIntExpr();
			return new AssignmentNode(assign.getLexeme(), l.getLexeme(), expr);
		}
		else {
			return null;
		}
	}

	private ASTNode parseIntExpr() {
		return parseIntAddExpr();
	}

	private ASTNode parseIntAddExpr() {
		ASTNode left = parseIntMulExpr();
		while (checkHasNext()) {
			Token op = stream.peek();
			if (op instanceof StaticTokenImpl t && (t.token == StaticToken.ADD || t.token == StaticToken.SUB)) {
				if (!expect(StaticToken.ADD, StaticToken.SUB)) return null;
				ASTNode right = parseIntMulExpr();
				left = new IntOperatorNode(t, left, right);
			}
			else {
				break;
			}
		}
		return left;
	}

	private ASTNode parseIntMulExpr() {
		ASTNode left = parseIntNegateExpr();
		while (checkHasNext()) {
			Token op = stream.peek();
			if (op instanceof StaticTokenImpl t && t.token == StaticToken.MUL) {
				if (!expect(StaticToken.MUL)) return null;
				ASTNode right = parseIntNegateExpr();
				left = new IntOperatorNode(t, left, right);
			}
			else {
				break;
			}
		}
		return left;
	}

	private ASTNode parseIntNegateExpr() {
		if (!checkHasNext()) return null;
		Token op = stream.peek();
		if (op instanceof StaticTokenImpl t && t.token == StaticToken.SUB) {
			if (!expect(StaticToken.SUB)) return null;
			ASTNode expr = parseIntParenExpr();
			return new IntOperatorNode(t, expr, null);
		}
		else {
			return parseIntParenExpr();
		}
	}

	private ASTNode parseIntParenExpr() {
		if (!checkHasNext()) return null;
		Token token = stream.peek();
		if (token instanceof StaticTokenImpl) {
			if (!expect(StaticToken.LEFT_PAREN)) return null;
			ASTNode expr = parseIntExpr();
			if (!expect(StaticToken.RIGHT_PAREN)) return null;
			return expr;
		}
		else if (token instanceof LabelToken label) {
			stream.next();
			return new LabelNode(label.getLexeme());
		}
		else if (token instanceof IntToken integer) {
			stream.next();
			return new IntConstantNode(integer.getLexeme());
		}
		else {
			throw new RuntimeException("unreachable");
		}
	}

	private ASTNode parseBoolExpr() {
		return parseBoolOrExpr();
	}

	private ASTNode parseBoolOrExpr() {
		ASTNode fst = parseBoolAndExpr();
		if (!checkHasNext()) return fst;
		Token token = stream.peek();
		if (token instanceof StaticTokenImpl t && t.token == StaticToken.OR) {
			if (!expect(StaticToken.OR)) return null;
			ASTNode snd = parseBoolOrExpr();
			return new BoolOperatorNode(t, fst, snd);
		}
		else {
			return fst;
		}
	}

	private ASTNode parseBoolAndExpr() {
		ASTNode fst = parseBoolNotExpr();
		if (!checkHasNext()) return fst;
		Token token = stream.peek();
		if (token instanceof StaticTokenImpl t && t.token == StaticToken.AND) {
			if (!expect(StaticToken.AND)) return null;
			ASTNode snd = parseBoolAndExpr();
			return new BoolOperatorNode(t, fst, snd);
		}
		else {
			return fst;
		}
	}

	private ASTNode parseBoolNotExpr() {
		if (!checkHasNext()) return null;
		Token op = stream.peek();
		if (op instanceof StaticTokenImpl t && t.token == StaticToken.NOT) {
			if (!expect(StaticToken.NOT)) return null;
			ASTNode expr = parseBoolParenExpr();
			return new BoolOperatorNode(t, expr, null);
		}
		else {
			return parseBoolParenExpr();
		}
	}

	private ASTNode parseBoolParenExpr() {
		if (!checkHasNext()) return null;
		Token token = stream.peek();
		if (token instanceof StaticTokenImpl) {
			if (!expect(StaticToken.LEFT_PAREN)) return null;
			ASTNode expr = parseBoolExpr();
			if (!expect(StaticToken.RIGHT_PAREN)) return null;
			return expr;
		}
		else {
			return parseBoolCmpExpr();
		}
	}

	private ASTNode parseBoolCmpExpr() {
		ASTNode fst = parseIntExpr();
		if (!checkHasNext()) return fst;
		Token cmp = stream.peek();
		if (!expect(StaticToken.GREATER, StaticToken.EQUAL, StaticToken.LESSER)) return fst;
		ASTNode snd = parseIntExpr();
		return new BoolCompareNode((StaticTokenImpl) cmp, fst, snd);
	}
}
