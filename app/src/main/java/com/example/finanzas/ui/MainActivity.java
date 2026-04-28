package com.example.finanzas.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.finanzas.R;
import com.example.finanzas.util.Prefs;
import com.example.finanzas.util.PinSession;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PinSession.lock();


        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

        NavHostFragment navHost =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navHost = Objects.requireNonNull(navHost, "NavHostFragment not found");
        navController = navHost.getNavController();


        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_list,
                R.id.nav_budget,
                R.id.nav_goals,
                R.id.nav_reminders,
                R.id.nav_imports,
                R.id.nav_perfil,
                R.id.nav_login,
                R.id.nav_register
        ).setOpenableLayout(drawerLayout).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);


        navView.setNavigationItemSelectedListener(this::onDrawerItemSelected);


        navController.addOnDestinationChangedListener((controller, destination, args) -> {
            int destId = destination.getId();
            boolean isAuthScreen = (destId == R.id.nav_login
                    || destId == R.id.nav_register
                    || destId == R.id.nav_pin_lock);

            if (isAuthScreen) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                }
                toolbar.setNavigationIcon(null);
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            }

            if (navView != null) {
                String token = Prefs.getToken(this);
                boolean loggedIn = token != null || Prefs.isLoggedIn(this);

                MenuItem logoutItem = navView.getMenu().findItem(R.id.nav_logout);
                if (logoutItem != null) logoutItem.setVisible(loggedIn);
            }

            if (!isAuthScreen) {
                enforcePinIfNeeded();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        enforcePinIfNeeded();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations() && Prefs.hasPin(this)) {
            PinSession.lock();
        }
    }

    private boolean onDrawerItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawers();

        int destId = item.getItemId();
        NavOptions opts = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.getGraph().getId(), true)
                .build();

        if (destId == R.id.nav_home
                || destId == R.id.nav_list
                || destId == R.id.nav_budget
                || destId == R.id.nav_goals
                || destId == R.id.nav_reminders
                || destId == R.id.nav_imports
                || destId == R.id.nav_perfil) {
            navController.navigate(destId, null, opts);
            return true;
        }

        if (destId == R.id.nav_logout) {

            Prefs.clearAuth(this);

            PinSession.lock();

            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

            NavOptions out = new NavOptions.Builder()
                    .setPopUpTo(navController.getGraph().getId(), true)
                    .build();
            navController.navigate(R.id.nav_login, null, out);
            return true;
        }

        return false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void enforcePinIfNeeded() {
        if (navController == null || !Prefs.hasPin(this) || PinSession.isUnlocked()) {
            return;
        }
        NavDestination dest = navController.getCurrentDestination();
        if (dest == null) return;
        int destId = dest.getId();
        if (destId == R.id.nav_pin_lock || destId == R.id.nav_login
                || destId == R.id.nav_register || destId == R.id.nav_pin_setup) {
            return;
        }
        navController.navigate(R.id.nav_pin_lock);
    }
}
