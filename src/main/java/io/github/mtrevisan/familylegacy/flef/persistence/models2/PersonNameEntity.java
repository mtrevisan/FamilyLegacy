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
package io.github.mtrevisan.familylegacy.flef.persistence.models2;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;


@NodeEntity(label = "PersonName")
public class PersonNameEntity extends AbstractEntity{

	@Id
	@GeneratedValue
	private Long id;

	@Relationship(type = "of", direction = Relationship.Direction.INCOMING)
	private PersonEntity person;

	//A verbatim copy of the (primary, that is the proper name) name written in the original language.
	private String personalName;

	//A verbatim copy of the (secondary, that is everything that is not a proper name, like a surname) name written in the original language.
	private String familyName;

	//Locale of the name as defined in ISO 639 (https://en.wikipedia.org/wiki/ISO_639).
	private String locale;

	//(ex. "birth name" (name given on birth certificate), "also known as" (an unofficial pseudonym, also known as, alias, etc), "nickname"
	// (a familiar name), "family nickname", "pseudonym", "legal" (legally changed name), "adoptive name" (name assumed upon adoption),
	// "stage name", "marriage name" (name assumed at marriage), "call name", "official name", "anglicized name", "religious order name",
	// "pen name", "name at work", "immigrant" (name assumed at the time of immigration) -- see
	// https://github.com/FamilySearch/gedcomx/blob/master/specifications/name-part-qualifiers-specification.md)
	private String type;


	@Override
	public Long getID(){
		return id;
	}

}
