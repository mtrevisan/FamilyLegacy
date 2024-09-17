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
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.annotation.Transient;


@RelationshipEntity(type = "transcription_of")
public class LocalizedTextJunctionEntity extends AbstractEntity{

	@Id
	@GeneratedValue
	private Long id;

	@StartNode
	private LocalizedTextEntity localizedText;

	//The table name this record is attached to (ex. "citation", "place").
	private String referenceTable;

	//The ID of the referenced record in the table.
	private Long referenceID;

	@Transient
	private Object referencedEntity;

	//The column name this record is attached to (ex. "extract", "name").
	@Property
	private String referenceType;


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

	public String getReferenceType(){
		return referenceType;
	}

	@Override
	public void setReferencedEntity(final AbstractEntity referencedEntity){
		this.referencedEntity = referencedEntity;
	}

}
