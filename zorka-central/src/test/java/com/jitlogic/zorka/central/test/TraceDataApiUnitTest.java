/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.central.test;


import com.jitlogic.zorka.central.HostStore;
import com.jitlogic.zorka.central.ReceiverContext;
import com.jitlogic.zorka.central.data.HostInfo;
import com.jitlogic.zorka.central.data.TraceRecordInfo;
import com.jitlogic.zorka.central.rest.TraceDataApi;
import com.jitlogic.zorka.central.test.support.CentralFixture;
import com.jitlogic.zorka.common.test.support.TestTraceGenerator;
import com.jitlogic.zorka.common.tracedata.Symbol;
import com.jitlogic.zorka.common.tracedata.TraceRecord;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.Assert.*;

public class TraceDataApiUnitTest extends CentralFixture {

    private JdbcTemplate jdbc;

    @Before
    public void prepareData() throws Exception {
        jdbc = new JdbcTemplate(instance.getDs());

        ReceiverContext rcx = new ReceiverContext(instance.getDs(), instance.getStoreManager().get("test"));
        TestTraceGenerator generator = new TestTraceGenerator();
        TraceRecord tr = generator.generate();
        Symbol s1 = new Symbol(tr.getClassId(), generator.getSymbols().symbolName(tr.getClassId()));
        rcx.process(s1);
        rcx.process(new Symbol(tr.getMethodId(), generator.getSymbols().symbolName(tr.getMethodId())));
        rcx.process(new Symbol(tr.getSignatureId(), generator.getSymbols().symbolName(tr.getSignatureId())));
        rcx.process(tr);

    }

    private HostInfo mkHost(int id, String name, String addr, String desc, int flags) {
        HostInfo info = new HostInfo();
        info.setId(id);
        info.setName(name);
        info.setAddr(addr);
        info.setDescription(desc);
        info.setFlags(flags);
        return info;
    }


    @Test
    public void testCreateHost() throws Exception {
        HostInfo myinfo = mkHost(0, "myhost", "127.0.0.1", "My Description", 0x20);
        traceDataApi.addHost(myinfo);

        assertEquals(1, (int) jdbc.queryForObject("select count(1) from HOSTS where HOST_NAME = ?", Integer.class, "myhost"));

        int hostId = jdbc.queryForObject("select HOST_ID from HOSTS where HOST_NAME = ?", Integer.class, "myhost");
        HostStore host = storeManager.getHost(hostId);
        assertNotNull(host);
        assertEquals("myhost", host.getHostInfo().getName());
        assertEquals("127.0.0.1", host.getHostInfo().getAddr());

        String hostAddr = jdbc.queryForObject("select HOST_ADDR from HOSTS where HOST_NAME = ?", String.class, "myhost");
        assertEquals("127.0.0.1", hostAddr);
    }


    @Test
    public void testCreateAndUpdateHost() throws Exception {
        HostInfo myinfo = mkHost(0, "myhost", "127.0.0.1", "My Description", 0x20);
        traceDataApi.addHost(myinfo);

        int hostId = jdbc.queryForObject("select HOST_ID from HOSTS where HOST_NAME = ?", Integer.class, "myhost");

        HostInfo newInfo = mkHost(0, "myhost", "1.2.3.4", "Other Description", 0x40);
        traceDataApi.updateHost(hostId, newInfo);

        String hostAddr = jdbc.queryForObject("select HOST_ADDR from HOSTS where HOST_NAME = ?", String.class, "myhost");
        assertEquals("1.2.3.4", hostAddr);
    }


    @Test
    public void testCreateAndDeleteHost() throws Exception {
        HostInfo myinfo = mkHost(0, "myhost", "127.0.0.1", "My Description", 0x20);
        traceDataApi.addHost(myinfo);

        int hostId = jdbc.queryForObject("select HOST_ID from HOSTS where HOST_NAME = ?", Integer.class, "myhost");

        traceDataApi.deleteHost(hostId);

        int hostCnt = jdbc.queryForObject("select count(1) from HOSTS where HOST_ID = ?", Integer.class, hostId);
        assertEquals(0, hostCnt);
    }


    // TODO update with improper host name (should throw exception)


    @Test
    public void testGetTraceRoot() throws Exception {
        int hostId = storeManager.getOrCreateHost("test", "").getHostInfo().getId();
        TraceRecordInfo tr = traceDataApi.getRecord(hostId, 0, "");
        assertEquals(0, tr.getChildren());
    }


    @Test
    public void testListTraceRoot() throws Exception {
        int hostId = storeManager.getOrCreateHost("test", "").getHostInfo().getId();
        List<TraceRecordInfo> lst = traceDataApi.listRecords(hostId, 0, "");
        assertNotNull(lst);
    }
}
