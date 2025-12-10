package com.example.finanzas.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.finanzas.R;
import com.example.finanzas.data.api.HouseholdService;
import com.example.finanzas.data.model.HouseholdSummary;
import com.example.finanzas.ui.adapter.HouseholdSummaryAdapter;
import com.example.finanzas.util.Format;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class HouseholdsFragment extends Fragment {

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private TextView tvEmpty;
    private HouseholdSummaryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_households, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipe = view.findViewById(R.id.swipeHouseholds);
        tvEmpty = view.findViewById(R.id.tvHouseholdsEmpty);
        androidx.recyclerview.widget.RecyclerView rv = view.findViewById(R.id.rvHouseholds);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HouseholdSummaryAdapter();
        adapter.setListener(this::mostrarDetalle);
        rv.setAdapter(adapter);

        swipe.setOnRefreshListener(this::cargarHogares);
        view.findViewById(R.id.btnCreateHousehold).setOnClickListener(v -> mostrarDialogoCrear());
        view.findViewById(R.id.btnJoinHousehold).setOnClickListener(v -> mostrarDialogoUnirse());

        cargarHogares();
    }

    private void cargarHogares() {
        swipe.setRefreshing(true);
        Calendar cal = Calendar.getInstance();
        int anio = cal.get(Calendar.YEAR);
        int mes = cal.get(Calendar.MONTH) + 1;
        HouseholdService.list(requireContext(), anio, mes, new HouseholdService.ListCallback() {
            @Override
            public void onSuccess(List<HouseholdSummary> hogares) {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                adapter.setItems(hogares);
                tvEmpty.setVisibility(hogares == null || hogares.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError() {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                Toast.makeText(requireContext(), R.string.household_list_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoCrear() {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_simple_text, null, false);
        TextInputLayout til = content.findViewById(R.id.tilSimple);
        TextInputEditText et = content.findViewById(R.id.etSimple);
        til.setHint(getString(R.string.household_create_hint));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.household_create_title)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.household_create, null)
                .create();

        dialog.setOnShowListener(dlg -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                til.setError(null);
                String nombre = et.getText() == null ? "" : et.getText().toString().trim();
                if (nombre.isEmpty()) {
                    til.setError(getString(R.string.household_error_name));
                    return;
                }
                positive.setEnabled(false);
                HouseholdService.create(requireContext(), nombre, new HouseholdService.CreateCallback() {
                    @Override
                    public void onSuccess(int hogarId, String codigoInvite) {
                        if (!isAdded()) return;
                        positive.setEnabled(true);
                        dialog.dismiss();
                        mostrarCodigo(codigoInvite);
                        cargarHogares();
                    }

                    @Override
                    public void onError() {
                        if (!isAdded()) return;
                        positive.setEnabled(true);
                        Toast.makeText(requireContext(), R.string.household_create_error, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        dialog.show();
    }

    private void mostrarDialogoUnirse() {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_join_household, null, false);
        TextInputEditText etCodigo = content.findViewById(R.id.etJoinCode);
        MaterialAutoCompleteTextView actRol = content.findViewById(R.id.actJoinRole);
        String[] roleLabels = getResources().getStringArray(R.array.household_role_labels);
        String[] roleValues = getResources().getStringArray(R.array.household_role_values);
        ArrayAdapter<String> adapterRoles = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, Arrays.asList(roleLabels));
        actRol.setAdapter(adapterRoles);
        actRol.setText(roleLabels.length > 0 ? roleLabels[0] : "", false);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.household_join_title)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.household_join, null)
                .create();

        dialog.setOnShowListener(dlg -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                etCodigo.setError(null);
                String codigo = etCodigo.getText() == null ? "" : etCodigo.getText().toString().trim();
                String rolSeleccionado = actRol.getText() == null ? "" : actRol.getText().toString().trim();
                String rol = "viewer";
                for (int i = 0; i < roleLabels.length && i < roleValues.length; i++) {
                    if (rolSeleccionado.equalsIgnoreCase(roleLabels[i])) {
                        rol = roleValues[i];
                        break;
                    }
                }
                if (codigo.isEmpty()) {
                    etCodigo.setError(getString(R.string.household_join_code_error));
                    return;
                }
                positive.setEnabled(false);
                HouseholdService.join(requireContext(), codigo, rol, new HouseholdService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        positive.setEnabled(true);
                        dialog.dismiss();
                        Toast.makeText(requireContext(), R.string.household_join_ok, Toast.LENGTH_SHORT).show();
                        cargarHogares();
                    }

                    @Override
                    public void onError() {
                        if (!isAdded()) return;
                        positive.setEnabled(true);
                        Toast.makeText(requireContext(), R.string.household_join_error, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        dialog.show();
    }

    private void mostrarDetalle(HouseholdSummary summary) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.household_detail_totals,
                summary.getMiembros(),
                summary.getRol(),
                summary.getNombre()))
                .append('\n')
                .append(getString(R.string.household_totals_label,
                        Format.money(summary.getIngresos()),
                        Format.money(summary.getGastos()),
                        Format.money(summary.getSaldo())));
        if (summary.getDetalle() != null && !summary.getDetalle().isEmpty()) {
            sb.append("\n\n");
            for (HouseholdSummary.HouseholdMember miembro : summary.getDetalle()) {
                sb.append(miembro.getNombre())
                        .append(" – ")
                        .append(getString(R.string.household_member_entry,
                                miembro.getRol(),
                                Format.money(miembro.getIngresos()),
                                Format.money(miembro.getGastos()),
                                Format.money(miembro.getSaldo())))
                        .append('\n');
            }
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(summary.getNombre())
                .setMessage(sb.toString())
                .setPositiveButton(android.R.string.ok, null);

        if (!"viewer".equalsIgnoreCase(summary.getRol())) {
            builder.setNegativeButton(R.string.household_invite, (dialog, which) ->
                    HouseholdService.invite(requireContext(), summary.getId(), new HouseholdService.InviteCallback() {
                        @Override
                        public void onSuccess(String codigoInvite) {
                            if (!isAdded()) return;
                            mostrarCodigo(codigoInvite);
                        }

                        @Override
                        public void onError() {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), R.string.household_invite_error, Toast.LENGTH_SHORT).show();
                        }
                    }));
        }

        builder.show();
    }

    private void mostrarCodigo(String codigo) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.household_code_title)
                .setMessage(getString(R.string.household_code_message, codigo))
                .setPositiveButton(R.string.household_code_copy, (dialog, which) -> copiarAlPortapapeles(codigo))
                .setNegativeButton(android.R.string.ok, null)
                .show();
    }

    private void copiarAlPortapapeles(String codigo) {
        ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("invite", codigo));
            Toast.makeText(requireContext(), R.string.household_code_copied, Toast.LENGTH_SHORT).show();
        }
    }
}
