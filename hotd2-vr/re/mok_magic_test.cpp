#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <algorithm>
#include <array>
#include <cmath>
#include <cstdio>
#include <vector>
enum class GunKind {Arcade,Pistol,Rifle,Custom};
struct HandsModel {glm::vec3 muzzle{0.f,0.f,-.75f};bool staff=true;}nativeModel,customModel{{0,.05f,-.14f},false};
bool nativeLoaded=true;
const HandsModel* handModel(GunKind kind){return kind==GunKind::Pistol&&nativeLoaded?&nativeModel:kind==GunKind::Custom?&customModel:nullptr;}
const glm::vec3 GunMuzzle{0,.0471f,-.1401f};
namespace theme {const glm::vec3 Accent{.6f,.4f,1.f},Text{1.f};}
#include "magic_types.inc"
using GLuint=unsigned;
GLuint modelProgram=0,glowProgram=1,modelVbo=0,modelIbo=0,modelVao=0,glowVbo=0,glowVao=0;
int gMvp=0,gShape=0,gStarAngle=0,mMvp=0,mModel=0,mEye=0,mColor=0,mSpecular=0,mShininess=0,mEmissive=0,mLight=0;
enum {GL_FALSE,GL_TRUE,GL_BLEND,GL_DEPTH_TEST,GL_LESS,GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA,GL_ONE};
struct Cache {void UseProgram(unsigned){}void Enable(int){}void Disable(int){}void DepthFunc(int){}void DepthMask(int){}void BlendFunc(int,int){}void DeleteProgram(unsigned){}}glcache;
void glUniformMatrix4fv(int,int,int,const float*){}void glUniform1f(int,float){}void glDeleteBuffers(int,const unsigned*){}void deleteVertexArray(unsigned){}void termHands(){}void logGlErrors(const char*){}
bool initGlow(){return true;}
std::array<std::vector<GlowVertex>,16> recorded;
void drawGlowShape(Shape shape,const std::vector<GlowVertex>& vertices){recorded[shape].insert(recorded[shape].end(),vertices.begin(),vertices.end());}
#include "magic_functions.inc"
int main(){
 int checks=0,failures=0;
 auto check=[&](bool pass,const char* message){++checks;if(!pass){++failures;std::printf("FAIL %s\n",message);}};
 auto reset=[&](){termGun();nativeModel.staff=true;nativeLoaded=true;for(auto& vertices:recorded)vertices.clear();};
 auto draw=[&](const GunView& gun){for(auto& vertices:recorded)vertices.clear();drawGunGlow(glm::mat4(1.f),{0,1.6f,0},gun,true);};
 auto colored=[&](bool green){size_t n=0;for(size_t i=0;i<recorded.size();i++)if(i!=Line&&i!=Dot&&i!=Star&&i!=Flame&&i!=Smoke)for(auto& v:recorded[i])if(green?v.color.g>v.color.r*1.5f&&v.color.b>.15f:v.color.r>.8f&&v.color.g>.5f&&v.color.b<.5f)++n;return n;};
 GunView staff;staff.kind=GunKind::Pistol;staff.shot=1;staff.now=1;staff.sinceShot=.01f;staff.restMuzzle={0,0,-.75f};
 reset();draw(staff);
 check(smoke.empty()&&recorded[Smoke].empty(),"native staff emits no ballistic smoke");
 check(recorded[Flame].empty()&&recorded[Star].empty(),"native staff emits no orange flame or gun starburst");
 check(colored(true)>0,"native staff creates an emerald magical pulse");
 check(colored(false)>0,"native staff creates small golden magic sparks");
 check(magicCount()==1,"one fresh staff shot produces one spell burst");
 drawGunGlow(glm::mat4(1.f),{.032f,1.6f,0},staff,false);
 check(magicCount()==1,"second stereo eye cannot duplicate the burst");
 staff.player=1;draw(staff);check(magicCount()==2&&colored(true)>0,"P2 creates an independent spell with the same shot number");
 for(float age:{.02f,.06f,.12f,.20f,.28f,.5f}){
  staff.now=1+age;staff.sinceShot=age;draw(staff);
  bool finite=true;for(auto& vertices:recorded)for(auto& v:vertices)for(int k=0;k<3;++k)finite=finite&&std::isfinite(v.pos[k])&&std::isfinite(v.color[k])&&v.color[k]>=0&&v.color[k]<=1;
  check(finite,"spell vertices and color remain finite while fading");
 }
 check(magicCount()==0&&colored(true)==0&&colored(false)==0,"spell particles expire instead of lingering like smoke");
 for(auto kind:{GunKind::Arcade,GunKind::Custom,GunKind::Pistol}){
  reset();nativeModel.staff=false;GunView gun=staff;gun.kind=kind;gun.player=0;gun.shot=1;gun.now=2;gun.sinceShot=.01f;draw(gun);
  check(!smoke.empty()&&!recorded[Flame].empty()&&magicCount()==0,"arcade, custom and other native pistols retain their firing effects");
 }
 reset();nativeLoaded=false;staff.player=0;staff.now=2;staff.sinceShot=.01f;draw(staff);
 check(!smoke.empty()&&magicCount()==0,"missing native staff keeps the fallback gun effect");
 reset();staff.menuPointer=true;draw(staff);
 check(smoke.empty()&&magicCount()==0&&recorded[Flame].empty(),"menu pointer cannot emit a weapon effect");
 reset();staff.menuPointer=false;staff.shot=0;staff.now=3;staff.sinceShot=1e9f;draw(staff);
 check(smoke.empty()&&magicCount()==0,"idle or reload without a shot produces no magic");
 staff.aimLine=staff.aimDot=true;staff.aimPoint={0,0,-3};draw(staff);
 check(!recorded[Line].empty()&&!recorded[Dot].empty(),"staff laser and aim dot remain available");staff.aimLine=staff.aimDot=false;
 reset();size_t worst=0;
 for(unsigned shot=1;shot<501;shot++){
  staff.shot=shot;staff.now=4+shot*.002;staff.sinceShot=.01f;staff.player=shot&1;draw(staff);
  size_t count=0;for(auto& vertices:recorded)count+=vertices.size();worst=std::max(worst,count);
 }
 check(magicCount()>0&&magicCount()<=24&&worst<=1152,"rapid two-player firing has bounded spell storage and geometry");
 termGun();check(magicCount()==0&&smoke.empty()&&lastShot[0]==0&&lastShot[1]==0,"GL context reset clears all old weapon effects");
 std::printf("%s %d production magic/legacy renderer checks (%d failures)\n",failures?"FAIL":"PASS",checks,failures);
 return failures?1:0;
}
