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
package io.github.mtrevisan.familylegacy.flef.ui.dialogs;

import io.github.mtrevisan.familylegacy.flef.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordCalendarOriginalID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordCitationID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordCulturalNormID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordDateID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordGroupID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordMediaID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordPersonID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordPhotoCrop;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordPhotoID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordPlaceID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordSourceID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordCalendarOriginalID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordPersonID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordPersonNameID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordPhotoCrop;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordPlaceID;


public class Main{

	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
//			final RepositoryDialog dialog = RepositoryDialog.create(store, parent);
//			final HistoricDateDialog dialog = HistoricDateDialog.create(store, parent);
//			final PlaceDialog dialog = PlaceDialog.create(store, parent);
//			final MediaDialog dialog = MediaDialog.create(store, parent);
//			final PersonDialog dialog = PersonDialog.create(store, parent);
//			final GroupDialog dialog = GroupDialog.create(store, parent);
//			final EventDialog dialog = EventDialog.create(store, parent);
//			final CulturalNormDialog dialog = CulturalNormDialog.create(store, parent);
			final ResearchStatusDialog dialog = ResearchStatusDialog.create(store, parent);
//			final ProjectDialog dialog = ProjectDialog.create(store, parent);
			dialog.loadData();

			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){
					final Map<String, Object> container = editCommand.getContainer();
					final String tableName = editCommand.getIdentifier();
					final Integer recordID = extractRecordID(container);
					switch(editCommand.getType()){
						//from: ?
//						case REPOSITORY -> {
//							final RepositoryDialog repositoryDialog = RepositoryDialog.create(store, parent);
//							repositoryDialog.loadData();
//							final Integer repositoryID = extractRecordRepositoryID(container);
//							if(repositoryID != null)
//								repositoryDialog.selectData(repositoryID);
//
//							repositoryDialog.show();
//						}

						//from: repository
						case SOURCE -> {
							final SourceDialog sourceDialog = SourceDialog.create(store, parent)
								.withFilterOnRepositoryID(recordID)
								.withOnCloseGracefully(record -> {
									if(record != null)
										record.put("repository_id", recordID);
								});
							sourceDialog.loadData();
							final Integer sourceID = extractRecordSourceID(container);
							if(sourceID != null)
								sourceDialog.selectData(sourceID);

							sourceDialog.showDialog();
						}

						//from: source
						case CITATION -> {
							final CitationDialog citationDialog = CitationDialog.create(store, parent)
								.withFilterOnSourceID(recordID)
								.withOnCloseGracefully(record -> {
									if(record != null)
										record.put("source_id", recordID);
								});
							citationDialog.loadData();
							final Integer citationID = extractRecordCitationID(container);
							if(citationID != null)
								citationDialog.selectData(citationID);

							citationDialog.showDialog();
						}

						//from: citation, person, person name, group, media, place, cultural norm, historic date, calendar
						case ASSERTION -> {
							final AssertionDialog assertionDialog = AssertionDialog.create(store, parent)
								.withReference(tableName, recordID);
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}


						//from: source, event, cultural norm, media
						case HISTORIC_DATE -> {
							final HistoricDateDialog historicDateDialog = HistoricDateDialog.create(store, parent);
							historicDateDialog.loadData();
							final Integer dateID = extractRecordDateID(container);
							if(dateID != null)
								historicDateDialog.selectData(dateID);

							historicDateDialog.showDialog();
						}

						//from: historic date
						case CALENDAR_ORIGINAL -> {
							final CalendarDialog calendarDialog = CalendarDialog.create(store, parent)
								.withOnCloseGracefully(record -> insertRecordCalendarOriginalID(container, extractRecordID(record)));
							calendarDialog.loadData();
							final Integer calendarID = extractRecordCalendarOriginalID(container);
							if(calendarID != null)
								calendarDialog.selectData(calendarID);

							calendarDialog.showDialog();
						}


						//from: repository, source, event, cultural norm
						case PLACE -> {
							final PlaceDialog placeDialog = PlaceDialog.create(store, parent)
								.withOnCloseGracefully(record -> insertRecordPlaceID(container, extractRecordID(record)));
							placeDialog.loadData();
							final Integer placeID = extractRecordPlaceID(container);
							if(placeID != null)
								placeDialog.selectData(placeID);

							placeDialog.showDialog();
						}


						//from: repository, source, citation, assertion, historic date, calendar, person, person name, group, event,
						// cultural norm, media, place
						case NOTE -> {
							final NoteDialog noteDialog = NoteDialog.create(store, parent)
								.withReference(tableName, recordID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", tableName);
										record.put("reference_id", recordID);
									}
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}


						//from: citation
						case LOCALIZED_EXTRACT -> {
							final LocalizedTextDialog localizedTextDialog = LocalizedTextDialog.createSimpleText(store, parent)
								.withReference("citation", recordID, EntityManager.LOCALIZED_TEXT_TYPE_EXTRACT)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", tableName);
										record.put("reference_id", recordID);
									}
								});
							localizedTextDialog.loadData();

							localizedTextDialog.showDialog();
						}

						//from: person name
						case LOCALIZED_PERSON_NAME -> {
							final LocalizedPersonNameDialog localizedTextDialog = LocalizedPersonNameDialog.create(store, parent)
								.withReference(recordID)
								.withOnCloseGracefully(record -> {
									if(record != null)
										insertRecordPersonNameID(record, recordID);
								});
							localizedTextDialog.loadData();

							localizedTextDialog.showDialog();
						}

						//from: place
						case LOCALIZED_PLACE_NAME -> {
							final LocalizedTextDialog localizedTextDialog = LocalizedTextDialog.createSimpleText(store, parent)
								.withReference(tableName, recordID, EntityManager.LOCALIZED_TEXT_TYPE_NAME)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", tableName);
										record.put("reference_id", recordID);
									}
								});
							localizedTextDialog.loadData();

							localizedTextDialog.showDialog();
						}


						//from: repository, source, citation, assertion, person, person name, group, event, cultural norm, note, place
						case MEDIA -> {
							final MediaDialog mediaDialog = MediaDialog.createForMedia(store, parent)
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(tableName, recordID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", tableName);
										record.put("reference_id", recordID);
									}
								});
							mediaDialog.loadData();
							final Integer mediaID = extractRecordMediaID(container);
							if(mediaID != null)
								mediaDialog.selectData(mediaID);

							mediaDialog.showDialog();
						}

						//from: person, group, place
						case PHOTO -> {
							final MediaDialog photoDialog = MediaDialog.createForPhoto(store, parent)
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(tableName, recordID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", tableName);
										record.put("reference_id", recordID);
									}
								});
							photoDialog.loadData();
							final Integer photoID = extractRecordPhotoID(container);
							if(photoID != null){
								//add photo manually because is not retrievable through a junction
								photoDialog.addData(container);
								photoDialog.selectData(recordID);
							}
							else
								photoDialog.showNewRecord();

							photoDialog.showDialog();
						}

						//from: person, group, media, place
						case PHOTO_CROP -> {
							final PhotoCropDialog photoCropDialog = PhotoCropDialog.create(store, parent);
							photoCropDialog.withOnCloseGracefully(record -> {
								final Rectangle crop = photoCropDialog.getCrop();
								if(crop != null){
									final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
									sj.add(Integer.toString(crop.x))
										.add(Integer.toString(crop.y))
										.add(Integer.toString(crop.width))
										.add(Integer.toString(crop.height));
									insertRecordPhotoCrop(container, sj.toString());
								}
							});
							try{
								if(recordID != null){
									final String photoCrop = extractRecordPhotoCrop(container);
									photoCropDialog.loadData(recordID, photoCrop);
								}

								photoCropDialog.setSize(420, 295);
								photoCropDialog.showDialog();
							}
							catch(final IOException ignored){}
						}


						//from: repository
						case PERSON -> {
							final PersonDialog personDialog = PersonDialog.create(store, parent)
								.withOnCloseGracefully(record -> insertRecordPersonID(container, extractRecordID(record)));
							personDialog.loadData();
							final Integer personID = extractRecordPersonID(container);
							if(personID != null)
								personDialog.selectData(personID);

							personDialog.showDialog();
						}

						//from: person
						case PERSON_NAME -> {
							final PersonNameDialog personNameDialog = PersonNameDialog.create(store, parent)
								.withReference(recordID)
								.withOnCloseGracefully(record -> {
									insertRecordPersonID(record, recordID);

									//update table identifier
									dialog.loadData();
								});
							personNameDialog.loadData();

							personNameDialog.showDialog();
						}


						//from: person, group, place
						case GROUP -> {
							final GroupDialog groupDialog = GroupDialog.create(store, parent)
								.withReference(tableName, recordID);
							groupDialog.loadData();
							final Integer groupID = extractRecordGroupID(container);
							if(groupID != null)
								groupDialog.selectData(groupID);

							groupDialog.showDialog();
						}


						//from: calendar, person, person name, group, cultural norm, media, place
						case EVENT -> {
							final EventDialog eventDialog = EventDialog.create(store, parent)
								.withReference(tableName, recordID);
							eventDialog.loadData();

							eventDialog.showDialog();
						}


						//from: assertion, person name, group, note
						case CULTURAL_NORM -> {
							final CulturalNormDialog culturalNormDialog = CulturalNormDialog.create(store, parent)
								.withReference("person_name", recordID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", tableName);
										record.put("reference_id", recordID);
									}
								});
							culturalNormDialog.loadData();
							final Integer culturalNormID = extractRecordCulturalNormID(container);
							if(culturalNormID != null)
								culturalNormDialog.selectData(culturalNormID);

							culturalNormDialog.showDialog();
						}


//						case RESEARCH_STATUS -> {
							//TODO
//						}


						//from: ?
//						case PROJECT -> {
							//TODO
//						}
					}
				}
			};
			EventBusService.subscribe(listener);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.out.println(store);
					System.exit(0);
				}
			});
			dialog.showDialog();
		});
	}

}
