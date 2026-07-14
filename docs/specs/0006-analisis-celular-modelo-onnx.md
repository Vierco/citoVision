# SPEC-0006 - Análisis celular con modelo de detección on-device (ONNX)

## Estado

Approved

> Depende de **RFC-0001** (motor de inferencia ONNX Runtime), aprobado y promovido a **ADR-0003**. Sustituye el mock de SPEC-0004 por la
> **generación real** del conteo celular a partir de un modelo YOLO11s afinado sobre glóbulos blancos
> (dataset UNIVALI), ejecutado **on-device**, y añade una **prioridad de revisión** (baja/media/alta) como
> apoyo al cribado morfológico.

## Contexto

Hoy "Iniciar Escáner" persiste un análisis con **datos mock** (`SaveMockAnalysisUseCase`, SPEC-0004): el
resumen y el conteo celular son ficticios. Esta spec conecta la imagen cargada (SPEC-0003) con el **modelo
real** para producir detecciones de células, derivar de ellas el **conteo celular**, una **prioridad de
revisión profesional** y un **resumen**, y persistir el análisis resultante en local (SPEC-0004) y, con
cuenta, en remoto (SPEC-0005).

> **Propósito y límite (crítico).** citoVision es una **herramienta de apoyo al cribado morfológico**, no un
> sistema de diagnóstico. La aplicación **no emite diagnósticos ni infiere enfermedades**. Su función es
> **priorizar muestras para revisión por un profesional humano** en función de la presencia de determinadas
> células hematológicas. El profesional humano siempre está **en el bucle**: la prioridad es una sugerencia
> de orden de revisión, nunca una conclusión clínica. Este límite es coherente con las capacidades reales de
> un modelo de detección entrenado con el dataset UNIVALI y evita que el producto se comporte como un
> dispositivo médico de diagnóstico.

## Objetivo

Que al pulsar "Iniciar Escáner" sobre una imagen cargada, la app ejecute el modelo ONNX on-device, cuente
las células detectadas por tipo, **calcule una prioridad de revisión (baja/media/alta)** según la relevancia
morfológica de lo hallado, y guarde un `Analysis` real (dominio `Analysis`/`CellCount` + nueva prioridad) en
lugar del mock, con estados de carga y error adecuados y **sin emitir diagnóstico**.

## No objetivos

- **Diagnosticar, inferir enfermedades o emitir conclusiones clínicas.** La salida es priorización +
  descripción de lo detectado; la interpretación clínica corresponde al profesional humano.
- Rediseñar el flujo, la card, el Historial o Pacientes. Se mantienen; solo cambia el **origen** del dato y
  se **añade** el campo `priority` (nuevo campo aditivo en `Analysis`, mostrado en card/detalle).
- iOS: la implementación `actual` de iOS (cinterop) queda para una fase posterior (RFC-0001). Esta spec se
  entrega para **Android y Desktop**.
- Reentrenar, versionar o actualizar el modelo en caliente (OTA). El modelo va **empaquetado**.
- Segmentación, tracking o visualización de bounding boxes sobre la imagen *(candidato a spec futura; ver
  Notas abiertas)*.
- Aceleración por GPU/NPU como requisito (se admite CPU; los *execution providers* son optimización, no
  requisito).

## Usuarios / actores

- **Usuario con cuenta** e **invitado**: **ambos pueden ejecutar el modelo** y analizar en local; la
  escritura remota sigue las reglas de SPEC-0005 (solo cuenta; invitado = guardado local silencioso).

## Requisitos funcionales

- **RF-1** Al pulsar **"Iniciar Escáner"** con una imagen cargada, se ejecuta el modelo ONNX sobre esa
  imagen y se obtiene una lista de **detecciones** (tipo de célula + confianza).
- **RF-2** El **conteo celular** (`cellCounts`) se deriva de las detecciones: por cada tipo detectado se
  genera un `CellCount` cuyo `value` incluye **recuento absoluto y porcentaje**, con el formato
  **`N (P%)`** (p. ej. `12 (34%)`). El porcentaje se calcula sobre el **total de células** (denominador
  según RN-6).
- **RF-3** El **resumen** (`summary`) se genera a partir del conteo, de forma **descriptiva** (qué células y
  cuántas), **sin lenguaje diagnóstico**, con esta plantilla:

  > *"{total} células detectadas. Prioridad de revisión: {PRIORIDAD}. Hallazgos principalmente relevantes:
  > {relevantes}. Otros hallazgos encontrados en la revisión: {otros}."*

  donde `{relevantes}` son los tipos con **peso morfológico > 0** presentes (RN-8, criterio por relevancia
  clínica) y `{otros}` el resto de tipos celulares presentes (habituales, +0) más las clases no celulares;
  cada entrada como `Tipo N (P%)`. Las secciones vacías se omiten con naturalidad.
