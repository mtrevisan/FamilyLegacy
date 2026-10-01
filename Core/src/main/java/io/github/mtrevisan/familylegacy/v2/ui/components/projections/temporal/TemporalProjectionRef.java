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

/**
 * A reference to a drawable element of the General Temporal Projection.
 * <p>
 * This sealed interface allows {@link ContextImpactLink} to point at
 * heterogeneous targets — a whole row, a specific entry, or a connection —
 * without forcing them to share a common supertype. Each variant carries
 * just enough information to resolve its anchor entity, so that the renderer
 * can highlight the target and the interaction handler can open the
 * underlying record.
 * <p>
 * Variants are declared as nested records to keep the hierarchy in a single
 * compilation unit and to make exhaustive {@code switch} patterns over the
 * sealed type possible.
 */
public sealed interface TemporalProjectionRef
	permits TemporalProjectionRef.RowRef, TemporalProjectionRef.EntryRef, TemporalProjectionRef.ConnectionRef{

	/**
	 * Returns the row entity that anchors this reference. For row references
	 * it is the row itself; for entry references it is the enclosing row;
	 * for connection references it is the source endpoint.
	 *
	 * @return the anchor entity, never {@code null}
	 */
	TemporalEntityRef anchorEntity();


	/**
	 * A reference to an entire row, identified by its row entity.
	 */
	record RowRef(TemporalEntityRef entity) implements TemporalProjectionRef{

		public RowRef{
			if(entity == null)
				throw new IllegalArgumentException("Row entity must not be null");
			if(!entity.isRowEntity())
				throw new IllegalArgumentException("RowRef requires a row entity, got: " + entity.type());
		}

		@Override
		public TemporalEntityRef anchorEntity(){
			return entity;
		}
	}

	/**
	 * A reference to a specific {@link TemporalEntry} inside a row.
	 */
	record EntryRef(TemporalEntityRef rowEntity, TemporalEntry entry) implements TemporalProjectionRef{

		public EntryRef{
			if(rowEntity == null)
				throw new IllegalArgumentException("Row entity must not be null");
			if(!rowEntity.isRowEntity())
				throw new IllegalArgumentException("EntryRef requires a row entity, got: " + rowEntity.type());
			if(entry == null)
				throw new IllegalArgumentException("Entry must not be null");
		}

		@Override
		public TemporalEntityRef anchorEntity(){
			return rowEntity;
		}
	}

	/**
	 * A reference to a {@link TemporalConnection} between two rows.
	 */
	record ConnectionRef(TemporalConnection connection) implements TemporalProjectionRef{

		public ConnectionRef{
			if(connection == null)
				throw new IllegalArgumentException("Connection must not be null");
		}

		@Override
		public TemporalEntityRef anchorEntity(){
			return connection.sourceRow();
		}
	}

}
