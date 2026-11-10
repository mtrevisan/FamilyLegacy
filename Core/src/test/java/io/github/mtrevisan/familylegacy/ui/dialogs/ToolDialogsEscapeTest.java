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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.comparison.IndividualFacts;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityLoops;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReport;
import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeDecision;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeField;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.quality.Statistics;
import io.github.mtrevisan.familylegacy.quality.StatisticsCalculator;
import io.github.mtrevisan.familylegacy.tree.GenealogyRepository;
import io.github.mtrevisan.familylegacy.tree.TreeService;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks.BookmarkStore;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;


/**
 * Every dialog of the tools, built one by one, closes with a real press of ESC on a focused component, and only itself.
 * <p>
 * The dialogs are found in the sources (the ones that install the ESC helper), and built with the constructor that can be fed with
 * what a test has: the owner, the model, a record, a tool context, simple values. A dialog that needs something else is listed in
 * {@link #NEEDS_SPECIAL_ARGUMENTS}, and the test fails if the list is out of date.
 */
class ToolDialogsEscapeTest{

	private static final Path SOURCES = Path.of("src", "main", "java");
	private static final String HELPER = "installEscapeToClose(this)";

	/** The dialogs whose constructors take something that is not worth building here: simple results, stores, services. */
	private static final Set<String> NEEDS_SPECIAL_ARGUMENTS = Set.of();


	private static List<Class<?>> discover() throws IOException, ClassNotFoundException{
		final List<Class<?>> classes = new ArrayList<>();
		try(final Stream<Path> files = Files.walk(SOURCES)){
			for(final Path file : files.filter(f -> f.toString().endsWith(".java")).toList()){
				if(!Files.readString(file, StandardCharsets.UTF_8).contains(HELPER))
					continue;

				final String name = SOURCES.relativize(file).toString()
					.replace(File.separatorChar, '.');
				classes.add(Class.forName(name.substring(0, name.length() - ".java".length())));
			}
		}
		return classes;
	}

	/** The value for a parameter of a constructor, or {@code null} if the test cannot make one. */
	private static Object argument(final Parameter parameter, final Window owner, final JPanel inOwner, final FLEFModel model){
		final Class<?> type = parameter.getType();
		if(type.isEnum())
			return type.getEnumConstants()[0];
		if(type == JComponent.class){
			//a view the dialog shows (the panel of a temporal projection takes the focus)
			final JPanel view = new JPanel(){};
			view.setFocusable(true);
			return view;
		}
		if(type == BookmarkStore.class)
			return new BookmarkStore();
		if(type == ImportReport.class)
			return new ImportReport();
		if(type == ConsanguinityReport.class)
			return new ConsanguinityReport(List.of(), List.of(), 0., 0.);
		if(type == ConsanguinityLoops.Result.class)
			return new ConsanguinityLoops.Result("x", null, null, 0., List.of(), false);
		if(type == IndividualFacts.Comparison.class)
			return new IndividualFacts.Comparison(List.of(), List.of(), List.of());
		if(type == TreeService.class)
			return new TreeService(new GenealogyRepository(RelationshipReader.BIOLOGICAL, model), model);
		if(type == MergeField.class)
			return mergeField(model);
		if(type == Statistics.class)
			return new StatisticsCalculator(model).compute(null);
		if(type == Supplier.class){
			final String generic = parameter.getParameterizedType().getTypeName();
			if(generic.contains("List"))
				return (Supplier<?>)List::of;
			if(generic.contains("String"))
				return (Supplier<?>)() -> "x";
			return (Supplier<?>)() -> model;
		}
		if(type == Window.class)
			return owner;
		if(type == FLEFModel.class)
			return model;
		if(type == FLEFRecord.class)
			return model.getRecords().getFirst();
		if(type == ToolContext.class)
			return new ToolContext(model, null, inOwner, null);
		if(type == String.class)
			return "x";
		if(type == boolean.class)
			return false;
		if(type == int.class)
			return 0;
		if(type == List.class)
			return List.of();
		if(type == Map.class)
			return Map.of();
		if(type == Set.class)
			return Set.of();
		if(type == File.class)
			return new File("none.flef");
		if(type == Class.class)
			return IndividualHandler.class;
		if(type == Function.class)
			return (Function<Object, Object>)x -> "x";
		return null;
	}

