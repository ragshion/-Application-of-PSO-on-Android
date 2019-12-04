/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.khilman.www.learngoogleapi.placehelpers;

import android.widget.TextView;

import com.google.android.libraries.places.api.model.Place;

/**
 * Utility class for converting objects to viewable strings and back.
 */
public final class StringUtil {

  private static final String FIELD_SEPARATOR = "\n\t";
  private static final String RESULT_SEPARATOR = "\n---\n\t";

  static void prepend(TextView textView, String prefix) {
    textView.setText(prefix + "\n\n" + textView.getText());
  }

  static String stringify(Place place) {
    return place.getName()
            + " ("
            + place.getAddress()
            + ")";
  }

  public static String stringifyAutocompleteWidget(Place place) {
    StringBuilder builder = new StringBuilder();

    builder.append("Autocomplete Widget Result:").append(RESULT_SEPARATOR);

    builder.append(stringify(place));

    return builder.toString();
  }
}
