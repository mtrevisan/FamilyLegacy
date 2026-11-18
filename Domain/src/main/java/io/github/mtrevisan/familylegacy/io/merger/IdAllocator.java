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
package io.github.mtrevisan.familylegacy.io.merger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Hands out record ids that are free in the file being built.
 * <p>
 * Ids are unique within a file and a file must not contain two ids that differ only in case, so ids are compared ignoring case. A
 * new id keeps the prefix of the id it replaces ({@code I12} becomes {@code I<n>}), which carries no meaning but keeps the file
 * readable.
 */
final class IdAllocator{

	private static final String DEFAULT_PREFIX = "R";


	private final Set<String> used = new HashSet<>();
	private final Map<String, Long> lastNumberByPrefix = new HashMap<>();


	void reserve(final String id){
		used.add(id.toLowerCase(Locale.ROOT));

		final String prefix = prefixOf(id);
		final String number = id.substring(prefix.length());
		if(!number.isEmpty() && number.length() < 18)
			lastNumberByPrefix.merge(prefix.toLowerCase(Locale.ROOT), Long.parseLong(number), Math::max);
	}

	boolean isTaken(final String id){
		return used.contains(id.toLowerCase(Locale.ROOT));
	}

	/**
	 * Reserves and returns a new id with the same prefix as {@code template}.
	 */
	String allocate(final String template){
		String prefix = prefixOf(template);
		if(prefix.isEmpty())
			prefix = DEFAULT_PREFIX;

		final String key = prefix.toLowerCase(Locale.ROOT);
		long next = lastNumberByPrefix.getOrDefault(key, 0L);
		String id;
		do{
			next ++;
			id = prefix + next;
		}while(isTaken(id));

		reserve(id);
		return id;
	}

	/** The id without its trailing digits. */
	private static String prefixOf(final String id){
		int end = id.length();
		while(end > 0 && Character.isDigit(id.charAt(end - 1)))
			end --;
		return id.substring(0, end);
	}

}
