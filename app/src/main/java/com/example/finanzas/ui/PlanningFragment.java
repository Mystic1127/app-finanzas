package com.example.finanzas.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.finanzas.R;
import com.example.finanzas.ui.view.SpendlyDecorBackgroundDrawable;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class PlanningFragment extends Fragment {
    private MaterialButton btnGoals;
    private MaterialButton btnReminders;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_planning, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackground(new SpendlyDecorBackgroundDrawable(requireContext()));
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.togglePlanning);
        btnGoals = view.findViewById(R.id.btnPlanningGoals);
        btnReminders = view.findViewById(R.id.btnPlanningReminders);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            showTab(checkedId == R.id.btnPlanningReminders);
        });
        toggle.check(R.id.btnPlanningGoals);
        showTab(false);
    }

    private void showTab(boolean reminders) {
        style(btnGoals, !reminders);
        style(btnReminders, reminders);
        Fragment fragment = reminders ? new RemindersFragment() : new GoalsFragment();
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.planningContainer, fragment)
                .commit();
    }

    private void style(@Nullable MaterialButton button, boolean selected) {
        if (button == null || getContext() == null) return;
        int bg = ContextCompat.getColor(requireContext(), selected ? R.color.md_theme_primary : R.color.md_theme_surface);
        int fg = ContextCompat.getColor(requireContext(), selected ? R.color.md_theme_onPrimary : R.color.md_theme_onSurface);
        int stroke = ContextCompat.getColor(requireContext(), selected ? R.color.md_theme_primary : R.color.md_theme_outline);
        button.setBackgroundTintList(ColorStateList.valueOf(bg));
        button.setTextColor(fg);
        button.setIconTint(ColorStateList.valueOf(fg));
        button.setStrokeColor(ColorStateList.valueOf(stroke));
    }
}
