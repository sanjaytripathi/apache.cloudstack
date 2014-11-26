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

package com.cloud.upgrade.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloud.hypervisor.Hypervisor;
import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade441to450 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade441to450.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.4.1", "4.5.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.5.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-441to450.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-441to450.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateSystemVmTemplates(conn);
        dropInvalidKeyFromStoragePoolTable(conn);
        dropDuplicatedForeignKeyFromAsyncJobTable(conn);
        upgradeVMwareLocalStorage(conn);
        upgradeMemoryOfVirtualRoutervmOffering(conn);
        upgradeMemoryOfInternalLoadBalancervmOffering(conn);
    }

    private void upgradeMemoryOfVirtualRoutervmOffering(Connection conn) {
        PreparedStatement updatePstmt = null;
        PreparedStatement selectPstmt = null;
        ResultSet selectResultSet = null;
        int newRamSize = 256; //256MB
        long serviceOfferingId = 0;

        /**
         * Pick first row in service_offering table which has system vm type as domainrouter. User added offerings would start from 2nd row onwards.
         * We should not update/modify any user-defined offering.
         */

        try {
            selectPstmt = conn.prepareStatement("SELECT id FROM `cloud`.`service_offering` WHERE vm_type='domainrouter'");
            updatePstmt = conn.prepareStatement("UPDATE `cloud`.`service_offering` SET ram_size=? WHERE id=?");
            selectResultSet = selectPstmt.executeQuery();
            if(selectResultSet.next()) {
                serviceOfferingId = selectResultSet.getLong("id");
            }

            updatePstmt.setInt(1, newRamSize);
            updatePstmt.setLong(2, serviceOfferingId);
            updatePstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to upgrade ram_size of service offering for domain router. ", e);
        } finally {
            try {
                if (selectPstmt != null) {
                    selectPstmt.close();
                }
                if (selectResultSet != null) {
                    selectResultSet.close();
                }
                if (updatePstmt != null) {
                    updatePstmt.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Done upgrading RAM for service offering of domain router to " + newRamSize);
    }

    private void upgradeMemoryOfInternalLoadBalancervmOffering(Connection conn) {
        PreparedStatement updatePstmt = null;
        PreparedStatement selectPstmt = null;
        ResultSet selectResultSet = null;
        int newRamSize = 256; //256MB
        long serviceOfferingId = 0;

        /**
         * Pick first row in service_offering table which has system vm type as internalloadbalancervm. User added offerings would start from 2nd row onwards.
         * We should not update/modify any user-defined offering.
         */

        try {
            selectPstmt = conn.prepareStatement("SELECT id FROM `cloud`.`service_offering` WHERE vm_type='internalloadbalancervm'");
            updatePstmt = conn.prepareStatement("UPDATE `cloud`.`service_offering` SET ram_size=? WHERE id=?");
            selectResultSet = selectPstmt.executeQuery();
            if(selectResultSet.next()) {
                serviceOfferingId = selectResultSet.getLong("id");
            }

            updatePstmt.setInt(1, newRamSize);
            updatePstmt.setLong(2, serviceOfferingId);
            updatePstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to upgrade ram_size of service offering for internal loadbalancer vm. ", e);
        } finally {
            try {
                if (selectPstmt != null) {
                    selectPstmt.close();
                }
                if (selectResultSet != null) {
                    selectResultSet.close();
                }
                if (updatePstmt != null) {
                    updatePstmt.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Done upgrading RAM for service offering of internal loadbalancer vm to " + newRamSize);
    }

    private void upgradeVMwareLocalStorage(Connection conn) {
        PreparedStatement updatePstmt = null;

        try {
            updatePstmt = conn.prepareStatement("update storage_pool set pool_type='VMFS',host_address=@newaddress where " +
                    "(@newaddress:=concat('VMFS datastore: ',path)) is not null and scope = 'HOST' and pool_type = 'LVM' " +
                    "and id in (select * from (select storage_pool.id from storage_pool,cluster where storage_pool.cluster_id = cluster.id and cluster.hypervisor_type='VMware') as t);");
            updatePstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to upgrade vmware local storage pool type", e);
        } finally {
            try {
                if (updatePstmt != null) {
                    updatePstmt.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Done, upgrade vmware local storage pool type to VMFS and host_address to VMFS format");
    }


    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-441to450-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-441to450-cleanup.sql");
        }

        return new File[] {new File(script)};
    }

    private void updateSystemVmTemplates(Connection conn) {
        s_logger.debug("Updating System Vm template IDs");
        try {
            //Get all hypervisors in use
            Set<Hypervisor.HypervisorType> hypervisorsListInUse = new HashSet<Hypervisor.HypervisorType>();
            try (PreparedStatement sel_hyp_type_pstmt = conn.prepareStatement("select distinct(hypervisor_type) from `cloud`.`cluster` where removed is null");) {
                try (ResultSet hyp_type_rs = sel_hyp_type_pstmt.executeQuery();) {
                    while (hyp_type_rs.next()) {
                        switch (Hypervisor.HypervisorType.getType(hyp_type_rs.getString(1))) {
                            case XenServer:
                                hypervisorsListInUse.add(Hypervisor.HypervisorType.XenServer);
                                break;
                            case KVM:
                                hypervisorsListInUse.add(Hypervisor.HypervisorType.KVM);
                                break;
                            case VMware:
                                hypervisorsListInUse.add(Hypervisor.HypervisorType.VMware);
                                break;
                            case Hyperv:
                                hypervisorsListInUse.add(Hypervisor.HypervisorType.Hyperv);
                                break;
                            case LXC:
                                hypervisorsListInUse.add(Hypervisor.HypervisorType.LXC);
                                break;
                        }
                    }
                } catch (SQLException e) {
                    throw new CloudRuntimeException("updateSystemVmTemplates:Exception:" + e.getMessage(), e);
                }
            } catch (SQLException e) {
                throw new CloudRuntimeException("updateSystemVmTemplates:Exception:" + e.getMessage(), e);
            }
            Map<Hypervisor.HypervisorType, String> NewTemplateNameList = new HashMap<Hypervisor.HypervisorType, String>() {
                {
                    put(Hypervisor.HypervisorType.XenServer, "systemvm-xenserver-4.5");
                    put(Hypervisor.HypervisorType.VMware, "systemvm-vmware-4.5");
                    put(Hypervisor.HypervisorType.KVM, "systemvm-kvm-4.5");
                    put(Hypervisor.HypervisorType.LXC, "systemvm-lxc-4.5");
                    put(Hypervisor.HypervisorType.Hyperv, "systemvm-hyperv-4.5");
                }
            };
            Map<Hypervisor.HypervisorType, String> routerTemplateConfigurationNames = new HashMap<Hypervisor.HypervisorType, String>() {
                {
                    put(Hypervisor.HypervisorType.XenServer, "router.template.xen");
                    put(Hypervisor.HypervisorType.VMware, "router.template.vmware");
                    put(Hypervisor.HypervisorType.KVM, "router.template.kvm");
                    put(Hypervisor.HypervisorType.LXC, "router.template.lxc");
                    put(Hypervisor.HypervisorType.Hyperv, "router.template.hyperv");
                }
            };
            Map<Hypervisor.HypervisorType, String> newTemplateUrl = new HashMap<Hypervisor.HypervisorType, String>() {
                {
                    put(Hypervisor.HypervisorType.XenServer, "http://download.cloud.com/templates/4.5/systemvm64template-4.5-xen.vhd.bz2");
                    put(Hypervisor.HypervisorType.VMware, "http://download.cloud.com/templates/4.5/systemvm64template-4.5-vmware.ova");
                    put(Hypervisor.HypervisorType.KVM, "http://download.cloud.com/templates/4.5/systemvm64template-4.5-kvm.qcow2.bz2");
                    put(Hypervisor.HypervisorType.LXC, "http://download.cloud.com/templates/4.5/systemvm64template-4.5-kvm.qcow2.bz2");
                    put(Hypervisor.HypervisorType.Hyperv, "http://download.cloud.com/templates/4.5/systemvm64template-4.5-hyperv.vhd.zip");
                }
            };
            Map<Hypervisor.HypervisorType, String> newTemplateChecksum = new HashMap<Hypervisor.HypervisorType, String>() {
                {
                    put(Hypervisor.HypervisorType.XenServer, "2b15ab4401c2d655264732d3fc600241");
                    put(Hypervisor.HypervisorType.VMware, "3106a79a4ce66cd7f6a7c50e93f2db57");
                    put(Hypervisor.HypervisorType.KVM, "aa9f501fecd3de1daeb9e2f357f6f002");
                    put(Hypervisor.HypervisorType.LXC, "aa9f501fecd3de1daeb9e2f357f6f002");
                    put(Hypervisor.HypervisorType.Hyperv, "70bd30ea02ee9ed67d2c6b85c179cee9");
                }
            };
            for (Map.Entry<Hypervisor.HypervisorType, String> hypervisorAndTemplateName : NewTemplateNameList.entrySet()) {
                s_logger.debug("Updating " + hypervisorAndTemplateName.getKey() + " System Vms");
                try (PreparedStatement sel_templ_pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = ? and removed is null order by id desc limit 1");)
                {
                    //Get 4.5.0 system Vm template Id for corresponding hypervisor
                    sel_templ_pstmt.setString(1, hypervisorAndTemplateName.getValue());
                    try (ResultSet rs = sel_templ_pstmt.executeQuery();)
                    {
                        if (rs.next()) {
                            long templateId = rs.getLong(1);
                            try(PreparedStatement update_cloud_templ_pstmt = conn.prepareStatement("update `cloud`.`vm_template` set type='SYSTEM' where id = ?");)
                            {
                                update_cloud_templ_pstmt.setLong(1, templateId);
                                update_cloud_templ_pstmt.executeUpdate();
                                // update templete ID of system Vms
                            }catch (SQLException e) {
                                throw new CloudRuntimeException("updateSystemVmTemplates:Exception:" + e.getMessage(), e);
                            }
                            try(PreparedStatement update_instance_pstmt = conn.prepareStatement("update `cloud`.`vm_instance` set vm_template_id = ? where type <> 'User' and hypervisor_type = ?");) {
                                update_instance_pstmt.setLong(1, templateId);
                                update_instance_pstmt.setString(2, hypervisorAndTemplateName.getKey().toString());
                                update_instance_pstmt.executeUpdate();
                            }catch (SQLException e) {
                                throw new CloudRuntimeException("updateSystemVmTemplates:Exception:" + e.getMessage(), e);
                            }
                            // Change value of global configuration parameter router.template.* for the corresponding hypervisor
                            try(PreparedStatement  update_cloud_pstmt = conn.prepareStatement("UPDATE `cloud`.`configuration` SET value = ? WHERE name = ?");)
                            {
                                update_cloud_pstmt.setString(1, hypervisorAndTemplateName.getValue());
                                update_cloud_pstmt.setString(2, routerTemplateConfigurationNames.get(hypervisorAndTemplateName.getKey()));
                                update_cloud_pstmt.executeUpdate();
                            }catch (SQLException e) {
                                throw new CloudRuntimeException("updateSystemVmTemplates:Exception:" + e.getMessage(), e);
                            }
                        } else {
                            if (hypervisorsListInUse.contains(hypervisorAndTemplateName.getKey())) {
                                throw new CloudRuntimeException("4.5.0 " + hypervisorAndTemplateName.getKey() + " SystemVm template not found. Cannot upgrade system Vms");
                            } else {
                                s_logger.warn("4.5.0 " + hypervisorAndTemplateName.getKey() + " SystemVm template not found. " + hypervisorAndTemplateName.getKey() + " hypervisor is not used, so not failing upgrade");
                                // Update the latest template URLs for corresponding hypervisor
                                try (PreparedStatement update_pstmt = conn.prepareStatement("UPDATE `cloud`.`vm_template` SET url = ? , checksum = ? WHERE hypervisor_type = ? AND type = 'SYSTEM' AND removed is null order by id desc limit 1");) {
                                    update_pstmt.setString(1, newTemplateUrl.get(hypervisorAndTemplateName.getKey()));
                                    update_pstmt.setString(2, newTemplateChecksum.get(hypervisorAndTemplateName.getKey()));
                                    update_pstmt.setString(3, hypervisorAndTemplateName.getKey().toString());
                                    update_pstmt.executeUpdate();
                                } catch (SQLException e) {
                                    throw new CloudRuntimeException("updateSystemVmTemplates:Exception:" + e.getMessage(), e);
                                }
                            }
                        }
                    } catch (SQLException e) {
                        throw new CloudRuntimeException("updateSystemVmTemplates:Exception:" + e.getMessage(), e);
                    }
                }
                s_logger.debug("Updating System Vm Template IDs Complete");
            }
        }catch(SQLException e){
            throw new CloudRuntimeException("updateSystemVmTemplates:Exception:" + e.getMessage(), e);
        }
    }


    private void dropInvalidKeyFromStoragePoolTable(Connection conn) {
        HashMap<String, List<String>> uniqueKeys = new HashMap<String, List<String>>();
        List<String> keys = new ArrayList<String>();

        keys.add("id_2");
        uniqueKeys.put("storage_pool", keys);

        s_logger.debug("Dropping id_2 key from storage_pool table");
        for (Map.Entry<String, List<String>> entry: uniqueKeys.entrySet()) {
            DbUpgradeUtils.dropKeysIfExist(conn,entry.getKey(), entry.getValue(), false);
        }
    }

    private void dropDuplicatedForeignKeyFromAsyncJobTable(Connection conn) {
        HashMap<String, List<String>> foreignKeys = new HashMap<String, List<String>>();
        List<String> keys = new ArrayList<String>();

        keys.add("fk_async_job_join_map__join_job_id");
        foreignKeys.put("async_job_join_map", keys);

        s_logger.debug("Dropping fk_async_job_join_map__join_job_id key from async_job_join_map table");
        for (Map.Entry<String, List<String>> entry: foreignKeys.entrySet()) {
            DbUpgradeUtils.dropKeysIfExist(conn,entry.getKey(), entry.getValue(), true);
        }
    }
}
