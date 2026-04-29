# Revisión técnica completa — app-finanzas

Fecha: 2026-04-28

## 1) Arquitectura

### Hallazgos
- No hay implementación de MVVM real: los `Fragment` contienen lógica de UI + reglas de negocio + acceso a datos mediante servicios estáticos.
- `LocalRepository` concentra demasiadas responsabilidades (auth, transacciones, presupuestos, metas, recordatorios, importaciones, dashboard).
- La capa `data/api` mezcla "servicios API" con wrappers locales en memoria/SQLite, lo que hace difuso el límite de capas.

### Cambios concretos recomendados
1. Introducir `ViewModel` por pantalla (`HomeViewModel`, `TransactionsViewModel`, etc.) con `StateFlow`/`LiveData`.
2. Dividir `LocalRepository` en repositorios por dominio (`AuthRepository`, `TransactionRepository`, `BudgetRepository`, `GoalRepository`, etc.).
3. Crear casos de uso para reglas de negocio (simulación, predicción de gasto, alertas, importación).
4. Unificar acceso a datos con interfaces e inyección de dependencias (Hilt/Koin) para desacoplar UI de infraestructura.

---

## 2) Código

### Hallazgos
- `HomeFragment` y otros fragments son muy grandes (muchas referencias de view y mucha lógica acoplada).
- Uso repetido de `Toast` y strings hardcodeadas en Java en vez de `strings.xml`.
- Operaciones de DB hechas de forma síncrona en hilo principal a través de servicios.
- Manejo de errores genérico, sin logging estructurado ni mensajes diferenciados por causa.

### Posibles bugs/crashes
- Riesgo de ANR/jank al consultar o escribir SQLite desde UI thread.
- `onUpgrade` destruye todas las tablas (pérdida total de datos al subir versión de BD).
- `UserService.changePassword` construye JSON por concatenación de strings (se rompe con comillas/caracteres especiales).

### Cambios concretos recomendados
1. Migrar strings hardcodeadas a recursos y aplicar `@StringRes`.
2. Ejecutar I/O con corrutinas (`Dispatchers.IO`) o `Executor` + callback main thread.
3. Construir payload JSON con `JSONObject` o serializador (Moshi/Gson/Kotlinx Serialization).
4. Añadir contratos de error (sealed class/result) en servicios.

---

## 3) UI/UX

### Hallazgos
- Hay múltiples pantallas con lógica de estado manual sin patrón consistente de loading/error/empty.
- El flujo de filtros en transacciones no valida formato de fecha en el momento del input (se ignora inválido silenciosamente).
- Existen labels hardcodeadas en navegación (`nav_graph`) y textos en código.

### Mejoras recomendadas
1. Aplicar un modelo de estado único por pantalla (`Loading`, `Content`, `Empty`, `Error`).
2. Validación inline en formularios con `TextInputLayout.setError` en vez de solo `Toast`.
3. Revisar consistencia tipográfica/espaciado y contrastes con Material 3.
4. Añadir estados vacíos accionables (CTA claros para crear primera transacción/meta/recordatorio).

---

## 4) Rendimiento

### Hallazgos
- Consultas y agregaciones complejas en SQLite se ejecutan en UI thread.
- No hay índices para columnas de filtro frecuentes (`fecha`, `categoria_id`, etc.).
- Se recalculan estructuras en memoria repetidamente en flujos de dashboard.

### Mejoras recomendadas
1. Índices en DB (`transacciones(fecha)`, `transacciones(categoria_id, fecha)`, `recordatorios(fecha_vencimiento)`).
2. Mover trabajo pesado a background.
3. Medir con `Macrobenchmark` y `androidx.tracing` para hotspots.

---

## 5) Base de datos / lógica

### Hallazgos
- Se usa `SQLiteOpenHelper` manual; no hay Room ni migraciones versionadas.
- No hay claves foráneas explícitas ni integridad referencial visible.
- Password de usuario se persiste en texto plano en tabla `users`.

### Mejoras recomendadas
1. Migrar a Room (entities, DAO, migraciones no destructivas).
2. Definir foreign keys y reglas `ON DELETE`/`ON UPDATE`.
3. Hash de password con sal fuerte (Argon2/Bcrypt/PBKDF2), idealmente del lado servidor.
4. Validaciones de negocio centralizadas (monto > 0, fecha válida, categoría consistente con tipo).

---

## 6) Gradle y configuración

### Hallazgos
- Doble dependencia de Material en `app/build.gradle.kts`.
- Se define `libs.versions.toml`, pero el módulo no usa version catalog para la mayoría de dependencias.
- Versiones inconsistentes entre catalog y dependencias hardcodeadas.

### Mejoras recomendadas
1. Consolidar todas las dependencias en version catalog.
2. Eliminar duplicados y habilitar chequeos de versiones.
3. Activar `buildConfigField`/flavors para URL por entorno (`debug`, `staging`, `release`).

