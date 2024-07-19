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
			dialog.initComponents();
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
//							repositoryDialog.initComponents();
//							repositoryDialog.loadData();
//							final Integer repositoryID = extractRecordRepositoryID(container);
//							if(repositoryID != null)
//								repositoryDialog.selectData(repositoryID);
//
//							repositoryDialog.setSize(400, 395);
//							repositoryDialog.setLocationRelativeTo(null);
//							repositoryDialog.setVisible(true);
//						}

						//from: repository
						case SOURCE -> {
							final SourceDialog sourceDialog = SourceDialog.create(store, parent)
								.withFilterOnRepositoryID(recordID)
								.withOnCloseGracefully(record -> {
									if(record != null)
										record.put("repository_id", recordID);
								});
							sourceDialog.initComponents();
							sourceDialog.loadData();
							final Integer sourceID = extractRecordSourceID(container);
							if(sourceID != null)
								sourceDialog.selectData(sourceID);

							sourceDialog.setSize(440, 462);
							sourceDialog.setLocationRelativeTo(null);
							sourceDialog.setVisible(true);
						}

						//from: source
						case CITATION -> {
							final CitationDialog citationDialog = CitationDialog.create(store, parent)
								.withFilterOnSourceID(recordID)
								.withOnCloseGracefully(record -> {
									if(record != null)
										record.put("source_id", recordID);
								});
							citationDialog.initComponents();
							citationDialog.loadData();
							final Integer citationID = extractRecordCitationID(container);
							if(citationID != null)
								citationDialog.selectData(citationID);

							citationDialog.setSize(420, 586);
							citationDialog.setLocationRelativeTo(dialog);
							citationDialog.setVisible(true);
						}

						//from: citation, person, person name, group, media, place, cultural norm, historic date, calendar
						case ASSERTION -> {
							final AssertionDialog assertionDialog = AssertionDialog.create(store, parent)
								.withReference(tableName, recordID);
							assertionDialog.initComponents();
							assertionDialog.loadData();

							assertionDialog.setSize(488, 386);
							assertionDialog.setLocationRelativeTo(dialog);
							assertionDialog.setVisible(true);
						}


						//from: source, event, cultural norm, media
						case HISTORIC_DATE -> {
							final HistoricDateDialog historicDateDialog = HistoricDateDialog.create(store, parent);
							historicDateDialog.initComponents();
							historicDateDialog.loadData();
							final Integer dateID = extractRecordDateID(container);
							if(dateID != null)
								historicDateDialog.selectData(dateID);

							historicDateDialog.setSize(481, 427);
							historicDateDialog.setLocationRelativeTo(null);
							historicDateDialog.setVisible(true);
						}

						//from: historic date
						case CALENDAR -> {
							final CalendarDialog calendarDialog = CalendarDialog.create(store, parent)
								.withOnCloseGracefully(record -> container.put("calendar_id", extractRecordID(record)));
							calendarDialog.initComponents();
							calendarDialog.loadData();
							final Integer calendarID = extractRecordCalendarID(container);
							if(calendarID != null)
								calendarDialog.selectData(calendarID);

							calendarDialog.setSize(309, 377);
							calendarDialog.setLocationRelativeTo(dialog);
							calendarDialog.setVisible(true);
						}

						//from: historic date
						case CALENDAR_ORIGINAL -> {
							final CalendarDialog calendarDialog = CalendarDialog.create(store, parent)
								.withOnCloseGracefully(record -> container.put("calendar_original_id", extractRecordID(record)));
							calendarDialog.initComponents();
							calendarDialog.loadData();
							final Integer calendarID = extractRecordCalendarOriginalID(container);
							if(calendarID != null)
								calendarDialog.selectData(calendarID);

							calendarDialog.setSize(309, 377);
							calendarDialog.setLocationRelativeTo(dialog);
							calendarDialog.setVisible(true);
						}


						//from: repository, source, event, cultural norm
						case PLACE -> {
							final PlaceDialog placeDialog = PlaceDialog.create(store, parent)
								.withOnCloseGracefully(record -> container.put("place_id", extractRecordID(record)));
							placeDialog.initComponents();
							placeDialog.loadData();
							final Integer placeID = extractRecordPlaceID(container);
							if(placeID != null)
								placeDialog.selectData(placeID);

							placeDialog.setSize(522, 618);
							placeDialog.setLocationRelativeTo(null);
							placeDialog.setVisible(true);
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
							noteDialog.initComponents();
							noteDialog.loadData();

							noteDialog.setSize(420, 474);
							noteDialog.setLocationRelativeTo(dialog);
							noteDialog.setVisible(true);
						}


						//from: citation
						case LOCALIZED_EXTRACT -> {
							final LocalizedTextDialog localizedTextDialog = LocalizedTextDialog.createSimpleText(store, parent)
								.withReference("citation", recordID, "extract")
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", tableName);
										record.put("reference_id", recordID);
									}
								});
							localizedTextDialog.initComponents();
							localizedTextDialog.loadData();

							localizedTextDialog.setSize(420, 453);
							localizedTextDialog.setLocationRelativeTo(dialog);
							localizedTextDialog.setVisible(true);
						}

						//from: person name
						case LOCALIZED_PERSON_NAME -> {
							final LocalizedTextDialog localizedTextDialog = LocalizedTextDialog.createSimpleTextWithSecondary(store, parent)
								.withReference(tableName, recordID, "name")
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", tableName);
										record.put("reference_id", recordID);
									}
								});
							localizedTextDialog.initComponents();
							localizedTextDialog.loadData();

							localizedTextDialog.setSize(420, 480);
							localizedTextDialog.setLocationRelativeTo(dialog);
							localizedTextDialog.setVisible(true);
						}

						//from: place
						case LOCALIZED_PLACE_NAME -> {
							final LocalizedTextDialog localizedTextDialog = LocalizedTextDialog.createSimpleText(store, parent)
								.withReference(tableName, recordID, "name")
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", tableName);
										record.put("reference_id", recordID);
									}
								});
							localizedTextDialog.initComponents();
							localizedTextDialog.loadData();

							localizedTextDialog.setSize(420, 453);
							localizedTextDialog.setLocationRelativeTo(dialog);
							localizedTextDialog.setVisible(true);
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
							mediaDialog.initComponents();
							mediaDialog.loadData();
							final Integer mediaID = extractRecordMediaID(container);
							if(mediaID != null)
								mediaDialog.selectData(mediaID);

							mediaDialog.setSize(420, 497);
							mediaDialog.setLocationRelativeTo(dialog);
							mediaDialog.setVisible(true);
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
							photoDialog.initComponents();
							photoDialog.loadData();
							final Integer photoID = extractRecordPhotoID(container);
							if(photoID != null){
								//add photo manually because is not retrievable through a junction
								photoDialog.addData(container);
								photoDialog.selectData(recordID);
							}
							else
								photoDialog.showNewRecord();

							photoDialog.setSize(420, 292);
							photoDialog.setLocationRelativeTo(dialog);
							photoDialog.setVisible(true);
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
									container.put("photo_crop", sj);
								}
							});
							try{
								if(recordID != null){
									final String photoCrop = extractRecordPhotoCrop(container);
									photoCropDialog.loadData(recordID, photoCrop);
								}

								photoCropDialog.setSize(420, 295);
								photoCropDialog.setLocationRelativeTo(dialog);
								photoCropDialog.setVisible(true);
							}
							catch(final IOException ignored){}
						}


						//from: repository
						case PERSON -> {
							final PersonDialog personDialog = PersonDialog.create(store, parent)
								.withOnCloseGracefully(record -> container.put("person_id", extractRecordID(record)));
							personDialog.initComponents();
							personDialog.loadData();
							final Integer personID = extractRecordPersonID(container);
							if(personID != null)
								personDialog.selectData(personID);

							personDialog.setSize(355, 469);
							personDialog.setLocationRelativeTo(null);
							personDialog.setVisible(true);
						}

						//from: person
						case PERSON_NAME -> {
							final PersonNameDialog personNameDialog = PersonNameDialog.create(store, parent)
								.withReference(recordID)
								.withOnCloseGracefully(record -> {
									record.put("person_id", recordID);

									//update table identifier
									dialog.loadData();
								});
							personNameDialog.initComponents();
							personNameDialog.loadData();

							personNameDialog.setSize(535, 469);
							personNameDialog.setLocationRelativeTo(null);
							personNameDialog.setVisible(true);
						}


						//from: person, group, place
						case GROUP -> {
							final GroupDialog groupDialog = GroupDialog.create(store, parent)
								.withReference(tableName, recordID);
							groupDialog.initComponents();
							groupDialog.loadData();
							final Integer groupID = extractRecordGroupID(container);
							if(groupID != null)
								groupDialog.selectData(groupID);

							groupDialog.setSize(468, 469);
							groupDialog.setLocationRelativeTo(null);
							groupDialog.setVisible(true);
						}


						//from: calendar, person, person name, group, cultural norm, media, place
						case EVENT -> {
							final EventDialog eventDialog = EventDialog.create(store, parent)
								.withReference(tableName, recordID);
							eventDialog.initComponents();
							eventDialog.loadData();

							eventDialog.setSize(309, 409);
							eventDialog.setLocationRelativeTo(null);
							eventDialog.setVisible(true);
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
							culturalNormDialog.initComponents();
							culturalNormDialog.loadData();
							final Integer culturalNormID = extractRecordCulturalNormID(container);
							if(culturalNormID != null)
								culturalNormDialog.selectData(culturalNormID);

							culturalNormDialog.setSize(474, 652);
							culturalNormDialog.setLocationRelativeTo(dialog);
							culturalNormDialog.setVisible(true);
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
//			dialog.setSize(400, 395);
//			dialog.setSize(481, 427);
//			dialog.setSize(522, 618);
//			dialog.setSize(420, 497);
//			dialog.setSize(355, 469);
//			dialog.setSize(468, 469);
//			dialog.setSize(309, 409);
//			dialog.setSize(474, 652);
			dialog.setSize(420, 567);
//			dialog.setSize(420, 282);
			dialog.setLocationRelativeTo(null);
			dialog.addComponentListener(new java.awt.event.ComponentAdapter() {
				@Override
				public void componentResized(final java.awt.event.ComponentEvent e) {
					System.out.println("Resized to " + e.getComponent().getSize());
				}
			});
			dialog.setVisible(true);
		});
	}


	protected static Integer extractRecordID(final Map<String, Object> record){
		return (record != null? (Integer)record.get("id"): null);
	}

	private static Integer extractRecordPersonID(final Map<String, Object> record){
		return (record != null? (Integer)record.get("person_id"): null);
	}

	private static Integer extractRecordPlaceID(final Map<String, Object> record){
		return (Integer)record.get("place_id");
	}

	private static Integer extractRecordSourceID(final Map<String, Object> record){
		return (Integer)record.get("source_id");
	}

	private static Integer extractRecordCitationID(final Map<String, Object> record){
		return (Integer)record.get("citation_id");
	}

	private static Integer extractRecordDateID(final Map<String, Object> record){
		return (Integer)record.get("date_id");
	}

	private static Integer extractRecordCalendarID(final Map<String, Object> record){
		return (Integer)record.get("calendar_id");
	}

	private static Integer extractRecordMediaID(final Map<String, Object> record){
		return (Integer)record.get("media_id");
	}

	private static Integer extractRecordPhotoID(final Map<String, Object> record){
		return (Integer)record.get("photo_id");
	}

	private static Integer extractRecordGroupID(final Map<String, Object> record){
		return (Integer)record.get("group_id");
	}

	private static Integer extractRecordCulturalNormID(final Map<String, Object> record){
		return (Integer)record.get("cultural_norm_id");
	}

	private static Integer extractRecordCalendarOriginalID(final Map<String, Object> record){
		return (Integer)record.get("calendar_original_id");
	}

	private static String extractRecordPhotoCrop(final Map<String, Object> record){
		return (String)record.get("photo_crop");
	}

}
