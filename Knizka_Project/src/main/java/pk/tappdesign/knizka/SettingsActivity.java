/*
 * Copyright (C) 2020 TappDesign Studios
 * Copyright (C) 2013-2020 Federico Iosue (federico@iosue.it)
 *
 * This software is based on Omni-Notes project developed by Federico Iosue
 * https://github.com/federicoiosue/Omni-Notes
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pk.tappdesign.knizka;

import android.view.View;

import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.pixplicity.easyprefs.library.Prefs;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import it.feio.android.analitica.AnalyticsHelper;
import pk.tappdesign.knizka.async.DataBackupIntentService;
import pk.tappdesign.knizka.databinding.ActivitySettingsBinding;
import pk.tappdesign.knizka.utils.ThemeHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.widget.Toolbar;

import android.view.ViewGroup;

import static pk.tappdesign.knizka.utils.ConstantsBase.PREF_HTML_COLOR_SCHEME;
import static pk.tappdesign.knizka.utils.ConstantsBase.PREF_HTML_COLOR_SCHEME_VALUE_BRIGHT;
import static pk.tappdesign.knizka.utils.ConstantsBase.PREF_HTML_COLOR_SCHEME_VALUE_DARK;


public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, FolderChooserDialog.FolderCallback {

   private List<Fragment> backStack = new ArrayList<>();
   private String currentTheme = "";
   private ActivitySettingsBinding binding;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      currentTheme = ThemeHelper.trySetTheme(this);

      binding = ActivitySettingsBinding.inflate(getLayoutInflater());
      View view = binding.getRoot();
      setContentView(view);

      initUI();
      getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new SettingsFragment()).commit();

   }

   @Override
   protected void onResume() {
      super.onResume();

      String themeFromPref = Prefs.getString(PREF_HTML_COLOR_SCHEME, PREF_HTML_COLOR_SCHEME_VALUE_BRIGHT);

      if (!(themeFromPref.equals(currentTheme))) {
         recreate();
      }
   }

   void initUI() {
      setSupportActionBar(binding.toolbar.toolbar);
      binding.toolbar.toolbar.setNavigationOnClickListener(v -> onBackPressed());
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setHomeButtonEnabled(true);
   }


   private void replaceFragment(Fragment sf) {
      getSupportFragmentManager().beginTransaction().setCustomAnimations(R.animator.fade_in, R.animator.fade_out,
              R.animator.fade_in, R.animator.fade_out).replace(R.id.content_frame, sf).commit();
   }


   @Override
   public void onBackPressed() {
      if (!backStack.isEmpty()) {
         replaceFragment(backStack.remove(backStack.size() - 1));
      } else {
         super.onBackPressed();
      }
   }


   public void showMessage(int messageId, Style style) {
      showMessage(getString(messageId), style);
   }


   public void showMessage(String message, Style style) {
      // ViewGroup used to show Crouton keeping compatibility with the new Toolbar
      Crouton.makeText(this, message, style, binding.croutonHandle.croutonHandle).show();
   }


   @Override
   public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
      new MaterialDialog.Builder(this)
              .title(R.string.data_import_message_warning)
              .content(folder.getName())
              .positiveText(R.string.confirm)
              .onPositive((dialog1, which) -> {
                 ((Knizka) getApplication()).getAnalyticsHelper().trackEvent(AnalyticsHelper.CATEGORIES.SETTING,
                         "settings_import_data");
                 Intent service = new Intent(getApplicationContext(), DataBackupIntentService.class);
                 service.setAction(DataBackupIntentService.ACTION_DATA_IMPORT_LEGACY);
                 service.putExtra(DataBackupIntentService.INTENT_BACKUP_NAME, folder.getAbsolutePath());
                 startService(service);
              }).build().show();
   }

   @Override
   public void onFolderChooserDismissed(@NonNull FolderChooserDialog dialog) {
      // Nothing to do
   }

   @Override
   public void onPointerCaptureChanged(boolean hasCapture) {
      // Nothing to do
   }

   @Override
   public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
      Bundle b = new Bundle();
      b.putString(SettingsFragment.XML_NAME, pref.getKey());

      final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
              getClassLoader(),
              pref.getFragment());
      fragment.setArguments(b);
      fragment.setTargetFragment(caller, 0);

      getSupportFragmentManager().beginTransaction()
              .replace(R.id.content_frame, fragment)
              .addToBackStack(null)
              .commit();
      return true;
   }

}
