import React, { useEffect, useState, useContext, useCallback } from "react";
import { Form, TimePicker } from "antd";
import { AntSelect } from "../../../shared-resources/components";
import { valuesToFilters, USE_PAGINATED_FILTERS_THRESHOLD } from "../../constants/constants";
import widgetConstants from "../../constants/widgetConstants";
import { get } from "lodash";
import APIFilterManyOptions from "./APIFilterManyOptions/APIFilterManyOptions";
import { useFilterClassnames } from "./utils/useFilterClassnames";
import { WidgetTabsContext } from "../../pages/context";
import { WIDGET_CONFIGURATION_KEYS } from "../../../constants/widgets";
import { stringSortingComparator } from "./sort.helper";
import {
  ITEM_TEST_ID,
  pagerdutyTimeToResolveAxisOptions,
  pagerdutyTimeToAcknowledgeAxisOptions,
  pagerdutyTimeToResolveMetricsOptions,
  pagerdutyTimeToResolveSampleInterval
} from "./Constants";
import FEBasedFiltersContainer from "../containers/FEBasedFilters/FEBasedFilters.container";
import { FEBasedFilterMap } from "../../dashboard-types/FEBasedFilterConfig.type";
import { allowWidgetDataSorting } from "../../helpers/helper";
import WidgetDataSortFilter from "./WidgetDataSortFilter";
import { PAGERDUTY_REPORT } from "../../constants/applications/names";
import { WEEK_DATE_FORMAT, WEEK_FORMAT_CONFIG_OPTIONS } from "constants/time.constants";

import moment from "moment";

interface PagerdutyFiltersProps {
  data: Array<any>;
  onFilterValueChange: (value: any, type?: any) => void;
  filters: any;
  reportType: string;
  acrossOptions: Array<any>;
  onAggregationAcrossSelection?: (value: any) => void;
  onMetadataChange?: (value: any, type: any, reportType?: String) => void;
  onTimeFilterValueChange?: (value: any, type?: any, rangeType?: string, isCustom?: boolean) => void;
  onTimeRangeTypeChange?: (key: string, value: any) => void;
  metaData?: any;
  dashboardMetaData?: any;
  isMultiTimeSeriesReport?: boolean;
  isCompositeChild?: boolean;
}

