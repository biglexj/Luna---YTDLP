# Winget

Los manifiestos de esta carpeta apuntan al MSI publicado en GitHub Releases. Se validan localmente con:

```powershell
winget validate --manifest .\packaging\winget\manifests\b\biglexj\LunaFetch\1.0.2
```

La presencia de estos archivos no implica que Luna Fetch ya esté disponible en el catálogo. La publicación requiere enviar y aprobar el manifiesto en `microsoft/winget-pkgs`.
