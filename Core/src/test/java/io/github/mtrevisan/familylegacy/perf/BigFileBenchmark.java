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
package io.github.mtrevisan.familylegacy.perf;

import io.github.mtrevisan.familylegacy.consanguinity.KinshipCalculator;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.ModelHistory;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.PrivacyExporter;
import io.github.mtrevisan.familylegacy.privacy.PrivacyPolicy;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.reports.FamilyBookGenerator;
import io.github.mtrevisan.familylegacy.reports.ReportConfig;
import io.github.mtrevisan.familylegacy.reports.ReportGenerator;
import io.github.mtrevisan.familylegacy.reports.ReportOption;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.familylegacy.reports.renderers.ReportRenderers;
import io.github.mtrevisan.familylegacy.search.SearchCriteria;
import io.github.mtrevisan.familylegacy.search.SearchMode;
import io.github.mtrevisan.familylegacy.tree.GenealogyRepository;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.TreeLayout;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.IndividualTreeGraphPanel;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.layout.TreeLayoutEngine;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.layout.sugiyama.GraphLayoutEngine;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.SearchService;
import io.github.mtrevisan.familylegacy.util.AtomicFileWriter;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportRenderer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;


/**
 * How long the program takes on a big file: opening, writing, validating, exporting, editing (the history and the autosave), searching an
 * individual, the kinship, the tree, the reports, the family book, and the memory. It prints the times, it asserts nothing about them:
 * it is to be read, and to be compared with the figures of the section <em>Performance on a large file</em> of the README.
 * <p>
 * It does not run with the other tests (a file of 50 000 individuals takes minutes): {@code mvn test -Dtest=BigFileBenchmark -Dperf=true},
 * with {@code -Dperf.individuals=N} for another size and {@code -Dperf.book=true} to build the whole family book too. The file is made by
 * {@link SyntheticFamily} (150 000 events, 150 000 relationships and 300 places for 50 000 individuals), written in {@code target/perf}.
 * The privacy policy of the preferences of the user must be the one that shows everything. A test is skipped if what it needs is not
 * there (a display for the tree, the PDF plug-in for the PDF).
 * </p>
 */
