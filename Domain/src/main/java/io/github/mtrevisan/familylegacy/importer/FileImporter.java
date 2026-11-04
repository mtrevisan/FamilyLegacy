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
package io.github.mtrevisan.familylegacy.importer;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;


/**
 * The reader of a file format of another genealogy program, that converts it into a FLEF model (the SPI of the importers).
 * <p>
 * The formats are found as {@link java.util.ServiceLoader} providers (see {@link FileImporters}): each one is a plugin, a jar
 * {@code importer-<id>.jar} in the {@code plugins} directory, so that a format is offered only if its jar is there. An importer only
 * <em>reads</em>: it never touches the model of the file being edited, it gives a new one.
 * </p>
 */
public interface FileImporter{

	/** Told how far the reading is. */
	@FunctionalInterface
	interface ProgressListener{

		/**
		 * @param percent  0-100, or a negative value for a step that has no measure
		 * @param message  short description of the current step
		 */
		void onProgress(int percent, String message);

	}


	/** @return	What identifies the format and names the jar of its plugin ({@code importer-<id>.jar}): {@code "gedcom"}, {@code "gramps"}. */
	String id();

	/** @return	The (localized) label of the entry of the menu, including the trailing ellipsis. */
	String menuLabel();

	/** @return	The (localized) title of the dialogs of the import. */
	String dialogTitle();

	/** @return	The (localized) description of the files of the format, for the file chooser. */
	String fileDescription();

	/** @return	The extensions of the files of the format, without the dot and in lower case. */
	List<String> fileExtensions();

	/** @return	The (localized) message to show when the file is not a valid document of the format. */
	String invalidFileMessage();

	/**
	 * Reads the file and converts it into a model. It can take long: call it away from the event dispatch thread.
	 *
	 * @param file      the file to read
	 * @param progress  told how far the reading is
	 * @return	A new model, with the content of the file.
	 * @throws IOException	If the file cannot be read, or it is not a valid document of the format.
	 */
	FLEFModel read(Path file, ProgressListener progress) throws IOException;

}
