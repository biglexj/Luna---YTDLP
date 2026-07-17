param(
    [Parameter(Mandatory)] [string]$ProjectRoot,
    [Parameter(Mandatory)] [string]$Version,
    [Parameter(Mandatory)] [string]$OutputPath,
    [Parameter(Mandatory)] [string]$MakeAppxPath
)

$ErrorActionPreference = "Stop"
$appImage = Join-Path $ProjectRoot "composeApp\build\compose\binaries\main\app\LunaFetch"
$manifestTemplate = Join-Path $ProjectRoot "packaging\msix\AppxManifest.xml.in"
$assets = Join-Path $ProjectRoot "Image"
$staging = Join-Path $ProjectRoot "build\msix\$Version-$([DateTime]::UtcNow.ToString('yyyyMMddHHmmss'))"

foreach ($required in @($appImage, $manifestTemplate, $assets, $MakeAppxPath)) {
    if (-not (Test-Path -LiteralPath $required)) { throw "No se encontró el recurso necesario: $required" }
}

New-Item -ItemType Directory -Path $staging -Force | Out-Null
& robocopy $appImage $staging /E /R:2 /W:1 /NFL /NDL /NJH /NJS /NP
if ($LASTEXITCODE -ge 8) { throw "Robocopy no pudo preparar la imagen MSIX; código $LASTEXITCODE." }
Copy-Item -LiteralPath $assets -Destination (Join-Path $staging "Image") -Recurse -Force

$manifest = (Get-Content -LiteralPath $manifestTemplate -Raw).Replace("{{VERSION}}", "$Version.0")
$manifest | Set-Content -LiteralPath (Join-Path $staging "AppxManifest.xml") -Encoding utf8

& $MakeAppxPath pack /d $staging /p $OutputPath /o
if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $OutputPath)) {
    throw "MakeAppx no pudo generar $OutputPath"
}
