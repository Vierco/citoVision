# CitoVision — Informe técnico del entrenamiento y validación del modelo

## 1. Propósito del documento

Este informe documenta el proceso seguido para desarrollar, entrenar, evaluar, exportar e integrar el modelo de inteligencia artificial del primer módulo de **CitoVision**.

CitoVision se plantea como una plataforma modular de análisis microscópico asistido por inteligencia artificial. El módulo desarrollado para el Trabajo Fin de Máster está orientado al **cribado morfológico hematológico**: analiza imágenes microscópicas, detecta y segmenta células, las clasifica y ayuda a priorizar las muestras que contienen hallazgos morfológicamente relevantes para su posterior revisión profesional.

El sistema no realiza diagnósticos ni determina enfermedades. Su finalidad es proporcionar apoyo técnico al proceso de revisión, destacando hallazgos que podrían requerir atención preferente.

---

## 2. Objetivo del modelo

El modelo debía cubrir en una única inferencia las siguientes funciones:

1. **Detección:** localizar las células presentes en la imagen.
2. **Segmentación:** delimitar el contorno de cada célula.
3. **Conteo:** calcular el número de células detectadas.
4. **Clasificación:** asignar a cada célula una categoría morfológica.
5. **Priorización:** proporcionar a CitoVision los datos necesarios para clasificar la revisión de la muestra como baja, media o alta.

La priorización no forma parte del modelo neuronal. Se implementa como lógica de negocio en CitoVision a partir de:

- la clase predicha;
- la confianza de la predicción;
- el número de hallazgos;
- la combinación de células encontradas.

---

## 3. Dataset

### 3.1. Dataset seleccionado

Se utilizó el **UNIVALI Leukocyte Dataset**, distribuido en Zenodo y preparado para tareas de segmentación con YOLO.

El conjunto completo empleado contiene:

| Partición original | Imágenes |
|---|---:|
| Train | 9.320 |
| Validation | 1.165 |
| Test | 1.166 |
| **Total** | **11.651** |

Las anotaciones utilizan el formato de segmentación de YOLO: cada objeto se representa mediante un identificador de clase y los puntos normalizados del polígono que delimita la célula.

### 3.2. Licencia

El dataset se publica bajo licencia **Creative Commons Attribution 4.0 International (CC BY 4.0)**. Su uso requiere atribuir correctamente la fuente, enlazar o identificar la licencia e indicar las modificaciones efectuadas.

En este proyecto se modificó la organización experimental del dataset mediante la creación de un nuevo reparto estratificado denominado:

```text
UNIVALI_stratified_v1
```

No se modificó el orden de las clases ni el significado de las anotaciones.

### 3.3. Clases

El modelo trabaja con las 14 clases originales, manteniendo sus identificadores:

| ID | Clase |
|---:|---|
| 0 | Artefato |
| 1 | Basofilo |
| 2 | Bastonete |
| 3 | Blasto |
| 4 | Eosinofilo |
| 5 | Eritroblasto |
| 6 | Linfocito |
| 7 | Linfocito atipico |
| 8 | Metamielocito |
| 9 | Mielocito |
| 10 | Monocito |
| 11 | Neutrofilo segmentado |
| 12 | Promielocito |
| 13 | Restos celulares |

Se mantuvo este orden durante el entrenamiento, la exportación a ONNX y la integración en CitoVision. Cambiarlo habría provocado errores en el mapeo entre los índices devueltos por el modelo y los nombres mostrados por la aplicación.

---

## 4. Herramientas utilizadas

### 4.1. Entrenamiento y análisis

- **Google Colab:** entorno de ejecución y acceso a GPU.
- **Python 3.12:** lenguaje utilizado en los cuadernos.
- **PyTorch:** framework subyacente de entrenamiento.
- **Ultralytics 8.4.98:** entrenamiento, validación y exportación de YOLO.
- **Tesla T4:** GPU empleada en el entrenamiento y en la evaluación PyTorch.
- **Pandas y NumPy:** auditoría y tratamiento de datos.
- **Pillow:** validación y lectura de imágenes.
- **OpenCV/ORB:** análisis de correspondencias visuales.
- **ImageHash/pHash:** generación inicial de candidatos visualmente similares.
- **scikit-image/SSIM:** comparación estructural de imágenes alineadas.
- **iterative-stratification:** creación del reparto multietiqueta.

