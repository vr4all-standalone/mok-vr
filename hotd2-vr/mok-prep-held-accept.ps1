$ErrorActionPreference='Stop'
$repo=Split-Path -Parent $PSScriptRoot
$model=Join-Path $repo 'build-win\data\staff-mok.bin'
$had=Test-Path -LiteralPath $model
$before=if($had){[IO.File]::ReadAllBytes($model)}else{$null}
try {
 [IO.File]::WriteAllBytes($model,[byte[]](1,2,3,4))
 & "$repo\hotd2-vr\vr-test.ps1" -Game mok -Name mok-prep-held -Port 27144 -Seconds 14 -XrSim -Config 'config:vr.HandsPrep=yes,config:vr.GameHands=yes,config:vr.BuiltInGun=no' -On @('VR hands: no model yet.*hero.s staff|0.4|xr right/a 1 5000','VR hands: no model yet.*hero.s staff|0.4|xr left/x 1 5000','VR hands: no model yet.*hero.s staff|0.7|xr right/b 1 5000') -Expect @('VR hands: skipped') -Reject @('Light gun P[12] at .*start 1','Light gun P[12] at .* B 1','XR: player 2 joins')
 $taskCode=$LASTEXITCODE
} finally {
 if($had){[IO.File]::WriteAllBytes($model,$before)}elseif(Test-Path -LiteralPath $model){Remove-Item -LiteralPath $model}
}
exit $taskCode
