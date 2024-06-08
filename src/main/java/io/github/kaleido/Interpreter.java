package io.github.kaleido;

import io.github.kaleido.Expr.*;

import java.util.*;
import java.util.function.Function;

/** Tree-walking evaluator with lexical scoping and first-class closures. */
public final class Interpreter {

    /** A callable value: either a user closure or a native builtin. arity -1 means variadic. */
    record Fn(String name, int arity, Function<List<Object>, Object> body) {
        @Override public String toString() { return "<fn " + name + "/" + (arity < 0 ? "*" : arity) + ">"; }
    }

    private static final class Env {
        private final Map<String, Object> vars = new HashMap<>();
        private final Env parent;
        Env(Env parent) { this.parent = parent; }

        Object get(String name) {
            for (Env e = this; e != null; e = e.parent) {
                if (e.vars.containsKey(name)) return e.vars.get(name);
            }
            throw new KaleidoException("Undefined variable '" + name + "'");
        }
        void define(String name, Object value) { vars.put(name, value); }
    }

    private final Env globals = new Env(null);

    public Interpreter() {
        globals.define("pi", Math.PI);
        globals.define("e", Math.E);
        builtin("sqrt", 1, a -> Math.sqrt(num(a.get(0))));
        builtin("sin", 1, a -> Math.sin(num(a.get(0))));
        builtin("cos", 1, a -> Math.cos(num(a.get(0))));
        builtin("abs", 1, a -> Math.abs(num(a.get(0))));
        builtin("floor", 1, a -> Math.floor(num(a.get(0))));
        builtin("max", 2, a -> Math.max(num(a.get(0)), num(a.get(1))));
        builtin("min", 2, a -> Math.min(num(a.get(0)), num(a.get(1))));
        builtin("str", 1, a -> show(a.get(0)));
        builtin("len", 1, a -> (double) ((String) a.get(0)).length());
        builtin("print", -1, a -> {
            StringJoiner sj = new StringJoiner(" ");
            a.forEach(x -> sj.add(show(x)));
            System.out.println(sj);
            return a.isEmpty() ? "" : a.get(a.size() - 1);
        });
    }

    private void builtin(String name, int arity, Function<List<Object>, Object> body) {
        globals.define(name, new Fn(name, arity, body));
    }

    /** Evaluates a whole program and returns the value of the last statement. */
    public Object run(String source) {
        Object last = "";
        for (Expr e : Parser.parse(source)) {
            last = eval(e, globals);
        }
        return last;
    }

    public List<Expr> parse(String source) {
        return Parser.parse(source);
    }

    private Object eval(Expr expr, Env env) {
        return switch (expr) {
            case Num n -> n.value();
            case Str s -> s.value();
            case Bool b -> b.value();
            case Var v -> env.get(v.name());
            case Let l -> {
                Object value = eval(l.value(), env);
                env.define(l.name(), value);              // defined AFTER evaluation, but closures see the env => recursion works
                yield value;
            }
            case Unary u -> {
                Object v = eval(u.operand(), env);
                yield u.op().equals("-") ? (Object) (-num(v)) : (Object) !truthy(v);
            }
            case Binary b -> binary(b, env);
            case If i -> truthy(eval(i.cond(), env)) ? eval(i.then(), env) : eval(i.otherwise(), env);
            case Lambda l -> closure(l, env);
            case Call c -> {
                Object callee = eval(c.callee(), env);
                if (!(callee instanceof Fn fn)) throw new KaleidoException("Not a function: " + show(callee));
                List<Object> args = new ArrayList<>();
                for (Expr a : c.args()) args.add(eval(a, env));
                if (fn.arity() >= 0 && fn.arity() != args.size()) {
                    throw new KaleidoException(fn.name() + " expects " + fn.arity() + " argument(s), got " + args.size());
                }
                yield fn.body().apply(args);
            }
        };
    }

    private Fn closure(Lambda l, Env captured) {
        return new Fn("lambda", l.params().size(), args -> {
            Env local = new Env(captured);
            for (int i = 0; i < args.size(); i++) local.define(l.params().get(i), args.get(i));
            return eval(l.body(), local);
        });
    }

    private Object binary(Binary b, Env env) {
        String op = b.op();
        if (op.equals("&&")) {                              // short-circuit
            return truthy(eval(b.left(), env)) && truthy(eval(b.right(), env));
        }
        if (op.equals("||")) {
            return truthy(eval(b.left(), env)) || truthy(eval(b.right(), env));
        }
        Object l = eval(b.left(), env);
        Object r = eval(b.right(), env);
        return switch (op) {
            case "+" -> (l instanceof String || r instanceof String) ? show(l) + show(r) : (Object) (num(l) + num(r));
            case "-" -> num(l) - num(r);
            case "*" -> num(l) * num(r);
            case "/" -> {
                if (num(r) == 0) throw new KaleidoException("Division by zero");
                yield num(l) / num(r);
            }
            case "%" -> num(l) % num(r);
            case "^" -> Math.pow(num(l), num(r));
            case "<" -> num(l) < num(r);
            case ">" -> num(l) > num(r);
            case "<=" -> num(l) <= num(r);
            case ">=" -> num(l) >= num(r);
            case "==" -> Objects.equals(l, r);
            case "!=" -> !Objects.equals(l, r);
            default -> throw new KaleidoException("Unknown operator " + op);
        };
    }

    private static double num(Object v) {
        if (v instanceof Double d) return d;
        throw new KaleidoException("Expected a number but got " + show(v));
    }

    private static boolean truthy(Object v) {
        return !(v instanceof Boolean b) || b;
    }

    public static String show(Object v) {
        if (v instanceof Double d) {
            if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) return String.valueOf(d.longValue());
            return d.toString();
        }
        return String.valueOf(v);
    }
}
