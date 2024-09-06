/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.persistence.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;


//https://en.wikipedia.org/wiki/ISO_8601
@Entity(name = "historic_date")
public class HistoricDateEntity extends AbstractEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "historic_date_generator")
	@SequenceGenerator(name = "historic_date_generator", allocationSize = 1, sequenceName = "historic_date_sequence")
	@Column(name = "id")
	private Long id;

	//The date as an ISO 8601 "DD MMM YYYY" date (ex. a "gregorian", "julian", "french republican", "venetan", "hebrew", "muslim", "chinese",
	// "indian", "buddhist", "coptic", "soviet eternal", "ethiopian", "mayan", or whatever converted into ISO 8601).
	@Column(name = "date", nullable = false)
	private String date;

	//The date as written into a document.
	@Column(name = "date_original")
	private String dateOriginal;

	//An xref ID of a calendar type for the original date.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "calendar_original_id", referencedColumnName = "id")
	private CalendarEntity calendarOriginal;

	@Column(name = "certainty")
	@Enumerated(EnumType.STRING)
	private CertaintyEnum certainty;

	@Column(name = "credibility")
	@Enumerated(EnumType.STRING)
	private CredibilityEnum credibility;


	@Override
	public Long getID(){
		return id;
	}

}
