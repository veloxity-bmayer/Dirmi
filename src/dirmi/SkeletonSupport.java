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

package dirmi;

import java.io.IOException;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;

import dirmi.io.Connection;
import dirmi.io.RemoteInput;
import dirmi.io.RemoteOutput;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public interface SkeletonSupport {
    Remote getObject(int objectID) throws NoSuchObjectException;

    int getObjectID(Remote object) throws NoSuchObjectException;

    RemoteInput createRemoteInput(Connection con) throws IOException;

    RemoteOutput createRemoteOutput(Connection con) throws IOException;
}