### 4.2. Exportación y ejecución

- **ONNX, opset 17:** formato de intercambio del modelo.
- **onnxslim:** simplificación del grafo exportado.
- **ONNX Runtime 1.27.0:** validación del modelo ONNX.
- **CitoVision:** aplicación multiplataforma que ejecuta el modelo y presenta los resultados.

---

## 5. Selección del modelo

El dataset contiene polígonos de segmentación, por lo que se eligió un modelo de la familia YOLO con capacidad de segmentación.

En una primera comparación controlada se entrenaron durante 20 épocas:

- `YOLO11n-seg` — variante Nano;
- `YOLO11s-seg` — variante Small.

Se mantuvieron constantes el dataset, el tamaño de entrada, el número de épocas y el entorno de entrenamiento.

Resultados iniciales:

| Métrica | YOLO11n-seg | YOLO11s-seg |
|---|---:|---:|
| Precisión global | 0,794 | 0,760 |
| Recall global | 0,349 | **0,395** |
| Mask mAP50 | 0,502 | **0,547** |
| Mask mAP50-95 | 0,460 | **0,519** |
| Tiempo de inferencia comunicado | **3,0 ms** | 6,2 ms |
| Tamaño de `best.pt` | **6 MB** | 20,5 MB |

Se eligió **YOLO11s-seg** porque mejoraba el recall y las métricas de segmentación. En un sistema de cribado interesa especialmente reducir los falsos negativos, siempre sin producir una cantidad inasumible de falsos positivos.

---

## 6. Primer entrenamiento y detección de un problema metodológico

El primer modelo funcional se entrenó con el reparto original del dataset. Aunque producía resultados visualmente buenos en algunas imágenes y métricas globales aparentemente razonables, un análisis posterior reveló un problema grave: las clases estaban distribuidas de forma muy desigual entre `train`, `valid` y `test`.

Ejemplos del reparto original:

| Clase | Train | Valid | Test | Total |
|---|---:|---:|---:|---:|
| Blasto | 272 | 907 | 0 | 1.179 |
| Eosinofilo | 144 | 3 | 348 | 495 |
| Eritroblasto | 44 | 0 | 0 | 44 |
| Promielocito | 214 | 0 | 0 | 214 |
| Mielocito | 249 | 4 | 0 | 253 |

Este reparto impedía evaluar correctamente varias clases críticas. Por ejemplo, la clase **Blasto**, fundamental para la priorización, no tenía ninguna instancia en el conjunto `test`.

La conclusión fue que las métricas globales ocultaban una evaluación incompleta y que el modelo no debía considerarse final.

---

## 7. Auditoría del dataset original

Antes de reorganizar el dataset se realizó una auditoría integral.

### 7.1. Integridad

Se comprobó:

- correspondencia entre cada imagen y su archivo de etiquetas;
- ausencia de etiquetas huérfanas;
- ausencia de imágenes corruptas;
- validez de los identificadores de clase;
- validez y normalización de los polígonos;
- imágenes de fondo sin objetos anotados.

Resultado:

| Split | Imágenes | Etiquetas | Sin etiqueta | Etiquetas huérfanas | Corruptas | Anotaciones inválidas |
|---|---:|---:|---:|---:|---:|---:|
| Train | 9.320 | 9.320 | 0 | 0 | 0 | 0 |
| Valid | 1.165 | 1.165 | 0 | 0 | 0 | 0 |
| Test | 1.166 | 1.166 | 0 | 0 | 0 | 0 |

El dataset estaba íntegro. El problema no era la falta o corrupción de archivos, sino la distribución de las clases.

### 7.2. Duplicados exactos

Se calcularon hashes SHA-256 para las 11.651 imágenes.

Resultado:

```text
Grupos duplicados exactos: 0
Archivos dentro de grupos duplicados: 0
Grupos duplicados entre splits: 0
```

### 7.3. Imágenes visualmente relacionadas

El dataset contiene imágenes aumentadas o derivadas. Una copia transformada no tiene el mismo SHA-256, por lo que fue necesario buscar similitud visual.

