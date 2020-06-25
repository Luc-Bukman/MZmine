/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.ims.imsVisualizer;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerParameters;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerTask;
import io.github.mzmine.parameters.ParameterSet;
import org.jfree.data.xy.AbstractXYZDataset;

import java.util.ArrayList;

public class MobilityFrameXYZDataset extends AbstractXYZDataset {

  private RawDataFile dataFiles[];
  private Scan scans[];
  private Range<Double> mzRange;
  ArrayList<Double> mobility;
  ArrayList<Double> mzValues;
  ArrayList<Double> intensity;
  private Double[] xValues;
  private Double[] yValues;
  private Double[] zValues;
  private Double selectedRetentionTime;
  private int itemSize;
  private ImsVisualizerTask imsVisualizerTask;

  public MobilityFrameXYZDataset(ParameterSet parameters, double retentionTime, ImsVisualizerTask imsVisualizerTask) {
    dataFiles =
        parameters
            .getParameter(ImsVisualizerParameters.dataFiles)
            .getValue()
            .getMatchingRawDataFiles();

    scans =
        parameters
            .getParameter(ImsVisualizerParameters.scanSelection)
            .getValue()
            .getMatchingScans(dataFiles[0]);

    mzRange = parameters.getParameter(ImsVisualizerParameters.mzRange).getValue();

    selectedRetentionTime = retentionTime;

    imsVisualizerTask = imsVisualizerTask;

    mobility = new ArrayList<>();
    mzValues = new ArrayList<>();
    intensity = new ArrayList<>();
    double maxIntensity = -1;
    if (selectedRetentionTime == -1) {
      for (int i = 0; i < scans.length; i++) {

        DataPoint dataPoint[] = scans[i].getDataPointsByMass(mzRange);
        double intensitySum = 0;
        for (int j = 0; j < dataPoint.length; j++) {
          intensitySum += dataPoint[j].getIntensity();
        }

        if (maxIntensity < intensitySum) {
          maxIntensity = intensitySum;
          selectedRetentionTime = scans[i].getRetentionTime();
        }
      }
      imsVisualizerTask.setSelectedRetentionTime(selectedRetentionTime);
    }

    for (int i = 0; i < scans.length; i++) {
      if (scans[i].getRetentionTime() == selectedRetentionTime) {
        DataPoint dataPoint[] = scans[i].getDataPointsByMass(mzRange);

        for (int j = 0; j < dataPoint.length; j++) {
          mobility.add(scans[i].getMobility());
          mzValues.add(dataPoint[j].getMZ());
          intensity.add(dataPoint[j].getIntensity());
        }
      }
    }

    itemSize = mobility.size();
    xValues = new Double[itemSize];
    yValues = new Double[itemSize];
    zValues = new Double[itemSize];
    xValues = mzValues.toArray(new Double[itemSize]);
    yValues = mobility.toArray(new Double[itemSize]);
    zValues = intensity.toArray(new Double[itemSize]);
  }

  public double getselectedRetentionTime() {
    return selectedRetentionTime;
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public Comparable getSeriesKey(int series) {
    return getRowKey(series);
  }

  public Comparable<?> getRowKey(int item) {
    return scans[item].toString();
  }

  @Override
  public int getItemCount(int series) {
    return itemSize;
  }

  @Override
  public Number getX(int series, int item) {
    return xValues[item];
  }

  @Override
  public Number getY(int series, int item) {
    return yValues[item];
  }

  @Override
  public Number getZ(int series, int item) {
    return zValues[item];
  }
}
