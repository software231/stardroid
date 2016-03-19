// Copyright 2010 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.android.stardroid.activities;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.android.stardroid.R;
import com.google.android.stardroid.R.layout;
import com.google.android.stardroid.R.string;
import com.google.android.stardroid.StardroidApplication;
import com.google.android.stardroid.search.SearchResult;
import com.google.android.stardroid.util.Analytics;
import com.google.android.stardroid.util.MiscUtil;
import com.google.android.stardroid.views.TimeTravelDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * A grab bag of dialogs used in the DynamicStarMapActivity.  Extracted
 * to this class simply to reduce clutter in an already complex class.
 *
 * @author John Taylor
 */
// TODO(jontayler): rework this into dialog fragments.
public class DialogFactory {
  private static final String TAG = MiscUtil.getTag(DialogFactory.class);

  static final int DIALOG_ID_TIME_TRAVEL = 1;
  static final int DIALOG_ID_MULTIPLE_SEARCH_RESULTS = 2;
  static final int DIALOG_ID_NO_SEARCH_RESULTS = 3;
  static final int DIALOG_ID_HELP = 4;
  static final int DIALOG_ID_EULA_NO_BUTTONS = 5;
  static final int DIALOG_ID_EULA_WITH_BUTTONS = 6;
  static final int DIALOG_ID_NO_SENSORS = 7;

  private DynamicStarMapActivity parentActivity;
  private ArrayAdapter<SearchResult> multipleSearchResultsAdaptor;
  private SharedPreferences preferences;

  /**
   * Constructor.
   *
   * @param parentActivity the parent activity showing these dialogs.
   */
  public DialogFactory(DynamicStarMapActivity parentActivity, SharedPreferences preferences) {
    this.parentActivity = parentActivity;
    this.preferences = preferences;
    multipleSearchResultsAdaptor = new ArrayAdapter<>(
        parentActivity, android.R.layout.simple_list_item_1, new ArrayList<SearchResult>());
  }

  /**
   * Creates dialogs on demand.  Delegated to by the parentActivity.
   */
  public Dialog onCreateDialog(int id) {
    switch (id) {
      case (DialogFactory.DIALOG_ID_HELP):
        return createHelpDialog();
      case (DialogFactory.DIALOG_ID_NO_SEARCH_RESULTS):
        return createNoSearchResultsDialog();
      case (DialogFactory.DIALOG_ID_MULTIPLE_SEARCH_RESULTS):
        return createMultipleSearchResultsDialog();
      case (DialogFactory.DIALOG_ID_TIME_TRAVEL):
        return createTimeTravelDialog();
      case (DIALOG_ID_EULA_NO_BUTTONS):
        return createTermsOfServiceDialog(true);
      case (DIALOG_ID_EULA_WITH_BUTTONS):
        return createTermsOfServiceDialog(false);
      case (DIALOG_ID_NO_SENSORS):
        return createNoSensorsDialog();
    }
    throw new RuntimeException("Unknown dialog Id.");
  }

