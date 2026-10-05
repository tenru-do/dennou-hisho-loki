@echo off
powershell.exe -NoProfile -STA -WindowStyle Hidden -Command "& ([scriptblock]::Create([IO.File]::ReadAllText('%~dp0loki-desktop.ps1', [Text.Encoding]::UTF8)))"
