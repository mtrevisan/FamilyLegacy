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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;


@Entity(name = "person_name")
public class PersonNameEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_name_generator")
	@SequenceGenerator(name = "person_name_generator", allocationSize = 1, sequenceName = "person_name_sequence")
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "person_id", referencedColumnName = "id", nullable = false)
	private PersonEntity person;

	//A verbatim copy of the (primary, that is the proper name) name written in the original language.
	@Column(name = "personal_name")
	private String personalName;

	//A verbatim copy of the (secondary, that is everything that is not a proper name, like a surname) name written in the original language.
	@Column(name = "family_name")
	private String familyName;

	//Locale of the name as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
	@Column(name = "locale")
	private String locale;

	//(ex. "birth name" (name given on birth certificate), "also known as" (an unofficial pseudonym, also known as, alias, etc), "nickname"
	// (a familiar name), "family nickname", "pseudonym", "legal" (legally changed name), "adoptive name" (name assumed upon adoption),
	// "stage name", "marriage name" (name assumed at marriage), "call name", "official name", "anglicized name", "religious order name",
	// "pen name", "name at work", "immigrant" (name assumed at the time of immigration) -- see
	// https://github.com/FamilySearch/gedcomx/blob/master/specifications/name-part-qualifiers-specification.md)
	@Column(name = "type")
	private String type;


	public Long getID(){
		return id;
	}


	@Override
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(!(o instanceof final PersonNameEntity other))
			return false;
		return (id != null && id.equals(other.getID()));
	}

	@Override
	public int hashCode(){
		return 31;
	}

}