El procedimiento fue:

1. Generación de candidatos mediante pHash tolerante a giros y volteos.
2. Verificación geométrica con características ORB.
3. Alineación mediante homografía.
4. Comparación estructural mediante SSIM.
5. Confirmación de que las etiquetas coincidían.

Resultados principales:

```text
Pares perceptualmente similares encontrados: 255
Pares perceptualmente similares entre splits originales: 4
```

La revisión visual y ORB determinó que los cuatro pares entre splits eran falsos positivos del pHash: obtenían cero correspondencias geométricas válidas.

Para agrupar únicamente variantes de alta confianza se utilizó un criterio conservador:

```text
aligned_ssim >= 0,92
overlap_ratio >= 0,85
inlier_ratio >= 0,97
inliers >= 50
mismas clases anotadas
```

Se identificaron:

```text
36 grupos de alta confianza
72 imágenes incluidas en grupos
tamaño máximo de grupo: 2
0 grupos con etiquetas inconsistentes
0 grupos que atravesaban los splits originales
```

Cada grupo se trató como una unidad indivisible durante el nuevo reparto. Esta decisión reduce el riesgo de colocar una imagen en entrenamiento y una variante casi idéntica en validación o test.

El criterio fue deliberadamente estricto. Es posible que algunas variantes más transformadas no fueran agrupadas; se prefirió ese riesgo a unir imágenes diferentes por error.

---

## 8. Creación de `UNIVALI_stratified_v1`

Se creó un nuevo reparto mediante estratificación multietiqueta. Las 36 parejas relacionadas se mantuvieron como unidades indivisibles.

El objetivo fue:

```text
Train: 80 %
Valid: 10 %
Test: 10 %
```

Reparto obtenido:

| Split | Imágenes | Porcentaje |
|---|---:|---:|
| Train | 9.322 | 80,01 % |
| Valid | 1.164 | 9,99 % |
| Test | 1.165 | 10,00 % |

Distribución final de instancias:

| ID | Clase | Train | Valid | Test | Total |
|---:|---|---:|---:|---:|---:|
| 0 | Artefato | 128 | 16 | 16 | 160 |
| 1 | Basofilo | 18 | 2 | 2 | 22 |
| 2 | Bastonete | 671 | 85 | 81 | 837 |
| 3 | Blasto | 934 | 124 | 121 | 1.179 |
| 4 | Eosinofilo | 393 | 50 | 52 | 495 |
| 5 | Eritroblasto | 35 | 4 | 5 | 44 |
| 6 | Linfocito | 2.167 | 267 | 267 | 2.701 |
| 7 | Linfocito atipico | 456 | 56 | 57 | 569 |
| 8 | Metamielocito | 359 | 43 | 43 | 445 |
| 9 | Mielocito | 203 | 26 | 24 | 253 |
| 10 | Monocito | 439 | 55 | 55 | 549 |
| 11 | Neutrofilo segmentado | 3.655 | 458 | 459 | 4.572 |
| 12 | Promielocito | 171 | 21 | 22 | 214 |
| 13 | Restos celulares | 790 | 98 | 98 | 986 |

Todas las clases aparecen en los tres splits y ninguno de los grupos relacionados fue dividido.

El dataset reorganizado se guardó como:

```text
UNIVALI_stratified_v1
```

Se creó también una copia comprimida con huella SHA-256 coincidente para garantizar su integridad y conservación.

---

## 9. Entrenamiento definitivo

### 9.1. Configuración

Modelo base:

```text
YOLO11s-seg
```

Configuración principal:

```text
epochs: 150 máximo
patience: 25
imgsz: 640
batch: 16
workers: 2
device: GPU 0
seed: 42
deterministic: true
```

El entrenamiento se detuvo mediante *early stopping* después de **141 épocas**, con una duración comunicada de **12,777 horas**. El modelo seleccionado fue `best.pt`, no `last.pt`.

### 9.2. Incidencias durante el entrenamiento

#### Desconexión de Colab

La sesión gratuita de Colab se desconectó durante el entrenamiento y posteriormente se agotó temporalmente la cuota gratuita de GPU. Se resolvió mediante:

- almacenamiento de `last.pt` en Google Drive;
- contratación de Colab Pro para recuperar acceso a GPU;
- reanudación desde el último checkpoint.

