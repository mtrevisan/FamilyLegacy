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
import jakarta.persistence.Transient;


@Entity(name = "restriction")
public class RestrictionEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "restriction_generator")
	@SequenceGenerator(name = "restriction_generator", allocationSize = 1, sequenceName = "restriction_sequence")
	@Column(name = "id")
	private Long id;

	//Specifies how the record should be treated. Known values and their meaning are: "confidential" (should not be distributed or exported),
	// "public" (can be freely distributed or exported).
	@Column(name = "restriction", nullable = false)
	private String restriction;

	//The table name this record is attached to (ex. "assertion", "citation", "source", "repository", "cultural norm", "historic date",
	// "event", "place", "note", "person name", "person", "group", "media").
	@Column(name = "reference_table", nullable = false)
	private String referenceTable;

	//The ID of the referenced record in the table.
	@Column(name = "reference_id", nullable = false)
	private Long referenceID;

	@Transient
	private Object referencedEntity;


	public Long getID(){
		return id;
	}

	public String getReferenceTable(){
		return referenceTable;
	}

	public Long getReferenceID(){
		return referenceID;
	}

	public void setReferencedEntity(final Object referencedEntity){
		this.referencedEntity = referencedEntity;
	}


	@Override
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(!(o instanceof final RestrictionEntity other))
			return false;
		return (id != null && id.equals(other.getID()));
	}

	@Override
	public int hashCode(){
		return 31;
	}

}
