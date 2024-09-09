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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


//Genealogical events and individual characteristics at various times and places are influenced by customs, practices, and conditions of their culture. This effects the interpretation of recorded information and the assertions made about a citation.
/*
Ex.
 - per i nomi in latino si deve usare il nominativo (quello che generalmente finisce in *-us* per il maschile e *-a* per il femminile).

https://it.wikisource.org/wiki/Codice_di_Napoleone_il_grande/Libro_I/Titolo_V
https://www.google.com/url?sa=t&source=web&rct=j&opi=89978449&url=https://elearning.unite.it/pluginfile.php/284578/mod_folder/content/0/ZZ%2520-%2520Lezioni%252028-30%2520novembre%25202023/3.%2520L_EREDITA_NAPOLEONICA_lezione.pdf%3Fforcedownload%3D1&ved=2ahUKEwiMj_Lx2-SGAxVM_7sIHc5-ATEQFnoECA8QAw&usg=AOvVaw1VloSAbSzRjtU1QBFag5uC
 - uomini: 23 anni minore, 29 anni maggiore (31 JAN 1807 - 19 FEB 1811) (25, atto rispettoso comunque fino ai 30).
 - donne: 22 anni minore (31 JAN 1807 - 13 MAY 1809) (21, atto rispettoso comunque fino ai 25).
*/
@Entity(name = "cultural_norm")
@Table(uniqueConstraints = @UniqueConstraint(name = "unique_identifier", columnNames = "identifier"))
public class CulturalNormEntity extends AbstractEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cultural_norm_generator")
	@SequenceGenerator(name = "cultural_norm_generator", allocationSize = 1, sequenceName = "cultural_norm_sequence")
	@Column(name = "id")
	private Long id;

	//An identifier of the rule.
	@Column(name = "identifier", nullable = false)
	private String identifier;

	//The description of the rule.
	@Column(name = "description")
	private String description;

	//The place this rule applies.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "place_id", referencedColumnName = "id")
	private PlaceEntity place;

	//The date this cultural norm went into effect.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "date_start_id", referencedColumnName = "id")
	private HistoricDateEntity dateStart;

	//The date this cultural norm stopped being in effect.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "date_end_id", referencedColumnName = "id")
	private HistoricDateEntity dateEnd;

	@Column(name = "certainty")
	@Enumerated(EnumType.STRING)
	private CertaintyEnum certainty;

	@Column(name = "credibility")
	@Enumerated(EnumType.STRING)
	private CredibilityEnum credibility;


	@Override
	public Long getID(){
		return id;
	}

}