#### Incompatibilidad al reanudar

Un primer intento de reanudación utilizó una versión distinta de Ultralytics. El proceso intentó cargar `coco8-seg.yaml`, cambió el número de clases a 80 y falló al restaurar el optimizador:

```text
loaded state dict has a different number of parameter groups
```

La inspección del checkpoint mostró:

```text
dataset: /content/UNIVALI_stratified_v1/data.yaml
epochs: 150
batch: 16
imgsz: 640
seed: 42
epoch guardada: 8
versión Ultralytics: 8.4.98
```

La solución fue:

1. instalar exactamente Ultralytics `8.4.98`;
2. restaurar el dataset en la misma ruta local;
3. montar Google Drive;
4. reanudar desde `last.pt`.

El entrenamiento continuó correctamente desde la época 10.

---

## 10. Resultados del modelo PyTorch

### 10.1. Validación

Resultados globales sobre `valid`:

| Métrica | Box | Mask |
|---|---:|---:|
| Precision | 0,958 | 0,958 |
| Recall | 0,964 | 0,964 |
| mAP50 | 0,982 | 0,983 |
| mAP50-95 | 0,967 | 0,966 |

### 10.2. Test

Resultados globales sobre el nuevo `test`:

| Métrica | Box | Mask |
|---|---:|---:|
| Precision | 0,953 | 0,953 |
| Recall | 0,964 | 0,964 |
| mAP50 | 0,980 | 0,980 |
| mAP50-95 | 0,965 | 0,962 |

La similitud entre validación y test indica que no existe una caída relevante entre ambas particiones.

Resultados PyTorch por clase en `test`:

| Clase | Instancias | Precision | Recall | Box mAP50 | Box mAP50-95 |
|---|---:|---:|---:|---:|---:|
| Artefato | 16 | 0,976 | 0,938 | 0,935 | 0,863 |
| Basofilo | 2 | 0,915 | 1,000 | 0,995 | 0,995 |
| Bastonete | 81 | 0,949 | 0,919 | 0,957 | 0,955 |
| Blasto | 121 | 0,981 | 0,983 | 0,993 | 0,980 |
| Eosinofilo | 52 | 0,981 | 0,985 | 0,995 | 0,986 |
| Eritroblasto | 5 | 0,964 | 1,000 | 0,995 | 0,995 |
| Linfocito | 267 | 0,974 | 0,993 | 0,995 | 0,979 |
| Linfocito atipico | 57 | 0,982 | 0,950 | 0,991 | 0,969 |
| Metamielocito | 43 | 0,975 | 0,977 | 0,988 | 0,978 |
| Mielocito | 24 | 0,839 | 0,867 | 0,952 | 0,941 |
| Monocito | 55 | 0,944 | 0,982 | 0,982 | 0,977 |
| Neutrofilo segmentado | 459 | 0,978 | 0,983 | 0,993 | 0,985 |
| Promielocito | 22 | 0,914 | 0,969 | 0,968 | 0,958 |
| Restos celulares | 98 | 0,969 | 0,943 | 0,985 | 0,943 |

El recall de **Blasto**, una de las clases más importantes para la priorización, fue de `0,983`.

Las métricas de **Basofilo** y **Eritroblasto** son únicamente orientativas debido al reducido número de ejemplos disponibles en el conjunto de evaluación: 2 y 5 instancias, respectivamente. Un resultado de 100 % sobre tan pocos casos no permite afirmar una generalización robusta.

---

## 11. Exportación a ONNX

El modelo `best.pt` se exportó con:

```text
format: onnx
opset: 17
imgsz: 640
simplify: true
dynamic: false
nms: false
```

Características del modelo exportado:

```text
Entrada: (1, 3, 640, 640), formato BCHW
Salida de detección: (1, 50, 8400)
Salida de prototipos de máscara: (1, 32, 160, 160)
Tamaño aproximado: 38,7 MB
```

Se mantuvo `nms=false` porque CitoVision realiza el postprocesado y NMS en la aplicación.

### 11.1. Problema al validar ONNX

Ultralytics no identificó inicialmente la tarea del ONNX y asumió `detect`. Esto produjo índices de clase inválidos:

