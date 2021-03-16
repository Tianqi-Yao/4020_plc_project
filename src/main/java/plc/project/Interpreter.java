// Jeremy DePoyster
// University of Florida
// COP4020 Spring 2021 Online

// Current as of 3/15/2021
// Updated Method, Assignment, and Source!

package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.management.RuntimeErrorException;

import plc.project.Environment.PlcObject;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        ast.getFields().forEach(f -> {
            visit(f);
        });
        ast.getMethods().forEach(m -> {
            visit(m);
        });
        List<PlcObject> args = new ArrayList<PlcObject>();
        return scope.lookupFunction("main", 0).invoke(args);
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        if (ast.getValue().isPresent())
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        else
            scope.defineVariable(ast.getName(), Environment.NIL);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        // System.out.println(ast.getName());
        // System.out.println(ast.getParameters());
        // System.out.println(ast.getStatements());

        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            try {
                scope = new Scope(scope);
                ast.getParameters().forEach( p -> {
                    args.forEach(a -> {
                        scope.defineVariable(p, a);
                    });
                });
                ast.getStatements().forEach(s -> {
                    visit(s);
                });
            } 
            catch (Return r) {
                return r.value;
            }
            finally {
                scope = scope.getParent();
            }
            return Environment.NIL;
        });
        

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        if (ast.getValue().isPresent())
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        else
            scope.defineVariable(ast.getName(), Environment.NIL);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        if (ast.getReceiver().getClass() == Ast.Expr.Access.class) {
            try {
                scope = new Scope(scope);
                Ast.Expr.Access temp = Ast.Expr.Access.class.cast(ast.getReceiver());
                if (temp.getReceiver().isPresent()) {
                    PlcObject rec = visit(temp.getReceiver().get());
                    // System.out.println(rec);
                    // System.out.println(temp.getName());
                    // System.out.println(rec.getField(temp.getName()));
                    rec.setField(temp.getName(), visit(ast.getValue()));
                }
                else {
                    // System.out.println(scope.lookupVariable(temp.getName()));
                    // System.out.println(temp);
                    // System.out.println(temp.getName());
                    scope.lookupVariable(temp.getName()).setValue(visit(ast.getValue()));
                }
            } finally {
                scope = scope.getParent();
            }
        }
        else
            throw new RuntimeException("Not Access Type");
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        if (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getThenStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        else if (!requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getElseStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        // System.out.println(ast.getName());
        // System.out.println(ast.getStatements());
        // System.out.println(ast.getValue());

        Iterable iter = requireType(Iterable.class, visit(ast.getValue()));
        iter.forEach( elem -> {
            try {
                scope = new Scope(scope);
                // System.out.println(elem);
                // System.out.println(elem.getClass());
                // System.out.println(PlcObject.class.cast(elem).getValue());
                // System.out.println(PlcObject.class.cast(elem).getValue().getClass()); 
                scope.defineVariable(ast.getName(), PlcObject.class.cast(elem));
                ast.getStatements().forEach( statement -> {
                    visit(statement);
                 });
            } finally {
                scope = scope.getParent();
            }
        });

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if (ast.getLiteral() == null)
            return Environment.NIL;
        else
            return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        String op = ast.getOperator();

        if (op.equals("AND")) {
            if (requireType(Boolean.class, visit(ast.getLeft())) == requireType(Boolean.class, visit(ast.getRight())))
                return visit(ast.getLeft());
            else
                return Environment.create(Boolean.FALSE);
        }
        
        else if (op.equals("OR")) {
            if (requireType(Boolean.class, visit(ast.getLeft())) == Boolean.TRUE)
                return visit(ast.getLeft());
            else if (requireType(Boolean.class, visit(ast.getRight())) == Boolean.TRUE)
                return visit(ast.getRight());
            else
                return Environment.create(Boolean.FALSE);
        }

        else if (op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=")) {
            if (visit(ast.getLeft()).getValue() instanceof Comparable && visit(ast.getLeft()).getValue().getClass() == visit(ast.getRight()).getValue().getClass()) {
                int compare;
                Comparable<Object> left = (Comparable<Object>)visit(ast.getLeft()).getValue();
                Comparable<Object> right = (Comparable<Object>)visit(ast.getRight()).getValue();
                compare = left.compareTo(right);

                switch (op) {
                    case "<":
                        if (compare < 0)
                            return Environment.create(Boolean.TRUE);
                        else
                            return Environment.create(Boolean.FALSE);
                    case "<=":
                        if (compare <= 0)
                            return Environment.create(Boolean.TRUE);
                        else
                            return Environment.create(Boolean.FALSE);
                    case ">":
                        if (compare > 0)
                            return Environment.create(Boolean.TRUE);
                        else
                            return Environment.create(Boolean.FALSE);
                    case ">=":
                        if (compare >= 0)
                            return Environment.create(Boolean.TRUE);
                        else
                            return Environment.create(Boolean.FALSE);
                }
            }
        }

        else if (op.equals("==") || op.equals("!=")) {
            switch (op) {
                case "==":
                    if (visit(ast.getLeft()).getValue().equals(visit(ast.getRight()).getValue()))
                        return Environment.create(Boolean.TRUE);
                    else
                        return Environment.create(Boolean.FALSE);
                case "!=":
                    if (visit(ast.getLeft()).getValue().equals(visit(ast.getRight()).getValue()))
                        return Environment.create(Boolean.FALSE);
                    else
                        return Environment.create(Boolean.TRUE);
            }
        }

        else if (op.equals("+")) {
            // String
            if (visit(ast.getLeft()).getValue().getClass() == String.class || visit(ast.getRight()).getValue().getClass() == String.class)
                return Environment.create(visit(ast.getLeft()).getValue().toString() + visit(ast.getRight()).getValue().toString());
            else if (visit(ast.getLeft()).getValue().getClass() == BigInteger.class && visit(ast.getLeft()).getValue().getClass() == visit(ast.getRight()).getValue().getClass())
                return Environment.create(BigInteger.class.cast(visit(ast.getLeft()).getValue()).add(BigInteger.class.cast(visit(ast.getRight()).getValue())));
            else if (visit(ast.getLeft()).getValue().getClass() == BigDecimal.class && visit(ast.getLeft()).getValue().getClass() == visit(ast.getRight()).getValue().getClass())
                return Environment.create(BigDecimal.class.cast(visit(ast.getLeft()).getValue()).add(BigDecimal.class.cast(visit(ast.getRight()).getValue())));
            else
                throw new RuntimeException("Wrong Concat types");
        }

        else if (op.equals("-") || op.equals("*")) {
            // System.out.println(visit(ast.getLeft()));
            // System.out.println(visit(ast.getRight()));
            // wow that's long
            if ((visit(ast.getLeft()).getValue().getClass() == BigDecimal.class || visit(ast.getLeft()).getValue().getClass() == BigInteger.class) && visit(ast.getLeft()).getValue().getClass() == visit(ast.getRight()).getValue().getClass()) {
                if (visit(ast.getLeft()).getValue().getClass() == BigInteger.class) {
                    if (op.equals("*"))
                        return Environment.create(BigInteger.class.cast(visit(ast.getLeft()).getValue()).multiply(BigInteger.class.cast(visit(ast.getRight()).getValue())));
                    else
                        return Environment.create(BigInteger.class.cast(visit(ast.getLeft()).getValue()).subtract(BigInteger.class.cast(visit(ast.getRight()).getValue())));
                }
                else if (visit(ast.getLeft()).getValue().getClass() == BigDecimal.class && visit(ast.getLeft()).getValue().getClass() == visit(ast.getRight()).getValue().getClass()) {
                    if (op.equals("*"))
                        return Environment.create(BigDecimal.class.cast(visit(ast.getLeft()).getValue()).multiply(BigDecimal.class.cast(visit(ast.getRight()).getValue())));
                    else
                        return Environment.create(BigDecimal.class.cast(visit(ast.getLeft()).getValue()).subtract(BigDecimal.class.cast(visit(ast.getRight()).getValue())));
                }
            }
            else
                throw new RuntimeException("Tried to - or * with wrong type");
        }

        else if (op.equals("/")) {
            if ((visit(ast.getLeft()).getValue().getClass() == BigDecimal.class || visit(ast.getLeft()).getValue().getClass() == BigInteger.class) && visit(ast.getLeft()).getValue().getClass() == visit(ast.getRight()).getValue().getClass()) {
                // Catch Div by 0
                if ((visit(ast.getRight()).getValue().getClass() == BigDecimal.class && visit(ast.getRight()).getValue().equals(BigDecimal.ZERO)) || (visit(ast.getRight()).getValue().getClass() == BigInteger.class && visit(ast.getRight()).getValue().equals(BigInteger.ZERO)))
                    throw new RuntimeException("Div by 0");
                // Divide
                if (visit(ast.getLeft()).getValue().getClass() == BigDecimal.class)
                    return Environment.create(BigDecimal.class.cast(visit(ast.getLeft()).getValue()).divide(BigDecimal.class.cast(visit(ast.getRight()).getValue()), RoundingMode.HALF_EVEN));
                else
                    return Environment.create(BigInteger.class.cast(visit(ast.getLeft()).getValue()).divide(BigInteger.class.cast(visit(ast.getRight()).getValue())));
            }
            else
                throw new RuntimeException("Tried to / with wrong type");
        }

        throw new RuntimeException("Wrong types");
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            PlcObject rec = visit(ast.getReceiver().get()); // Can remove this var later
            return rec.getField(ast.getName()).getValue();
        }
        return scope.lookupVariable(ast.getName()).getValue();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        try {
            scope = new Scope(scope);
            List<Environment.PlcObject> args = new ArrayList<Environment.PlcObject>();
            for (int i = 0; i < ast.getArguments().size(); i++) {
                args.add(visit(ast.getArguments().get(i)));
            }
            if (ast.getReceiver().isPresent()) {
                PlcObject rec = visit(ast.getReceiver().get());
                return rec.callMethod(ast.getName(), args);
            }
            else {
                return scope.lookupFunction(ast.getName(), args.size()).invoke(args);
            }
        } finally {
            scope = scope.getParent();
        }
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
