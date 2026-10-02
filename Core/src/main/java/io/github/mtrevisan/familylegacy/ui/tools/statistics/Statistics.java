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
package io.github.mtrevisan.familylegacy.ui.tools.statistics;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;

import java.util.List;


/**
 * The result of a statistics computation over a FLEF model. The record
 * is immutable and holds every piece of data the result dialog needs,
 * so the dialog can be opened multiple times without recomputing.
 */
public record Statistics(
	Completeness completeness,
	Distribution distribution,
	Coverage coverage
){

	/** Completeness section: how much of the expected data is present. */
	public record Completeness(
		int totalIndividuals,
		int totalEvents,
		int totalSources,
		List<Category> categories
	){

		/**
		 * One category of missing data. The records are kept so the
		 * dialog can open them on double-click.
		 *
		 * @param label   the human-readable label of the category
		 * @param records the records that fall into the category
		 */
		public record Category(String label, List<FLEFRecord> records){
			public int count(){
				return records.size();
			}
		}
	}

	/** Distribution section: names and demographics. */
	public record Distribution(
		int male,
		int female,
		int unknownSex,
		int totalSurnames,
		int totalGivenNames,
		List<NameGroup> surnames,
		List<NameGroup> givenNames
	){}

	/**
	 * A group of surnames that are considered the same family name.
	 *
	 * @param canonical the most common spelling in the group
	 * @param variants  every spelling that was merged into the group
	 * @param count     total occurrences across all variants
	 */
	public record NameGroup(String canonical, List<String> variants, int count){
	}

	/** Coverage section: how much of the model is backed by sources. */
	public record Coverage(
		int totalRecords,
		int totalCitations,
		List<TypeCoverage> byType,
		List<SourceUse> topSources
	){
	}

	/**
	 * Coverage of a single record type.
	 *
	 * @param tag   the record tag (e.g. {@code individual})
	 * @param total number of records of this type
	 * @param cited number of records with at least one source citation
	 */
	public record TypeCoverage(String tag, int total, int cited){
		public double percentage(){
			return (total > 0? 100.0 * cited / total: 0.0);
		}
	}

	/**
	 * One source with its usage count.
	 *
	 * @param sourceId    the id of the source record
	 * @param displayName the full display text of the source, not
	 *                    truncated
	 * @param count       how many times the source is cited
	 */
	public record SourceUse(String sourceId, String displayName, int count){
	}

}
