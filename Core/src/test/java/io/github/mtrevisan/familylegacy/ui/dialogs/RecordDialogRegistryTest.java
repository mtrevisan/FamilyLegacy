package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordDialogs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;


class RecordDialogRegistryTest{

	/**
	 * The handlers do not name the editors: the catalog registers them. A type with a handler and no editor would fail only when the user
	 * opens it, so every type of the handlers has to have its editor.
	 */
	@Test
	void everyTypeOfTheHandlersHasItsEditor(){
		final Set<String> missing = new TreeSet<>(HandlerRegistry.types());
		missing.removeAll(RecordDialogs.registeredTypes());

		Assertions.assertTrue(missing.isEmpty(), "No editor is registered for " + missing);
		Assertions.assertFalse(HandlerRegistry.types().isEmpty());
	}

	@Test
	void aTypeWithoutAnEditorIsReportedClearly(){
		final IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
			() -> RecordDialogs.createNew("NO_SUCH_TYPE", null, null));

		Assertions.assertTrue(e.getMessage().contains("NO_SUCH_TYPE"), e.getMessage());
	}

}
