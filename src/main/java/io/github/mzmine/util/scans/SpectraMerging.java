/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.util.scans;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.impl.SimpleMergedMsMsSpectrum;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SpectraMerging {

  private static final DataPointSorter sorter = new DataPointSorter(SortingProperty.Intensity,
      SortingDirection.Descending);

  /**
   * @param source
   * @param noiseLevel
   * @param tolerance
   * @param mergingType
   * @param mzCenterFunction
   * @param <T>
   * @return double[2][] array, [0][] being the mzs, [1] being the intensities.
   */
  public static <T extends MassSpectrum> double[][] calculatedMergedMzsAndIntensities(
      Collection<T> source, double noiseLevel, MZTolerance tolerance, MergingType mergingType,
      CenterFunction mzCenterFunction) {

    // extract all datapoints above the noise level
    // todo: once we got rid of MassLists, we should update this to use the replacement and not use another noise level
    List<IndexedDataPoint> dataPoints = new ArrayList<>();
    int index = 0;
    for (T spectrum : source) {
//      double[] mzs = new double[spectrum.getNumberOfDataPoints()];
//      double[] intensities = new double[spectrum.getNumberOfDataPoints()];

      double[][] data = DataPointUtils
          .getDatapointsAboveNoiseLevel(spectrum.getMzValues(), spectrum.getIntensityValues(),
              noiseLevel);

      double[] mzs = data[0];
      double[] intensities = data[1];

      for (int i = 0; i < mzs.length; i++) {
        IndexedDataPoint dp = new IndexedDataPoint(mzs[i], intensities[i], index);
        dataPoints.add(dp);
      }
      index++;
    }

    Collections.sort(dataPoints, sorter);

    // set is sorted by the index of the datapoint, so we can quickly check the presence of the same index
    RangeMap<Double, SortedSet<IndexedDataPoint>> dataPointRanges = TreeRangeMap.create();

    for (IndexedDataPoint dp : dataPoints) {
      SortedSet<IndexedDataPoint> dplist = dataPointRanges.get(dp.getMZ());
      boolean containsIndex = false;

      // no entry -> make a new one
      if (dplist == null) {
        dplist = new TreeSet<>(Comparator.comparingInt(IndexedDataPoint::getIndex));
        Range<Double> range = createNewNonOverlappingRange(dataPointRanges,
            tolerance.getToleranceRange(dp.getMZ()));
        dataPointRanges.put(range, dplist);
      } else { // we have an entry, check if if we have the same index in there already
        if (dp.getIndex() > dplist.first().getIndex() && dp.getIndex() < dplist.last().getIndex()) {
          for (IndexedDataPoint indexedDataPoint : dplist) {
            if (dp.getIndex() == indexedDataPoint.getIndex()) {
              containsIndex = true;
              break;
            }
            if (dp.getIndex() > indexedDataPoint.getIndex()) {
              break;
            }
          }
        }
        // if an entry contains that index, make a new entry
        if (containsIndex) {
          dplist = new TreeSet<>(Comparator.comparingInt(IndexedDataPoint::getIndex));
          Range<Double> range = createNewNonOverlappingRange(dataPointRanges,
              tolerance.getToleranceRange(dp.getMZ()));
          dataPointRanges.put(range, dplist);
        }
      }

      // now add the datapoint to the set
      dplist.add(dp);
    }

    int numDps = dataPointRanges.asMapOfRanges().size();
    double[] newIntensities = new double[numDps];
    double[] newMzs = new double[numDps];
    int counter = 0;

    // now we got everything in place and have to calculate the new intensities and mzs
    for (Entry<Range<Double>, SortedSet<IndexedDataPoint>> entry : dataPointRanges
        .asMapOfRanges().entrySet()) {
      double[] mzs = entry.getValue().stream().mapToDouble(IndexedDataPoint::getMZ).toArray();
      double[] intensities = entry.getValue().stream().mapToDouble(IndexedDataPoint::getIntensity)
          .toArray();

      double newMz = mzCenterFunction.calcCenter(mzs, intensities);
      double newIntensity = switch (mergingType) {
        case SUMMED -> Arrays.stream(intensities).sum();
        case MAXIMUM -> Arrays.stream(intensities).max().getAsDouble();
        case AVERAGE -> Arrays.stream(intensities).average().getAsDouble();
      };

      newMzs[counter] = newMz;
      newIntensities[counter] = newIntensity;
      counter++;
    }

    double[][] data = new double[2][];
    data[0] = newMzs;
    data[1] = newIntensities;
    return data;
  }

  private static Range<Double> createNewNonOverlappingRange(RangeMap<Double, ?> rangeMap,
      Range<Double> proposedRange) {
    Entry<Range<Double>, ?> lowerEntry = rangeMap.getEntry(proposedRange.lowerEndpoint());
    Entry<Range<Double>, ?> upperEntry = rangeMap.getEntry(proposedRange.upperEndpoint());

    double lowerBound =
        (lowerEntry == null) ? proposedRange.lowerEndpoint() : lowerEntry.getKey().upperEndpoint();
    double upperBound =
        (upperEntry == null) ? proposedRange.upperEndpoint() : upperEntry.getKey().lowerEndpoint();

    if (lowerEntry == null && upperEntry == null) {
      return Range.closed(lowerBound, upperBound);
    } else if (lowerEntry != null && upperEntry == null) {
      return Range.openClosed(lowerBound, upperBound);
    } else if (lowerEntry == null && upperEntry != null) {
      return Range.closedOpen(lowerBound, upperBound);
    } else {
      return Range.open(lowerBound, upperBound);
    }
  }

  public static MergedMsMsSpectrum getMergedMsMsSpectrumForPASEF(ImsMsMsInfo info, double noiseLevel,
      MZTolerance tolerance, MergingType mergingType, MemoryMapStorage storage) {

    if(info == null) {
      return null;
    }

    Range<Integer> spectraNumbers = info.getSpectrumNumberRange();
    Frame frame = info.getFrameNumber();
    float collisionEnergy = info.getCollisionEnergy();
    double precursorMz = info.getLargestPeakMz();

    List<MobilityScan> mobilityScans = frame.getMobilityScans().stream()
        .filter(ms -> spectraNumbers.contains(ms.getMobilityScamNumber())).collect(
            Collectors.toList());

    CenterFunction cf = new CenterFunction(CenterMeasure.AVG, Weighting.LINEAR);

    double[][] merged = calculatedMergedMzsAndIntensities(mobilityScans, noiseLevel, tolerance,
        mergingType, cf);

    return new SimpleMergedMsMsSpectrum(storage, merged[0],
        merged[1], precursorMz, null, collisionEnergy, frame.getMSLevel(), mobilityScans,
        mergingType, cf);
  }

  public enum MergingType {
    SUMMED, MAXIMUM, AVERAGE
  }
}
