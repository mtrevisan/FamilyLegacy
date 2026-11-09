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

import java.io.InputStream;
import java.util.Properties;


/**
 * Who the software is: what is written in the header of a file as the software that wrote it ({@code source.name}, {@code source.version},
 * {@code source.organization}), and what the application shows as its name and version.
 * <p>
 * The version is the one of the project (the version of the parent pom, filtered into {@code software.properties} when the project is
 * built). A file that is converted from another format (GEDCOM, Gramps) is written by this software, whatever wrote the original.
 * </p>
 */
public final class SoftwareInfo{

	public static final String NAME = "Family Legacy";
	public static final String ORGANIZATION = "Mauro Trevisan";

	private static final String VERSION;
	static{
		String version = "unknown";
		try(final InputStream is = SoftwareInfo.class.getResourceAsStream("/software.properties")){
			if(is != null){
				final Properties properties = new Properties();
				properties.load(is);
				version = properties.getProperty("software.version", version);
			}
		}
		catch(final Exception ignored){}

		VERSION = version;
	}


	private SoftwareInfo(){}


	/** @return	The version of the project (for example {@code 1.0.0}), {@code unknown} if it cannot be read. */
	public static String version(){
		return VERSION;
	}

}
