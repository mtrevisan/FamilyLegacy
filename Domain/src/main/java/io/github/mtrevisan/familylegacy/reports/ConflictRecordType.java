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


/** Which conflicts the Conflicts report lists, by the kind of record that is in conflict. */
public enum ConflictRecordType{

	/** Every conflict. */
	ALL,
	/** Births, baptisms, deaths and burials. */
	EVENT,
	/** The relationships between individuals: parentage and the type of the relationship. */
	RELATIONSHIP,
	/** The attributes of the individuals. */
	ATTRIBUTE,
	/** The names of the individuals. */
	NAME,
	/** The identity hypotheses. */
	IDENTITY;


	/** @return	Whether the conflicts of the kind are listed. */
	public boolean accepts(final ReportConflict.Kind kind){
		return switch(this){
			case ALL -> true;
			case EVENT -> (kind == ReportConflict.Kind.BIRTH || kind == ReportConflict.Kind.BAPTISM || kind == ReportConflict.Kind.DEATH
				|| kind == ReportConflict.Kind.BURIAL);
			case RELATIONSHIP -> (kind == ReportConflict.Kind.PARENTAGE || kind == ReportConflict.Kind.RELATIONSHIP);
			case ATTRIBUTE -> (kind == ReportConflict.Kind.ATTRIBUTE);
			case NAME -> (kind == ReportConflict.Kind.NAME);
			case IDENTITY -> (kind == ReportConflict.Kind.IDENTITY);
		};
	}

}
