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
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.Transient;


//What the source says at the citation within the source.
@NodeEntity(label = "Assertion")
public class AssertionEntity extends AbstractEntity{

	@Id
	@GeneratedValue
	private Long id;

	//The citation from which this assertion is derived.
	@Relationship(type = "generate", direction = Relationship.Direction.INCOMING)
	private CitationEntity citation;

	//The table name this record is attached to (ex. "place", "cultural norm", "historic date", "calendar", "person", "group", "media",
	// "person name").
	private String referenceTable;

	//The ID of the referenced record in the table.
	private Long referenceID;

	@Transient
	private Object referencedEntity;

	//What role the cited entity played in the event that is being cited in this context (ex. "child", "father", "mother", "partner",
	// "midwife", "bridesmaid", "best man", "parent", "prisoner", "religious officer", "justice of the peace", "supervisor", "employer",
	// "employee", "witness", "assistant", "roommate", "landlady", "landlord", "foster parent", "makeup artist", "financier", "florist",
	// "usher", "photographer", "bartender", "bodyguard", "adoptive parent", "hairdresser", "chauffeur", "treasurer", "trainer", "secretary",
	// "navigator", "neighbor", "maid", "pilot", "undertaker", "mining partner", "legal guardian", "interior decorator", "executioner",
	// "driver", "host", "hostess", "farm hand", "ranch hand", "junior partner", "butler", "boarder", "chef", "patent attorney").
	@Property(name = "role")
	private String role;

	@Property(name = "certainty")
	private CertaintyEnum certainty;

	@Property(name = "credibility")
	private CredibilityEnum credibility;


	@Override
	public Long getID(){
		return id;
	}

	@Override
	public String getReferenceTable(){
		return referenceTable;
	}

	@Override
	public Long getReferenceID(){
		return referenceID;
	}

	@Override
	public void setReferencedEntity(final AbstractEntity referencedEntity){
		this.referencedEntity = referencedEntity;
	}

}
