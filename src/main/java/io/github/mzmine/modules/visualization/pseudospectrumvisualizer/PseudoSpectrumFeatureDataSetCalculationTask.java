/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.pseudospectrumvisualizer;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_extract_mz_ranges.ExtractMzRangesIonSeriesFunction;
import io.github.mzmine.modules.visualization.chromatogram.FeatureDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.collections.BinarySearch;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

class PseudoSpectrumFeatureDataSetCalculationTask extends AbstractTask {

  private final RawDataFile rawDataFile;
  private final TICPlot chromPlot;
  private final Scan pseudoScan;
  private final ModularFeature feature;
  private final MZTolerance mzTolerance;
  private ExtractMzRangesIonSeriesFunction extractFunction;

  PseudoSpectrumFeatureDataSetCalculationTask(RawDataFile rawDataFile, TICPlot chromPlot,
      Scan pseudoScan, ModularFeature feature, MZTolerance mzTolerance) {
    super(null, Instant.now());
    this.rawDataFile = rawDataFile;
    this.chromPlot = chromPlot;
    this.pseudoScan = pseudoScan;
    this.feature = feature;
    this.mzTolerance = mzTolerance;
  }

  @Override
  public String getTaskDescription() {
    return "Calculate feature datasets";
  }

  @Override
  public double getFinishedPercentage() {
    return extractFunction == null ? 0 : extractFunction.getFinishedPercentage();
  }


  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    ModularFeatureList newFeatureList = new ModularFeatureList("Feature list " + this.hashCode(),
        null, rawDataFile);

    if (getStatus() == TaskStatus.CANCELED) {
      return;
    }

    Range<Float> featureRtRange = feature.getRawDataPointsRTRange();

    // use scans from feature list
    final List<? extends Scan> allScans = feature.getFeatureList().getSeletedScans(rawDataFile);
    if (allScans == null) {
      setErrorMessage("Selected scans were not set for pseudo scan in dataset caluclation task");
      setStatus(TaskStatus.ERROR);
      return;
    }

    // filter scans within RT range of feature
    List<? extends Scan> scans = BinarySearch.indexRange(featureRtRange.lowerEndpoint(),
        featureRtRange.upperEndpoint(), allScans.size(),
        index -> allScans.get(index).getRetentionTime()).sublist(allScans);

    // get all mz ranges
    List<Range<Double>> mzRangesSorted = Arrays.stream(pseudoScan.getMzValues(new double[0]))
        .sorted().mapToObj(mzTolerance::getToleranceRange).toList();

    // extract all IonSeries at once
    extractFunction = new ExtractMzRangesIonSeriesFunction(rawDataFile, scans, mzRangesSorted,
        this);

    BuildingIonSeries[] ionSeries = extractFunction.calculate();

    if (isCanceled()) {
      return;
    }

    // create feature datasets
    final List<FeatureDataSet> features = Arrays.stream(ionSeries)
        .map(series -> series.toFullIonTimeSeries(null, scans)) //
        .map(series -> new ModularFeature(newFeatureList, rawDataFile, series,
            FeatureStatus.DETECTED)) //
        .map(FeatureDataSet::new) //
        .toList();

    MZmineCore.runLater(() -> {
      if (getStatus() == TaskStatus.CANCELED) {
        return;
      }
      chromPlot.removeAllFeatureDataSets(false);
      for (FeatureDataSet featureDataSet : features) {
        chromPlot.addFeatureDataSetRandomColor(featureDataSet);
      }
    });

    setStatus(TaskStatus.FINISHED);
  }
}
