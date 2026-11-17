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

package io.github.mtrevisan.familylegacy.externalsearch.flef;

import io.github.mtrevisan.familylegacy.externalsearch.Placeholder;
import io.github.mtrevisan.familylegacy.externalsearch.SearchSubject;
import io.github.mtrevisan.familylegacy.privacy.PrivacyLevel;

import java.util.List;


/**
 * What an individual gives to a search: its names, and the other values (sex, years and places of birth and death, relatives).
 *
 * @param individualId	The identifier of the individual.
 * @param names	The names the individual can be searched by, the first one first as written in the file; may be empty.
 * @param facts	The values that are not the name.
 * @param privacy	The effective privacy of the individual.
 * @param mayBeLiving	Whether the individual may be living.
 */
public record SearchProfile(String individualId, List<NameCandidate> names, SearchSubject facts, PrivacyLevel privacy,
		boolean mayBeLiving){

	public SearchProfile{
		names = List.copyOf(names);
	}


	/**
	 * @param name	A name of the person.
	 * @return	The values of a search by that name.
	 */
	public SearchSubject subjectFor(final NameCandidate name){
		SearchSubject subject = facts;
		for(final Placeholder placeholder : Placeholder.values())
			if(placeholder.isName())
				subject = subject.with(placeholder, null);
		return subject.withAll(name.asSubject());
	}

	/**
	 * @return	Whether sending the name of the individual to a site needs the consent of the user: it may be living, or the file
	 * 	restricts it.
	 */
	public boolean needsConsent(){
		return (mayBeLiving || privacy != PrivacyLevel.PUBLIC);
	}

}
