package com.example.finanzas.data.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.finanzas.data.local.LocalRepository;
import com.example.finanzas.data.model.FinancialAccount;
import com.example.finanzas.util.CurrencyConverter;
import com.example.finanzas.util.Prefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsService {

    public interface SaveCb { void onSuccess(); void onFail(); }

    private static final String PREFS = "finanzas_settings";
    private static final String KEY_DASHBOARD_LEGACY = "dashboard_prefs";
    private static final String KEY_TRAVEL_LEGACY = "travel_prefs";
    private static final String KEY_DASHBOARD_PREFIX = "dashboard_prefs_user_";
    private static final String KEY_TRAVEL_PREFIX = "travel_prefs_user_";
    private static final String KEY_INITIAL_BALANCES_PREFIX = "initial_balances_user_";
    private static final String KEY_FINANCIAL_ACCOUNTS_PREFIX = "financial_accounts_user_";
    private static final String KEY_LAST_TRANSACTION_ACCOUNT_PREFIX = "last_transaction_account_user_";
    private static final String KEY_LAST_TRANSACTION_DESTINATION_PREFIX = "last_transaction_destination_user_";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_THEME_MODE_PREFIX = "theme_mode_user_";
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    public static void save(Context ctx, boolean notificationsEnabled, SaveCb cb) {
        cb.onSuccess();
    }

    public static String getDashboardRaw(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return getRawForUser(sp, dashboardKey(currentUserId(ctx)), KEY_DASHBOARD_LEGACY);
    }

    public static String getTravelRaw(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return getRawForUser(sp, travelKey(currentUserId(ctx)), KEY_TRAVEL_LEGACY);
    }

    public static String getCurrencyCode(Context ctx) {
        try {
            JSONObject body = new JSONObject(getTravelRaw(ctx));
            String code = body.optString("currency", body.optString("base", "PEN"));
            return normalizeCurrency(code);
        } catch (Exception e) {
            return "PEN";
        }
    }

    public static String getCurrencySymbol(Context ctx) {
        return CurrencyConverter.symbol(getCurrencyCode(ctx));
    }

    public static String getCurrencySymbol(String currencyCode) {
        return CurrencyConverter.symbol(currencyCode);
    }

    public static boolean hasCurrencyConfigured(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String raw = getRawForUser(sp, travelKey(currentUserId(ctx)), KEY_TRAVEL_LEGACY);
            JSONObject body = new JSONObject(raw);
            String code = body.optString("currency", body.optString("base", ""));
            return !code.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public static double getManualRate(Context ctx) {
        try {
            JSONObject body = new JSONObject(getTravelRaw(ctx));
            return Math.max(0.0, body.optDouble("rate", 0.0));
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static double getInitialCashBalance(Context ctx) {
        return getInitialBalanceRaw(ctx, "cash");
    }

    public static double getInitialCardBalance(Context ctx) {
        return getInitialBalanceRaw(ctx, "card");
    }

    public static String getInitialBalancesCurrency(Context ctx) {
        try {
            JSONObject body = new JSONObject(getInitialBalancesRaw(ctx));
            return normalizeCurrency(body.optString("currency", getCurrencyCode(ctx)));
        } catch (Exception e) {
            return getCurrencyCode(ctx);
        }
    }

    public static String getInitialBalancesRaw(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = sp.getString(initialBalancesKey(currentUserId(ctx)), null);
        return raw != null ? raw : "{}";
    }

    public static boolean isInitialBalanceConfigured(Context ctx) {
        try {
            JSONObject body = new JSONObject(getInitialBalancesRaw(ctx));
            return body.optBoolean("configured", false);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasLegacyInitialBalances(Context ctx) {
        return getInitialCashBalance(ctx) > 0.0 || getInitialCardBalance(ctx) > 0.0;
    }

    public static void markInitialBalanceConfigured(Context ctx, String currencyCode) {
        try {
            JSONObject body = new JSONObject();
            body.put("configured", true);
            body.put("currency", normalizeCurrency(currencyCode == null || currencyCode.trim().isEmpty() ? getCurrencyCode(ctx) : currencyCode));
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(initialBalancesKey(currentUserId(ctx)), body.toString()).apply();
            LocalRepository.invalidateDataVersion();
        } catch (Exception ignored) {
        }
    }

    public static void clearInitialBalances(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().remove(initialBalancesKey(currentUserId(ctx))).apply();
            LocalRepository.invalidateDataVersion();
        } catch (Exception ignored) {
        }
    }

    public static String normalizeAccountType(String value) {
        if (value == null) return "CARD";
        String clean = value.trim();
        if (clean.isEmpty()) return "CARD";
        String upper = clean.toUpperCase(Locale.ROOT);
        if ("CASH".equals(upper) || "EFECTIVO".equals(upper)) return "CASH";
        if ("CARD".equals(upper) || "TARJETA".equals(upper) || "TARJETA/CUENTA".equals(upper)) return "CARD";
        return upper.replaceAll("[^A-Z0-9_:-]", "_");
    }

    public static List<FinancialAccount> listFinancialAccounts(Context ctx) {
        ArrayList<FinancialAccount> out = new ArrayList<>();
        try {
            JSONObject body = new JSONObject(getFinancialAccountsRaw(ctx));
            JSONArray accounts = body.optJSONArray("accounts");
            if (accounts == null) return out;
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject item = accounts.optJSONObject(i);
                if (item == null) continue;
                String id = normalizeAccountType(item.optString("id", ""));
                String name = item.optString("name", "").trim();
                if (id.isEmpty() || "CASH".equals(id) || "CARD".equals(id) || name.isEmpty()) continue;
                out.add(new FinancialAccount(
                        id,
                        name,
                        item.optLong("createdAt", 0L),
                        cleanLast4(item.optString("last4", "")),
                        item.optBoolean("includedInTotal", true),
                        item.optBoolean("visibleInHome", false),
                        true
                ));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static List<FinancialAccount> listCardAccounts(Context ctx) {
        ArrayList<FinancialAccount> out = new ArrayList<>();
        out.add(defaultCardAccount(ctx));
        out.addAll(listFinancialAccounts(ctx));
        ensureSingleVisibleCard(ctx, out);
        return out;
    }

    public static FinancialAccount defaultCardAccount(Context ctx) {
        try {
            JSONObject body = new JSONObject(getFinancialAccountsRaw(ctx));
            JSONObject card = body.optJSONObject("defaultCard");
            String last4 = card != null && card.optBoolean("last4Configured", false)
                    ? cleanLast4(card.optString("last4", ""))
                    : "";
            return new FinancialAccount(
                    "CARD",
                    card != null ? card.optString("name", "Tarjeta predeterminada") : "Tarjeta predeterminada",
                    0L,
                    last4,
                    card == null || card.optBoolean("includedInTotal", true),
                    card == null || card.optBoolean("visibleInHome", true),
                    false
            );
        } catch (Exception e) {
            return new FinancialAccount("CARD", "Tarjeta predeterminada", 0L, "", true, true, false);
        }
    }

    public static FinancialAccount getVisibleCardAccount(Context ctx) {
        List<FinancialAccount> cards = listCardAccounts(ctx);
        for (FinancialAccount account : cards) {
            if (account.isVisibleInHome()) return account;
        }
        return cards.isEmpty() ? defaultCardAccount(ctx) : cards.get(0);
    }

    public static String getLastTransactionAccountType(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return normalizeAccountType(sp.getString(lastTransactionAccountKey(currentUserId(ctx)), "CARD"));
        } catch (Exception e) {
            return "CARD";
        }
    }

    public static void setLastTransactionAccountType(Context ctx, String accountType) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit()
                    .putString(lastTransactionAccountKey(currentUserId(ctx)), normalizeAccountType(accountType))
                    .apply();
        } catch (Exception ignored) {
        }
    }

    public static String getLastTransactionDestinationAccountType(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return normalizeAccountType(sp.getString(lastTransactionDestinationKey(currentUserId(ctx)), "CASH"));
        } catch (Exception e) {
            return "CASH";
        }
    }

    public static void setLastTransactionDestinationAccountType(Context ctx, String accountType) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit()
                    .putString(lastTransactionDestinationKey(currentUserId(ctx)), normalizeAccountType(accountType))
                    .apply();
        } catch (Exception ignored) {
        }
    }

    public static int countIncludedCardAccounts(Context ctx) {
        int count = 0;
        for (FinancialAccount account : listCardAccounts(ctx)) {
            if (account.isIncludedInTotal()) count++;
        }
        return count;
    }

    public static boolean isCardIncludedInTotal(Context ctx, String accountId) {
        String normalized = normalizeAccountType(accountId);
        for (FinancialAccount account : listCardAccounts(ctx)) {
            if (normalizeAccountType(account.getId()).equals(normalized)) return account.isIncludedInTotal();
        }
        return true;
    }

    public static void setCardIncludedInTotal(Context ctx, String accountId, boolean included) {
        updateCardMetadata(ctx, accountId, item -> item.put("includedInTotal", included));
    }

    public static void setVisibleCardAccount(Context ctx, String accountId) {
        String visibleId = normalizeAccountType(accountId);
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONObject body = new JSONObject(getFinancialAccountsRaw(ctx));
            JSONObject defaultCard = body.optJSONObject("defaultCard");
            if (defaultCard == null) defaultCard = new JSONObject();
            defaultCard.put("name", defaultCard.optString("name", "Tarjeta predeterminada"));
            defaultCard.put("last4", cleanLast4(defaultCard.optString("last4", "")));
            defaultCard.put("includedInTotal", "CARD".equals(visibleId) || defaultCard.optBoolean("includedInTotal", true));
            defaultCard.put("visibleInHome", "CARD".equals(visibleId));
            body.put("defaultCard", defaultCard);

            JSONArray accounts = body.optJSONArray("accounts");
            if (accounts != null) {
                for (int i = 0; i < accounts.length(); i++) {
                    JSONObject item = accounts.optJSONObject(i);
                    if (item == null) continue;
                    boolean visible = normalizeAccountType(item.optString("id", "")).equals(visibleId);
                    item.put("visibleInHome", visible);
                    if (visible) item.put("includedInTotal", true);
                }
            }
            sp.edit().putString(financialAccountsKey(currentUserId(ctx)), body.toString()).apply();
            LocalRepository.invalidateDataVersion();
        } catch (Exception ignored) {
        }
    }

    public static void deleteFinancialAccount(Context ctx, String accountId) {
        String normalized = normalizeAccountType(accountId);
        if ("CARD".equals(normalized) || "CASH".equals(normalized)) return;
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONObject body = new JSONObject(getFinancialAccountsRaw(ctx));
            JSONArray current = body.optJSONArray("accounts");
            JSONArray kept = new JSONArray();
            boolean removedVisible = false;
            if (current != null) {
                for (int i = 0; i < current.length(); i++) {
                    JSONObject item = current.optJSONObject(i);
                    if (item == null) continue;
                    if (normalizeAccountType(item.optString("id", "")).equals(normalized)) {
                        removedVisible = item.optBoolean("visibleInHome", false);
                    } else {
                        kept.put(item);
                    }
                }
            }
            body.put("accounts", kept);
            sp.edit().putString(financialAccountsKey(currentUserId(ctx)), body.toString()).apply();
            if (removedVisible) setVisibleCardAccount(ctx, "CARD");
            LocalRepository.invalidateDataVersion();
        } catch (Exception ignored) {
        }
    }

    public static FinancialAccount addFinancialAccount(Context ctx, String rawName) {
        return addFinancialAccount(ctx, rawName, "");
    }

    public static FinancialAccount addFinancialAccount(Context ctx, String rawName, String rawLast4) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Nombre de cuenta requerido");
        }
        name = limitCardName(name);
        long now = System.currentTimeMillis();
        String id = "ACCOUNT_" + now;
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONObject body = new JSONObject(getFinancialAccountsRaw(ctx));
            JSONArray current = body.optJSONArray("accounts");
            JSONArray accounts = current == null ? new JSONArray() : current;
            JSONObject item = new JSONObject();
            item.put("id", id);
            item.put("name", name);
            item.put("last4", cleanLast4(rawLast4));
            item.put("includedInTotal", true);
            item.put("visibleInHome", false);
            item.put("createdAt", now);
            accounts.put(item);
            body.put("accounts", accounts);
            sp.edit().putString(financialAccountsKey(currentUserId(ctx)), body.toString()).apply();
            LocalRepository.invalidateDataVersion();
            return new FinancialAccount(id, name, now, cleanLast4(rawLast4), true, false, true);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo guardar la cuenta", e);
        }
    }

    public static void updateCardDetails(Context ctx, String accountId, String rawName, String rawLast4) {
        String normalized = normalizeAccountType(accountId);
        String name = limitCardName(rawName == null ? "" : rawName.trim());
        String last4 = cleanLast4(rawLast4);
        if (name.isEmpty() && !"CARD".equals(normalized)) {
            throw new IllegalArgumentException("Nombre de cuenta requerido");
        }
        updateCardMetadata(ctx, normalized, item -> {
            item.put("name", name.isEmpty() ? "Tarjeta predeterminada" : name);
            item.put("last4", last4);
            if ("CARD".equals(normalized)) item.put("last4Configured", !last4.isEmpty());
        });
    }

    public static void clearFinancialAccounts(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().remove(financialAccountsKey(currentUserId(ctx))).apply();
            LocalRepository.invalidateDataVersion();
        } catch (Exception ignored) {
        }
    }

    public static String getFinancialAccountName(Context ctx, String accountType) {
        String normalized = normalizeAccountType(accountType);
        if ("CASH".equals(normalized)) return "Efectivo";
        if ("CARD".equals(normalized)) return defaultCardAccount(ctx).getName();
        for (FinancialAccount account : listFinancialAccounts(ctx)) {
            if (normalized.equals(account.getId())) {
                return account.getName();
            }
        }
        return "Cuenta";
    }

    public static String getFinancialAccountsRaw(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = sp.getString(financialAccountsKey(currentUserId(ctx)), null);
        return raw != null ? raw : "{}";
    }

    public static void saveInitialBalances(Context ctx, double cashBalance, double cardBalance, String currencyCode, SaveCb cb) {
        try {
            if (!isValidAmount(cashBalance) || !isValidAmount(cardBalance) || (cashBalance <= 0.0 && cardBalance <= 0.0)) {
                cb.onFail();
                return;
            }
            JSONObject body = new JSONObject();
            body.put("cash", cashBalance);
            body.put("card", cardBalance);
            body.put("currency", normalizeCurrency(currencyCode == null || currencyCode.trim().isEmpty() ? getCurrencyCode(ctx) : currencyCode));
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(initialBalancesKey(currentUserId(ctx)), body.toString()).apply();
            LocalRepository.invalidateDataVersion();
            cb.onSuccess();
        } catch (Exception e) {
            cb.onFail();
        }
    }

    public static void saveDashboard(Context ctx, JSONObject body, SaveCb cb) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(dashboardKey(currentUserId(ctx)), body != null ? body.toString() : "{}").apply();
            cb.onSuccess();
        } catch (Exception e) {
            cb.onFail();
        }
    }

    public static void saveTravel(Context ctx, JSONObject body, SaveCb cb) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(travelKey(currentUserId(ctx)), body != null ? body.toString() : "{}").apply();
            LocalRepository.invalidateDataVersion();
            cb.onSuccess();
        } catch (Exception e) {
            cb.onFail();
        }
    }

    public static void saveCurrency(Context ctx, String currencyCode, double manualRate, SaveCb cb) {
        try {
            JSONObject body;
            try {
                body = new JSONObject(getTravelRaw(ctx));
            } catch (Exception e) {
                body = new JSONObject();
            }
            String code = normalizeCurrency(currencyCode);
            body.put("enabled", false);
            body.put("base", code);
            body.put("currency", code);
            body.put("rate", Math.max(0.0, manualRate));
            saveTravel(ctx, body, cb);
        } catch (Exception e) {
            cb.onFail();
        }
    }

    public static JSONObject exportSyncSettings(Context ctx) {
        JSONObject body = new JSONObject();
        try {
            long userId = currentUserId(ctx);
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            body.put("dashboard", getRawForUser(sp, dashboardKey(userId), KEY_DASHBOARD_LEGACY));
            body.put("travel", getRawForUser(sp, travelKey(userId), KEY_TRAVEL_LEGACY));
            body.put("initialBalances", sp.getString(initialBalancesKey(userId), "{}"));
            body.put("financialAccounts", sp.getString(financialAccountsKey(userId), "{}"));
            body.put("lastTransactionAccount", sp.getString(lastTransactionAccountKey(userId), "CARD"));
            body.put("lastTransactionDestination", sp.getString(lastTransactionDestinationKey(userId), "CASH"));
            body.put("themeMode", getThemeMode(ctx));
        } catch (Exception ignored) {
        }
        return body;
    }

    public static void importSyncSettings(Context ctx, JSONObject body) {
        if (body == null) return;
        try {
            long userId = currentUserId(ctx);
            SharedPreferences.Editor editor = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
            editor.putString(dashboardKey(userId), body.optString("dashboard", "{}"));
            editor.putString(travelKey(userId), body.optString("travel", "{}"));
            editor.putString(initialBalancesKey(userId), body.optString("initialBalances", "{}"));
            editor.putString(financialAccountsKey(userId), body.optString("financialAccounts", "{}"));
            editor.putString(lastTransactionAccountKey(userId), normalizeAccountType(body.optString("lastTransactionAccount", "CARD")));
            editor.putString(lastTransactionDestinationKey(userId), normalizeAccountType(body.optString("lastTransactionDestination", "CASH")));
            editor.putString(themeModeKey(userId), normalizeThemeMode(body.optString("themeMode", THEME_SYSTEM)));
            editor.apply();
            applyThemeMode(ctx);
        } catch (Exception ignored) {
        }
    }

    private static long currentUserId(Context ctx) {
        long userId = Prefs.getCurrentUserId(ctx.getApplicationContext());
        if (userId <= 0) {
            throw new IllegalStateException("No hay usuario autenticado");
        }
        return userId;
    }

    private static String dashboardKey(long userId) {
        return KEY_DASHBOARD_PREFIX + userId;
    }

    private static String travelKey(long userId) {
        return KEY_TRAVEL_PREFIX + userId;
    }

    private static String initialBalancesKey(long userId) {
        return KEY_INITIAL_BALANCES_PREFIX + userId;
    }

    private static String financialAccountsKey(long userId) {
        return KEY_FINANCIAL_ACCOUNTS_PREFIX + userId;
    }

    private static String lastTransactionAccountKey(long userId) {
        return KEY_LAST_TRANSACTION_ACCOUNT_PREFIX + userId;
    }

    private static String lastTransactionDestinationKey(long userId) {
        return KEY_LAST_TRANSACTION_DESTINATION_PREFIX + userId;
    }

    private interface JsonUpdater {
        void update(JSONObject object) throws Exception;
    }

    private static void updateCardMetadata(Context ctx, String accountId, JsonUpdater updater) {
        String normalized = normalizeAccountType(accountId);
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONObject body = new JSONObject(getFinancialAccountsRaw(ctx));
            if ("CARD".equals(normalized)) {
                JSONObject card = body.optJSONObject("defaultCard");
                if (card == null) card = new JSONObject();
                card.put("name", card.optString("name", "Tarjeta predeterminada"));
                card.put("last4", cleanLast4(card.optString("last4", "")));
                card.put("includedInTotal", card.optBoolean("includedInTotal", true));
                card.put("visibleInHome", card.optBoolean("visibleInHome", true));
                updater.update(card);
                body.put("defaultCard", card);
            } else {
                JSONArray accounts = body.optJSONArray("accounts");
                if (accounts == null) accounts = new JSONArray();
                for (int i = 0; i < accounts.length(); i++) {
                    JSONObject item = accounts.optJSONObject(i);
                    if (item != null && normalizeAccountType(item.optString("id", "")).equals(normalized)) {
                        updater.update(item);
                        break;
                    }
                }
                body.put("accounts", accounts);
            }
            sp.edit().putString(financialAccountsKey(currentUserId(ctx)), body.toString()).apply();
            LocalRepository.invalidateDataVersion();
        } catch (Exception ignored) {
        }
    }

    private static void ensureSingleVisibleCard(Context ctx, List<FinancialAccount> cards) {
        if (cards == null || cards.isEmpty()) return;
        int visible = 0;
        FinancialAccount firstVisible = null;
        for (FinancialAccount account : cards) {
            if (account.isVisibleInHome()) {
                visible++;
                if (firstVisible == null) firstVisible = account;
            }
        }
        if (visible == 1) return;
        setVisibleCardAccount(ctx, firstVisible != null ? firstVisible.getId() : cards.get(0).getId());
    }

    private static String cleanLast4(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("[^0-9]", "");
        if (digits.length() >= 4) return digits.substring(digits.length() - 4);
        return digits;
    }

    private static String limitCardName(String raw) {
        String clean = raw == null ? "" : raw.trim();
        return clean.length() > 7 ? clean.substring(0, 7) : clean;
    }

    private static double getInitialBalanceRaw(Context ctx, String key) {
        try {
            JSONObject body = new JSONObject(getInitialBalancesRaw(ctx));
            double value = body.optDouble(key, 0.0);
            return isValidAmount(value) ? value : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static void prepareCurrencySetupForNewUser(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(travelKey(currentUserId(ctx)), "{}").apply();
        } catch (Exception ignored) {
        }
    }

    public static String getThemeMode(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long userId = safeCurrentUserId(ctx);
        String mode = userId > 0
                ? sp.getString(themeModeKey(userId), sp.getString(KEY_THEME_MODE, THEME_SYSTEM))
                : sp.getString(KEY_THEME_MODE, THEME_SYSTEM);
        return normalizeThemeMode(mode);
    }

    public static void saveThemeMode(Context ctx, String mode) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        long userId = safeCurrentUserId(ctx);
        if (userId > 0) {
            editor.putString(themeModeKey(userId), normalizeThemeMode(mode));
        } else {
            editor.putString(KEY_THEME_MODE, normalizeThemeMode(mode));
        }
        editor.apply();
    }

    public static void applyThemeMode(Context ctx) {
        AppCompatDelegate.setDefaultNightMode(appCompatNightMode(getThemeMode(ctx)));
    }

    private static boolean isValidAmount(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0.0;
    }

    private static String getRawForUser(SharedPreferences sp, String userKey, String legacyKey) {
        String userValue = sp.getString(userKey, null);
        if (userValue != null) {
            return userValue;
        }

        String legacyValue = sp.getString(legacyKey, null);
        if (legacyValue != null) {
            sp.edit()
                    .putString(userKey, legacyValue)
                    .remove(legacyKey)
                    .apply();
            return legacyValue;
        }

        return "{}";
    }

    private static long safeCurrentUserId(Context ctx) {
        try {
            return Prefs.getCurrentUserId(ctx.getApplicationContext());
        } catch (Exception e) {
            return -1L;
        }
    }

    private static String themeModeKey(long userId) {
        return KEY_THEME_MODE_PREFIX + userId;
    }

    private static String normalizeCurrency(String code) {
        return CurrencyConverter.normalize(code);
    }

    private static String normalizeThemeMode(String mode) {
        if (THEME_LIGHT.equals(mode)) return THEME_LIGHT;
        if (THEME_DARK.equals(mode)) return THEME_DARK;
        return THEME_SYSTEM;
    }

    private static int appCompatNightMode(String mode) {
        if (THEME_LIGHT.equals(mode)) return AppCompatDelegate.MODE_NIGHT_NO;
        if (THEME_DARK.equals(mode)) return AppCompatDelegate.MODE_NIGHT_YES;
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }
}
