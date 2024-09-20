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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

}
