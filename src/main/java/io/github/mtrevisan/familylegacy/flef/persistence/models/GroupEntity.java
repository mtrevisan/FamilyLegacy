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


@Entity(name = "group")
public class GroupEntity extends AbstractEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_generator")
	@SequenceGenerator(name = "group_generator", allocationSize = 1, sequenceName = "group_sequence")
	@Column(name = "id")
	private Long id;

	//The type of the group (ex. "family", "neighborhood", "fraternity", "ladies club", "literary society").
	@Column(name = "type")
	private String type;

	//The primary photo for this group.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "photo_id", referencedColumnName = "id")
	private MediaEntity photo;

	//Top-left coordinate and width-height length of the enclosing box inside an photo.
	@Column(name = "photo_crop")
	private String photoCrop;


	@Override
	public Long getID(){
		return id;
	}

}
