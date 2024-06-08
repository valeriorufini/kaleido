package io.github.kaleido;

import io.github.kaleido.Expr.*;

import java.util.*;

/**
 * Pratt parser (a.k.a. top-down operator precedence).
 * Each infix operator has a binding power; {@code expr(minBp)} keeps consuming operators
 * as long as they bind at least as tightly as {@code minBp}.
 */
final class Parser {
    private static final int UNARY_BP = 70;
    private static final int CALL_BP = 90;

    private final List<Token> tokens;
    private int pos;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    static List<Expr> parse(String source) {
        return new Parser(Lexer.lex(source)).program();
    }

    List<Expr> program() {
        List<Expr> out = new ArrayList<>();
        while (peek().type() != TokenType.EOF) {
            if (matchOp(";")) continue;
            out.add(statement());
        }
        return out;
    }

    private Expr statement() {
        if (matchKeyword("let")) {
            String name = expect(TokenType.ID).text();
            expectOp("=");
            return new Let(name, expr(0));
        }
        // sugar: `fn name(a, b) => ...` is `let name = fn(a, b) => ...`
        if (peek().is(TokenType.KEYWORD, "fn") && tokens.get(pos + 1).type() == TokenType.ID) {
            pos++;
            String name = advance().text();
            return new Let(name, lambdaRest());
        }
        return expr(0);
    }

    private Expr expr(int minBp) {
        Expr left = prefix();
        while (true) {
            Token t = peek();
            if (t.type() != TokenType.OP) break;
            if (t.text().equals("(")) {                               // call: highest precedence
                if (CALL_BP < minBp) break;
                advance();
                List<Expr> args = new ArrayList<>();
                if (!matchOp(")")) {
                    do { args.add(expr(0)); } while (matchOp(","));
                    expectOp(")");
                }
                left = new Call(left, args);
                continue;
            }
            int bp = infixBp(t.text());
            if (bp < 0 || bp < minBp) break;
            advance();
            boolean rightAssoc = t.text().equals("^");
            Expr right = expr(rightAssoc ? bp : bp + 1);
            left = new Binary(t.text(), left, right);
        }
        return left;
    }

    private Expr prefix() {
        Token t = advance();
        switch (t.type()) {
            case NUM: {
                try {
                    return new Num(Double.parseDouble(t.text()));
                } catch (NumberFormatException e) {
                    throw new KaleidoException("Bad number '" + t.text() + "'");
                }
            }
            case STR: return new Str(t.text());
            case ID: return new Var(t.text());
            case KEYWORD:
                switch (t.text()) {
                    case "true": return new Bool(true);
                    case "false": return new Bool(false);
                    case "if": {
                        Expr cond = expr(0);
                        expectKeyword("then");
                        Expr then = expr(0);
                        expectKeyword("else");
                        return new If(cond, then, expr(0));
                    }
                    case "fn": return lambdaRest();
                    default: break;
                }
                break;
            case OP:
                if (t.text().equals("(")) {
                    Expr inner = expr(0);
                    expectOp(")");
                    return inner;
                }
                if (t.text().equals("-") || t.text().equals("!")) {
                    return new Unary(t.text(), expr(UNARY_BP));
                }
                break;
            default: break;
        }
        throw new KaleidoException("Unexpected '" + t.text() + "' at position " + t.pos());
    }

    /** Parses `(a, b) => body` after the `fn` keyword (and optional name) was consumed. */
    private Expr lambdaRest() {
        expectOp("(");
        List<String> params = new ArrayList<>();
        if (!matchOp(")")) {
            do { params.add(expect(TokenType.ID).text()); } while (matchOp(","));
            expectOp(")");
        }
        expectOp("=>");
        return new Lambda(params, expr(0));
    }

    private static int infixBp(String op) {
        return switch (op) {
            case "||" -> 10;
            case "&&" -> 20;
            case "==", "!=" -> 30;
            case "<", ">", "<=", ">=" -> 40;
            case "+", "-" -> 50;
            case "*", "/", "%" -> 60;
            case "^" -> 80;
            default -> -1;
        };
    }

    private Token peek() { return tokens.get(pos); }
    private Token advance() { return tokens.get(pos++); }

    private boolean matchOp(String s) {
        if (peek().is(TokenType.OP, s)) { pos++; return true; }
        return false;
    }

    private boolean matchKeyword(String s) {
        if (peek().is(TokenType.KEYWORD, s)) { pos++; return true; }
        return false;
    }

    private void expectOp(String s) {
        if (!matchOp(s)) throw new KaleidoException("Expected '" + s + "' but found '" + peek().text() + "' at " + peek().pos());
    }

    private void expectKeyword(String s) {
        if (!matchKeyword(s)) throw new KaleidoException("Expected '" + s + "' but found '" + peek().text() + "' at " + peek().pos());
    }

    private Token expect(TokenType type) {
        Token t = advance();
        if (t.type() != type) throw new KaleidoException("Expected " + type + " but found '" + t.text() + "' at " + t.pos());
        return t;
    }
}
