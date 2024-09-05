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
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import java.time.ZonedDateTime;


@Entity(name = "research_status")
@Table(uniqueConstraints = @UniqueConstraint(name = "unique_identifier", columnNames = "identifier"))
public class ResearchStatusEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "research_status_generator")
	@SequenceGenerator(name = "research_status_generator", allocationSize = 1, sequenceName = "research_status_sequence")
	@Column(name = "id")
	private Long id;

	//The table name this record is attached to.
	@Column(name = "reference_table", nullable = false)
	private String referenceTable;

	//The ID of the referenced record in the table.
	@Column(name = "reference_id", nullable = false)
	private Long referenceID;

	@Transient
	private Object referencedEntity;

	//An identifier.
	@Column(name = "identifier", nullable = false)
	private String identifier;

	//The description of the research status. Text following markdown language. Reference to an entry in a table can be written as
	// `[text](<TABLE_NAME>@<XREF>)`.
	@Column(name = "description")
	private String description;

	//Research status (ex. "open": recorded but not started yet, "active": currently being searched, "ended": all the information has been
	// found).
	@Column(name = "status")
	private String status;

	@Column(name = "priority")
	private Integer priority;

	@Column(name = "creation_date", columnDefinition= "TIMESTAMP WITH TIME ZONE", nullable = false)
	private ZonedDateTime creationDate;


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
		if(!(o instanceof final ResearchStatusEntity other))
			return false;
		return (id != null && id.equals(other.getID()));
	}

	@Override
	public int hashCode(){
		return 31;
	}

}
