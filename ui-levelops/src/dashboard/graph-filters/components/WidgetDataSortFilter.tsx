import React, { useMemo } from "react";
import { Form } from "antd";
import { get } from "lodash";
import {
  widgetDataSortingOptions,
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { AntSelect } from "shared-resources/components";
import { widgetFilterOptionsNode } from "dashboard/helpers/helper";
import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import { ITEM_TEST_ID } from "./Constants";
import { useSelector } from "react-redux";
import { customTimeFilterKeysSelector } from "reduxConfigs/selectors/jira.selector";

interface WidgetDataSortFilterProps {
  acrossIsAzureIteration?: boolean;
  filters: any;
  onFilterValueChange?: (value: any, type?: any, exclude?: boolean, addToMetaData?: any) => void;
}
const WidgetDataSortFilter: React.FC<WidgetDataSortFilterProps> = ({
  filters,
  onFilterValueChange,
  acrossIsAzureIteration
}) => {
  const customeDateTimeKeysFields: Array<string> = useSelector(customTimeFilterKeysSelector);
  const acrossValue = get(filters, ["across"], "");

  const getSortingOptions = useMemo(() => {
    if (acrossIsAzureIteration) {
      return widgetDataSortingOptions[widgetDataSortingOptionsNodeType.AZURE_ITERATION_BASED];
    }
    return widgetDataSortingOptions[widgetFilterOptionsNode(acrossValue, customeDateTimeKeysFields)];
  }, [acrossValue, customeDateTimeKeysFields, acrossIsAzureIteration]);

  const defaultValue = useMemo(
    () => widgetDataSortingOptionsDefaultValue[widgetFilterOptionsNode(acrossValue, customeDateTimeKeysFields)],
    [[acrossValue, customeDateTimeKeysFields]]
  );

  return (
    <Form.Item
      key="sort_xaxis"
      label={"Sort X-Axis"}
      data-filterselectornamekey={`${ITEM_TEST_ID}-widget-sorting`}
      data-filtervaluesnamekey={`${ITEM_TEST_ID}-widget-sorting`}>
      <AntSelect
        dropdownTestingKey={`${ITEM_TEST_ID}-widget-sorting_dropdown`}
        showArrow={true}
        value={get(filters, [WIDGET_DATA_SORT_FILTER_KEY], defaultValue)}
        options={getSortingOptions}
        mode={"single"}
        onChange={(value: any) => onFilterValueChange?.(value, WIDGET_DATA_SORT_FILTER_KEY)}
      />
    </Form.Item>
  );
};

export default WidgetDataSortFilter;
