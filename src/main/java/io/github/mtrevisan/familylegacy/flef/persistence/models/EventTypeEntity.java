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


@Entity(name = "event_type")
public class EventTypeEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_type_generator")
	@SequenceGenerator(name = "event_type_generator", allocationSize = 1, sequenceName = "event_type_sequence")
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "super_type_id", referencedColumnName = "id", nullable = false)
	private EventSuperTypeEntity superType;

	//(ex. Historical events: "historic fact", "natural disaster", "invention", "patent filing", "patent granted", Personal origins: "birth",
	// "sex", "fosterage", "adoption", "guardianship", Physical description: "physical description", "eye color", "hair color", "height",
	// "weight", "build", "complexion", "gender", "race", "ethnic origin", "marks/scars", "special talent", "disability", Citizenship and
	// migration: "nationality", "emigration", "immigration", "naturalization", "caste", Real estate assets: "residence", "land grant",
	// "land purchase", "land sale", "property", "deed", "escrow", Education: "education", "graduation", "able to read", "able to write",
	// "learning", "enrollment", Work and Career: "employment", "occupation", "career", "retirement", "resignation", Legal Events and
	// Documents: "coroner report", "will", "probate", "legal problem", "name change", "inquest", "jury duty", "draft registration",
	// "pardon", Health problems and habits: "hospitalization", "illness", "tobacco use", "alcohol use", "drug problem", Marriage and family
	// life: "engagement", "betrothal", "cohabitation", "union", "wedding", "marriage", "number of marriages", "marriage bann",
	// "marriage license", "marriage contract", "marriage settlement", "filing for divorce", "divorce", "annulment", "separation",
	// "number of children (total)", "number of children (living)", "marital status", "wedding anniversary", "anniversary celebration",
	// Military: "military induction", "military enlistment", "military rank", "military award", "military promotion", "military service",
	// "military release", "military discharge", "military resignation", "military retirement", "missing in action", Confinement:
	// "imprisonment", "deportation", "internment", Transfers and travel: "travel", Accolades: "honor", "award", "membership", Death and
	// burial: "death", "execution", "autopsy", "funeral", "cremation", "scattering of ashes", "inurnment", "burial", "exhumation",
	// "reburial", Others: "anecdote", "political affiliation", "hobby", "partnership", "celebration of life", "ran away from home",
	// Religious events: "religion", "religious conversion", "bar mitzvah", "bas mitzvah", "baptism", "excommunication", "christening",
	// "confirmation", "ordination", "blessing", "first communion")
	@Column(name = "type", nullable = false)
	private String type;

	//(ex. birth of a person: "birth", death of a person: "death", "execution", union between two persons: "betrothal", "cohabitation",
	// "union", "wedding", "marriage", "marriage bann", "marriage license", "marriage contract", adoption of a person: "adoption",
	// "fosterage")
	@Column(name = "category")
	private String category;


	public Long getID(){
		return id;
	}


	@Override
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(!(o instanceof final EventTypeEntity other))
			return false;
		return (id != null && id.equals(other.getID()));
	}

	@Override
	public int hashCode(){
		return 31;
	}

}