```text
IndexError: index 22 is out of bounds for axis 0 with size 15
```

Se resolvió especificando explícitamente:

```python
YOLO(model_path, task="segment")
```

La validación ONNX se realizó con `batch=1`, ya que el modelo tiene entrada estática.

### 11.2. Resultados ONNX sobre test

| Métrica | PyTorch | ONNX | Diferencia |
|---|---:|---:|---:|
| Precision global | 0,953 | 0,928 | −0,025 |
| Recall global | 0,964 | 0,954 | −0,010 |
| Box mAP50 | 0,980 | 0,978 | −0,002 |
| Box mAP50-95 | 0,965 | 0,963 | −0,002 |
| Mask mAP50 | 0,980 | 0,978 | −0,002 |
| Mask mAP50-95 | 0,962 | 0,958 | −0,004 |

Para Blasto:

| Métrica | PyTorch | ONNX |
|---|---:|---:|
| Precision | 0,981 | 0,929 |
| Recall | 0,983 | 0,980 |
| mAP50 | 0,993 | 0,985 |
| Box mAP50-95 | 0,980 | 0,971 |

La caída fue limitada y el recall de Blasto se mantuvo en `0,980`. El modelo ONNX se consideró técnicamente aceptable.

La velocidad de ONNX medida con `CPUExecutionProvider` no debe compararse directamente con PyTorch sobre Tesla T4:

```text
PyTorch/T4: 12,3 ms de inferencia por imagen
ONNX/CPU: 422,9 ms de inferencia por imagen
```

Estas cifras corresponden a hardware y proveedores de ejecución diferentes.

---

## 12. Equivalencia PyTorch, ONNX y CitoVision

### 12.1. Prueba con `img_011642.jpg`

Esta imagen pertenece al `train` del nuevo reparto. Se utilizó exclusivamente para comprobar equivalencia técnica, no para medir generalización.

Resultados:

| Entorno | Clase | Confianza |
|---|---|---:|
| PyTorch | Linfocito | 0,9490 |
| PyTorch | Neutrofilo segmentado | 0,9401 |
| ONNX | Linfocito | 0,9490 |
| ONNX | Neutrofilo segmentado | 0,9401 |
| CitoVision | Linfocito | 95 % |
| CitoVision | Neutrofilo segmentado | 94 % |

PyTorch y ONNX devolvieron exactamente las mismas clases, confianzas y cajas. CitoVision reprodujo los valores con redondeo visual.

Esta prueba validó:

- carga del ONNX correcto;
- preprocesado coherente;
- orden de clases;
- lectura de confianzas;
- postprocesado;
- equivalencia básica entre los tres entornos.

### 12.2. Error inicial en la confianza mostrada

En una primera versión, CitoVision mostraba un 50 % para cada una de dos células. No era la confianza del modelo, sino el porcentaje que representaba cada clase sobre el total detectado.

Se corrigió separando:

- confianza de la detección;
- porcentaje de distribución de clases.

---

## 13. Pruebas funcionales sobre el nuevo test

### 13.1. `img_002149.jpg`

Ground truth:

- 1 Bastonete;
- 1 Eosinofilo;
- 1 Metamielocito;
- 1 Promielocito.

Resultado de CitoVision:

| Clase mostrada | Cantidad | Confianza |
|---|---:|---:|
| Neutrófilo en banda/cayado | 1 | 91 % |
| Eosinófilo | 1 | 96 % |
| Metamielocito | 1 | 97 % |
| Promielocito | 1 | 68 % |

La denominación «Neutrófilo en banda/cayado» corresponde a la clase `Bastonete`. Las cuatro anotaciones fueron detectadas correctamente.

### 13.2. `img_001304.jpg`

Ground truth:

- 1 Neutrofilo segmentado;
- 1 Promielocito;
- 2 Mielocitos.

Con un umbral global de `0,25`, PyTorch, ONNX y CitoVision detectaban:

| Clase | Confianza |
|---|---:|
| Mielocito | 0,9728 |
| Neutrofilo segmentado | 0,8536 |

Al reducir temporalmente el umbral a `0,05`, aparecieron también:

| Clase | Confianza |
|---|---:|
| Mielocito | 0,0907 |
| Promielocito | 0,0897 |

