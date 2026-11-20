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
package io.github.mtrevisan.familylegacy.events;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;


/**
 * Semantic knowledge about one built-in event type (one of {@link io.github.mtrevisan.familylegacy.io.model.readers.EventReader#TYPES}).
 * <p>
 * This is application knowledge, <b>not</b> part of the FLEF grammar: every expectation is advisory (genealogical data is often
 * incomplete), so a violation is a warning, never an error. Custom (free text) event types have no entry.
 *
 * @param type	The protocol value of the event type (e.g. {@code marriage}).
 * @param principals	The expected number of principal participants (1 or 2).
 * @param allowedAfterDeath	Whether the event may legitimately occur after the death of its principals.
 * @param marital	Whether the event is part of the marriage lifecycle.
 * @param defaultRoles	The participant roles typically present in the event, in presentation order, the principal first.
 */
public record EventTypeInfo(String type, int principals, boolean allowedAfterDeath, boolean marital, Set<String> defaultRoles){

	public EventTypeInfo{
		Objects.requireNonNull(type, "type");
		if(principals < 1 || principals > 2)
			throw new IllegalArgumentException("Principals must be 1 or 2, was " + principals);

		defaultRoles = Collections.unmodifiableSet(new LinkedHashSet<>(defaultRoles));
	}

}