	/** The constructor of a merge field is not public: a field with no instances on either side is enough for a dialog. */
	private static MergeField mergeField(final FLEFModel model){
		try{
			final Constructor<MergeField> constructor = MergeField.class.getDeclaredConstructor(String.class, List.class, List.class,
				MergeDecision.class, boolean.class);
			constructor.setAccessible(true);
			return constructor.newInstance("x", List.of(model.getRecords().getFirst()), List.of(model.getRecords().getFirst()),
				MergeDecision.values()[0], false);
		}
		catch(final ReflectiveOperationException e){
			return null;
		}
	}

	private static JDialog build(final Class<?> type, final JDialog owner, final JPanel inOwner, final FLEFModel model) throws Exception{
		for(final Constructor<?> constructor : type.getDeclaredConstructors()){
			if(constructor.isSynthetic())
				continue;

			final Parameter[] parameters = constructor.getParameters();
			final Object[] arguments = new Object[parameters.length];
			boolean feasible = true;
			for(int i = 0; i < parameters.length && feasible; i ++){
				arguments[i] = argument(parameters[i], owner, inOwner, model);
				feasible = (arguments[i] != null);
			}
			if(!feasible)
				continue;

			constructor.setAccessible(true);
			final Object dialog = constructor.newInstance(arguments);
			return (JDialog)dialog;
		}
		return null;
	}


	@Test
	void everyToolDialogClosesWithARealEscAndOnlyItself() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		Assumptions.assumeTrue(Files.isDirectory(SOURCES), "The sources are not here");
		final FLEFModel model = DialogsEscapeTest.convert();
		final List<Class<?>> classes = discover();
		Assertions.assertTrue(classes.size() > 30, "only " + classes.size() + " dialogs found: is the scan right?");

		final Set<String> audited = new TreeSet<>();
		final Map<String, String> notBuilt = new TreeMap<>();
		final Map<String, String> notClosed = new TreeMap<>();

		final JDialog owner = new JDialog((Window)null, "owner");
		final JPanel inOwner = new JPanel();
		owner.add(inOwner);
		owner.pack();
		owner.setVisible(true);
		try{
			for(final Class<?> type : classes){
				if(!JDialog.class.isAssignableFrom(type) || Modifier.isAbstract(type.getModifiers()))
					continue;

				final String name = type.getSimpleName();
				final JDialog dialog;
				try{
					dialog = build(type, owner, inOwner, model);
				}
				catch(final Exception | AssertionError e){
					notBuilt.put(name, String.valueOf(e));
					continue;
				}
				if(dialog == null){
					notBuilt.put(name, "no constructor that the test can feed");
					continue;
				}

				dialog.setModalityType(Dialog.ModalityType.MODELESS);
				dialog.setVisible(true);
				DialogsEscapeTest.flushEventQueue();
				try{
					DialogsEscapeTest.typeEscape(dialog);
				}
				catch(final AssertionError e){
					notClosed.put(name, e.getMessage());
					dialog.dispose();
					continue;
				}

				if(dialog.isDisplayable())
					notClosed.put(name, "still displayed after ESC");
				else
					audited.add(name);
				Assertions.assertTrue(owner.isDisplayable(), "ESC in " + name + " closed the dialog below it");
				dialog.dispose();
			}
		}
		finally{
			owner.dispose();
		}

		System.out.println("Tool dialogs closed with a real ESC: " + audited.size() + " " + audited);
		System.out.println("Tool dialogs not built: " + notBuilt);
		final Map<String, String> unexpected = new TreeMap<>(notBuilt);
		unexpected.keySet().removeAll(NEEDS_SPECIAL_ARGUMENTS);
		Assertions.assertTrue(notClosed.isEmpty(), "dialogs not closed by a real ESC press: " + notClosed);
		Assertions.assertTrue(unexpected.isEmpty(), "dialogs that could not be built: " + unexpected);
	}

}
