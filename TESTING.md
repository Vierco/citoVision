Testing

Consideraciones generales

Estilo de los tests

* Los tests seguirán el patrón Given / When / Then para mejorar su legibilidad y mantenibilidad.
* Se utilizarán las anotaciones @BeforeTest y @AfterTest siempre que resulte aconsejable para preparar o limpiar el entorno de ejecución de los tests.

⸻

Framework de testing

Inicialmente se utilizará la herramienta oficial y estándar de Kotlin Multiplatform:

kotlin.test

Este framework viene integrado directamente con Kotlin y proporciona las anotaciones esenciales:

* @Test
* @BeforeTest
* @AfterTest

Además, incluye las funciones de aserción más habituales:

* assertEquals()
* assertTrue()
* assertFalse()
* assertFailsWith()
* etc.

Todo ello funciona de forma idéntica en todas las plataformas objetivo de Kotlin Multiplatform:

* Android
* iOS
* JVM
* Desktop
* JavaScript
* WebAssembly (Wasm)

Motores utilizados por plataforma

Aunque el código de los tests se escriba utilizando kotlin.test, internamente cada plataforma utiliza su propio motor de ejecución:

Plataforma	Motor utilizado
Android	JUnit 4 o JUnit 5
JVM	JUnit o Kotest
iOS / Kotlin Native	Runtime de Kotlin/Native
JavaScript / Wasm	Frameworks como Mocha o Jest

⸻

Mocking

Cuando sea necesario simular dependencias (Mocking), se utilizarán herramientas específicamente diseñadas para Kotlin Multiplatform.

Las opciones recomendadas son:

* Mockative
* Mokkery

Estas herramientas sustituyen a Mockito o MockK, ya que estos últimos están orientados principalmente al ecosistema JVM/Android y no ofrecen soporte multiplataforma completo.

⸻

Herramientas complementarias

Si durante el desarrollo fuese conveniente incorporar herramientas adicionales de testing, deberán proponerse previamente indicando:

* Qué problema resuelven.
* Ventajas que aportan.
* Compatibilidad con Kotlin Multiplatform.
* Impacto sobre el proyecto.

⸻

1. Alcance mínimo deseable de testing

Se seguirá la pirámide clásica de testing.

Tipo de test	Cobertura recomendada
Tests Unitarios	60–70 %
Tests de Integración	20–30 %
Tests End-to-End (E2E)	5–10 %

Tests Unitarios

Constituyen la base del sistema de testing y deben cubrir la mayor parte de la lógica de negocio.

Tests de Integración

Verifican el comportamiento conjunto de varios componentes trabajando entre sí.

Tests End-to-End (E2E)

Comprueban el funcionamiento completo de la aplicación, incluyendo backend, bases de datos y servicios externos.

Los tests End-to-End únicamente se desarrollarán cuando el desarrollador los solicite expresamente.

⸻

2. Métricas de cobertura

Como objetivo general del proyecto se establecen las siguientes métricas:

Métrica	Objetivo
Statements (Sentencias)	≥ 80 %
Branches (Ramas)	≥ 80 % (cuando la herramienta lo permita)
Functions (Funciones)	Idealmente 100 %

La cobertura porcentual no garantiza por sí sola la calidad del código. Es únicamente un indicador cuantitativo.

⸻

3. Calidad del testing

Además de la cobertura, se priorizará la calidad de los tests.

Se considera que un buen conjunto de tests debe:

* Ser sencillo de entender.
* Ser fácil de mantener.
* Ser determinista (sin resultados aleatorios).
* Ser rápido de ejecutar.
* Facilitar el refactoring con seguridad.
* Detectar regresiones de forma fiable.

Una tarea de desarrollo no se considerará finalizada hasta disponer de una batería de tests adecuada.

⸻

Organización de los tests

La estructura de la carpeta test deberá replicar la organización del código existente en main.

src
├── main
│   └── kotlin
│       └── feature
│           ├── presentation
│           ├── application
│           ├── domain
│           └── infrastructure
│
└── test
    └── kotlin
        └── feature
            ├── presentation
            ├── application
            ├── domain
            └── infrastructure

De esta forma, cada clase tendrá su correspondiente clase de test en la misma ubicación relativa dentro de la jerarquía del proyecto, facilitando la navegación y el mantenimiento.