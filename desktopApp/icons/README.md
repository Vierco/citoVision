# Iconos de la aplicación Desktop

`citovision-1024.png` es la **fuente de verdad**: 1024×1024 RGBA, con el arte ocupando el 80,5 % del
lienzo dentro de un squircle centrado, según la rejilla de iconos de macOS de Apple. Todo lo demás se
deriva de él, así que cualquier rediseño empieza por sustituir este fichero.

Derivados en uso:

- `citovision.icns` — icono del `.app` y del `.dmg`. Lo consume `compose.desktop.nativeDistributions.macOS`.
- `../src/desktopMain/resources/icons/citovision.png` (512×512) — icono **en ejecución**: el Dock de macOS
  vía `java.awt.Taskbar` y la barra de tareas de Windows/Linux vía el parámetro `icon` de la ventana.
  No sale del `.icns`; son caminos distintos y hay que regenerar los dos.

## Regenerar tras cambiar el arte

```bash
cd desktopApp/icons
ISET=$(mktemp -d)/citovision.iconset && mkdir -p "$ISET"
for size in 16 32 128 256 512; do
  sips -z "$size" "$size" citovision-1024.png --out "$ISET/icon_${size}x${size}.png"
  sips -z $((size * 2)) $((size * 2)) citovision-1024.png --out "$ISET/icon_${size}x${size}@2x.png"
done
iconutil -c icns "$ISET" -o citovision.icns
sips -z 512 512 citovision-1024.png --out ../src/desktopMain/resources/icons/citovision.png
```

`iconutil` es estricto con los nombres del `.iconset`: si alguno no encaja con el patrón
`icon_<w>x<h>[@2x].png` falla con un escueto «Failed to generate ICNS» sin decir cuál.
