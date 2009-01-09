/*
 *  Copyright 2009 Brian S O'Neill
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

import java.io.Closeable;
import java.io.IOException;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public interface Broker<C extends Closeable> extends Acceptor<C>, Connector<C>, Closeable {
    /**
     * @return local address or null if unknown or not applicable
     */
    Object getLocalAddress();

    /**
     * @return remote address or null if unknown or not applicable
     */
    Object getRemoteAddress();

    /**
     * Prevents new channels from being created and disconnects all existing channels.
     */
    void close() throws IOException;
}