- **RF-3b** Se calcula una **prioridad de revisión** (`Priority` ∈ {BAJA, MEDIA, ALTA}) según la puntuación
  de relevancia morfológica de RN-8/RN-9. La prioridad se **muestra** en la card y en el detalle con un
  indicador claro (etiqueta + color, tokens de `DESIGN.md`; no depender solo del color, AGENTS.md §15).
- **RF-3c** La UI incluye, de forma visible junto al resultado, un **aviso** de que citoVision **no realiza
  diagnósticos** y que la prioridad es una **sugerencia de revisión** para un profesional.
- **RF-4** Mientras el modelo procesa, la UI muestra un **estado de carga** (la imagen se conserva); la
  inferencia ocurre **fuera del hilo principal**.
- **RF-5** Al terminar con al menos una célula, se persiste un `Analysis` real (con su `priority`; local
  SPEC-0004; remoto vía outbox SPEC-0005 si hay cuenta) y aparece en el Historial, exactamente como hoy.
- **RF-6** Si la inferencia **no detecta ninguna célula**, se **informa** al usuario con un popup ("no se
  han detectado células") y **no se guarda** ningún análisis (RN-7).
- **RF-7** Si la inferencia falla (carga del modelo, decodificado de imagen, ejecución), se muestra un
  **popup de error** con opción de **reintentar**; no se persiste un análisis a medias.
- **RF-8** El modelo se **carga una vez** y se reutiliza entre análisis (no se recarga por cada escaneo).

## Requisitos no funcionales

- Inferencia en un `CoroutineDispatcher` **inyectado** (Koin); nunca en UI (RULES.md §Concurrencia).
- `CellDetector` devuelve siempre **`Result`** (RULES.md §Manejo de errores).
- Pre/post-procesado **determinista** en `commonMain`, testeable sin motor real.
- Sin secretos ni PII en logs (Napier); no loguear la imagen ni buffers.
- Tamaño de binario auditado tras empaquetar el modelo (AGENTS.md §16).
- `OrtSession` como `single` en Koin; cierre correcto de recursos nativos (`OnnxTensor`/`OrtSession`).

## Reglas de negocio

- **RN-1** El conjunto de **clases** del modelo (14, ver Contratos) y su orden lo define el export; el mapeo
  índice→etiqueta legible en español es **fuente única** en `commonMain`. El orden de índices **no debe
  alterarse** (coincide con la salida del modelo).
- **RN-2** Parámetros de inferencia (de la metadata del `.onnx`): `imgsz` 640×640, NCHW, RGB, normalización
  0..1. **Umbral de confianza** e **IoU de NMS** no vienen embebidos → valores por defecto **conf 0.25 /
  IoU 0.45**, fijos y documentados, ajustables tras validar contra la referencia de Ultralytics.
- **RN-3** Una detección por debajo del umbral de confianza **no cuenta**.
- **RN-4** El paciente sigue siendo un **código seudonimizado** (hereda RN de SPEC-0004/0005); el modelo no
  procesa ni infiere identidad.
- **RN-5** El resultado es **reproducible** para la misma imagen y modelo (inferencia determinista).
- **RN-6** Dos de las 14 clases **no son células** (`Artefacto` idx 0, `Restos celulares` idx 13). Se
  **detectan y se muestran con su recuento**, pero quedan **fuera del denominador del porcentaje**: el `%`
  de cada tipo celular se calcula **solo sobre el total de células reales** (las 12 clases marcadas ✅). Las
  dos clases no celulares aparecen con su recuento absoluto pero **sin porcentaje** (o con `%` sobre células
  a título informativo, a concretar en implementación); nunca inflan el denominador.
- **RN-7** **Cero células detectadas** → se informa y **no se persiste** análisis (RF-6). Un análisis
  guardado tiene siempre al menos una célula.
- **RN-8** **Puntuación de relevancia morfológica.** Se calcula por **presencia** de cada tipo relevante
  (una única contribución por tipo presente con ≥1 detección sobre umbral, **independiente del número** de
  células de ese tipo — *Nota abierta 10*). Pesos:

  | Hallazgo (presente) | Puntos |
  |---|---|
  | Blasto | +5 |
  | Promielocito | +4 |
  | Mielocito | +3 |
  | Metamielocito | +3 |
  | Linfocito atípico | +2 |
  | Basófilo | +2 |
  | Eritroblasto | +2 |
  | Neutrófilo en banda (bastonete) | +1 |
  | Resto de tipos celulares (Linfocito, Neutrófilo segmentado, Monocito, Eosinófilo) | +0 |
  | Clases no celulares (Artefacto, Restos celulares) | +0 (no influyen, RN-6) |

  La puntuación total es la **suma** de los pesos de los tipos relevantes presentes.
- **RN-9** **Mapeo puntuación → prioridad:** `0 → BAJA`; `1–4 → MEDIA`; `≥5 → ALTA`. (Un blasto presente
  fuerza ALTA por sí solo; la presencia conjunta de hallazgos moderados puede alcanzar ALTA por suma.)
- **RN-10** La prioridad es **apoyo al cribado**, nunca diagnóstico (ver Propósito y límite). No se deriva
  de ella ninguna afirmación sobre enfermedad.

## Estados de UI

- **Analizando**: indicador de progreso tras "Iniciar Escáner"; imagen conservada; acciones deshabilitadas.
- **Éxito**: se persiste y navega/actualiza como hoy (card en Historial), mostrando la **prioridad**
  (indicador baja/media/alta con etiqueta + color) y el **aviso de no-diagnóstico** (RF-3b, RF-3c).
- **Sin células**: popup informativo ("no se han detectado células") con botón de cerrar; **no se guarda**;
  la imagen permanece para reintentar o cambiarla (RF-6, RN-7).
- **Error de inferencia**: popup con mensaje y botón **"Reintentar"** (+ cerrar); la imagen permanece.

## Contratos de datos

**Dominio — cambio aditivo en `Analysis`:** hoy es
`Analysis(id, patient, performedAt, summary, imagePath, cellCounts)`. Se **añade** `priority: Priority`.
`CellCount(name, value, position)` no cambia de forma; sí cambia **cómo se rellena** su `value` (RF-2).

> **Impacto de esquema (aditivo).** El nuevo campo `priority` obliga a: (a) ampliar la entidad de dominio
> `Analysis` (SPEC-0004); (b) **migración no destructiva** de Room subiendo la versión de la BD local
> (RULES.md §Persistencia); (c) añadir el campo al documento de Firestore y a sus DTOs/mappers (SPEC-0005).
> Análisis antiguos sin prioridad se tratan como `null`/BAJA por defecto (a concretar en la migración).
> *Estas specs se tocan por dependencia; requiere confirmación del owner (AGENTS.md §0.4).*

**Nuevo (dominio/application):**

```
Priority { BAJA, MEDIA, ALTA }

Detection
  classIndex: Int
  label:      String     // etiqueta legible del tipo celular (RN-1)
  confidence: Float      // 0..1
  box:        BoundingBox // xyxy normalizado (para posible visualización futura; ver No objetivos)

CellDetector (puerto, application/ports, commonMain)
  suspend fun detect(image: ImageInput): Result<List<Detection>, InferenceError>

PriorityCalculator (dominio, commonMain, función pura)
  fun priorityOf(detections: List<Detection>): Priority   // aplica RN-8/RN-9
```

**Clases del modelo (14, orden = índice de salida; fuente `data.yaml`, portugués → etiqueta ES):**

| Idx | Modelo (pt) | Etiqueta ES | ¿Célula? |
|---|---|---|---|
| 0 | Artefato | Artefacto | ❌ |
| 1 | Basofilo | Basófilo | ✅ |
| 2 | Bastonete | Neutrófilo en banda (cayado) | ✅ |
| 3 | Blasto | Blasto | ✅ |
| 4 | Eosinofilo | Eosinófilo | ✅ |
| 5 | Eritroblasto | Eritroblasto | ✅ |
| 6 | Linfocito | Linfocito | ✅ |
| 7 | Linfocito atipico | Linfocito atípico | ✅ |
| 8 | Metamielocito | Metamielocito | ✅ |
| 9 | Mielocito | Mielocito | ✅ |
| 10 | Monocito | Monocito | ✅ |
| 11 | Neutrofilo segmentado | Neutrófilo segmentado | ✅ |
| 12 | Promielocito | Promielocito | ✅ |
| 13 | Restos celulares | Restos celulares | ❌ |

**Configuración del modelo (leída de la metadata embebida en `citovision_yolo11s_seg_v1.onnx`):**

| Parámetro | Valor |
|---|---|
| `task` | **`segment`** (YOLO11s-seg, segmentación de instancias) |
| `imgsz` | `640 × 640` |
| `stride` | 32 · `batch` 1 · `channels` 3 (RGB) |
| Normalización | 0..1, layout NCHW |
| `names` | 14 clases (coinciden con la tabla; orden = índice confirmado) |
| Umbral de confianza / IoU NMS | **no embebidos** → defaults propuestos **conf 0.25 / IoU 0.45**, ajustables tras validar |

> **El modelo es de segmentación, pero SPEC-0006 usa solo la rama de detección.** Un YOLO11-seg expone dos
> salidas: (1) cabezal de detección `[1, 4+nc+32, 8400]` = `[1, 50, 8400]` (caja + 14 clases + 32
> coeficientes de máscara), y (2) prototipos de máscara `[1, 32, 160, 160]`. Para **contar y priorizar** se
> decodifica **solo** `[1, 4+nc, 8400]` (caja + clases), se aplica NMS por clase y se cuentan instancias;
> los **coeficientes y prototipos de máscara se ignoran**. Las formas exactas se confirman al cargar vía
> `OrtSession` en implementación. *(La segmentación real —contornos/máscaras— queda para la spec futura de
> visualización, junto a las cajas.)*

**Empaquetado:** fichero `.onnx` (39 MB) en **Compose Resources** →
`shared/src/commonMain/composeResources/files/citovision_yolo11s_seg_v1.onnx`, leído con
`Res.readBytes("files/citovision_yolo11s_seg_v1.onnx")`. Un único artefacto para las tres plataformas. Dado
el tamaño y que el repo **no usa Git LFS**, se decide entre commit directo o habilitar LFS.

## Errores

`InferenceError` (application/domain):
- `ModelLoadFailed` — no se pudo cargar/inicializar el modelo o la `OrtSession`.
- `ImageDecodeFailed` — la imagen no se pudo decodificar a píxeles.
- `InferenceFailed` — fallo durante la ejecución del tensor.
- `Unknown(cause)`.

Las excepciones nativas (ONNX Runtime, decodificado) se capturan en `infrastructure` y se transforman en
estos errores (RULES.md §Manejo de errores). El ViewModel los traduce al popup de RF-6.

## Casos borde

- **Imagen sin células** → cero detecciones → popup informativo, **no se guarda** (RF-6, RN-7).
- **Imagen corrupta / formato no soportado** → `ImageDecodeFailed` → popup.
- **Modelo ausente o corrupto en el paquete** → `ModelLoadFailed` → popup (no debería ocurrir si va
  empaquetado, pero se contempla).
- **Imagen muy grande** → el preprocesado la redimensiona (letterbox); vigilar memoria (AGENTS.md §16).
- **Escaneo mientras otro análisis está en curso** → se evita reentrada (un análisis a la vez).
- **Invitado analiza** → ejecuta el modelo y guarda **solo en local, de forma silenciosa** (SPEC-0005 RN-5).

## Telemetría / analytics

Fuera de alcance. *(Posible métrica futura: tiempo de inferencia por plataforma.)*

## Seguridad y privacidad

- **No es un dispositivo de diagnóstico.** La app no diagnostica ni infiere enfermedades; prioriza para
  revisión humana (Propósito y límite, RN-10). El aviso de no-diagnóstico es **requisito de UI** (RF-3c).
  Esto acota la responsabilidad clínica y el encuadre regulatorio del producto.
- **Humano en el bucle**: la prioridad no dispara acciones automáticas ni sustituye la revisión profesional.
- Inferencia **on-device**: la imagen **no sale del dispositivo** para analizarse (mejora de privacidad
  frente a inferencia remota).
- No loguear imágenes, buffers de píxeles ni tensores (SECURITY_MOBILE §Logging).
- El modelo empaquetado no contiene secretos; es un artefacto de pesos.
- La escritura remota del resultado sigue la deuda de reglas de SPEC-0005 (no la reabre esta spec).

## Criterios de aceptación

- Cargar una imagen y pulsar "Iniciar Escáner" → estado "Analizando" → aparece en el Historial un `Analysis`
  con `cellCounts` **derivados de detecciones reales** (no mock), un `summary` descriptivo y una `priority`.
- **Priorización correcta**: una muestra con un blasto presente → **ALTA**; con solo un bastonete → **MEDIA**;
  con únicamente linfocitos/neutrófilos segmentados/monocitos/eosinófilos → **BAJA**; artefactos/restos no
  alteran la prioridad (RN-8/RN-9).
- El **aviso de no-diagnóstico** es visible junto al resultado (RF-3c).
- Con cuenta, el análisis real (con prioridad) se sincroniza a remoto vía outbox (SPEC-0005).
- Provocar un fallo de inferencia → popup con "Reintentar"; no se persiste análisis a medias.
- Misma imagen procesada dos veces → mismo conteo (determinismo, RN-5).
- La inferencia no bloquea la UI (estado de carga responsivo).
- Funciona en **Android y Desktop** con el **mismo `.onnx`** y el mismo pre/post de `commonMain`.

## Tests requeridos

- **commonTest**: `YoloPostprocessor` (decodificado + NMS con salida sintética conocida; umbral descarta
  detecciones bajas; NMS elimina solapadas); `YoloPreprocessor` (letterbox/normalización con dimensiones
  conocidas → tamaños/valores esperados); mapeo índice→etiqueta (RN-1); derivación conteo/summary de
  detecciones (RF-2/RF-3); **`PriorityCalculator`** (función pura: cada peso individual, suma de moderados
  que alcanza ALTA, presencia de blasto → ALTA, solo habituales → BAJA, artefactos/restos no puntúan —
  cubre RN-8/RN-9); `AnalysisViewModel`/flujo de escaneo con un `CellDetector` **fake** (éxito → persiste
  con prioridad; cero células → popup, no persiste; fallo → popup, no persiste).
- **Grafo Koin**: `CellDetector`, `OrtSession`/dispatcher, use cases y ViewModel resuelven
  (`AppModulesGraphTest`).
- **Integración de plataforma** (manual o instrumentada): inferencia real sobre una imagen de muestra en
  Android y Desktop, comparando detecciones con la referencia de Ultralytics (validación del pipeline).

## Dependencias

- **RFC-0001** aprobado (→ promover a ADR).
- **ONNX Runtime** (`onnxruntime-android` + `onnxruntime` JVM); nueva entrada en el catálogo de versiones.
- **SPEC-0003** (imagen cargada), **SPEC-0004** (dominio `Analysis`/`CellCount`, persistencia local, flujo
  de escáner, mock a sustituir), **SPEC-0005** (sincronización remota del resultado).
- **Impacto sobre SPEC-0004 y SPEC-0005** por el nuevo campo `priority`: entidad de dominio, **migración
  Room** (versión BD local) y **campo Firestore + DTO/mapper**. Cambios aditivos, pero tocan specs aprobadas
  → **requieren confirmación del owner** (AGENTS.md §0.4).
- Modelo **YOLO11s** afinado (dataset UNIVALI), exportado a `.onnx`, con su lista de clases y parámetros de
  export.

## Notas abiertas

**Resueltas por el owner:**

- ✅ **Formato de `value`**: recuento **y** porcentaje → **`N (P%)`** (p. ej. `12 (34%)`) (RF-2).
- ✅ **Cero detecciones**: se **informa** y **no se guarda** (RF-6, RN-7).
- ✅ **Clases**: 14, listadas en Contratos con etiqueta ES (fuente `data.yaml`).
- ✅ **Clases no celulares** (`Artefacto`, `Restos celulares`): se muestran con su recuento pero **fuera del
  denominador del porcentaje** (RN-6).
- ✅ **Priorización** (baja/media/alta) por puntuación de relevancia morfológica (RN-8/RN-9) y **encuadre
  no-diagnóstico** con humano en el bucle (Propósito y límite, RN-10, RF-3b/3c).
- ✅ **Eritroblasto**: peso **+2** (relevancia moderada, RN-8).
- ✅ **Impacto de esquema** (campo `priority`: dominio + migración Room + campo Firestore): **autorizado**
  por el owner; se reflejará en SPEC-0004/0005 al implementar.
- ✅ **Invitado ejecuta el modelo**: sí (local silencioso; remoto solo con cuenta).
- ✅ **Empaquetado**: Compose Resources `files/citovision_yolo11s_seg_v1.onnx` (39 MB; LFS vs commit directo,
  decisión operativa, no bloquea la spec).
- ✅ **Visualización de cajas**: **fuera de alcance**, candidata a spec futura (que ahora podría incluir
  máscaras, al ser el modelo de segmentación).
- ✅ **Modelo real**: **YOLO11s-seg** (segmentación); SPEC-0006 usa **solo la rama de detección**, ignora
  máscaras (ver Contratos).
- ✅ **Parámetros de inferencia** (fleco 5): leídos de la metadata → `imgsz` 640, NCHW/RGB, norm 0..1;
  conf **0.25** / IoU **0.45** por defecto (no embebidos), ajustables tras validar (RN-2).
- ✅ **Plantilla del `summary`** (RF-3) y su **agrupación por relevancia clínica** (opción A): relevantes =
  peso >0; otros = habituales + no-células.

**Pendientes:** ninguna que bloquee. *(Detalle heredado, no bloqueante: RN-8 puntúa por **presencia**, no
por recuento, coherente con el enunciado; revisable si algún día se quiere ponderar por número.)*
