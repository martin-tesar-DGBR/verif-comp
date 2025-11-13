package lexer;

public class LabelToken implements Token {
	LocatedString lexeme;

	LabelToken(LocatedString lexeme) {
		this.lexeme = lexeme;
	}

	@Override
	public LocatedString getLexeme() {
		return lexeme;
	}
}
