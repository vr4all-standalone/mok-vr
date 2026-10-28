#include "service_controls.h"
#include <array>
#include <atomic>
#include <cstdint>
#include <cstdio>
#include <map>
using u8=uint8_t;using u16=uint16_t;using u32=uint32_t;
namespace hands { struct Source { enum Kind {None,HerosStaff,CmGuns,LupinGun};Kind kind=None;}; }
struct CodePatch {u32 offset=0;u16 stock=0,patched=0;};
struct GameProfile {const char* gameId="";hands::Source model;std::array<CodePatch,8>fovPatch{};struct {u32 playing=0;bool floats=false;}gunRam;};
const GameProfile* activeProfile=nullptr;
std::map<u32,u16> words;u8 members=0;
u16 ReadMem16_nommu(u32 addr){return words[addr];}
u8 ReadMem8_nommu(u32){return members;}
std::atomic<bool> startInputReady{false};
struct {bool active=true;bool running(){return active;}}emu;
#include "mok_start_under_test.inc"
int main(){
 int checks=0,failures=0;
 auto check=[&](bool value,const char* label){++checks;if(!value){++failures;std::printf("FAIL %s\n",label);}};
 auto load=[&](const GameProfile& p,bool patched){for(const auto& guard:p.fovPatch)if(guard.offset)words[0x8c000000+guard.offset]=patched?guard.patched:guard.stock;};
 auto snapshot=[&](){startInputReady=snapshotStart();};
 check(!snapshotStart(),"unknown game never opens the Start gate");
 GameProfile mok=mokProfile();activeProfile=&mok;
 check(mok.model.kind==hands::Source::HerosStaff&&mok.fovPatch[0].offset!=0,"real MOK profile supplies guarded staff-game code");
 check(!mok.gunRam.playing&&!mok.gunRam.floats,"MOK has no measured native membership fields");
 snapshot();check(!gameStartLoaded()&&!gameStartReady(),"unloaded MOK stays blocked");
 load(mok,false);snapshot();
 check(gameStartReady(),"loaded stock MOK code accepts Start at the title screen");
 for(int hand:{0,1}){
  vr::xr::StartReleaseGate gate;
  check(!gate.sample(hand,true,false),"loading press is consumed");
  check(!gate.sample(hand,true,gameStartReady()),"Start held through loading still requires release");
  gate.sample(hand,false,gameStartReady());
  check(gate.sample(hand,true,gameStartReady()),"fresh A/X reaches native Start after MOK is loaded");
 }
 load(mok,true);snapshot();check(gameStartReady(),"widened MOK code also accepts Start");
 emu.active=false;check(!gameStartReady()&&gameStartLoaded(),"paused menu retains loaded state but cannot start the game");emu.active=true;
 words[0x8c000000+mok.fovPatch[2].offset]=0xffff;snapshot();check(!gameStartReady(),"one mismatched guard blocks Start again");
 GameProfile noGuard=mok;noGuard.fovPatch={};activeProfile=&noGuard;check(!snapshotStart(),"staff identity without code guards does not bypass loading");
 GameProfile cm;cm.model.kind=hands::Source::CmGuns;cm.gunRam.playing=0x100;activeProfile=&cm;members=0;
 check(snapshotStart(),"CM retains inactive-title readiness from its native membership byte");members=4;
 check(!snapshotStart(),"CM corrupt membership remains blocked");
#ifdef HAVE_STATE_SUPPORT
 activeProfile=&mok;check(!playerStateSupported(),"MOK uses its established prompt-based join path");
 activeProfile=&cm;check(playerStateSupported(),"CM retains native join acknowledgment");
 GameProfile lupin;lupin.gunRam.floats=true;activeProfile=&lupin;check(playerStateSupported(),"Lupin retains its native VM join acknowledgment");
 activeProfile=nullptr;check(!playerStateSupported(),"unknown profile never claims native player state");
#endif
 std::printf("%s %d MOK production readiness checks (%d failures)\n",failures?"FAIL":"PASS",checks,failures);
 return failures?1:0;
}
