/*
 *  Copyright 2006 Brian S O'Neill
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

package dirmi.core2;

/**
 * If an exception is thrown from a skeleton-invoked method which is
 * asynchronous, it cannot write the exception to the channel. Instead it
 * wraps the exception in an AsynchronousInvocationException for the server to
 * log.
 *
 * @author Brian S O'Neill
 */
public class AsynchronousInvocationException extends Exception {
    private static final long serialVersionUID = 1;

    public AsynchronousInvocationException(Throwable cause) {
        super(cause);
    }
}