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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.index.RelationIndex;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;


/**
 * Generates the "Number of Ancestors" report of an individual: for each generation, how many ancestors the tree could have and how many
 * the file knows, how many different individuals they are (the pedigree collapse: a person who is an ancestor by more than one line fills
 * more than one place), and who the individuals that appear more than once are.
 * <p>
 * The generation is counted from the subject (1 is the parents). An individual the privacy policy hides is not an ancestor. A place of
 * the tree is filled once for every line through which an individual is an ancestor, so the known places of a generation are never more
 * than the possible ones.
 * </p>
 */
public final class PedigreeCollapseGenerator{

	/** The places of the tree double at every generation: this keeps them in a {@code long} (and stops a loop in a corrupt file). */
	static final int MAX_GENERATIONS = 40;
	/** How many repeated individuals are listed at most. */
	static final int MAX_REPEATED = 50;
	private static final String NO_VALUE = "—";


	/**
	 * One generation of the tree.
	 *
	 * @param generation	1 is the parents
	 * @param possible	the places of the tree in the generation (2, 4, 8, ...)
	 * @param known	the places the file fills (an individual counted once for every line)
	 * @param different	the different individuals in them
	 */
	record Level(int generation, long possible, long known, int different){}

	/**
	 * An individual that is an ancestor by more than one line.
	 *
	 * @param person	the individual
	 * @param times	the lines through which they are an ancestor
	 * @param generations	the generations they are in
	 */
	record Repeated(FLEFRecord person, long times, List<Integer> generations){}

	/**
	 * @param levels	the generations, the nearest first
	 * @param different	the different individuals in all the generations (one in two generations is counted once)
	 * @param repeated	the individuals that are ancestors by more than one line, the most repeated first
	 */
	record Result(List<Level> levels, int different, List<Repeated> repeated){

		long possible(){
			return levels.stream().mapToLong(Level::possible).sum();
		}

		long known(){
			return levels.stream().mapToLong(Level::known).sum();
		}
	}


	private final FLEFModel model;
	private final FLEFRecord root;
	private final ReportConfig config;


	public PedigreeCollapseGenerator(final FLEFModel model, final FLEFRecord root, final ReportConfig config){
		this.model = Objects.requireNonNull(model, "model");
		this.root = Objects.requireNonNull(root, "root");
		this.config = Objects.requireNonNull(config, "config");
	}


	public ReportDocument generate(){
		return generate(ReportProgressListener.NOOP);
	}

	public ReportDocument generate(final ReportProgressListener listener){
		final ReportContext ctx = ReportContext.build(model, root, config);
		final ReportLabels labels = ctx.labels;
		final Locale locale = labels.language().locale();
		final String title = labels.sections().pedigreeCollapseTitle();
		final String language = locale.toLanguageTag();

		final List<ReportSection> sections = new ArrayList<>(new HeaderSection(ctx).build());
		sections.add(new ReportSection.Heading(1, labels.sections().overview()));
		listener.onProgress(10, null);

		final Result result = compute(ctx.index, root, config.generations());
		listener.onProgress(70, null);

		if(result.levels().isEmpty())
			sections.add(new ReportSection.Paragraph(labels.sections().pedigreeCollapseNoAncestors()));
		else{
			final long known = result.known();
			sections.add(new ReportSection.Paragraph(String.format(labels.sections().pedigreeCollapseIntro(), number(locale, known),
				number(locale, result.possible()), ctx.formatInt(result.levels().size()), ctx.formatInt(result.different()),
				percent(locale, known - result.different(), known))));

			final List<List<String>> rows = new ArrayList<>(result.levels().size());
			for(final Level level : result.levels())
				rows.add(List.of(ctx.formatInt(level.generation()), number(locale, level.possible()), number(locale, level.known()),
					percent(locale, level.known(), level.possible()), ctx.formatInt(level.different()),
					percent(locale, level.known() - level.different(), level.known())));
			sections.add(new ReportSection.Table(List.of(labels.sections().pedigreeCollapseColumn("GENERATION"),
				labels.sections().pedigreeCollapseColumn("POSSIBLE"), labels.sections().pedigreeCollapseColumn("KNOWN"),
				labels.sections().pedigreeCollapseColumn("KNOWN_PERCENT"), labels.sections().pedigreeCollapseColumn("DIFFERENT"),
				labels.sections().pedigreeCollapseColumn("COLLAPSE")), rows));

			sections.add(new ReportSection.Heading(2, labels.sections().pedigreeCollapseRepeatedTitle()));
			if(result.repeated().isEmpty())
				sections.add(new ReportSection.Paragraph(labels.sections().pedigreeCollapseNone()));
			else{
				sections.add(new ReportSection.Paragraph(String.format(labels.sections().pedigreeCollapseRepeatedIntro(),
					ctx.formatInt(result.repeated().size()))));
				final List<Repeated> shown = result.repeated().subList(0, Math.min(MAX_REPEATED, result.repeated().size()));
				final List<List<String>> repeatedRows = new ArrayList<>(shown.size());
				for(final Repeated repeated : shown)
					repeatedRows.add(List.of(ReportFormatters.escape(ctx.displayText(repeated.person())), number(locale, repeated.times()),
						String.join(", ", repeated.generations().stream()
							.map(ctx::formatInt)
							.toList())));
				sections.add(new ReportSection.Table(List.of(labels.sections().pedigreeCollapseColumn("NAME"),
					labels.sections().pedigreeCollapseColumn("TIMES"), labels.sections().pedigreeCollapseColumn("GENERATIONS")),
					repeatedRows));
				if(shown.size() < result.repeated().size())
					sections.add(new ReportSection.Paragraph(String.format(labels.sections().pedigreeCollapseRepeatedMore(),
						ctx.formatInt(shown.size()))));
			}
			sections.add(new ReportSection.Paragraph("*" + labels.sections().pedigreeCollapseNote() + "*"));
		}
		listener.onProgress(100, null);

		return new ReportDocument(title, ReportFormatters.escape(ctx.displayText(root)), sections, language, labels.sections().contents());
	}


