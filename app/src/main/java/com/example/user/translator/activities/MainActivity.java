package com.example.user.translator.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;

import com.example.user.translator.fragments.FavoriteFragment;
import com.example.user.translator.fragments.HistoryFragment;
import com.example.user.translator.fragments.SettingsFragment;
import com.example.user.translator.fragments.TranslateFragment;
import com.example.user.translator.R;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private Fragment fragment;
    private FragmentManager fragmentManager;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_translate:
                    fragment = new TranslateFragment();
                    break;
                case R.id.navigation_history:
                    fragment = new HistoryFragment();
                    break;
                case R.id.navigation_favorite:
                    fragment = new FavoriteFragment();
                    break;
            }
            hideKeyboard();
//            очищаем BackStack после выхода из настроек
            for (int i = 0; i < fragmentManager.getBackStackEntryCount(); ++i) {
                fragmentManager.popBackStack();
            }
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.content, fragment).commit();
            return true;
        }

    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            hideKeyboard();
            fragment = new SettingsFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.content, fragment)
                    .addToBackStack(getString(R.string.action_settings)).commit();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentManager = getSupportFragmentManager();

        bottomNavigation = (BottomNavigationView) findViewById(R.id.navigation);
        bottomNavigation.getMenu().getItem(1).setChecked(true);
        bottomNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);


        if (savedInstanceState == null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            fragment = new TranslateFragment();
            transaction.add(R.id.content, fragment).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void hideKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }
}
