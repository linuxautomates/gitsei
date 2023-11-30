import { Form } from "antd";
import { get } from "lodash";
import { LevelOpsFilter, OUFilterData } from "model/filters/levelopsFilters";
import React, { useCallback, useMemo } from "react";
import { CustomSelect, NewCustomFormItemLabel } from "shared-resources/components";
import { showInfoProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";
import { toTitleCase } from "utils/stringUtils";

interface UniversalOUFiltersWrapperProps {
  filterProps: LevelOpsFilter;
  metadata: any;
  handleMetadataChange?: (val: any, type: string) => void;
  queryParamDashboardOUId?: any;
}

const UniversalOUFiltersComponent: React.FC<UniversalOUFiltersWrapperProps> = (
  props: UniversalOUFiltersWrapperProps
) => {
  const { handleMetadataChange: onMetadataChange, filterProps, metadata: metaData } = props;
  const { filterMetaData, beKey, filterInfo, defaultValue, allFilters } = filterProps;
  const { filtersByApplications, dashboardMetaData, customFieldsRecords } = filterMetaData as OUFilterData;

  const handleChange = useCallback(
    (application: string) => {
      return (value: any) => {
        const ouFilters = get(metaData, beKey, {});
        const _data = {
          ...ouFilters,
          [application]: value
        };
        onMetadataChange?.(_data, beKey);
      };
    },
    [metaData, onMetadataChange, beKey]
  );

  const renderContent = useMemo(() => {
    return Object.keys(filtersByApplications || {}).map((app: string) => {
      let options: { label: string; value: string }[] = [];

      const filterOptions = filtersByApplications[app].options;

      if (typeof filterOptions === "function") {
        options = filterOptions({ customFieldsRecords });
      } else options = filterOptions;

      const value = get(metaData, [beKey, app], []);
      const finalValue = value.length ? value : defaultValue;
      return (
        <Form.Item key={app} label={<NewCustomFormItemLabel label={toTitleCase(app)} />}>
          <CustomSelect
            createOption={false}
            sortOptions
            labelCase={"title_case"}
            showArrow
            value={finalValue}
            options={options}
            labelKey={"label"}
            valueKey={"value"}
            mode="multiple"
            onChange={handleChange(app)}
          />
        </Form.Item>
      );
    });
  }, [metaData, onMetadataChange, beKey, customFieldsRecords]);

  const filterInfoConfig: showInfoProps = useMemo(() => {
    if (typeof filterInfo === "function")
      return {
        showInfo: !!filterInfo({ filters: allFilters }),
        description: filterInfo({ filters: allFilters }) || ""
      };
    return { showInfo: !!filterInfo, description: filterInfo || "" };
  }, [filterInfo, allFilters]);

  if ((dashboardMetaData?.ou_ids || []).length === 0 && (props.queryParamDashboardOUId || []).length === 0) return null;

  return (
    <>
      <Form.Item label={<NewCustomFormItemLabel label={"OU Unit Overrides"} withInfo={filterInfoConfig} />}>
        <div>{renderContent}</div>
      </Form.Item>
    </>
  );
};

export default React.memo(UniversalOUFiltersComponent);
