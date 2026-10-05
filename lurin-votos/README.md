# Lurín vota

Seguimiento automático del conteo de la ONPE para el distrito de Lurín.
Java 17 + GitHub Actions (cada 5 min) + GitHub Pages.

## Puesta en marcha
1. Crea un repositorio en GitHub y sube todo este proyecto.
2. **Settings → Pages**: Source = *Deploy from a branch*, rama `main`, carpeta `/docs`.
3. **Settings → Actions → General → Workflow permissions**: *Read and write permissions*.
4. **Prueba con datos falsos**: Settings → Secrets and variables → Actions → *Variables* → crea `DEMO` = `true`.
   Ve a la pestaña Actions → "Actualizar resultados" → *Run workflow* varias veces y abre tu página.
5. **El domingo (datos reales)**: cambia `DEMO` a `false` (o bórrala), borra el contenido de `lecturas` en
   `docs/datos.json`, y crea el *Secret* `ONPE_URL` con la dirección que entrega los resultados de Lurín.
6. Ajusta `parsear()` en `Recolector.java` a los nombres de campo reales.

## Cómo encontrar ONPE_URL
Abre resultadoelectoral.onpe.gob.pe, entra a los resultados de Lurín, abre las herramientas de desarrollador
(F12 → pestaña Network → Fetch/XHR), recarga y busca la petición que devuelve los votos en JSON.
Copia esa URL. Respeta las condiciones de uso de la ONPE y no bajes de 5 minutos entre consultas.

## Probar en tu PC
    set DEMO=true && mvn -q compile exec:java    (Windows)
    DEMO=true mvn -q compile exec:java           (Linux/Mac)

## ONPE_URL de Lurín (distrital, ubigeo 140113)
    https://resultadoelectoral.onpe.gob.pe/presentacion-backend/eleccion-distrital/participantes-ubicacion-geografica-nombre?idEleccion=4&idAmbitoGeografico=1&tipoFiltro=ubigeo_nivel_03&ubigeoNivel1=140000&ubigeoNivel2=140100&ubigeoNivel3=140113
Antes de automatizar, revisa https://resultadoelectoral.onpe.gob.pe/robots.txt y los términos de uso.
