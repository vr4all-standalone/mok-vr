#include "gun_input.h"
#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <cstdio>
#include <vector>
using u32=uint32_t;
namespace config { int VrReload=0; bool VrStaffHand=false; }
namespace hands { struct Source { enum Kind {None,HerosStaff,CmGuns}; Kind kind=HerosStaff; }; }
hands::Source source; bool hasSource=true;
const hands::Source* gameModelSource(){return hasSource?&source:nullptr;}
enum { MeshFrame,MeshSlide,MeshGunHand,MeshOpenHand };
struct Model { bool staff; };
struct View { glm::mat4 gunPose{1.f},handPose{1.f}; float slide=0; bool otherHand=true; };
glm::mat4 viewProj{1.f}; std::vector<int> drawn;
void drawMesh(Model&,int mesh,const glm::mat4&,const glm::mat4&,float,float,const glm::vec4& = glm::vec4(0.f)){drawn.push_back(mesh);}
enum class Holds { Gun,Controller }; Holds held=Holds::Gun;
Holds holds(int){return held;}
struct Gun { int hand=1,player=0; bool pointedDown=false,offArmed=false,gunRam=false,gunKnown=false; long long reloadAt=0; struct {int ammo=0,magazine=7;}gunWas; };
long long time=1000000000; bool originSet=true,tracked[2]={true,true},onScreen=true,aimOutside=false;
void haptic(int,float,float){}
#define NOTICE_LOG(...)
#include "mok_reload_under_test.inc"
int main(){
 int checks=0,failed=0;
 auto check=[&](bool pass,const char* label){++checks;if(!pass){++failed;std::printf("FAIL %s\n",label);}};
 check(!mokSensorReload,"MOK preserves its measured native reload transport");
 vr::GunInput p1,p2; int x=2500,y=4000;u32 b=0;
 vr::adaptGunInput(p1,mokSensorReload,x,y,b);
 x=y=-10000;vr::adaptGunInput(p1,mokSensorReload,x,y,b);
 check(x==-10000&&y==-10000&&b==0,"native MOK receives off-screen aim with no implicit trigger");
 x=y=-10000;b=2;vr::adaptGunInput(p1,mokSensorReload,x,y,b);
 check(x<0&&y<0&&b==2,"off-screen trigger explicitly signals reload");
 x=y=-10000;b=0;vr::adaptGunInput(p1,mokSensorReload,x,y,b);
 check(x==-10000&&y==-10000&&b==0,"release sends no synthetic sensor pulse");
 x=2500;y=4000;b=128;vr::adaptGunInput(p1,mokSensorReload,x,y,b);
 check(x==2500&&y==4000&&b==128,"B/Y retains MOK native cabinet B reload");
 x=y=-10000;b=0;vr::adaptGunInput(p2,mokSensorReload,x,y,b);
 check(x==-10000&&y==-10000&&b==0,"P2 native input remains independent");
 for(bool slide:{false,true}){
  Gun gun;config::VrReload=0;source.kind=hands::Source::HerosStaff;
  floorReload(gun,slide,true,{0,-1,0});
  check(gun.reloadAt==0,"Shoot mode: pointing at the floor cannot implicitly reload either staff");
 }
 for(bool has:{false,true}){
  Gun legacy;hasSource=has;source.kind=hands::Source::CmGuns;
  floorReload(legacy,false,true,{0,-1,0});
  check(legacy.reloadAt==time,"other game profiles retain their inherited shortcut");
 }
 hasSource=true;source.kind=hands::Source::HerosStaff;
 Gun gun;config::VrReload=1;onScreen=true;pointReload(gun);
 check(gun.offArmed&&gun.reloadAt==0,"Point mode arms on screen");
 onScreen=false;pointReload(gun);
 check(!gun.offArmed&&gun.reloadAt==time,"Point mode requests one reload on leaving the screen");
 gun.reloadAt=0;pointReload(gun);check(!gun.reloadAt,"staying off-screen does not repeat reload");
 onScreen=true;pointReload(gun);onScreen=false;aimOutside=true;pointReload(gun);
 check(!gun.reloadAt,"grey margin does not request reload");
 aimOutside=false;held=Holds::Controller;pointReload(gun);
 check(!gun.reloadAt&&!gun.offArmed,"controller/guide mode cannot reload");
 held=Holds::Gun;config::VrReload=0;onScreen=true;pointReload(gun);
 onScreen=false;pointReload(gun);check(!gun.reloadAt,"Shoot mode never arms implicit point reload");
 Model staff{true};View view;drawn.clear();render(staff,view);
 check(drawn==std::vector<int>({MeshFrame,MeshSlide}),"native staff draws no spare open hand or default fist");
 config::VrStaffHand=true;drawn.clear();render(staff,view);
 check(drawn==std::vector<int>({MeshFrame,MeshSlide,MeshGunHand}),"optional gripping fist does not enable a spare hand");
 Model pistol{false};drawn.clear();render(pistol,view);
 check(drawn==std::vector<int>({MeshFrame,MeshSlide,MeshGunHand,MeshOpenHand}),"pistol slide hand rendering stays available");
 std::printf("%s %d MOK production reload/profile/render checks (%d failed)\n",failed?"FAIL":"PASS",checks,failed);
 return failed?1:0;
}
