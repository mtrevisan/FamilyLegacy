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


//https://www.evidenceexplained.com/content/sample-quickcheck-models
@NodeEntity(label = "Source")
public class SourceEntity extends AbstractEntity{

	@Id
	@GeneratedValue
	private Long id;

	//The repository from which this source is contained.
	@Relationship(type = "from", direction = Relationship.Direction.INCOMING)
	private RepositoryEntity repository;

	//The title of the source (must be unique, ex. "1880 US Census").
	private String identifier;

	//ex. "newspaper", "technical journal", "magazine", "genealogy newsletter", "blog", "baptism record", "birth certificate",
	// "birth register", "book", "grave marker", "census", "death certificate", "yearbook", "directory (organization)",
	// "directory (telephone)", "deed", "land patent", "patent (invention)", "diary", "email message", "interview", "personal knowledge",
	// "family story", "audio record", "video record", "letter/postcard", "probate record", "will", "legal proceedings record", "manuscript",
	// "map", "marriage certificate", "marriage license", "marriage register", "marriage record", "naturalization", "obituary",
	// "pension file", "photograph", "painting/drawing", "passenger list", "tax roll", "death index", "birth index", "town record",
	// "web page", "military record", "draft registration", "enlistment record", "muster roll", "burial record", "cemetery record",
	// "death notice", "marriage index", "alumni publication", "passport", "passport application", "identification card",
	// "immigration record", "border crossing record", "funeral home record", "article", "newsletter", "brochure", "pamphlet", "poster",
	// "jewelry", "advertisement", "cemetery", "prison record", "arrest record".
	private String type;

	//The person, agency, or entity who created the record. For a published work, this could be the author, compiler, transcriber,
	// abstractor, or editor. For an unpublished source, this may be an individual, a government agency, church organization, or private
	// organization, etc.
	private String author;

	//The place this source was created.
	@Relationship(type = "in", direction = Relationship.Direction.INCOMING)
	private PlaceEntity place;

	//The date this source was created.
	@Relationship(type = "on", direction = Relationship.Direction.INCOMING)
	private HistoricDateEntity date;

	//Specific location within the information referenced. The data in this field should be in the form of a label and value pair (eg.
	// 'Film: 1234567, Frame: 344, Line: 28').
	private String location;


	@Override
	public Long getID(){
		return id;
	}

}
