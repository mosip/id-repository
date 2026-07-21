package io.mosip.idrepository.test.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Builds temporary classpath jars for bootstrap {@link ClassLoader} unit tests.
 */
public final class ClassLoaderTestSupport {

	private ClassLoaderTestSupport() {
	}

	public static Path createJar(Path jarPath, Map<String, byte[]> entries) throws IOException {
		Files.createDirectories(jarPath.getParent());
		try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
			for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
				jarOutputStream.putNextEntry(new JarEntry(entry.getKey()));
				jarOutputStream.write(entry.getValue());
				jarOutputStream.closeEntry();
			}
		}
		return jarPath;
	}

	public static Path compileClassJar(Path jarPath, String className, String source) throws IOException {
		return compileClassJar(jarPath, Map.of(className, source), Map.of());
	}

	public static Path compileClassJar(Path jarPath, Map<String, String> sources) throws IOException {
		return compileClassJar(jarPath, sources, Map.of());
	}

	public static Path compileClassJar(Path jarPath, Map<String, String> sources, Map<String, byte[]> extraEntries)
			throws IOException {
		Path workDir = Files.createTempDirectory("classloader-test-");
		Path classesDir = workDir.resolve("classes");
		Files.createDirectories(classesDir);
		for (Map.Entry<String, String> source : sources.entrySet()) {
			compileSource(source.getKey(), source.getValue(), classesDir);
		}
		Map<String, byte[]> entries = readClassEntries(classesDir);
		entries.putAll(extraEntries);
		return createJar(jarPath, entries);
	}

	private static void compileSource(String className, String source, Path classesDir) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new IllegalStateException("JDK compiler is required for classloader tests");
		}
		try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
			List<String> options = List.of("-d", classesDir.toString());
			JavaFileObject sourceFile = new SimpleJavaFileObject(
					URI.create("string:///" + className.replace('.', '/') + ".java"), JavaFileObject.Kind.SOURCE) {
				@Override
				public CharSequence getCharContent(boolean ignoreEncodingErrors) {
					return source;
				}
			};
			Boolean compiled = compiler.getTask(null, fileManager, null, options, null, List.of(sourceFile)).call();
			if (!Boolean.TRUE.equals(compiled)) {
				throw new IllegalStateException("Failed to compile " + className);
			}
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static Map<String, byte[]> readClassEntries(Path classesDir) throws IOException {
		Map<String, byte[]> entries = new java.util.LinkedHashMap<>();
		try (Stream<Path> paths = Files.walk(classesDir)) {
			paths.filter(Files::isRegularFile).forEach(path -> {
				try {
					String entryName = classesDir.relativize(path).toString().replace('\\', '/');
					entries.put(entryName, Files.readAllBytes(path));
				} catch (IOException ex) {
					throw new UncheckedIOException(ex);
				}
			});
		}
		return entries;
	}

	public static String joinClasspath(Path... entries) {
		List<String> paths = new ArrayList<>();
		for (Path entry : entries) {
			paths.add(entry.toAbsolutePath().toString());
		}
		return String.join(java.io.File.pathSeparator, paths);
	}
}
