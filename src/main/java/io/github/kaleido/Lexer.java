package io.github.kaleido;

import java.util.*;

final class Lexer {
    private static final Set<String> KEYWORDS = Set.of("let", "fn", "if", "then", "else", "true", "false");
    private static final List<String> TWO_CHAR_OPS = List.of("==", "!=", "<=", ">=", "&&", "||", "=>");
    private static final String ONE_CHAR_OPS = "+-*/%^<>!=(),;";

    private Lexer() {}

    static List<Token> lex(String src) {
        List<Token> out = new ArrayList<>();
        int i = 0, n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '#') {                                   // comment until end of line
                while (i < n && src.charAt(i) != '\n') i++;
            } else if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(src.charAt(i + 1)))) {
                int start = i;
                while (i < n && (Character.isDigit(src.charAt(i)) || src.charAt(i) == '.')) i++;
                out.add(new Token(TokenType.NUM, src.substring(start, i), start));
            } else if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(src.charAt(i)) || src.charAt(i) == '_')) i++;
                String word = src.substring(start, i);
                out.add(new Token(KEYWORDS.contains(word) ? TokenType.KEYWORD : TokenType.ID, word, start));
            } else if (c == '"') {
                int start = i++;
                StringBuilder sb = new StringBuilder();
                while (i < n && src.charAt(i) != '"') {
                    char ch = src.charAt(i++);
                    if (ch == '\\' && i < n) {
                        char esc = src.charAt(i++);
                        sb.append(switch (esc) { case 'n' -> '\n'; case 't' -> '\t'; default -> esc; });
                    } else {
                        sb.append(ch);
                    }
                }
                if (i >= n) throw new KaleidoException("Unterminated string starting at " + start);
                i++; // closing quote
                out.add(new Token(TokenType.STR, sb.toString(), start));
            } else {
                String two = i + 1 < n ? src.substring(i, i + 2) : "";
                if (TWO_CHAR_OPS.contains(two)) {
                    out.add(new Token(TokenType.OP, two, i));
                    i += 2;
                } else if (ONE_CHAR_OPS.indexOf(c) >= 0) {
                    out.add(new Token(TokenType.OP, String.valueOf(c), i));
                    i++;
                } else {
                    throw new KaleidoException("Unexpected character '" + c + "' at " + i);
                }
            }
        }
        out.add(new Token(TokenType.EOF, "<eof>", n));
        return out;
    }
}