El modelo sí generaba las cuatro detecciones, pero dos quedaban ocultas por el umbral global.

Esta observación condujo al análisis formal de umbrales usando exclusivamente el conjunto `valid`.

---

## 14. Ajuste de umbrales por clase

### 14.1. Clases críticas

Se consideraron críticas para el mecanismo de priorización:

| ID | Clase |
|---:|---|
| 3 | Blasto |
| 7 | Linfocito atipico |
| 8 | Metamielocito |
| 9 | Mielocito |
| 12 | Promielocito |

### 14.2. Metodología

Se ejecutó el modelo sobre `valid` con un umbral mínimo de `0,05`. Después se compararon dos configuraciones:

- clases críticas con umbral `0,08`;
- clases críticas con umbral `0,10`;
- resto de clases con umbral `0,25`;
- coincidencia correcta si la clase era la misma y el IoU era al menos `0,50`.

### 14.3. Resultado con umbral crítico 0,08

| Clase | TP | FP | FN | Precision | Recall | F1 |
|---|---:|---:|---:|---:|---:|---:|
| Blasto | 123 | 18 | 1 | 0,872 | 0,992 | 0,928 |
| Linfocito atipico | 53 | 5 | 3 | 0,914 | 0,946 | 0,930 |
| Metamielocito | 42 | 11 | 1 | 0,792 | 0,977 | 0,875 |
| Mielocito | 26 | 6 | 0 | 0,812 | 1,000 | 0,897 |
| Promielocito | 20 | 8 | 1 | 0,714 | 0,952 | 0,816 |
| **Total** | **264** | **48** | **6** | **0,846** | **0,978** | — |

### 14.4. Resultado con umbral crítico 0,10

| Clase | TP | FP | FN | Precision | Recall | F1 |
|---|---:|---:|---:|---:|---:|---:|
| Blasto | 123 | 18 | 1 | 0,872 | 0,992 | 0,928 |
| Linfocito atipico | 53 | 4 | 3 | 0,930 | 0,946 | 0,938 |
| Metamielocito | 42 | 9 | 1 | 0,824 | 0,977 | 0,894 |
| Mielocito | 26 | 6 | 0 | 0,812 | 1,000 | 0,897 |
| Promielocito | 20 | 8 | 1 | 0,714 | 0,952 | 0,816 |
| **Total** | **264** | **45** | **6** | **0,854** | **0,978** | — |

Bajar de `0,10` a `0,08` no recuperó ningún verdadero positivo adicional en `valid` y añadió tres falsos positivos. Por tanto, `0,10` fue elegido como umbral crítico principal.

---

## 15. Lógica final de umbrales

La política implementada en CitoVision es:

```text
Clases no críticas con confianza >= 0,25
→ detección estándar

Clases críticas con confianza >= 0,10
→ detección estándar

Clases críticas con confianza entre 0,08 y 0,10
→ posible hallazgo de baja confianza; requiere revisión

Confianza por debajo del umbral aplicable
→ detección descartada
```

Las detecciones de baja confianza:

- se muestran separadas de las detecciones principales;
- incluyen una advertencia explícita;
- no se contabilizan como resultados confirmados;
- pueden modificar la prioridad, pero no se tratan igual que una detección estándar.

En `img_001304.jpg`, la interfaz final muestra:

- Mielocito al 97 %;
- Neutrofilo segmentado al 85 %;
- posible Mielocito al 9 %;
- posible Promielocito al 9 %.

---

## 16. Lógica de prioridad

Se detectó un segundo problema: una clase crítica con confianza baja podía elevar directamente la prioridad a Alta.

Se corrigió separando:

- **severidad potencial de la clase**;
- **fiabilidad de la detección**.

Regla conceptual final:

```text
Hallazgo crítico con confianza suficiente
→ puede elevar la prioridad a Alta

Hallazgo crítico en la franja de baja confianza
→ puede impedir que la prioridad quede en Baja
→ puede elevarla a Media
→ no eleva por sí solo la prioridad a Alta

Múltiples hallazgos críticos o combinaciones relevantes
→ pueden elevar la prioridad según las reglas de negocio
```

