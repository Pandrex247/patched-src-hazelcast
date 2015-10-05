/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.cache.impl.operation;

import com.hazelcast.cache.CacheEntryView;
import com.hazelcast.cache.impl.CacheDataSerializerHook;
import com.hazelcast.cache.impl.CacheEntryViews;
import com.hazelcast.cache.impl.ICacheRecordStore;
import com.hazelcast.cache.impl.ICacheService;
import com.hazelcast.cache.impl.record.CacheRecord;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.BackupOperation;
import com.hazelcast.spi.impl.MutatingOperation;

import java.io.IOException;

/**
 * Backup operation for the operation of adding cache entries into record stores.
 * @see CacheEntryProcessorOperation
 * @see CachePutOperation
 * @see CachePutIfAbsentOperation
 * @see CacheReplaceOperation
 * @see CacheGetAndReplaceOperation
 */
public class CachePutBackupOperation
        extends AbstractCacheOperation
        implements BackupOperation, MutatingOperation {

    private CacheRecord cacheRecord;
    private boolean wanOriginated;

    public CachePutBackupOperation() {
    }

    public CachePutBackupOperation(String name, Data key, CacheRecord cacheRecord) {
        super(name, key);
        this.cacheRecord = cacheRecord;
    }

    public CachePutBackupOperation(String name, Data key, CacheRecord cacheRecord, boolean wanOriginated) {
        this(name, key, cacheRecord);
        this.wanOriginated = wanOriginated;
    }

    @Override
    public void run()
            throws Exception {
        ICacheService service = getService();
        ICacheRecordStore cache = service.getOrCreateRecordStore(name, getPartitionId());
        cache.putRecord(key, cacheRecord);
        response = Boolean.TRUE;
    }

    @Override
    public void afterRun() throws Exception {
        if (!wanOriginated && cache.isWanReplicationEnabled()) {
            CacheEntryView<Data, Data> entryView = CacheEntryViews.createDefaultEntryView(key,
                    getNodeEngine().getSerializationService().toData(cacheRecord.getValue()), cacheRecord);
            wanEventPublisher.publishWanReplicationUpdateBackup(name, entryView);
        }
    }

    @Override
    protected void writeInternal(ObjectDataOutput out)
            throws IOException {
        super.writeInternal(out);
        out.writeObject(cacheRecord);
        out.writeBoolean(wanOriginated);
    }

    @Override
    protected void readInternal(ObjectDataInput in)
            throws IOException {
        super.readInternal(in);
        cacheRecord = in.readObject();
        wanOriginated = in.readBoolean();
    }

    @Override
    public int getId() {
        return CacheDataSerializerHook.PUT_BACKUP;
    }

}
