import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Tokenizer {
    private final String input;
    private int pos = 0;
    private final List<Token> tokens = new ArrayList<>();

    private static final Set<String> KEYWORDS = Set.of(
        "CREATE", "DATABASE", "TABLE", "USE", "DESCRIBE", "ALL", "SELECT", "FROM", "WHERE",
        "LET", "KEY", "INSERT", "VALUES", "UPDATE", "SET", "DELETE", "RENAME", "INPUT", "OUTPUT",
        "EXIT", "PRIMARY", "INTEGER", "FLOAT", "TEXT", "AND", "OR", "COUNT", "MIN", "MAX",
        "AVERAGE", "AVG"
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
            } else if (Character.isDigit(c) || isNegativeNumberStart()) {
                readNumber();
            } else if (c == '"') {
                readString();
            } else {
                readSymbol();
            }
        }

        tokens.add(new Token(TokenType.EOF, "EOF"));
    }

    private boolean isNegativeNumberStart() {
        return input.charAt(pos) == '-' && pos + 1 < input.length() && Character.isDigit(input.charAt(pos + 1));
    }

    private void readWord() {
        int start = pos;
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '-' || c == '/' || c == '\\') pos++;
            else break;
        }

        String word = input.substring(start, pos);
        String upper = word.toUpperCase();
        if (KEYWORDS.contains(upper)) tokens.add(new Token(TokenType.KEYWORD, upper));
        else tokens.add(new Token(TokenType.IDENTIFIER, word));
    }

    private void readNumber() {
        int start = pos;
        if (input.charAt(pos) == '-') pos++;
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
        if (pos < input.length() && input.charAt(pos) == '.') {
            pos++;
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
        }

        String num = input.substring(start, pos);
        if (num.contains(".")) tokens.add(new Token(TokenType.FLOAT, num));
        else tokens.add(new Token(TokenType.INTEGER, num));
    }

    private void readString() {
        pos++;
        int start = pos;
        while (pos < input.length() && input.charAt(pos) != '"') pos++;
        if (pos >= input.length()) throw new RuntimeException("Unterminated string literal");
        String val = input.substring(start, pos);
        pos++;
        tokens.add(new Token(TokenType.STRING, val));
    }

    private void readSymbol() {
        char c = input.charAt(pos);
        if ((c == '<' || c == '>' || c == '!') && pos + 1 < input.length() && input.charAt(pos + 1) == '=') {
            tokens.add(new Token(TokenType.SYMBOL, "" + c + '='));
            pos += 2;
            return;
        }
        if ("(),;=*<>".indexOf(c) >= 0) {
            tokens.add(new Token(TokenType.SYMBOL, "" + c));
            pos++;
            return;
        }
        throw new RuntimeException("Invalid char: " + c);
    }
}