Por tanto, un posible Blasto con una confianza del 9 % no se presenta como un hallazgo sólido ni eleva automáticamente la prioridad a Alta. Debe producir una advertencia y, como máximo por sí solo, una prioridad Media.

Esta separación reduce el riesgo de alarmas excesivas sin ocultar hallazgos potencialmente relevantes.

---

## 17. Problemas encontrados y soluciones

| Problema | Consecuencia | Solución |
|---|---|---|
| Dataset inicial de clasificación con células recortadas | No permitía detectar varias células en una imagen | Sustitución por UNIVALI con segmentación y 14 clases |
| Split original muy desbalanceado | Validación y test no evaluaban clases críticas | Auditoría y creación de `UNIVALI_stratified_v1` |
| Posibles variantes aumentadas | Riesgo de fuga entre splits | pHash, ORB, homografía, SSIM y agrupación conservadora |
| Desconexión y cuota de GPU de Colab | Interrupción del entrenamiento | Checkpoints en Drive, Colab Pro y reanudación |
| Versión distinta de Ultralytics | Fallo del optimizador y carga errónea de COCO | Reinstalación exacta de Ultralytics 8.4.98 |
| ONNX interpretado como detección | Índices de clase inválidos | Especificar `task="segment"` |
| Confianza mostrada como porcentaje de distribución | Valores erróneos en la interfaz | Separar confianza y porcentaje de clase |
| Umbral global 0,25 | Ocultaba hallazgos críticos de baja confianza | Evaluación en `valid` y umbrales diferenciados |
| Hallazgo crítico débil elevaba a Alta | Sobrerreacción del motor de prioridad | Prioridad sensible a clase y nivel de confianza |
| Rendimiento pobre en imágenes externas | Cambio de dominio | Documentación como limitación y línea futura |

---

## 18. Limitaciones

### 18.1. Sin validación clínica

El modelo no ha sido validado clínicamente ni debe utilizarse para diagnosticar enfermedades. Las reglas de prioridad son reglas de ingeniería para el MVP y requieren revisión por especialistas antes de un uso sanitario real.

### 18.2. Generalización externa

El modelo funciona de forma muy sólida dentro del dominio visual de UNIVALI, pero mostró una caída importante en imágenes procedentes de atlas externos con:

- otras tinciones;
- otros microscopios y cámaras;
- diferente balance de color;
- distinto nivel de zoom;
- diferente preparación de las muestras.

Este fenómeno se identifica como *domain shift*. Los resultados internos no garantizan el mismo comportamiento en otros laboratorios.

### 18.3. Clases minoritarias

Basofilo y Eritroblasto tienen muy pocos ejemplos:

```text
Basofilo: 22 instancias totales; 2 en valid y 2 en test
Eritroblasto: 44 instancias totales; 4 en valid y 5 en test
```

Sus métricas no son estadísticamente robustas.

También deben interpretarse con cautela las clases con pocas decenas de instancias de evaluación, como Mielocito y Promielocito.

### 18.4. Variantes relacionadas

Se agruparon 36 parejas de imágenes derivadas con un criterio de alta confianza. El umbral conservador reduce falsos agrupamientos, pero no garantiza que todas las posibles variantes del mismo original hayan sido identificadas.

Las métricas excepcionalmente altas deben interpretarse dentro de esta limitación y de la homogeneidad del dataset.

### 18.5. Validación externa orientativa

Las imágenes externas recopiladas no disponen de anotaciones homogéneas confirmadas por un hematólogo dentro del proyecto. Por ello, su uso es exploratorio y cualitativo, no una evaluación clínica formal.

### 18.6. Prioridad no validada por especialistas

La correspondencia entre clases, confianza y prioridad se ha diseñado para el MVP. Antes de cualquier uso real deberá definirse y validarse con profesionales de hematología.

---

## 19. Decisión de validez para el MVP

El modelo se considera **técnicamente válido para su integración en el MVP de CitoVision** por los siguientes motivos:

