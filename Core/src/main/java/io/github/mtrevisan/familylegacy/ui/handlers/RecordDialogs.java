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
package io.github.mtrevisan.familylegacy.ui.handlers;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.dialogs.api.RecordDialog;

import java.awt.Window;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;


/**
 * Where the editors of the records are created, by the type of the record.
 * <p>
 * The handlers know the types, not the editors: the editors are registered by a {@link RecordDialogProvider}, found as a service, so
 * that this package does not depend on the packages of the editors.
 * </p>
 */
public final class RecordDialogs{

	/** Creates the editor of a new record. */
	@FunctionalInterface
	public interface NewDialog{

		RecordDialog create(Window parent, FLEFModel model);

	}

	/** Creates the editor of an existing record. */
	@FunctionalInterface
	public interface EditDialog{

		RecordDialog create(Window parent, FLEFModel model, FLEFRecord record);

	}


	private record Factory(NewDialog newDialog, EditDialog editDialog){}


	private static Map<String, Factory> factories;


	private RecordDialogs(){}


	/** The registration the providers use. */
	public static final class Registry{

		private final Map<String, Factory> factories = new HashMap<>();


		private Registry(){}


		/**
		 * @param type       the type of the record (e.g. {@code INDIVIDUAL})
		 * @param newDialog  creates the editor of a new record
		 * @param editDialog creates the editor of an existing record
		 */
		public void register(final String type, final NewDialog newDialog, final EditDialog editDialog){
			factories.put(type.toUpperCase(Locale.ROOT), new Factory(newDialog, editDialog));
		}

	}


	private static synchronized Map<String, Factory> factories(){
		if(factories == null){
			final Registry registry = new Registry();
			ServiceLoader.load(RecordDialogProvider.class)
				.forEach(provider -> provider.register(registry));
			factories = registry.factories;
		}
		return factories;
	}

	/** @return the types of record that have an editor */
	public static java.util.Set<String> registeredTypes(){
		return java.util.Set.copyOf(factories().keySet());
	}

	private static Factory factory(final String type){
		final Factory factory = factories().get(type.toUpperCase(Locale.ROOT));
		if(factory == null)
			throw new IllegalStateException("No editor is registered for the record type " + type);

		return factory;
	}

	/**
	 * @param type   the type of the record
	 * @param parent the parent window
	 * @param model  the model
	 * @return the editor of a new record (not shown yet)
	 * @throws IllegalStateException if no editor is registered for the type
	 */
	public static RecordDialog createNew(final String type, final Window parent, final FLEFModel model){
		return factory(type).newDialog()
			.create(parent, model);
	}

	/**
	 * @param type   the type of the record
	 * @param parent the parent window
	 * @param model  the model
	 * @param record the record to edit
	 * @return the editor of the record (not shown yet)
	 * @throws IllegalStateException if no editor is registered for the type
	 */
	public static RecordDialog createEdit(final String type, final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return factory(type).editDialog()
			.create(parent, model, record);
	}

}
