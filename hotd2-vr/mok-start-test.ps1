# Exercise actual MOK readiness/profile code with unloaded, stock and patched RAM.
param()
$ErrorActionPreference='Stop'
$repo=Split-Path -Parent $PSScriptRoot
$out=Join-Path $repo 'build-win/vrtest/mok-start-unit'
New-Item -ItemType Directory -Path $out -Force | Out-Null
$source=[IO.File]::ReadAllText((Join-Path $repo 'core/rend/vr_reproject.cpp')).Replace("`r`n","`n")
function Block([string]$signature){
 $start=$source.IndexOf($signature)
 if($start -lt 0){throw "Missing production function $signature"}
 $open=$source.IndexOf('{',$start);$depth=1;$end=$open+1
 while($depth -gt 0 -and $end -lt $source.Length){if($source[$end] -eq '{'){$depth++};if($source[$end] -eq '}'){$depth--};$end++}
 if($depth){throw 'Unbalanced production function'}
 $source.Substring($start,$end-$start)
}
$start=$source.IndexOf('p.gameId = "THE MAZE OF THE KINGS";')
$profile=$source.Substring($start,$source.IndexOf('list.push_back(p);',$start)-$start)
$assignments=[regex]::Matches($profile,'(?m)^\s*p\.(?:gameId|model\.kind|fovPatch\[\d+\]|gunRam\.\w+)\s*=.*?;') | ForEach-Object {$_.Value}
$begin=$source.IndexOf('const GameProfile *prof = activeProfile;',$source.IndexOf('static void snapGuns('))
$end=$source.IndexOf("`n`tu8 enabled = 0;",$begin)
if($begin -lt 0 -or $end -le $begin){throw 'Missing production Start readiness snapshot'}
$code=@((Block 'static bool profileCodeThere('),(Block 'bool gameStartLoaded()'),(Block 'bool gameStartReady()'))
$code+='GameProfile mokProfile(){GameProfile p;'+($assignments -join "`n")+'return p;}'
$code+='bool snapshotStart(){'+$source.Substring($begin,$end-$begin)+'return nativeReady;}'
if($source.Contains('bool playerStateSupported()')){$code+='#define HAVE_STATE_SUPPORT';$code+=Block 'bool playerStateSupported()'}
[IO.File]::WriteAllText((Join-Path $out 'mok_start_under_test.inc'),($code -join "`n"),[Text.UTF8Encoding]::new($false))
$vswhere=Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio/Installer/vswhere.exe'
$vs=& $vswhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
$vcvars=Join-Path $vs 'VC/Auxiliary/Build/vcvars64.bat'
$cpp=Join-Path $PSScriptRoot 're/mok_start_test.cpp';$exe=Join-Path $out 'mok_start_test.exe';$log=Join-Path $out 'build.log'
Push-Location $out
try{
 & $env:ComSpec /d /c "call `"$vcvars`" >nul && cl /nologo /std:c++17 /EHsc /O2 /W3 /I`"$out`" /I`"$repo/core/rend/vr`" /Fe`"$exe`" `"$cpp`" >`"$log`" 2>&1"
 if($LASTEXITCODE){Get-Content $log;throw 'MOK readiness test compilation failed'}
 & $exe
 $code=$LASTEXITCODE
}finally{Pop-Location}
exit $code
