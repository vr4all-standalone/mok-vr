# Exercise the production effect routing, geometry and lifetime with GL calls recorded.
param()
$ErrorActionPreference='Stop'
$repo=Split-Path -Parent $PSScriptRoot
$out=Join-Path $repo 'build-win/vrtest/mok-magic-unit'
New-Item -ItemType Directory -Path $out -Force | Out-Null
$source=[IO.File]::ReadAllText((Join-Path $repo 'core/rend/vr/xr_gun.cpp')).Replace("`r`n","`n")
$header=[IO.File]::ReadAllText((Join-Path $repo 'core/rend/vr/xr_gun.h')).Replace("`r`n","`n")
function Block([string]$text,[string]$signature){
 $start=$text.IndexOf($signature)
 if($start -lt 0){throw "Missing production code $signature"}
 $at=$text.IndexOf('{',$start);$end=$at+1;$depth=1
 while($depth -gt 0 -and $end -lt $text.Length){if($text[$end] -eq '{'){$depth++};if($text[$end] -eq '}'){$depth--};$end++}
 if($depth){throw 'Unbalanced effect production code'}
 $text.Substring($start,$end-$start)
}
$begin=$source.IndexOf('struct Puff')
$end=$source.IndexOf('// A cheap deterministic random number',$begin)
$types=@((Block $source 'struct GlowVertex')+';',[regex]::Match($source,'enum Shape \{[^}]+\};').Value,(Block $header 'struct GunView')+';',$source.Substring($begin,$end-$begin))
[IO.File]::WriteAllText((Join-Path $out 'magic_types.inc'),($types -join "`n"),[Text.UTF8Encoding]::new($false))
$functions=@()
foreach($signature in @('float shotRandom(','void billboard(','void flame(','glm::vec3 gunMuzzle(','static bool staffMagic(','static void muzzleNow(','static void addStaffMagic(','void drawGunGlow(','void termGun()')){
 if($source.Contains($signature)){$functions+=Block $source $signature}
}
$functions+='size_t magicCount(){return '+$(if($source.Contains('std::vector<MagicBurst> magicBursts')){'magicBursts.size()'}else{'0'})+';}'
[IO.File]::WriteAllText((Join-Path $out 'magic_functions.inc'),($functions -join "`n"),[Text.UTF8Encoding]::new($false))
$vswhere=Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio/Installer/vswhere.exe'
$vs=& $vswhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
$vcvars=Join-Path $vs 'VC/Auxiliary/Build/vcvars64.bat'
$cpp=Join-Path $PSScriptRoot 're/mok_magic_test.cpp';$exe=Join-Path $out 'mok_magic_test.exe';$log=Join-Path $out 'build.log'
Push-Location $out
try{
 & $env:ComSpec /d /c "call `"$vcvars`" >nul && cl /nologo /std:c++17 /EHsc /O2 /W3 /I`"$out`" /I`"$repo/core/deps/glm`" /Fe`"$exe`" `"$cpp`" >`"$log`" 2>&1"
 if($LASTEXITCODE){Get-Content $log;throw 'Magic renderer test compilation failed'}
 & $exe
 $code=$LASTEXITCODE
}finally{Pop-Location}
exit $code
