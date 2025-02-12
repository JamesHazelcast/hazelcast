/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.client.impl.protocol.codec;

import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.Generated;
import com.hazelcast.client.impl.protocol.codec.builtin.*;
import com.hazelcast.client.impl.protocol.codec.custom.*;

import javax.annotation.Nullable;

import static com.hazelcast.client.impl.protocol.ClientMessage.*;
import static com.hazelcast.client.impl.protocol.codec.builtin.FixedSizeTypesCodec.*;

/*
 * This file is auto-generated by the Hazelcast Client Protocol Code Generator.
 * To change this file, edit the templates or the protocol
 * definitions on the https://github.com/hazelcast/hazelcast-client-protocol
 * and regenerate it.
 */

/**
 * Adds a new diagnostics configuration to a running cluster.
 */
@SuppressWarnings("unused")
@Generated("59d7661e129d2f71207c7fd34fcb1c30")
public final class DynamicConfigSetDiagnosticsConfigCodec {
    //hex: 0x1B1500
    public static final int REQUEST_MESSAGE_TYPE = 1774848;
    //hex: 0x1B1501
    public static final int RESPONSE_MESSAGE_TYPE = 1774849;
    private static final int REQUEST_ENABLED_FIELD_OFFSET = PARTITION_ID_FIELD_OFFSET + INT_SIZE_IN_BYTES;
    private static final int REQUEST_INCLUDE_EPOCH_TIME_FIELD_OFFSET = REQUEST_ENABLED_FIELD_OFFSET + BOOLEAN_SIZE_IN_BYTES;
    private static final int REQUEST_MAX_ROLLED_FILE_SIZE_IN_MB_FIELD_OFFSET = REQUEST_INCLUDE_EPOCH_TIME_FIELD_OFFSET + BOOLEAN_SIZE_IN_BYTES;
    private static final int REQUEST_MAX_ROLLED_FILE_COUNT_FIELD_OFFSET = REQUEST_MAX_ROLLED_FILE_SIZE_IN_MB_FIELD_OFFSET + INT_SIZE_IN_BYTES;
    private static final int REQUEST_INITIAL_FRAME_SIZE = REQUEST_MAX_ROLLED_FILE_COUNT_FIELD_OFFSET + INT_SIZE_IN_BYTES;
    private static final int RESPONSE_INITIAL_FRAME_SIZE = RESPONSE_BACKUP_ACKS_FIELD_OFFSET + BYTE_SIZE_IN_BYTES;

    private DynamicConfigSetDiagnosticsConfigCodec() {
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
    public static class RequestParameters {

        /**
         * determines whether the performance monitor is enabled
         */
        public boolean enabled;

        /**
         * the output type for the diagnostics
         */
        public java.lang.String outputType;

        /**
         * indicates if the epoch time should be included in the 'top' section
         */
        public boolean includeEpochTime;

        /**
         * the maximum size in MB for a single file
         */
        public int maxRolledFileSizeInMB;

        /**
         * the maximum number of rolling files to keep on disk
         */
        public int maxRolledFileCount;

        /**
         * the output directory of the performance log files
         */
        public java.lang.String logDirectory;

        /**
         * the prefix for the diagnostics file
         */
        public @Nullable java.lang.String fileNamePrefix;

        /**
         * Properties of the plugin
         */
        public @Nullable java.util.Map<java.lang.String, java.lang.String> properties;
    }

    public static ClientMessage encodeRequest(boolean enabled, java.lang.String outputType, boolean includeEpochTime, int maxRolledFileSizeInMB, int maxRolledFileCount, java.lang.String logDirectory, @Nullable java.lang.String fileNamePrefix, @Nullable java.util.Map<java.lang.String, java.lang.String> properties) {
        ClientMessage clientMessage = ClientMessage.createForEncode();
        clientMessage.setRetryable(false);
        clientMessage.setOperationName("DynamicConfig.SetDiagnosticsConfig");
        ClientMessage.Frame initialFrame = new ClientMessage.Frame(new byte[REQUEST_INITIAL_FRAME_SIZE], UNFRAGMENTED_MESSAGE);
        encodeInt(initialFrame.content, TYPE_FIELD_OFFSET, REQUEST_MESSAGE_TYPE);
        encodeInt(initialFrame.content, PARTITION_ID_FIELD_OFFSET, -1);
        encodeBoolean(initialFrame.content, REQUEST_ENABLED_FIELD_OFFSET, enabled);
        encodeBoolean(initialFrame.content, REQUEST_INCLUDE_EPOCH_TIME_FIELD_OFFSET, includeEpochTime);
        encodeInt(initialFrame.content, REQUEST_MAX_ROLLED_FILE_SIZE_IN_MB_FIELD_OFFSET, maxRolledFileSizeInMB);
        encodeInt(initialFrame.content, REQUEST_MAX_ROLLED_FILE_COUNT_FIELD_OFFSET, maxRolledFileCount);
        clientMessage.add(initialFrame);
        StringCodec.encode(clientMessage, outputType);
        StringCodec.encode(clientMessage, logDirectory);
        CodecUtil.encodeNullable(clientMessage, fileNamePrefix, StringCodec::encode);
        MapCodec.encodeNullable(clientMessage, properties, StringCodec::encode, StringCodec::encode);
        return clientMessage;
    }

    public static DynamicConfigSetDiagnosticsConfigCodec.RequestParameters decodeRequest(ClientMessage clientMessage) {
        ClientMessage.ForwardFrameIterator iterator = clientMessage.frameIterator();
        RequestParameters request = new RequestParameters();
        ClientMessage.Frame initialFrame = iterator.next();
        request.enabled = decodeBoolean(initialFrame.content, REQUEST_ENABLED_FIELD_OFFSET);
        request.includeEpochTime = decodeBoolean(initialFrame.content, REQUEST_INCLUDE_EPOCH_TIME_FIELD_OFFSET);
        request.maxRolledFileSizeInMB = decodeInt(initialFrame.content, REQUEST_MAX_ROLLED_FILE_SIZE_IN_MB_FIELD_OFFSET);
        request.maxRolledFileCount = decodeInt(initialFrame.content, REQUEST_MAX_ROLLED_FILE_COUNT_FIELD_OFFSET);
        request.outputType = StringCodec.decode(iterator);
        request.logDirectory = StringCodec.decode(iterator);
        request.fileNamePrefix = CodecUtil.decodeNullable(iterator, StringCodec::decode);
        request.properties = MapCodec.decodeNullable(iterator, StringCodec::decode, StringCodec::decode);
        return request;
    }

    public static ClientMessage encodeResponse() {
        ClientMessage clientMessage = ClientMessage.createForEncode();
        ClientMessage.Frame initialFrame = new ClientMessage.Frame(new byte[RESPONSE_INITIAL_FRAME_SIZE], UNFRAGMENTED_MESSAGE);
        encodeInt(initialFrame.content, TYPE_FIELD_OFFSET, RESPONSE_MESSAGE_TYPE);
        clientMessage.add(initialFrame);

        return clientMessage;
    }
}
