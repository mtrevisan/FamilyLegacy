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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;


public final class JavaHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(JavaHelper.class);


	private JavaHelper(){}


	public static Properties getProperties(final String filename){
		final Properties rulesProperties = new Properties();
		final InputStream is = JavaHelper.class.getResourceAsStream(filename);
		if(is != null){
			try(final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)){
				rulesProperties.load(isr);
			}
			catch(final IOException e){
				LOGGER.error("Cannot load ANSEL table", e);
			}
		}
		return rulesProperties;
	}


	public static String textFormat(final String message, final Object... parameters){
		return MessageFormatter.arrayFormat(message, parameters)
			.getMessage();
	}


	public static <K, V> Map<K, V> deepClone(final Map<K, V> original){
		if(original == null)
			return null;

		try{
			final byte[] byteArray = serializeToByteArray(original);
			return deserializeFromByteArray(byteArray);
		}
		catch(final IOException | ClassNotFoundException e){
			e.printStackTrace();

			return null;
		}
	}

	private static <K, V> byte[] serializeToByteArray(final Map<K, V> original) throws IOException{
		try(
				final ByteArrayOutputStream bos = new ByteArrayOutputStream();
				final ObjectOutput out = new ObjectOutputStream(bos)){
			out.writeObject(original);
			return bos.toByteArray();
		}
	}

	@SuppressWarnings("unchecked")
	private static <K, V> Map<K, V> deserializeFromByteArray(final byte[] byteArray) throws IOException, ClassNotFoundException{
		try(
				final ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
				final ObjectInputStream in = new ObjectInputStream(bis)){
			return (Map<K, V>)in.readObject();
		}
	}

}
