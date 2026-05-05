package com.example.finanzas.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.local.LocalRepository;
import com.example.finanzas.ui.viewmodel.BudgetViewModel;
import com.example.finanzas.ui.viewmodel.HomeViewModel;
import com.example.finanzas.ui.viewmodel.ReportsViewModel;
import com.example.finanzas.ui.viewmodel.TransactionsViewModel;
import com.example.finanzas.util.Prefs;
import com.example.finanzas.util.PinSession;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_POST_NOTIFICATIONS = 1001;

    private AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private MaterialToolbar toolbar;
    private View navHostView;
    private View bottomNavContainer;
    private int contentTopMargin;
    private int contentBottomMargin;
    private int pendingDrawerDestination = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SettingsService.applyThemeMode(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PinSession.lock();


        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        configureSystemBars();

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setScrimColor(ContextCompat.getColor(this, R.color.drawer_scrim));
        navView = findViewById(R.id.nav_view);
        navView.setBackgroundColor(ContextCompat.getColor(this, R.color.drawer_body_background));
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                if (slideOffset > 0f) {
                    applyDrawerSystemBars();
                }
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                applyDrawerSystemBars();
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                configureSystemBars();
            }
        });
        navHostView = findViewById(R.id.nav_host_fragment);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) navHostView.getLayoutParams();
        contentTopMargin = params.topMargin;
        contentBottomMargin = params.bottomMargin;

        NavHostFragment navHost =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navHost = Objects.requireNonNull(navHost, "NavHostFragment not found");
        navController = navHost.getNavController();


        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_analysis,
                R.id.nav_list,
                R.id.nav_budget,
                R.id.nav_planning,
                R.id.nav_settings,
                R.id.nav_reports,
                R.id.nav_goals,
                R.id.nav_reminders,
                R.id.nav_imports,
                R.id.nav_perfil,
                R.id.nav_welcome,
                R.id.nav_login,
                R.id.nav_register
        ).setOpenableLayout(drawerLayout).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
        setupBottomNavigation();


        navView.setNavigationItemSelectedListener(this::onDrawerItemSelected);


        navController.addOnDestinationChangedListener((controller, destination, args) -> {
            configureSystemBars();
            int destId = destination.getId();
            boolean isAuthScreen = (destId == R.id.nav_login
                    || destId == R.id.nav_register
                    || destId == R.id.nav_welcome
                    || destId == R.id.nav_pin_lock);
            boolean isWelcomeScreen = destId == R.id.nav_welcome;
            boolean hasLocalHeader = destId == R.id.nav_home
                    || destId == R.id.nav_analysis
                    || destId == R.id.nav_settings
                    || destId == R.id.nav_planning;
            toolbar.setVisibility((isWelcomeScreen || hasLocalHeader) ? View.GONE : View.VISIBLE);
            setContentTopMargin((isWelcomeScreen || hasLocalHeader) ? 0 : contentTopMargin);
            boolean bottomVisible = isBottomDestination(destId);
            setContentBottomMargin(bottomVisible ? dp(96) : contentBottomMargin);
            if (bottomNavContainer != null) {
                bottomNavContainer.setVisibility(bottomVisible ? View.VISIBLE : View.GONE);
            }
            updateBottomSelection(destId);

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
                requestNotificationPermissionIfNeeded();
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
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            configureSystemBars();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations() && Prefs.hasPin(this)) {
            PinSession.lock();
        }
    }

    private boolean onDrawerItemSelected(@NonNull MenuItem item) {
        int destId = item.getItemId();
        if (destId == R.id.nav_home
                || destId == R.id.nav_analysis
                || destId == R.id.nav_list
                || destId == R.id.nav_budget
                || destId == R.id.nav_planning
                || destId == R.id.nav_settings
                || destId == R.id.nav_reports
                || destId == R.id.nav_goals
                || destId == R.id.nav_reminders
                || destId == R.id.nav_imports
                || destId == R.id.nav_perfil) {
            navigateAfterDrawerCloses(destId);
            return true;
        }

        if (destId == R.id.nav_logout) {
            drawerLayout.closeDrawer(GravityCompat.START);
            navView.postDelayed(() -> {

                Prefs.clearAuth(this);
                clearScopedViewModelCaches();
                LocalRepository.invalidateDataVersion();

            PinSession.lock();

            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

            NavOptions out = new NavOptions.Builder()
                    .setPopUpTo(navController.getGraph().getId(), true)
                    .build();
            navController.navigate(R.id.nav_welcome, null, out);
            }, 160L);
            return true;
        }

        return false;
    }

    private void clearScopedViewModelCaches() {
        ViewModelProvider provider = new ViewModelProvider(this);
        provider.get(HomeViewModel.class).clearCache();
        provider.get(TransactionsViewModel.class).clearCache();
        provider.get(ReportsViewModel.class).clearCache();
        provider.get(BudgetViewModel.class).clearCache();
    }

    public void switchToUser(long userId, String email, String name) {
        if (userId <= 0) return;
        Prefs.setToken(this, "local-token");
        Prefs.setUserSession(this, userId, email, name);
        LocalRepository.invalidateDataVersion();
        clearScopedViewModelCaches();
        PinSession.lock();

        Toast.makeText(this, getString(R.string.account_switch_success), Toast.LENGTH_SHORT).show();
        NavOptions opts = new NavOptions.Builder()
                .setPopUpTo(navController.getGraph().getId(), true)
                .build();
        navController.navigate(R.id.nav_home, null, opts);
    }

    private void setupBottomNavigation() {
        bottomNavContainer = findViewById(R.id.bottom_nav_container);
        bindBottomItem(R.id.bottomNavHome, R.id.nav_home);
        bindBottomItem(R.id.bottomNavAnalysis, R.id.nav_analysis);
        bindBottomItem(R.id.bottomNavNew, R.id.nav_new);
        bindBottomItem(R.id.bottomNavBudget, R.id.nav_budget);
        bindBottomItem(R.id.bottomNavPlanning, R.id.nav_planning);
    }

    private void bindBottomItem(int viewId, int destinationId) {
        View item = findViewById(viewId);
        if (item == null) return;
        item.setOnClickListener(v -> navigateFromBottom(destinationId));
    }

    private void navigateFromBottom(int destinationId) {
        NavDestination current = navController.getCurrentDestination();
        if (current != null && current.getId() == destinationId) return;

        NavOptions opts = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(R.id.nav_home, false, true)
                .build();
        navController.navigate(destinationId, null, opts);
    }

    private boolean isBottomDestination(int destId) {
        return destId == R.id.nav_home
                || destId == R.id.nav_analysis
                || destId == R.id.nav_new
                || destId == R.id.nav_budget
                || destId == R.id.nav_planning;
    }

    private void updateBottomSelection(int destId) {
        applyBottomItem(R.id.bottomIconHome, R.id.bottomLabelHome, R.id.bottomDotHome, destId == R.id.nav_home);
        applyBottomItem(R.id.bottomIconAnalysis, R.id.bottomLabelAnalysis, R.id.bottomDotAnalysis, destId == R.id.nav_analysis);
        applyBottomItem(0, 0, R.id.bottomDotNew, destId == R.id.nav_new);
        applyBottomItem(R.id.bottomIconBudget, R.id.bottomLabelBudget, R.id.bottomDotBudget, destId == R.id.nav_budget);
        applyBottomItem(R.id.bottomIconPlanning, R.id.bottomLabelPlanning, R.id.bottomDotPlanning, destId == R.id.nav_planning);
    }

    private void applyBottomItem(int iconId, int labelId, int dotId, boolean selected) {
        int color = ContextCompat.getColor(this, selected ? R.color.md_theme_primary : R.color.md_theme_onSurfaceVariant);
        if (iconId != 0) {
            ImageView icon = findViewById(iconId);
            if (icon != null) icon.setColorFilter(color);
        }
        if (labelId != 0) {
            TextView label = findViewById(labelId);
            if (label != null) {
                label.setTextColor(color);
                label.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        }
        View dot = findViewById(dotId);
        if (dot != null) dot.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
    }

    private void navigateAfterDrawerCloses(int destId) {
        NavDestination current = navController.getCurrentDestination();
        if (current != null && current.getId() == destId) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        pendingDrawerDestination = destId;
        drawerLayout.closeDrawer(GravityCompat.START);
        navView.postDelayed(() -> {
            if (pendingDrawerDestination != destId) return;
            pendingDrawerDestination = 0;
            NavOptions opts = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setPopUpTo(R.id.nav_home, false, true)
                    .build();
            navController.navigate(destId, null, opts);
        }, 160L);
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
                || destId == R.id.nav_register || destId == R.id.nav_welcome || destId == R.id.nav_pin_setup) {
            return;
        }
        navController.navigate(R.id.nav_pin_lock);
    }

    private void configureSystemBars() {
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;

        int systemBarColor = ContextCompat.getColor(this, R.color.md_theme_background);

        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        );

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        getWindow().setStatusBarColor(systemBarColor);
        getWindow().setNavigationBarColor(systemBarColor);

        // Si Samsung/Android deja la status bar transparente, esto evita que se vea una capa verde debajo.
        getWindow().getDecorView().setBackgroundColor(systemBarColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        controller.setAppearanceLightStatusBars(!night);
        controller.setAppearanceLightNavigationBars(!night);
    }

    private void applyDrawerSystemBars() {
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        int statusColor = ContextCompat.getColor(this, R.color.gradient_start);
        int navigationColor = ContextCompat.getColor(this, R.color.drawer_body_background);

        getWindow().setStatusBarColor(statusColor);
        getWindow().setNavigationBarColor(navigationColor);
        getWindow().getDecorView().setBackgroundColor(navigationColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(!night);
    }

    private void setContentTopMargin(int topMargin) {
        if (navHostView == null) return;
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) navHostView.getLayoutParams();
        if (params.topMargin == topMargin) return;
        params.topMargin = topMargin;
        navHostView.setLayoutParams(params);
    }

    private void setContentBottomMargin(int bottomMargin) {
        if (navHostView == null) return;
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) navHostView.getLayoutParams();
        if (params.bottomMargin == bottomMargin) return;
        params.bottomMargin = bottomMargin;
        navHostView.setLayoutParams(params);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_POST_NOTIFICATIONS
        );
    }
}
