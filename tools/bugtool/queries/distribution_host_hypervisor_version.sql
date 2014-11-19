select hypervisor_type, hypervisor_version, count(*) from host
 where hypervisor_type is not null
 group by hypervisor_type, hypervisor_version
