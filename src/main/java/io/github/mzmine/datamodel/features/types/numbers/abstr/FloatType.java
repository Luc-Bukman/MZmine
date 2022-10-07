/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.types.numbers.abstr;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import java.text.NumberFormat;
import java.util.List;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class FloatType extends NumberType<Float> {

  protected FloatType(NumberFormat defaultFormat) {
    super(defaultFormat);
  }

  @Override
  public @NotNull String getFormattedString(Float value) {
    return value == null ? "" : getFormatter().format(value);
  }

  public @NotNull String getFormattedString(float value) {
    return getFormatter().format(value);
  }

  @Override
  public Class<Float> getValueClass() {
    return Float.class;
  }

  @Override
  public Property<Float> createProperty() {
    return new SimpleObjectProperty<Float>();
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (value == null) {
      return;
    }
    if (!(value instanceof Float)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: "
              + value.getClass());
    }
    writer.writeCharacters(String.valueOf(value));
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull ModularFeatureList flist,
      @NotNull ModularFeatureListRow row, @Nullable ModularFeature feature,
      @Nullable RawDataFile file) throws XMLStreamException {
    String str = reader.getElementText();
    if (str == null || str.isEmpty()) {
      return null;
    }
    return Float.parseFloat(str);
  }

  @Override
  public Object evaluateBindings(@NotNull BindingsType bindingType,
      @NotNull List<? extends ModularDataModel> models) {
    Object result = super.evaluateBindings(bindingType, models);
    if (result == null) {
      // general cases here - special cases handled in other classes
      switch (bindingType) {
        case AVERAGE: {
          // calc average center of ranges
          float mean = 0f;
          int c = 0;
          for (var model : models) {
            Float value = model.get(this);
            if (value != null) {
              mean += value;
              c++;
            }
          }
          return c == 0 ? 0f : mean / c;
        }
        case SUM, CONSENSUS: {
          // calc average center of ranges
          float sum = 0f;
          for (var model : models) {
            Float value = model.get(this);
            if (value != null) {
              sum += value;
            }
          }
          return sum;
        }
        case RANGE: {
          // calc average center of ranges
          Range<Float> range = null;
          for (var model : models) {
            Float value = model.get(this);
            if (value != null) {
              if (range == null) {
                range = Range.singleton(value);
              } else {
                range.span(Range.singleton(value));
              }
            }
          }
          return range;
        }
        case MIN: {
          // calc average center of ranges
          Float min = null;
          for (var model : models) {
            Float value = model.get(this);
            if (value != null && (min == null || value < min)) {
              min = value;
            }
          }
          return min;
        }
        case MAX: {
          // calc average center of ranges
          Float max = null;
          for (var model : models) {
            Float value = model.get(this);
            if (value != null && (max == null || value > max)) {
              max = value;
            }
          }
          return max;
        }
      }
    }
    return result;
  }

}
