import { Form } from "antd";
import { useTicketCategorizationFilters } from "custom-hooks";
import { get, uniq } from "lodash";
import { EffortInvestmentProfileFilterData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { default as React, useEffect, useMemo } from "react";
import { NewCustomFormItemLabel, CustomSelect } from "shared-resources/components";
import { ITEM_TEST_ID } from "../Constants";
import { stringSortingComparator } from "../sort.helper";

interface UniversalEffortInvestmentProfileFilterProps {
  filterProps: LevelOpsFilter;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
}

const UniversalEffortInvestmentProfileFilter: React.FC<UniversalEffortInvestmentProfileFilterProps> = (
  props: UniversalEffortInvestmentProfileFilterProps
) => {
  const { filterProps, onFilterValueChange } = props;
  const { label, beKey, allFilters: filters, filterMetaData, required } = filterProps;
  const {
    dashboardMetaData,
    reportType,
    showDefaultScheme,
    withProfileCategory,
    isCategoryRequired,
    categorySelectionMode,
    categoryBEKey,
    allowClearEffortInvestmentProfile
  } = filterMetaData as EffortInvestmentProfileFilterData;
  const { apiData } = useTicketCategorizationFilters(reportType!, [reportType]);
  const ticketCategorizationSchemesValue = get(filters, [beKey], undefined);

  useEffect(() => {
    const defaultScheme = apiData.filter((data: any) => data?.default_scheme)[0];
    if (showDefaultScheme && defaultScheme && !ticketCategorizationSchemesValue) {
      onFilterValueChange(defaultScheme.id, beKey);
    }
  }, [apiData, ticketCategorizationSchemesValue]);

  const ticketCategorizationSchemesCategoryValue = get(filters, [categoryBEKey ?? "ticket_categories"], undefined);

  const getSchemeOptions = useMemo(() => {
    const options = apiData.map((data: any) => {
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
      const scheme = apiData.find((record: any) => record?.id === ticketCategorizationSchemesValue);
      if (scheme) {
        const categories = Object.values(get(scheme, ["config", "categories"], {}));
        uniq(categories).map((category: any) => {
          categoryOptions.push({ label: category?.name, value: category?.name });
        });
      }
    }
    return categoryOptions.sort(stringSortingComparator("label"));
  }, [ticketCategorizationSchemesValue, apiData]);

  const isRequired = useMemo(() => {
    if (typeof required === "boolean") return required;
    if (required instanceof Function) return required({ filters });
    return false;
  }, [required, filters]);

  const categoryRequired = useMemo(() => {
    if (typeof isCategoryRequired === "boolean") return isCategoryRequired;
    if (isCategoryRequired instanceof Function) return isCategoryRequired({ filters });
    return false;
  }, [isCategoryRequired, filters]);

  // show dahsboard level profile if it is enabled
  const isDashboardEffortProfileEnabled = useMemo(() => {
    return get(dashboardMetaData, ["effort_investment_profile"], false);
  }, [dashboardMetaData]);

  return (
    <>
      <Form.Item
        key="ticket_categorization_scheme_selection"
        label={<NewCustomFormItemLabel label={label} required={isRequired} />}
        data-filterselectornamekey={`${ITEM_TEST_ID}-effort-profiles`}
        data-filtervaluesnamekey={`${ITEM_TEST_ID}-effort-profiles`}>
        <CustomSelect
          createOption={false}
          labelKey={"label"}
          allowClear={allowClearEffortInvestmentProfile}
          valueKey={"value"}
          labelCase={"none"}
          mode={"default"}
          disabled={isDashboardEffortProfileEnabled}
          showArrow={true}
          value={
            isDashboardEffortProfileEnabled
              ? get(dashboardMetaData, ["effort_investment_profile_filter"], "")
              : ticketCategorizationSchemesValue
          }
          options={getSchemeOptions}
          onChange={(value: any) => onFilterValueChange(value, beKey)}
        />
      </Form.Item>
      {withProfileCategory && (
        <Form.Item
          className={"custom-universal-filter-item"}
          key="ticket_categorization_category_selection"
          label={<NewCustomFormItemLabel label={"Effort Investment Category"} required={categoryRequired} />}
          data-filterselectornamekey={`${ITEM_TEST_ID}-ba-category`}
          data-filtervaluesnamekey={`${ITEM_TEST_ID}-ba-category`}>
          <CustomSelect
            createOption={false}
            labelCase={"none"}
            allowClear
            labelKey={"label"}
            valueKey={"value"}
            disabled={!ticketCategorizationSchemesValue}
            showArrow={true}
            value={ticketCategorizationSchemesCategoryValue}
            options={getCategoryOptions}
            mode={categorySelectionMode || "multiple"}
            onChange={(value: any) =>
              onFilterValueChange(
                !!value && categorySelectionMode === "default" ? [value] : value,
                categoryBEKey ?? "ticket_categories"
              )
            }
          />
        </Form.Item>
      )}
    </>
  );
};

export default UniversalEffortInvestmentProfileFilter;
