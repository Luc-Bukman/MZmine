package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.fattyacyls;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;

public class AdvancedFattyAcylAnnotationParameters extends SimpleParameterSet {

  public static final MultiChoiceParameter<IonizationType> IONS_TO_IGNORE = new MultiChoiceParameter<IonizationType>(
      "Ions to ignore",
      "List of ions that will be ignored if checked. Reduces false positives by removing "
          + "ion notations incompatible with utilized LC buffers (E.g. formate vs acetate)",
      IonizationType.values());

  public AdvancedFattyAcylAnnotationParameters() {
    super(IONS_TO_IGNORE);
  }

}