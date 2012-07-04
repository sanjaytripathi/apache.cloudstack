// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.storage.dao;


import java.util.Date;
import java.util.List;

import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.utils.db.GenericDao;

/*
 * Data Access Object for snapshot_schedule table
 */
public interface SnapshotScheduleDao extends GenericDao<SnapshotScheduleVO, Long> {

	List<SnapshotScheduleVO> getCoincidingSnapshotSchedules(long volumeId, Date date);

    List<SnapshotScheduleVO> getSchedulesToExecute(Date currentTimestamp);

    SnapshotScheduleVO getCurrentSchedule(Long volumeId, Long policyId, boolean executing);

    SnapshotScheduleVO findOneByVolume(long volumeId);

    SnapshotScheduleVO findOneByVolumePolicy(long volumeId, long policyId);

}
