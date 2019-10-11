// It's not a compiler, it's just an interpreter with extra steps.

import java.io.File;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;

import java.nio.charset.Charset;
import java.nio.file.Files;

import java.util.List;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.ArrayList;


public class PythonConverter {
    public String[] reserved = {"clear",
                                "copy",
                                "decr",
                                "do",
                                "end",
                                "incr",
                                "init",
                                "not",
                                "to",
                                "while",
                                "init"};

    public List<String> declaredVariables = new ArrayList<String>();
    public String inputFile;
    public String outputFile;
    public BufferedWriter fileWriter;

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

    public void parseLine(String line) throws IOException {
        List<String> tokens = tokenise(line);
        System.out.println(tokens);

        if (checkForSemicolon(tokens)) {
            if ((!tokens.get(0).toLowerCase().equals("end")) && checkIdentifierName(tokens.get(1))) {
                switch (tokens.get(0).toLowerCase()) {
                    case "clear":
                        // System.out.println("Clear " + tokens.get(0).toLowerCase());
                        handleClearStatement(tokens);
                        break;

                    case "incr":
                        // System.out.println("Incr " + tokens.get(0).toLowerCase());
                        handleIncrStatement(tokens);
                        break;

                    case "while":
                        // TODO: figure out.
                        // TODO: check identifier names.
                        ;
                }
            }
        }

    }

    private Boolean checkForSemicolon(List<String> tokenisedLine) {
        if (!tokenisedLine.get(tokenisedLine.size() - 1).equals(";")) {
            throw new SyntaxError("Statements must be terminated with a semicolon");
        }

        return true;
    }

    private void writeLineToFile(String pythonCode) throws IOException {
        fileWriter = new BufferedWriter(new FileWriter(outputFile, true));
        fileWriter.append(pythonCode);
        fileWriter.close();
    }

    private void handleClearStatement(List<String> tokenisedLine) throws IOException {
        if (tokenisedLine.size() != 3) {
            throw new SyntaxError("Invalid clear statement syntax");
        }

        String variable = tokenisedLine.get(1);

        if (!declaredVariables.contains(variable)) {
            declaredVariables.add(variable);
        }

        writeLineToFile(String.format("%s = 0\n", variable));
    }

    private void handleIncrStatement(List<String> tokenisedLine) throws IOException {
        if (tokenisedLine.size() != 3) {
            throw new SyntaxError("Invalid incr statement syntax");
        }

        String variable = tokenisedLine.get(1);

        if (!declaredVariables.contains(variable)) {
            throw new UndeclaredVariableException("Variables must be declared with a clear statement before use");
        }

        writeLineToFile(String.format("%s += 1\n", variable));
    }


    public static void main(String[] args) throws IOException {
        final String TEST_INPUT = "C:\\Users\\Elliott\\IdeaProjects\\Space Cadets\\SpaceCadets2\\test_file.txt";
        final String TEST_OUTPUT = "C:\\Users\\Elliott\\IdeaProjects\\Space Cadets\\SpaceCadets2\\test_output_file.txt";
        
        PythonConverter fuck = new PythonConverter(TEST_INPUT, TEST_OUTPUT);
        List<String> lines = fuck.readFileLines();

        for (String line: lines) {
            // System.out.println(line);
            fuck.parseLine(line);
        }
    }
}