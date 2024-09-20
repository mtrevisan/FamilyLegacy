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
package io.github.mtrevisan.familylegacy.flef.helpers;

import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;


public class LZMAManager{

	private LZMAManager(){}


	public static byte[] compress(final String input) throws IOException{
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try(final XZOutputStream xzOutputStream = new XZOutputStream(byteArrayOutputStream, new LZMA2Options(4))){
			xzOutputStream.write(input.getBytes(StandardCharsets.UTF_8));
		}
		return byteArrayOutputStream.toByteArray();
	}

	public static String decompress(final byte[] compressedData) throws IOException{
		final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedData);
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try(final XZInputStream xzInputStream = new XZInputStream(byteArrayInputStream)){
			final byte[] buffer = new byte[1024];
			int len;
			while((len = xzInputStream.read(buffer)) >= 0)
				byteArrayOutputStream.write(buffer, 0, len);
		}
		return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
	}

	/**
	 * Converts an array of bytes into a string representing the hexadecimal values of each byte in order.
	 *
	 * @param array	Array to be converted to hexadecimal characters.
	 * @return	The hexadecimal characters.
	 */
	public static String toHexString(final byte[] array){
		final int length = (array != null? array.length: 0);
		final char[] hexChars = new char[length << 1];
		for(int i = 0; i < length; i ++){
			final int elem = array[i] & 0xFF;

			final char highDigit = Character.forDigit((elem >>> 4) & 0x0F, 16);
			final char lowDigit = Character.forDigit(elem & 0x0F, 16);
			hexChars[i << 1] = highDigit;
			hexChars[(i << 1) + 1] = lowDigit;
		}
		return new String(hexChars)
			.toUpperCase(Locale.ROOT);
	}

}
