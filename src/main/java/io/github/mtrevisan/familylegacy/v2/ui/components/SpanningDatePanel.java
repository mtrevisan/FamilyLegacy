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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Panel for {@code SPANNING} date (duration) according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * struct SpanningDate {
 *   from?: QualifiedDate
 *   to?: QualifiedDate
 *   require one_of(from, to)
 * }
 * </pre>
 */
public class SpanningDatePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 6538587318116402553L;


	private static final String TAG_FROM = "FROM";
	private static final String TAG_TO = "TO";
	private static final String TAG_SPANNING = "SPANNING";

	private final QualifiedDatePanel fromPanel;
	private final QualifiedDatePanel toPanel;


	public SpanningDatePanel(final Dialog parent, final FLEFModel model){
		this.fromPanel = new QualifiedDatePanel(parent, model);
		this.toPanel = new QualifiedDatePanel(parent, model);


		initComponents();
	}


	private void initComponents(){
		setLayout(new MigLayout("ins 0,fillx,top", "[grow,fill][grow,fill]"));
		setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		final JPanel fromPanel = new JPanel(new MigLayout("fillx", "[right]rel[grow]"));
		fromPanel.setBorder(new TitledBorder("From"));
		fromPanel.add(this.fromPanel, "growx");
		add(fromPanel, "growx");

		final JPanel toPanel = new JPanel(new MigLayout("fillx", "[right]rel[grow]"));
		toPanel.setBorder(new TitledBorder("To"));
		toPanel.add(this.toPanel, "growx");
		add(toPanel, "growx");
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final FLEFRecord from = FLEFRecordHelper.findChild(record, TAG_FROM);
		if(from != null)
			fromPanel.load(from);

		final FLEFRecord to = FLEFRecordHelper.findChild(record, TAG_TO);
		if(to != null)
			toPanel.load(to);
	}

	public FLEFRecord saveToRecord(){
		final FLEFRecord record = FLEFRecord.createEmpty();

		if(fromPanel.hasData()){
			final FLEFRecord from = fromPanel.save();
			record.addChildWithTag(TAG_FROM, from);
		}

		if(toPanel.hasData()){
			final FLEFRecord to = toPanel.save();
			record.addChildWithTag(TAG_TO, to);
		}

		return (record.hasData()? record.setTag(TAG_SPANNING): FLEFRecord.createEmpty());
	}

	public void clear(){
		fromPanel.clear();
		toPanel.clear();
	}

	public boolean hasData(){
		return (fromPanel.hasData() || toPanel.hasData());
	}

	public boolean validateData(){
		if(!hasData()){
			JOptionPane.showMessageDialog(this,
				"At least one of From or To is required for SPANNING date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		if(fromPanel.hasData() && !fromPanel.validateData())
			return false;

		return (!toPanel.hasData() || toPanel.validateData());
	}

}
