import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Convert {

    public static String readPythonFile(String pythonFile) throws IOException {
        Path path = Paths.get(pythonFile);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Python file not found: " + pythonFile);
        }
        return Files.readString(path);
    }

    public static String convertWithLlm(String pythonCode, Path outputFile) throws IOException, InterruptedException {
        String prompt = "Convert the following Python code to equivalent Java code.\n\n"
                + "Requirements:\n"
                + "1. The Java code must compile successfully with javac\n"
                + "2. The Java code must produce the same output as the Python code\n"
                + "3. Use appropriate Java idioms and conventions\n"
                + "4. Include necessary imports\n"
                + "5. Create a class named based on the output file: " + getFileNameWithoutExtension(outputFile) + "\n\n"
                + "Python code:\n"
                + "```python\n"
                + pythonCode + "\n"
                + "```\n\n"
                + "Output only the Java code, no explanations.";

        ProcessBuilder pb = new ProcessBuilder("codex", "exec", "--sandbox", "read-only", prompt);
        pb.directory(outputFile.getParent().toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Codex conversion failed: " + output);
        }

        String javaCode = output.strip();

        if (javaCode.isEmpty()) {
            throw new RuntimeException("LLM returned empty Java code");
        }

        Files.writeString(outputFile, javaCode);
        return javaCode;
    }

    public static boolean compileJava(Path javaFile) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("javac", javaFile.getFileName().toString());
        pb.directory(javaFile.getParent().toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String errorOutput = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            System.err.println("Compilation failed:\n" + errorOutput);
            return false;
        }

        return true;
    }

    public static RunResult runJava(Path javaFile) throws IOException, InterruptedException {
        String className = getFileNameWithoutExtension(javaFile);
        ProcessBuilder pb = new ProcessBuilder("java", className);
        pb.directory(javaFile.getParent().toFile());
        pb.redirectErrorStream(false);
        Process process = pb.start();

        String stdout = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        return new RunResult(exitCode == 0, stdout);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: Convert <python_file> [output_dir]");
            System.exit(1);
        }

        String pythonFile = args[0];
        Path outputDir = args.length > 1 ? Paths.get(args[1]) : Paths.get(System.getProperty("user.dir"));

        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            System.err.println("Failed to create output directory: " + e.getMessage());
            System.exit(1);
        }

        String pythonCode;
        try {
            pythonCode = readPythonFile(pythonFile);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
            return;
        }
        System.out.println("Read Python code from " + pythonFile);

        Path javaFile = outputDir.resolve("Converted.java");
        int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            System.out.println("\nAttempt " + attempt + "/" + maxAttempts);

            try {
                convertWithLlm(pythonCode, javaFile);
            } catch (Exception e) {
                System.out.println("Conversion error: " + e.getMessage());
                continue;
            }
            System.out.println("Generated Java code in " + javaFile);

            try {
                if (compileJava(javaFile)) {
                    System.out.println("\u2713 Compilation successful");

                    RunResult result = runJava(javaFile);
                    if (result.success) {
                        System.out.println("\u2713 Execution successful");
                        System.out.println("\nOutput:\n" + result.output);
                        System.out.println("\n\u2705 Conversion complete! Java file: " + javaFile);
                        return;
                    } else {
                        System.out.println("\u2717 Execution failed, retrying...");
                    }
                } else {
                    System.out.println("\u2717 Compilation failed, retrying...");
                }
            } catch (Exception e) {
                System.out.println("Error during compile/run: " + e.getMessage());
            }
        }

        System.err.println("\n\u274C Failed after " + maxAttempts + " attempts");
        System.exit(1);
    }

    private static String getFileNameWithoutExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    public static class RunResult {
        public final boolean success;
        public final String output;

        public RunResult(boolean success, String output) {
            this.success = success;
            this.output = output;
        }
    }
}
