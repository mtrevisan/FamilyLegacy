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
package io.github.mtrevisan.familylegacy.consanguinity;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;


/**
 * The consanguinity report as text, to read in a dialog or to copy. It is made of segments: a piece of text, that may stand for a record
 * (an individual, a relationship), so that the dialog can make it a link to the editor of that record.
 */
public final class ConsanguinityReportText{

	/**
	 * A piece of the text of the report.
	 *
	 * @param text      what is written
	 * @param recordId  the identifier of the record that the text stands for ({@code null} for a text that is not a reference)
	 */
	public record Segment(String text, String recordId){

		/** @return	Whether the text is a reference to a record. */
		public boolean isReference(){
			return (recordId != null);
		}
	}

	/** A reference to a record, as an argument of a sentence. */
	private record Reference(String text, String recordId){}

	/** A list of references, written one after the other separated by commas. */
	private record References(List<Reference> items){}


	private ConsanguinityReportText(){}


	private static final String INDENT = "    ";
	/** What stands for an argument that is a reference, while the sentence is written (it cannot be in a text of the report). */
	private static final char MARK = '\u0001';

	/** @return	A coefficient as a percentage with two decimals (the locale does not change it: it is for comparing and copying). */
	public static String percent(final double coefficient){
		return String.format(Locale.ROOT, "%.2f%%", coefficient * 100.);
	}

	/** @return	A coefficient with four decimals. */
	public static String decimal(final double coefficient){
		return String.format(Locale.ROOT, "%.4f", coefficient);
	}

	/**
	 * @param report the report
	 * @param names  the name to show of an individual
	 * @return	The text of the report.
	 */
	public static String format(final ConsanguinityReport report, final Function<String, String> names){
		final StringBuilder sb = new StringBuilder();
		for(final Segment segment : segments(report, names))
			sb.append(segment.text());
		return sb.toString();
	}

	/**
	 * @param report the report
	 * @param names  the name to show of an individual
	 * @return	The report as pieces of text, in order: the ones that stand for a record (the partners, the common ancestors, the
	 * 	relationships that unite the couple, the children, the inbred individuals) are marked, the others are not. Put together they are the
	 * 	text of {@link #format}.
	 */
	public static List<Segment> segments(final ConsanguinityReport report, final Function<String, String> names){
		final List<Segment> out = new ArrayList<>();
		add(out, I18N.t("text.consanguinity.title") + StringUtils.LF + I18N.t("text.consanguinity.note") + StringUtils.LF + StringUtils.LF);

		add(out, I18N.tf("text.consanguinity.unions", decimal(report.minimumKinship()), report.unions().size()) + StringUtils.LF);
		int index = 0;
		for(final ConsanguinityReport.ConsanguineousUnion union : report.unions()){
			index ++;
			add(out, StringUtils.LF + "[" + index + "] ");
			out.add(new Segment(union.firstName(), union.firstId()));
			add(out, " + ");
			out.add(new Segment(union.secondName(), union.secondId()));
			add(out, StringUtils.LF);
			if(!union.term().isEmpty())
				add(out, INDENT + I18N.tf("text.consanguinity.related_as", union.term()) + StringUtils.LF);
			add(out, INDENT + I18N.tf("text.consanguinity.coefficients", decimal(union.relationship()), percent(union.kinship()))
				+ StringUtils.LF);
			if(!union.commonAncestors().isEmpty()){
				add(out, INDENT);
				out.addAll(sentence("text.consanguinity.common_ancestors", ancestorReferences(union.commonAncestors(), names)));
				add(out, StringUtils.LF);
			}
			add(out, INDENT);
			final List<Reference> relationships = union.relationshipIds().stream()
				.map(id -> new Reference(id, id))
				.toList();
			out.addAll(sentence("text.consanguinity.recorded_as", (union.relationshipIds().isEmpty()
				? I18N.t("text.consanguinity.recorded.no_union")
				: new Inner("text.consanguinity.recorded.union", new References(relationships)))));
			add(out, StringUtils.LF + INDENT + I18N.tf("text.consanguinity.children", union.childIds().size()));
			if(!union.childIds().isEmpty()){
				add(out, " (");
				appendReferences(out, union.childIds().stream()
					.map(id -> new Reference(id, id))
					.toList());
				add(out, ")");
			}
			add(out, StringUtils.LF);
		}

		add(out, StringUtils.LF + I18N.tf("text.consanguinity.inbred", percent(report.minimumInbreeding()),
			report.inbredIndividuals().size()) + StringUtils.LF);
		index = 0;
		for(final ConsanguinityReport.InbredIndividual individual : report.inbredIndividuals()){
			index ++;
			add(out, StringUtils.LF + "[" + index + "] ");
			out.add(new Segment(individual.name(), individual.id()));
			add(out, " (");
			out.add(new Segment(individual.id(), individual.id()));
			add(out, "): F = " + percent(individual.inbreeding()) + StringUtils.LF);
			if(!individual.term().isEmpty())
				add(out, INDENT + I18N.tf("text.consanguinity.parents_related", individual.term()) + StringUtils.LF);
			if(!individual.commonAncestors().isEmpty()){
				add(out, INDENT);
				out.addAll(sentence("text.consanguinity.parents_ancestors", ancestorReferences(individual.commonAncestors(), names)));
				add(out, StringUtils.LF);
			}
		}
		return out;
	}

