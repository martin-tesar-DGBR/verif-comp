package lexer;

public class StaticTokenImpl implements Token {
	public StaticToken token;
	LocatedString lexeme;

	StaticTokenImpl(StaticToken token, int line, int col) {
		this.token = token;
		this.lexeme = new LocatedString(token.lexeme, line, col);
	}

	@Override
	public LocatedString getLexeme() {
		return lexeme;
	}
}