	/**
	 * @param index	the relations of the model (they hide what the privacy policy hides)
	 * @param root	the subject
	 * @param generations	how many generations of ancestors to follow (1 is the parents), 0 for all of them
	 * @return	The generations of ancestors, as far as there is one.
	 */
	static Result compute(final RelationIndex index, final FLEFRecord root, final int generations){
		final int limit = (generations > 0? Math.min(generations, MAX_GENERATIONS): MAX_GENERATIONS);
		final List<Level> levels = new ArrayList<>();
		final Map<String, FLEFRecord> records = new LinkedHashMap<>();
		final Map<String, Long> times = new LinkedHashMap<>();
		final Map<String, TreeSet<Integer>> inGenerations = new LinkedHashMap<>();

		Map<String, Long> current = new LinkedHashMap<>();
		current.put(root.getId(), 1L);
		records.put(root.getId(), root);
		for(int generation = 1; generation <= limit; generation ++){
			if(Thread.currentThread().isInterrupted())
				throw new RuntimeException("Report generation cancelled");

			final Map<String, Long> next = new LinkedHashMap<>();
			for(final Map.Entry<String, Long> entry : current.entrySet())
				for(final FLEFRecord parent : index.parentsOf(records.get(entry.getKey()))){
					records.putIfAbsent(parent.getId(), parent);
					next.merge(parent.getId(), entry.getValue(), Long::sum);
				}
			if(next.isEmpty())
				break;

			long known = 0;
			for(final Map.Entry<String, Long> entry : next.entrySet()){
				known += entry.getValue();
				times.merge(entry.getKey(), entry.getValue(), Long::sum);
				inGenerations.computeIfAbsent(entry.getKey(), k -> new TreeSet<>()).add(generation);
			}
			levels.add(new Level(generation, 1L << generation, known, next.size()));
			current = next;
		}

		final List<Repeated> repeated = new ArrayList<>();
		for(final Map.Entry<String, Long> entry : times.entrySet())
			if(entry.getValue() > 1)
				repeated.add(new Repeated(records.get(entry.getKey()), entry.getValue(), List.copyOf(inGenerations.get(entry.getKey()))));
		repeated.sort(Comparator.comparingLong(Repeated::times).reversed()
			.thenComparing(r -> r.person().getId()));
		return new Result(levels, times.size(), repeated);
	}


	private static String number(final Locale locale, final long value){
		return String.format(locale, "%,d", value);
	}

	/** @return	The share of the numerator in the denominator, a dash if there is nothing to divide. */
	static String percent(final Locale locale, final long numerator, final long denominator){
		return (denominator > 0? String.format(locale, "%.1f%%", 100. * numerator / denominator): NO_VALUE);
	}

}
