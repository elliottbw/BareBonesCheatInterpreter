import java.io.File;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.nio.charset.Charset;
import java.nio.file.Files;

import java.util.List;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.ArrayList;


public class PythonConverter {
    public String[] reserved = {"clear", // done
                                "copy", // done
                                "decr", // done
                                "do", // done
                                "end", // done
                                "incr", // done
                                "init", // done
                                "not", // done
                                "to", // done
                                "while"}; // done

    public List<String> declaredVariables = new ArrayList<String>();
    public String inputFile;
    public String outputFile;
    public BufferedWriter fileWriter;
    public Boolean initAllowed = true;
    public int whileLoopDepth;

    PythonConverter(String pathToInputFile, String pathToOutputFile) throws IOException {
        inputFile = pathToInputFile;
        outputFile = pathToOutputFile;

        fileWriter = new BufferedWriter(new FileWriter(outputFile, false));

    }

    public List<String> readFileLines() throws IOException {
        File file = new File(inputFile);
        List<String> output = Files.readAllLines(file.toPath(), Charset.defaultCharset());

        return output;
    }

    private void writeLineToFile(String pythonCode) throws IOException {
        fileWriter = new BufferedWriter(new FileWriter(outputFile, true));
        fileWriter.append(pythonCode);
        fileWriter.close();
    }

    public String getInput(String message) throws Exception {
        System.out.print(message);
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String output = input.readLine();

        return output;
    }

    public List<String> tokenise(String line) {
        // Split line string into a series of tokens
        // The semicolon is also marked as its own token with a positive lookahead
        List<String> tokenList = Arrays.asList(line.split("((?<! ) (?! )|(?=;))"));

        //Remove leading whitespace
        String preWhitespaceRemoval;
        for (int i = 0; i < tokenList.size(); i++) {
            preWhitespaceRemoval = tokenList.get(i);
            tokenList.set(i, preWhitespaceRemoval.replaceAll("( {2,}|	)", ""));
        }

        return tokenList;
    }

    private Boolean checkIdentifierName(String variableName) {
        // Positive lookbehind for "name" property, non-greedy search for any number of characters,
        // then positive lookahead for a open tag. 
        variableName = variableName.toLowerCase();

        final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9]");

        if (!Character.isLetter(variableName.charAt(0))) {
            throw new InvalidIdentifierNameException("Identifiers must begin with a letter"); 
        } else if (NON_ALPHANUMERIC.matcher(variableName).find()) {
            throw new InvalidIdentifierNameException("Identifiers must contain only alphanumeric characters");
        } else if (Arrays.asList(reserved).contains(variableName.toLowerCase())) {
            throw new InvalidIdentifierNameException("Identifier must not be named a reserved keyword");
        }
        
