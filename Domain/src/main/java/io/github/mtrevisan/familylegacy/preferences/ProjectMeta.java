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
package io.github.mtrevisan.familylegacy.preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import io.github.mtrevisan.familylegacy.util.AtomicFileWriter;


/**
 * Reads and writes the properties files that the application keeps next to a FLEF file, in its {@code .meta} folder (see
 * {@link TagStore#metaDirectory(String)}): data about the project that is not genealogy, so it is not in the FLEF file, but that must
 * travel with the file.
 * <p>
 * A file is written through a temporary file; a missing or corrupt one reads as empty, and never blocks the opening of the project.
 */
public final class ProjectMeta{

	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectMeta.class);

	/** The first line comment of the files: the version of their format. */
	private static final int FORMAT_VERSION = 1;


	private ProjectMeta(){}


	/**
	 * @param documentPath	The path of the FLEF file.
	 * @param name	The name of the file in the {@code .meta} folder.
	 * @return	The path of that file (that may not exist).
	 */
	public static Path file(final String documentPath, final String name){
		return TagStore.metaDirectory(documentPath)
			.resolve(name);
	}

	/** @return	The content of the file, empty if it is missing or cannot be read. */
	public static Properties read(final Path file){
		final Properties props = new Properties();
		if(Files.exists(file))
			try(final InputStream is = Files.newInputStream(file)){
				props.load(is);
			}
			catch(final IOException | IllegalArgumentException e){
				LOGGER.warn("Could not load {}: {}", file, e.getMessage());
				props.clear();
			}
		return props;
	}

	/**
	 * Writes the file, creating the {@code .meta} folder if needed.
	 *
	 * @param what	What the file keeps (written in its first line).
	 * @return	Whether the file was written.
	 */
	public static boolean write(final Path file, final Properties props, final String what){
		try{
			Files.createDirectories(file.getParent());
			final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			props.store(bytes, "FamilyLegacy " + what + ", format " + FORMAT_VERSION);
			AtomicFileWriter.write(file, bytes.toString(StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1);
			return true;
		}
		catch(final IOException e){
			LOGGER.warn("Could not save {}: {}", file, e.getMessage());
			return false;
		}
	}

}
