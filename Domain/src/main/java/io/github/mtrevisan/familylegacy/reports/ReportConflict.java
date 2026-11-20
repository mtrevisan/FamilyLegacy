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

import java.util.List;
import java.util.Locale;
import java.util.Objects;


/**
 * A conflict of the research, as the Conflicts report tells it: the competing assertions, and how a conclusion of the researcher
 * judged them, if one did.
 *
 * @param kind	What the assertions are about.
 * @param problem	What is wrong with them.
 * @param personIds	The individuals the conflict concerns (a conflict is left out of the report if any of them is hidden by its privacy policy).
 * @param assertions	The competing assertions (events, relationships, attributes or hypotheses).
 * @param settlement	The conclusion that settles the conflict, {@code null} if none does.
 */
public record ReportConflict(Kind kind, Problem problem, List<String> personIds, List<Assertion> assertions, Settlement settlement){

	/** What the competing assertions are about (each one is a section of the report). */
	public enum Kind{

		BIRTH,
		BAPTISM,
		DEATH,
		BURIAL,
		PARENTAGE,
		RELATIONSHIP,
		ATTRIBUTE,
		NAME,
		IDENTITY;


		/** @return	The key of the labels of the kind. */
		public String key(){
			return name().toLowerCase(Locale.ROOT);
		}

	}

	/** What is wrong with the assertions. */
	public enum Problem{

		/** Dates that cannot all be right (no day is common to them). */
		DIFFERENT_DATES,
		/** Places that cannot all be right (none contains the other). */
		DIFFERENT_PLACES,
		/** Several records of what an individual has only once, that do not contradict each other (yet). */
		SEVERAL_RECORDS,
		/** More biological parents than two. */
		TOO_MANY_PARENTS,
		/** Two biological parents of the same sex. */
		SAME_SEX_PARENTS,
		/** An individual that is their own ancestor. */
		OWN_ANCESTOR,
		/** The same child and parent, related in ways that exclude each other. */
		DIFFERENT_TYPES,
		/** Several values of what can have only one. */
		DIFFERENT_VALUES,
		/** Several names of the same kind (birth, official...) that differ. */
		DIFFERENT_NAMES,
		/** An identity hypothesis no conclusion has evaluated. */
		OPEN_HYPOTHESIS;


		/** @return	The key of the labels of the problem. */
		public String key(){
			return name().toLowerCase(Locale.ROOT);
		}

	}

	/**
	 * @param recordId	The identifier of the record that asserts it (the event, the relationship, the attribute, the hypothesis).
	 * @param value	What it asserts, as a text (the date, the type, the value, the other parent...).
	 * @param placeId	The place it asserts, or {@code null}.
	 * @param confidence	The confidence the researcher gave it ({@code low}, {@code medium}, {@code high}), or {@code null}.
	 * @param supporting	How many citations support it, {@code null} to count the ones of the record.
	 * @param contradicting	How many citations contradict it, {@code null} to count the ones of the record.
	 */
	public record Assertion(String recordId, String value, String placeId, String confidence, Integer supporting, Integer contradicting){

		public Assertion{
			Objects.requireNonNull(recordId, "recordId");
		}

		/** An assertion of a record that has its own citations. */
		public Assertion(final String recordId, final String value, final String placeId, final String confidence){
			this(recordId, value, placeId, confidence, null, null);
		}

	}

	/**
	 * @param conclusionId	The identifier of the conclusion.
	 * @param preferredId	The identifier of the assertion it prefers, or {@code null}.
	 * @param proofStatus	Its proof status ({@code conflicting_evidence}, {@code supported}, {@code proven}, {@code disproven}), or {@code null}.
	 */
	public record Settlement(String conclusionId, String preferredId, String proofStatus){}


	public ReportConflict{
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(problem, "problem");
		personIds = List.copyOf(personIds);
		assertions = List.copyOf(assertions);
	}


	/** @return	Whether a conclusion settles the conflict. */
	public boolean resolved(){
		return (settlement != null);
	}

}
