import { Form } from "antd";
import { getFilterValue } from "configurable-dashboard/helpers/helper";
import { filterWithInfoType, FILTER_WITH_INFO_MAPPING } from "dashboard/constants/filterWithInfo.mapping";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { map } from "lodash";
import React, { useCallback, useMemo } from "react";
import { CustomFormItemLabel, CustomSelect } from "shared-resources/components";
import { sanitizeStages } from "shared-resources/containers/widget-api-wrapper/helper";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { ITEM_TEST_ID } from "./Constants";

interface JiraExcludeStatusFilterProps {
  data: any[];
  filters: any;
  reportType: string;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  createOption?: boolean;
}

const JiraExcludeStatusFilter: React.FC<JiraExcludeStatusFilterProps> = (props: JiraExcludeStatusFilterProps) => {
  const { data, filters, reportType, onFilterValueChange, createOption = true } = props;

  const excludeStatusState = useParamSelector(getGenericRestAPISelector, {
    uri: "jira_filter_values",
    method: "list",
    uuid: "exclude_status"
  });

  const filterInfo: filterWithInfoType[] = useMemo(
    () => getWidgetConstant(reportType, FILTER_WITH_INFO_MAPPING, {}),
    [reportType]
  );

  const filterValue = useCallback(
    (key: string) => {
      let _filters = sanitizeStages(excludeStatusState, reportType, filters);

      return getFilterValue(_filters, key);
    },
    [filters, filterInfo]
  );

  const renderFilters = useMemo(
    () =>
      map(filterInfo, filter => (
        <Form.Item
          key="jira_stages"
          data-filterselectornamekey={`${ITEM_TEST_ID}-jira-stages`}
          data-filtervaluesnamekey={`${ITEM_TEST_ID}-jira-stages`}
          label={
            <CustomFormItemLabel
              label={filter.label}
              withInfo={{
                showInfo: true,
                description: filter.description
              }}
            />
          }>
          <CustomSelect
            valueKey={"key"}
            mode="multiple"
            createOption={createOption}
            labelCase={"title_case"}
            options={data}
            showArrow={true}
            sortOptions
            value={filterValue(filter.filterKey)}
            onChange={(value: any) => onFilterValueChange(value, filter.filterKey, true)}
          />
        </Form.Item>
      )),
    [filterInfo, filters, data]
  );

  return <>{renderFilters}</>;
};

export default React.memo(JiraExcludeStatusFilter);
