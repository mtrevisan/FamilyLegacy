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
import jakarta.persistence.Transient;


//What the source says at the citation within the source.
@Entity(name = "assertion")
public class AssertionEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assertion_generator")
	@SequenceGenerator(name = "assertion_generator", allocationSize = 1, sequenceName = "assertion_sequence")
	@Column(name = "id")
	private Long id;

	//The citation from which this assertion is derived.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "citation_id", referencedColumnName = "id", nullable = false)
	private CitationEntity citation;

	//The table name this record is attached to (ex. "place", "cultural norm", "historic date", "calendar", "person", "group", "media",
	// "person name").
	@Column(name = "reference_table")
	private String referenceTable;

	//The ID of the referenced record in the table.
	@Column(name = "reference_id")
	private Long referenceID;

	@Transient
	private Object referencedEntity;

	//What role the cited entity played in the event that is being cited in this context (ex. "child", "father", "mother", "partner",
	// "midwife", "bridesmaid", "best man", "parent", "prisoner", "religious officer", "justice of the peace", "supervisor", "employer",
	// "employee", "witness", "assistant", "roommate", "landlady", "landlord", "foster parent", "makeup artist", "financier", "florist",
	// "usher", "photographer", "bartender", "bodyguard", "adoptive parent", "hairdresser", "chauffeur", "treasurer", "trainer", "secretary",
	// "navigator", "neighbor", "maid", "pilot", "undertaker", "mining partner", "legal guardian", "interior decorator", "executioner",
	// "driver", "host", "hostess", "farm hand", "ranch hand", "junior partner", "butler", "boarder", "chef", "patent attorney").
	@Column(name = "role")
	private String role;

	@Column(name = "certainty")
	@Enumerated(EnumType.STRING)
	private CertaintyEnum certainty;

	@Column(name = "credibility")
	@Enumerated(EnumType.STRING)
	private CredibilityEnum credibility;


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
		if(!(o instanceof final AssertionEntity other))
			return false;
		return (id != null && id.equals(other.getID()));
	}

	@Override
	public int hashCode(){
		return 31;
	}

}
