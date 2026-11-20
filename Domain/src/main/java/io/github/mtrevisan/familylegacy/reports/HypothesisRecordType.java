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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;


/** Which identity hypotheses the Identity Hypotheses report lists, by the kind of record that may be a duplicate. */
public enum HypothesisRecordType{

	/** Every hypothesis. */
	ALL,
	/** Possible duplicate individuals. */
	INDIVIDUAL,
	/** Possible duplicate places. */
	PLACE,
	/** Possible duplicate sources. */
	SOURCE,
	/** Possible duplicate repositories. */
	REPOSITORY,
	/** Possible duplicates of any other kind (groups, media, cultural norms, historic events). */
	OTHER;


	/** @param candidateTag	The tag of the candidates of the hypothesis ({@code individual}, {@code place}, ...). */
	public boolean accepts(final String candidateTag){
		return switch(this){
			case ALL -> true;
			case INDIVIDUAL -> IdentityHypothesisReader.TAG_IDENTITY_INDIVIDUAL.equals(candidateTag);
			case PLACE -> IdentityHypothesisReader.TAG_IDENTITY_PLACE.equals(candidateTag);
			case SOURCE -> IdentityHypothesisReader.TAG_IDENTITY_SOURCE.equals(candidateTag);
			case REPOSITORY -> IdentityHypothesisReader.TAG_IDENTITY_REPOSITORY.equals(candidateTag);
			case OTHER -> !(IdentityHypothesisReader.TAG_IDENTITY_INDIVIDUAL.equals(candidateTag) || IdentityHypothesisReader.TAG_IDENTITY_PLACE.equals(candidateTag) || IdentityHypothesisReader.TAG_IDENTITY_SOURCE.equals(candidateTag)
				|| IdentityHypothesisReader.TAG_IDENTITY_REPOSITORY.equals(candidateTag));
		};
	}

}
