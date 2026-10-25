// SPDX-License-Identifier: GPL-2.0-or-later
#pragma once
#include <cstdint>
#include <cstring>
#include <vector>
#include <string>
#include <fstream>
#include <filesystem>

namespace vr::hands {
// The staff extractor writes HND1 v1, including optional glove meshes. Bound and
// validate the same payload before both preparation reuse and renderer loading.
inline bool validateStaffCache(const std::vector<uint8_t>& data)
{
    if (data.size() < 136 || data.size() > 16 * 1024 * 1024
            || std::memcmp(data.data(), "HND1", 4) != 0)
        return false;
    size_t at = 4;
    bool ok = true;
    auto word = [&]() -> uint32_t {
        if (data.size() - at < 4) { ok = false; return 0; }
        uint32_t value = 0;
        for (unsigned i = 0; i < 4; i++) value |= uint32_t(data[at++]) << (8 * i);
        return value;
    };
    auto skip = [&](size_t count) {
        if (count > data.size() - at) ok = false;
        else at += count;
    };
    if (word() != 1) return false;
    for (int i = 0; i < 23; i++) {
        uint32_t bits = word();
        if (!ok || (bits & 0x7f800000u) == 0x7f800000u
                || (i == 6 && (bits & 0x7fffffffu) != 0)) return false;
    }
    const uint32_t textures = word();
    if (!ok || textures == 0 || textures > 64) return false;
    for (uint32_t i = 0; i < textures; i++) {
        const uint32_t width = word(), height = word(), flags = word();
        if (!ok || width == 0 || height == 0 || width > 1024 || height > 1024
                || (flags & ~31u)) return false;
        skip(size_t(width) * height * 4);
        if (!ok) return false;
    }
    if (word() != 4 || !ok) return false;
    for (int mesh = 0; mesh < 4; mesh++) {
        const uint32_t parts = word();
        if (!ok || parts > 64 || (mesh == 0 && parts == 0) || (mesh == 1 && parts != 0)) return false;
        for (uint32_t p = 0; p < parts; p++) {
            const uint32_t texture = word(), vertices = word();
            if (!ok || texture >= textures || vertices == 0 || vertices > 65536
                    || vertices % 3 != 0 || size_t(vertices) * 36 > data.size() - at) return false;
            for (uint32_t v = 0; v < vertices; v++) {
                for (int f = 0; f < 8; f++)
                    if ((word() & 0x7f800000u) == 0x7f800000u || !ok) return false;
                skip(4);
            }
        }
    }
    return ok && at == data.size();
}

inline bool loadStaffCache(const std::string& path)
{
    try {
        std::ifstream in(std::filesystem::u8path(path), std::ios::binary | std::ios::ate);
        const auto size = in.tellg();
        if (!in || size <= 0 || size > 16 * 1024 * 1024) return false;
        std::vector<uint8_t> data(static_cast<size_t>(size));
        in.seekg(0);
        return bool(in.read(reinterpret_cast<char*>(data.data()), size)) && validateStaffCache(data);
    } catch (const std::exception&) { return false; }
}
}
