package util

class CountingSinkOutputStream extends OutputStream {

    private long count = 0;

    @Override
    void write(int b) throws IOException {
        count++
    }

    @Override
    void write(byte[] b, int off, int len) throws IOException {
        if ((off | len | (off + len) | (b.length - (off + len))) < 0)
            throw new IndexOutOfBoundsException();

        count += len
    }

    long getCount() {
        return count
    }

}
