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
package io.github.mtrevisan.familylegacy.v2.io.merger;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;


/**
 * Computes a trust score for a record based on its audit history and presence of sources.
 * A higher score indicates that the record is more reliable.
 */
public final class TrustScorer{

	private TrustScorer(){}


	/**
	 * Returns a trust score between 0 and 1.
	 * The score is increased when:
	 * - the record has an audit with a recent creation date (within the last year)
	 * - the record has at least one source citation
	 */
	public static double score(final FLEFRecord record){
		if(record == null)
			return 0.;

		double score = 0.5; // base

		// Audit creation date: more recent = higher trust
		final FLEFRecord audit = FLEFRecordHelper.findChild(record, "audit");
		if(audit != null){
			final FLEFRecord creation = FLEFRecordHelper.findChild(audit, "creation");
			if(creation != null){
				final String dateStr = FLEFRecordHelper.getChildValue(creation, "date");
				if(dateStr != null){
					try{
						final LocalDate ld = LocalDate.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
						final long days = ChronoUnit.DAYS.between(ld, LocalDate.now());
						if(days < 30)
							score += 0.4;
						else if(days < 365)
							score += 0.2;
						// else no bonus
					}
					catch(final Exception ignored){}
				}
			}
		}

		// Source citations increase trust
		if(FLEFRecordHelper.findChild(record, "source") != null)
			score += 0.1;

		return Math.min(1., score);
	}

}
