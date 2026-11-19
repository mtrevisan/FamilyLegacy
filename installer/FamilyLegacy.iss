; Inno Setup script of the Windows installer. It is not meant to be run by hand: the Maven profile windows-inno
; (module Ephemeris) builds the application image with jpackage and then calls ISCC with these defines:
;   /DAppVersion=1.0.0  /DAppGuid=<upgrade uuid>  /DSourceDir=<jpackage app-image>  /DIconFile=<.ico>  /DOutputDir=<folder>
; To compile it by hand, pass the same defines.

#ifndef AppVersion
  #define AppVersion "1.0.0"
#endif
#ifndef AppGuid
  #define AppGuid "5d3f2c1e-8a47-4b6e-9c1d-2f7a9e0b6c41"
#endif
#define AppName "FamilyLegacy"
#define AppExe "FamilyLegacy.exe"
#define AppPublisher "Mauro Trevisan"

[Setup]
; the same identifier forever: it lets a new version upgrade the installed one
AppId={{{#AppGuid}}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
DefaultDirName={autopf}\{#AppName}
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
; no administrator rights needed (per-user installation)
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
OutputDir={#OutputDir}
OutputBaseFilename={#AppName}-{#AppVersion}-setup
SetupIconFile={#IconFile}
UninstallDisplayIcon={app}\{#AppExe}
Compression=lzma2/ultra64
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
WizardStyle=modern
; the running application is closed (and restarted) when upgrading
CloseApplications=yes
RestartApplications=no

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "italian"; MessagesFile: "compiler:Languages\Italian.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
; the whole jpackage app-image: launcher, app (jar, modules, plugins, fonts) and the bundled runtime
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs ignoreversion

[Dirs]
; the application installs plugins and fonts here (Help > Optional Components...): they must survive an uninstall check
Name: "{app}\app\plugins"
Name: "{app}\app\fonts"

[Icons]
Name: "{autoprograms}\{#AppName}"; Filename: "{app}\{#AppExe}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExe}"; Description: "{cm:LaunchProgram,{#AppName}}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
; plugins and fonts downloaded after the installation are not known to the uninstaller
Type: filesandordirs; Name: "{app}\app\plugins"
Type: filesandordirs; Name: "{app}\app\fonts"
