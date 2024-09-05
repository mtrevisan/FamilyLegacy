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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


//https://en.wikipedia.org/wiki/List_of_calendars
@Entity(name = "calendar")
@Table(uniqueConstraints = @UniqueConstraint(name = "unique_type", columnNames = "type"))
public class CalendarEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "calendar_generator")
	@SequenceGenerator(name = "calendar_generator", allocationSize = 1, sequenceName = "calendar_sequence")
	@Column(name = "id")
	private Long id;

	//A calendar type (must be unique, ex. "gregorian", "julian", "venetan", "french republican", "hebrew", "muslim", "chinese", "indian",
	// "buddhist", "coptic", "soviet eternal", "ethiopian", "mayan").
	@Column(name = "type", nullable = false)
	private String type;


	public Long getID(){
		return id;
	}


	@Override
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(!(o instanceof final CalendarEntity other))
			return false;
		return (id != null && id.equals(other.getID()));
	}

	@Override
	public int hashCode(){
		return 31;
	}

}