  private Dialog createNoSensorsDialog() {
    LayoutInflater inflater = parentActivity.getLayoutInflater();
    final View view = inflater.inflate(R.layout.no_sensor_warning, null);
    AlertDialog alertDialog = new Builder(parentActivity)
        .setTitle(R.string.warning_dialog_title)
        .setView(view).setNegativeButton(android.R.string.ok,
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                Log.d(TAG, "No Sensor Dialog closed");
                preferences.edit().putBoolean(
                    DynamicStarMapActivity.NO_WARN_ABOUT_MISSING_SENSORS,
                    ((CheckBox) view.findViewById(R.id.no_show_dialog_again)).isChecked()).commit();
                dialog.dismiss();
              }
            }).create();
    return alertDialog;
  }

  /**
   * Creates and returns a help dialog box.
   */
  private Dialog createHelpDialog() {
    final LayoutInflater inflater = parentActivity.getLayoutInflater();
    final View view = inflater.inflate(R.layout.help, null);
    final AlertDialog alertDialog = new Builder(parentActivity)
        .setTitle(R.string.help_dialog_title)
        .setView(view).setNegativeButton(android.R.string.ok,
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                Log.d(TAG, "Help Dialog closed");
                dialog.dismiss();
              }
            }).create();
    String helpText = String.format(parentActivity.getString(R.string.help_text), getVersionName());
    Spanned formattedHelpText = Html.fromHtml(helpText);
    TextView helpTextView = (TextView) view.findViewById(R.id.help_box_text);
    helpTextView.setText(formattedHelpText, TextView.BufferType.SPANNABLE);
    return alertDialog;
  }

  private String getVersionName() {
    return ((StardroidApplication) parentActivity.getApplication()).getVersionName();
  }

  /**
   * Creates and returns a dialog indicating that no search results were found.
   */
  private Dialog createNoSearchResultsDialog() {
    final AlertDialog dialog = new Builder(parentActivity)
        .setTitle(string.no_search_title).setMessage(string.no_search_results_text)
        .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            Log.d(TAG, "No search results Dialog closed");
            dialog.dismiss();
          }
        }).create();
    return dialog;
  }

  /**
   * Creates and returns a dialog allowing the user to choose amongst several search
   * results.  The search results are stored in the {@link #multipleSearchResultsAdaptor}.
   */
  private Dialog createMultipleSearchResultsDialog() {
    final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        if (whichButton == Dialog.BUTTON2) {
          Log.d(TAG, "Many search results Dialog closed with cancel");
          dialog.dismiss();
        } else {
          final SearchResult item = multipleSearchResultsAdaptor.getItem(whichButton);
          parentActivity.activateSearchTarget(item.coords, item.capitalizedName);
          dialog.dismiss();
        }
      }
    };

    final AlertDialog dialog = new Builder(parentActivity)
        .setTitle(string.many_search_results_title)
        .setNegativeButton(android.R.string.cancel, onClickListener)
        .setAdapter(multipleSearchResultsAdaptor, onClickListener)
        .create();
    return dialog;
  }

  /**
   * Helper method that modifies the {@link #multipleSearchResultsAdaptor}.
   * This isn't really the right way to do it.  The correct thing
   * to do would be to do away with this method and pass the search results to
   * the dialog creator inside a Bundle.  However, that would require
   * SearchResults to be made into a Parcelable.  Furthermore, onCreateDialog
   * is not called the second time this dialog is requested, so we would have
   * to override onPrepareDialog instead
   * ...and that has only been around since API v8.
   * So, enough excuses - this is wrong, but so much easier.
   *
   * @param results the search results
   */
  public void showUserChooseResultDialog(List<SearchResult> results) {
    multipleSearchResultsAdaptor.clear();
    for (SearchResult result : results) {
      multipleSearchResultsAdaptor.add(result);
    }
    parentActivity.showDialog(DialogFactory.DIALOG_ID_MULTIPLE_SEARCH_RESULTS);
  }

  /**
   * Display the Terms of Service and privacy policy to the user.
   */
  private Dialog createTermsOfServiceDialog(boolean hideButtons) {
    AlertDialog tosDialog = null;
    LayoutInflater inflater = parentActivity.getLayoutInflater();
    View view = inflater.inflate(layout.tos_view, null);

    String apologyText = parentActivity.getString(string.language_apology_text);
    Spanned formattedApologyText = Html.fromHtml(apologyText);
    TextView apologyTextView = (TextView) view.findViewById(R.id.language_apology_box_text);
    apologyTextView.setText(formattedApologyText, TextView.BufferType.SPANNABLE);

    String whatsNewText = String.format(parentActivity.getString(string.whats_new_text), getVersionName());
    Spanned formattedWhatsNewText = Html.fromHtml(whatsNewText);
    TextView whatsNewTextView = (TextView) view.findViewById(R.id.whats_new_box_text);
    whatsNewTextView.setText(formattedWhatsNewText, TextView.BufferType.SPANNABLE);

    String eulaText = String.format(parentActivity.getString(R.string.eula_text), getVersionName());
    Spanned formattedEulaText = Html.fromHtml(eulaText);
    TextView eulaTextView = (TextView) view.findViewById(R.id.eula_box_text);
    eulaTextView.setText(formattedEulaText, TextView.BufferType.SPANNABLE);

    // Note that we've made the "accept" button the negative button and the "decline" button
    // the positive button as an experiment.
    if (!hideButtons) {
      tosDialog = new Builder(parentActivity)
          .setTitle(string.menu_tos)
          .setView(view)
          .setNegativeButton(string.dialog_accept,
              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                  Log.d(TAG, "TOS Dialog closed.  User accepts.");
                  parentActivity.recordEulaAccepted();
                  dialog.dismiss();
                  Analytics.getPreviouslyCreatedInstance().trackEvent(
                      Analytics.APP_CATEGORY, Analytics.TOS_ACCEPT, Analytics.TOS_ACCEPTED, 1);
                }
              })
          .setPositiveButton(string.dialog_decline,
              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                  Log.d(TAG, "TOS Dialog closed.  User declines.");
                  dialog.dismiss();
                  Analytics.getPreviouslyCreatedInstance().trackEvent(
                      Analytics.APP_CATEGORY, Analytics.TOS_ACCEPT, Analytics.TOS_REJECTED, 0);
                  parentActivity.finish();
                }
              })
          .create();
    } else {
      tosDialog = new Builder(parentActivity)
          .setTitle(string.menu_tos)
          .setView(view)
          .create();
    }
    return tosDialog;
  }

  /**
   * Creates the time travel dialog.
   */
  private Dialog createTimeTravelDialog() {
    Log.d(TAG, "Creating time dialog.");
    TimeTravelDialog timeTravelDialog = new TimeTravelDialog(parentActivity,
        parentActivity.getModel());
    return timeTravelDialog;
  }
}
