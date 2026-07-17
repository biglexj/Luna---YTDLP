param(
    [string]$Version,
    [string]$ReleaseNotesFile = "RELEASE_MESSAGE.md",
    [switch]$LocalOnly,
    [switch]$SkipTests,
    [switch]$SkipBuild,
    [switch]$SkipSigning
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
. (Join-Path $root "scripts\release\ReleaseTools.ps1")

$repository = "biglexj/Luna---YTDLP"
$propertiesPath = Join-Path $root "gradle.properties"
$releaseNotesPath = Join-Path $root $ReleaseNotesFile
$properties = Get-Content -LiteralPath $propertiesPath -Raw -Encoding UTF8
$versionMatch = [regex]::Match($properties, '(?m)^lunafetch\.versionName=(\d+\.\d+\.\d+)$')
$codeMatch = [regex]::Match($properties, '(?m)^lunafetch\.versionCode=(\d+)$')
if (-not $versionMatch.Success -or -not $codeMatch.Success) { throw "No se pudo leer la versión de Luna Fetch." }

$currentVersion = $versionMatch.Groups[1].Value
$newCode = [int]$codeMatch.Groups[1].Value
if (-not $Version) { $Version = $currentVersion }
Assert-SemanticVersion $Version
if (-not (Test-Path -LiteralPath $releaseNotesPath)) { throw "No se encontró $releaseNotesPath" }
if (-not $LocalOnly -and $SkipSigning) { throw "Una publicación oficial no puede usar -SkipSigning." }

$tag = "v$Version"
$output = Join-Path $root "release"
if (-not $LocalOnly) { Assert-PublishPreflight -Root $root -Repository $repository -Tag $tag }

if ($Version -ne $currentVersion) {
    $newCode++
    $properties = $properties `
        -replace '(?m)^(lunafetch\.versionName=)\d+\.\d+\.\d+$', "`${1}$Version" `
        -replace '(?m)^(lunafetch\.versionCode=)\d+$', "`${1}$newCode"
    Set-Content -LiteralPath $propertiesPath -Value $properties -Encoding UTF8 -NoNewline
}

$jdk = Get-FullJdk
$makeAppx = Get-WindowsSdkTool "makeappx.exe"
$signTool = if ($SkipSigning) { $null } else { Get-WindowsSdkTool "signtool.exe" }
$env:JAVA_HOME = $jdk
$env:ANDROID_HOME = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }

if (-not $LocalOnly) {
    $hasSigningFile = Test-Path -LiteralPath (Join-Path $root "keystore.properties")
    $hasSigningEnvironment = @(
        "LUNAFETCH_ANDROID_KEYSTORE",
        "LUNAFETCH_ANDROID_STORE_PASSWORD",
        "LUNAFETCH_ANDROID_KEY_ALIAS",
        "LUNAFETCH_ANDROID_KEY_PASSWORD"
    ) | ForEach-Object { [bool][Environment]::GetEnvironmentVariable($_) }
    if (-not $hasSigningFile -and $hasSigningEnvironment.Contains($false)) {
        throw "Falta la firma Android permanente. Configura keystore.properties o las variables LUNAFETCH_ANDROID_* completas."
    }
}

if (-not $SkipBuild) {
    $tasks = @(
        ":composeApp:createDistributable",
        ":composeApp:packageExe",
        ":composeApp:packageMsi"
    )
    if ($LocalOnly) {
        $tasks += ":composeApp:assembleDebug"
    } else {
        $tasks += @(
            ":composeApp:assembleRelease",
            ":composeApp:bundleRelease"
        )
    }
    if (-not $SkipTests) { $tasks = @(":composeApp:desktopTest") + $tasks }
    Invoke-Checked (Join-Path $root "gradlew.bat") (@("-Dorg.gradle.java.home=$jdk") + $tasks)
}

