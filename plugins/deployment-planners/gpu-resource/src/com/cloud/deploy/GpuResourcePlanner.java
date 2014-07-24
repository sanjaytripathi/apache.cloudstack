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
package com.cloud.deploy;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.gpu.GPU;
import com.cloud.gpu.dao.HostGpuGroupsDao;
import com.cloud.host.Host;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = DeploymentPlanner.class)
public class GpuResourcePlanner extends FirstFitPlanner implements DeploymentClusterPlanner {

    private static final Logger s_logger = Logger.getLogger(GpuResourcePlanner.class);

    @Inject
    private ServiceOfferingDetailsDao _serviceOfferingDetailsDao;
    @Inject
    private HostGpuGroupsDao _hostGpuGroupsDao;

    @Override
    public List<Long> orderClusters(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid) throws InsufficientServerCapacityException {
        List<Long> clusterList = super.orderClusters(vmProfile, plan, avoid);
        if (clusterList == null || clusterList.isEmpty()) {
            return clusterList;
        }

        ServiceOffering offering = vmProfile.getServiceOffering();
        // In case of non-GPU VMs, protect GPU enabled Hosts and prefer VM deployment on non-GPU Hosts.
        if ((_serviceOfferingDetailsDao.findDetail(offering.getId(), GPU.Keys.vgpuType.toString()) == null) && !(_hostGpuGroupsDao.listHostIds().isEmpty())) {
            s_logger.info("Reordering cluster list to protect GPU resources from non-gpu VM deployment");
            int requiredCpu = offering.getCpu() * offering.getSpeed();
            long requiredRam = offering.getRamSize() * 1024L * 1024L;
            reorderGpuEnabledClusters(clusterList, requiredCpu, requiredRam);
        }
        return clusterList;
    }

    private void reorderGpuEnabledClusters(List<Long> clusterList, int requiredCpu, long requiredRam) {
        List<Long> preferredClusterList = new ArrayList<Long>();
        List<Long> gpuHostList = _hostGpuGroupsDao.listHostIds();
        for (Long clusterId : clusterList) {
            List<Long> hostList = _capacityDao.listHostsWithEnoughCapacity(requiredCpu, requiredRam, clusterId, Host.Type.Routing.toString());
            hostList.removeAll(gpuHostList);
            if (!hostList.isEmpty()) {
                preferredClusterList.add(clusterId);
            }
        }
        // Move all preferred clusters to the beginning of cluster list
        clusterList.removeAll(preferredClusterList);
        clusterList.addAll(0, preferredClusterList);
    }
}