---

## 7) Seguridad

### Hallazgos
- `network_security_config` permite cleartext HTTP (`10.0.2.2` y `localhost`).
- Token y datos sensibles en `SharedPreferences` sin cifrado.
- PIN hasheado con SHA-256 pero sin sal/per-user random salt fuerte.
- Falta manejo de permiso `POST_NOTIFICATIONS` para Android 13+ si se usan recordatorios.

### Mejoras recomendadas
1. HTTPS obligatorio en release y bloquear cleartext fuera de debug.
2. `EncryptedSharedPreferences` + Android Keystore para secretos.
3. Mejorar política de PIN (intentos, lockout, biometric fallback opcional).
4. Endurecer autenticación/sesión (expiración real, refresh token, logout robusto).

---

## 8) Testing

### Hallazgos
- Solo existen tests plantilla (`ExampleUnitTest`, `ExampleInstrumentedTest`).
- No hay tests de repositorio, parseo, filtros, reglas de negocio ni UI crítica.

### Recomendaciones
1. Unit tests: parseo de monto/fecha, cálculo de resumen, reglas de filtros.
2. Integration tests DB (Room/SQLite): CRUD transacciones, presupuestos por categoría, migraciones.
3. UI tests Espresso: login/registro, alta/edición de transacción, filtros, importación.
4. Tests de seguridad: manejo de sesión, bloqueo PIN, persistencia segura.

---

## 9) Escalabilidad

### Riesgos al crecer
- Arquitectura actual dificultará mantenimiento por acoplamiento alto entre UI y datos.
- `LocalRepository` seguirá creciendo monolítico y eleva riesgo de regresiones.
- Ausencia de DI/abstracciones complica mocking y pruebas automáticas.

### Ruta de escalado sugerida
1. Modularizar por feature (`feature-home`, `feature-transactions`, `core-data`, `core-ui`).
2. Introducir DI (Hilt) y contratos por capa.
3. Estandarizar patrón MVI/MVVM y un `UiState` común.
4. Definir observabilidad (crash reporting + analytics de eventos clave).

---

## Mejoras concretas priorizadas (alto impacto primero)

1. **Seguridad de credenciales y sesión**: eliminar password en texto plano, cifrar preferencias, endurecer auth.
2. **No bloquear UI con DB**: mover I/O a background + estado observable en UI.
3. **Migración de BD no destructiva**: reemplazar `onUpgrade` con migraciones reales.
4. **Refactor de arquitectura (MVVM + repositorios por dominio + DI)**.
5. **Consistencia de Gradle/dependencias**: version catalog único + limpieza de duplicados.
6. **Cobertura de testing real** en dominio + integración + UI.

---

## Funcionalidades realistas propuestas

1. **Presupuesto recurrente y plantillas mensuales** por categoría.
2. **Detección de gastos anómalos** (alerta por desviación de promedio histórico).
3. **Objetivos automáticos** (reglas: “redondear compras y ahorrar diferencia”).
4. **Importación bancaria robusta** (CSV/OFX con mapping persistente por banco).
5. **Panel de salud financiera** (score simple: ahorro, deuda/ingreso, cumplimiento de metas).
6. **Backup/restore cifrado** local o en nube privada del usuario.

---

## Archivos específicos que modificaría y cómo

### Arquitectura / datos
- `app/src/main/java/com/example/finanzas/data/local/LocalRepository.java`
  - Separar por dominios y reducir clase “god object”.
- `app/src/main/java/com/example/finanzas/data/local/LocalDatabase.java`
  - Subir versión + migraciones no destructivas + índices + FK.

### UI / presentación
- `app/src/main/java/com/example/finanzas/ui/HomeFragment.java`
  - Extraer lógica a `HomeViewModel`, estado único y render por secciones.
- `app/src/main/java/com/example/finanzas/ui/NuevaTransaccionFragment.java`
  - Mover validaciones/parsing a capa dominio y testear utilidades.
- `app/src/main/java/com/example/finanzas/ui/ListaTransaccionesFragment.java`
  - Encapsular filtros en ViewModel y exponer estado filtrado/paginado.

### Seguridad
- `app/src/main/java/com/example/finanzas/util/Prefs.java`
  - Migrar a `EncryptedSharedPreferences`, separar claves sensibles.
- `app/src/main/res/xml/network_security_config.xml`
  - Permitir cleartext solo en debug.
- `app/src/main/AndroidManifest.xml`
  - Revisar permisos y política de backup para datos sensibles.

### Build / configuración
- `app/build.gradle.kts`
  - Eliminar dependencias duplicadas, migrar a `libs.versions.toml`.
- `gradle/libs.versions.toml`
  - Centralizar versiones reales usadas por el módulo.

### Testing
- `app/src/test/...` y `app/src/androidTest/...`
  - Añadir suites para reglas de negocio, DB, flujos críticos de UI.
