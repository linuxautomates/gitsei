import { Form } from "antd";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { useTicketCategorizationFilters } from "custom-hooks";
import {
  ACTIVE_WORK_UNIT_FILTER_KEY,
  azureActiveWorkUnitOptions,
  azureEffortInvestmentUnitFilterOptions,
  BA_WIDGET_TIME_RANGE_FILTER_CONFIG,
  DefaultKeyTypes,
  INTERVAL_OPTIONS,
  jiraActiveWorkUnitOptions,
  SHOW_EFFORT_UNIT_INSIDE_TAB,
  SHOW_PROFILE_INSIDE_TAB,
  SHOW_SAMPLE_INTERVAL_INSIDE_TAB,
  SUPPORT_ACTIVE_WORK_UNIT_FILTERS,
  SUPPORT_CATEGORY_EPIC_ACROSS_FILTER,
  SUPPORT_DISPLAY_FORMAT_FILTERS,
  SUPPORT_TICKET_CATEGORIZATION_FILTERS,
  SUPPORT_TICKET_CATEGORIZATION_UNIT_FILTERS,
  SUPPORT_TIME_RANGE_FILTER,
  SUPPORT_TREND_INTERVAL,
  ticketCategorizationUnitFilterOptions,
  TICKET_CATEGORIZATION_SCHEMES_KEY,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { WidgetTabsContext } from "dashboard/pages/context";
import { get } from "lodash";
import { default as React, useContext, useEffect } from "react";
import { AntSelect } from "shared-resources/components";
import { TicketCategorizationFilters } from ".";
import BAReportTimeRangeFilter from "./BAReportTimeRangeFilter";
import BAWidgetAcrossFilters from "./BAWidgetAcrossFilters";
import { ITEM_TEST_ID } from "./Constants";
import DisplayFormatFilter from "./display-format-filter/DisplayFormatFilter";
import { DISPLAY_FORMAT_FILTER_KEY } from "./display-format-filter/helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface TicketcategorizationFiltersWrapperProps {
  metaData: any;
  reportType: string;
  filters: any;
  onMetadataChange?: (value: any, type: any, reportType?: String) => void;
  onTimeFilterValueChange?: (value: any, type?: any, rangeType?: string) => void;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  dashboardMetadata?: any;
}