	/** @return	The names of the ancestors with the generations that separate them from the two individuals; at most 4. */
	public static String ancestors(final List<ConsanguinityCalculator.CommonAncestor> ancestors, final Function<String, String> names){
		final StringBuilder sb = new StringBuilder();
		for(final Segment segment : ancestorSegments(ancestors, names))
			sb.append(segment.text());
		return sb.toString();
	}

	/** @return	The same, as segments: each ancestor is a reference. */
	public static List<Segment> ancestorSegments(final List<ConsanguinityCalculator.CommonAncestor> ancestors,
			final Function<String, String> names){
		final List<Segment> out = new ArrayList<>();
		for(int i = 0; i < Math.min(4, ancestors.size()); i ++){
			final ConsanguinityCalculator.CommonAncestor ancestor = ancestors.get(i);
			if(i > 0)
				add(out, "; ");
			out.addAll(expand(I18N.tf("text.consanguinity.ancestor", MARK + "0" + MARK, ancestor.stepsFromFirst(), ancestor.stepsFromSecond()),
				List.of(new Reference(names.apply(ancestor.id()), ancestor.id()))));
		}
		if(ancestors.size() > 4)
			add(out, "; " + I18N.tf("text.consanguinity.and_more", ancestors.size() - 4));
		return out;
	}


	/** A sentence inside a sentence: the key and the arguments of the inner one, to be written where the outer one asks for a text. */
	private record Inner(String key, Object... arguments){}

	/** @return	The ancestors as an argument of a sentence. */
	private static Object ancestorReferences(final List<ConsanguinityCalculator.CommonAncestor> ancestors,
			final Function<String, String> names){
		return ancestorSegments(ancestors, names);
	}

	/**
	 * Writes a sentence of the bundle whose single argument is a text, a list of segments, or an inner sentence: the segments that are
	 * references stay references.
	 */
	private static List<Segment> sentence(final String key, final Object argument){
		final List<Segment> inner = new ArrayList<>();
		switch(argument){
			case String text -> inner.add(new Segment(text, null));
			case Inner sentence -> inner.addAll(sentence(sentence.key(), sentence.arguments()[0]));
			case References references -> appendReferences(inner, references.items());
			case List<?> segments -> {
				for(final Object segment : segments)
					inner.add((Segment)segment);
			}
			default -> throw new IllegalArgumentException("Unexpected argument: " + argument);
		}

		//the pattern of the sentence is written with a mark where the argument goes, and cut there
		final String pattern = I18N.tf(key, String.valueOf(MARK));
		final int at = pattern.indexOf(MARK);
		final List<Segment> out = new ArrayList<>();
		if(at < 0){
			add(out, pattern);
			return out;
		}

		add(out, pattern.substring(0, at));
		out.addAll(inner);
		add(out, pattern.substring(at + 1));
		return out;
	}

	/** Splits a text at the marks that stand for the references (the mark, the number of the reference, the mark). */
	private static List<Segment> expand(final String text, final List<Reference> references){
		final List<Segment> out = new ArrayList<>();
		int from = 0;
		while(from < text.length()){
			final int start = text.indexOf(MARK, from);
			if(start < 0){
				add(out, text.substring(from));
				break;
			}

			add(out, text.substring(from, start));
			final int end = text.indexOf(MARK, start + 1);
			final Reference reference = references.get(Integer.parseInt(text.substring(start + 1, end)));
			out.add(new Segment(reference.text(), reference.recordId()));
			from = end + 1;
		}
		return out;
	}

	private static void appendReferences(final List<Segment> out, final List<Reference> references){
		for(int i = 0; i < references.size(); i ++){
			if(i > 0)
				add(out, ", ");
			out.add(new Segment(references.get(i).text(), references.get(i).recordId()));
		}
	}

	/** Adds a text that is not a reference (nothing if it is empty). */
	private static void add(final List<Segment> out, final String text){
		if(!text.isEmpty())
			out.add(new Segment(text, null));
	}

}
