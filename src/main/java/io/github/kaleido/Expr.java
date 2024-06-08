package io.github.kaleido;

import java.util.List;

/** The AST. Everything is an expression, even `let` and `if`. */
sealed interface Expr {
    record Num(double value) implements Expr {}
    record Str(String value) implements Expr {}
    record Bool(boolean value) implements Expr {}
    record Var(String name) implements Expr {}
    record Let(String name, Expr value) implements Expr {}
    record Unary(String op, Expr operand) implements Expr {}
    record Binary(String op, Expr left, Expr right) implements Expr {}
    record If(Expr cond, Expr then, Expr otherwise) implements Expr {}
    record Lambda(List<String> params, Expr body) implements Expr {}
    record Call(Expr callee, List<Expr> args) implements Expr {}
}
