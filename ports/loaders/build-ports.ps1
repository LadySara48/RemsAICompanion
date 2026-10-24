param(
    [ValidateSet('forge','neoforge','all')][string]$Loader = 'all',
    [string[]]$Versions = @()
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$configuration = Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'versions.json') | ConvertFrom-Json
$loaders = if ($Loader -eq 'all') { @('forge','neoforge') } else { @($Loader) }
Push-Location -LiteralPath $projectRoot
try {
    foreach ($targetLoader in $loaders) {
        $targets = if ($Versions.Count -gt 0) { $Versions } else { @($configuration.$targetLoader.PSObject.Properties.Name) }
        foreach ($minecraftTarget in $targets) {
            & (Join-Path $projectRoot 'gradlew.bat') -p (Join-Path $projectRoot "ports/$targetLoader") "-Pmc=$minecraftTarget" build --console=plain
            if ($LASTEXITCODE -ne 0) { throw "$targetLoader $minecraftTarget build failed." }
        }
        & python (Join-Path $PSScriptRoot 'verify-dist.py') $targetLoader @targets
        if ($LASTEXITCODE -ne 0) { throw "$targetLoader artifact verification failed." }
    }
} finally { Pop-Location }
