batfile = Left(WScript.ScriptFullName,InStrRev(WScript.ScriptFullName,"\")) & "\clj-fe.bat"
Set WshShell = CreateObject("WScript.Shell")
WshShell.Run chr(34) & batfile & Chr(34), 0, false
Set WshShell = Nothing