New-Item -ItemType Directory -Path $output -Force | Out-Null
$resolvedRoot = [IO.Path]::GetFullPath($root).TrimEnd('\') + '\'
$resolvedOutput = [IO.Path]::GetFullPath($output).TrimEnd('\') + '\'
if (-not $resolvedOutput.StartsWith($resolvedRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw "La salida resuelta quedó fuera del proyecto."
}
try {
    Get-ChildItem -LiteralPath $output -File -ErrorAction SilentlyContinue | Remove-Item -Force
} catch [System.UnauthorizedAccessException] {
    $lockedPath = $_.Exception.ItemName
    $lockingProcesses = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object { $_.ExecutablePath -and $_.ExecutablePath.Equals($lockedPath, [StringComparison]::OrdinalIgnoreCase) } |
        ForEach-Object { "$($_.Name) (PID $($_.ProcessId))" }
    $lockDescription = if ($lockingProcesses) { $lockingProcesses -join ", " } else { "un proceso externo" }
    throw "No se puede limpiar '$lockedPath': está en uso por $lockDescription. Cierra esa instancia y vuelve a ejecutar."
}

$exe = Join-Path $output "LunaFetch-Windows-$Version.exe"
$msi = Join-Path $output "LunaFetch-Windows-$Version.msi"
$msix = Join-Path $output "LunaFetch-Windows-$Version.msix"
Copy-Item -LiteralPath (Join-Path $root "composeApp\build\compose\binaries\main\exe\LunaFetch-$Version.exe") -Destination $exe
Copy-Item -LiteralPath (Join-Path $root "composeApp\build\compose\binaries\main\msi\LunaFetch-$Version.msi") -Destination $msi
foreach ($artifact in @($exe, $msi)) {
    (Get-Item -LiteralPath $artifact).IsReadOnly = $false
}
& (Join-Path $root "scripts\packaging\New-MsixPackage.ps1") `
    -ProjectRoot $root -Version $Version -OutputPath $msix -MakeAppxPath $makeAppx

$androidArtifacts = @()
if ($LocalOnly) {
    $apk = Join-Path $output "LunaFetch-Android-$Version-debug.apk"
    Copy-Item -LiteralPath (Join-Path $root "composeApp\build\outputs\apk\debug\composeApp-debug.apk") -Destination $apk
    $androidArtifacts += $apk
} else {
    $apk = Join-Path $output "LunaFetch-Android-$Version.apk"
    $aab = Join-Path $output "LunaFetch-Android-$Version.aab"
    $apkSource = Join-Path $root "composeApp\build\outputs\apk\release\composeApp-release.apk"
    $aabSource = Join-Path $root "composeApp\build\outputs\bundle\release\composeApp-release.aab"
    foreach ($artifact in @($apkSource, $aabSource)) {
        if (-not (Test-Path -LiteralPath $artifact)) { throw "No se generó el artefacto Android esperado: $artifact" }
    }

    $buildTools = Get-ChildItem -LiteralPath (Join-Path $env:ANDROID_HOME "build-tools") -Directory |
        Sort-Object { [version]$_.Name } -Descending |
        Select-Object -First 1
    if (-not $buildTools) { throw "No se encontraron Android build-tools para validar el APK." }
    $apkSigner = Join-Path $buildTools.FullName "apksigner.bat"
    $aapt = Join-Path $buildTools.FullName "aapt.exe"
    Invoke-Checked $apkSigner @("verify", "--verbose", "--print-certs", $apkSource)
    $badging = (& $aapt dump badging $apkSource | Select-Object -First 1)
    if ($LASTEXITCODE -ne 0) { throw "No se pudo inspeccionar la identidad del APK." }
    if ($badging -notmatch "name='com\.biglexj\.lunafetch'") {
        throw "El applicationId del APK no corresponde a Luna Fetch: $badging"
    }
    if ($badging -notmatch "versionCode='$newCode'" -or $badging -notmatch "versionName='$([regex]::Escape($Version))'") {
        throw "La versión interna del APK no coincide con $Version ($newCode): $badging"
    }
    & (Join-Path $jdk "bin\jarsigner.exe") -verify $aabSource *> $null
    if ($LASTEXITCODE -ne 0) { throw "La firma del AAB no superó la verificación." }

    Copy-Item -LiteralPath $apkSource -Destination $apk
    Copy-Item -LiteralPath $aabSource -Destination $aab
    $androidArtifacts += @($apk, $aab)
}

if (-not $SkipSigning) {
    $certificate = Join-Path $root "LunaFetch_Dev_Certificate.pfx"
    if (-not (Test-Path -LiteralPath $certificate)) { throw "Falta LunaFetch_Dev_Certificate.pfx." }
    foreach ($artifact in @($exe, $msi, $msix)) {
        Invoke-Checked $signTool @("sign", "/fd", "SHA256", "/f", $certificate, $artifact)
        Assert-SignedArtifact -Path $artifact -Publisher "CN=biglexj"
    }
}

foreach ($artifact in @($exe, $msi, $msix) + $androidArtifacts) {
    if (-not (Test-Path -LiteralPath $artifact) -or (Get-Item -LiteralPath $artifact).Length -eq 0) {
        throw "El artefacto final falta o está vacío: $artifact"
    }
}

$hashPath = Join-Path $output "SHA256SUMS.txt"
Get-ChildItem -LiteralPath $output -File |
    Get-FileHash -Algorithm SHA256 |
    ForEach-Object { "{0}  {1}" -f $_.Hash.ToLowerInvariant(), (Split-Path $_.Path -Leaf) } |
    Set-Content -LiteralPath $hashPath -Encoding UTF8

if ($LocalOnly) { Write-Host "Build local terminado en $output" -ForegroundColor Green; exit 0 }

Push-Location $root
try {
    Invoke-Checked git @("add", "-A")
    Invoke-Checked git @("diff", "--cached", "--check")
    & git diff --cached --quiet
    if ($LASTEXITCODE -ne 0) { Invoke-Checked git @("commit", "-m", "release: Luna Fetch $tag") }
    Invoke-Checked git @("tag", "-a", $tag, "-m", "Luna Fetch $tag")
    Invoke-Checked git @("push", "--atomic", "origin", "HEAD:main", "refs/tags/$tag")
} finally { Pop-Location }

$assets = @($exe, $msi, $msix, $hashPath) + $androidArtifacts
Invoke-Checked gh (@("release", "create", $tag) + $assets + @(
    "--repo", $repository,
    "--verify-tag",
    "--title", "Luna Fetch $tag",
    "--notes-file", $releaseNotesPath
))
