/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.panels;

import java.util.Map;


public interface GroupListenerInterface{

	void onGroupEdit(GroupPanel groupPanel);

	void onGroupAdd(GroupPanel groupPanel);

	void onGroupLink(GroupPanel groupPanel);

	void onGroupRemove(GroupPanel groupPanel);

	/**
	 * Iterate through parents of a person.
	 *
	 * @param groupPanel	The current group panel from which to iterate.
	 * @param personPanel	The current person (that remain the same).
	 * @param newParents	The union that has to change.
	 */
	void onPersonChangeParents(GroupPanel groupPanel, PersonPanel personPanel, Map<String, Object> newParents);

	/**
	 * Iterate through unions of a person.
	 *
	 * @param groupPanel	The current union panel from which to iterate.
	 * @param oldPartner	The current partner (that has to change).
	 * @param newPartner	The new partner.
	 * @param newUnion	The union that has to change to.
	 */
	void onPersonChangeUnion(GroupPanel groupPanel, PersonPanel oldPartner, Map<String, Object> newPartner,
		Map<String, Object> newUnion);

}
