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
import io.github.mtrevisan.familylegacy.search.SearchCriteria;
import io.github.mtrevisan.familylegacy.search.SearchMode;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.ShortcutRegistry;
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
				.search(new SearchCriteria(HandlerRegistry.getHandler(type).getType(), "", Set.of(SearchMode.FUZZY))), type.getSimpleName());
	}

	@Test
	void aTextQueryFindsRecordsOfDifferentTypes(){
		final FLEFModel model = new FLEFParser().parse(FLEF);
		final SearchService service = new SearchService(model);

		final List<FLEFRecord> people = service.search(new SearchCriteria(IndividualHandler.getInstance().getType(), "Anna", Set.of(SearchMode.FUZZY)));
		final List<FLEFRecord> places = service.search(new SearchCriteria(PlaceHandler.getInstance().getType(), "Treviso", Set.of(SearchMode.FUZZY)));
		final List<FLEFRecord> sources = service.search(new SearchCriteria(SourceHandler.getInstance().getType(), "register", Set.of(SearchMode.FUZZY)));

		Assertions.assertEquals(List.of("I1"), people.stream().map(FLEFRecord::getId).toList());
		Assertions.assertEquals(List.of("P1"), places.stream().map(FLEFRecord::getId).toList());
		Assertions.assertEquals(List.of("S1"), sources.stream().map(FLEFRecord::getId).toList());
	}

	@Test
	void theTextOfTheNotesIsSearchedOnlyWhenAsked(){
		final String flef = """
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
				note {
					title Emigration
					text Left for Argentina in the spring
				}
			}
			individual I2 {
				name {
					part {
						type given
						value Berta
					}
				}
				note {
					text Nothing to say
					translation {
						text Partita per l'Argentina
					}
				}
			}
			individual I3 {
				name {
					part {
						type given
						value Carla
					}
				}
				source {
					source S1
					locator Folio 12 verso
					extract {
						text Battezzata il giorno di Pasqua
					}
				}
			}
			""";
		final FLEFModel model = new FLEFParser().parse(flef);
		final SearchService service = new SearchService(model);
		final String type = IndividualHandler.getInstance().getType();

		Assertions.assertEquals(List.of(), service.search(new SearchCriteria(type, "Argentina", Set.of())),
			"without the option the notes are not read");
		Assertions.assertEquals(List.of("I1", "I2"), service.search(new SearchCriteria(type, "Argentina", Set.of(SearchMode.NOTES_AND_CITATIONS)))
			.stream().map(FLEFRecord::getId).sorted().toList(), "the text of a note, and of a translation");
		Assertions.assertEquals(List.of("I3"), service.search(new SearchCriteria(type, "pasqua", Set.of(SearchMode.NOTES_AND_CITATIONS)))
			.stream().map(FLEFRecord::getId).toList(), "the text of an extract of a citation");
		Assertions.assertEquals(List.of("I3"), service.search(new SearchCriteria(type, "verso", Set.of(SearchMode.NOTES_AND_CITATIONS)))
			.stream().map(FLEFRecord::getId).toList(), "the locator of a citation");
		Assertions.assertEquals(List.of(), service.search(new SearchCriteria(type, "pasqua", Set.of())), "not without the option");
		Assertions.assertEquals(List.of("I1"), service.search(new SearchCriteria(type, "emigration", Set.of(SearchMode.NOTES_AND_CITATIONS)))
			.stream().map(FLEFRecord::getId).toList(), "the title of a note");
		Assertions.assertEquals(List.of("I1", "I2"), service.search(new SearchCriteria(type, "Argentna spring",
				Set.of(SearchMode.NOTES_AND_CITATIONS, SearchMode.FUZZY, SearchMode.WHOLE_WORD)))
			.stream().map(FLEFRecord::getId).toList(), "together with the fuzzy and whole word modes (a typo, ranked by tokens)");
		Assertions.assertEquals(List.of("I1"), service.search(new SearchCriteria(type, "Anna argentina", Set.of(SearchMode.NOTES_AND_CITATIONS)))
			.stream().map(FLEFRecord::getId).toList().subList(0, 1), "a name and a note together rank first");
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
