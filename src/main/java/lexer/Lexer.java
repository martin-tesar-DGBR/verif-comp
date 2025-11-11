package lexer;

import lexer.StaticToken.TokenStateTree;
import logging.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Lexer implements Iterator<Token> {

	static class PushbackCharStream implements Iterator<LocatedChar> {
		BufferedReader fs;
		String currLine;
		int currLineNum;
		int currCharNum;
		Stack<LocatedChar> stack;

		PushbackCharStream(Path file) throws IOException {
			this.fs = Files.newBufferedReader(file);
			this.stack = new Stack<>();
			this.currLineNum = 0;
			preloadLine();
		}

		void pushback(LocatedChar c) {
			this.stack.push(c);
		}

		@Override
		public boolean hasNext() {
			return !(this.stack.isEmpty() && this.currLine == null);
		}

		@Override
		public LocatedChar next() {
			if (!this.stack.isEmpty()) {
				return stack.pop();
			}
			LocatedChar c = new LocatedChar(this.currLine.charAt(this.currCharNum), this.currLineNum, this.currCharNum);
			this.currCharNum += 1;
			if (this.currCharNum >= this.currLine.length()) {
				preloadLine();
			}

			return c;
		}

		private void preloadLine() {
			try {
				do {
					this.currLine = this.fs.readLine();
					this.currLineNum += 1;
					this.currCharNum = 0;
				} while (this.currLine != null && this.currLine.length() == 0);
			} catch (IOException e) {
				throw new RuntimeException("Lexing error");
			}
		}
	}

	PushbackCharStream stream;
	Token peek;

	Logger logger;

	public static Lexer make(String filename) throws IOException {
		return Lexer.make(Path.of(filename));
	}

	public static Lexer make(Path file) throws IOException {
		return new Lexer(new PushbackCharStream(file));
	}

	public Token peek() {
		if (this.peek == null) {
			this.peek = readNextToken();
		}
		return this.peek;
	}

	@Override
	public boolean hasNext() {
		return peek != null || stream.hasNext();
	}

	@Override
	public Token next() {
		Token ret;
		if (this.peek != null) {
			ret = this.peek;
			this.peek = null;
		}
		else {
			ret = readNextToken();
		}
		return ret;
	}

	private Lexer(PushbackCharStream stream) {
		this.stream = stream;
		this.peek = null;
		this.logger = Logger.get(LogType.LEXER);
	}

	private Token readNextToken() {
		Token ret;
		LocatedChar fst = stream.next();
		int line = fst.line, col = fst.col;
		stream.pushback(fst);
		if ((ret = scanStaticToken(line, col)) != null);
		else if ((ret = scanIntToken(line, col)) != null);
		else if ((ret = scanLabelToken(line, col)) != null);
		else {
			// error: cannot lex
			ret = scanError(line, col);
		}
		skipWhitespace();
		return ret;
	}

	private void skipWhitespace() {
		while (stream.hasNext()) {
			LocatedChar nxt = stream.next();
			if (!Character.isWhitespace(nxt.c)) {
				stream.pushback(nxt);
				break;
			}
		}
	}

	private Token scanStaticToken(int line, int col) {
		TokenStateTree currState = StaticToken.tokenTree;
		Stack<LocatedChar> s = new Stack<>();
		StaticToken token = null;
		while (stream.hasNext()) {
			LocatedChar nxt = stream.next();
			if (currState.nextStates.containsKey(nxt.c)) {
				s.push(nxt);
				currState = currState.nextStates.get(nxt.c);
				token = currState.terminal;
			}
			else {
				stream.pushback(nxt);
				token = currState.terminal;
				break;
			}
		}
		if (token == null) {
			while (!s.isEmpty()) {
				stream.pushback(s.pop());
			}
			return null;
		}
		return new StaticTokenImpl(token, line, col);
	}

	private Token scanIntToken(int line, int col) {
		StringBuilder builder = new StringBuilder();
		Stack<LocatedChar> s = new Stack<>();
		while (stream.hasNext()) {
			LocatedChar nxt = stream.next();
			if (Character.isDigit(nxt.c)) {
				s.push(nxt);
				builder.append(nxt.c);
			}
			else {
				stream.pushback(nxt);
				break;
			}
		}
		if (builder.length() == 0) {
			while (!s.isEmpty()) {
				stream.pushback(s.pop());
			}
			return null;
		}
		return new IntToken(new LocatedString(builder.toString(), line, col));
	}

	private Token scanLabelToken(int line, int col) {
		StringBuilder builder = new StringBuilder();
		Stack<LocatedChar> s = new Stack<>();
		assert stream.hasNext();
		{
			LocatedChar fst = stream.next();
			if (Character.isLetter(fst.c)) {
				s.push(fst);
				builder.append(fst.c);
			}
			else {
				stream.pushback(fst);
				return null;
			}
		}
		while (stream.hasNext()) {
			LocatedChar nxt = stream.next();
			if (Character.isLetterOrDigit(nxt.c)) {
				s.push(nxt);
				builder.append(nxt.c);
			}
			else {
				stream.pushback(nxt);
				break;
			}
		}
		if (builder.length() == 0) {
			while (!s.isEmpty()) {
				stream.pushback(s.pop());
			}
			return null;
		}
		return new LabelToken(new LocatedString(builder.toString(), line, col));
	}

	private Token scanError(int line, int col) {
		// scan until the next whitespace character
		StringBuilder builder = new StringBuilder();
		while (stream.hasNext()) {
			LocatedChar nxt = stream.next();
			if (Character.isWhitespace(nxt.c)) {
				stream.pushback(nxt);
				break;
			}
			else {
				builder.append(nxt.c);
			}
		}
		this.logger.log(LogLevel.SEVERE, "Invalid token at line " + line + ", column " + col);
		return new ErrorToken(new LocatedString(builder.toString(), line, col));
	}
}
