param([switch]$Reuse,[ValidateRange(1024,65535)][int]$Port=27144)
$ErrorActionPreference='Stop'
$repo=Split-Path -Parent $PSScriptRoot
$out=Join-Path $repo 'build-win\vrtest\mok-gameplay-final'
if(-not $Reuse){
 $actions=@(); foreach($second in 36..62 | Where-Object {$_ % 2 -eq 0}){$actions+="$second=xr right/a 1 200"}
 $actions+=@('68=xr right/trigger 1 180','69=xr left/trigger 1 180','70=xr right/trigger 1 180',
 '72=xr left/x 1 200','75=xr left/trigger 1 180','76=xr right/trigger 1 180','78=shot dual',
 '80=xr left/menu 1 150','81=stick right 0 -1 150','81.5=stick right 0 -1 150',
 '82=stick right 0 -1 150','82.5=stick right 0 -1 150','83=stick right 0 -1 150',
 '84=stick right 1 0 150','85=shot layout','86=xr right/b 1 150',
 '88=xr left/trigger 1 180','89=xr right/trigger 1 180','90=shot swapped',
 '101=xr left/menu 1 150','102=shot pause-a','104=shot pause-b',
 '105=pose head 0.1 1.6 -0.1 15 -15','106=shot pause-look',
 '107=pose head 0 1.6 0 0 0','108=xr right/b 1 150',
 '110=shot resumed-a','114=shot resumed-b','120=xr left/trigger 1 180','121=xr right/trigger 1 180',
 '126=shot later','138=shot later-stage','148=shot final')
 & "$PSScriptRoot\vr-test.ps1" -Game mok -Name mok-gameplay-final -Port $Port -Seconds 151 -Visible -XrSim -Config 'config:vr.GameHands=yes,config:vr.BuiltInGun=no,config:vr.Gun=1' -At $actions -Expect @(
 'VR: camera profile for THE MAZE OF THE KINGS','XR: gun: in-game staff','XR: the gun is in the left hand','XR: the gun is in the right hand',
 'XR: player 2 joins: a gun in each hand','XR: menu Two players -> P1 right / P2 left','XR: player 1 shot','XR: player 2 shot',
 'XR: menu open \(paused\)','XR: menu closed \(resumed\)')
 if($LASTEXITCODE){exit $LASTEXITCODE}
}
$python=if($env:MOK_TEST_PYTHON){$env:MOK_TEST_PYTHON}else{'python'}
$check=Join-Path $PSScriptRoot 're\vrcheck.py'
foreach($spec in @(
 @('same',"$out\t102-pause-a.png","$out\t104-pause-b.png",'--max-diff','1.5'),
 @('differ',"$out\t104-pause-b.png","$out\t106-pause-look.png",'--min-diff','2'),
 @('differ',"$out\t110-resumed-a.png","$out\t114-resumed-b.png",'--min-diff','0.5')
)){
 & $python -I $check @spec
 if($LASTEXITCODE){throw 'MOK pause/head-motion/resume image check failed'}
}
Write-Output 'PASS MOK native staffs, both player layouts, solo transfer, paused animation, live head tracking and resumed animation.'
