// SPDX-License-Identifier: GPL-2.0-or-later
#pragma once
#include <string_view>
namespace vr {
inline const char *standaloneGameError(std::string_view gameId)
{
#if defined(VR_STANDALONE_MOK) && VR_STANDALONE_MOK
    if (gameId != "THE MAZE OF THE KINGS")
        return "The Maze of the Kings VR only supports The Maze of the Kings (GDS-0022, mok.zip).";
#endif
    return nullptr;
}
}
