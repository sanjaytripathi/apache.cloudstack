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
package org.apache.cloudstack.api.response;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class GpuResponse extends BaseResponse {

    @SerializedName(ApiConstants.GPUGROUPNAME)
    @Param(description = "GPU cards present in the host")
    private String gpuGroupName;

    @SerializedName(ApiConstants.VGPU)
    @Param(description = "the list of enabled vGPUs", responseObject = VgpuResponse.class)
    private List<VgpuResponse> vgpu;

    @SerializedName("capacityused")
    @Param(description = "the capacity currently in use")
    private String capacityUsed;

    @SerializedName("capacitytotal")
    @Param(description = "the total capacity available")
    private Long capacityTotal;

    @SerializedName("percentused")
    @Param(description = "the percentage of capacity currently in use")
    private String percentUsed;

    public void setGpuGroupName(String gpuGroupName) {
        this.gpuGroupName = gpuGroupName;
    }

    public void setVgpu(List<VgpuResponse> vgpu) {
        this.vgpu = vgpu;
    }

    public void setCapacityUsed(String capacityUsed) {
        this.capacityUsed = capacityUsed;
    }

    public void setCapacityTotal(Long capacityTotal) {
        this.capacityTotal = capacityTotal;
    }

    public void setPercentUsed(String percentUsed) {
        this.percentUsed = percentUsed;
    }
}