@EnabledIfSystemProperty(named = "perf", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BigFileBenchmark{

	private static final int INDIVIDUALS = Integer.getInteger("perf.individuals", 50_000);
	private static final Path DIRECTORY = Path.of("target", "perf");

	private SyntheticFamily family;
	private Path file;
	private FLEFModel model;


	private static <T> T time(final String label, final int runs, final Supplier<T> action){
		T result = null;
		final StringBuilder times = new StringBuilder();
		for(int i = 0; i < runs; i ++){
			final long start = System.nanoTime();
			result = action.get();
			times.append(i == 0? "": " / ").append((System.nanoTime() - start) / 1_000_000).append(" ms");
		}
		System.out.printf("%-72s %s%n", label, times);
		return result;
	}

	private static long usedMegabytes(){
		System.gc();
		System.gc();
		final Runtime runtime = Runtime.getRuntime();
		return (runtime.totalMemory() - runtime.freeMemory()) >> 20;
	}


	@BeforeAll
	void prepare() throws IOException{
		Assumptions.assumeTrue(PrivacyService.policy() == PrivacyPolicy.SHOW_ALL,
			"the privacy policy of the preferences must be the one that shows everything");

		Files.createDirectories(DIRECTORY);
		family = new SyntheticFamily();
		final long start = System.nanoTime();
		final FLEFModel generated = family.generate(INDIVIDUALS, 150, 300);
		family.padRelationships(150_000L * INDIVIDUALS / 50_000);
		family.padEvents(150_000L * INDIVIDUALS / 50_000);
		System.out.printf("generated in %d ms: individuals=%d events=%d participations=%d relationships=%d records=%d generations=%d%n",
			(System.nanoTime() - start) / 1_000_000, family.individuals, family.events, family.participations, family.relationships,
			generated.getRecordCount(), family.maxGen + 1);

		file = DIRECTORY.resolve("big.flef");
		FLEFWriter.create().write(generated, file, false);
		System.out.printf("file: %d MB%n", Files.size(file) >> 20);
		model = new FLEFParser().parse(file, (percent, message) -> {});
	}


	@Test
	@Order(1)
	void openWriteValidateExport() throws IOException{
		time("open the file", 3, () -> {
			try{
				return new FLEFParser().parse(file, (percent, message) -> {});
			}
			catch(final IOException e){
				throw new RuntimeException(e);
			}
		});
		time("write the file (indented)", 2, () -> write(FLEFWriter.create(), DIRECTORY.resolve("write.flef")));
		time("write the file (compact)", 2, () -> write(FLEFWriter.createCompact(), DIRECTORY.resolve("write-compact.flef")));
		final Path compressed = DIRECTORY.resolve("write.flef.gz");
		time("write the file (gzip)", 1, () -> {
			try{
				FLEFWriter.create().write(model, compressed, true);
				return 0;
			}
			catch(final IOException e){
				throw new RuntimeException(e);
			}
		});
		time("open the gzip file", 1, () -> {
			try{
				return new FLEFParser().parse(compressed, (percent, message) -> {});
			}
			catch(final IOException e){
				throw new RuntimeException(e);
			}
		});

		final FLEFValidator validator = new FLEFValidator(MergeGrammar.instance());
		time("validate: schema", 2, () -> validator.validateSchema(model));
		time("validate: integrity", 2, () -> validator.validateIntegrity(model));
		time("validate: business rules", 1, () -> validator.validateBusinessRules(model));

		for(final PrivacyPolicy policy : PrivacyPolicy.values())
			time("export FLEF, policy " + policy, 1, () -> {
				try{
					final Path target = DIRECTORY.resolve("export.flef");
					AtomicFileWriter.write(target, StandardCharsets.UTF_8,
						writer -> PrivacyExporter.write(model, policy, FLEFWriter.create(), writer));
					return 0;
				}
				catch(final IOException e){
					throw new RuntimeException(e);
				}
			});
	}

	private int write(final FLEFWriter writer, final Path target){
		try{
			writer.write(model, target, false);
			return 0;
		}
		catch(final IOException e){
			throw new RuntimeException(e);
		}
	}

	@Test
	@Order(2)
	void editWithTheHistoryAndTheAutosave(){
		final FLEFModel edited = time("open the file (for the edits)", 1, () -> {
			try{
				return new FLEFParser().parse(file, (percent, message) -> {});
			}
			catch(final IOException e){
				throw new RuntimeException(e);
			}
		});
		final ModelHistory history = time("new ModelHistory", 1, () -> new ModelHistory(edited));
		for(int i = 0; i < 4; i ++){
			final int edit = i;
			edited.getRecordById("I" + (100 + i)).addChild(FLEFRecord.createChildWithTagAndValue("note", "edit " + i));
			time("edit " + i + ": checkpoint", 1, () -> {
				history.checkpoint();
				return 0;
			});
			final ModelHistory.SaveTicket ticket = time("edit " + i + ": prepareSave", 1, history::prepareSave);
			time("edit " + i + ": canUndo + isModified (nothing changed)", 1, () -> history.canUndo() | history.isModified());
			time("edit " + i + ": write of the autosave", 1, () -> {
				try{
					AtomicFileWriter.write(DIRECTORY.resolve("autosave.flef"), StandardCharsets.UTF_8,
						writer -> FLEFWriter.createCompact().write(ticket.view(), writer));
					return edit;
				}
				catch(final IOException e){
					throw new RuntimeException(e);
				}
			});
			history.markSaved(ticket);
		}
		time("undo", 1, history::undo);
	}

	@Test
	@Order(3)
	void searchAnIndividual(){
		final SearchService service = new SearchService(model);
		final Object[][] cases = {
			{"a surname", "Familyroni", EnumSet.noneOf(SearchMode.class)},
			{"a given name and a surname", "Givenbaro Familyroni", EnumSet.noneOf(SearchMode.class)},
			{"a given name", "Givenroba", EnumSet.noneOf(SearchMode.class)},
			{"a surname with a typo (fuzzy)", "Famlyroni", EnumSet.of(SearchMode.FUZZY)},
			{"a surname (whole word)", "Familyroni", EnumSet.of(SearchMode.WHOLE_WORD)},
			{"a surname (notes and citations too)", "Familyroni", EnumSet.of(SearchMode.NOTES_AND_CITATIONS)},
			{"no text, every individual", "", EnumSet.noneOf(SearchMode.class)}
		};
		for(final Object[] c : cases){
			@SuppressWarnings("unchecked")
			final SearchCriteria criteria = new SearchCriteria(IndividualReader.TYPE, (String)c[1], (Set<SearchMode>)c[2]);
			final List<?> found = time("search an individual: " + c[0], 3, () -> service.search(criteria));
			System.out.println("   found: " + found.size());
		}
	}

	@Test
	@Order(4)
	void kinship(){
		final List<SyntheticFamily.Person> deep = new ArrayList<>();
		for(final SyntheticFamily.Person person : family.people)
			if(person.gen >= family.maxGen - 1 && person.coupleOfParents >= 0)
				deep.add(person);
		final Random random = new Random(7);

		final KinshipCalculator probe = time("KinshipCalculator.forModel", 1, () -> KinshipCalculator.forModel(model));
		final List<String[]> related = new ArrayList<>();
		for(int tried = 0; related.size() < 200 && tried < 20_000; tried ++){
			final SyntheticFamily.Person a = deep.get(random.nextInt(deep.size()));
			final SyntheticFamily.Person b = deep.get(random.nextInt(deep.size()));
			if(a != b && probe.calculate(a.id, b.id).isRelated())
				related.add(new String[]{a.id, b.id});
		}
		System.out.println("   related pairs: " + related.size());
		time("calculate 200 related pairs (a new calculator)", 1, () -> {
			final KinshipCalculator calculator = KinshipCalculator.forModel(model);
			int steps = 0;
			for(final String[] pair : related)
				steps += calculator.calculate(pair[0], pair[1]).totalSteps();
			return steps;
		});
		final KinshipCalculator warm = KinshipCalculator.forModel(model);
		time("calculate 200 related pairs (the same calculator)", 3, () -> {
			int steps = 0;
			for(final String[] pair : related)
				steps += warm.calculate(pair[0], pair[1]).totalSteps();
			return steps;
		});
		final SyntheticFamily.Person reference = deep.get(1);
		final List<SyntheticFamily.Person> branch = family.people.subList(0, Math.min(5000, family.people.size()));
		time("the kinship term of 5 000 people against one", 1, () -> {
			final KinshipCalculator calculator = KinshipCalculator.forModel(model);
			int length = 0;
			for(final SyntheticFamily.Person person : branch)
				length += calculator.shortTerm(person.id, reference.id).length();
			return length;
		});
	}

	@Test
	@Order(5)
	void kinshipInAClosedPopulation(){
		//a few founders and marriages only among the descendants: the paths between two people are astronomically many
		for(final int[] configuration : new int[][]{{10_000, 150}, {20_000, 150}, {20_000, 60}}){
			final SyntheticFamily closed = new SyntheticFamily();
			closed.inGenPercent = 100;
			final FLEFModel closedModel = closed.generate(configuration[0], configuration[1], 50);
			final List<SyntheticFamily.Person> last = new ArrayList<>();
			for(final SyntheticFamily.Person person : closed.people)
				if(person.gen >= closed.maxGen - 1 && person.p1 != null)
					last.add(person);
			final Random random = new Random(3);
			final KinshipCalculator calculator = KinshipCalculator.forModel(closedModel);
			time((closed.maxGen + 1) + " generations: 100 pairs", 1, () -> {
				int related = 0;
				for(int i = 0; i < 100; i ++)
					if(calculator.calculate(last.get(random.nextInt(last.size())).id, last.get(random.nextInt(last.size())).id).isRelated())
						related ++;
				return related;
			});
		}
	}

	@Test
	@Order(6)
	void tree() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "the tree needs a display");

		final String root = family.richestAncestry().id;
		final GenealogyRepository repository = time("GenealogyRepository", 1,
			() -> new GenealogyRepository(RelationshipReader.BIOLOGICAL, model));
		for(final String engine : new String[]{"TreeLayoutEngine", "GraphLayoutEngine"}){
			final IndividualTreeGraphPanel[] panel = new IndividualTreeGraphPanel[1];
			final long start = System.nanoTime();
			SwingUtilities.invokeAndWait(() -> panel[0] = new IndividualTreeGraphPanel(TreeLayout.VERTICAL,
				(engine.startsWith("Tree")? new TreeLayoutEngine(): new GraphLayoutEngine()), repository, model).withShowPartner());
			System.out.printf("%-72s %d ms%n", engine + ": construct the panel", (System.nanoTime() - start) / 1_000_000);
			final JFrame[] frame = new JFrame[1];
			SwingUtilities.invokeAndWait(() -> {
				frame[0] = new JFrame();
				frame[0].add(panel[0]);
				frame[0].setSize(1600, 900);
				frame[0].setVisible(true);
			});
			for(final int generations : new int[]{3, 5, 7, 9})
				for(int run = 0; run < 2; run ++){
					final long loadStart = System.nanoTime();
					SwingUtilities.invokeAndWait(() -> panel[0].load(root, generations));
					SwingUtilities.invokeAndWait(() -> {});
					final long loaded = System.nanoTime();
					SwingUtilities.invokeAndWait(() -> {
						final var size = panel[0].getSize();
						final BufferedImage image = new BufferedImage(Math.max(1, size.width), Math.max(1, size.height),
							BufferedImage.TYPE_INT_RGB);
						panel[0].paint(image.getGraphics());
					});
					System.out.printf("%-72s load %d ms, paint %d ms%n", engine + ": " + generations + " generations, run " + run,
						(loaded - loadStart) / 1_000_000, (System.nanoTime() - loaded) / 1_000_000);
				}
			SwingUtilities.invokeAndWait(() -> frame[0].dispose());
		}
	}

	@Test
	@Order(7)
	void reportsOfAPerson() throws IOException{
		final ReportRenderer renderer = ReportRenderers.available().stream()
			.filter(candidate -> "pdf".equals(candidate.id()))
			.findFirst()
			.orElse(null);
		Assumptions.assumeTrue(renderer != null, "the PDF plug-in is not there");

		for(final SyntheticFamily.Person person : new SyntheticFamily.Person[]{family.richestAncestry(), family.richestDescendance()}){
			System.out.println("person " + person.id + ": " + SyntheticFamily.ancestors(person) + " ancestors, "
				+ SyntheticFamily.descendants(person) + " descendants");
			final FLEFRecord root = model.getRecordById(person.id);
			for(final int generations : new int[]{3, 6, 9}){
				final ReportConfig config = ReportConfig.uniform(true, ReportLanguage.ENGLISH, PrivacyPolicy.SHOW_ALL)
					.with(ReportOption.GENERATIONS, generations);
				final ReportDocument document = time("report, " + generations + " generations: build", 1,
					() -> new ReportGenerator(model, root, config).generate());
				render(renderer, document, DIRECTORY.resolve("report.pdf"));
			}
		}
	}

	@Test
	@Order(8)
	@EnabledIfSystemProperty(named = "perf.book", matches = "true")
	void familyBook() throws IOException{
		final ReportRenderer renderer = ReportRenderers.available().stream()
			.filter(candidate -> "pdf".equals(candidate.id()))
			.findFirst()
			.orElse(null);
		Assumptions.assumeTrue(renderer != null, "the PDF plug-in is not there");

		final ReportDocument document = time("family book of everybody: build", 1,
			() -> new FamilyBookGenerator(model, ReportConfig.defaults()).generate());
		System.out.println("   sections: " + document.sections().size());
		render(renderer, document, DIRECTORY.resolve("book.pdf"));
	}

	private static void render(final ReportRenderer renderer, final ReportDocument document, final Path target){
		time("   render the PDF", 1, () -> {
			try{
				renderer.render(document, target);
				return Files.size(target);
			}
			catch(final IOException e){
				throw new RuntimeException(e);
			}
		});
		try{
			System.out.println("   size: " + (Files.size(target) >> 10) + " KB");
		}
		catch(final IOException ignored){
			//nothing to tell
		}
	}

	@Test
	@Order(9)
	void memory(){
		model = null;
		final long base = usedMegabytes();
		final FLEFModel parsed;
		try{
			parsed = new FLEFParser().parse(file, (percent, message) -> {});
		}
		catch(final IOException e){
			throw new RuntimeException(e);
		}
		final long modelSize = usedMegabytes() - base;
		final ModelHistory history = new ModelHistory(parsed);
		final long withHistory = usedMegabytes() - base;
		final ModelHistory.SaveTicket ticket = history.prepareSave();
		final long withView = usedMegabytes() - base;
		System.out.printf("memory: the model %d MB, with the history %d MB, with the view for the save %d MB%n", modelSize, withHistory,
			withView);
		Assertions.assertEquals(parsed.getRecordCount(), ticket.view().getRecordCount());
	}

}
