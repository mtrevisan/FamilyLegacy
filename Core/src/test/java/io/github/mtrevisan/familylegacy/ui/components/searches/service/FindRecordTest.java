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
package io.github.mtrevisan.familylegacy.ui.components.searches.service;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchMode;
import io.github.mtrevisan.familylegacy.ui.dialogs.help.ShortcutRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/** The global search (Ctrl+F): every type of record that has a search strategy can be found, the people first. */
class FindRecordTest{

	private static final String FLEF = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		individual I1 {
			name {
				part {
					type given
					value Anna
				}
			}
		}
		place P1 {
			name {
				value Treviso
			}
		}
		source S1 {
			title.value Parish register of Treviso
		}
		""";


	@Test
	void everyTypeOfTheListHasAHandlerAndTheTypesAreNotRepeated(){
		final List<Class<? extends RecordTypeHandler<?>>> types = SearchService.searchableHandlers();

		Assertions.assertEquals(IndividualHandler.class, types.getFirst(), "the people first");
		Assertions.assertEquals(types.size(), new HashSet<>(types).size());
		for(final Class<? extends RecordTypeHandler<?>> type : types)
			Assertions.assertNotNull(HandlerRegistry.getHandler(type), type.getSimpleName());
	}

	@Test
	void everyTypeOfTheListCanBeSearched(){
		final FLEFModel model = new FLEFParser().parse(FLEF);

		for(final Class<? extends RecordTypeHandler<?>> type : SearchService.searchableHandlers())
			Assertions.assertDoesNotThrow(() -> new SearchService(model)
				.search(new SearchCriteria(HandlerRegistry.getHandler(type), "", Set.of(SearchMode.FUZZY))), type.getSimpleName());
	}

	@Test
	void aTextQueryFindsRecordsOfDifferentTypes(){
		final FLEFModel model = new FLEFParser().parse(FLEF);
		final SearchService service = new SearchService(model);

		final List<FLEFRecord> people = service.search(new SearchCriteria(IndividualHandler.getInstance(), "Anna", Set.of(SearchMode.FUZZY)));
		final List<FLEFRecord> places = service.search(new SearchCriteria(PlaceHandler.getInstance(), "Treviso", Set.of(SearchMode.FUZZY)));
		final List<FLEFRecord> sources = service.search(new SearchCriteria(SourceHandler.getInstance(), "register", Set.of(SearchMode.FUZZY)));

		Assertions.assertEquals(List.of("I1"), people.stream().map(FLEFRecord::getId).toList());
		Assertions.assertEquals(List.of("P1"), places.stream().map(FLEFRecord::getId).toList());
		Assertions.assertEquals(List.of("S1"), sources.stream().map(FLEFRecord::getId).toList());
	}

	@Test
	void controlFIsTheShortcutOfTheSearchAndNothingElseUsesItInTheNavigation(){
		Assertions.assertEquals(KeyEvent.VK_F, ShortcutRegistry.NAV_FIND_RECORD.keyStroke().getKeyCode());

		final Set<javax.swing.KeyStroke> others = new HashSet<>();
		ShortcutRegistry.getAllShortcuts().stream()
			.filter(shortcut -> shortcut != ShortcutRegistry.NAV_FIND_RECORD)
			.forEach(shortcut -> others.add(shortcut.keyStroke()));
		Assertions.assertFalse(others.contains(ShortcutRegistry.NAV_FIND_RECORD.keyStroke()), "no other action has Ctrl+F");
	}

}
