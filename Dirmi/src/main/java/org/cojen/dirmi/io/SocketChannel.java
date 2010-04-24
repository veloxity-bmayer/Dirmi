/*
 *  Copyright 2010 Brian S O'Neill
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

package org.cojen.dirmi.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.Socket;

import java.util.Map;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 
 *
 * @author Brian S O'Neill
 */
abstract class SocketChannel implements Channel {
    public static SimpleSocket toSimpleSocket(final Socket socket) {
        return new SimpleSocket() {
            public void close() throws IOException {
                socket.close();
            }

            public Object getLocalAddress() {
                return socket.getLocalSocketAddress();
            }

            public Object getRemoteAddress() {
                return socket.getRemoteSocketAddress();
            }

            public InputStream getInputStream() throws IOException {
                return socket.getInputStream();
            }

            public OutputStream getOutputStream() throws IOException {
                return socket.getOutputStream();
            }
        };
    }

    private static final AtomicIntegerFieldUpdater<SocketChannel> closedUpdater =
        AtomicIntegerFieldUpdater.newUpdater(SocketChannel.class, "mClosed");

    private final IOExecutor mExecutor;
    private final SimpleSocket mSocket;
    private final ChannelInputStream mIn;
    private final ChannelOutputStream mOut;
    private final Map<Channel, Object> mAccepted;

    private volatile int mClosed;

    SocketChannel(IOExecutor executor, SimpleSocket socket, Map<Channel, Object> accepted)
        throws IOException
    {
        mExecutor = executor;
        mSocket = socket;
        mIn = createInputStream(socket);
        mOut = createOutputStream(socket);
        if (accepted != null) {
            accepted.put(this, "");
        }
        mAccepted = accepted;
    }

    /**
     * Copy constructor which accepts different streams.
     */
    SocketChannel(SocketChannel channel, ChannelInputStream in, ChannelOutputStream out) {
        mExecutor = channel.mExecutor;
        mSocket = channel.mSocket;
        mIn = in;
        mOut = out;
        if ((mAccepted = channel.mAccepted) != null) {
            mAccepted.put(this, "");
            mAccepted.remove(channel);
        }
    }

    public ChannelInputStream getInputStream() {
        return mIn;
    }

    public ChannelOutputStream getOutputStream() {
        return mOut;
    }

    public Object getLocalAddress() {
        return mSocket.getLocalAddress();
    }

    public Object getRemoteAddress() {
        return mSocket.getRemoteAddress();
    }

    public boolean isInputReady() throws IOException {
        return mIn.isReady();
    }

    public boolean isOutputReady() throws IOException {
        return mOut.isReady();
    }

    public int setInputBufferSize(int size) {
        return mIn.setBufferSize(size);
    }

    public int setOutputBufferSize(int size) {
        return mOut.setBufferSize(size);
    }

    public void inputNotify(Channel.Listener listener) {
        mIn.inputNotify(mExecutor, listener);
    }

    public void outputNotify(Channel.Listener listener) {
        mOut.outputNotify(mExecutor, listener);
    }

    public boolean inputResume() {
        return mIn.inputResume();
    }

    public boolean isResumeSupported() {
        return mIn.isResumeSupported();
    }

    public boolean outputSuspend() throws IOException {
        return mOut.outputSuspend();
    }

    @Override
    public String toString() {
        return "Channel {localAddress=" + getLocalAddress() +
            ", remoteAddress=" + getRemoteAddress() + '}';
    }

    public void flush() throws IOException {
        mOut.flush();
    }

    public boolean usesSelectNotification() {
        return false;
    }

    public boolean isClosed() {
        return mClosed != 0;
    }

    public void close() throws IOException {
        mClosed = 1;

        if (mAccepted != null) {
            mAccepted.remove(this);
        }

        try {
            // Ensure buffer is flushed before closing socket.
            mOut.outputClose();
        } catch (IOException e) {
            try {
                mSocket.close();
            } catch (IOException e2) {
                // Ignore.
            }
            throw e;
        }

        mSocket.close();

        // Input must must always be explicitly closed to ensure that
        // subsequent reads throw ClosedException. Do so after socket close in
        // case buffered implementation blocks concurrent close while reading.
        mIn.inputClose();
    }

    public void disconnect() {
        mClosed = 1;

        if (mAccepted != null) {
            mAccepted.remove(this);
        }

        mOut.outputDisconnect();
        mIn.inputDisconnect();

        try {
            mSocket.close();
        } catch (IOException e) {
            // Ignore.
        }
    }

    protected IOExecutor executor() {
        return mExecutor;
    }

    protected SimpleSocket socket() {
        return mSocket;
    }

    /**
     * @return true if just marked closed
     */
    protected boolean markClosed() {
        return closedUpdater.compareAndSet(this, 0, 1);
    }

    /**
     * Called by constructor.
     */
    abstract ChannelInputStream createInputStream(SimpleSocket socket) throws IOException;

    /**
     * Called by constructor.
     */
    abstract ChannelOutputStream createOutputStream(SimpleSocket socket) throws IOException;
}
