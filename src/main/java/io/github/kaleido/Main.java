package io.github.kaleido;

import java.nio.file.*;
import java.util.Scanner;

/**
 * Usage:
 *   java -jar kaleido.jar                 interactive REPL
 *   java -jar kaleido.jar file.kal        run a script
 *   java -jar kaleido.jar --ast "1+2*3"   print the syntax tree
 */
public final class Main {
    public static void main(String[] args) throws Exception {
        // deep recursion in the interpreted program needs a big Java stack
        Thread t = new Thread(null, () -> {
            try {
                realMain(args);
            } catch (Exception e) {
                System.err.println("error: " + e.getMessage());
            }
        }, "kaleido", 512L * 1024 * 1024);
        t.start();
        t.join();
    }

    private static void realMain(String[] args) throws Exception {
        Interpreter interp = new Interpreter();
        if (args.length == 2 && args[0].equals("--ast")) {
            interp.parse(args[1]).forEach(System.out::println);
        } else if (args.length == 1) {
            interp.run(Files.readString(Path.of(args[0])));
        } else {
            repl(interp);
        }
    }

    private static void repl(Interpreter interp) {
        System.out.println("Kaleido REPL  -  try: let sq = fn(x) => x * x; sq(12)   (Ctrl+D to quit)");
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.print("kal> ");
            if (!in.hasNextLine()) break;
            String line = in.nextLine().trim();
            if (line.isEmpty()) continue;
            try {
                System.out.println("=> " + Interpreter.show(interp.run(line)));
            } catch (KaleidoException e) {
                System.out.println("error: " + e.getMessage());
            } catch (StackOverflowError so) {
                System.out.println("error: stack overflow (infinite recursion?)");
            }
        }
    }
}
