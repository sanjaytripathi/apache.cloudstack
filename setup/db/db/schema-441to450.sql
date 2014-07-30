-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

--
-- Schema upgrade from 4.4.0 to 4.5.0
--

-- Disable foreign key checking
-- SET foreign_key_checks = 0;

ALTER TABLE `cloud`.`volumes` ADD COLUMN `provisioning_type` VARCHAR(32) NOT NULL DEFAULT 'thin' COMMENT 'pre allocation setting of the volume';
ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `provisioning_type` VARCHAR(32) NOT NULL DEFAULT 'thin' COMMENT 'pre allocation setting of the volume';

DROP VIEW IF EXISTS `cloud`.`disk_offering_view`;
CREATE VIEW `cloud`.`disk_offering_view` AS
    select
        disk_offering.id,
        disk_offering.uuid,
        disk_offering.name,
        disk_offering.display_text,
        disk_offering.provisioning_type,
        disk_offering.disk_size,
        disk_offering.min_iops,
        disk_offering.max_iops,
        disk_offering.created,
        disk_offering.tags,
        disk_offering.customized,
        disk_offering.customized_iops,
        disk_offering.removed,
        disk_offering.use_local_storage,
        disk_offering.system_use,
        disk_offering.hv_ss_reserve,
        disk_offering.bytes_read_rate,
        disk_offering.bytes_write_rate,
        disk_offering.iops_read_rate,
        disk_offering.iops_write_rate,
        disk_offering.cache_mode,
        disk_offering.sort_key,
        disk_offering.type,
        disk_offering.display_offering,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path
    from
        `cloud`.`disk_offering`
            left join
        `cloud`.`domain` ON disk_offering.domain_id = domain.id
    where
        disk_offering.state='ACTIVE';