const PagerdutyFilters: React.FC<PagerdutyFiltersProps> = (props: PagerdutyFiltersProps) => {
  const { isVisibleOnTab } = useContext(WidgetTabsContext);
  // const isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) = tabType === WIDGET_CONFIGURATION_KEYS.METRICS;
  // const isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) = tabType === WIDGET_CONFIGURATION_KEYS.FILTERS;
  // const isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.AGGREGATIONS) = tabType === WIDGET_CONFIGURATION_KEYS.AGGREGATIONS;

  const [officeHours, setOfficeHours] = useState<any>({});
  const { data, onFilterValueChange, filters, reportType } = props;
  const [activePopKey, setActivePopKey] = useState<string | undefined>();

  const { innerClassName, outerClassName } = useFilterClassnames({
    activePopKey: !!activePopKey
  });

  const mapping = get(widgetConstants, [reportType, "filterOptionMap"], undefined);
  const valuesToFilterMapping = get(widgetConstants, [reportType, "valuesToFilters"], valuesToFilters);

  const supportedFilters =
    reportType === "pagerduty_release_incidents"
      ? // @ts-ignore
        get(widgetConstants, [reportType, "supported_filters"], []).reduce((acc: [], obj: any) => {
          // @ts-ignore
          acc.push(...(obj.values || []));
          return acc;
        }, [])
      : get(widgetConstants, [reportType, "supported_filters", "values"], []);

  const getFilterOptions = (data: any, name: any = null): Array<any> => {
    if (!data) {
      return [];
    }
    if (!Array.isArray(data)) {
      return [];
    }
    if (name === "cicd_job_id") {
      return data
        .filter((item: any) => item)
        .map((item: any) => ({
          label: item.key,
          value: item.cicd_job_id
        }));
    }
    if (name === "user_id" || name === "pd_service") {
      return data
        .filter((item: any) => item)
        .map((item: any) => ({
          label: item.name,
          value: item.id
        }));
    }
    return data
      .filter((item: any) => item)
      .map((item: any) => ({
        label: (item || "").toUpperCase(),
        value: item
      }));
  };

  const getMode = (item: { [key: string]: string }) => {
    const filter = Object.keys(item)[0];
    const singleSelect = get(widgetConstants, [reportType, "singleSelectFilters"], []);
    if (singleSelect.includes(filter)) {
      return "single";
    }
    return "multiple";
  };

  const getRequired = (item: { [key: string]: string }) => {
    const filter = Object.keys(item)[0];
    const requiredFilters = get(widgetConstants, [reportType, "requiredFilters"], []);
    return !!requiredFilters.includes(filter);
  };

  useEffect(() => {
    if (!!filters?.office_hours?.to && !!filters?.office_hours?.from) {
      setOfficeHours(filters.office_hours);
    }

    if (!!filters?.office_hours?.$to && !!filters?.office_hours?.$from) {
      setOfficeHours(filters.office_hours);
    }
  }, []);

  const timeSelected = (type: string, time: any, timeString: any) => {
    setOfficeHours((state: any) => ({ ...state, [type]: timeString }));

    const _officeHours = { ...officeHours, [type]: timeString };

    if (reportType === PAGERDUTY_REPORT.RESPONSE_REPORTS) {
      if (
        _officeHours?.$to &&
        _officeHours?.$from &&
        (filters?.office_hours?.$to !== _officeHours?.$to || filters?.office_hours?.$from !== _officeHours?.$from)
      ) {
        onFilterValueChange(_officeHours, "office_hours");
      }

      if (Object.keys(_officeHours).length > 1 && (!_officeHours?.$to || !_officeHours?.$from)) {
        onFilterValueChange({}, "office_hours");
      }
    } else {
      if (
        _officeHours?.to &&
        _officeHours?.from &&
        (filters?.office_hours?.to !== _officeHours?.to || filters?.office_hours?.from !== _officeHours?.from)
      ) {
        onFilterValueChange(_officeHours, "office_hours");
      }

      if (Object.keys(_officeHours).length > 1 && (!_officeHours?.to || !_officeHours?.from)) {
        onFilterValueChange({}, "office_hours");
      }
    }
  };

  const filterFEBasedFilters = useCallback((filtersConfig: FEBasedFilterMap) => {
    return filtersConfig;
  }, []);

  // @ts-ignore
  return (
    <div data-testid="pagerduty-filters-component" className={outerClassName}>
      <div className={innerClassName}>
        {
          // @ts-ignore
          widgetConstants[reportType]?.xaxis !== false && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.AGGREGATIONS) && (
            <Form.Item
              label={"X-Axis"}
              data-filterselectornamekey={`${ITEM_TEST_ID}-pagerduty-xaxis`}
              data-filtervaluesnamekey={`${ITEM_TEST_ID}-pagerduty-xaxis`}
              required>
              <AntSelect
                dropdownTestingKey={`${ITEM_TEST_ID}-pagerduty-xaxis_dropdown`}
                options={props.acrossOptions}
                value={filters.across}
                onSelect={(value: any) =>
                  props.onAggregationAcrossSelection && props.onAggregationAcrossSelection(value)
                }
              />
            </Form.Item>
          )
        }

        {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) &&
          data.map((item: any, index) => {
            if (!supportedFilters.includes(Object.keys(item)[0])) {
              return null;
            }

            if (item) {
              const name = Object.keys(item)[0];
              const selectName = get(mapping, name, name.replace(/_/g, " "));
              const options = getFilterOptions(item[Object.keys(item)[0]], Object.keys(item)[0]);
              const dataKey = valuesToFilterMapping[name as keyof typeof valuesToFilters];
              const selectedValues = filters[dataKey];
              const selectMode = getMode(item);

              const shouldUsePaginatedFilters = options.length > USE_PAGINATED_FILTERS_THRESHOLD;

              if (shouldUsePaginatedFilters) {
                return (
                  <APIFilterManyOptions
                    key={`${dataKey}-${index}`}
                    data_testId={"pager-duty-filters-apifiltermanyoptions"}
                    singleSelect={selectMode === "single"}
                    APIFiltersProps={{
                      isCustom: true,
                      activePopkey: activePopKey,
                      handleActivePopkey: setActivePopKey,
                      handlePartialValueChange: () => {},
                      handleFilterValueChange: props.onFilterValueChange
                    }}
                    apiFilterProps={{
                      dataKey: dataKey,
                      selectName: selectName,
                      value: selectedValues,
                      options,
                      switchValue: false, // Exclude not supported for levelops widgets.
                      partialValue: undefined, // Partial values not supported for levelops widgets
                      withSwitchConfig: undefined // Same as above.
                    }}
                  />
                );
              }
              const identificationKey = `${ITEM_TEST_ID}-${(selectName || "").split(" ").join("-")}`;
              return (
                <Form.Item
                  key={`${dataKey}-${index}`}
                  label={selectName}
                  required={getRequired(item)}
                  data-filterselectornamekey={`${identificationKey}`}
                  data-filtervaluesnamekey={`${identificationKey}`}>
                  <AntSelect
                    dropdownTestingKey={`${identificationKey}_dropdown`}
                    value={selectedValues}
                    mode={selectMode}
                    options={options}
                    onChange={(value: any, options: any) => onFilterValueChange(value, Object.keys(item)[0])}
                  />
                </Form.Item>
              );
            }
          })}
        {["pagerduty_hotspot_report", PAGERDUTY_REPORT.RESPONSE_REPORTS].includes(reportType) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
            <Form.Item
              label={"Stacks"}
              data-filterselectornamekey={`${ITEM_TEST_ID}-pagerduty_stacks`}
              data-filtervaluesnamekey={`${ITEM_TEST_ID}-pagerduty_stacks`}>
              <AntSelect
                value={filters.stacks}
                dropdownTestingKey={`${ITEM_TEST_ID}-pagerduty_stacks_dropdown`}
                // mode={"multiple"}
                // @ts-ignore
                options={(widgetConstants[reportType]?.stack_filters || [])
                  .filter((opt: string) => opt !== filters?.across)
                  .map((opt: string) => ({
                    value: opt,
                    label: get(mapping, opt, opt.toUpperCase().replace("_", " "))
                  }))
                  .sort(stringSortingComparator("label"))}
                onChange={(value: any, options: any) => onFilterValueChange([value], "stacks")}
              />
            </Form.Item>
          )}
        {reportType === "pagerduty_after_hours" && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) && (
          <Form.Item key={`${ITEM_TEST_ID}-pagerduty_office_hours`} label={"Office hours"}>
            <TimePicker
              value={officeHours?.from ? moment(officeHours?.from, "HH:mm") : undefined}
              style={{ width: "48%", marginRight: "4%" }}
              format="HH:mm"
              placeholder={"Select from"}
              onChange={(time: any, timeString: any) => timeSelected("from", time, timeString)}
            />
            <TimePicker
              value={officeHours?.to ? moment(officeHours?.to, "HH:mm") : undefined}
              style={{ width: "48%" }}
              format="HH:mm"
              placeholder={"Select to"}
              onChange={(time: any, timeString: any) => timeSelected("to", time, timeString)}
            />
          </Form.Item>
        )}
        {reportType === PAGERDUTY_REPORT.RESPONSE_REPORTS && (
          <>
            {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) && (
              <Form.Item key={`${ITEM_TEST_ID}-pagerduty_office_hours`} label={"Office hours"}>
                <TimePicker
                  value={officeHours?.$from ? moment(officeHours?.$from, "HH:mm") : undefined}
                  style={{ width: "48%", marginRight: "4%" }}
                  format="HH:mm"
                  placeholder={"Select from"}
                  onChange={(time: any, timeString: any) => timeSelected("$from", time, timeString)}
                />
                <TimePicker
                  value={officeHours?.$to ? moment(officeHours?.$to, "HH:mm") : undefined}
                  style={{ width: "48%" }}
                  format="HH:mm"
                  placeholder={"Select to"}
                  onChange={(time: any, timeString: any) => timeSelected("$to", time, timeString)}
                />
              </Form.Item>
            )}
            {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
              <>
                <Form.Item key={`${ITEM_TEST_ID}-metric`} label={"Metric"}>
                  <AntSelect
                    showArrow={true}
                    value={get(props.filters, "metric", "resolve")}
                    options={pagerdutyTimeToResolveMetricsOptions}
                    mode={"single"}
                    onChange={(value: any, options: any) => props.onFilterValueChange(value, "metric")}
                  />
                </Form.Item>
                <Form.Item key={`${ITEM_TEST_ID}-leftYAxis`} label={"Left Y Axis"}>
                  <AntSelect
                    showArrow={true}
                    value={get(props.metaData, "leftYAxis", "mean")}
                    options={
                      filters?.metric === "resolve"
                        ? pagerdutyTimeToResolveAxisOptions
                        : pagerdutyTimeToAcknowledgeAxisOptions
                    }
                    mode={"single"}
                    onChange={(value: any, options: any) => props.onMetadataChange?.(value, "leftYAxis")}
                  />
                </Form.Item>
              </>
            )}
            {["alert_resolved_at", "incident_resolved_at", "incident_created_at", "alert_created_at"].includes(
              filters?.across
            ) &&
              isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
                <>
                  <Form.Item key={`${ITEM_TEST_ID}-sample-interval`} label={"Sample Interval"}>
                    <AntSelect
                      showArrow={true}
                      value={get(props.filters, ["interval"], "day")}
                      options={pagerdutyTimeToResolveSampleInterval}
                      mode={"single"}
                      onChange={(value: any, options: any) => props.onFilterValueChange(value, "interval")}
                    />
                  </Form.Item>
                  {get(props.filters, ["interval"], "day") === "week" && (
                    <Form.Item key={`${ITEM_TEST_ID}-weekdate-format`} label={"Week Date Format"}>
                      <AntSelect
                        showArrow={true}
                        value={get(props.metaData, ["weekdate_format"], WEEK_DATE_FORMAT.DATE)}
                        options={WEEK_FORMAT_CONFIG_OPTIONS}
                        mode={"single"}
                        onChange={(value: any, options: any) => props.onMetadataChange?.(value, "weekdate_format")}
                      />
                    </Form.Item>
                  )}
                </>
              )}
          </>
        )}
        {allowWidgetDataSorting(reportType, filters) &&
          isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) &&
          !props.isMultiTimeSeriesReport &&
          !props.isCompositeChild && (
            <WidgetDataSortFilter filters={filters} onFilterValueChange={onFilterValueChange} />
          )}
        <FEBasedFiltersContainer
          filters={filters}
          metadata={props.metaData}
          report={reportType}
          onFilterValueChange={onFilterValueChange}
          onTimeRangeTypeChange={props.onTimeRangeTypeChange}
          onTimeFilterValueChange={props.onTimeFilterValueChange}
          filterFEBasedFilters={filterFEBasedFilters}
          onMetadataChange={props.onMetadataChange}
          dashboardMetaData={props.dashboardMetaData}
        />
      </div>
    </div>
  );
};

export default PagerdutyFilters;
