param(
    [string]$ReleaseNotes = "Perbaikan dan peningkatan stabilitas.",
    [string]$GithubUser = "KueBakar",
    [string]$GithubRepo = "Piecer"
)

$ErrorActionPreference = "Stop"

Write-Host "==> Menjalankan unit test..."
& .\gradlew testDebugUnitTest
if ($LASTEXITCODE -ne 0) {
    throw "Unit test gagal. Rilis dibatalkan."
}

$keystoreCandidates = @()
if (-not [string]::IsNullOrWhiteSpace($env:PIECER_KEYSTORE_PROPERTIES)) {
    $keystoreCandidates += $env:PIECER_KEYSTORE_PROPERTIES
}
if (-not [string]::IsNullOrWhiteSpace($env:USERPROFILE)) {
    $keystoreCandidates += (Join-Path $env:USERPROFILE ".piecer-signing\keystore.properties")
}
$keystoreCandidates += (Join-Path $PSScriptRoot "keystore.properties")

$keystoreProps = $keystoreCandidates | Where-Object { Test-Path $_ -PathType Leaf } | Select-Object -First 1

if (-not $keystoreProps) {
    Write-Host "==> keystore.properties tidak ditemukan. Melewati assembleRelease (lihat SIGNING.md)."
    exit 0
}

Write-Host "==> Menemukan konfigurasi signing (kredensial tidak ditampilkan)."
Write-Host "==> Membangun assembleRelease..."
& .\gradlew assembleRelease
if ($LASTEXITCODE -ne 0) {
    throw "assembleRelease gagal."
}

$gradleText = Get-Content (Join-Path $PSScriptRoot "app\build.gradle.kts") -Raw
$versionCode = [int]([regex]::Match($gradleText, 'versionCode\s*=\s*(\d+)').Groups[1].Value)
$versionName = [regex]::Match($gradleText, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
$minSdk = [int]([regex]::Match($gradleText, 'minSdk\s*=\s*(\d+)').Groups[1].Value)

$releaseApk = Join-Path $PSScriptRoot "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $releaseApk -PathType Leaf)) {
    throw "APK release tidak ditemukan di $releaseApk"
}

$distDir = Join-Path $PSScriptRoot "dist"
if (-not (Test-Path $distDir)) {
    New-Item -ItemType Directory -Path $distDir | Out-Null
}

$distApkName = "PIECER-v$versionName.apk"
$distApkPath = Join-Path $distDir $distApkName
Copy-Item -Path $releaseApk -Destination $distApkPath -Force

$sha256 = (Get-FileHash -Path $distApkPath -Algorithm SHA256).Hash.ToLowerInvariant()
$sizeBytes = (Get-Item $distApkPath).Length

$apkUrl = "https://github.com/$GithubUser/$GithubRepo/releases/download/v$versionName/$distApkName"
$updateObj = [ordered]@{
    versionCode  = $versionCode
    versionName  = $versionName
    apkUrl       = $apkUrl
    sha256       = $sha256
    sizeBytes    = $sizeBytes
    minSdk       = $minSdk
    releaseNotes = $ReleaseNotes
}

$docsDir = Join-Path $PSScriptRoot "docs"
if (-not (Test-Path $docsDir)) {
    New-Item -ItemType Directory -Path $docsDir | Out-Null
}
$updateJsonPath = Join-Path $docsDir "update.json"
$updateObj | ConvertTo-Json -Depth 4 | Set-Content -Path $updateJsonPath -Encoding UTF8

Write-Host "==> Selesai!"
Write-Host "APK    : $distApkPath"
Write-Host "Ukuran : $sizeBytes bytes"
Write-Host "SHA256 : $sha256"
Write-Host "Update : $updateJsonPath"
