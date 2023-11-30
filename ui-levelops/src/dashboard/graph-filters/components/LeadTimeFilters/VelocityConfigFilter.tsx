import React, { useEffect, useMemo, useState } from "react";
import { Form, Select } from "antd";
import { AZURE_LEAD_TIME_ISSUE_REPORT, LEAD_TIME_REPORTS } from "dashboard/constants/applications/names";
import { AntSelect, AntButton, AntTag, CustomFormItemLabel, CustomSelect } from "shared-resources/components";
import { getStageDurationComputationOptions } from "../helper";
import { stringSortingComparator } from "../sort.helper";
import { ITEM_TEST_ID } from "../Constants";
import useVelocityConfigProfiles from "custom-hooks/useVelocityConfigProfiles";

const { Option } = Select;

interface VelocityConfigFilterProps {
  filters: any;
  metaData: any;
  onFilterValueChange: (value: any, key: string) => void;
  onMetadataChange?: (value: any, type: any, reportType?: String) => void;
  reportType: string;
}

const VelocityConfigFilter: React.FC<VelocityConfigFilterProps> = props => {
  const { filters, metaData, reportType, onFilterValueChange, onMetadataChange } = props;

  const [searchFilterQuery, setSearchFilterQuery] = useState("");
  const [showLeadTimeAdvancedSettings, setShowLeadTimeAdvancedSettings] = useState<boolean>(false);
  const { apiData, apiLoading, defaultProfile } = useVelocityConfigProfiles(reportType);

  const data = apiData?.filter((record: any) => record?.is_new === false);
  const loading = apiLoading;
  const defaultConfigId = defaultProfile?.id;
  const configId = filters?.velocity_config_id || defaultConfigId;
  const tagStyle = { marginLeft: "8px" };

  const modifiedOptions = useMemo(() => {
    if (loading) {
      return [];
    }
    return data
      .sort(stringSortingComparator("name"))
      .filter((item: any) => {
        if (searchFilterQuery) {
          return item?.name?.toLocaleUpperCase()?.includes(searchFilterQuery?.toLocaleUpperCase());
        }
        return true;
      })
      .map((item: any) => (
        <Option key={item.id} value={item.id}>
          {item.name}
          {!!item.default_config && (
            <AntTag color="purple" style={tagStyle}>
              Default
            </AntTag>
          )}
        </Option>
      ));
  }, [loading, data, searchFilterQuery]);

  const stages = useMemo(() => {
    const selectedConfig = data.find((item: any) => item.id === configId);

    if (selectedConfig) {
      const configStages = [
        ...(selectedConfig.pre_development_custom_stages || []),
        ...(selectedConfig.fixed_stages || []),
        ...(selectedConfig.post_development_custom_stages || [])
      ];
      return configStages.map((item: any) => item.name);
    }
    return [];
  }, [data, configId]);

  const renderConfigField = useMemo(
    () => (
      <Form.Item
        key="lead_time_configuration_profile"
        label={"Workflow Configuration Profile"}
        data-filterselectornamekey={`${ITEM_TEST_ID}-lead-config-profile`}
        data-filtervaluesnamekey={`${ITEM_TEST_ID}-lead-config-profile`}>
        <AntSelect
          dropdownTestingKey={`${ITEM_TEST_ID}-lead-config-profile_dropdown`}
          placeholder="Workflow Configuration Profile"
          showArrow
          showSearch
          filterOption={false}
          loading={loading}
          value={configId}
          onChange={(value: any, options: any) => onFilterValueChange(value, "velocity_config_id")}
          onBlur={(e: any) => setSearchFilterQuery("")}
          onSearch={(value: any) => {
            setSearchFilterQuery(value);
          }}>
          {modifiedOptions}
        </AntSelect>
      </Form.Item>
    ),
    [loading, data, filters, modifiedOptions]
  );

  const renderShowHideBtn = useMemo(
    () => (
      <AntButton
        className={"velocity-advanced-settings-button"}
        onClick={() => setShowLeadTimeAdvancedSettings(state => !state)}>
        {showLeadTimeAdvancedSettings ? "- Hide Advanced Settings" : "+ Show Advanced Settings"}
      </AntButton>
    ),
    [showLeadTimeAdvancedSettings]
  );

  const renderAdvanceSettings = useMemo(() => {
    if (!showLeadTimeAdvancedSettings) {
      return null;
    }

    return (
      <>
        <Form.Item
          label={
            <CustomFormItemLabel
              label="Exclude Stages"
              withInfo={{
                showInfo: true,
                description: "Exclude selected stages from Lead Time computation"
              }}
            />
          }
          data-filterselectornamekey={`${ITEM_TEST_ID}-exclude-stages`}
          data-filtervaluesnamekey={`${ITEM_TEST_ID}-exclude-stages`}>
          <CustomSelect
            dataFilterNameDropdownKey={`${ITEM_TEST_ID}-exclude-stages_dropdown`}
            sortOptions
            mode="multiple"
            createOption={true}
            labelCase={"none"}
            options={stages}
            showArrow={true}
            value={metaData?.hide_stages || []}
            onChange={(value: any) => onMetadataChange?.(value, "hide_stages")}
          />
        </Form.Item>
        {reportType !== LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT && (
          <Form.Item
            key="limit_to_only_applicable_data"
            label="Computation Model"
            data-filterselectornamekey={`${ITEM_TEST_ID}-computational-model`}
            data-filtervaluesnamekey={`${ITEM_TEST_ID}-computational-model`}>
            <AntSelect
              showArrow
              dropdownTestingKey={`${ITEM_TEST_ID}-computational-model_dropdown`}
              value={!!filters?.limit_to_only_applicable_data}
              options={getStageDurationComputationOptions(filters?.calculation).sort(stringSortingComparator("label"))}
              mode="single"
              onChange={(value: any, options: any) => onFilterValueChange(value, "limit_to_only_applicable_data")}
            />
          </Form.Item>
        )}
      </>
    );
  }, [reportType, filters, showLeadTimeAdvancedSettings, data, metaData]);

  return (
    <>
      {renderConfigField}
      {renderShowHideBtn}
      {renderAdvanceSettings}
    </>
  );
};

export default VelocityConfigFilter;