const TicketCategorizationFiltersWrapper: React.FC<TicketcategorizationFiltersWrapperProps> = (
  props: TicketcategorizationFiltersWrapperProps
) => {
  const { reportType, filters, metaData, onFilterValueChange, onMetadataChange } = props;
  const { apiData: ticketCategorizationData } = useTicketCategorizationFilters(reportType, [reportType]);
  const showDefaultScheme = getWidgetConstant(reportType, DefaultKeyTypes.DEFAULT_SCHEME_KEY);
  const ticketCategorizationSchemesValue = get(filters, [TICKET_CATEGORIZATION_SCHEMES_KEY], undefined);
  const application = getWidgetConstant(reportType, "application");
  const { isVisibleOnTab } = useContext(WidgetTabsContext);

  useEffect(() => {
    const defaultScheme = ticketCategorizationData.filter((data: any) => data?.default_scheme)[0];
    if (showDefaultScheme && defaultScheme && !ticketCategorizationSchemesValue) {
      onFilterValueChange(defaultScheme.id, TICKET_CATEGORIZATION_SCHEMES_KEY);
    }
  }, [ticketCategorizationData, ticketCategorizationSchemesValue]);

  return (
    <>
      {getWidgetConstant(reportType, SUPPORT_TICKET_CATEGORIZATION_FILTERS) &&
        isVisibleOnTab(getWidgetConstant(reportType, SHOW_PROFILE_INSIDE_TAB, WIDGET_CONFIGURATION_KEYS.FILTERS)) && (
          <TicketCategorizationFilters
            filters={filters}
            onFilterValueChange={onFilterValueChange}
            reportType={reportType}
            apiData={ticketCategorizationData || []}
            dashboardMetadata={props.dashboardMetadata}
          />
        )}
      {getWidgetConstant(reportType, SUPPORT_TICKET_CATEGORIZATION_UNIT_FILTERS) &&
        isVisibleOnTab(
          getWidgetConstant(reportType, SHOW_EFFORT_UNIT_INSIDE_TAB, WIDGET_CONFIGURATION_KEYS.METRICS)
        ) && (
          <Form.Item
            key="UNIT_FILTERS"
            label={"Effort Unit"}
            data-filterselectornamekey={`${ITEM_TEST_ID}-effort-unit`}
            data-filtervaluesnamekey={`${ITEM_TEST_ID}-effort-unit`}>
            <AntSelect
              dropdownTestingKey={`${ITEM_TEST_ID}-effort-unit_dropdown`}
              showArrow={true}
              value={get(
                filters,
                [TICKET_CATEGORIZATION_UNIT_FILTER_KEY],
                getWidgetConstant(reportType, DefaultKeyTypes.DEFAULT_EFFORT_UNIT)
              )}
              options={
                application === IntegrationTypes.AZURE
                  ? azureEffortInvestmentUnitFilterOptions
                  : ticketCategorizationUnitFilterOptions
              }
              onChange={(value: any) => onFilterValueChange(value, TICKET_CATEGORIZATION_UNIT_FILTER_KEY)}
            />
          </Form.Item>
        )}
      {getWidgetConstant(reportType, SUPPORT_ACTIVE_WORK_UNIT_FILTERS) &&
        isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
          <Form.Item
            key="active-work-filters"
            label={"Active Work Unit"}
            data-filterselectornamekey={`${ITEM_TEST_ID}-active-work-unit`}
            data-filtervaluesnamekey={`${ITEM_TEST_ID}-active-work-unit`}>
            <AntSelect
              dropdownTestingKey={`${ITEM_TEST_ID}-active-work_dropdown`}
              showArrow={true}
              value={get(filters, [ACTIVE_WORK_UNIT_FILTER_KEY])}
              options={application === IntegrationTypes.AZURE ? azureActiveWorkUnitOptions : jiraActiveWorkUnitOptions}
              onChange={(value: any) => onFilterValueChange(value, ACTIVE_WORK_UNIT_FILTER_KEY)}
            />
          </Form.Item>
        )}
      {getWidgetConstant(reportType, SUPPORT_DISPLAY_FORMAT_FILTERS) &&
        isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
          <DisplayFormatFilter
            currentValue={get(metaData, [DISPLAY_FORMAT_FILTER_KEY], "")}
            onFilterChange={onMetadataChange}
          />
        )}
      {getWidgetConstant(reportType, SUPPORT_TIME_RANGE_FILTER) &&
        isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) && (
          <BAReportTimeRangeFilter
            filters={filters}
            config={getWidgetConstant(reportType, BA_WIDGET_TIME_RANGE_FILTER_CONFIG)}
            onFilterValueChange={onFilterValueChange}
          />
        )}
      {getWidgetConstant(reportType, SUPPORT_TREND_INTERVAL) &&
        isVisibleOnTab(
          getWidgetConstant(reportType, SHOW_SAMPLE_INTERVAL_INSIDE_TAB, WIDGET_CONFIGURATION_KEYS.FILTERS)
        ) && (
          <Form.Item
            label="Sample Interval"
            key="BA Report Trend Interval"
            data-filterselectornamekey={`${ITEM_TEST_ID}-ba-sample-interval`}
            data-filtervaluesnamekey={`${ITEM_TEST_ID}-ba-sample-interval`}
            required>
            <AntSelect
              dropdownTestingKey={`${ITEM_TEST_ID}-ba-sample-interval_dropdown`}
              showArrow={true}
              selectLabel={"Sample Interval"}
              value={get(filters, ["interval"], undefined)}
              options={getWidgetConstant(reportType, INTERVAL_OPTIONS)}
              onChange={(value: any) => onFilterValueChange(value, "interval")}
            />
          </Form.Item>
        )}
      {getWidgetConstant(reportType, SUPPORT_CATEGORY_EPIC_ACROSS_FILTER) &&
        isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.AGGREGATIONS) && (
          <BAWidgetAcrossFilters filters={filters} onFilterValueChange={onFilterValueChange} />
        )}
    </>
  );
};

export default TicketCategorizationFiltersWrapper;
