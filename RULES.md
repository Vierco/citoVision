# RULES.md

## Stack tecnológico oficial

| Categoría | Tecnología oficial |
|---|---|
| Inyección de dependencias | Koin |
| Networking | Ktor Client |
| Logging | Napier |
| Serialización | kotlinx.serialization |
| Persistencia | Room Multiplatform |
| Carga de imágenes | Coil 3 |
| UI | Compose Multiplatform + Material 3 |
| Concurrencia | Kotlin Coroutines + Flow |
| Testing | kotlin.test |

------------------------------------------------------------------------

## Inyección de Dependencias --- Koin

-   Koin es el framework oficial de inyección de dependencias.
-   Un módulo por feature y módulos transversales para servicios
    compartidos.
-   Prohibido utilizar Service Locator manual.
-   Utilizar `single` para dependencias compartidas y `factory` cuando
    se requiera una nueva instancia.
-   Los ViewModels deberán resolverse mediante Koin.
-   Cada plataforma aportará sus dependencias específicas mediante su
    módulo de plataforma.
-   La inicialización de Koin se realizará una única vez por plataforma.

> Implementación detallada: consultar el Skill correspondiente.

------------------------------------------------------------------------

## Networking --- Ktor

-   Ktor Client es la librería oficial de networking.
-   Existirá una única instancia de `HttpClient`, configurada de forma
    centralizada e inyectada mediante Koin.
-   Ningún Repository ni DataSource podrá crear instancias propias de
    `HttpClient`.
-   Los engines serán específicos de cada plataforma mediante
    `expect/actual`.
-   La configuración del cliente será única para toda la aplicación e
    incluirá:
    -   Serialización mediante `kotlinx.serialization`.
    -   Timeouts explícitos.
    -   Logging centralizado.
    -   Interceptors comunes.
-   Todas las peticiones deberán utilizar el cliente inyectado.
-   Los DataSources remotos devolverán siempre `Result` o `Either`.
-   Las URLs base y los endpoints estarán centralizados.
-   Los DTOs permanecerán exclusivamente en la capa `data`.

> Implementación detallada: consultar el Skill correspondiente.

------------------------------------------------------------------------

## Logging --- Napier

-   Napier es la librería oficial de logging.
-   Todo el logging deberá realizarse mediante Napier.
-   Prohibido utilizar `println`, `print`, `Log.*`, `NSLog` o
    equivalentes.
-   Nunca registrar información sensible.
-   En producción solo deberán mantenerse los niveles de log necesarios.

------------------------------------------------------------------------

## Serialización --- kotlinx.serialization

-   `kotlinx.serialization` es la librería oficial de serialización.
-   Todas las clases serializables deberán utilizar `@Serializable`.
-   Prohibido utilizar Gson, Moshi u otras librerías basadas en
    reflexión.
-   La configuración de `Json` será única y centralizada.
-   Los DTOs únicamente existirán en la capa `data`.

------------------------------------------------------------------------

## Persistencia --- Room Multiplatform

-   Room Multiplatform es la solución oficial de persistencia local.
-   Las entidades de persistencia únicamente existirán en `data`.
-   Todo acceso a la base de datos se realizará mediante Repositories y
    DataSources.
-   Las migraciones deberán mantenerse versionadas.
-   Prohibido utilizar migraciones destructivas en producción.
-   Las implementaciones específicas de plataforma utilizarán
    `expect/actual`.

> Implementación detallada: consultar el Skill correspondiente.

------------------------------------------------------------------------

## Imágenes --- Coil 3

-   Coil 3 es la librería oficial para carga de imágenes.
-   Todas las imágenes remotas deberán cargarse mediante Coil.
-   Prohibido descargar imágenes manualmente.
-   El `ImageLoader` será único e inyectado mediante Koin.
-   Toda imagen remota deberá contemplar estados de carga y error.

------------------------------------------------------------------------

## UI --- Material 3

-   Toda la UI utilizará Compose Multiplatform + Material 3.
-   El tema será único y centralizado.
-   Prohibido hardcodear colores, tipografías o dimensiones.
-   Todos los textos procederán de recursos.
-   Los Composables no accederán directamente a Repositories ni
    DataSources.

------------------------------------------------------------------------

## Manejo de errores --- Result / Either

-   Toda operación susceptible de fallo devolverá `Result` o `Either`.
-   Prohibido utilizar excepciones como mecanismo habitual de control de
    flujo.
-   Las excepciones se capturarán en el límite adecuado de cada capa y
    se transformarán en errores de dominio o aplicación.
-   Los ViewModels traducirán los resultados a `UiState`.

------------------------------------------------------------------------

## Interceptors

Toda petición HTTP deberá pasar por interceptors centralizados.

Como mínimo:

-   Autenticación.
-   Logging.
-   Mapeo de errores.
-   Reintentos cuando proceda.

Queda prohibido implementar esta lógica manualmente en cada Repository o
DataSource.

> Implementación detallada: consultar el Skill correspondiente.

------------------------------------------------------------------------

## Patrón Factory

-   Utilizar el patrón Factory cuando existan múltiples estrategias de
    construcción de un mismo tipo.
-   No introducir Factories innecesarias cuando una única implementación
    sea suficiente.

------------------------------------------------------------------------

## Concurrencia --- Coroutines & Flow

-   Toda operación asíncrona utilizará `suspend` o `Flow`.
-   Los `CoroutineDispatcher` deberán inyectarse mediante DI.
-   Prohibido utilizar `GlobalScope`.
-   Utilizar `Flow` para datos observables y `suspend` para operaciones
    puntuales.
-   Respetar la cancelación cooperativa.

------------------------------------------------------------------------

## Convenciones Kotlin

-   Seguir la guía oficial de estilo de Kotlin.
-   `val` por defecto; `var` solo cuando sea necesario.
-   Prohibido utilizar `!!` salvo justificación excepcional.
-   Favorecer la inmutabilidad.
-   Funciones pequeñas y con responsabilidad única.

------------------------------------------------------------------------

## Control de versiones

-   Commits siguiendo Conventional Commits.
-   Un Pull Request por feature o corrección.
-   Todo Pull Request deberá cumplir este documento.

------------------------------------------------------------------------

## Checklist previo al merge

-   No existen instancias de `HttpClient` fuera del módulo de
    networking.
-   Toda petición utiliza el cliente configurado centralmente.
-   No existen excepciones cruzando capas sin controlar.
-   DTOs y entidades permanecen dentro de la capa `data`.
-   Todo el logging utiliza Napier.
-   No existen colores ni textos hardcodeados en la UI.
-   Se utiliza Factory únicamente cuando procede.
-   Se han añadido o actualizado los tests correspondientes.
-   No existen secretos, credenciales ni tokens en el código fuente.
