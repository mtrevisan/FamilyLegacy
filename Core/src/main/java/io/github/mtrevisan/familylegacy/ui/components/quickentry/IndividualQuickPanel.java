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
package io.github.mtrevisan.familylegacy.ui.components.quickentry;

import io.github.mtrevisan.familylegacy.dossier.EditorTab;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.quickentry.FullNameParser;
import io.github.mtrevisan.familylegacy.quickentry.LifeFactUpdater;
import io.github.mtrevisan.familylegacy.quickentry.ParsedName;
import io.github.mtrevisan.familylegacy.quickentry.QuickEntryService;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.Window;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.function.Consumer;


/**
 * The quick entry of an individual: the sex, the birth and the death, and for a new individual the full name in one line too. It is a way
 * to fill the editor of the individual with the most common facts, without knowing which records they are made of; the editor keeps all
 * the rest.
 * <p>
 * The name is split into the parts of a name (see {@link FullNameParser}) and handed to the list of the personal names of the editor,
 * where it is shown and can be corrected with the full editor of the name: the parser only proposes. The sex, the birth and the death
 * are read when the editor is confirmed. For an individual that exists ({@link #load}) they show what the file says, and what the user
 * changes is the only thing that is written back (see {@link LifeFactUpdater}).
 */
public final class IndividualQuickPanel extends JPanel{

	private static final String PARTS_SEPARATOR = " · ";


	private final Consumer<FLEFRecord> nameSink;

	private final JTextField fullNameField = new JTextField(28);
	private final JLabel namePreview = new JLabel(StringUtils.SPACE);
	private final JButton addButton = new JButton(I18N.t("dialog.quick_entry.full_name.add"));
	private final JComboBox<SexType> sexCombo = new JComboBox<>(new SexType[]{SexType.UNKNOWN, SexType.MALE, SexType.FEMALE});
	private final LifeFactRow birthPanel;
	private final LifeFactRow deathPanel;
	private final LifeFactRow burialPanel;
	/** The occupation (a text): the one of the individual, if it has one only. */
	private final JTextField occupationField = new JTextField(28);
	/** The notes that lead to the Attributes tab, when the sex or the occupation cannot be edited here. */
	private final TabLink sexNote = new TabLink(EditorTab.ATTRIBUTES);
	private final TabLink occupationNote = new TabLink(EditorTab.ATTRIBUTES);
	private String loadedOccupation = StringUtils.EMPTY;
	/** The sex the individual had when the editor was opened (unknown for a new individual). */
	private SexType loadedSex = SexType.UNKNOWN;


