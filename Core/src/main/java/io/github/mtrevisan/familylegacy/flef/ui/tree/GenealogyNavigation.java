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
package io.github.mtrevisan.familylegacy.flef.ui.tree;

import java.util.ArrayDeque;
import java.util.Deque;


public class GenealogyNavigation{

	private static final Integer NO_ID = -1;


	private final Deque<Integer> backwardHistoryPerson = new ArrayDeque<>();
	private final Deque<Integer> backwardHistoryUnion = new ArrayDeque<>();
	private final Deque<Integer> forwardHistoryPerson = new ArrayDeque<>();
	private final Deque<Integer> forwardHistoryUnion = new ArrayDeque<>();


	public void navigateToPerson(final int personID){
		backwardHistoryPerson.push(personID);
		backwardHistoryUnion.push(NO_ID);
	}

	public void navigateToUnion(final int unionID){
		backwardHistoryPerson.push(NO_ID);
		backwardHistoryUnion.push(unionID);
	}

	public void navigateTo(final int personID, final int unionID){
		backwardHistoryPerson.push(personID);
		backwardHistoryUnion.push(unionID);
	}

	public boolean goBack(){
		final boolean hasData = canGoBack();
		if(hasData){
			final Integer personID = backwardHistoryPerson.pop();
			final Integer unionID = backwardHistoryUnion.pop();

			//move current status to forward history
			forwardHistoryPerson.push(personID);
			forwardHistoryUnion.push(unionID);
		}
		return hasData;
	}

	public boolean goForward(){
		final boolean hasData = canGoForward();
		if(hasData){
			final Integer personID = forwardHistoryPerson.pop();
			final Integer unionID = forwardHistoryUnion.pop();

			//move current status to navigation history
			backwardHistoryPerson.push(personID);
			backwardHistoryUnion.push(unionID);
		}
		return hasData;
	}

	public Integer getLastPersonID(){
		return (!backwardHistoryPerson.isEmpty()? getValidID(backwardHistoryPerson.peek()): null);
	}

	public Integer getLastUnionID(){
		return (!backwardHistoryUnion.isEmpty()? getValidID(backwardHistoryUnion.peek()): null);
	}

	private static Integer getValidID(final Integer id){
		return (id != null && id > 0? id: null);
	}

	public boolean canGoBack(){
		return (backwardHistoryPerson.size() > 1);
	}

	public boolean canGoForward(){
		return !forwardHistoryPerson.isEmpty();
	}

}
