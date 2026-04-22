import java.util.*;

public class Tokenizer {

    private final String input;
    private int pos = 0;
    private final List<Token> tokens = new ArrayList<>();

    private static final Set<String> KEYWORDS = Set.of(
        "CREATE", "TABLE", "INSERT", "VALUES", "SELECT", "FROM",
        "WHERE", "DELETE", "EXIT", "PRIMARY", "KEY",
        "INTEGER", "FLOAT", "TEXT", "AND", "OR",
        "UPDATE", "SET", "DESCRIBE", "INPUT", "LET"
    );

    public Tokenizer(String input) {
        this.input = input;
        tokenize();
    }

    public List<Token> getTokens() {
        return tokens;
    }

    private void tokenize() {
        while (pos < input.length()) {
            char c = input.charAt(pos);

            if (Character.isWhitespace(c)) {
                pos++;
            } else if (Character.isLetter(c) || c == '_') {
                readWord();
            } else if (Character.isDigit(c)) {
                readNumber();
            } else if (c == '"') {
                readString();
            } else {
                readSymbol();
            }
        }

        tokens.add(new Token(TokenType.EOF, "EOF"));
    }

    private void readWord() {
        int start = pos;

        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_') pos++;
            else break;
        }

        String word = input.substring(start, pos);
        String upper = word.toUpperCase();

        if (KEYWORDS.contains(upper)) tokens.add(new Token(TokenType.KEYWORD, upper));
        else tokens.add(new Token(TokenType.IDENTIFIER, word));
    }

    private void readNumber() {
        int start = pos;

        while (pos < input.length() &&
               (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
            pos++;
        }

        String num = input.substring(start, pos);

        if (num.contains(".")) tokens.add(new Token(TokenType.FLOAT, num));
        else tokens.add(new Token(TokenType.INTEGER, num));
    }

    private void readString() {
        pos++;
        int start = pos;

        while (pos < input.length() && input.charAt(pos) != '"') {
            pos++;
        }

        if (pos >= input.length()) {
            throw new RuntimeException("Unterminated string literal");
        }

        String val = input.substring(start, pos);
        pos++;

        tokens.add(new Token(TokenType.STRING, val));
    }

    private void readSymbol() {
        char c = input.charAt(pos);

        if ("(),;=<>!*".indexOf(c) >= 0) {
            if ((c == '>' || c == '<' || c == '!') && pos + 1 < input.length() && input.charAt(pos + 1) == '=') {
                tokens.add(new Token(TokenType.SYMBOL, "" + c + '='));
                pos += 2;
            } else {
                tokens.add(new Token(TokenType.SYMBOL, "" + c));
                pos++;
            }
        } else {
            throw new RuntimeException("Invalid char: " + c);
        }
    }
}