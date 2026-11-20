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

import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;


/** Which conflicts the Conflicts report lists, by the confidence given to the competing assertions. */
public enum ConflictConfidence{

	/** Every conflict. */
	ALL(null),
	/** The conflicts with an assertion that has low confidence. */
	LOW(ConfidenceReader.ENUM_LOW),
	/** The conflicts with an assertion that has medium confidence. */
	MEDIUM(ConfidenceReader.ENUM_MEDIUM),
	/** The conflicts with an assertion that has high confidence. */
	HIGH(ConfidenceReader.ENUM_HIGH),
	/** The conflicts with an assertion whose confidence is not stated. */
	NOT_STATED(null);


	private final String level;


	ConflictConfidence(final String level){
		this.level = level;
	}


	/** @return	Whether the conflict is listed: at least one of its assertions has this confidence. */
	public boolean accepts(final ReportConflict conflict){
		return switch(this){
			case ALL -> true;
			case NOT_STATED -> conflict.assertions().stream()
				.anyMatch(assertion -> assertion.confidence() == null || assertion.confidence().isBlank());
			default -> conflict.assertions().stream()
				.anyMatch(assertion -> level.equalsIgnoreCase(assertion.confidence()));
		};
	}

}
