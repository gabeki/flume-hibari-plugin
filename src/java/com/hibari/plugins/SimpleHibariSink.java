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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import com.cloudera.flume.conf.Context;
import com.cloudera.flume.conf.SinkFactory.SinkBuilder;
import com.cloudera.flume.core.Event;
import com.cloudera.flume.core.EventSink;
import com.cloudera.util.Pair;
import org.safehaus.uuid.UUID;
import org.safehaus.uuid.UUIDGenerator;

import com.hibari.thrift.HibariException;

public class SimpleHibariSink extends EventSink.Base {

    private static final UUIDGenerator UUID_GEN = UUIDGenerator.getInstance();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMddHH");
    private static final Calendar CAL = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));

    private HibariClient client;
    private String table;

    public SimpleHibariSink(String table, String[] servers)
    {
        this.table = table;
        client = new HibariClient(servers);
    }

    @Override
    public void open() throws IOException
    {
        client.open();
    }

    @Override
    public void close() throws IOException
    {
        client.close();
    }

    @Override
    public void append(Event event) throws IOException
    {
        if (event.getBody() == null || event.getBody().length == 0)
            return;

        UUID uuid = UUID_GEN.generateTimeBasedUUID();
        String key = SDF.format(CAL.getTime()) + "/" + uuid.toString();
        
        try {
            client.insert(table, key.getBytes(), event.getBody());
        } catch (HibariException he) {
            throw new IOException(he.getWhy());
        }
        super.append(event);
    }

    public static SinkBuilder builder()
    {
        return new SinkBuilder() {
            @Override
            public EventSink build(Context context, String ... args) {
                if (args.length < 2) {
                    throw new IllegalArgumentException(
                        "usage: simpleHibariSink(\"table\", \"host\"...");
                }

                String[] servers = Arrays.copyOfRange(args, 1, args.length);
                return new SimpleHibariSink(args[0], servers);
            }
        };
    }

    public static List<Pair<String, SinkBuilder>> getSinkBuilders()
    {
        List<Pair<String, SinkBuilder>> builders =
            new ArrayList<Pair<String, SinkBuilder>>();
        builders.add(new Pair<String, SinkBuilder>("simpleHibariSink", builder()));
        return builders;
    }
}
