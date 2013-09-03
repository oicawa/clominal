Dim oShell
Set oShell = CreateObject("Wscript.Shell") 
oShell.run "cmd /c .\clominal.bat", vbhide
Set oShell = Nothing
