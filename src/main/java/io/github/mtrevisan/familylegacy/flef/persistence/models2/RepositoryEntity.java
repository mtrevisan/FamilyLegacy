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


//A representation of where a source or set of sources is located
@NodeEntity(label = "Repository")
public class RepositoryEntity extends AbstractEntity{

	@Id
	@GeneratedValue
	private Long id;

	//Repository identifier (must be unique, ex. "familysearch.org", or "University College London").
	private String identifier;

	//Repository type (ex. "public library", "college library", "national library", "prison library", "national archives", "website",
	// "personal collection", "cemetery/mausoleum", "museum", "state library", "religious library", "genealogy society collection",
	// "government agency", "funeral home").
	private String type;

	//An xref ID of the person, if present in the tree and is the repository of a source.
	@Relationship(type = "referent", direction = Relationship.Direction.INCOMING)
	private PersonEntity person;

	//The place this repository is.
	@Relationship(type = "in", direction = Relationship.Direction.INCOMING)
	private PlaceEntity place;


	@Override
	public Long getID(){
		return id;
	}

}
