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
package io.github.mtrevisan.familylegacy.social;

import java.util.List;
import java.util.Map;


/**
 * One person who appears together with the subject of a FAN table (Friends, Associates, Neighbors) in the documents: the events they
 * share, the roles each of them had, when and where.
 *
 * @param personId       the identifier of the other person
 * @param name           the name of the other person
 * @param eventIds       the identifiers of the events the two share, the oldest first (the undated last)
 * @param linkTypes      how many different pairs of roles (the subject's, the other person's) link the two (for example
 *                       principal-witness and principal-midwife make two)
 * @param score          how strong the link is (see {@link FanService}): the higher, the stronger
 * @param ownRoles       how many shared events the subject had each role in
 * @param otherRoles     how many times the other person had each role in the shared events
 * @param firstYear      the year of the oldest dated shared event ({@code null} if none is dated)
 * @param lastYear       the year of the newest dated shared event ({@code null} if none is dated)
 * @param places         the names of the places of the shared events, without repetitions
 * @param kin            whether the two are known to be related (spouses, parent and child, siblings): such links are not what a FAN
 *                       is looking for, so they are listed after the others
 * @param roleTotal      how many times the other person has a role other than principal in the whole file: a person that is a witness
 *                       in dozens of events is probably the sacristan, not a friend
 */
public record FanEntry(String personId, String name, List<String> eventIds, int linkTypes, int score, Map<String, Integer> ownRoles,
		Map<String, Integer> otherRoles, Integer firstYear, Integer lastYear, List<String> places, boolean kin, int roleTotal){

	public FanEntry{
		eventIds = List.copyOf(eventIds);
		ownRoles = Map.copyOf(ownRoles);
		otherRoles = Map.copyOf(otherRoles);
		places = List.copyOf(places);
	}


	/** @return how many events the two share */
	public int sharedEvents(){
		return eventIds.size();
	}

}
