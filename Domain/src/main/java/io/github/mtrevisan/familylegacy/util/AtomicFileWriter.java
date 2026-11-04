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
package io.github.mtrevisan.familylegacy.util;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;


/**
 * Writes a file without ever leaving it half written: the content goes first into a temporary file in the same directory, which
 * is forced to the disk and then moved over the target. If anything fails, the target is exactly as it was and the temporary file
 * is removed.
 */
public final class AtomicFileWriter{

	private static final String TEMPORARY_SUFFIX = ".tmp";


	private AtomicFileWriter(){}


	/**
	 * Writes the text to the file, replacing it if it exists.
	 *
	 * @param target	The file to write (its directory must exist).
	 * @param content	The text.
	 * @param charset	The encoding of the text.
	 * @throws IOException	If the content cannot be written or the file replaced; the target is not touched in that case.
	 */
	public static void write(final Path target, final String content, final Charset charset) throws IOException{
		final Path absolute = target.toAbsolutePath();
		final Path directory = absolute.getParent();
		final Path temporary = Files.createTempFile(directory, absolute.getFileName().toString() + ".", TEMPORARY_SUFFIX);
		try{
			Files.writeString(temporary, content, charset);
			//the bytes must be on the disk before the old file disappears
			try(final FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)){
				channel.force(true);
			}

			try{
				Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch(final AtomicMoveNotSupportedException ignored){
				//the file system cannot move atomically: still replace the file in a single call
				Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch(final IOException | RuntimeException e){
			Files.deleteIfExists(temporary);
			throw e;
		}
	}

}
