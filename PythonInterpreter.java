import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class PythonInterpreter {
    public String pathToCode;

    PythonInterpreter(String pathToPyFile) {
        pathToCode = pathToPyFile;
    }

    public List<String> runPythonCode() throws IOException, InterruptedException {
        try {
            ProcessBuilder pythonProcessBuilder = new ProcessBuilder(Arrays.asList("python", pathToCode));
            Process pythonProcess = pythonProcessBuilder.start();

            BufferedReader pythonOutput = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));

            String line = "";
            List<String> output = new ArrayList<String>();

            pythonProcess.waitFor();

            while ((line = pythonOutput.readLine()) != null) {
                output.add(line);
            }

            return output;
        } catch (java.io.IOException e) {
            throw new NoPythonFoundError("Could not find Python installation");
        }
    }
}