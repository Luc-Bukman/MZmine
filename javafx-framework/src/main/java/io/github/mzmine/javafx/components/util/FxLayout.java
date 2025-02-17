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

package io.github.mzmine.javafx.components.util;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.controlsfx.tools.Borders;

public class FxLayout {

  public static final int DEFAULT_SPACE = 5;
  public static final int DEFAULT_ICON_SPACE = 0;
  public static final Insets DEFAULT_PADDING_INSETS = new Insets(5);

  public static FlowPane newIconPane(Orientation orientation, Node... children) {
    var alignment = orientation == Orientation.HORIZONTAL ? Pos.CENTER_LEFT : Pos.TOP_CENTER;
    var pane = newFlowPane(alignment, Insets.EMPTY, children);
    pane.setOrientation(orientation);
    pane.setHgap(DEFAULT_ICON_SPACE);
    pane.setVgap(DEFAULT_ICON_SPACE);
    return pane;
  }

  public static FlowPane newFlowPane() {
    return new FlowPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE);
  }

  public static FlowPane newFlowPane(Node... children) {
    return newFlowPane(DEFAULT_PADDING_INSETS, children);
  }

  public static FlowPane newFlowPane(Pos alignment, Node... children) {
    return newFlowPane(alignment, DEFAULT_PADDING_INSETS, children);
  }

  public static FlowPane newFlowPane(Insets padding, Node... children) {
    return newFlowPane(Pos.CENTER_LEFT, padding, children);
  }

  public static FlowPane newFlowPane(Pos alignment, Insets padding, Node... children) {
    var pane = new FlowPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE, children);
    pane.setPadding(padding);
    pane.setAlignment(alignment);
    return pane;
  }

  public static VBox newVBox(Node... children) {
    return newVBox(DEFAULT_PADDING_INSETS, children);
  }

  public static VBox newVBox(Pos alignment, Node... children) {
    return newVBox(alignment, DEFAULT_PADDING_INSETS, children);
  }

  public static VBox newVBox(Insets padding, Node... children) {
    return newVBox(Pos.CENTER_LEFT, padding, children);
  }

  public static VBox newVBox(Pos alignment, Insets padding, Node... children) {
    var pane = new VBox(DEFAULT_SPACE, children);
    pane.setPadding(padding);
    pane.setAlignment(alignment);
    return pane;
  }

  public static HBox newHBox(Node... children) {
    return newHBox(DEFAULT_PADDING_INSETS, children);
  }

  public static HBox newHBox(Pos alignment, Node... children) {
    return newHBox(alignment, DEFAULT_PADDING_INSETS, children);
  }

  public static HBox newHBox(Insets padding, Node... children) {
    return newHBox(Pos.CENTER_LEFT, padding, children);
  }

  public static HBox newHBox(Pos alignment, Insets padding, Node... children) {
    var pane = new HBox(DEFAULT_SPACE, children);
    pane.setAlignment(alignment);
    pane.setPadding(padding);
    return pane;
  }

  public static StackPane newStackPane(Node... children) {
    return newStackPane(DEFAULT_PADDING_INSETS, children);
  }

  public static StackPane newStackPane(Insets padding, Node... children) {
    var pane = new StackPane(children);
    pane.setPadding(padding);
    return pane;
  }

  public static BorderPane newBorderPane(Node center) {
    return newBorderPane(DEFAULT_PADDING_INSETS, center);
  }

  public static BorderPane newBorderPane(Insets padding, Node center) {
    var pane = new BorderPane(center);
    pane.setPadding(padding);
    return pane;
  }

  public static void centerAllNodesHorizontally(GridPane pane) {
    pane.getChildren().forEach(node -> GridPane.setHalignment(node, HPos.CENTER));
  }

  public static void centerAllNodesVertically(GridPane pane) {
    pane.getChildren().forEach(node -> GridPane.setValignment(node, VPos.CENTER));
  }

  public static Node wrapInBorder(Node node) {
    return Borders.wrap(node).lineBorder().radius(FxLayout.DEFAULT_SPACE)
        .innerPadding(FxLayout.DEFAULT_SPACE).outerPadding(FxLayout.DEFAULT_SPACE).buildAll();
  }
}
