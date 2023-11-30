import React from "react";
import { useSupportedFilters } from "../../../../custom-hooks/useSupportedFilters";
import { Spin } from "antd";
import { DashboardGraphFilters } from "../../../graph-filters/components";

interface CommonApplicationFiltersProps {
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  onExcludeChange?: (key: string, value: boolean) => void;
  onTimeRangeTypeChange?: (key: string, value: any) => void;
  onTimeFilterValueChange?: (value: any, type?: any, rangeType?: string) => void;
  onPartialChange: (key: string, value: any) => void;
  partialFilterError?: any;
  metaData?: any;
  filters: any;
  integrationIds: Array<any>;
  supported_filters: any;
  application: string;
  reportType: string;
}

const CommonApplicationFilters: React.FC<CommonApplicationFiltersProps> = (props: CommonApplicationFiltersProps) => {
  const { loading: apiLoading, apiData } = useSupportedFilters(
    props.supported_filters,
    props.integrationIds,
    props.application
  );

  const getCustomFieldData = () => {
    const fields = apiData.find((item: any) => Object.keys(item)[0] === "custom_fields");
    if (fields && fields.hasOwnProperty("custom_fields")) {
      return fields.custom_fields
        .map((field: any) => {
          const valuesRecord = apiData.find(item => Object.keys(item)[0] === field.key);
          if (valuesRecord) {
            return {
              name: field.name,
              key: field.key,
              values: valuesRecord[Object.keys(valuesRecord)[0]]
            };
          }
          return undefined;
        })
        .filter((item: any) => item !== undefined);
    } else return [];
  };

  const getSupportedFiltersData = () => {
    return apiData
      .map((item: any) => {
        if (props.supported_filters && props.supported_filters.values.includes(Object.keys(item)[0])) {
          return item;
        }
        return undefined;
      })
      .filter(item => item !== undefined);
  };

  return (
    <div style={{ width: "100%", height: "100%" }}>
      {apiLoading && (
        <div style={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100%" }}>
          <Spin />
        </div>
      )}
      {!apiLoading && apiData && (
        <DashboardGraphFilters
          customData={getCustomFieldData()}
          application={props.application}
          data={getSupportedFiltersData()}
          filters={props.filters}
          reportType={props.reportType}
          onFilterValueChange={props.onFilterValueChange}
          applicationUse={true}
          onExcludeChange={props.onExcludeChange}
          metaData={props.metaData}
          onPartialChange={props.onPartialChange}
          partialFilterError={props.partialFilterError}
          onTimeRangeTypeChange={props.onTimeRangeTypeChange}
          onTimeFilterValueChange={props.onTimeFilterValueChange}
          integrationIds={props.integrationIds}
        />
      )}
    </div>
  );
};

export default CommonApplicationFilters;
