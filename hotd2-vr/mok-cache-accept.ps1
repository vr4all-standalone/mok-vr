$ErrorActionPreference='Stop'
$repo=Split-Path -Parent $PSScriptRoot
$model=Join-Path $repo 'build-win\data\staff-mok.bin'
$before=[IO.File]::ReadAllBytes($model)
try {
 [IO.File]::WriteAllBytes($model,[byte[]](1,2,3,4))
 & "$repo\hotd2-vr\vr-test.ps1" -Game mok -Name mok-cache-recovery -Port 27144 -Seconds 26 -XrSim -Config 'config:vr.HandsPrep=yes,config:vr.GameHands=yes,config:vr.BuiltInGun=no' -On @('VR hands: no model yet.*hero.s staff|0.4|xr right/a 1 40000','VR hands: no model yet.*hero.s staff|0.4|xr left/x 1 40000') -Expect @('VR hands: the hero.s staff from this game','VR hands: done','XR: gun: in-game staff') -Reject @('Light gun P[12] at .*start 1','XR: player 2 joins')
 if($LASTEXITCODE){throw 'Corrupt-cache recovery or completion input-latch check failed'}
 if((Get-Item -LiteralPath $model).Length -lt 1000){throw 'Staff cache was not rebuilt'}
 [IO.File]::WriteAllBytes($model,[byte[]](1,2,3,4))
 & "$repo\hotd2-vr\vr-test.ps1" -Game mok -Name mok-cache-skip -Port 27144 -Seconds 44 -XrSim -Config 'config:vr.HandsPrep=yes,config:vr.GameHands=yes,config:vr.BuiltInGun=no' -On @('VR hands: no model yet.*hero.s staff|0.4|xr left/y 1 6000') -Expect @('VR hands: skipped','XR: gun: Arcade gun') -Reject @('VR hands: the hero.s staff from this game','Light gun P[12] at .* B 1')
 if($LASTEXITCODE){throw 'Skip/session-release check failed'}
 if((Get-Item -LiteralPath $model).Length -ne 4){throw 'Skipped session unexpectedly captured a model'}
 Write-Output 'PASS corrupt cache rebuilt; successful preparation consumes held Starts; Y skip consumes held secondary and disables capture for the session.'
} finally {[IO.File]::WriteAllBytes($model,$before)}
