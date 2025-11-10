package lexer;

public class ErrorToken implements Token {
	LocatedString lexeme;

	ErrorToken(LocatedString lexeme) {
		this.lexeme = lexeme;
	}

	@Override
	public LocatedString getLexeme() {
		return lexeme;
	}
}
