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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.ui.assistant.identity.CompareRecordsAction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The key of an action of a finding is the same wherever the action is offered. */
class DiagnosticActionsKeysTest{

	private static DiagnosticAction action(final int slot){
		return new DiagnosticAction(){
			@Override
			public String title(){
				return "slot " + slot;
			}

			@Override
			public int keySlot(){
				return slot;
			}

			@Override
			public void execute(final AssistantContext context){}
		};
	}


	@Test
	void eachActionKeepsItsKeyWhateverTheOthers(){
		final DiagnosticAction compare = action(1);
		final DiagnosticAction open = action(3);
		final DiagnosticAction draft = action(0);

		//a finding whose first action is the comparison: it is run with Ctrl+Enter as everywhere, and Enter does it too
		final List<DiagnosticAction> withoutDraft = List.of(compare, open);
		Assertions.assertSame(compare, DiagnosticActions.forSlot(withoutDraft, 1));
		Assertions.assertSame(compare, DiagnosticActions.forSlot(withoutDraft, 0));
		Assertions.assertSame(open, DiagnosticActions.forSlot(withoutDraft, 3));
		Assertions.assertNull(DiagnosticActions.forSlot(withoutDraft, 2));

		//with a draft Enter is the draft, and the comparison stays on Ctrl+Enter
		final List<DiagnosticAction> withDraft = List.of(draft, compare, open);
		Assertions.assertSame(draft, DiagnosticActions.forSlot(withDraft, 0));
		Assertions.assertSame(compare, DiagnosticActions.forSlot(withDraft, 1));
		Assertions.assertSame(open, DiagnosticActions.forSlot(withDraft, 3));
	}

	@Test
	void theKnownActionsHaveTheirOwnKey(){
		Assertions.assertEquals(1, new CompareRecordsAction(null, null, null).keySlot());
		Assertions.assertEquals(3, new OpenRecordsAction(List.of()).keySlot());
	}

}
