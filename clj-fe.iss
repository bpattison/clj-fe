[Setup]
AppName=clj-fe
AppVerName=clj-fe version 1.0
DefaultDirName={pf}\clj-fe
DefaultGroupName=clj-fe
;UninstallDisplayIcon={app}\MyProg.exe
Compression=lzma2
SolidCompression=yes
OutputBaseFilename=clj-fe-installer
OutputDir=.
ChangesAssociations=yes

[Registry]
Root: HKCR; Subkey: ".clj"; ValueType: string; ValueName: ""; ValueData: "clj"; Flags: uninsdeletevalue
Root: HKCR; Subkey: "clj"; ValueType: string; ValueName: ""; ValueData: "Clojure Front End"; Flags: uninsdeletekey
Root: HKCR; Subkey: "clj\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\bin\clj.exe,0"
Root: HKCR; Subkey: "clj\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\bin\clj.exe"" ""%1"""

[Dirs]
Name: "{app}\bin"
Name: "{app}\examples"
Name: "{app}\lib"
Name: "{app}\src"

[Files]
; Source: "Readme.txt";       DestDir: "{app}"; Flags: isreadme
Source: "images\clj-logo.ico"; DestDir: "{app}";
Source: "bin\*";               DestDir: "{app}\bin";
Source: "examples\*";          DestDir: "{app}\examples";
Source: "lib\*";               DestDir: "{app}\lib";
Source: "images\*";            DestDir: "{app}\images";
;Source: "src\*";            DestDir: "{app}\src"; Excludes: "*.ncb,*.sdf,*.pdb,*.aps,Release\*,Debug\*,ipch\*"; Flags: recursesubdirs

[Icons]
Name: "{group}\clj"; Filename: "{app}\bin\clj.exe"; IconFilename: "{app}\images\clj-logo.ico"
Name: "{group}\clj-fe"; Filename: "{app}\bin\clj-fe.vbs"; IconFilename: "{app}\images\clj-fe-logo.ico"; Flags: runminimized
Name: "{group}\examples"; Filename: "{app}\examples";
Name: "{group}\un-install"; Filename: "{app}\unins000.exe";

[Run]
;Filename: "{app}\bin\clj-fe-tray.bat";

[UninstallRun]
;Filename: "{app}\bin\cljtray.exe -stop";