	/**
	 * @param owner	The window that owns the dialogs that the panel opens.
	 * @param model	The model.
	 * @param nameSink	Takes the {@code name} structure made of what is typed as the full name (the editor adds it to its names);
	 * 	{@code null} if the individual exists already: there is no full name then.
	 */
	public IndividualQuickPanel(final Window owner, final FLEFModel model, final Consumer<FLEFRecord> nameSink){
		this.nameSink = nameSink;
		birthPanel = new LifeFactRow(owner, model, "dialog.quick_entry.birth_date");
		deathPanel = new LifeFactRow(owner, model, "dialog.quick_entry.death_date");
		burialPanel = new LifeFactRow(owner, model, "dialog.quick_entry.burial_date");
		occupationField.setToolTipText(I18N.t("dialog.quick_entry.occupation.tooltip"));

		fullNameField.setToolTipText(I18N.t("dialog.quick_entry.full_name.tooltip"));
		fullNameField.addActionListener(e -> addName());
		LifeFactRow.listen(fullNameField, this::updateNamePreview);
		addButton.addActionListener(e -> addName());
		//the line that tells how the name is split takes room only while there is something to say
		namePreview.setVisible(false);
		//there is nothing to add until something is typed
		addButton.setEnabled(false);

		sexCombo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public java.awt.Component getListCellRendererComponent(final javax.swing.JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus){
				return super.getListCellRendererComponent(list, I18N.t("enum.individual.sex." + ((SexType)value).name().toLowerCase(Locale.ROOT)),
					index, isSelected, cellHasFocus);
			}
		});

		setBorder(new TitledBorder(I18N.t(nameSink != null? "dialog.quick_entry.heading": "dialog.quick_entry.heading.facts")));
		//one grid for everything, so that the labels and the fields of every row are aligned
		setLayout(new MigLayout("ins 5,hidemode 3,fillx,gapy 5", "[right]rel[]rel[right]rel[grow,fill]rel[]"));
		if(nameSink != null){
			add(new JLabel(I18N.t("dialog.quick_entry.full_name")));
			add(fullNameField, "span 3,growx");
			add(addButton, "wrap");
			add(namePreview, "skip 1,span 4,wrap");
		}
		add(new JLabel(I18N.t("dialog.individual.sex")));
		add(sexCombo, "growx 0,wrap");
		add(sexNote, "skip 1,span 4,wrap");
		birthPanel.addTo(this);
		deathPanel.addTo(this);
		burialPanel.addTo(this);
		add(new JLabel(I18N.t("dialog.quick_entry.occupation")));
		add(occupationField, "span 3,growx,wrap");
		add(occupationNote, "skip 1,span 4,wrap");
	}


	/** Makes the notes that point to another tab ("see the Participations tab") lead there when clicked. */
	public void setNavigator(final TabLink.Navigator navigator){
		birthPanel.setNavigator(navigator);
		deathPanel.setNavigator(navigator);
		burialPanel.setNavigator(navigator);
		sexNote.setNavigator(navigator);
		occupationNote.setNavigator(navigator);
	}


	/** @return	The sex chosen ({@link SexType#UNKNOWN} if none). */
	public SexType sex(){
		return (SexType)sexCombo.getSelectedItem();
	}

	/**
	 * Sets the sex, and whether the user may change it (it may not when the sex is imposed by the link that is being created: the father of
	 * a person is male).
	 *
	 * @param sex	The sex ({@code null}: unknown).
	 * @param locked	Whether the sex is fixed.
	 */
	public void setSex(final SexType sex, final boolean locked){
		sexCombo.setSelectedItem(sex != null? sex: SexType.UNKNOWN);
		sexCombo.setEnabled(!locked);
		sexCombo.setToolTipText(locked? I18N.t("dialog.quick_entry.sex.locked"): null);
	}

	/**
	 * Shows what the file says of an individual that exists: its sex, and its birth and its death (the first one if it has more than one).
	 *
	 * @param model	The model.
	 * @param individual	The individual.
	 */
	public void load(final FLEFModel model, final FLEFRecord individual){
		final List<FLEFRecord> assertions = PreferredSex.assertions(individual.getId(), model);
		//with a single attribute the sex is what it says; with many, which one counts is for the user to say in the Attributes tab
		loadedSex = (assertions.size() == 1
			? SexType.fromRaw(FLEFRecordHelper.getChildValue(assertions.getFirst(), IndividualAttributeReader.TAG_VALUE))
			: PreferredSex.of(individual, model));
		sexCombo.setSelectedItem(loadedSex);
		final boolean several = (assertions.size() > 1);
		sexCombo.setEnabled(!several);
		sexCombo.setToolTipText(several? I18N.t("dialog.quick_entry.sex.several"): null);
		if(several)
			sexNote.show(I18N.t("dialog.quick_entry.sex.several"), assertions.getFirst());

		birthPanel.load(LifeFactUpdater.principalEvents(model, individual.getId(), EventReader.ENUM_TYPE_BIRTH), individual.getId());
		deathPanel.load(LifeFactUpdater.principalEvents(model, individual.getId(), EventReader.ENUM_TYPE_DEATH), individual.getId());
		burialPanel.load(LifeFactUpdater.principalEvents(model, individual.getId(), EventReader.ENUM_TYPE_BURIAL), individual.getId());

		final List<FLEFRecord> occupations = LifeFactUpdater.occupations(model, individual.getId());
		if(!occupations.isEmpty()){
			loadedOccupation = StringUtils.normalizeSpace(FLEFRecordHelper.getChildValue(occupations.getFirst(), IndividualAttributeReader.TAG_VALUE));
			occupationField.setText(loadedOccupation);
			//a person with several occupations, or one with details of its own (a source, dates, a place), keeps them in the Attributes tab
			if(occupations.size() > 1){
				occupationField.setEnabled(false);
				occupationField.setToolTipText(I18N.t("dialog.quick_entry.occupation.several"));
				occupationNote.show(I18N.t("dialog.quick_entry.occupation.several"), occupations.getFirst());
			}
			else if(!LifeFactUpdater.isOccupationEditable(occupations.getFirst())){
				occupationField.setEnabled(false);
				occupationField.setToolTipText(I18N.t("dialog.quick_entry.occupation.locked"));
				occupationNote.show(I18N.t("dialog.quick_entry.occupation.locked.note"), occupations.getFirst());
			}
		}
	}

	/** @return	Whether the user changed the sex of an individual that exists. */
	public boolean sexChanged(){
		return (sex() != loadedSex);
	}

	/** @return	The panel of the birth. */
	public LifeFactRow birthPanel(){
		return birthPanel;
	}

	/** @return	The panel of the death. */
	public LifeFactRow deathPanel(){
		return deathPanel;
	}

	/** @return	The panel of the burial. */
	public LifeFactRow burialPanel(){
		return burialPanel;
	}

	/** @return	The occupation typed (blank spaces normalized, empty if none). */
	public String occupation(){
		return StringUtils.normalizeSpace(occupationField.getText());
	}

	/** @return	Whether the user changed the occupation of an individual that exists. */
	public boolean occupationChanged(){
		return !occupation().equals(loadedOccupation);
	}

	/** Adds what is typed as the full name to the names of the editor (nothing if there is none), as {@code Enter} does. */
	public void flushName(){
		addName();
	}

	/**
	 * Checks the dates and settles the places of the birth and the death (see {@link LifeFactRow#prepare()}).
	 *
	 * @return	Whether the editor can be saved.
	 */
	public boolean prepare(){
		return (birthPanel.prepare() && deathPanel.prepare() && burialPanel.prepare());
	}

	/** @return	The birth, as {@link #prepare()} settled it. */
	public QuickEntryService.LifeFact birth(){
		return birthPanel.fact();
	}

	/** @return	The death, as {@link #prepare()} settled it. */
	public QuickEntryService.LifeFact death(){
		return deathPanel.fact();
	}

	/** @return	The burial, as {@link #prepare()} settled it. */
	public QuickEntryService.LifeFact burial(){
		return burialPanel.fact();
	}


	private void addName(){
		final ParsedName name = FullNameParser.parse(fullNameField.getText());
		if(name.isEmpty())
			return;

		nameSink.accept(name.toRecord());
		fullNameField.setText(StringUtils.EMPTY);
	}

	/** Tells, under the field, how the name will be split. */
	private void updateNamePreview(){
		final ParsedName name = FullNameParser.parse(fullNameField.getText());
		addButton.setEnabled(!name.isEmpty());
		final boolean visible = namePreview.isVisible();
		namePreview.setVisible(!name.isEmpty());
		if(visible == name.isEmpty()){
			revalidate();
			repaint();
		}
		if(name.isEmpty()){
			namePreview.setText(StringUtils.SPACE);
			return;
		}

		final StringJoiner parts = new StringJoiner(PARTS_SEPARATOR);
		for(final ParsedName.Part part : name.parts())
			parts.add(I18N.t("enum.part.type." + part.type()) + StringUtils.SPACE + part.value());
		namePreview.setText(I18N.tf("dialog.quick_entry.full_name.preview", parts.toString()));
		namePreview.setForeground(UIManager.getColor("Label.disabledForeground"));
	}

}
