import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Convert {

    private static String readPythonFile(String pythonFile) throws IOException {
        Path path = Paths.get(pythonFile);
        if (!Files.exists(path)) {
            throw new IOException("Python file not found: " + pythonFile);
        }
        return Files.readString(path);
    }

    private static String convertWithLlm(String pythonCode, Path outputFile) throws IOException, InterruptedException {
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

        List<String> command = new ArrayList<>();
        command.add("codex");
        command.add("exec");
        command.add("--sandbox");
        command.add("read-only");
        command.add(prompt);

        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(outputFile.getParent().toFile())
                .redirectErrorStream(false);

        Process process = pb.start();

        String stdout = readStream(process);
        String stderr = readErrorStream(process);

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Codex conversion failed: " + stderr);
        }

        String javaCode = stdout.trim();

        if (javaCode.isEmpty()) {
            throw new RuntimeException("LLM returned empty Java code");
        }

        Files.writeString(outputFile, javaCode);
        return javaCode;
    }

    private static boolean compileJava(Path javaFile) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("javac");
        command.add(javaFile.getFileName().toString());

        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(javaFile.getParent().toFile())
                .redirectErrorStream(false);

        Process process = pb.start();
        String stderr = readErrorStream(process);
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            System.err.println("Compilation failed:\n" + stderr);
            return false;
        }

        return true;
    }

    private static ProcessResult runJava(Path javaFile) throws IOException, InterruptedException {
        String className = getFileNameWithoutExtension(javaFile);

        List<String> command = new ArrayList<>();
        command.add("java");
        command.add(className);

        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(javaFile.getParent().toFile())
                .redirectErrorStream(false);

        Process process = pb.start();
        String stdout = readStream(process);
        process.waitFor();

        return new ProcessResult(process.exitValue() == 0, stdout);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: convert.py <python_file> [output_dir]");
            System.exit(1);
        }

        String pythonFile = args[0];
        Path outputDir = args.length > 1 ? Paths.get(args[1]) : Paths.get(".");

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
            System.err.println("Failed to read Python file: " + e.getMessage());
            System.exit(1);
            return;
        }
        System.out.println("Read Python code from " + pythonFile);

        Path javaFile = outputDir.resolve("Converted.java");
        int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            System.out.println("\nAttempt " + attempt + "/" + maxAttempts);

            String javaCode;
            try {
                javaCode = convertWithLlm(pythonCode, javaFile);
            } catch (Exception e) {
                System.out.println("Conversion error: " + e.getMessage());
                continue;
            }
            System.out.println("Generated Java code in " + javaFile);

            try {
                if (compileJava(javaFile)) {
                    System.out.println("\u2713 Compilation successful");

                    ProcessResult result = runJava(javaFile);
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
                System.out.println("Build/run error: " + e.getMessage());
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

    private static String readStream(Process process) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private static String readErrorStream(Process process) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private static class ProcessResult {
        final boolean success;
        final String output;

        ProcessResult(boolean success, String output) {
            this.success = success;
            this.output = output;
        }
    }
}
