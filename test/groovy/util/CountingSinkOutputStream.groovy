package util

class CountingSinkOutputStream extends OutputStream {

    private long count = 0;

    @Override
    public void write(int b) throws IOException {
        count++
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException()
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
            ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException()
        }
        count += len
    }

    long getCount() {
        return count
    }

}
