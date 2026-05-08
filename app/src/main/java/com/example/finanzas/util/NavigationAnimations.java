package com.example.finanzas.util;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.navigation.NavOptions;

import com.example.finanzas.R;

public final class NavigationAnimations {

    private NavigationAnimations() {
    }

    @NonNull
    public static NavOptions mainSection(@IdRes int popUpToDestination) {
        return withMainFade(new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(popUpToDestination, false, true))
                .build();
    }

    @NonNull
    public static NavOptions mainFade() {
        return withMainFade(new NavOptions.Builder()).build();
    }

    @NonNull
    public static NavOptions detailSlide() {
        return new NavOptions.Builder()
                .setEnterAnim(R.anim.nav_slide_in_right)
                .setExitAnim(R.anim.nav_slide_out_left)
                .setPopEnterAnim(R.anim.nav_slide_in_left)
                .setPopExitAnim(R.anim.nav_slide_out_right)
                .build();
    }

    @NonNull
    public static NavOptions.Builder withMainFade(@NonNull NavOptions.Builder builder) {
        return builder
                .setEnterAnim(R.anim.nav_fade_in)
                .setExitAnim(R.anim.nav_fade_out)
                .setPopEnterAnim(R.anim.nav_fade_in)
                .setPopExitAnim(R.anim.nav_fade_out);
    }
}
