/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.core.control.calculators;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreted function/script block for the web and headless engines. Evaluates the
 * common subset of classic Java-block (typ 61) scripts without a Java compiler.
 *
 * <p>Supported:
 * <ul>
 *   <li>Inputs: {@code xIN[0..N]}, {@code u1..uN}, {@code in[0..N]}</li>
 *   <li>Outputs: {@code yOUT[0..M]}, {@code y[0..M]}, or a bare expression as the
 *       script's single statement (formula mode writes {@code yOUT[0]})</li>
 *   <li>Time: {@code t}, {@code time}, {@code dt}, {@code deltaT}; constants {@code PI}, {@code E}</li>
 *   <li>Math functions: {@code sin}, {@code cos}, {@code tan}, {@code asin}, {@code acos},
 *       {@code atan}, {@code atan2}, {@code sinh}, {@code cosh}, {@code tanh}, {@code sqrt},
 *       {@code cbrt}, {@code abs}, {@code exp}, {@code log}, {@code log10}, {@code pow},
 *       {@code min}, {@code max}, {@code floor}, {@code ceil}, {@code round}, {@code signum}</li>
 *   <li>Conditionals: {@code if (cond) { ... } else { ... }} and ternary {@code ? :}</li>
 *   <li>Persistent state variables across simulation steps</li>
 *   <li>Classic Java block normalization: {@code Math.} prefixes, type declarations
 *       ({@code double x = ...; double buf[] = new double[N];}), and {@code return;}
 *       statements are tolerated</li>
 * </ul>
 *
 * <p>Deliberate deviations from real Java semantics (this is an interpreter shim,
 * not a Java compiler):
 * <ul>
 *   <li>{@code ==} / {@code !=} compare numerically with a 1e-12 tolerance</li>
 *   <li>Division and modulo by zero yield 0 instead of Infinity/NaN and log a
 *       warning once per simulation run (see {@code hasWarnedDivideByZero()})</li>
 *   <li>No loops, no method definitions; user array declarations collapse to scalars
 *       (element indexing works only for xIN/yOUT)</li>
 *   <li>Unknown function names evaluate to 0</li>
 * </ul>
 *
 * <p>Code that cannot be compiled keeps the block's outputs at their initial value;
 * the error is logged once and available via {@link #getCompileError()}.
 */
public class ScriptBlockCalculator extends AbstractControlCalculatable implements InitializableAtSimulationStart {

    private static final Logger LOGGER = LogManager.getLogger(ScriptBlockCalculator.class);

    private final String rawSourceCode;
    private final String initCode;
    private final String variablesCode;
    private final int numInputs;
    private final int numOutputs;

    private List<Statement> compiledStatements = new ArrayList<>();
    private List<Statement> compiledInitStatements = new ArrayList<>();
    private final Map<String, Double> stateVariables = new HashMap<>();
    private boolean hasLoggedError = false;
    private boolean hasWarnedDivideByZero = false;
    private String compileError = null;

    public ScriptBlockCalculator(int numInputs, int numOutputs, String sourceCode) {
        this(numInputs, numOutputs, sourceCode, "", "");
    }

    public ScriptBlockCalculator(int numInputs, int numOutputs, String sourceCode, String initCode, String variablesCode) {
        super(Math.max(0, numInputs), Math.max(1, numOutputs));
        this.numInputs = Math.max(0, numInputs);
        this.numOutputs = Math.max(1, numOutputs);
        this.rawSourceCode = sourceCode != null ? sourceCode : "";
        this.initCode = initCode != null ? initCode : "";
        this.variablesCode = variablesCode != null ? variablesCode : "";

        compileAll();
    }

    private void compileAll() {
        try {
            // Compile variable initializations and static code
            String combinedInit = normalizeCode(variablesCode + "\n" + initCode);
            if (!combinedInit.isBlank()) {
                compiledInitStatements = parseStatements(combinedInit);
            }

            // Compile step execution code
            String normalizedSource = normalizeCode(rawSourceCode);
            if (!normalizedSource.isBlank()) {
                compiledStatements = parseStatements(normalizedSource);
            }
        } catch (Exception e) {
            compileError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            LOGGER.error("Failed to compile script block, outputs stay at their initial value: {} -- source: {}",
                    e.getMessage(), abbreviateSource(rawSourceCode));
        }
    }

    /** True when the script compiled without errors. */
    public boolean isCompiled() {
        return compileError == null;
    }

    /** Compile error message, or null when the script compiled. */
    public String getCompileError() {
        return compileError;
    }

    private static String abbreviateSource(String source) {
        if (source == null) {
            return "";
        }
        String flat = source.replaceAll("\\s+", " ").trim();
        return flat.length() > 200 ? flat.substring(0, 200) + "..." : flat;
    }

    @Override
    public void initializeAtSimulationStart(double deltaT) {
        stateVariables.clear();
        hasLoggedError = false;
        hasWarnedDivideByZero = false;

        ExecutionContext ctx = new ExecutionContext(_time, deltaT);
        executeStatements(compiledInitStatements, ctx);
        // Persist variables initialized in init block
        stateVariables.putAll(ctx.variables);
    }

    /**
     * Logs a divide-by-zero warning once per simulation run (it would otherwise
     * fire at every step and flood the log). The result of the operation is
     * still forced to 0.
     */
    void warnDivideByZero() {
        if (!hasWarnedDivideByZero) {
            hasWarnedDivideByZero = true;
            LOGGER.warn("Script block evaluated a division or modulo by zero (result forced to 0) -- source: {}",
                    abbreviateSource(rawSourceCode));
        }
    }

    /** True when a division or modulo by zero was evaluated since simulation start. */
    public boolean hasWarnedDivideByZero() {
        return hasWarnedDivideByZero;
    }

    @Override
    public void calculateYOUT(double deltaT) {
        ExecutionContext ctx = new ExecutionContext(_time, deltaT);
        // Load persistent state
        ctx.variables.putAll(stateVariables);

        try {
            executeStatements(compiledStatements, ctx);

            // Copy results to output signals
            for (int i = 0; i < numOutputs; i++) {
                _outputSignal[i][0] = ctx.outputs[i];
            }

            // Update persistent variables (excluding inputs/outputs/time)
            for (Map.Entry<String, Double> entry : ctx.variables.entrySet()) {
                String key = entry.getKey();
                if (!isReservedKeyword(key)) {
                    stateVariables.put(key, entry.getValue());
                }
            }
        } catch (Exception e) {
            if (!hasLoggedError) {
                LOGGER.error("Error executing script block: {}", e.getMessage(), e);
                hasLoggedError = true;
            }
        }
    }

    public int getNumInputs() {
        return numInputs;
    }

    public int getNumOutputs() {
        return numOutputs;
    }

    public String getRawSourceCode() {
        return rawSourceCode;
    }

    // =========================================================================
    // Code Normalization for Classic Java Block Compatibility
    // =========================================================================

    public static String normalizeCode(String code) {
        if (code == null) {
            return "";
        }

        // Remove comments
        String cleaned = code.replaceAll("//.*", "");
        cleaned = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL).matcher(cleaned).replaceAll("");

        // Normalize Math.method calls to direct method calls
        cleaned = cleaned.replaceAll("\\bMath\\.", "");

        // Remove Java type keywords: double, int, float, final, etc.
        cleaned = cleaned.replaceAll("\\b(double|int|float|long|boolean|final)\\s*\\[\\s*\\]\\s*\\[\\s*\\]", "");
        cleaned = cleaned.replaceAll("\\b(double|int|float|long|boolean|final)\\s*\\[\\s*\\]", "");
        cleaned = cleaned.replaceAll("\\b(double|int|float|long|boolean|final)\\s+", "");
        cleaned = cleaned.replaceAll("(\\[\\s*\\]\\s*)+([a-zA-Z_])", "$2");

        // Desugar compound assignments: `x += y;` -> `x = x + (y);`, etc.
        cleaned = cleaned.replaceAll("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\+=\\s*([^;]+);", "$1 = $1 + ($2);");
        cleaned = cleaned.replaceAll("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*-=\\s*([^;]+);", "$1 = $1 - ($2);");
        cleaned = cleaned.replaceAll("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\*=\\s*([^;]+);", "$1 = $1 * ($2);");
        cleaned = cleaned.replaceAll("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*/=\\s*([^;]+);", "$1 = $1 / ($2);");

        // Neutralize `new <type>[...]` array allocations. Initializer expressions
        // become 0 (`double buf[] = new double[4];` -> `double buf[] = 0;`) and
        // standalone allocation statements vanish (`new double[4];` -> `;`).
        cleaned = cleaned.replaceAll("=\\s*new\\s+[a-zA-Z0-9_]+\\s*(?:\\s*\\[[^\\]]*\\])+", "= 0");
        cleaned = cleaned.replaceAll("\\bnew\\s+[a-zA-Z0-9_]+\\s*(?:\\s*\\[[^\\]]*\\])+\\s*;", ";");

        // Drop empty `[]` declarator suffixes left over from array declarations
        // (`double buf[] = 0;` -> `buf = 0;`, including `m[][]`) so the statement
        // parses as an assignment
        cleaned = cleaned.replaceAll("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?:\\[\\s*\\]\\s*)+(?==)", "$1 ");

        // Normalize array literal assignments like "= {{...}};" or "= {...};" to "= 0;"
        cleaned = cleaned.replaceAll("(?s)=\\s*\\{.*?\\}\\s*;", "= 0;");

        // Replace top-level commas (outside parentheses, brackets, braces) with semicolons
        StringBuilder splitComma = new StringBuilder();
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        for (int i = 0; i < cleaned.length(); i++) {
            char ch = cleaned.charAt(i);
            if (ch == '(') parenDepth++;
            else if (ch == ')') parenDepth = Math.max(0, parenDepth - 1);
            else if (ch == '[') bracketDepth++;
            else if (ch == ']') bracketDepth = Math.max(0, bracketDepth - 1);
            else if (ch == '{') braceDepth++;
            else if (ch == '}') braceDepth = Math.max(0, braceDepth - 1);
            else if (ch == ',' && parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                splitComma.append(';');
                continue;
            }
            splitComma.append(ch);
        }
        cleaned = splitComma.toString();

        // Remove return statements like "return yOUT;" or "return;"
        cleaned = cleaned.replaceAll("\\breturn\\s+[^;]*;", "");
        cleaned = cleaned.replaceAll("\\breturn\\s*;", "");

        return cleaned.trim();
    }

    /**
     * Names that can never be persistent user variables because reads resolve to
     * time, constants, inputs, or outputs. Matched exactly (modulo case) so user
     * variables like {@code integral}, {@code upper}, or {@code yRef} keep their state.
     */
    private static boolean isReservedKeyword(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.equals("t") || lower.equals("time") || lower.equals("dt")
                || lower.equals("deltat") || lower.equals("pi") || lower.equals("e")
                || lower.equals("xin") || lower.equals("in") || lower.equals("yout")
                || lower.equals("y")) {
            return true;
        }
        // u<N> aliases resolve to inputs, so they can never be variables either
        if (lower.length() > 1 && lower.charAt(0) == 'u') {
            for (int i = 1; i < lower.length(); i++) {
                if (!Character.isDigit(lower.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    // =========================================================================
    // Execution Context
    // =========================================================================

    private class ExecutionContext {
        final double time;
        final double dt;
        final double[] inputs;
        final double[] outputs;
        final Map<String, Double> variables = new HashMap<>();

        ExecutionContext(double time, double dt) {
            this.time = time;
            this.dt = dt;
            this.inputs = new double[numInputs];
            for (int i = 0; i < numInputs; i++) {
                if (_inputSignal != null && i < _inputSignal.length && _inputSignal[i] != null) {
                    this.inputs[i] = _inputSignal[i][0];
                }
            }
            this.outputs = new double[numOutputs];
            // Initialize outputs from existing signals
            for (int i = 0; i < numOutputs; i++) {
                if (_outputSignal != null && i < _outputSignal.length && _outputSignal[i] != null) {
                    this.outputs[i] = _outputSignal[i][0];
                }
            }
        }

        double getVariable(String name) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.equals("t") || lower.equals("time")) {
                return time;
            }
            if (lower.equals("dt") || lower.equals("deltat")) {
                return dt;
            }
            if (lower.equals("pi")) {
                return Math.PI;
            }
            if (lower.equals("e")) {
                return Math.E;
            }
            // Check inputs: u1, u2...
            if (name.length() > 1 && (name.charAt(0) == 'u' || name.charAt(0) == 'U')) {
                try {
                    int idx = Integer.parseInt(name.substring(1)) - 1;
                    if (idx >= 0 && idx < inputs.length) {
                        return inputs[idx];
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return variables.getOrDefault(name, 0.0);
        }

        void setVariable(String name, double val) {
            variables.put(name, val);
        }

        double getInput(int index) {
            return (index >= 0 && index < inputs.length) ? inputs[index] : 0.0;
        }

        double getOutput(int index) {
            return (index >= 0 && index < outputs.length) ? outputs[index] : 0.0;
        }

        void setOutput(int index, double val) {
            if (index >= 0 && index < outputs.length) {
                outputs[index] = val;
            }
        }

        void warnDivideByZero() {
            ScriptBlockCalculator.this.warnDivideByZero();
        }
    }

    private void executeStatements(List<Statement> statements, ExecutionContext ctx) {
        for (Statement stmt : statements) {
            stmt.execute(ctx);
        }
    }

    // =========================================================================
    // AST Statements & Expressions
    // =========================================================================

    private interface Statement {
        void execute(ExecutionContext ctx);
    }

    private interface Expr {
        double eval(ExecutionContext ctx);
    }

    private static class AssignmentStatement implements Statement {
        final String varName;
        final Expr indexExpr; // null for scalar variable
        final Expr valueExpr;
        final boolean isOutputArray;
        final boolean isInputArray;

        AssignmentStatement(String varName, Expr indexExpr, Expr valueExpr) {
            this.varName = varName;
            this.indexExpr = indexExpr;
            this.valueExpr = valueExpr;
            String lower = varName.toLowerCase(Locale.ROOT);
            this.isOutputArray = lower.equals("yout") || lower.equals("y");
            this.isInputArray = lower.equals("xin") || lower.equals("in");
        }

        @Override
        public void execute(ExecutionContext ctx) {
            double val = valueExpr.eval(ctx);
            if (indexExpr != null) {
                int idx = (int) Math.round(indexExpr.eval(ctx));
                if (isOutputArray) {
                    ctx.setOutput(idx, val);
                } else if (isInputArray) {
                    // write to local input shadow if desired
                    if (idx >= 0 && idx < ctx.inputs.length) {
                        ctx.inputs[idx] = val;
                    }
                }
            } else {
                if (isOutputArray) {
                    ctx.setOutput(0, val);
                } else {
                    ctx.setVariable(varName, val);
                }
            }
        }
    }

    private static class IfStatement implements Statement {
        final Expr condition;
        final List<Statement> thenBranch;
        final List<Statement> elseBranch;

        IfStatement(Expr condition, List<Statement> thenBranch, List<Statement> elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch != null ? elseBranch : List.of();
        }

        @Override
        public void execute(ExecutionContext ctx) {
            if (condition.eval(ctx) != 0.0) {
                for (Statement s : thenBranch) {
                    s.execute(ctx);
                }
            } else {
                for (Statement s : elseBranch) {
                    s.execute(ctx);
                }
            }
        }
    }

    private static class ExpressionStatement implements Statement {
        final Expr expr;

        ExpressionStatement(Expr expr) {
            this.expr = expr;
        }

        @Override
        public void execute(ExecutionContext ctx) {
            // Default: single expression writes to output[0]
            double result = expr.eval(ctx);
            ctx.setOutput(0, result);
        }
    }

    // =========================================================================
    // Expression AST Nodes
    // =========================================================================

    private static class NumberLiteral implements Expr {
        final double val;

        NumberLiteral(double val) {
            this.val = val;
        }

        @Override
        public double eval(ExecutionContext ctx) {
            return val;
        }
    }

    private static class VariableExpr implements Expr {
        final String name;

        VariableExpr(String name) {
            this.name = name;
        }

        @Override
        public double eval(ExecutionContext ctx) {
            return ctx.getVariable(name);
        }
    }

    private static class ArrayAccessExpr implements Expr {
        final String name;
        final Expr indexExpr;

        ArrayAccessExpr(String name, Expr indexExpr) {
            this.name = name;
            this.indexExpr = indexExpr;
        }

        @Override
        public double eval(ExecutionContext ctx) {
            int idx = (int) Math.round(indexExpr.eval(ctx));
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.equals("xin") || lower.equals("in")) {
                return ctx.getInput(idx);
            }
            if (lower.equals("yout") || lower.equals("y")) {
                return ctx.getOutput(idx);
            }
            return 0.0;
        }
    }

    private static class FunctionCallExpr implements Expr {
        final String funcName;
        final List<Expr> args;

        FunctionCallExpr(String funcName, List<Expr> args) {
            this.funcName = funcName.toLowerCase(Locale.ROOT);
            this.args = args;
        }

        @Override
        public double eval(ExecutionContext ctx) {
            double a0 = !args.isEmpty() ? args.get(0).eval(ctx) : 0.0;
            double a1 = args.size() > 1 ? args.get(1).eval(ctx) : 0.0;

            return switch (funcName) {
                case "sin" -> Math.sin(a0);
                case "cos" -> Math.cos(a0);
                case "tan" -> Math.tan(a0);
                case "asin" -> Math.asin(a0);
                case "acos" -> Math.acos(a0);
                case "atan" -> Math.atan(a0);
                case "atan2" -> Math.atan2(a0, a1);
                case "sinh" -> Math.sinh(a0);
                case "cosh" -> Math.cosh(a0);
                case "tanh" -> Math.tanh(a0);
                case "sqrt" -> Math.sqrt(a0);
                case "cbrt" -> Math.cbrt(a0);
                case "abs" -> Math.abs(a0);
                case "exp" -> Math.exp(a0);
                case "log", "ln" -> Math.log(a0);
                case "log10" -> Math.log10(a0);
                case "pow" -> Math.pow(a0, a1);
                case "min" -> Math.min(a0, a1);
                case "max" -> Math.max(a0, a1);
                case "floor" -> Math.floor(a0);
                case "ceil" -> Math.ceil(a0);
                case "round" -> Math.round(a0);
                case "signum", "sign", "sgn" -> Math.signum(a0);
                default -> 0.0;
            };
        }
    }

    private static class UnaryOpExpr implements Expr {
        final char op;
        final Expr operand;

        UnaryOpExpr(char op, Expr operand) {
            this.op = op;
            this.operand = operand;
        }

        @Override
        public double eval(ExecutionContext ctx) {
            double v = operand.eval(ctx);
            return op == '-' ? -v : (v == 0.0 ? 1.0 : 0.0);
        }
    }

    private static class BinaryOpExpr implements Expr {
        final String op;
        final Expr left;
        final Expr right;

        BinaryOpExpr(String op, Expr left, Expr right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Override
        public double eval(ExecutionContext ctx) {
            double l = left.eval(ctx);
            double r = right.eval(ctx);
            return switch (op) {
                case "+" -> l + r;
                case "-" -> l - r;
                case "*" -> l * r;
                case "/" -> r != 0.0 ? l / r : warnAndZero(ctx);
                case "%" -> r != 0.0 ? l % r : warnAndZero(ctx);
                case "^" -> Math.pow(l, r);
                case "<" -> l < r ? 1.0 : 0.0;
                case "<=" -> l <= r ? 1.0 : 0.0;
                case ">" -> l > r ? 1.0 : 0.0;
                case ">=" -> l >= r ? 1.0 : 0.0;
                case "==" -> Math.abs(l - r) < 1e-12 ? 1.0 : 0.0;
                case "!=" -> Math.abs(l - r) >= 1e-12 ? 1.0 : 0.0;
                case "&&" -> (l != 0.0 && r != 0.0) ? 1.0 : 0.0;
                case "||" -> (l != 0.0 || r != 0.0) ? 1.0 : 0.0;
                default -> 0.0;
            };
        }

        private static double warnAndZero(ExecutionContext ctx) {
            ctx.warnDivideByZero();
            return 0.0;
        }
    }

    private static class TernaryExpr implements Expr {
        final Expr condition;
        final Expr thenExpr;
        final Expr elseExpr;

        TernaryExpr(Expr condition, Expr thenExpr, Expr elseExpr) {
            this.condition = condition;
            this.thenExpr = thenExpr;
            this.elseExpr = elseExpr;
        }

        @Override
        public double eval(ExecutionContext ctx) {
            return condition.eval(ctx) != 0.0 ? thenExpr.eval(ctx) : elseExpr.eval(ctx);
        }
    }

    // =========================================================================
    // Lexer & Recursive Descent Parser
    // =========================================================================

    private static List<Statement> parseStatements(String code) {
        List<Statement> statements = new ArrayList<>();
        Lexer lexer = new Lexer(code);
        Parser parser = new Parser(lexer);

        while (parser.peek() != null && !parser.peek().isEOF()) {
            Statement s = parser.parseStatement();
            if (s != null) {
                statements.add(s);
            }
        }

        // A bare expression writes yOUT[0] only in formula mode (the script's single
        // statement). Stray statements left over from stripped declarations
        // (`double alpha, beta;` -> `alpha; beta;`) must not clobber outputs in
        // multi-statement scripts.
        if (statements.size() > 1) {
            statements.replaceAll(s -> s instanceof ExpressionStatement ? NO_OP : s);
        }
        return statements;
    }

    private static final Statement NO_OP = ctx -> {
    };

    private enum TokenType {
        NUMBER, IDENTIFIER, OPERATOR, LPAREN, RPAREN, LBRACKET, RBRACKET,
        LBRACE, RBRACE, COMMA, SEMICOLON, QUESTION, COLON, IF, ELSE, EOF
    }

    private static class Token {
        final TokenType type;
        final String text;
        final double numberValue;

        Token(TokenType type, String text) {
            this(type, text, 0.0);
        }

        Token(TokenType type, String text, double numberValue) {
            this.type = type;
            this.text = text;
            this.numberValue = numberValue;
        }

        boolean isEOF() {
            return type == TokenType.EOF;
        }
    }

    private static class Lexer {
        private final String input;
        private int pos = 0;

        Lexer(String input) {
            this.input = input;
        }

        Token nextToken() {
            skipWhitespace();
            if (pos >= input.length()) {
                return new Token(TokenType.EOF, "");
            }

            char c = input.charAt(pos);

            // Numbers
            if (Character.isDigit(c) || (c == '.' && pos + 1 < input.length() && Character.isDigit(input.charAt(pos + 1)))) {
                int start = pos;
                while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                    pos++;
                }
                // Scientific notation (e.g. 1e-6)
                if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
                    pos++;
                    if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
                        pos++;
                    }
                    while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                        pos++;
                    }
                }
                String text = input.substring(start, pos);
                double val = Double.parseDouble(text);
                return new Token(TokenType.NUMBER, text, val);
            }

            // Identifiers / Keywords
            if (Character.isLetter(c) || c == '_') {
                int start = pos;
                while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) {
                    pos++;
                }
                String id = input.substring(start, pos);
                if (id.equals("if")) {
                    return new Token(TokenType.IF, id);
                }
                if (id.equals("else")) {
                    return new Token(TokenType.ELSE, id);
                }
                return new Token(TokenType.IDENTIFIER, id);
            }

            // Multi-char operators
            if (pos + 1 < input.length()) {
                String twoChar = input.substring(pos, pos + 2);
                if (twoChar.equals("==") || twoChar.equals("!=") || twoChar.equals("<=")
                        || twoChar.equals(">=") || twoChar.equals("&&") || twoChar.equals("||")) {
                    pos += 2;
                    return new Token(TokenType.OPERATOR, twoChar);
                }
            }

            // Single-char operators and punctuation
            pos++;
            return switch (c) {
                case '(' -> new Token(TokenType.LPAREN, "(");
                case ')' -> new Token(TokenType.RPAREN, ")");
                case '[' -> new Token(TokenType.LBRACKET, "[");
                case ']' -> new Token(TokenType.RBRACKET, "]");
                case '{' -> new Token(TokenType.LBRACE, "{");
                case '}' -> new Token(TokenType.RBRACE, "}");
                case ',' -> new Token(TokenType.COMMA, ",");
                case ';' -> new Token(TokenType.SEMICOLON, ";");
                case '?' -> new Token(TokenType.QUESTION, "?");
                case ':' -> new Token(TokenType.COLON, ":");
                case '+', '-', '*', '/', '%', '^', '<', '>', '=', '!' -> new Token(TokenType.OPERATOR, String.valueOf(c));
                default -> new Token(TokenType.OPERATOR, String.valueOf(c));
            };
        }

        private void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }
    }

    private static class Parser {
        private final List<Token> tokens;
        private int pos = 0;

        Parser(Lexer lexer) {
            this.tokens = new ArrayList<>();
            Token t;
            do {
                t = lexer.nextToken();
                tokens.add(t);
            } while (!t.isEOF());
        }

        Token peek() {
            return peek(0);
        }

        Token peek(int offset) {
            int p = pos + offset;
            if (p < tokens.size()) {
                return tokens.get(p);
            }
            return new Token(TokenType.EOF, "");
        }

        Token consume() {
            if (pos < tokens.size()) {
                return tokens.get(pos++);
            }
            return new Token(TokenType.EOF, "");
        }

        Token consume(TokenType expected) {
            Token t = peek();
            if (t.type == expected) {
                return consume();
            }
            throw new RuntimeException("Expected token " + expected + " but got " + t.type + " ('" + t.text + "')");
        }

        Statement parseStatement() {
            if (peek().type == TokenType.SEMICOLON) {
                consume();
                return null;
            }

            if (peek().type == TokenType.IF) {
                return parseIfStatement();
            }

            // Check for comma-separated identifier declarations (e.g. "alpha, beta, d, q, theta;")
            if (peek().type == TokenType.IDENTIFIER && peek(1).type == TokenType.COMMA) {
                while (peek().type != TokenType.SEMICOLON && !peek().isEOF()) {
                    consume();
                }
                if (peek().type == TokenType.SEMICOLON) {
                    consume();
                }
                return null;
            }

            // Check if it's an assignment:
            if (isAssignment()) {
                Token id = consume();
                Expr indexExpr = null;
                if (peek().type == TokenType.LBRACKET) {
                    consume(TokenType.LBRACKET);
                    indexExpr = parseExpression();
                    consume(TokenType.RBRACKET);
                }
                consume(); // '='
                Expr value = parseExpression();
                if (peek().type == TokenType.SEMICOLON) {
                    consume();
                }
                return new AssignmentStatement(id.text, indexExpr, value);
            }

            // General expression statement
            Expr expr = parseExpression();
            if (peek().type == TokenType.SEMICOLON) {
                consume();
            }
            return new ExpressionStatement(expr);
        }

        private boolean isAssignment() {
            if (peek().type != TokenType.IDENTIFIER) {
                return false;
            }
            if (peek(1).type == TokenType.OPERATOR && peek(1).text.equals("=")) {
                return true;
            }
            if (peek(1).type == TokenType.LBRACKET) {
                int depth = 0;
                for (int i = 1; i < tokens.size() - pos; i++) {
                    TokenType t = peek(i).type;
                    if (t == TokenType.LBRACKET) {
                        depth++;
                    } else if (t == TokenType.RBRACKET) {
                        depth--;
                        if (depth == 0) {
                            return peek(i + 1).type == TokenType.OPERATOR && peek(i + 1).text.equals("=");
                        }
                    } else if (t == TokenType.SEMICOLON || t == TokenType.EOF) {
                        break;
                    }
                }
            }
            return false;
        }

        private Statement parseIfStatement() {
            consume(TokenType.IF);
            consume(TokenType.LPAREN);
            Expr condition = parseExpression();
            consume(TokenType.RPAREN);

            List<Statement> thenBranch = parseBlockOrStatement();
            List<Statement> elseBranch = null;

            if (peek().type == TokenType.ELSE) {
                consume(TokenType.ELSE);
                elseBranch = parseBlockOrStatement();
            }

            return new IfStatement(condition, thenBranch, elseBranch);
        }

        private List<Statement> parseBlockOrStatement() {
            List<Statement> list = new ArrayList<>();
            if (peek().type == TokenType.LBRACE) {
                consume(TokenType.LBRACE);
                while (peek().type != TokenType.RBRACE && !peek().isEOF()) {
                    Statement s = parseStatement();
                    if (s != null) {
                        list.add(s);
                    }
                }
                consume(TokenType.RBRACE);
            } else {
                Statement s = parseStatement();
                if (s != null) {
                    list.add(s);
                }
            }
            return list;
        }

        Expr parseExpression() {
            return parseTernary();
        }

        private Expr parseTernary() {
            Expr cond = parseLogicalOr();
            if (peek().type == TokenType.QUESTION) {
                consume();
                Expr thenExpr = parseExpression();
                consume(TokenType.COLON);
                Expr elseExpr = parseExpression();
                return new TernaryExpr(cond, thenExpr, elseExpr);
            }
            return cond;
        }

        private Expr parseLogicalOr() {
            Expr left = parseLogicalAnd();
            while (peek().type == TokenType.OPERATOR && peek().text.equals("||")) {
                String op = consume().text;
                Expr right = parseLogicalAnd();
                left = new BinaryOpExpr(op, left, right);
            }
            return left;
        }

        private Expr parseLogicalAnd() {
            Expr left = parseEquality();
            while (peek().type == TokenType.OPERATOR && peek().text.equals("&&")) {
                String op = consume().text;
                Expr right = parseEquality();
                left = new BinaryOpExpr(op, left, right);
            }
            return left;
        }

        private Expr parseEquality() {
            Expr left = parseRelational();
            while (peek().type == TokenType.OPERATOR && (peek().text.equals("==") || peek().text.equals("!="))) {
                String op = consume().text;
                Expr right = parseRelational();
                left = new BinaryOpExpr(op, left, right);
            }
            return left;
        }

        private Expr parseRelational() {
            Expr left = parseAdditive();
            while (peek().type == TokenType.OPERATOR && (peek().text.equals("<") || peek().text.equals("<=")
                    || peek().text.equals(">") || peek().text.equals(">="))) {
                String op = consume().text;
                Expr right = parseAdditive();
                left = new BinaryOpExpr(op, left, right);
            }
            return left;
        }

        private Expr parseAdditive() {
            Expr left = parseMultiplicative();
            while (peek().type == TokenType.OPERATOR && (peek().text.equals("+") || peek().text.equals("-"))) {
                String op = consume().text;
                Expr right = parseMultiplicative();
                left = new BinaryOpExpr(op, left, right);
            }
            return left;
        }

        private Expr parseMultiplicative() {
            Expr left = parsePower();
            while (peek().type == TokenType.OPERATOR && (peek().text.equals("*") || peek().text.equals("/") || peek().text.equals("%"))) {
                String op = consume().text;
                Expr right = parsePower();
                left = new BinaryOpExpr(op, left, right);
            }
            return left;
        }

        private Expr parsePower() {
            Expr left = parseUnary();
            if (peek().type == TokenType.OPERATOR && peek().text.equals("^")) {
                String op = consume().text;
                Expr right = parsePower(); // right-associative
                return new BinaryOpExpr(op, left, right);
            }
            return left;
        }

        private Expr parseUnary() {
            if (peek().type == TokenType.OPERATOR && (peek().text.equals("-") || peek().text.equals("!"))) {
                char op = consume().text.charAt(0);
                Expr operand = parseUnary();
                return new UnaryOpExpr(op, operand);
            }
            return parsePrimary();
        }

        private Expr parsePrimary() {
            Token t = peek();
            if (t.type == TokenType.NUMBER) {
                consume();
                return new NumberLiteral(t.numberValue);
            }
            if (t.type == TokenType.LPAREN) {
                consume(TokenType.LPAREN);
                Expr expr = parseExpression();
                consume(TokenType.RPAREN);
                return expr;
            }
            if (t.type == TokenType.IDENTIFIER) {
                Token id = consume();
                return parseExprStartingWithIdent(id, null);
            }
            throw new RuntimeException("Unexpected token in expression: " + t.text);
        }

        private Expr parseExprStartingWithIdent(Token id, Expr preParsedIndex) {
            // Function call: ident(arg1, arg2...)
            if (peek().type == TokenType.LPAREN) {
                consume(TokenType.LPAREN);
                List<Expr> args = new ArrayList<>();
                if (peek().type != TokenType.RPAREN) {
                    args.add(parseExpression());
                    while (peek().type == TokenType.COMMA) {
                        consume(TokenType.COMMA);
                        args.add(parseExpression());
                    }
                }
                consume(TokenType.RPAREN);
                return new FunctionCallExpr(id.text, args);
            }

            // Array indexing: ident[index]
            if (preParsedIndex != null) {
                return new ArrayAccessExpr(id.text, preParsedIndex);
            }
            if (peek().type == TokenType.LBRACKET) {
                consume(TokenType.LBRACKET);
                Expr index = parseExpression();
                consume(TokenType.RBRACKET);
                return new ArrayAccessExpr(id.text, index);
            }

            // Simple variable
            return new VariableExpr(id.text);
        }
    }
}
