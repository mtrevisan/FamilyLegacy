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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository.ProjectionMutator;


public interface EntityListener{

	void onRootEntitySelected(String recordId);

	void onEntityEdit(FLEFRecord individual);

	void onEntityRemove(FLEFRecord individual);

	void onEntityRelocate(FLEFRecord individual);


	/**
	 * Returns the projection mutator that owns the structural changes to
	 * the model backing this view, or {@code null} when the listener does
	 * not support mutations (e.g. a read-only placeholder).
	 * <p>
	 * The popup menu factory uses this to build a {@link io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext}
	 * whose dispatcher exposes the mutator to the tools that need it,
	 * such as {@code UnlinkRelationshipsTool}. Without it, the tool falls
	 * back to a direct model mutation and the shared caches are not
	 * invalidated, so the tree keeps showing the old connections.
	 *
	 * @return the mutator, or {@code null}
	 */
	default ProjectionMutator getMutator(){
		return null;
	}

}
