package io.github.kaleido;

enum TokenType { NUM, STR, ID, KEYWORD, OP, EOF }

record Token(TokenType type, String text, int pos) {
    boolean is(TokenType t, String s) {
        return type == t && text.equals(s);
    }
}
