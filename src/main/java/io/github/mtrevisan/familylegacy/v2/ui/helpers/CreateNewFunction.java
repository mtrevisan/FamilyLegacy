package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;

import javax.swing.JDialog;
import java.awt.Dialog;


@FunctionalInterface
public interface CreateNewFunction{

	JDialog apply(Dialog dialog, FLEFModel model);

}
