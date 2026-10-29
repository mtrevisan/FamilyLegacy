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
package io.github.mtrevisan.familylegacy.importer.gramps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;


/**
 * Extracts the XML of a Gramps file.
 * <p>
 * A {@code .gramps} file is an XML document, usually compressed with gzip; a {@code .gpkg} file is a tar archive (itself usually
 * compressed with gzip) that holds the XML as {@code data.gramps}, together with the media files.
 */
public final class GrampsPackageReader{

	private static final int GZIP_MAGIC_FIRST = 0x1F;
	private static final int GZIP_MAGIC_SECOND = 0x8B;
	private static final int TAR_BLOCK = 512;
	private static final int TAR_NAME_LENGTH = 100;
	private static final int TAR_SIZE_OFFSET = 124;
	private static final int TAR_SIZE_LENGTH = 12;
	private static final int TAR_MAGIC_OFFSET = 257;
	private static final String TAR_MAGIC = "ustar";
	private static final String PACKAGE_DATA_NAME = "data.gramps";


	private GrampsPackageReader(){}


	/**
	 * @param in	The content of a {@code .gramps} or {@code .gpkg} file.
	 * @return	The XML document.
	 * @throws IOException	If the content cannot be read, or the package has no data.
	 */
	public static byte[] readXml(final InputStream in) throws IOException{
		final byte[] content = gunzip(in.readAllBytes());
		if(!isTar(content))
			return content;

		final byte[] data = findEntry(content, PACKAGE_DATA_NAME);
		if(data == null)
			throw new IOException("The package has no " + PACKAGE_DATA_NAME);

		return gunzip(data);
	}

	/** Decompresses the content while it is compressed (a file can be compressed twice, as the package and its data). */
	private static byte[] gunzip(final byte[] content) throws IOException{
		byte[] current = content;
		while(current.length > 2 && (current[0] & 0xFF) == GZIP_MAGIC_FIRST && (current[1] & 0xFF) == GZIP_MAGIC_SECOND)
			try(final InputStream gzip = new GZIPInputStream(new ByteArrayInputStream(current))){
				current = gzip.readAllBytes();
			}
		return current;
	}

	private static boolean isTar(final byte[] content){
		return content.length >= TAR_BLOCK && TAR_MAGIC.equals(new String(content, TAR_MAGIC_OFFSET, TAR_MAGIC.length(),
			StandardCharsets.US_ASCII));
	}

	/** @return	The content of the entry of the archive whose name ends with the given one, or {@code null} if there is none. */
	private static byte[] findEntry(final byte[] archive, final String name) throws IOException{
		int position = 0;
		while(position + TAR_BLOCK <= archive.length){
			final String entryName = cString(archive, position, TAR_NAME_LENGTH);
			if(entryName.isEmpty())
				//the end of the archive
				return null;

			final int size = octal(cString(archive, position + TAR_SIZE_OFFSET, TAR_SIZE_LENGTH));
			final int start = position + TAR_BLOCK;
			if(entryName.endsWith(name)){
				if(start + size > archive.length)
					throw new IOException("The package is truncated");

				return Arrays.copyOfRange(archive, start, start + size);
			}

			//the content is padded to a whole block
			position = start + ((size + TAR_BLOCK - 1) / TAR_BLOCK) * TAR_BLOCK;
		}
		return null;
	}

	private static String cString(final byte[] content, final int offset, final int length){
		int end = offset;
		while(end < offset + length && content[end] != 0)
			end ++;
		return new String(content, offset, end - offset, StandardCharsets.UTF_8).trim();
	}

	private static int octal(final String text) throws IOException{
		try{
			return (text.isEmpty()? 0: Integer.parseInt(text, 8));
		}
		catch(final NumberFormatException nfe){
			throw new IOException("The package is not valid", nfe);
		}
	}

}
