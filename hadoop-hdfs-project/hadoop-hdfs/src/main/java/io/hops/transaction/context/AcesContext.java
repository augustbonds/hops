/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.transaction.context;

import io.hops.exception.StorageCallPreventedException;
import io.hops.exception.StorageException;
import io.hops.exception.TransactionContextException;
import io.hops.metadata.common.FinderType;
import io.hops.metadata.hdfs.dal.AceDataAccess;
import io.hops.metadata.hdfs.entity.Ace;
import io.hops.transaction.lock.TransactionLocks;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AcesContext
    extends BaseEntityContext<Ace.PrimaryKey, Ace> {
  
  private AceDataAccess dataAccess;
  Map<Integer, List<Ace>> inodeAces = new HashMap<>();
  
  public AcesContext(AceDataAccess aceDataAccess) {
    this.dataAccess = aceDataAccess;
  }
  
  @Override
  public Ace find(FinderType<Ace> finder, Object... params)
      throws TransactionContextException, StorageException {
    Ace.Finder aceFinder = (Ace.Finder) finder;
    switch (aceFinder){
      case InodeIdAndId:
        return findByPrimaryKey(aceFinder, params);
      default:
        throw new RuntimeException(UNSUPPORTED_FINDER);
    }
  }
  
  @Override
  public Collection<Ace> findList(FinderType<Ace> finder, Object... params)
      throws TransactionContextException, StorageException {
    Ace.Finder aceFinder = (Ace.Finder) finder;
    switch (aceFinder){
      case ByInodeId:
        return findByInodeId(aceFinder, params);
      default:
        throw new RuntimeException(UNSUPPORTED_FINDER);
    }
  }
  
  private Collection<Ace> findByInodeId(Ace.Finder aceFinder, Object[] params)
      throws StorageCallPreventedException, StorageException {
    final Integer inodeId = (Integer) params[0];
    List<Ace> result;
    if (inodeAces.containsKey(inodeId)) {
      result = inodeAces.get(inodeId);
      hit(aceFinder, result, "inodeId", inodeId);
    } else {
      aboutToAccessStorage(aceFinder, params);
      result = dataAccess.getAcesByInodeId(inodeId);
      inodeAces.put(inodeId, result);
      miss(aceFinder, result, "inodeId", inodeId);
    }
    return result;
  }
  
  private Ace findByPrimaryKey(Ace.Finder aceFinder, Object[] params)
      throws StorageException, StorageCallPreventedException {
    int inodeId = (Integer) params[0];
    int id = (Integer) params[1];
    Ace.PrimaryKey pk = new Ace.PrimaryKey(inodeId, id);
    Ace result;
    if (contains(pk)){
      result = get(pk);
      hit(aceFinder, result, "inodeId", inodeId, "id", id);
    } else {
      aboutToAccessStorage(aceFinder, params);
      result = (Ace) dataAccess.getAceByPK(inodeId, id);
      gotFromDB(pk, result);
      miss(aceFinder, result, "inodeId", inodeId, "id", id);
    }
    
    return result;
  }
  
  @Override
  public void prepare(TransactionLocks tlm)
      throws TransactionContextException, StorageException {
    dataAccess.prepare(getRemoved(),getModified());
  }
  
  @Override
  Ace.PrimaryKey getKey(Ace ace) {
    return new Ace.PrimaryKey(ace.getId(), ace.getInodeId());
  }
}