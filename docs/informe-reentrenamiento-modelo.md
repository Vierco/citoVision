# Informe: reentrenamiento del modelo de detección celular (citoVision)

**Para:** responsable del entrenamiento del modelo
**Origen:** validación del pipeline de inferencia on-device (SPEC-0006, fase de validación)
**Modelo evaluado:** `citovision_yolo11s_seg_v1.onnx` (YOLO11s-seg, 14 clases, dataset Roboflow
`annotation-ranuk/visao_computacional` v10)

---

## 1. Resumen

El modelo actual **no generaliza**. Memorizó las imágenes de entrenamiento y falla sobre datos no vistos,
en particular en las clases clínicamente relevantes.

**El problema no está en los hiperparámetros de entrenamiento, sino en cómo está repartido el dataset.**
Reentrenar con el mismo reparto y distintos epochs / learning rate / augmentation **no lo arreglará**.

Toda la evidencia de abajo se obtuvo ejecutando el `.onnx` entregado con Ultralytics (pre/post canónico,
conf 0.25 / IoU 0.45, imgsz 640), no con código propio.

---

## 2. Evidencia

### 2.1. Misma clase, mismo modelo, dos splits

| Split | Acierto top-1 en **Blasto** |
|---|---|
| `train` (visto en entrenamiento) | **96%** (77/80) |
| `valid` (no visto) | **2%** (2/114) |

Un 96% → 2% es memorización, no aprendizaje. En `valid`, los blastos se clasifican como
Linfocito (42%), Restos celulares (35%) o Monocito (17%).

### 2.2. Acierto top-1 por clase en `test` (160 imágenes de una sola anotación)

| Clase | Acierto | Relevancia para el producto |
|---|---|---|
| Eosinófilo | 100% | baja |
| Linfocito | 97% | baja |
| Neutrófilo segmentado | 97% | baja |
| Restos celulares | 65% | no es célula |
| Monocito | 12% | baja |
| **Bastonete (cayado)** | **0%** | **relevante** |
| **Linfocito atípico** | **0%** | **relevante** |
| **Blasto** (medido en `valid`) | **2%** | **el más relevante** |

El patrón es sistemático: **el modelo acierta en las clases frecuentes y sin peso clínico, y falla en
todas las que motivan una revisión prioritaria.** La aplicación usa estas clases para priorizar muestras
de cara a revisión humana; con el modelo actual, sobre datos nuevos, prácticamente todo saldría como
prioridad baja, que es justo el fallo que no nos podemos permitir (un falso negativo en blasto).

---

## 3. Causa raíz: el split no está estratificado

Los tres splits son **cortes por número de fichero**, sin barajar:

```
train: img_000001 .. img_009320   (9320 imgs)
valid: img_009321 .. img_010485   (1165 imgs)
test:  img_010486 .. img_011651   (1166 imgs)
```

Como el dataset está ordenado/agrupado por clase u origen, cada split cayó sobre una zona distinta y las
distribuciones resultantes no se parecen entre sí:

| Clase | Total | train | valid | test |
|---|---|---|---|---|
| Blasto | 1179 | **272** | **907** | **0** |
| Eosinófilo | 495 | 144 | 3 | **348** |
| Linfocito atípico | 569 | 520 | 3 | 46 |
| Promielocito | 214 | 214 | 0 | 0 |
| Mielocito | 253 | 249 | 4 | 0 |
| Eritroblasto | 44 | 44 | 0 | 0 |

Consecuencias directas:

- **El 77% de los blastos del dataset (907 de 1179) quedó fuera del entrenamiento**, en `valid`.
- `test` **no contiene ni un solo blasto**, ni promielocitos, ni mielocitos, ni eritroblastos → las
  métricas de test nunca pudieron detectar este fallo.
- Las métricas de validación se calcularon sobre un split que es **78% blastos**, no representativo.

Esto explica por qué el entrenamiento pudo parecer correcto: **las métricas se estaban midiendo mal.**

*(Comprobado: no hay imágenes duplicadas exactas entre splits, así que el problema es el reparto, no
copias filtradas.)*

---

## 4. Qué hay que hacer

### 4.1. Rehacer el split — estratificado por clase (imprescindible)

Reagrupar las ~11.651 imágenes y volver a repartir **80/10/10 estratificando por clase**, de modo que
cada clase mantenga aproximadamente la misma proporción en los tres splits. Con `scikit-learn`:

