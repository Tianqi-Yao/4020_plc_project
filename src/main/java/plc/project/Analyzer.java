// Jeremy DePoyster
// University of Florida
// COP4020 Spring 2021 Online

// Current as of 4/5/2021
// My computer is acting up so this is my most up to date file from
// Previous backup......
// Will resubmit up to date when it fixes.

package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.management.RuntimeErrorException;

import plc.project.Environment.PlcObject;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        try {
            boolean mainPresent = false;
            // convert to forEach, but were acting up, so keep as is for now
            if (!ast.getFields().isEmpty()) {
                for (int i = 0; i < ast.getFields().size(); i++)
                    visit(ast.getFields().get(i));
            }
            
            if (!ast.getMethods().isEmpty()) {
                for (int i = 0; i < ast.getMethods().size(); i++)
                    visit(ast.getMethods().get(i));
                for (int i = 0; i < ast.getMethods().size(); i++) {
                    Ast.Method temp = ast.getMethods().get(i);
                    if (temp.getName().equals("main") && temp.getReturnTypeName().get().equals("Integer") && temp.getParameters().isEmpty()) {
                        mainPresent = true;
                    }
                }
            }
            
            if (!mainPresent) {
                // System.out.println("NO MAIN PRESENT!");
                throw new RuntimeException("No main method with proper arguments!");
            }
        } catch (RuntimeException r) {
            // System.out.println("Ast.Source:");
            // System.out.println(r);
            throw new RuntimeException(r);
        }
        

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        try {
            if (ast.getValue().isPresent()) {
                visit(ast.getValue().get());
                requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
                scope.defineVariable(ast.getName(), ast.getName(), ast.getValue().get().getType(), Environment.NIL);
                ast.setVariable(scope.lookupVariable(ast.getName()));
            }
            else {
                scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), Environment.NIL);
                ast.setVariable(scope.lookupVariable(ast.getName()));
            }
        } catch (RuntimeException r) {
            // System.out.println("Ast.Field:");
            // System.out.println(r);
            throw new RuntimeException(r);
        }
        
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {

        try {
            // Define returnType for Ast.Expr.Return
            Environment.Type returnType;
            if (ast.getReturnTypeName().isPresent())
                returnType = Environment.getType(ast.getReturnTypeName().get());
            else
                returnType = Environment.Type.NIL;
            scope.defineVariable("returnType", "returnType", returnType, Environment.NIL);

            // Rest of Method
            List<String> paramStrings = ast.getParameterTypeNames();
            Environment.Type[] paramTypes = new Environment.Type[paramStrings.size()];
            if (!paramStrings.isEmpty()) {
                for (int i = 0; i < paramStrings.size(); i++)
                    paramTypes[i] = Environment.getType(paramStrings.get(i));
            }

            scope.defineFunction(ast.getName(), ast.getName(), Arrays.asList(paramTypes), returnType, args -> Environment.NIL);

            if (!ast.getStatements().isEmpty()) {
                for (int i = 0; i < ast.getStatements().size(); i++) {
                    try {
                        scope = new Scope(scope);
                        visit(ast.getStatements().get(i));
                    } finally {
                        scope = scope.getParent();
                    }
                }
            }

            ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));
        } catch (RuntimeException r) {
            // System.out.println("Ast.Method:");
            // System.out.println(r);
            throw new RuntimeException(r);
        }
        
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        try {
            if (ast.getExpression().getClass() != Ast.Expr.Function.class) {
                // System.out.println("NOT FUNCTION TYPE!");
                throw new RuntimeException("Not function type!");
            }
        }
        catch (RuntimeException r) {
            // System.out.println("Ast.Expression:");
            // System.out.println(r);
            throw new RuntimeException(r);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        try {
            if (ast.getValue().isPresent()) {
                visit(ast.getValue().get());
                scope.defineVariable(ast.getName(), ast.getName(), ast.getValue().get().getType(), Environment.NIL);
                ast.setVariable(scope.lookupVariable(ast.getName()));
            }
            else {
                scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName().get()), Environment.NIL);
                ast.setVariable(scope.lookupVariable(ast.getName()));
            }
        } catch (RuntimeException r) {
            // System.out.println("Ast.Declaration:");
            // System.out.println(r);
            throw new RuntimeException(r);
        }
        
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        try {
            if (ast.getReceiver().getClass() != Ast.Expr.Access.class) {
                // System.out.println("Not Access!");
                throw new RuntimeException("Not Access!");
    
            }
            visit(ast.getValue());
            visit(ast.getReceiver());
            requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        } catch (RuntimeException r) {
            // System.out.println("Ast.Assignment:");
            // System.out.println(r);
            throw new RuntimeException(r);
        }
        
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        try {
            if (ast.getThenStatements().isEmpty()) {
                // System.out.println("No then statement!");
                throw new RuntimeException("No then statement!");
            }
            visit(ast.getCondition());
            requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
            if (!ast.getElseStatements().isEmpty()) {
                for (int i = 0; i < ast.getElseStatements().size(); i++) {
                    try {
                        scope = new Scope(scope);
                        visit(ast.getElseStatements().get(i));
                    } finally {
                        scope = scope.getParent();
                    }
                }
            }
    
            for (int i = 0; i < ast.getThenStatements().size(); i++) {
                try {
                    scope = new Scope(scope);
                    visit(ast.getThenStatements().get(i));
                } finally {
                    scope = scope.getParent();
                }
            }
        } catch (RuntimeException r) {
            // System.out.println("Ast.If:");
            // System.out.println(r);
            throw new RuntimeException(r);
        }
        
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        try {
            if (ast.getStatements().isEmpty()) {
                // System.out.println("No statements!");
                throw new RuntimeException("No statements!");
            }
            visit(ast.getValue());
            requireAssignable(Environment.Type.INTEGER_ITERABLE, ast.getValue().getType());
            
            ast.getStatements().forEach(elem -> {
                try {
                    scope = new Scope(scope);
                    scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL);
                } finally {
                    scope = scope.getParent();
                }
            });
        } catch (RuntimeException r) {
            // System.out.println("Ast.For:");
            // System.out.println(r);
            throw new RuntimeException(r);
        }
        
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        try {
            visit(ast.getCondition());
            requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        } catch (RuntimeException r) {
            // System.out.println("Ast.While:");
            // System.out.println(r);
            throw new RuntimeException(r);
        }
        
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        try {
            visit(ast.getValue());
        
            // Method needs to store this return type
            Environment.Variable ret = scope.lookupVariable("returnType");
            requireAssignable(ret.getType(), ast.getValue().getType());
        } catch (RuntimeException r) {
            // System.out.println("Ast.Return:");
            // System.out.println(r);
            throw new RuntimeException(r);
        }
        

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        try {
            if (ast.getLiteral() instanceof String)
                ast.setType(Environment.Type.STRING);
            else if (ast.getLiteral() instanceof Character)
                ast.setType(Environment.Type.CHARACTER);
            else if (ast.getLiteral() == Environment.NIL)
                ast.setType(Environment.Type.NIL);
            else if (ast.getLiteral() instanceof Boolean)
                ast.setType(Environment.Type.BOOLEAN);
            else if (ast.getLiteral() instanceof BigInteger) {
                try {
                    BigInteger temp = BigInteger.class.cast(ast.getLiteral());
                    if ((temp.intValueExact() > Integer.MAX_VALUE) || (temp.intValueExact() < Integer.MIN_VALUE))
                        throw new RuntimeException("Int outside range");
                    ast.setType(Environment.Type.INTEGER);
                }
                catch (RuntimeException r) {
                    // System.out.println("Int Out Range");
                    throw new RuntimeException("Integer outside range");
                }
            }
            else if (ast.getLiteral() instanceof BigDecimal) {
                try {
                    BigDecimal temp = BigDecimal.class.cast(ast.getLiteral());
                    if ((temp.doubleValue() > Double.MAX_VALUE) || (temp.doubleValue() < Double.MIN_VALUE))
                        throw new RuntimeException("Decimal outside range");
                    ast.setType(Environment.Type.DECIMAL);
                }
                catch (RuntimeException r) {
                    // System.out.println("Decimal Out Range");
                    throw new RuntimeException("Decimal outside range");
                }
            }
            else {
                // System.out.println("Type doesn't exist!");
                throw new RuntimeException("Type doesn't exist");
            }
        } catch (RuntimeException r) {
            // System.out.println("Ast.Literal:");
            // System.out.println(r);
            throw new RuntimeException(r);
        }
        
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        try {
            visit(ast.getExpression());
            try {
                if (ast.getExpression().getClass() != Ast.Expr.Binary.class) {
                    // System.out.println("NOT BINARY TYPE!");
                    throw new RuntimeException("Not binary type!");
                }
            }
            catch (RuntimeException r) {
                // System.out.println("NOT BINARY TYPE!");
                throw new RuntimeException("Not binary type!");
            }
        } catch (RuntimeException r) {
            // System.out.println("Ast.Group:");
            // System.out.println(r);
            throw new RuntimeException(r);
        }
        
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        try {
            String op = ast.getOperator();
            visit(ast.getLeft());
            visit(ast.getRight());
    
            if (op.equals("AND") || op.equals("OR")) {
                requireAssignable(Environment.Type.BOOLEAN, ast.getLeft().getType());
                requireAssignable(Environment.Type.BOOLEAN, ast.getRight().getType());
                ast.setType(Environment.Type.BOOLEAN);
            }
            else if (op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=") || op.equals("==") || op.equals("!=")) {
                requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
                requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
                ast.setType(Environment.Type.BOOLEAN);
            }
            else if (op.equals("+")) {
                if (ast.getLeft().getType() == Environment.Type.STRING || ast.getRight().getType() == Environment.Type.STRING) {
                    ast.setType(Environment.Type.STRING);
                }
                else if (ast.getLeft().getType() == Environment.Type.INTEGER || ast.getLeft().getType() == Environment.Type.DECIMAL) {
                    if (ast.getLeft().getType() != ast.getRight().getType()) {
                        // System.out.println("NOT RIGHT TYPES FOR +");
                        throw new RuntimeException("Not right types for +");
                    }
                    ast.setType(ast.getLeft().getType());
                }
                else {
                    // System.out.println("NOT RIGHT TYPES FOR +");
                    throw new RuntimeException("Not right types for +");
                }
            }
    
            else if (op.equals("-") || op.equals("*") || op.equals("/")) {
                if (ast.getLeft().getType() == Environment.Type.INTEGER || ast.getLeft().getType() == Environment.Type.DECIMAL) {
                    if (ast.getLeft().getType() != ast.getRight().getType()) {
                        // System.out.println("NOT RIGHT TYPES FOR *, -, /");
                        throw new RuntimeException("Not right types for *, -, /");
                    }
                    ast.setType(ast.getLeft().getType());
                }
                else {
                    // System.out.println("NOT RIGHT TYPES FOR *, -, /");
                    throw new RuntimeException("Not right types for *, -, /");
                }
            }
            else {
                // System.out.println("HMMM HOW'D THIS HAPPEN");
                throw new RuntimeException("Not right types for Binary");
            }
        } catch (RuntimeException r) {
            // System.out.println("Ast.Binary:");
            // System.out.println(r);
            throw new RuntimeException(r);
        }
        

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        // Sorry for the comment I'm just so happy I figured this out, wow, so happy.

        try {
            if (ast.getReceiver().isPresent()) {
                Ast.Expr.Access temp = Ast.Expr.Access.class.cast(ast.getReceiver().get());
                temp.setVariable(scope.lookupVariable(temp.getName()));
                try {
                    scope = scope.lookupVariable(temp.getName()).getType().getScope();
                    ast.setVariable(scope.lookupVariable(ast.getName()));
                } finally {
                    scope = scope.getParent();
                }
            }
            else {
                ast.setVariable(scope.lookupVariable(ast.getName()));
            }
        } catch (RuntimeException r) {
            // System.out.println("Ast.Access:");
            // System.out.println(r);
            throw new RuntimeException(r);
        }
        
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        try {
            if (ast.getReceiver().isPresent()) {
                visit(ast.getReceiver().get());
                Ast.Expr.Access temp = Ast.Expr.Access.class.cast(ast.getReceiver().get());
                List<Environment.Type> params = scope.lookupVariable(temp.getName()).getType().getMethod(ast.getName(), ast.getArguments().size()).getParameterTypes();
                for (int i = 0; i < ast.getArguments().size(); i++) {
                    visit(ast.getArguments().get(i));
                    requireAssignable(params.get(i + 1), ast.getArguments().get(i).getType());
                }
                ast.setFunction(scope.lookupVariable(temp.getName()).getType().getMethod(ast.getName(), ast.getArguments().size()));
            }
            else {
                List<Environment.Type> params = scope.lookupFunction(ast.getName(), ast.getArguments().size()).getParameterTypes();
                for (int i = 0; i < ast.getArguments().size(); i++) {
                    visit(ast.getArguments().get(i));
                    requireAssignable(params.get(i), ast.getArguments().get(i).getType());
                }
                ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));
            }
        } catch (RuntimeException r) {
            // System.out.println("Ast.Function:");
            // System.out.println(r);
            throw new RuntimeException(r);
        }
         
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        try {
            if (target != type && target != Environment.Type.ANY && target != Environment.Type.COMPARABLE) {
                // System.out.println("REQUIRE ASSIGNABLE - target: " + target + " type: " + type);
                throw new RuntimeException("Not matching types!");
            }
        } catch (RuntimeException r) {
            // System.out.println("Require Assignable Triggered.");
            // System.out.println(r);
            throw new RuntimeException(r);
        }
        
    }
}
