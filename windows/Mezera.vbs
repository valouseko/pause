' Spusti Mezeru potichu na pozadi (bez okna konzole).
' Dobre pro slozku po spusteni (shell:startup) nebo kdyz uz appku znas.
Set fso = CreateObject("Scripting.FileSystemObject")
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
Set sh = CreateObject("WScript.Shell")
sh.CurrentDirectory = scriptDir
sh.Run "cmd /c npm start", 0, False
