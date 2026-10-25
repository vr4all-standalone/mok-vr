// SPDX-License-Identifier: GPL-2.0-or-later
#include "rend/vr/preparation_input.h"
#include <cstdio>
int main(){
 int passed=0,failed=0;
 auto check=[&](bool ok){(ok?passed:failed)++;};
 for(unsigned during=0;during<8;during++)for(unsigned after=0;after<8;after++) {
  vr::xr::PreparationInput gate;
  auto held=gate.filter(true,during&1,during&2,during&4);
  for(unsigned i=0;i<3;i++)check(!held[i]);
  auto done=gate.filter(false,after&1,after&2,after&4);
  for(unsigned i=0;i<3;i++)check(done[i]==bool((after&(1<<i))&&!(during&(1<<i))));
  auto still=gate.filter(false,after&1,after&2,after&4);
  check(done==still);
  auto released=gate.filter(false,false,false,false);
  for(bool b:released)check(!b);
  auto fresh=gate.filter(false,true,true,true);
  for(bool b:fresh)check(b);
 }
 vr::xr::PreparationInput gate;
 gate.filter(true,true,true,true);gate.filter(true,false,false,false);
 auto done=gate.filter(false,true,true,true);for(bool b:done)check(b);
 std::printf("MOK preparation input: %d passed, %d failed\n",passed,failed);
 return failed?1:0;
}
