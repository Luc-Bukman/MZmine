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

package io.github.mzmine.javafx.util;

public enum FxIcons implements IconCodeSupplier {
  // status
  CHECK_CIRCLE, X_CIRCLE, EXCLAMATION_TRIANGLE, // control flow
  ARROW_LEFT, ARROW_RIGHT, ARROW_UP, ARROW_DOWN,

  //
  USER, BUG, WEBSITE, GEAR_PREFERENCES, RELOAD, YOUTUBE, DEVELOPMENT, BOOK, ROCKET;


  @Override
  public String getIconCode() {
    return switch (this) {
      case CHECK_CIRCLE -> "bi-check2-circle";
      case X_CIRCLE -> "bi-x-circle";
      case EXCLAMATION_TRIANGLE -> "bi-exclamation-triangle";
      case USER -> "bi-person-circle";
      case BUG -> "bi-bug";
      case GEAR_PREFERENCES -> "bi-gear";
      case WEBSITE -> "bi-globe2";
      case ARROW_LEFT -> "bi-arrow-left";
      case ARROW_RIGHT -> "bi-arrow-right";
      case ARROW_UP -> "bi-arrow-up";
      case ARROW_DOWN -> "bi-arrow-down";
      case RELOAD -> "bi-arrow-repeat";
      case YOUTUBE -> "bi-youtube";
      case DEVELOPMENT -> "bi-code-slash";
      case BOOK -> "bi-book-half";
      case ROCKET -> "las-rocket";
    };
  }
}
