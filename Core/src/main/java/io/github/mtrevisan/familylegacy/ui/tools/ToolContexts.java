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
package io.github.mtrevisan.familylegacy.ui.tools;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.group.GroupListener;
import io.github.mtrevisan.familylegacy.ui.components.projections.group.GroupPanel;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.EntityListener;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.ui.components.projections.repository.ProjectionMutator;

import java.awt.Component;
import java.util.function.Consumer;


/**
 * Factory methods for {@link ToolContext}, so that every tool invocation
 * obtains its context from a single place with a consistent dispatcher.
 * <p>
 * The recurring mistake this class prevents is passing {@code null} as the
 * dispatcher: the canonical constructor silently replaces it with a default
 * that returns no mutator, so tools that mutate the relationship graph
 * ({@code UnlinkRelationshipsTool}, {@code DeleteIndividualTool},
 * {@code PasteIndividualTool}, ...) fall back to a direct model mutation
 * and never invalidate the shared caches, leaving the views stale.
 */
public final class ToolContexts{

	private ToolContexts(){}


	/**
	 * Context for read-only tools. The dispatcher has no mutator; use this
	 * only for tools that do not touch the model.
	 */
	public static ToolContext readOnly(final FLEFModel model, final String entityId, final Component component){
		return new ToolContext(model, entityId, component, null);
	}

	/**
	 * Context whose dispatcher exposes the projection mutator obtained
	 * from the listener. Use this for every tool that mutates the model
	 * through the graph (edit, delete, paste, unlink, relocate, add).
	 */
	public static ToolContext withMutator(final FLEFModel model, final EntityListener listener,
			final Component component, final String entityId){
		return new ToolContext(model, entityId, component, new ToolDispatcher(){
			@Override
			public ProjectionMutator getMutator(){
				return (listener != null? listener.getMutator(): null);
			}

			@Override
			public void editEntity(final String id){
				if(listener == null || id == null)
					return;

				final FLEFRecord record = model.getRecordById(id);
				if(record != null)
					listener.onEntityEdit(record);
			}

			@Override
			public void paste(){
				if(listener != null){
					if(listener instanceof IndividualListener indListener)
						indListener.onIndividualPaste((IndividualPanel)component);
					else if(listener instanceof GroupListener grpListener)
						grpListener.onGroupPaste((GroupPanel)component);
				}
			}

			@Override
			public void removeEntity(final String id){
				if(listener == null || id == null)
					return;

				final FLEFRecord record = model.getRecordById(id);
				if(record != null)
					listener.onEntityRemove(record);
			}

			@Override
			public void loadRoot(final String id){
				if(listener != null)
					listener.onRootEntitySelected(id);
			}
		});
	}

	public static ToolContext withMutatorAndEdit(final FLEFModel model, final EntityListener listener,
			final Component component, final String entityId, final Consumer<String> onEdit){
		return new ToolContext(model, entityId, component, new ToolDispatcher(){
			@Override
			public ProjectionMutator getMutator(){
				return (listener != null? listener.getMutator(): null);
			}

			@Override
			public void editEntity(final String id){
				if(onEdit != null)
					onEdit.accept(id);
			}
		});
	}

	/**
	 * Context with a custom dispatcher. Use this when the tool must invoke
	 * one or more {@link ToolDispatcher} methods in a way that is not just
	 * exposing the mutator, for example when the listener needs to decide
	 * the fallback root after a deletion.
	 */
	public static ToolContext custom(final FLEFModel model, final String entityId, final Component component,
			final ToolDispatcher dispatcher){
		return new ToolContext(model, entityId, component, dispatcher);
	}

}
