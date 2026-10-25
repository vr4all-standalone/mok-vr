// SPDX-License-Identifier: GPL-2.0-or-later
#pragma once
#include <array>
namespace vr::xr {
// Physical buttons sampled during model preparation remain consumed until each
// has actually been released. Apply this before player ownership and hold-menu logic.
struct PreparationInput {
    std::array<bool, 3> blocked{};
    std::array<bool, 3> filter(bool preparing, bool leftStart, bool rightStart, bool secondary)
    {
        const std::array<bool, 3> raw{leftStart, rightStart, secondary};
        std::array<bool, 3> result{};
        for (unsigned i=0;i<raw.size();i++) {
            blocked[i] = preparing ? raw[i] : blocked[i] && raw[i];
            result[i] = raw[i] && !blocked[i];
        }
        return result;
    }
};
}
