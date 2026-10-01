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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


//A representation of where a source or set of sources is located
@Entity(name = "repository")
@Table(uniqueConstraints = @UniqueConstraint(name = "unique_identifier", columnNames = "identifier"))
public class RepositoryEntity extends AbstractEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "repository_generator")
	@SequenceGenerator(name = "repository_generator", allocationSize = 1, sequenceName = "repository_sequence")
	@Column(name = "id")
	private Long id;

	//Repository identifier (must be unique, ex. "familysearch.org", or "University College London").
	@Column(name = "identifier", nullable = false)
	private String identifier;

	//Repository type (ex. "public library", "college library", "national library", "prison library", "national archives", "website",
	// "personal collection", "cemetery/mausoleum", "museum", "state library", "religious library", "genealogy society collection",
	// "government agency", "funeral home").
	@Column(name = "type")
	private String type;

	//An xref ID of the person, if present in the tree and is the repository of a source.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "person_id", referencedColumnName = "id")
	private PersonEntity person;

	//The place this repository is.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "place_id", referencedColumnName = "id")
	private PlaceEntity place;


	@Override
	public Long getID(){
		return id;
	}

}
