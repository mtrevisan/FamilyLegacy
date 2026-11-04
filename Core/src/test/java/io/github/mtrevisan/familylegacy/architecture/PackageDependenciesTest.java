/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.architecture;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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


/**
 * Structural rules of the application (the domain, module Domain, and the UI, module Core), checked on the imports of the main sources:
 * <ul>
 * 	<li>no cycle between packages;</li>
 * 	<li>the layers only depend downwards: {@code util} depends on nothing, everything outside {@code ui} depends on the UI only through
 * 	the few packages listed in {@link #UI_ALLOWED_FOR_NON_UI};</li>
 * 	<li>nothing outside the Research Assistant imports it (the main window finds it through {@code ResearchAssistantPort}).</li>
 * </ul>
 * Imports and fully qualified names are looked at: uses inside the same package and reflection are not covered.
 */
class PackageDependenciesTest{

	private static final String ROOT = "io.github.mtrevisan.familylegacy";
	private static final String UI = ROOT + ".ui";
	private static final String UTIL = ROOT + ".util";
	/** The Research Assistant plugs into the application (it is found through a port), nothing else knows it. */
	private static final String ASSISTANT = UI + ".assistant";
	/** What the non-UI code may reach in the UI: nothing (the messages and the preferences are in {@code i18n} and {@code preferences}). */
	private static final Set<String> UI_ALLOWED_FOR_NON_UI = Set.of();

	private static final Pattern PACKAGE = Pattern.compile("(?m)^package\\s+([\\w.]+);");
	private static final Pattern COMMENT = Pattern.compile("(?s)/\\*.*?\\*/|(?m)//.*$");
	private static final Pattern HEADER_LINE = Pattern.compile("(?m)^(import|package)\\s.*$");
	/** A class used by its fully qualified name, with no import: it is a dependency all the same. */
	private static final Pattern QUALIFIED_USE = Pattern.compile("(?<![\\w.])" + Pattern.quote(ROOT) + "\\.[a-z0-9_.]*[A-Z]\\w*");
	private static final Pattern IMPORT = Pattern.compile("(?m)^import\\s+(static\\s+)?([\\w.]+?)(\\.\\*)?;");

	private static final Map<String, Set<String>> DEPENDENCIES = new TreeMap<>();
	/** The packages (outside the UI) that import Swing, with how many of their classes do. */
	private static final Map<String, Integer> SWING_USERS = new TreeMap<>();


	@BeforeAll
	static void scan() throws IOException{
		//the domain (module Domain) and the UI (this module) are seen together: the dependencies between them are the point
		final List<Path> sources = new ArrayList<>();
		for(final Path root : List.of(Path.of("src", "main", "java"), Path.of("..", "Domain", "src", "main", "java")))
			try(final Stream<Path> walk = Files.walk(root)){
				walk.filter(path -> path.toString().endsWith(".java") && !path.endsWith("module-info.java"))
					.forEach(sources::add);
			}

		final Map<Path, String> packageOf = new HashMap<>();
		final Set<String> classes = new HashSet<>();
		final Map<Path, String> texts = new HashMap<>();
		for(final Path source : sources){
			final String text = Files.readString(source, StandardCharsets.UTF_8);
			final Matcher matcher = PACKAGE.matcher(text);
			if(!matcher.find())
				continue;

			final String pkg = matcher.group(1);
			texts.put(source, text);
			packageOf.put(source, pkg);
			DEPENDENCIES.computeIfAbsent(pkg, k -> new TreeSet<>());
			classes.add(pkg + "." + Strings.CS.removeEnd(source.getFileName().toString(), ".java"));
		}

		for(final Map.Entry<Path, String> entry : texts.entrySet()){
			final String pkg = packageOf.get(entry.getKey());
			if(entry.getValue().contains("\nimport javax.swing."))
				SWING_USERS.merge(pkg, 1, Integer::sum);
			final Matcher matcher = IMPORT.matcher(entry.getValue());
			while(matcher.find()){
				final String target = targetPackage(matcher.group(2), matcher.group(1) == null && matcher.group(3) != null, classes);
				if(target != null && !target.equals(pkg))
					DEPENDENCIES.get(pkg).add(target);
			}

			final String code = HEADER_LINE.matcher(COMMENT.matcher(entry.getValue()).replaceAll("")).replaceAll("");
			final Matcher qualified = QUALIFIED_USE.matcher(code);
			while(qualified.find()){
				final String target = targetPackage(qualified.group(), false, classes);
				if(target != null && !target.equals(pkg))
					DEPENDENCIES.get(pkg).add(target);
			}
		}
	}

	private static String targetPackage(final String imported, final boolean packageWildcard, final Set<String> classes){
		if(packageWildcard && DEPENDENCIES.containsKey(imported))
			return imported;

		String candidate = imported;
		while(candidate.contains(".")){
			if(classes.contains(candidate))
				return StringUtils.substringBeforeLast(candidate, ".");
			candidate = StringUtils.substringBeforeLast(candidate, ".");
		}
		return null;
	}


	@Test
	void noCycleBetweenPackages(){
		final List<List<String>> cycles = stronglyConnectedComponents(DEPENDENCIES).stream()
			.filter(component -> component.size() > 1)
			.map(component -> component.stream().map(PackageDependenciesTest::shortName).sorted().toList())
			.toList();

		Assertions.assertTrue(cycles.isEmpty(), "package cycles: " + cycles);
	}

