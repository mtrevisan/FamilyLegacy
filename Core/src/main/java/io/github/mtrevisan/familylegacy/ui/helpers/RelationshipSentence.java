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
package io.github.mtrevisan.familylegacy.ui.helpers;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;


/**
 * Says a relationship as a sentence read in the natural direction ("Alice is the biological child of John"), whatever the way the
 * record stores it (the subject is the child, the target is the parent).
 * <p>
 * The wording of every type is in the messages ({@code text.relationship.sentence.<type>}); the types that take a role in the
 * sentence have a second key, with the suffix {@code .role}.
 */
public final class RelationshipSentence{

	public static final String KEY_PREFIX = "text.relationship.sentence.";
	public static final String KEY_ROLE_SUFFIX = ".role";

	/** The types whose sentence tells the role. */
	private static final Set<String> ROLE_TYPES = Set.of(RelationshipReader.ENUM_TYPE_ASSOCIATE, RelationshipReader.ENUM_TYPE_GROUP_MEMBER);


	private RelationshipSentence(){}


	/**
	 * @param type	The type of the relationship.
	 * @param subject	The name of the subject.
	 * @param target	The name of the target.
	 * @param role	The role of the subject (it is told only by the types that use it).
	 * @return	The sentence, or an empty string if there is no type.
	 */
	public static String of(final String type, final String subject, final String target, final String role){
		if(StringUtils.isBlank(type))
			return StringUtils.EMPTY;

		final String subjectName = StringUtils.defaultIfBlank(subject, "--");
		final String targetName = StringUtils.defaultIfBlank(target, "--");

		final String key = KEY_PREFIX + type;
		if(ROLE_TYPES.contains(type) && StringUtils.isNotBlank(role))
			return I18N.tf(key + KEY_ROLE_SUFFIX, subjectName, targetName, role.strip());
		if(I18N.t(key, null) != null)
			return I18N.tf(key, subjectName, targetName);

		//a type with no wording of its own
		return I18N.tf("dialog.relationship.display_without_role", subjectName, targetName,
			I18N.t("enum.relationship.type." + type, type.replace('_', ' ')));
	}

}