DROP TABLE IF EXISTS `cloud`.`external_nuage_vsp_devices`;
CREATE TABLE `cloud`.`external_nuage_vsp_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network in to which nuage vsp is added',
  `provider_name` varchar(255) NOT NULL COMMENT 'the service provider name corresponding to this nuage vsp device',
  `device_name` varchar(255) NOT NULL COMMENT 'the name of the nuage vsp device',
  `host_id` bigint unsigned NOT NULL COMMENT 'host id corresponding to the external nuage vsp device',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_external_nuage_vsp_devices__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_nuage_vsp_devices__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP VIEW IF EXISTS `cloud`.`service_offering_view`;
CREATE VIEW `cloud`.`service_offering_view` AS
    select
        service_offering.id,
        disk_offering.uuid,
        disk_offering.name,
        disk_offering.display_text,
        disk_offering.provisioning_type,
        disk_offering.created,
        disk_offering.tags,
        disk_offering.removed,
        disk_offering.use_local_storage,
        disk_offering.system_use,
        disk_offering.customized_iops,
        disk_offering.min_iops,
        disk_offering.max_iops,
        disk_offering.hv_ss_reserve,
        disk_offering.bytes_read_rate,
        disk_offering.bytes_write_rate,
        disk_offering.iops_read_rate,
        disk_offering.iops_write_rate,
        disk_offering.cache_mode,
        service_offering.cpu,
        service_offering.speed,
        service_offering.ram_size,
        service_offering.nw_rate,
        service_offering.mc_rate,
        service_offering.ha_enabled,
        service_offering.limit_cpu_use,
        service_offering.host_tag,
        service_offering.default_use,
        service_offering.vm_type,
        service_offering.sort_key,
        service_offering.is_volatile,
        service_offering.deployment_planner,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path
    from
        `cloud`.`service_offering`
            inner join
        `cloud`.`disk_offering` ON service_offering.id = disk_offering.id
            left join
        `cloud`.`domain` ON disk_offering.domain_id = domain.id
    where
        disk_offering.state='Active';

DROP VIEW IF EXISTS `cloud`.`volume_view`;
CREATE VIEW `cloud`.`volume_view` AS
    select
        volumes.id,
        volumes.uuid,
        volumes.name,
        volumes.device_id,
        volumes.volume_type,
        volumes.provisioning_type,
        volumes.size,
        volumes.min_iops,
        volumes.max_iops,
        volumes.created,
        volumes.state,
        volumes.attached,
        volumes.removed,
        volumes.pod_id,
        volumes.display_volume,
        volumes.format,
        volumes.path,
        volumes.chain_info,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.networktype data_center_type,
        vm_instance.id vm_id,
        vm_instance.uuid vm_uuid,
        vm_instance.name vm_name,
        vm_instance.state vm_state,
        vm_instance.vm_type,
        user_vm.display_name vm_display_name,
        volume_store_ref.size volume_store_size,
        volume_store_ref.download_pct,
        volume_store_ref.download_state,
        volume_store_ref.error_str,
        volume_store_ref.created created_on_store,
        disk_offering.id disk_offering_id,
        disk_offering.uuid disk_offering_uuid,
        disk_offering.name disk_offering_name,
        disk_offering.display_text disk_offering_display_text,
        disk_offering.use_local_storage,
        disk_offering.system_use,
        disk_offering.bytes_read_rate,
        disk_offering.bytes_write_rate,
        disk_offering.iops_read_rate,
        disk_offering.iops_write_rate,
        disk_offering.cache_mode,
        storage_pool.id pool_id,
        storage_pool.uuid pool_uuid,
        storage_pool.name pool_name,
        cluster.hypervisor_type,
        vm_template.id template_id,
        vm_template.uuid template_uuid,
        vm_template.extractable,
        vm_template.type template_type,
        vm_template.name template_name,
        vm_template.display_text template_display_text,
        iso.id iso_id,
        iso.uuid iso_uuid,
        iso.name iso_name,
        iso.display_text iso_display_text,
        resource_tags.id tag_id,
        resource_tags.uuid tag_uuid,
        resource_tags.key tag_key,
        resource_tags.value tag_value,
        resource_tags.domain_id tag_domain_id,
        resource_tags.account_id tag_account_id,
        resource_tags.resource_id tag_resource_id,
        resource_tags.resource_uuid tag_resource_uuid,
        resource_tags.resource_type tag_resource_type,
        resource_tags.customer tag_customer,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`volumes`
            inner join
        `cloud`.`account` ON volumes.account_id = account.id
            inner join
        `cloud`.`domain` ON volumes.domain_id = domain.id
            left join
        `cloud`.`projects` ON projects.project_account_id = account.id
            left join
        `cloud`.`data_center` ON volumes.data_center_id = data_center.id
            left join
        `cloud`.`vm_instance` ON volumes.instance_id = vm_instance.id
            left join
        `cloud`.`user_vm` ON user_vm.id = vm_instance.id
            left join
        `cloud`.`volume_store_ref` ON volumes.id = volume_store_ref.volume_id
            left join
        `cloud`.`disk_offering` ON volumes.disk_offering_id = disk_offering.id
            left join
        `cloud`.`storage_pool` ON volumes.pool_id = storage_pool.id
            left join
        `cloud`.`cluster` ON storage_pool.cluster_id = cluster.id
            left join
        `cloud`.`vm_template` ON volumes.template_id = vm_template.id
            left join
        `cloud`.`vm_template` iso ON iso.id = volumes.iso_id
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = volumes.id
            and resource_tags.resource_type = 'Volume'
            left join
        `cloud`.`async_job` ON async_job.instance_id = volumes.id
            and async_job.instance_type = 'Volume'
            and async_job.job_status = 0;

CREATE TABLE `cloud`.`external_brocade_vcs_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network in to which brocade vcs switch is added',
  `provider_name` varchar(255) NOT NULL COMMENT 'Service Provider name corresponding to this brocade vcs switch',
  `device_name` varchar(255) NOT NULL COMMENT 'name of the brocade vcs switch',
  `host_id` bigint unsigned NOT NULL COMMENT 'host id coresponding to the external brocade vcs switch',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_external_brocade_vcs_devices__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_brocade_vcs_devices__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`brocade_network_vlan_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `network_id` bigint unsigned NOT NULL COMMENT 'id of the network',
  `vlan_id` int(10) COMMENT 'vlan id of the network',
  PRIMARY KEY  (`id`),
   CONSTRAINT `fk_brocade_network_vlan_map__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/* As part of the separation of Xen and XenServer, update the column for the network labels */
ALTER TABLE `cloud`.`physical_network_traffic_types` CHANGE `xen_network_label` `xenserver_network_label` varchar(255) COMMENT 'The network name label of the physical device dedicated to this traffic on a XenServer host';

