package com.example.finanzas.data.api;

public class ApiConfig {
    public static final String BASE_URL = "http://10.0.2.2/finanzas_api/";

    public static final String TRANS_LIST   = "transacciones/list.php";
    public static final String TRANS_CREATE = "transacciones/create.php";
    public static final String TRANS_UPDATE = "transacciones/update.php";
    public static final String TRANS_DELETE = "transacciones/delete.php";
    public static final String TRANS_SUGGEST = "transacciones/sugerir_categoria.php";

    public static final String CATS_LIST    = "categorias/list.php";

    public static final String AUTH_LOGIN   = "auth/login.php";
    public static final String AUTH_REGISTER = "auth/register.php";
    public static final String AUTH_SET_ROLE = "auth/set_role.php";

    public static final String PRES_GET = "presupuestos/get.php";
    public static final String PRES_SET = "presupuestos/set.php";
    public static final String PRES_CAT_LIST = "presupuestos/categorias/list.php";
    public static final String PRES_CAT_SET  = "presupuestos/categorias/set.php";

    public static final String AUTH_CHANGE_PASS = "auth/change_password.php";
    public static final String USER_ME          = "auth/me.php";

    public static final String DASHBOARD_SUMMARY = "dashboard/summary.php";

    public static final String GOALS_LIST   = "metas/list.php";
    public static final String GOALS_SAVE   = "metas/save.php";
    public static final String GOALS_DELETE = "metas/delete.php";
    public static final String GOALS_MILESTONE_SAVE   = "metas/hitos/save.php";
    public static final String GOALS_MILESTONE_DELETE = "metas/hitos/delete.php";

    public static final String REMINDERS_LIST     = "recordatorios/list.php";
    public static final String REMINDERS_SAVE     = "recordatorios/save.php";
    public static final String REMINDERS_COMPLETE = "recordatorios/complete.php";
    public static final String REMINDERS_DELETE   = "recordatorios/delete.php";
    public static final String SETTINGS_DASHBOARD_SAVE = "settings/dashboard_save.php";
    public static final String SETTINGS_TRAVEL_SAVE    = "settings/travel_save.php";

    public static final String IMPORT_CREATE  = "importaciones/create.php";
    public static final String IMPORT_LIST    = "importaciones/list.php";
    public static final String IMPORT_PROCESS = "importaciones/process.php";
    public static final String IMPORT_RULES_LIST   = "importaciones/reglas/list.php";
    public static final String IMPORT_RULES_SAVE   = "importaciones/reglas/save.php";
    public static final String IMPORT_RULES_DELETE = "importaciones/reglas/delete.php";

    public static final String HOGARES_LIST   = "hogares/list.php";
    public static final String HOGARES_CREATE = "hogares/create.php";
    public static final String HOGARES_INVITE = "hogares/invite.php";
    public static final String HOGARES_JOIN   = "hogares/join.php";
    public static final String HOGARES_REMOVE = "hogares/remove.php";


    public static String TOKEN = null;
}

