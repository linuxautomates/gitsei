package io.levelops.integrations.tenable.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Bean for Asset download response <a href="https://cloud.tenable.com/assets/export/export_uuid/chunks/chunk_id</a>
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Asset.AssetBuilder.class)
public class Asset {
    @JsonProperty
    String id;

    @JsonProperty("has_agent")
    Boolean hasAgent;

    @JsonProperty("has_plugin_results")
    Boolean hasPluginResults;

    @JsonProperty("created_at")
    String createdAt;

    @JsonProperty("terminated_at")
    String terminatedAt;

    @JsonProperty("terminated_by")
    String terminatedBy;

    @JsonProperty("updated_at")
    String updatedAt;

    @JsonProperty("deleted_at")
    String deletedAt;

    @JsonProperty("deleted_by")
    String deletedBy;

    @JsonProperty("first_seen")
    String firstSeen;

    @JsonProperty("last_seen")
    String lastSeen;

    @JsonProperty("first_scan_time")
    String firstScanTime;

    @JsonProperty("last_scan_time")
    String lastScanTime;

    @JsonProperty("last_authenticated_scan_date")
    String lastAuthenticatedScanDate;

    @JsonProperty("last_licensed_scan_date")
    String lastLicensedScanDate;

    @JsonProperty("last_scan_id")
    String lastScanId;

    @JsonProperty("last_schedule_id")
    String lastScheduleId;

    @JsonProperty("azure_vm_id")
    String azureVmID;

    @JsonProperty("azure_resource_id")
    String azureResourceId;

    @JsonProperty("gcp_project_id")
    String gcpProjectId;

    @JsonProperty("gcp_zone")
    String gcpZone;

    @JsonProperty("gcp_instance_id")
    String gcpInstanceId;

    @JsonProperty("aws_ec2_instance_ami_id")
    String awsEC2InstanceAMIId;

    @JsonProperty("aws_ec2_instance_id")
    String awsEC2InstanceId;

    @JsonProperty("agent_uuid")
    String agentUUID;

    @JsonProperty("bios_uuid")
    String biosUUID;

    @JsonProperty("network_id")
    String networkId;

    @JsonProperty("network_name")
    String networkName;

    @JsonProperty("aws_owner_id")
    String awsOwnerId;

    @JsonProperty("aws_availability_zone")
    String awsAvailabilityZone;

    @JsonProperty("aws_region")
    String awsRegion;

    @JsonProperty("aws_vpc_id")
    String awsVPCId;

    @JsonProperty("aws_ec2_instance_group_name")
    String awsEC2InstanceGroupName;

    @JsonProperty("aws_ec2_instance_state_name")
    String awsEC2InstanceStateName;

    @JsonProperty("aws_ec2_instance_type")
    String awsEC2InstanceType;

    @JsonProperty("aws_subnet_id")
    String awsSubnetId;

    @JsonProperty("aws_ec2_product_code")
    String awsEC2ProductCode;

    @JsonProperty("aws_ec2_name")
    String awsEC2Name;

    @JsonProperty("mcafee_epo_guid")
    String mcafeeEPOUUID;

    @JsonProperty("mcafee_epo_agent_guid")
    String mcafeeEPOAgentGUID;

    @JsonProperty("servicenow_sysid")
    String servicenowSysid;

    @JsonProperty("bigfix_asset_id")
    String bigfixAssetId;

    @JsonProperty("agent_names")
    List<String> agentNames;

    @JsonProperty("installed_software")
    List<String> installedSoftware;

    @JsonProperty
    List<String> ipv4s;

    @JsonProperty
    List<String> ipv6s;

    @JsonProperty
    List<String> fqdns;

    @JsonProperty("mac_addresses")
    List<String> macAddresses;

    @JsonProperty("netbios_names")
    List<String> netbiosNames;

    @JsonProperty("operating_systems")
    List<String> operatingSystems;

    @JsonProperty("system_types")
    List<String> systemTypes;

    @JsonProperty
    List<String> hostnames;

    @JsonProperty("ssh_fingerprints")
    List<String> sslFingerprints;

    @JsonProperty("qualys_asset_ids")
    List<String> qualysAssetIds;

    @JsonProperty("qualys_host_ids")
    List<String> qualysHostIds;

    @JsonProperty("manufacturer_tpm_ids")
    List<String> manufacturerTpmIds;

    @JsonProperty("symantec_ep_hardware_keys")
    List<String> symantecEPHardwareKeys;

    @JsonProperty
    List<Source> sources;

    @JsonProperty
    List<Tag> tags;

    @JsonProperty("network_interfaces")
    List<NetworkInterface> networkInterfaces;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = NetworkInterface.NetworkInterfaceBuilder.class)
    public static class NetworkInterface {
        @JsonProperty
        String name;

        @JsonProperty("mac_address")
        List<String> macAddress;

        @JsonProperty
        List<String> ipv4;

        @JsonProperty
        List<String> ipv6;

        @JsonProperty
        List<String> fqdn;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Source.SourceBuilder.class)
    public static class Source {
        @JsonProperty
        String name;

        @JsonProperty("first_seen")
        String firstSeen;

        @JsonProperty("last_seen")
        String lastSeen;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Tag.TagBuilder.class)
    public static class Tag {
        @JsonProperty
        String uuid;

        @JsonProperty
        String key;

        @JsonProperty
        String value;

        @JsonProperty("added_by")
        String addedBy;

        @JsonProperty("added_at")
        String addedAt;
    }
}
