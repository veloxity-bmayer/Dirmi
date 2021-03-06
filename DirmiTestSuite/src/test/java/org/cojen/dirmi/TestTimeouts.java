/*
 *  Copyright 2009-2010 Brian S O'Neill
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

package org.cojen.dirmi;

import java.rmi.RemoteException;

import java.util.concurrent.TimeUnit;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public class TestTimeouts extends AbstractTestSuite {
    public static void main(String[] args) {
        org.junit.runner.JUnitCore.main(TestTimeouts.class.getName());
    }

    protected SessionStrategy createSessionStrategy(Environment env) throws Exception {
        return new PipedSessionStrategy(env, null, new RemoteTimeoutsServer());
    }

    @Test
    public void slow1() throws Exception {
        RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;

        remote.slow(1);

        try {
            remote.slow(2000);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1000 milliseconds", e.getMessage());
        }

        remote.slow(1);
    }

    @Test
    public void slow2() throws Exception {
        RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;

        assertEquals("foo", remote.slow(1, "foo"));

        try {
            remote.slow(3000, "foo");
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 2 seconds", e.getMessage());
        }
    }

    @Test
    public void slow3() throws Exception {
        RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;

        assertEquals(1, remote.slow(1, (byte) 1));

        try {
            remote.slow(3000, (byte) 1);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1 second", e.getMessage());
        }
    }

    @Test
    public void slow4() throws Exception {
        RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;

        assertEquals(new Byte((byte) 1), remote.slow(1, new Byte((byte) 1)));

        try {
            remote.slow(3000, new Byte((byte) 2));
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 2 seconds", e.getMessage());
        }

        assertEquals(new Byte((byte) -1), remote.slow(100, (Byte) null));
    }

    @Test
    public void slow5() throws Exception {
        RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;

        assertEquals(30000, remote.slow(1, (short) 30000));

        try {
            remote.slow(3000, (short) 1);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1 second", e.getMessage());
        }
    }

    @Test
    public void slow6() throws Exception {
        RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;

        assertEquals(new Short((short) -100), remote.slow(1, new Short((short) -100)));

        try {
            remote.slow(3000, new Short((short) 1));
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1 second", e.getMessage());
        }

        assertEquals(new Short((short) 2), remote.slow(100, (Short) null));

        try {
            remote.slow(3000, (Short) null);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 2 seconds", e.getMessage());
        }
    }

    @Test
    public void slow7() throws Exception {
        RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;

        try {
            remote.slow(1, 0);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 0 milliseconds", e.getMessage());
        }

        assertEquals(2000000000, remote.slow(1, 2000000000));

        try {
            remote.slow(3000, 100);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 100 milliseconds", e.getMessage());
        }

        // Verify that abnormal errors don't cause timeout exception.

        env.executor().execute(new Runnable() {
            public void run() {
                try {
                    sleep(1000);
                    sessionStrategy.remoteSession.close();
                } catch (Exception e) {
                }
            }
        });

        try {
            remote.slow(3000, 2000);
            fail();
        } catch (RemoteTimeoutException e) {
            fail();
        } catch (RemoteException e) {
        }
    }

    @Test
    public void slow8() throws Exception {
        RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;

        assertEquals(new Integer(-100), remote.slow(1, new Integer(-100)));

        try {
            remote.slow(3000, new Integer(1));
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1 millisecond", e.getMessage());
        }

        assertEquals(new Integer(1500), remote.slow(100, (Integer) null));

        try {
            remote.slow(3000, (Integer) null);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1500 milliseconds", e.getMessage());
        }
    }

    @Test
    public void slow9() throws Exception {
        RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;

        try {
            remote.slow(1, (long) 0);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 0 milliseconds", e.getMessage());
        }

        assertEquals(Long.MAX_VALUE, remote.slow(1, Long.MAX_VALUE));

        try {
            remote.slow(3000, (long) 100);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 100 milliseconds", e.getMessage());
        }
    }

    @Test
    public void slow10() throws Exception {
        RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;

        assertEquals(new Long(-100), remote.slow(1, new Long(-100)));

        try {
            remote.slow(3000, new Long(2));
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 2 milliseconds", e.getMessage());
        }

        assertEquals(new Long(-1), remote.slow(100, (Long) null));
    }

    @Test
    public void slow11() throws Exception {
        RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;

        try {
            remote.slow(1, (double) 0);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 0.0 milliseconds", e.getMessage());
        }

        try {
            remote.slow(1000, (double) 1.0);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1.0 millisecond", e.getMessage());
        }

        try {
            remote.slow(1000, (double) 1.25);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1.25 milliseconds", e.getMessage());
        }

        try {
            remote.slow(1, Double.NaN);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after NaN milliseconds", e.getMessage());
        }

        assertEquals(Double.POSITIVE_INFINITY, remote.slow(1, Double.POSITIVE_INFINITY), 0);
    }

    @Test
    public void slow12() throws Exception {
        RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;

        assertEquals(new Double(-100), remote.slow(1, new Double(-100)));

        try {
            remote.slow(3000, new Double(1.0));
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1.0 millisecond", e.getMessage());
        }

        assertEquals(new Double(1500), remote.slow(100, (Double) null));

        try {
            remote.slow(3000, (Double) null);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1500.0 milliseconds", e.getMessage());
        }
    }

    @Test
    public void slow13() throws Exception {
        RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;

        try {
            remote.slow(1, (float) 0);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 0.0 milliseconds", e.getMessage());
        }

        try {
            remote.slow(1000, (float) 1.0);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1.0 millisecond", e.getMessage());
        }

        try {
            remote.slow(1000, (float) 1.25);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1.25 milliseconds", e.getMessage());
        }

        try {
            remote.slow(1, Float.NaN);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after NaN milliseconds", e.getMessage());
        }

        assertEquals(Float.POSITIVE_INFINITY, remote.slow(1, Float.POSITIVE_INFINITY), 0);
    }

    @Test
    public void slow14() throws Exception {
        RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;

        assertEquals(new Float(-100), remote.slow(1, new Float(-100)));

        try {
            remote.slow(3000, new Float(1.0));
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1.0 millisecond", e.getMessage());
        }

        assertEquals(new Float(-1), remote.slow(100, (Float) null));
    }

    @Test
    public void slow15() throws Exception {
        RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;

        assertEquals(new Float(-100), remote.slow(new Float(-100), 1));

        try {
            remote.slow(new Float(1.0), 3000);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1.0 millisecond", e.getMessage());
        }

        assertEquals(new Float(-1), remote.slow((Float) null, 100));
    }

    @Test
    public void unitParam() throws Exception {
        RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;

        try {
            remote.slow(60000, 1, TimeUnit.SECONDS);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1 second", e.getMessage());
        }

        TimeUnit unit = remote.slow(1, 1000, null);
        assertEquals(TimeUnit.MILLISECONDS, unit);

        try {
            remote.slow(60000, 60, null);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 60 milliseconds", e.getMessage());
        }

        try {
            remote.slow(5.0, TimeUnit.NANOSECONDS, 6000);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 5.0 nanoseconds", e.getMessage());
        }

        try {
            remote.slow(5.0, TimeUnit.MICROSECONDS, 6000);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 5.0 microseconds", e.getMessage());
        }

        try {
            remote.slow(1.0, TimeUnit.MILLISECONDS, 6000);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1.0 millisecond", e.getMessage());
        }

        try {
            remote.slow(0.12, TimeUnit.SECONDS, 6000);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 0.12 seconds", e.getMessage());
        }

        try {
            remote.slow(0.03125, TimeUnit.MINUTES, 60000);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 0.03125 minutes", e.getMessage());
        }

        try {
            remote.slow(0.0001, TimeUnit.HOURS, 60000);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1.0E-4 hours", e.getMessage());
        }

        try {
            remote.slow(4.2e-6, TimeUnit.DAYS, 60000);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 4.2E-6 days", e.getMessage());
        }

        unit = remote.slow(1000.0, null, 1);
        assertEquals(TimeUnit.MILLISECONDS, unit);

        try {
            remote.slow(60.0, null, 60000);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 60.0 milliseconds", e.getMessage());
        }

        unit = remote.slow(1L, null, 1);
        assertEquals(TimeUnit.MINUTES, unit);
    }

    @Test
    public void interfaceDefaultTimeout() throws Exception {
        RemoteTimeouts2 remote2;
        {
            RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;
            remote2 = remote.createRemoteTimeouts2();
        }

        assertEquals(new Long(-100), remote2.slow(1, new Long(-100)));

        try {
            remote2.slow(3000, new Long(2));
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 2 milliseconds", e.getMessage());
        }

        assertEquals(new Long(2000), remote2.slow(100, (Long) null));

        try {
            remote2.slow(3000, (Long) null);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 2000 milliseconds", e.getMessage());
        }
    }

    @Test
    public void interfaceDefaultTimeoutAndUnit() throws Exception {
        RemoteTimeouts3 remote3;
        {
            RemoteTimeouts remote = (RemoteTimeouts) sessionStrategy.remoteServer;
            remote3 = remote.createRemoteTimeouts3();
        }

        try {
            remote3.slow(3000);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1 second", e.getMessage());
        }

        try {
            remote3.slow2(4000);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 2 seconds", e.getMessage());
        }

        try {
            remote3.slow3(4000);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1500 milliseconds", e.getMessage());
        }

        try {
            remote3.slow4(1000);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1 millisecond", e.getMessage());
        }

        try {
            remote3.slow5(4000, 2);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 2 seconds", e.getMessage());
        }

        try {
            remote3.slow6(1000, TimeUnit.MICROSECONDS);
            fail();
        } catch (RemoteTimeoutException e) {
            assertEquals("Timed out after 1 microsecond", e.getMessage());
        }
    }
}
