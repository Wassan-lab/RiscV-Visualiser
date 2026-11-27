import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.*;
import javax.tools.ToolProvider;

/** Run with Java 21: java Build.java [test|run]. No extra build tool is needed. */
public class Build {
    private static final Path CLASSES = Path.of("build", "classes");
    private static final Path JAR = Path.of("dist", "member3-preview.jar");

    public static void main(String[] args) throws Exception {
        String action = args.length == 0 ? "build" : args[0];
        if (!List.of("build", "test", "run").contains(action)) {
            throw new IllegalArgumentException("Use: java Build.java [test|run]");
        }
        if (!Files.isDirectory(Path.of("src", "main", "java"))) {
            throw new IllegalStateException("Run this command from the project folder containing Build.java.");
        }
        // Rebuild cleanly so removed classes cannot remain in the package.
        if (Files.exists(CLASSES)) {
            try (var files = Files.walk(CLASSES)) {
                for (Path file : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
        Files.createDirectories(CLASSES);
        compile(Path.of("src", "main", "java"), CLASSES, null);
        Files.createDirectories(JAR.getParent());
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "team.riscv.preview.Member3Preview");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(JAR), manifest);
             var files = Files.walk(CLASSES)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                String name = CLASSES.relativize(file).toString().replace('\\', '/');
                JarEntry entry = new JarEntry(name);
                jar.putNextEntry(entry);
                Files.copy(file, jar);
                jar.closeEntry();
            }
        }
        System.out.println("Built " + JAR + " (Member 3 preview, not the final simulator)");
        if (action.equals("test")) {
            Path tests = Path.of("build", "test-classes");
            Files.createDirectories(tests);
            compile(Path.of("src", "test", "java"), tests, CLASSES.toString());
            runJava("-Djava.awt.headless=true", "-cp", CLASSES + java.io.File.pathSeparator + tests,
                    "team.riscv.tests.Member3Tests");
        } else if (action.equals("run")) {
            runJava("-jar", JAR.toString());
        }
    }

    private static void compile(Path source, Path output, String classpath) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Install a Java 21 JDK to build the source.");
        List<String> options = new ArrayList<>(List.of("-source", "21", "-target", "21", "-encoding", "UTF-8",
                "-Xlint:all,-serial", "-d", output.toString()));
        if (classpath != null) options.addAll(List.of("-cp", classpath));
        try (var files = Files.walk(source)) {
            options.addAll(files.filter(p -> p.toString().endsWith(".java")).sorted().map(Path::toString).toList());
        }
        if (compiler.run(null, null, null, options.toArray(String[]::new)) != 0) {
            throw new IllegalStateException("Compilation failed; see the messages above.");
        }
    }

    private static void runJava(String... args) throws Exception {
        String binary = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", binary).toString());
        command.addAll(List.of(args));
        int result = new ProcessBuilder(command).inheritIO().start().waitFor();
        if (result != 0) throw new IllegalStateException("Java process failed with exit code " + result);
    }
}
