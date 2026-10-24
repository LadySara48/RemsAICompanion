param(
    [string[]]$Versions = @('1.18.2','1.20.1','1.21.1','1.21.4','1.21.11','26.2')
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Push-Location -LiteralPath $projectRoot
try {
    foreach ($minecraftTarget in $Versions) {
        & (Join-Path $projectRoot 'gradlew.bat') -p $PSScriptRoot "-Pmc=$minecraftTarget" build --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Fabric $minecraftTarget build failed." }
    }
    & python (Join-Path $PSScriptRoot 'verify-dist.py') @Versions
    if ($LASTEXITCODE -ne 0) { throw 'Fabric artifact verification failed.' }
} finally { Pop-Location }