```python
from sklearn.model_selection import train_test_split
# y = clase de cada imagen (para imágenes multiclase, usar la clase minoritaria presente)
train, temp = train_test_split(items, test_size=0.2, stratify=y, random_state=42)
valid, test = train_test_split(temp, test_size=0.5, stratify=y_temp, random_state=42)
```

Reparto objetivo aproximado para la clase crítica: **Blasto ≈ 943 train / 118 valid / 118 test**
(hoy: 272 / 907 / 0).

**Verificación obligatoria antes de entrenar:** imprimir el recuento por clase de cada split y comprobar
que las proporciones son equivalentes. Si un split tiene 0 ejemplos de una clase, el reparto está mal.

### 4.2. Cuidado con el data leakage al barajar

Si varias imágenes proceden **del mismo frotis o del mismo paciente** (recortes distintos de un mismo
campo), un split aleatorio puro las repartiría entre train y test e **inflaría artificialmente** las
métricas. Si existe ese metadato de origen, hay que **agrupar por él** (`StratifiedGroupKFold`), no solo
estratificar por clase. Si no existe, conviene decirlo explícitamente para que conste como limitación.

### 4.3. Clases que probablemente no son viables

| Clase | Total en todo el dataset |
|---|---|
| Basófilo | **22** |
| Eritroblasto | **44** |

Con esos volúmenes (≈17 y ≈35 imágenes de entrenamiento tras el split) no se aprende una clase, por bien
que se entrene. Ambas puntúan en la priorización de la app. Opciones: **conseguir más ejemplos**, o
asumir y **documentar** que esas dos clases no son fiables. Conviene decidirlo antes de entrenar, no
después.

### 4.4. Entrenamiento

Con el split arreglado, la configuración estándar debería bastar; **no hace falta nada exótico**:

- YOLO11s-seg, `imgsz=640`, `epochs≈150` con early stopping (`patience≈25`).
- Augmentation por defecto de Ultralytics.
- Para el desbalance restante (Neutrófilo 4572 vs Promielocito 214): sobremuestrear las clases
  minoritarias en train, o al menos **reportar métricas por clase**, nunca solo la media global.

---

## 5. Requisitos del export ONNX (innegociables para la app)

La aplicación ya está implementada contra el contrato del modelo actual. **Si estos puntos cambian, la app
deja de funcionar** y hay que tocar código:

1. **Las 14 clases deben mantener EXACTAMENTE el mismo orden** que el `data.yaml` actual:
   `['Artefato','Basofilo','Bastonete','Blasto','Eosinofilo','Eritroblasto','Linfocito',
   'Linfocito atipico','Metamielocito','Mielocito','Monocito','Neutrofilo segmentado','Promielocito',
   'Restos celulares']`
   La app mapea **por índice**. Si el orden cambia, las células se etiquetan mal en silencio (fallo grave
   y difícil de detectar). Si cambia, **avisar explícitamente**.
2. **`imgsz=640`**, entrada `[1, 3, 640, 640]`, NCHW, RGB, normalización 0..1.
3. **Export sin NMS embebido** (`nms=False`, que es el valor por defecto) y **sin ejes dinámicos**
   (`dynamic=False`). La app aplica su propio NMS y asume la salida `[1, 50, 8400]`.
4. Comando de referencia:
   ```bash
   yolo export model=best.pt format=onnx imgsz=640 opset=12 simplify=True dynamic=False nms=False
   ```
5. Puede seguir siendo un modelo **-seg**: la app usa solo la rama de detección e ignora las máscaras.

---

## 6. Criterios de aceptación del nuevo modelo

Antes de integrarlo, entregar sobre el **test estratificado nuevo** (datos no vistos):

- [ ] Recuento por clase de los tres splits (prueba de que el reparto es correcto).
- [ ] **Métricas por clase**, no solo la media: precisión, recall y mAP50.
- [ ] **Matriz de confusión** sobre test.
- [ ] Específicamente: **recall de Blasto ≥ 0,80** en test. Es la métrica que decide si la priorización
      del producto tiene sentido; hoy es ~0,02.
- [ ] Comprobación de que el acierto en `train` y en `test` **son parecidos**. Una diferencia grande
      (como el 96% vs 2% actual) significa que el problema persiste.

---

## 7. Nota sobre el ámbito

La aplicación **no diagnostica**: sugiere una prioridad de revisión para un profesional, que siempre
decide. Aun así, un falso negativo en blasto es el peor error posible del sistema, porque justamente esos
son los casos que la herramienta existe para poner los primeros de la cola. De ahí que el recall de Blasto
sea el criterio de aceptación principal.
