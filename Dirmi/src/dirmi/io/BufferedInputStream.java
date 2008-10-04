/*
 *  Copyright 2008 Brian S O'Neill
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dirmi.io;

import java.io.InputStream;
import java.io.IOException;

/**
 * Replacement for {@link java.io.BufferedInputStream} which does not have the
 * block forever on read bug. Marking is not supported. Any exception thrown by
 * the underlying stream causes it to be automatically closed. In addition,
 * this stream never returns the EOF marker. Instead, it throws an exception.
 *
 * @author Brian S O'Neill
 */
public class BufferedInputStream extends AbstractBufferedInputStream {
    private final InputStream mIn;

    BufferedInputStream(InputStream in) {
        mIn = in;
    }

    @Override
    public synchronized int read() throws IOException {
        int b = super.read();
        if (b < 0) {
            forceClose();
            throw new IOException("Closed");
        }
        return b;
    }

    @Override
    public synchronized int available() throws IOException {
        try {
            int available = super.available();
            if (available <= 0) {
                available = mIn.available();
                if (available < 0) {
                    throw new IOException("Closed");
                }
            }
            return available;
        } catch (IOException e) {
            forceClose();
            throw e;
        }
    }

    @Override
    public synchronized void close() throws IOException {
        mIn.close();
    }

    @Override
    protected int doRead(byte[] buffer, int offset, int length) throws IOException {
        try {
            int amt = mIn.read(buffer, offset, length);
            if (amt <= 0) {
                throw new IOException("Closed");
            }
            return amt;
        } catch (IOException e) {
            forceClose();
            throw e;
        }
    }

    @Override
    protected long doSkip(long n) throws IOException {
        try {
            long amt = mIn.skip(n);
            if (amt < 0) {
                throw new IOException("Closed");
            }
            return amt;
        } catch (IOException e) {
            forceClose();
            throw e;
        }
    }

    private void forceClose() {
        try {
            mIn.close();
        } catch (IOException e2) {
            // Ignore.
        }
    }
}
