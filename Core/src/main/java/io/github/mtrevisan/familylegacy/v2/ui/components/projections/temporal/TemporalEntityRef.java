/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.Objects;


/**
 * A reference to a FLEF entity participating in the General Temporal
 * Projection.
 * <p>
 * A reference couples three pieces of information:
 * <ul>
 *   <li>the <b>kind</b> of entity ({@link TemporalEntityType}), which
 *       determines whether it can own a row ({@link #isRowEntity()}) or
 *       appears only as contextual background ({@link #isContextEntity()});</li>
 *   <li>the <b>backing record</b> from the FLEF model, used by the renderer
 *       and by the interaction handler to open the original data;</li>
 *   <li>a pre‑computed <b>display label</b>, so that the projection does not
 *       need to invoke the record handlers on every repaint.</li>
 * </ul>
 * <p>
 * Equality and hash code are based solely on the pair
 * {@code (type, id)}, so that two references to the same underlying entity
 * compare equal regardless of the display label or of the specific
 * {@code FLEFRecord} instance they carry.
 */
public record TemporalEntityRef(TemporalEntityType type, String id, FLEFRecord record, String displayLabel){

	/**
	 * Compact constructor with validation.
	 */
	public TemporalEntityRef{
		if(type == null)
			throw new IllegalArgumentException("Entity type must not be null");
		if(id == null || id.isBlank())
			throw new IllegalArgumentException("Entity id must not be null or blank");
		if(record == null)
			throw new IllegalArgumentException("Backing record must not be null");
		if(displayLabel == null)
			throw new IllegalArgumentException("Display label must not be null (use an empty string if unknown)");
	}


	/**
	 * Creates a reference from a FLEF record, deriving the {@code id} from
	 * the record itself.
	 *
	 * @param type         the entity kind
	 * @param record       the backing FLEF record (must not be {@code null}
	 *                     and must have a non‑null id)
	 * @param displayLabel the pre‑computed display label
	 * @return a new entity reference
	 */
	public static TemporalEntityRef of(final TemporalEntityType type, final FLEFRecord record,
		final String displayLabel){
		if(record == null)
			throw new IllegalArgumentException("Backing record must not be null");
		final String id = record.getId();
		if(id == null)
			throw new IllegalArgumentException("Backing record must have a non-null id");
		return new TemporalEntityRef(type, id, record, displayLabel);
	}


	/**
	 * Returns whether this entity can own a row in the projection.
	 *
	 * @return {@code true} for individuals, groups and places
	 */
	public boolean isRowEntity(){
		return type.isRowEntity();
	}

	/**
	 * Returns whether this entity appears only as contextual background.
	 *
	 * @return {@code true} for historic events and cultural norms
	 */
	public boolean isContextEntity(){
		return type.isContextEntity();
	}


	/**
	 * Equality is based on the pair {@code (type, id)} only.
	 */
	@Override
	public boolean equals(final Object other){
		if(this == other)
			return true;
		if(!(other instanceof TemporalEntityRef ref))
			return false;
		return (type == ref.type && Objects.equals(id, ref.id));
	}

	/**
	 * Hash code is based on the pair {@code (type, id)} only.
	 */
	@Override
	public int hashCode(){
		return Objects.hash(type, id);
	}

	@Override
	public String toString(){
		return type + ":" + id + " (" + displayLabel + ")";
	}

}
