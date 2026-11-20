<#
.SYNOPSIS
	Signs a Windows installer of Family Legacy (Authenticode) and prints the fingerprint of the key that signed it.

.DESCRIPTION
	Needs signtool.exe (Windows SDK) on the PATH and a code-signing certificate, either installed in the certificate store (-Thumbprint)
	or in a .pfx file (-PfxFile, the password is read from the environment variable FAMILYLEGACY_PFX_PASSWORD, never from the command line).
	Afterwards the signature is checked the way the application checks it, and the fingerprint to publish is printed.

.EXAMPLE
	.\installer\sign-installer.ps1 -File Core\target\windows-inno\FamilyLegacy-1.0.0-setup.exe -Thumbprint 0123...
#>
param(
	[Parameter(Mandatory = $true)][string]$File,
	[string]$Thumbprint,
	[string]$PfxFile,
	[string]$TimestampUrl = 'http://timestamp.digicert.com'
)

$ErrorActionPreference = 'Stop'

if(-not (Test-Path -LiteralPath $File -PathType Leaf)){
	throw "File not found: $File"
}
if((-not $Thumbprint) -and (-not $PfxFile)){
	throw 'Give -Thumbprint (certificate in the store) or -PfxFile'
}

$arguments = @('sign', '/fd', 'sha256', '/tr', $TimestampUrl, '/td', 'sha256', '/d', 'Family Legacy')
if($Thumbprint){
	$arguments += @('/sha1', $Thumbprint)
}
else{
	$arguments += @('/f', $PfxFile)
	if($env:FAMILYLEGACY_PFX_PASSWORD){
		$arguments += @('/p', $env:FAMILYLEGACY_PFX_PASSWORD)
	}
}
$arguments += $File

& signtool.exe @arguments
if($LASTEXITCODE -ne 0){
	throw "signtool failed with exit code $LASTEXITCODE"
}

# the same code the application runs when it downloads the installer
$classes = Join-Path $PSScriptRoot '..\Updater\target\classes'
if(-not (Test-Path -LiteralPath $classes)){
	throw "Build the Updater module first (mvn -pl Updater compile): $classes not found"
}
& java.exe -cp $classes io.github.mtrevisan.updater.SignatureVerifier $File
if($LASTEXITCODE -ne 0){
	throw 'The signature is not valid for Windows (a self-signed certificate is not trusted: use one from a certification authority)'
}
Write-Host 'Add the fingerprint above to Updater/src/main/resources/io/github/mtrevisan/updater/trusted-signers.txt and installer/SIGNING.md.'
