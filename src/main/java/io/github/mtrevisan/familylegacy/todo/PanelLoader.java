package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import javax.swing.JPanel;


@FunctionalInterface
interface PanelLoader{

	void load(JPanel panel, FLEFRecord record);

}
