/*
 * Copyright: (c) 2008-2010 Gemini Mobile Technologies, Inc.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hibari.plugins;
 
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;

import com.hibari.thrift.Add;
import com.hibari.thrift.Delete;
import com.hibari.thrift.ErrorCode;
import com.hibari.thrift.Get;
import com.hibari.thrift.Hibari;
import com.hibari.thrift.HibariException;
import com.hibari.thrift.HibariResponse;
import com.hibari.thrift.Replace;

public class HibariClient
{
    private ArrayList<String> servers;
    private final static int PORT = 7600;
    private Hibari.Client client;
    private TTransport transport;

    public HibariClient(final String[] hostnames)
    {
        servers = new ArrayList<String>(hostnames.length);
        for (String hostname : hostnames)
            servers.add(hostname);
    }

    public void open() throws IOException
    {
        try {
            transport = new TSocket(nextAvailableServer(), PORT);
            TProtocol proto = new TBinaryProtocol(transport);
            client = new Hibari.Client(proto);
            transport.open();

        } catch (HibariException he) {
            throw new IOException(he);

        } catch (TTransportException tte) {
            throw new IOException(tte);
        }
    }

    public void close() {
        transport.close();
    }

    public void insert(String table, byte[] key, byte[] value)
    throws HibariException
    {
        try {
            client.Add(new Add(table, ByteBuffer.wrap(key),
                ByteBuffer.wrap(value)));
        } catch (TException e) {
            throw new HibariException(ErrorCode.SERVICE_NOT_AVAIL.getValue(),
                e.getMessage());
        }
    }

    public void insert(String table, String key, String value)
    throws HibariException
    {
        insert(table, key.getBytes(), value.getBytes());
    }

    // @todo(gabeki): is manual load balance / keepalive needed?
    private String nextAvailableServer() throws HibariException
    {
        if (servers.isEmpty()) {
            throw new HibariException(ErrorCode.SERVICE_NOT_AVAIL.getValue(),
                "No Server is available");
        } else {
            return servers.get(0);
        }
    }
}
