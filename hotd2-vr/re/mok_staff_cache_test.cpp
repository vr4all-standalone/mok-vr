// SPDX-License-Identifier: GPL-2.0-or-later
#include "rend/vr/staff_cache.h"
#include <cstdio>
#include <cstring>
#include <vector>
#include <cstdint>
static void word(std::vector<uint8_t>& b, uint32_t w) {
    for(int i=0;i<4;i++) b.push_back(static_cast<uint8_t>(w>>(8*i)));
}
static void set(std::vector<uint8_t>& b, size_t at, uint32_t w) {
    for(int i=0;i<4;i++)b[at+i]=static_cast<uint8_t>(w>>(8*i));
}
int main() {
    int passed=0,failed=0;
    auto check=[&](bool ok,const char* name){(ok?passed:failed)++;if(!ok)std::printf("FAIL %s\n",name);};
    std::vector<uint8_t> good{'H','N','D','1'};
    word(good,1); for(int i=0;i<23;i++)word(good, i==2 ? 0xbf800000 : 0);
    word(good,1); word(good,2); word(good,2); word(good,0);
    good.insert(good.end(),16,255); word(good,4);
    word(good,1); word(good,0); word(good,3); good.insert(good.end(),108,0);
    word(good,0);word(good,0);word(good,0);
    check(vr::hands::validateStaffCache(good),"complete v1 staff accepted");
    for(size_t n=0;n<good.size();n++)check(!vr::hands::validateStaffCache({good.begin(),good.begin()+n}),"truncated staff rejected");
    for(auto change: std::vector<std::pair<size_t,uint32_t>>{{4,2},{8,0x7fc00000},{32,0x3f800000},{100,0},{100,65},{104,0},{104,2048},{112,32},{132,5},{136,0},{140,1},{144,4},{148,0x7f800000}}) {
        auto bad=good;set(bad,change.first,change.second);check(!vr::hands::validateStaffCache(bad),"malformed staff rejected");
    }
    auto extra=good;extra.push_back(0);check(!vr::hands::validateStaffCache(extra),"trailing data rejected");
    auto wrong=good;wrong[0]='X';check(!vr::hands::validateStaffCache(wrong),"wrong magic rejected");
    std::printf("MOK staff cache: %d passed, %d failed\n",passed,failed);
    return failed?1:0;
}