- el problema del split original fue identificado y corregido;
- el nuevo reparto es estratificado, reproducible y mantiene juntas las variantes de alta confianza;
- todas las clases aparecen en train, valid y test;
- el modelo fue evaluado de forma separada sobre validación y test;
- el recall PyTorch de Blasto en test fue `0,983`;
- el recall ONNX de Blasto fue `0,980`;
- PyTorch y ONNX coincidieron exactamente en la prueba controlada;
- CitoVision reprodujo clases y confianzas;
- los umbrales diferenciados se eligieron usando `valid`;
- la aplicación distingue detecciones principales y hallazgos de baja confianza;
- el motor de prioridad considera tanto la clase como la fiabilidad.

Esta aceptación se limita al alcance experimental y académico del TFM. No implica:

- validación clínica;
- autorización como producto sanitario;
- capacidad diagnóstica;
- generalización garantizada a otros centros;
- rendimiento suficiente para uso asistencial real.

---

## 20. Conclusiones

El desarrollo del modelo de CitoVision no consistió únicamente en entrenar una red neuronal. El trabajo principal incluyó:

- definir un problema útil y acotado;
- encontrar un dataset compatible con detección, segmentación y clasificación;
- auditar críticamente la calidad experimental de sus particiones;
- detectar que las primeras métricas no evaluaban adecuadamente las clases críticas;
- reconstruir un reparto estratificado;
- proteger grupos de imágenes visualmente relacionadas;
- entrenar un modelo reproducible;
- validar PyTorch y ONNX;
- integrar el modelo en una aplicación real;
- detectar problemas de presentación, umbral y priorización;
- resolverlos mediante pruebas controladas.

El resultado es un modelo funcional para el dominio UNIVALI y un flujo completo:

```text
Imagen microscópica
→ segmentación y detección
→ clasificación
→ conteo
→ evaluación de confianza
→ priorización
→ presentación en CitoVision
```

La experiencia también demuestra una conclusión relevante para el TFM: unas métricas globales aparentemente correctas no bastan. La distribución de clases, la independencia de los splits, el comportamiento por clase, la exportación y la integración deben verificarse de forma explícita.

---

## 21. Siguientes pasos

### Imprescindibles para cerrar el TFM

- conservar `best.pt`, `best.onnx`, el dataset estratificado y los CSV de auditoría;
- documentar las versiones exactas del entorno;
- añadir capturas de la matriz de confusión y de casos representativos;
- completar pruebas funcionales de la aplicación;
- documentar privacidad, seguridad, licencias y limitaciones;
- preparar una demo reproducible sin dependencia de red.

### Mejoras posteriores al MVP

- ampliar las clases minoritarias con datos independientes;
- incorporar imágenes de diferentes laboratorios y dispositivos;
- validar el modelo sobre un conjunto externo anotado por especialistas;
- calibrar probabilidades por clase;
- estudiar umbrales específicos para cada clase crítica;
- evaluar cuantización y aceleración del ONNX en cada plataforma;
- analizar falsos positivos y falsos negativos con especialistas;
- convertir las reglas de prioridad en un protocolo validado;
- evaluar el sistema a nivel de muestra o paciente, no solo de célula;
- estudiar los requisitos regulatorios aplicables antes de cualquier uso sanitario.

---

## 22. Artefactos finales

Modelo PyTorch:

```text
citoModel3/resultados/
yolo11s_split_estratificado_v1/
weights/best.pt
```

Modelo integrado en CitoVision:

```text
citoModel3/resultados/
yolo11s_split_estratificado_v1/
weights/best.onnx
```

Dataset definitivo:

```text
citoModel3/datasets/UNIVALI_stratified_v1
```

Resultados de evaluación PyTorch:

```text
citoModel3/resultados/
yolo11s_split_estratificado_v1_test
```

Resultados de evaluación ONNX:

```text
citoModel3/resultados/
yolo11s_split_estratificado_v1_onnx_test
```

Auditoría:

```text
citoModel3/auditoria_UNIVALI
```

---

## 23. Declaración final

> El modelo se considera técnicamente válido para su integración en el MVP de CitoVision, al haber demostrado un rendimiento adecuado sobre el conjunto de evaluación interno, equivalencia entre PyTorch y ONNX y coherencia funcional dentro de la aplicación. Esta aceptación se limita al alcance experimental del Trabajo Fin de Máster y no constituye validación clínica ni garantiza la generalización a imágenes obtenidas con otros microscopios, tinciones, cámaras o condiciones de adquisición.

