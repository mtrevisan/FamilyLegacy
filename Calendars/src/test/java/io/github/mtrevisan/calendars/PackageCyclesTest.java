package io.github.mtrevisan.calendars;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


/** No cycle between the packages of the module, checked on the imports of the main sources. */
class PackageCyclesTest{

	private static final Pattern PACKAGE = Pattern.compile("(?m)^package\\s+([\\w.]+);");
	private static final Pattern IMPORT = Pattern.compile("(?m)^import\\s+(static\\s+)?([\\w.]+?)(\\.\\*)?;");


	@Test
	void noCycleBetweenPackages() throws IOException{
		final Map<String, Set<String>> dependencies = new TreeMap<>();
		final Map<Path, String> texts = new HashMap<>();
		final Set<String> classes = new HashSet<>();
		try(final Stream<Path> walk = Files.walk(Path.of("src", "main", "java"))){
			for(final Path source : walk.filter(p -> p.toString().endsWith(".java") && !p.endsWith("module-info.java")).toList()){
				final String text = Files.readString(source, StandardCharsets.UTF_8);
				final Matcher matcher = PACKAGE.matcher(text);
				if(matcher.find()){
					texts.put(source, text);
					final String name = source.getFileName().toString();
					dependencies.computeIfAbsent(matcher.group(1), k -> new TreeSet<>());
					classes.add(matcher.group(1) + "." + name.substring(0, name.length() - ".java".length()));
				}
			}
		}
		for(final String text : texts.values()){
			final String pkg = PACKAGE.matcher(text).results().findFirst().orElseThrow().group(1);
			final Matcher matcher = IMPORT.matcher(text);
			while(matcher.find()){
				final String name = matcher.group(2);
				final String target;
				if(matcher.group(3) != null && matcher.group(1) == null)
					target = name;
				else{
					String found = null;
					String candidate = name;
					while(found == null && candidate.indexOf('.') > 0){
						if(classes.contains(candidate))
							found = candidate.substring(0, candidate.lastIndexOf('.'));
						candidate = candidate.substring(0, candidate.lastIndexOf('.'));
					}
					target = found;
				}
				if(target != null && !target.equals(pkg) && dependencies.containsKey(target))
					dependencies.get(pkg).add(target);
			}
		}

		final List<String> cycles = new ArrayList<>();
		final Set<String> done = new HashSet<>();
		for(final String start : dependencies.keySet())
			visit(start, new ArrayList<>(), done, dependencies, cycles);
		Assertions.assertTrue(cycles.isEmpty(), "Cycles between packages: " + cycles);
	}

	private static void visit(final String node, final List<String> path, final Set<String> done,
			final Map<String, Set<String>> dependencies, final List<String> cycles){
		final int index = path.indexOf(node);
		if(index >= 0){
			cycles.add(String.join(" -> ", path.subList(index, path.size())) + " -> " + node);
			return;
		}
		if(!done.add(node))
			return;

		path.add(node);
		for(final String next : dependencies.get(node))
			visit(next, path, done, dependencies, cycles);
		path.remove(path.size() - 1);
	}

}
