# Kaleido

A small **expression-oriented programming language** with a hand-written **Pratt parser**,
a sealed-interface **AST** (Java 21 records + pattern-matching `switch`) and a tree-walking interpreter
with **lexical closures**. ~450 lines, zero dependencies.

```text
fn fact(n) => if n <= 1 then 1 else n * fact(n - 1)
let compose = fn(f, g) => fn(x) => f(g(x))
print(fact(10), compose(sqrt, abs)(-16))
```

## Language tour
| Feature | Example |
|---|---|
| numbers, strings, booleans | `42`, `3.14`, `"hi"`, `true` |
| operators (with precedence) | `+ - * / % ^ < > <= >= == != && \|\| !` |
| everything is an expression | `if x > 0 then "pos" else "neg"` |
| variables | `let x = 10` |
| first-class functions & closures | `fn(x) => x + 1`, `fn name(a, b) => a * b` |
| recursion | `fn fib(n) => if n < 2 then n else fib(n-1) + fib(n-2)` |
| builtins | `print sqrt sin cos abs floor min max str len pi e` |

## Run
```bash
mvn -q package
java -jar target/kaleido-1.0.0.jar examples/showcase.kal     # run a script
java -jar target/kaleido-1.0.0.jar                           # REPL
java -jar target/kaleido-1.0.0.jar --ast "1 + 2 * 3 ^ 2"     # show the syntax tree
# without Maven:
javac -d out src/main/java/io/github/kaleido/*.java && java -cp out io.github.kaleido.Main examples/showcase.kal
```

## How it works
`Lexer` → `Parser` (Pratt: each operator has a binding power, `expr(minBp)` loops while operators bind tighter)
→ `Expr` AST (sealed interface) → `Interpreter` (exhaustive `switch` over the AST, environments chained for scoping).

## Ideas to extend it
Lists and maps, `while`/tail-call optimisation, a bytecode compiler + stack VM, better error messages with line/column.
