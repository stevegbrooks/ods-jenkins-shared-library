package util

import java.util.zip.CRC32

class CRC32OutputStream extends FilterOutputStream {

    private CRC32 crc = new CRC32()

    CRC32OutputStream(OutputStream out) {
        super(out)
    }

    @Override
    void write(byte[] b, int off, int len) {
        if ((off | len | (off + len) | (b.length - (off + len))) < 0)
            throw new IndexOutOfBoundsException();

        out.write(b, off, len)
        crc.update(b, off, len)
    }

    @Override
    void write(int b) {
        out.write(b)
        crc.update(b)
    }

    long getCrc() {
        return crc.value
    }

}
