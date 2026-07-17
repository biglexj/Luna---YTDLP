# Empaquetado de Luna Fetch

La versión se centraliza en `gradle.properties`. Compose Desktop configura EXE, MSI, DEB y RPM; Android Gradle Plugin genera APK y AAB.

## Build local

```powershell
.\build-release.ps1 -LocalOnly -SkipSigning
```

El flujo ejecuta pruebas de escritorio, compila Android, prepara EXE/MSI/MSIX, copia un APK de depuración y calcula `SHA256SUMS.txt`. Los artefactos quedan en `release/`.

Para firmar Windows localmente, coloca `LunaFetch_Dev_Certificate.pfx` en la raíz y omite `-SkipSigning`. El publicador esperado es `CN=biglexj`. El certificado y las claves Android están ignorados por Git.

## Publicación oficial

Antes de publicar Android crea `keystore.properties` en la raíz:

```properties
storeFile=signing/lunafetch-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Como alternativa, configura:

- `LUNAFETCH_ANDROID_KEYSTORE`
- `LUNAFETCH_ANDROID_STORE_PASSWORD`
- `LUNAFETCH_ANDROID_KEY_ALIAS`
- `LUNAFETCH_ANDROID_KEY_PASSWORD`

Después ejecuta:

```powershell
.\build-release.ps1
```

La publicación exige `main` sincronizada, GitHub CLI autenticado, certificado Windows y firma Android permanente. Valida la firma e identidad del APK, la firma del AAB y los paquetes Windows; después crea commit, tag, push atómico y GitHub Release con EXE, MSI, MSIX, APK, AAB y hashes.

DEB y RPM deben generarse y probarse en Linux mediante `:composeApp:packageDeb` y `:composeApp:packageRpm`.
