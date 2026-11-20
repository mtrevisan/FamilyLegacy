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
package io.github.mtrevisan.familylegacy.ui.dialogs.structures;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentifierReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentifierHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Dialog for editing an {@code IDENTIFIER} structure according to FLEF 0.2.0: how a source is identified (an ISBN, a DOI, a microfilm
 * number), or where it is found in a repository (a shelf mark, a call number, a classification number).
 * <p>
 * Structure:
 * <pre>
 * struct IdentifierStructure {
 *   scheme: enum { isbn, issn, doi, url, call_number, dewey, lcc, udc, colon, garside, fhl_film, fhl_digital_folder, fhl_ancestral_file, index_card } | Text
 *   value: Text
 *
 *   require not_blank(value)
 * }
 * </pre>
 * The schemes that suit the place the identifier is for come first in the list: the ones of a repository when the dialog is opened from a
 * repository citation, the ones of the source itself otherwise. A value that fails the check of its scheme (the check digit of an ISBN or
 * an ISSN, the syntax of a URL) is shown as a warning, and does not prevent saving.
 * <p>
 * Tabs:
 * Tab 1 (Properties): scheme, value
 */
public class IdentifierStructureDialog extends BaseRecordDialog{

	private final JPanel propertiesPanel;

	private final BoundComboBox<String> schemeCombo;
	private final BoundTextField valueField;
	private final JLabel warningLabel;

	public static IdentifierStructureDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, IdentifierStructureDialog::new);
	}

	public static IdentifierStructureDialog createEdit(final Window parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, IdentifierStructureDialog::new);
	}

	private IdentifierStructureDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, IdentifierHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]5[]");

		schemeCombo = new BoundComboBox<>(IdentifierReader.TAG_SCHEME, GUIHelper.fillCombo(schemesFor(parent), null));
		schemeCombo.setI18NPrefix("enum.identifier.scheme");
		schemeCombo.setEditable(true);
		valueField = new BoundTextField(IdentifierReader.TAG_VALUE);
		valueField.setToolTipText(I18N.t("tooltip.identifier.value"));
		warningLabel = new JLabel(StringUtils.SPACE);
		final Color warningColor = UIManager.getColor("Component.warning.focusedBorderColor");
		warningLabel.setForeground(warningColor != null? warningColor: new Color(0xB26A00));

		components = new RecordDialogBuilder(this, model, this.record)
			.build();

		components.bind(schemeCombo);
		components.bind(valueField);

		//a value tells its own scheme (a DOI, an ISBN, a URL): set it when the field is left, if the user has not chosen one
		valueField.addFocusListener(new FocusAdapter(){
			@Override
			public void focusLost(final FocusEvent e){
				inferScheme();
			}
		});
		final DocumentListener refresh = new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				refreshWarning();
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				refreshWarning();
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				refreshWarning();
			}
		};
		valueField.getDocument()
			.addDocumentListener(refresh);
		schemeCombo.addItemListener(e -> refreshWarning());

		finalizeDialog(parent);
	}

	/** @return	The schemes, the ones that suit the place the identifier is for first (a repository citation, or a source). */
	static String[] schemesFor(final Window parent){
		final boolean forRepository = (parent instanceof RootPaneContainer container
			&& IdentifierHandler.SCOPE_REPOSITORY.equals(container.getRootPane().getClientProperty(IdentifierHandler.SCOPE_PROPERTY)));
		final Set<String> schemes = new LinkedHashSet<>(Arrays.asList(forRepository
			? IdentifierReader.REPOSITORY_SCHEMES
			: IdentifierReader.SOURCE_SCHEMES));
		schemes.addAll(Arrays.asList(IdentifierReader.SCHEMES));
		return schemes.toArray(String[]::new);
	}

	@Override
	protected JPanel createPropertiesPanel(){
		// scheme
		GUIHelper.addLabeledComponent(propertiesPanel, GUIHelper.requiredLabel("dialog.identifier.scheme"), schemeCombo);

		// value
		GUIHelper.addLabeledComponent(propertiesPanel, GUIHelper.requiredLabel("dialog.identifier.value"), valueField);

		// warning about the value
		GUIHelper.addComponent(propertiesPanel, warningLabel);

		return propertiesPanel;
	}

	@Override
	protected void loadData(){
		components.load(record);

		refreshWarning();
	}

	@Override
	protected boolean validData(){
		inferScheme();

		if(StringUtils.isBlank(valueField.getText())){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.identifier.value")),
				tabbedPane, propertiesPanel, valueField);

			return false;
		}
		if(StringUtils.isBlank(schemeCombo.getText())){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.identifier.scheme")),
				tabbedPane, propertiesPanel, schemeCombo);

			return false;
		}

		return true;
	}

	/** Sets the scheme from the value, if there is none yet and the value is recognized (an ISBN, an ISSN, a DOI, a URL). */
	private void inferScheme(){
		if(StringUtils.isNotBlank(schemeCombo.getText()))
			return;

		final String scheme = IdentifierReader.inferScheme(valueField.getText());
		if(scheme != null)
			schemeCombo.setText(scheme);
	}

	/** Tells, without preventing anything, that the value fails the check of its scheme. */
	private void refreshWarning(){
		final IdentifierReader.Validity validity = IdentifierReader.validity(schemeCombo.getText(), valueField.getText());
		if(validity != IdentifierReader.Validity.INVALID){
			warningLabel.setText(StringUtils.SPACE);

			return;
		}

		final boolean url = IdentifierReader.ENUM_SCHEME_URL.equalsIgnoreCase(StringUtils.trim(schemeCombo.getText()));
		warningLabel.setText(I18N.t(url? "text.identifier.invalid.url": "text.identifier.invalid.check_digit"));
	}

	@Override
	protected void saveData(){
		inferScheme();
		components.save(record);
	}

	static void main(final String[] args){
		GUIHelper.launch(IdentifierStructureDialog::createNew);
	}

}
