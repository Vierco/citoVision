# ADR-0005 - Subir el toolchain a Kotlin 2.2.20 para habilitar el enlazado de iOS (native)

## Estado

Aceptada — 2026-08-25

## Contexto

Durante el bring-up de iOS (ADR-0004), la app compila el framework de iOS pero **falla al enlazar**
(`./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` en dinámico, y el build de la app en Xcode 26):

```
Undefined symbols for architecture arm64:
  "_kfun:androidx.lifecycle.viewmodel.compose#...LocalViewModelStoreOwner$stableprop_getter$artificial..."
      referenced from AppNavHost, AnalysisScreen, HistoryScreen, PatientsScreen
```

Diagnóstico (confirmado paso a paso):

- El **compilador Kotlin/Native efectivo es 2.2.10** (lo arrastra AGP 9.2.1; el `kotlin = "2.1.0"` del
  catálogo es solo un marcador y no hay override `kotlin.native.version`).
- **Compose Multiplatform 1.10.3 exige Kotlin ≥ 2.2.20 para targets native**
  (doc oficial "What's new in Compose Multiplatform 1.10"). Sus librerías (lifecycle 2.10.0,
  navigation 2.9.2, compose-runtime) se publicaron con el **compose-compiler de 2.2.20**, que genera el
  símbolo sintético de estabilidad `LocalViewModelStoreOwner$stableprop_getter$artificial`. Nuestro
  compose-compiler **2.2.10** emite la *referencia* con otra firma → en el **link duro de Native** el
  símbolo no existe. En la JVM (Android/Desktop) se resuelve dinámicamente, por eso **solo iOS rompe**.
- Subir `jetbrains-lifecycle` a 2.10.0 (versión que empareja con CMP 1.10.3) **no** resolvió el fallo:
  confirma que el problema es del **compilador**, no de la librería (el klib de lifecycle 2.10.0 sí lo
  lee el compilador 2.2.10 —el build llega al link—, pero el símbolo artificial del compose-compiler no
  casa entre 2.2.10 y 2.2.20).
- Aparte, con framework **estático** Xcode 26 rechaza el enlazado del app contra frameworks privados que
  arrastra Compose (`cannot link directly with 'SwiftUICore'... not an allowed client`,
  `framework 'UIUtilities' not found`). En **dinámico** ese enlazado lo resuelve el propio framework y
  esos errores desaparecen (cambio ya aplicado en `shared/build.gradle.kts`, ver ADR-0004).

## Decisión

Alinear el toolchain con lo que Compose Multiplatform 1.10.3 requiere para native, con el cambio
**mínimo** posible:

| Pin (`gradle/libs.versions.toml`) | Antes | Después |
|---|---|---|
| `kotlin` | `2.1.0` (efectivo 2.2.10 vía AGP) | **`2.2.20`** |
| `ksp` | `2.2.10-2.0.2` | **`2.2.20-2.0.4`** (debe casar con Kotlin) |
| `jetbrains-lifecycle` | `2.9.6` → `2.10.0` | `2.10.0` (ya aplicado; empareja con CMP 1.10.3) |
| `mokkery` | `2.9.0` | **la versión que soporte Kotlin 2.2.20** (plugin de compilador; se fija al aplicar) |
| framework iOS `isStatic` | `true` → `false` | `false` (ya aplicado; ver ADR-0004) |

- Subir `kotlin` cambia también, por `version.ref`, los plugins `kotlin.compose` (compose-compiler),
  `kotlin.android` y `kotlin.serialization` a 2.2.20 — que es exactamente lo que arregla el símbolo.
- Se actualizan los comentarios del catálogo que hoy dicen "compilador efectivo 2.2.10".

## Alternativas consideradas

1. **Bajar CMP + lifecycle + navigation a un set compilado con Kotlin 2.2.10.** Descartada: es una
   regresión mayor (toca la base de UI/navegación del proyecto, ya calibrada para 1.10.3) frente a un
   bump de patch (2.2.10 → 2.2.20).
2. **Mantener el framework estático.** Descartada: en Xcode 26 el estático choca con el
   *allowed-client* de `SwiftUICore`/`UIUtilities`. El dinámico lo evita.
3. **Override parcial `kotlin.native.version=2.2.20` dejando el resto en 2.2.10.** Descartada: el símbolo
   lo genera el **compose-compiler**, ligado a la versión de Kotlin del proyecto, no solo al backend
   Native; un override parcial no cambiaría el compose-compiler y no arreglaría el símbolo.

## Consecuencias

**Positivas**

- Desbloquea el enlazado de iOS (objetivo de ADR-0004) y alinea el proyecto con el requisito oficial de
  CMP 1.10.3 para native.
- El compilador **sube** (2.2.10 → 2.2.20): sigue leyendo todos los klibs actuales (FileKit 0.11.0,
  Coil 3.3.0, Firebase 2.1.0, Koin 4.0.0). Es la **dirección segura** de compatibilidad de klibs
  (compilador nuevo ↔ klibs iguales o más viejos); no toca los límites documentados de FileKit 0.12+/
  Coil 3.4+ (que son el caso contrario y no se modifican).

**Negativas / deuda**

- **Cambio transversal**: obliga a **reverificar las tres plataformas** antes de commitear —
  Android (APK debug), Desktop (build/run) e iOS (link + arranque en simulador).
- **Mokkery** es un plugin de compilador y debe casar con 2.2.20; puede requerir subir su versión.
  Impacto acotado a tests (`commonTest`), no al binario de producción.
- Bump de patch de Kotlin: aunque de bajo riesgo, puede desplazar versiones transitivas del grafo; se
  valida con la reverificación por plataforma.
- El framework dinámico embebe la librería en el bundle (frente al estático); impacto de tamaño/arranque
  despreciable para este caso.

## Verificación requerida antes de commit

1. `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` enlaza sin `Undefined symbols`.
2. Xcode 26: **Build + Run** en simulador iPhone → arranca la UI Compose (modo invitado).
3. Android: `:androidApp:assembleDebug` compila.
4. Desktop: build/run de `:desktopApp`.
5. Tests: `:shared:testDebugUnitTest`/`:shared:allTests` compilan (Mokkery OK) o se ajusta su versión.
6. Sergio revisa el diff y **hace él el commit**.

## Referencias

- ADR-0004 - Bring-up de iOS (Fase 1): origen de este bloqueo; cambio a framework dinámico.
- "What's new in Compose Multiplatform 1.10" (kotlinlang.org): requisito Kotlin ≥ 2.2.20 para native.
- KT-75781 (soporte de Xcode ≥ 16.3 desde Kotlin 2.1.21).
- KSP `2.2.20-2.0.4` (Maven Central).
- SPEC-0006 / ADR-0003 (inferencia ONNX iOS: fase futura, no afectada aquí).
- AGENTS.md §3 (dependencias/KMP), §7 (spec-driven), §16 (no compilar), §17 (no afirmar sin ejecutar).
