package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import javax.swing.JPanel;


@FunctionalInterface
interface PanelSaver{

	void save(JPanel panel, FLEFRecord record);

}
