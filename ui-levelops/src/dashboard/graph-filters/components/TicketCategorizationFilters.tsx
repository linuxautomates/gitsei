import { Form } from "antd";
import {
  CATEGORY_SELECTION_MODE,
  DISABLE_CATEGORY_SELECTION,
  RequiredFiltersType,
  REQUIRED_FILTERS_MAPPING,
  TICKET_CATEGORIZATION_SCHEMES_KEY,
  TICKET_CATEGORIZATION_SCHEME_CATEGORY_KEY
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { get, uniq } from "lodash";
import React, { useMemo } from "react";
import { AntSelect } from "shared-resources/components";
import { ITEM_TEST_ID } from "./Constants";
import { stringSortingComparator } from "./sort.helper";

interface TicketCategorizationFiltersProps {
  reportType: string;
  apiData: any[];
  filters: any;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  dashboardMetadata?: any;
}

const TicketCategorizationFilters: React.FC<TicketCategorizationFiltersProps> = ({
  apiData,
  filters,
  reportType,
  onFilterValueChange,
  dashboardMetadata
}) => {
  const disableCategorySelection = getWidgetConstant(reportType, [DISABLE_CATEGORY_SELECTION], false);
  const requiredFilters = getWidgetConstant(reportType, REQUIRED_FILTERS_MAPPING);

  const categorySelectionMode = getWidgetConstant(reportType, CATEGORY_SELECTION_MODE);
  const ticketCategorizationSchemesValue = get(filters, [TICKET_CATEGORIZATION_SCHEMES_KEY], undefined);
  const ticketCategorizationSchemesCategoryValue = get(filters, [TICKET_CATEGORIZATION_SCHEME_CATEGORY_KEY], undefined);

  const getSchemeOptions = useMemo(() => {
    const options = apiData.map(data => {
      return {
        label: data?.name,
        value: data?.id
      };
    });
    return options.sort(stringSortingComparator("label"));
  }, [apiData]);

  const getCategoryOptions = useMemo(() => {
    let categoryOptions: any = [];
    if (ticketCategorizationSchemesValue) {
      const scheme = apiData.find(record => record?.id === ticketCategorizationSchemesValue);
      if (scheme) {
        const categories = Object.values(get(scheme, ["config", "categories"], {}));
        uniq(categories).map((category: any) => {
          categoryOptions.push({ label: category?.name, value: category?.name });
        });
      }
    }
    return categoryOptions.sort(stringSortingComparator("label"));
  }, [ticketCategorizationSchemesValue, apiData]);

  return (
    <>
      <Form.Item
        key="ticket_categorization_scheme_selection"
        label={"Effort Investment Profile"}
        data-filterselectornamekey={`${ITEM_TEST_ID}-effort-profiles`}
        data-filtervaluesnamekey={`${ITEM_TEST_ID}-effort-profiles`}
        required={requiredFilters?.[RequiredFiltersType.SCHEME_SELECTION]}>
        <AntSelect
          disabled={get(dashboardMetadata, ["effort_investment_profile"], false)}
          dropdownTestingKey={`${ITEM_TEST_ID}-effort-profiles_dropdown`}
          showArrow={true}
          value={(apiData || []).length ? ticketCategorizationSchemesValue : ""} // not showing id if there is no profile data
          options={getSchemeOptions}
          mode={"single"}
          onChange={(value: any) => onFilterValueChange(value, TICKET_CATEGORIZATION_SCHEMES_KEY)}
        />
      </Form.Item>
      {!disableCategorySelection && (
        <Form.Item
          className={"custom-universal-filter-item"}
          key="ticket_categorization_category_selection"
          label="Effort Investment Category"
          data-filterselectornamekey={`${ITEM_TEST_ID}-ba-category`}
          data-filtervaluesnamekey={`${ITEM_TEST_ID}-ba-category`}
          required={requiredFilters?.[RequiredFiltersType.CATEGORY_SELECTION]}>
          <AntSelect
            dropdownTestingKey={`${ITEM_TEST_ID}-ba-category_dropdown`}
            allowClear
            disabled={!ticketCategorizationSchemesValue}
            showArrow={true}
            value={ticketCategorizationSchemesCategoryValue}
            options={getCategoryOptions}
            mode={categorySelectionMode || "multiple"}
            onChange={(value: any) =>
              onFilterValueChange(
                !!value && categorySelectionMode === "single" ? [value] : value,
                TICKET_CATEGORIZATION_SCHEME_CATEGORY_KEY
              )
            }
          />
        </Form.Item>
      )}
    </>
  );
};

export default TicketCategorizationFilters;