	/**
	 * What IntelliJ's dependency matrix shows when the packages are collapsed to their first {@code depth} segments ({@code ui},
	 * {@code ui.dialogs}, {@code ui.dialogs.records}...): a package and its sub-packages count as one, so a cycle between a package and its
	 * children, or between two branches, is seen here even if no two leaf packages are in a cycle.
	 */
	@Test
	void noCycleBetweenGroupsOfPackages(){
		final List<String> cycles = new ArrayList<>();
		for(int depth = 1; depth <= 9; depth ++){
			final Map<String, Set<String>> groups = new TreeMap<>();
			for(final Map.Entry<String, Set<String>> entry : DEPENDENCIES.entrySet()){
				final String from = groupOf(entry.getKey(), depth);
				final Set<String> targets = groups.computeIfAbsent(from, k -> new TreeSet<>());
				for(final String target : entry.getValue()){
					final String to = groupOf(target, depth);
					if(!to.equals(from))
						targets.add(to);
				}
			}
			for(final Set<String> targets : new ArrayList<>(groups.values()))
				for(final String target : targets)
					groups.computeIfAbsent(target, k -> new TreeSet<>());

			for(final List<String> component : stronglyConnectedComponents(groups))
				if(component.size() > 1)
					cycles.add("depth " + depth + ": " + component.stream().sorted().toList());
		}

		Assertions.assertTrue(cycles.isEmpty(), "cycles between groups of packages: " + cycles);
	}

	@Test
	void nonUiCodeDoesNotUseSwing(){
		final List<String> violations = new ArrayList<>();
		for(final Map.Entry<String, Integer> entry : SWING_USERS.entrySet())
			if(!isIn(entry.getKey(), UI) && !entry.getKey().equals(ROOT))
				violations.add(shortName(entry.getKey()) + " (" + entry.getValue() + ")");

		Assertions.assertTrue(violations.isEmpty(), "the domain must not import javax.swing: " + violations);
	}

	@Test
	void utilDependsOnNothingOfTheProject(){
		final List<String> violations = new ArrayList<>();
		for(final Map.Entry<String, Set<String>> entry : DEPENDENCIES.entrySet())
			if(isIn(entry.getKey(), UTIL))
				for(final String target : entry.getValue())
					if(!isIn(target, UTIL))
						violations.add(shortName(entry.getKey()) + " -> " + shortName(target));

		Assertions.assertTrue(violations.isEmpty(), "util must be a leaf: " + violations);
	}

	@Test
	void nonUiCodeReachesTheUiOnlyThroughAllowedPackages(){
		final List<String> violations = new ArrayList<>();
		for(final Map.Entry<String, Set<String>> entry : DEPENDENCIES.entrySet()){
			final String from = entry.getKey();
			if(isIn(from, UI) || from.equals(ROOT))
				continue;

			for(final String target : entry.getValue())
				if(isIn(target, UI) && UI_ALLOWED_FOR_NON_UI.stream().noneMatch(allowed -> isIn(target, allowed)))
					violations.add(shortName(from) + " -> " + shortName(target));
		}

		Assertions.assertTrue(violations.isEmpty(), "the domain must not depend on the UI: " + violations);
	}


	@Test
	void onlyTheAssistantKnowsTheAssistant(){
		final List<String> violations = new ArrayList<>();
		for(final Map.Entry<String, Set<String>> entry : DEPENDENCIES.entrySet())
			if(!isIn(entry.getKey(), ASSISTANT))
				for(final String target : entry.getValue())
					if(isIn(target, ASSISTANT))
						violations.add(shortName(entry.getKey()) + " -> " + shortName(target));

		Assertions.assertTrue(violations.isEmpty(), "the assistant must be reached through the port: " + violations);
	}


	private static boolean isIn(final String pkg, final String root){
		return pkg.equals(root) || pkg.startsWith(root + ".");
	}

	private static String groupOf(final String pkg, final int depth){
		if(pkg.equals(ROOT))
			return "(root)";

		final String[] parts = StringUtils.split(Strings.CS.removeStart(pkg, ROOT + "."), '.');
		return StringUtils.join(parts, '.', 0, Math.min(depth, parts.length));
	}

	private static String shortName(final String pkg){
		return Strings.CS.removeStart(pkg, ROOT + ".");
	}

	private static List<List<String>> stronglyConnectedComponents(final Map<String, Set<String>> graph){
		final Map<String, Integer> index = new HashMap<>();
		final Map<String, Integer> lowLink = new HashMap<>();
		final Deque<String> stack = new ArrayDeque<>();
		final Set<String> onStack = new HashSet<>();
		final List<List<String>> components = new ArrayList<>();
		for(final String pkg : graph.keySet())
			if(!index.containsKey(pkg))
				visit(graph, pkg, index, lowLink, stack, onStack, components);
		return components;
	}

	private static void visit(final Map<String, Set<String>> graph, final String pkg, final Map<String, Integer> index, final Map<String, Integer> lowLink,
			final Deque<String> stack, final Set<String> onStack, final List<List<String>> components){
		index.put(pkg, index.size());
		lowLink.put(pkg, index.get(pkg));
		stack.push(pkg);
		onStack.add(pkg);
		for(final String next : graph.get(pkg)){
			if(!graph.containsKey(next))
				continue;

			if(!index.containsKey(next)){
				visit(graph, next, index, lowLink, stack, onStack, components);
				lowLink.put(pkg, Math.min(lowLink.get(pkg), lowLink.get(next)));
			}
			else if(onStack.contains(next))
				lowLink.put(pkg, Math.min(lowLink.get(pkg), index.get(next)));
		}
		if(lowLink.get(pkg).equals(index.get(pkg))){
			final List<String> component = new ArrayList<>();
			String member;
			do{
				member = stack.pop();
				onStack.remove(member);
				component.add(member);
			}while(!member.equals(pkg));
			components.add(component);
		}
	}

}
