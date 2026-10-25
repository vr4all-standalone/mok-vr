// hotd2-vr: added in 2026 for The Maze of the Kings VR.
package com.flycast.emulator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** MokFiles finds The Maze of the Kings's three files (made-up stand-ins here, no game data). */
public class MokFilesTest
{
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File games, download;

    @Before
    public void setUp() throws IOException
    {
        games = tmp.newFolder("files", "games");
        download = tmp.newFolder("Download");
    }

    private List<File> roots()
    {
        // as the start screen passes them: the app's own folders (one not there), then Download
        return Arrays.asList(games, new File(tmp.getRoot(), "external/games"), download);
    }

    private File file(File dir, String path) throws IOException
    {
        File f = new File(dir, path);
        f.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(f)) {
            if (path.toLowerCase(java.util.Locale.ROOT).endsWith(".chd")) {
                out.write(chdHeader(5));
            } else if (path.endsWith(".gdi")) {
                out.write("3\n1 0 4 2048 track.bin 0\n2 450 0 2352 track.bin 0\n3 45000 4 2048 track.bin 0\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                file(f.getParentFile(), "track.bin");
            } else {
                out.write(new byte[] { 1, 2, 3, 4 });
            }
        }
        return f;
    }

    private File zip(File dir, String path, String... entries) throws IOException
    {
        File f = new File(dir, path);
        f.getParentFile().mkdirs();
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f))) {
            for (String e : entries) {
                out.putNextEntry(new ZipEntry(e));
                out.write(new byte[e.endsWith(".pic") ? 0x4000 : 0x200000]);
                out.closeEntry();
            }
        }
        return f;
    }

    private static boolean names(List<String> missing, String what)
    {
        for (String line : missing)
            if (line.contains(what))
                return true;
        return false;
    }

    @Test
    public void allThreeThere() throws IOException
    {
        File z = zip(download, "mok.zip", "317-0333-com.pic");
        file(download, "mok/gds-0022.chd");
        zip(download, "naomi.zip", "epr-21578h.ic27");
        MokFiles.Result r = MokFiles.find(roots());
        assertEquals(z.getCanonicalFile(), r.zip.getCanonicalFile());
        assertTrue(r.chd);
        assertEquals("gds-0022.chd", r.disc == null ? null : r.disc.getName());
        assertTrue(r.bios);
        assertTrue(r.missing.toString(), r.missing.isEmpty());
    }

    @Test
    public void noDiscFolder() throws IOException
    {
        zip(download, "mok.zip", "317-0333-com.pic");
        zip(download, "naomi.zip", "epr-21578h.ic27");
        MokFiles.Result r = MokFiles.find(roots());
        assertNotNull(r.zip);
        assertFalse(r.chd);
        assertTrue(r.bios);
        assertEquals(r.missing.toString(), 1, r.missing.size());
        assertTrue(r.missing.toString(), names(r.missing, "mok/gds-0022.chd"));
    }

    @Test
    public void discNotInItsFolder() throws IOException
    {
        zip(download, "mok.zip", "317-0333-com.pic");
        file(download, "mok.chd");
        zip(download, "naomi.zip", "epr-21578h.ic27");
        MokFiles.Result r = MokFiles.find(roots());
        assertFalse(r.chd);
        assertTrue(r.missing.toString(), names(r.missing, "mok/gds-0022.chd"));
    }

    @Test
    public void discAsCueOrGdi() throws IOException
    {
        zip(download, "mok.zip", "317-0333-com.pic");
        file(download, "mok/gds-0022.gdi");
        zip(download, "naomi.zip", "epr-21578h.ic27");
        MokFiles.Result r = MokFiles.find(roots());
        assertTrue(r.chd);
        // the start screen names the disc that's there
        assertEquals("gds-0022.gdi", r.disc == null ? null : r.disc.getName());
        assertTrue(r.missing.toString(), r.missing.isEmpty());
    }

    @Test
    public void biosInsideTheGameZip() throws IOException
    {
        zip(download, "mok.zip", "317-0333-com.pic", "epr-21577h.ic27");
        file(download, "mok/gds-0022.chd");
        MokFiles.Result r = MokFiles.find(roots());
        assertTrue(r.bios);
        assertTrue(r.missing.toString(), r.missing.isEmpty());
    }

    @Test
    public void noBios() throws IOException
    {
        zip(download, "mok.zip", "317-0333-com.pic");
        file(download, "mok/gds-0022.chd");
        MokFiles.Result r = MokFiles.find(roots());
        assertFalse(r.bios);
        assertEquals(r.missing.toString(), 1, r.missing.size());
        assertTrue(r.missing.toString(), names(r.missing, "naomi.zip"));
    }

    @Test
    public void notAZip() throws IOException
    {
        file(download, "mok.zip");
        file(download, "mok/gds-0022.chd");
        MokFiles.Result r = MokFiles.find(roots());
        assertNotNull(r.zip);
        assertFalse(r.bios);
        assertTrue(r.missing.toString(), names(r.missing, "naomi.zip"));
    }

    @Test
    public void zipNameInAnyCase() throws IOException
    {
        zip(download, "Mok.ZIP", "317-0333-com.pic");
        MokFiles.Result r = MokFiles.find(roots());
        assertNotNull(r.zip);
        assertTrue(r.zip.getName().equalsIgnoreCase("mok.zip"));
    }

    @Test
    public void threeFoldersDeep() throws IOException
    {
        zip(download, "a/b/mok.zip", "317-0333-com.pic");
        file(download, "a/b/mok/gds-0022.chd");
        zip(download, "a/b/naomi.zip", "epr-21578h.ic27");
        MokFiles.Result r = MokFiles.find(roots());
        assertNotNull(r.zip);
        assertTrue(r.missing.toString(), r.missing.isEmpty());
    }

    @Test
    public void tooDeep() throws IOException
    {
        zip(download, "a/b/c/d/mok.zip", "317-0333-com.pic");
        MokFiles.Result r = MokFiles.find(roots());
        assertNull(r.zip);
        assertFalse(r.chd);
        assertFalse(r.bios);
        assertEquals(r.missing.toString(), 3, r.missing.size());
        assertTrue(names(r.missing, "mok.zip"));
        assertTrue(names(r.missing, "mok/gds-0022.chd"));
        assertTrue(names(r.missing, "naomi.zip"));
    }

    @Test
    public void nothingThere()
    {
        MokFiles.Result r = MokFiles.find(roots());
        assertNull(r.zip);
        assertEquals(r.missing.toString(), 3, r.missing.size());
    }

    @Test
    public void theCompleteSetWins() throws IOException
    {
        zip(download, "mok.zip", "317-0333-com.pic");      // alone, nothing next to it
        File full = zip(download, "cm/mok.zip", "317-0333-com.pic");
        file(download, "cm/mok/gds-0022.chd");
        zip(download, "cm/naomi.zip", "epr-21578h.ic27");
        MokFiles.Result r = MokFiles.find(roots());
        assertEquals(full.getCanonicalFile(), r.zip.getCanonicalFile());
        assertTrue(r.missing.toString(), r.missing.isEmpty());
    }

    @Test
    public void theAppsOwnFolder() throws IOException
    {
        zip(games, "mok.zip", "317-0333-com.pic", "epr-21578g.ic27");
        file(games, "mok/gds-0022.chd");
        MokFiles.Result r = MokFiles.find(roots());
        assertNotNull(r.zip);
        assertTrue(r.missing.toString(), r.missing.isEmpty());
    }

    @Test
    public void missingLinesSayWhere() throws IOException
    {
        zip(download, "mok.zip", "317-0333-com.pic");
        MokFiles.Result r = MokFiles.find(roots());
        assertEquals(r.missing.toString(), 2, r.missing.size());
        for (String line : r.missing)
            assertTrue(line, line.contains("Download") && line.contains("next to mok.zip"));
    }

    @Test
    public void noZipButTheOtherTwo() throws IOException
    {
        // the disc and the BIOS are there, only the game's zip isn't: one line, for the zip
        file(download, "mok/gds-0022.chd");
        zip(download, "naomi.zip", "epr-21578h.ic27");
        MokFiles.Result r = MokFiles.find(roots());
        assertNull(r.zip);
        assertTrue(r.chd);
        assertTrue(r.bios);
        assertEquals(r.missing.toString(), 1, r.missing.size());
        assertTrue(r.missing.toString(), r.missing.get(0).startsWith("mok.zip"));
        assertTrue(r.missing.toString(), r.missing.get(0).contains("Download"));
    }

    @Test
    public void noZipButTheBios() throws IOException
    {
        zip(download, "naomi.zip", "epr-21578h.ic27");
        MokFiles.Result r = MokFiles.find(roots());
        assertFalse(r.chd);
        assertTrue(r.bios);
        assertEquals(r.missing.toString(), 2, r.missing.size());
        assertTrue(names(r.missing, "mok.zip"));
        assertTrue(names(r.missing, "mok/gds-0022.chd"));
    }

    @Test
    public void biosUnderAnotherNameByItsCrc() throws IOException
    {
        // the emulator finds the BIOS by its CRC first (naomi_cart.cpp): an entry renamed, with
        // epr-21576h.ic27's CRC (synthetic bytes of the expected size, no BIOS data)
        File z = new File(download, "mok.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(z))) {
            out.putNextEntry(new ZipEntry("317-0333-com.pic"));
            out.write(new byte[0x4000]);
            out.closeEntry();
            out.putNextEntry(new ZipEntry("bios.bin"));
            out.write(withCrc(0xd4895685L, 0x200000));
            out.closeEntry();
        }
        file(download, "mok/gds-0022.chd");
        MokFiles.Result r = MokFiles.find(roots());
        assertTrue(r.bios);
        assertTrue(r.missing.toString(), r.missing.isEmpty());
    }

    @Test
    public void theWalkStopsInAHugeTree() throws IOException
    {
        File crowd = new File(download, "a");
        assertTrue(crowd.mkdirs());
        for (int i = 0; i < MokFiles.WALK_LIMIT; i++)
            assertTrue(new File(crowd, "f" + i).createNewFile());
        zip(download, "b/mok.zip", "317-0333-com.pic");    // after the crowd: past the cap
        assertNull(MokFiles.find(roots()).zip);
        zip(download, "mok.zip", "317-0333-com.pic");      // at the top: still found
        assertNotNull(MokFiles.find(roots()).zip);
    }

    /** Synthetic bytes whose CRC-32 is crc (the final four bytes depend only on the four
     *  table indices: found from the top byte down, then the bytes that give them). */
    private static byte[] withCrc(long crc, int length)
    {
        int[] table = new int[256];
        for (int n = 0; n < 256; n++) {
            int c = n;
            for (int k = 0; k < 8; k++)
                c = (c & 1) != 0 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
            table[n] = c;
        }
        // state = T[m3] ^ T[m2] >>> 8 ^ T[m1] >>> 16 ^ T[m0] >>> 24
        int rest = ~(int) crc;
        int[] m = new int[4];
        for (int i = 3; i >= 0; i--) {
            int shift = 8 * (3 - i), top = (rest >>> (24 - shift)) & 0xFF;
            for (int n = 0; n < 256; n++)
                if ((table[n] >>> 24) == top) { m[i] = n; break; }
            rest ^= table[m[i]] >>> shift;
        }
        byte[] out = new byte[length];
        int s = 0xFFFFFFFF;
        for (int i = 0; i < length - 4; i++)
            s = table[s & 0xFF] ^ (s >>> 8);
        for (int i = 0; i < 4; i++) {
            out[length - 4 + i] = (byte) (m[i] ^ (s & 0xFF));
            s = table[m[i]] ^ (s >>> 8);
        }
        CRC32 check = new CRC32();
        check.update(out);
        assertEquals(crc, check.getValue());
        return out;
    }
    private static final String ORIGINAL_SHA1 = "ddd6bf6a93f44f04199b278149ded19b26cdcab4";

    /** Plausible bounded headers/maps, made up without disc payload or game data. */
    private byte[] chdHeader(int version)
    {
        byte[] header = new byte[512];
        java.nio.ByteBuffer b = java.nio.ByteBuffer.wrap(header);
        b.put("MComprHD".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        b.putInt(version == 3 ? 120 : version == 4 ? 108 : 124).putInt(version);
        if (version == 5) {
            b.putLong(32, 9792).putLong(40, 124);
            b.putInt(56, 9792).putInt(60, 2448);
        } else {
            b.putInt(20, 1).putInt(24, 1).putLong(28, 9792);
            b.putInt(version == 3 ? 76 : 44, 9792);
        }
        int sha = version == 3 ? 80 : version == 4 ? 48 : 84;
        String identity = version == 3 ? "ba9d27a248d86218898d9bb5f1fe22fdefb8c03f" : ORIGINAL_SHA1;
        b.position(sha);
        for (int i = 0; i < identity.length(); i += 2)
            b.put((byte) Integer.parseInt(identity.substring(i, i + 2), 16));
        return header;
    }

    private void chdBytes(byte[] header) throws IOException
    {
        java.nio.file.Files.write(new File(download, "mok/gds-0022.chd").toPath(), header);
    }

    private String cue() throws IOException
    {
        return new String(java.nio.file.Files.readAllBytes(new File(download, "mok/gds-0022.cue").toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
    }

    private void complete() throws IOException
    {
        zip(download, "mok.zip", "317-0333-com.pic");
        file(download, "mok/gds-0022.chd");
        zip(download, "naomi.zip", "epr-21576h.ic27");
    }

    private void text(String path, String value) throws IOException
    {
        File f = new File(download, path);
        f.getParentFile().mkdirs();
        java.nio.file.Files.write(f.toPath(), value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test public void misleadingRevANameExplainsOriginalLayout() throws IOException
    {
        zip(download, "notmok.zip", "317-0333-com.pic");
        file(download, "notmok/gds-0022.chd");
        zip(download, "naomi.zip", "epr-21576h.ic27");
        MokFiles.Result r = MokFiles.find(roots());
        assertFalse(r.missing.isEmpty());
        assertTrue(r.missing.toString(), names(r.missing, "mok.zip"));
        assertTrue(r.missing.toString(), names(r.missing, "mok/gds-0022.chd"));
    }

    @Test public void originalSetRejectsRevADiscName() throws IOException
    {
        zip(download, "mok.zip", "317-0333-com.pic");
        file(download, "mok/gds-9999.chd");
        zip(download, "naomi.zip", "epr-21576h.ic27");
        MokFiles.Result r = MokFiles.find(roots());
        assertFalse(r.chd);
        assertTrue(r.missing.toString(), names(r.missing, "GDS-0022"));
    }

    @Test public void wrongChdRevisionNeverReady() throws IOException
    {
        complete();
        File f = new File(download, "mok/gds-0022.chd");
        try (java.io.RandomAccessFile out = new java.io.RandomAccessFile(f, "rw")) {
            out.seek(84);
            out.write(0x7f);
        }
        MokFiles.Result r = MokFiles.find(roots());
        assertFalse(r.chd);
        assertTrue(r.missing.toString(), names(r.missing, "original GDS-0022"));
    }

    @Test public void invalidChdNeverReady() throws IOException
    {
        complete();
        text("mok/gds-0022.chd", "not a CHD");
        assertFalse(MokFiles.find(roots()).chd);
    }

    @Test public void parentDependentChdNeverReady() throws IOException
    {
        complete();
        File f = new File(download, "mok/gds-0022.chd");
        try (java.io.RandomAccessFile out = new java.io.RandomAccessFile(f, "rw")) {
            out.seek(104);
            out.write(1);
        }
        assertFalse(MokFiles.find(roots()).chd);
    }

    @Test public void missingPicNeverReady() throws IOException
    {
        complete();
        zip(download, "mok.zip", "other.bin");
        MokFiles.Result r = MokFiles.find(roots());
        assertTrue(r.missing.toString(), names(r.missing, "317-0333-com.pic"));
    }

    @Test public void truncatedPicNeverReady() throws IOException
    {
        complete();
        File f = new File(download, "mok.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f))) {
            out.putNextEntry(new ZipEntry("317-0333-com.pic"));
            out.write(1);
            out.closeEntry();
        }
        assertFalse(MokFiles.find(roots()).missing.isEmpty());
    }

    @Test public void arbitraryBiosZipNeverReady() throws IOException
    {
        complete();
        text("naomi.zip", "not a ZIP");
        assertFalse(MokFiles.find(roots()).bios);
    }

    @Test public void unsupportedBiosNeverReady() throws IOException
    {
        complete();
        zip(download, "naomi.zip", "epr-21576e.ic27");
        assertFalse(MokFiles.find(roots()).bios);
    }

    @Test public void emptyBiosEntryNeverReady() throws IOException
    {
        complete();
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(new File(download, "naomi.zip")))) {
            out.putNextEntry(new ZipEntry("epr-21576h.ic27"));
            out.closeEntry();
        }
        assertFalse(MokFiles.find(roots()).bios);
    }

    @Test public void completeCueUsesAllTracks() throws IOException
    {
        zip(download, "mok.zip", "317-0333-com.pic", "epr-21576h.ic27");
        text("mok/gds-0022.cue", "REM SINGLE-DENSITY AREA\nFILE \"track 1.bin\" BINARY\n TRACK 01 MODE1/2048\n INDEX 01 00:00:00\nFILE \"track 2.bin\" BINARY\n TRACK 02 AUDIO\n INDEX 01 00:00:00\nREM HIGH-DENSITY AREA\nFILE \"track 3.bin\" BINARY\n TRACK 03 MODE1/2048\n INDEX 01 00:00:00\n");
        file(download, "mok/track 1.bin");
        file(download, "mok/track 2.bin");
        file(download, "mok/track 3.bin");
        assertTrue(MokFiles.find(roots()).missing.toString(), MokFiles.find(roots()).missing.isEmpty());
    }

    @Test public void incompleteCueNeverReady() throws IOException
    {
        completeCueUsesAllTracks();
        assertTrue(new File(download, "mok/track 2.bin").delete());
        assertFalse(MokFiles.find(roots()).chd);
    }

    @Test public void incompleteGdiNeverReady() throws IOException
    {
        discAsCueOrGdi();
        assertTrue(new File(download, "mok/track.bin").delete());
        assertFalse(MokFiles.find(roots()).chd);
    }

    @Test public void wrongGdiTrackCountNeverReady() throws IOException
    {
        discAsCueOrGdi();
        text("mok/gds-0022.gdi", "2\n1 45000 4 2048 track.bin 0\n");
        assertFalse(MokFiles.find(roots()).chd);
    }

    @Test public void malformedCueNeverReady() throws IOException
    {
        complete();
        assertTrue(new File(download, "mok/gds-0022.chd").delete());
        text("mok/gds-0022.cue", "not a cue");
        assertFalse(MokFiles.find(roots()).chd);
    }

    @Test public void revisionFolderCannotSupplyOriginalDisc() throws IOException
    {
        zip(download, "mok.zip", "317-0333-com.pic", "epr-21576h.ic27");
        file(download, "notmok/gds-9999.chd");
        assertFalse(MokFiles.find(roots()).chd);
    }

    @Test public void aHugeSingleDirectoryStopsAtLimit() throws IOException
    {
        for (int i = 0; i < MokFiles.WALK_LIMIT; i++)
            assertTrue(new File(download, String.format(java.util.Locale.ROOT, "a%05d", i)).createNewFile());
        zip(download, "mok.zip", "317-0333-com.pic");
        assertNull(MokFiles.find(roots()).zip);
    }    @Test public void explicitOriginalLaunchIsValidated() throws IOException
    {
        complete();
        assertEquals(new File(download, "mok.zip").getAbsolutePath(),
                MokFiles.launchPath(new File(download, "mok.zip"), roots()));
    }

    @Test public void explicitForeignGameCannotBypassSetup() throws IOException
    {
        complete();
        File other = zip(download, "confmiss.zip", "317-0333-com.pic");
        assertNull(MokFiles.launchPath(other, roots()));
    }

    @Test public void explicitDiscCannotBypassGameZip() throws IOException
    {
        complete();
        assertNull(MokFiles.launchPath(new File(download, "mok/gds-0022.chd"), roots()));
    }

    @Test public void explicitMissingFilesCannotBypassSetup() throws IOException
    {
        File incomplete = zip(games, "mok.zip", "317-0333-com.pic");
        complete();
        assertNull(MokFiles.launchPath(incomplete, roots()));
    }

    @Test public void launchWithoutExplicitPathDiscoversOriginal() throws IOException
    {
        complete();
        assertEquals(new File(download, "mok.zip").getAbsolutePath(), MokFiles.launchPath(null, roots()));
    }

    @Test public void caseVariantsResolveToEngineNames() throws IOException
    {
        zip(download, "MOK.ZIP", "317-0333-com.pic", "epr-21576h.ic27");
        file(download, "MOK/GDS-0022.CHD");
        // The shared-storage layout is case insensitive; a case-sensitive filesystem must instead
        // explain the required canonical names and keep Play disabled.
        MokFiles.Result r = MokFiles.find(roots());
        assertNotNull(r.zip);
        if (new File(download, "mok/gds-0022.chd").exists())
            assertTrue(r.missing.toString(), r.missing.isEmpty());
        else
            assertFalse(r.missing.isEmpty());
    }    @Test public void gdiNeedsThreeTracksLikeEngine() throws IOException
    {
        discAsCueOrGdi();
        text("mok/gds-0022.gdi", "1\n1 0 4 2048 track.bin 0\n");
        assertFalse(MokFiles.find(roots()).chd);
    }

    @Test public void gdiNeedsAudioSecondTrack() throws IOException
    {
        discAsCueOrGdi();
        text("mok/gds-0022.gdi", "3\n1 0 4 2048 track.bin 0\n2 450 4 2048 track.bin 0\n3 45000 4 2048 track.bin 0\n");
        assertFalse(MokFiles.find(roots()).chd);
    }

    @Test public void gdiNeedsHighDensityTrackAt45000() throws IOException
    {
        discAsCueOrGdi();
        text("mok/gds-0022.gdi", "3\n1 0 4 2048 track.bin 0\n2 450 0 2352 track.bin 0\n3 450 4 2048 track.bin 0\n");
        assertFalse(MokFiles.find(roots()).chd);
    }

    @Test public void cueNeedsIndexForEachTrack() throws IOException
    {
        completeCueUsesAllTracks();
        File f = new File(download, "mok/gds-0022.cue");
        String cue = new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        text("mok/gds-0022.cue", cue.replace(" INDEX 01 00:00:00", ""));
        assertFalse(MokFiles.find(roots()).chd);
    }

    @Test public void cueNeedsHighDensityAreaForNaomi() throws IOException
    {
        completeCueUsesAllTracks();
        File f = new File(download, "mok/gds-0022.cue");
        String cue = new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        text("mok/gds-0022.cue", cue.replace("REM HIGH-DENSITY AREA", ""));
        assertFalse(MokFiles.find(roots()).chd);
    }    @Test public void biosEntryNameIsCaseSensitiveLikeEngineUnlessCrcMatches() throws IOException
    {
        complete();
        zip(download, "naomi.zip", "EPR-21576H.IC27");
        assertFalse(MokFiles.find(roots()).bios);
    }

    @Test public void picEntryNameIsCaseSensitiveLikeEngineUnlessCrcMatches() throws IOException
    {
        complete();
        File f = new File(download, "mok.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f))) {
            out.putNextEntry(new ZipEntry("317-0333-COM.PIC"));
            out.write(new byte[0x4000]);
            out.closeEntry();
        }
        assertFalse(MokFiles.find(roots()).missing.isEmpty());
    }
    @Test public void supportedChdHeaderVersionsRemainReady() throws IOException
    {
        complete();
        for (int version : new int[] { 3, 4, 5 }) {
            chdBytes(chdHeader(version));
            assertTrue("CHD v" + version, MokFiles.find(roots()).chd);
        }
    }

    @Test public void zeroChdHunkSizeNeverReady() throws IOException
    {
        complete();
        for (int version : new int[] { 3, 4, 5 }) {
            byte[] header = chdHeader(version);
            java.nio.ByteBuffer.wrap(header).putInt(version == 3 ? 76 : version == 4 ? 44 : 56, 0);
            chdBytes(header);
            assertFalse("CHD v" + version, MokFiles.find(roots()).chd);
        }
    }

    @Test public void chdHunksMustContainWholeCdFrames() throws IOException
    {
        complete();
        for (int version : new int[] { 3, 4, 5 }) {
            byte[] header = chdHeader(version);
            java.nio.ByteBuffer.wrap(header).putInt(version == 3 ? 76 : version == 4 ? 44 : 56, 9793);
            chdBytes(header);
            assertFalse("partial CD frame v" + version, MokFiles.find(roots()).chd);
        }
    }

    @Test public void zeroChdUnitSizeNeverReady() throws IOException
    {
        complete();
        byte[] header = chdHeader(5);
        java.nio.ByteBuffer.wrap(header).putInt(60, 0);
        chdBytes(header);
        assertFalse(MokFiles.find(roots()).chd);
    }

    @Test public void impossibleChdCountsAndSizesNeverReady() throws IOException
    {
        complete();
        for (int version : new int[] { 3, 4, 5 }) {
            byte[] header = chdHeader(version);
            java.nio.ByteBuffer.wrap(header).putInt(version == 3 ? 76 : version == 4 ? 44 : 56,
                    version < 5 ? 16 * 1024 * 1024 : 128 * 1024 * 1024);
            chdBytes(header);
            assertFalse("oversized hunk v" + version, MokFiles.find(roots()).chd);
            header = chdHeader(version);
            java.nio.ByteBuffer.wrap(header).putLong(version < 5 ? 28 : 32, 0);
            chdBytes(header);
            assertFalse("empty logical disc v" + version, MokFiles.find(roots()).chd);
            if (version < 5) {
                header = chdHeader(version);
                java.nio.ByteBuffer.wrap(header).putInt(24, 0);
                chdBytes(header);
                assertFalse("zero hunks v" + version, MokFiles.find(roots()).chd);
            }
        }
    }

    @Test public void truncatedOrOutOfFileChdMapsNeverReady() throws IOException
    {
        complete();
        byte[] header = chdHeader(5);
        java.nio.ByteBuffer.wrap(header).putLong(40, 510);
        chdBytes(header);
        assertFalse("v5 map beyond file", MokFiles.find(roots()).chd);
        chdBytes(Arrays.copyOf(chdHeader(5), 128));
        assertFalse("v5 truncated at map end", MokFiles.find(roots()).chd);
        header = chdHeader(5);
        java.nio.ByteBuffer.wrap(header).putInt(16, 0x63646c7a).putInt(124, 500);
        chdBytes(header);
        assertFalse("v5 compressed map beyond file", MokFiles.find(roots()).chd);
        for (int version : new int[] { 3, 4 }) {
            header = chdHeader(version);
            java.nio.ByteBuffer.wrap(header).putInt(24, 40);
            chdBytes(header);
            assertFalse("legacy map beyond file v" + version, MokFiles.find(roots()).chd);
        }
    }

    @Test public void outOfFileChdMetadataNeverReady() throws IOException
    {
        complete();
        for (int version : new int[] { 3, 4, 5 }) {
            byte[] header = chdHeader(version);
            java.nio.ByteBuffer.wrap(header).putLong(version < 5 ? 36 : 48, 510);
            chdBytes(header);
            assertFalse("metadata beyond file v" + version, MokFiles.find(roots()).chd);
        }
    }

    @Test public void cueHighDensityIndexMustStartAt45150() throws IOException
    {
        completeCueUsesAllTracks();
        text("mok/gds-0022.cue", cue().replace("TRACK 03 MODE1/2048\n INDEX 01 00:00:00",
                "TRACK 03 MODE1/2048\n INDEX 01 00:01:00"));
        assertFalse(MokFiles.find(roots()).chd);
    }

    @Test public void cueIndexFramesMustBeUnder75() throws IOException
    {
        completeCueUsesAllTracks();
        String original = cue();
        for (String index : new String[] { "00:00:75", "00:00:79", "00:60:00" }) {
            text("mok/gds-0022.cue", original.replace("INDEX 01 00:00:00", "INDEX 01 " + index));
            assertFalse(index, MokFiles.find(roots()).chd);
        }
    }

    @Test public void cueHighDensityMarkerMustPrecedeNewFileContext() throws IOException
    {
        completeCueUsesAllTracks();
        String original = cue();
        text("mok/gds-0022.cue", original.replace("REM HIGH-DENSITY AREA\nFILE \"track 3.bin\" BINARY",
                "FILE \"track 3.bin\" BINARY\nREM HIGH-DENSITY AREA"));
        assertFalse("REM clears the native FILE context", MokFiles.find(roots()).chd);
    }

    @Test public void cueIndexZeroNeedsPendingTrackAndCorrectOrder() throws IOException
    {
        completeCueUsesAllTracks();
        String original = cue();
        for (String invalid : new String[] {
                "INDEX 00 00:00:00\n" + original,
                original.replace("INDEX 01 00:00:00", "INDEX 01 00:00:00\n INDEX 00 00:00:00"),
                original.replace("INDEX 01 00:00:00", "INDEX 00 00:00:01\n INDEX 01 00:00:00") }) {
            text("mok/gds-0022.cue", invalid);
            assertFalse(MokFiles.find(roots()).chd);
        }
        text("mok/gds-0022.cue", original.replace("INDEX 01 00:00:00", "INDEX 00 00:00:00\n INDEX 01 00:00:00"));
        assertTrue("valid INDEX 00/01 ordering", MokFiles.find(roots()).chd);
    }

    @Test public void staleCueMustNotHideBehindValidGdi() throws IOException
    {
        discAsCueOrGdi();
        text("mok/gds-0022.cue", "FILE track.bin BINARY\n TRACK 01 MODE1/2048\n INDEX 01 00:00:00\n");
        MokFiles.Result r = MokFiles.find(roots());
        assertFalse("the engine chooses the stale parseable CUE first", r.chd);
        assertNull(MokFiles.launchPath(null, roots()));
    }
}