/*Adding domainId field to the user table in order to restrict duplicated users creation on the db level*/
ALTER TABLE `cloud`.`user` ADD COLUMN domain_id bigint(20) unsigned DEFAULT NULL;
ALTER TABLE `cloud`.`user` ADD CONSTRAINT `fk_user__domain_id` FOREIGN KEY `fk_user__domain_id`(`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE;
UPDATE `cloud`.`user` SET `cloud`.`user`.domain_id=(SELECT `cloud`.`account`.domain_id   FROM `cloud`.`account`   WHERE `cloud`.`account`.id=`cloud`.`user`.account_id) where id > 0;
ALTER TABLE `cloud`.`user` ADD UNIQUE KEY `username_domain_id` (`username`,`domain_id`);

ALTER TABLE `cloud`.`volumes` CHANGE COLUMN `iso_id` `iso_id` bigint(20) unsigned COMMENT 'The id of the iso from which the volume was created';

DROP VIEW IF EXISTS `cloud`.`storage_pool_view`;
CREATE VIEW `cloud`.`storage_pool_view` AS
    select
        storage_pool.id,
        storage_pool.uuid,
        storage_pool.name,
        storage_pool.status,
        storage_pool.path,
        storage_pool.pool_type,
        storage_pool.host_address,
        storage_pool.created,
        storage_pool.removed,
        storage_pool.capacity_bytes,
        storage_pool.capacity_iops,
        storage_pool.scope,
        storage_pool.hypervisor,
        storage_pool.storage_provider_name,
        cluster.id cluster_id,
        cluster.uuid cluster_uuid,
        cluster.name cluster_name,
        cluster.cluster_type,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.networktype data_center_type,
        host_pod_ref.id pod_id,
        host_pod_ref.uuid pod_uuid,
        host_pod_ref.name pod_name,
        storage_pool_details.name tag,
        op_host_capacity.used_capacity disk_used_capacity,
        op_host_capacity.reserved_capacity disk_reserved_capacity,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`storage_pool`
            left join
        `cloud`.`cluster` ON storage_pool.cluster_id = cluster.id
            left join
        `cloud`.`data_center` ON storage_pool.data_center_id = data_center.id
            left join
        `cloud`.`host_pod_ref` ON storage_pool.pod_id = host_pod_ref.id
            left join
        `cloud`.`storage_pool_details` ON storage_pool_details.pool_id = storage_pool.id
            and storage_pool_details.value = 'true'
            left join
        `cloud`.`op_host_capacity` ON storage_pool.id = op_host_capacity.host_id
            and op_host_capacity.capacity_type in (3,9)
            left join
        `cloud`.`async_job` ON async_job.instance_id = storage_pool.id
            and async_job.instance_type = 'StoragePool'
            and async_job.job_status = 0;


DROP VIEW IF EXISTS `cloud`.`template_view`;
CREATE VIEW `cloud`.`template_view` AS
    select
        vm_template.id,
        vm_template.uuid,
        vm_template.unique_name,
        vm_template.name,
        vm_template.public,
        vm_template.featured,
        vm_template.type,
        vm_template.hvm,
        vm_template.bits,
        vm_template.url,
        vm_template.format,
        vm_template.created,
        vm_template.checksum,
        vm_template.display_text,
        vm_template.enable_password,
        vm_template.dynamically_scalable,
        vm_template.state template_state,
        vm_template.guest_os_id,
        guest_os.uuid guest_os_uuid,
        guest_os.display_name guest_os_name,
        vm_template.bootable,
        vm_template.prepopulate,
        vm_template.cross_zones,
        vm_template.hypervisor_type,
        vm_template.extractable,
        vm_template.template_tag,
        vm_template.sort_key,
        vm_template.removed,
        vm_template.enable_sshkey,
        source_template.id source_template_id,
        source_template.uuid source_template_uuid,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        launch_permission.account_id lp_account_id,
        template_store_ref.store_id,
        image_store.scope as store_scope,
        template_store_ref.state,
        template_store_ref.download_state,
        template_store_ref.download_pct,
        template_store_ref.error_str,
        template_store_ref.size,
        template_store_ref.destroyed,
        template_store_ref.created created_on_store,
        vm_template_details.name detail_name,
        vm_template_details.value detail_value,
        resource_tags.id tag_id,
        resource_tags.uuid tag_uuid,
        resource_tags.key tag_key,
        resource_tags.value tag_value,
        resource_tags.domain_id tag_domain_id,
        resource_tags.account_id tag_account_id,
        resource_tags.resource_id tag_resource_id,
        resource_tags.resource_uuid tag_resource_uuid,
        resource_tags.resource_type tag_resource_type,
        resource_tags.customer tag_customer,
        CONCAT(vm_template.id, '_', IFNULL(data_center.id, 0)) as temp_zone_pair
    from
        `cloud`.`vm_template`
            inner join
        `cloud`.`guest_os` ON guest_os.id = vm_template.guest_os_id
            inner join
        `cloud`.`account` ON account.id = vm_template.account_id
            inner join
        `cloud`.`domain` ON domain.id = account.domain_id
            left join
        `cloud`.`projects` ON projects.project_account_id = account.id
            left join
        `cloud`.`vm_template_details` ON vm_template_details.template_id = vm_template.id
            left join
        `cloud`.`vm_template` source_template ON source_template.id = vm_template.source_template_id
            left join
        `cloud`.`template_store_ref` ON template_store_ref.template_id = vm_template.id and template_store_ref.store_role = 'Image' and template_store_ref.destroyed=0
            left join
        `cloud`.`image_store` ON image_store.removed is NULL AND template_store_ref.store_id is not NULL AND image_store.id = template_store_ref.store_id
            left join
        `cloud`.`template_zone_ref` ON template_zone_ref.template_id = vm_template.id AND template_store_ref.store_id is NULL AND template_zone_ref.removed is null
            left join
        `cloud`.`data_center` ON (image_store.data_center_id = data_center.id OR template_zone_ref.zone_id = data_center.id)
            left join
        `cloud`.`launch_permission` ON launch_permission.template_id = vm_template.id
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = vm_template.id
            and (resource_tags.resource_type = 'Template' or resource_tags.resource_type='ISO');



UPDATE `cloud`.`configuration` SET description="If set to true, will set guest VM's name as it appears on the hypervisor, to its hostname. The flag is supported for VMware hypervisor only" WHERE name='vm.instancename.flag';