        return true;
    }

    private Boolean checkForSemicolon(List<String> tokenisedLine) {
        if (!tokenisedLine.get(tokenisedLine.size() - 1).equals(";")) {
            throw new SyntaxError("Statements must be terminated with a semicolon");
        }

        return true;
    }

    public void parseLine(String line) throws IOException, Exception {
        List<String> tokens = tokenise(line);

        if (tokens.isEmpty() || tokens.get(0).equals("")) {
            return;
        }

        if (checkForSemicolon(tokens)) {
            if ((!tokens.get(0).equalsIgnoreCase("end")) && checkIdentifierName(tokens.get(1))) {
                switch (tokens.get(0).toLowerCase()) {
                    case "init":
                        handleInitStatement(tokens);
                        break;

                    case "clear":
                        initAllowed = false;
                        handleClearStatement(tokens);
                        break;

                    case "incr":
                        initAllowed = false;
                        handleIncrStatement(tokens);
                        break;

                    case "decr":
                        initAllowed = false;
                        handleDecrStatement(tokens);
                        break;

                    case "copy":
                        initAllowed = false;
                        handleCopyStatement(tokens);
                        break;

                    case "while":
                        initAllowed = false;
                        handleWhileLoop(tokens);
                        break;
                }
            } else if (tokens.get(0).equalsIgnoreCase("end")) {
                initAllowed = false;
                handleEndStatement(tokens);
            }
        }
    }

    public void finishParsing() throws IOException {
        for (String variable: declaredVariables) {
            writeLineToFile(String.format("print(f\"%s: {%s}\")\n", variable, variable));
        }
    }

    private void handleClearStatement(List<String> tokenisedLine) throws IOException {
        if (tokenisedLine.size() != 3) {
            throw new SyntaxError("Invalid clear statement syntax");
        }

        String variable = tokenisedLine.get(1);

        if (!declaredVariables.contains(variable)) {
            declaredVariables.add(variable);
        }

        writeLineToFile("    ".repeat(whileLoopDepth) + String.format("%s = 0\n", variable));
    }

    private void handleIncrStatement(List<String> tokenisedLine) throws IOException {
        if (tokenisedLine.size() != 3) {
            throw new SyntaxError("Invalid incr statement syntax");
        }

        String variable = tokenisedLine.get(1);

        if (!declaredVariables.contains(variable)) {
            throw new UndeclaredVariableException("Variables must be declared with a clear, init, or copy statement before use");
        }

        writeLineToFile("    ".repeat(whileLoopDepth) + String.format("%s += 1\n", variable));
    }

    private void handleDecrStatement(List<String> tokenisedLine) throws IOException {
        if (tokenisedLine.size() != 3) {
            throw new SyntaxError("Invalid decr statement syntax");
        }

        String variable = tokenisedLine.get(1);

        if (!declaredVariables.contains(variable)) {
            throw new UndeclaredVariableException("Variables must be declared with a clear, init, or copy statement before use");
        }

        writeLineToFile("    ".repeat(whileLoopDepth) + String.format("if %s > 0:\n%s    %s -= 1\n", variable, "    ".repeat(whileLoopDepth), variable));
    }

    private void handleInitStatement(List<String> tokenisedLine) throws IOException, Exception {
        if (!initAllowed) {
            throw new UndeclaredVariableException("init variable declarations must be placed before the main code body");
        }

        if (tokenisedLine.size() != 3 && tokenisedLine.size() != 5) {
            throw new SyntaxError("Invalid init statement syntax");
        }

        String variable = tokenisedLine.get(1);

        if (!declaredVariables.contains(variable)) {
            declaredVariables.add(variable);
        } else {
            throw new InvalidIdentifierNameException(String.format("%s already exists", variable));
        }

        // Why is this needed?
        String initialVariableValue = "";

        if (tokenisedLine.size() == 3) {
            initialVariableValue = getInput(String.format("%s = ", variable));

        } else if (tokenisedLine.size() == 5) {
            if (!tokenisedLine.get(2).equals("=")) {
                throw new SyntaxError("Invalid init statement syntax");
            }
            
            initialVariableValue = tokenisedLine.get(3);
        }

        try {
            int variableValue = Integer.parseInt(initialVariableValue);

            if (variableValue < 0) {
                throw new VariableValueException("Variables must have non-negative integer values");
            }

            writeLineToFile(String.format("%s = %d\n", variable, variableValue));
        } catch (java.lang.NumberFormatException e) {
            throw new VariableValueException(String.format("%s is not an integer", initialVariableValue));
        }
    }

    private void handleCopyStatement(List<String> tokenisedLine) throws IOException {
        if (tokenisedLine.size() != 5 || !tokenisedLine.get(2).equals("to")) {
            throw new SyntaxError("Invalid copy statement syntax");
        }
        
        String copyFromVariable = tokenisedLine.get(1);
        String copyToVariable = tokenisedLine.get(3);

        writeLineToFile("    ".repeat(whileLoopDepth) + String.format("%s = %s\n", copyToVariable, copyFromVariable));
    }

    private void handleWhileLoop(List<String> tokenisedLine) throws IOException {
        if (tokenisedLine.size() != 6 || !tokenisedLine.get(2).equalsIgnoreCase("not") || !tokenisedLine.get(4).equalsIgnoreCase("do")) {
            throw new SyntaxError("Invalid while loop syntax");
        }
        
        String variable = tokenisedLine.get(1);

        if (!declaredVariables.contains(variable)) {
            throw new UndeclaredVariableException("Variables must be declared with a clear, init, or copy statement before use");
        }
        
        String finalValueAsString = tokenisedLine.get(3);

        try {
            int finalValue = Integer.parseInt(finalValueAsString);

            if (finalValue < 0) {
                throw new VariableValueException("Variables must have non-negative integer values");
            }

            writeLineToFile("    ".repeat(whileLoopDepth) + String.format("while %s != %d:\n", variable, finalValue));
            whileLoopDepth += 1;
        } catch (java.lang.NumberFormatException e) {
            throw new VariableValueException(String.format("%s is not an integer", finalValueAsString));
        }
    }

    private void handleEndStatement(List<String> tokenisedLine) {
        if (whileLoopDepth <= 0) {
            throw new SyntaxError("end statement not expected outside of while loop");
        }

        whileLoopDepth -= 1;
    }
    
    public static void main(String[] args) throws IOException, Exception {
        final String TEST_INPUT = "C:\\Users\\Elliott\\IdeaProjects\\Space Cadets\\SpaceCadets2\\multiply.bb";
        final String TEST_OUTPUT = "C:\\Users\\Elliott\\IdeaProjects\\Space Cadets\\SpaceCadets2\\test_output_file.py";
        
        PythonConverter converter = new PythonConverter(TEST_INPUT, TEST_OUTPUT);
        PythonInterpreter interpreter = new PythonInterpreter(TEST_OUTPUT);

        List<String> lines = converter.readFileLines();

        for (String line: lines) {
            converter.parseLine(line);
        }

        converter.finishParsing();
        System.out.println(interpreter.runPythonCode());

    }
}
