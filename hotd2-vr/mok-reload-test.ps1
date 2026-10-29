# Run actual MOK profile wiring, draw commands and reload branches without an XR runtime.
param([string]$Baseline='')
$ErrorActionPreference='Stop'
$repo=Split-Path -Parent $PSScriptRoot
$out=Join-Path $repo 'build-win/vrtest/mok-reload-unit'
New-Item -ItemType Directory -Path $out -Force | Out-Null
$hostSource=[IO.File]::ReadAllText((Join-Path $repo 'core/rend/vr/xr_host.cpp'))
$hands=[IO.File]::ReadAllText((Join-Path $repo 'core/rend/vr/xr_hands.cpp'))
$profiles=[IO.File]::ReadAllText((Join-Path $repo 'core/rend/vr_reproject.cpp'))
if($Baseline){
 $hostSource=(& git -C $repo show "${Baseline}:core/rend/vr/xr_host.cpp") -join [Environment]::NewLine
 if($LASTEXITCODE){throw 'Cannot read baseline host'}
 $hands=(& git -C $repo show "${Baseline}:core/rend/vr/xr_hands.cpp") -join [Environment]::NewLine
 if($LASTEXITCODE){throw 'Cannot read baseline hands'}
 $profiles=(& git -C $repo show "${Baseline}:core/rend/vr_reproject.cpp") -join [Environment]::NewLine
 if($LASTEXITCODE){throw 'Cannot read baseline profile'}
}
$begin=$profiles.IndexOf('p.gameId = "THE MAZE OF THE KINGS";')
$profile=$profiles.Substring($begin,$profiles.IndexOf('list.push_back(p);',$begin)-$begin)
$sensor=[regex]::Match($profile,'p\.analogOffscreenReload\s*=\s*(true|false);')
$flag=if($sensor.Success){$sensor.Groups[1].Value}else{'false'}
$begin=$hands.IndexOf('// metal with a sheen')
$draw=$hands.Substring($begin,$hands.IndexOf('bindVertexArray(0);',$begin)-$begin)
$floor=[regex]::Match($hostSource,'(?s)(?:const hands::Source \*reloadSource = gameModelSource\(\);\s*)?if \(!slide && live.*?\r?\n\t\t}')
if(-not $floor.Success){throw 'Missing production floor reload branch'}
$begin=$hostSource.IndexOf('if (config::VrReload == 1 && holds(g.hand)')
$point=$hostSource.Substring($begin,$hostSource.IndexOf('// Seen but outside',$begin)-$begin)
$source="constexpr bool mokSensorReload=$flag;"+"`nvoid render(Model& model,const View& view) {`n// Native staff-only fixture: Arcade material override is disabled.`nconst glm::vec4 arcadeBase(0.f);`n"+$draw+"`n}`nvoid floorReload(Gun& g,bool slide,bool live,glm::vec3 d) {`n"+$floor.Value+"`n}`nvoid pointReload(Gun& g) {`n"+$point+"`n}"
[IO.File]::WriteAllText((Join-Path $out 'mok_reload_under_test.inc'),$source,[Text.UTF8Encoding]::new($false))
$vswhere=Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio/Installer/vswhere.exe'
$vsroot=& $vswhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
$envCmd=Join-Path $vsroot 'VC/Auxiliary/Build/vcvars64.bat'
$cpp=Join-Path $PSScriptRoot 're/mok_reload_test.cpp'
$exe=Join-Path $out 'mok_reload_test.exe'
$log=Join-Path $out 'build.log'
Push-Location $out
try {
 & $env:ComSpec /d /c "call `"$envCmd`" >nul && cl /nologo /std:c++17 /EHsc /O2 /W3 /I`"$out`" /I`"$repo/core/deps/glm`" /I`"$repo/core/rend/vr`" /Fe`"$exe`" `"$cpp`" >`"$log`" 2>&1"
 if($LASTEXITCODE -ne 0){Get-Content -LiteralPath $log;throw 'MOK reload test compilation failed'}
 & $exe
 $code=$LASTEXITCODE
}finally{Pop-Location}
exit $code
