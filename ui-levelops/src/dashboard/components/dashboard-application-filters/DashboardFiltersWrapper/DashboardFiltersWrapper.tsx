import React, { useMemo, useState } from "react";
import "./DashboardFiltersWrapper.scss";
import { AntText } from "shared-resources/components";
import { getJiraFilterReport } from "../helper";
import { DASHBOARD_FILTER_INFO_TEXT } from "../constants";
import { get, unset, cloneDeep } from "lodash";
import { valuesToFilters } from "../../../constants/constants";
import { removeFilterKey } from "../AddFiltersComponent/helpers";
import AddFiltersComponent from "../AddFiltersComponent/AddFiltersComponent";
import { buildExcludeQuery, buildWidgetQuery, buildPartialQuery } from "configurable-dashboard/helpers/queryHelper";

interface DashboardFiltersWrapperProps {
  integrationIds: Array<any>;
  filters: any;
  setJiraOrFilters: any;
  orderedFilters: string[];
  setJiraOrOrderedFilters: any;
}

const DashboardFiltersWrapper: React.FC<DashboardFiltersWrapperProps> = ({
  integrationIds,
  filters,
  setJiraOrFilters,
  orderedFilters,
  setJiraOrOrderedFilters
}) => {
  const [showMore, setShowMore] = useState<boolean>(false);
  const [partialFiltersErrors, setPartialFiltersErrors] = useState<any>({});

  const description = useMemo(() => {
    return !showMore ? DASHBOARD_FILTER_INFO_TEXT.substr(0, 94) : DASHBOARD_FILTER_INFO_TEXT;
  }, [showMore]);

  const onJiraOrFilterChange = (value: any, type?: any, exclude?: boolean) => {
    setJiraOrFilters((filters: any) => buildWidgetQuery(filters, value, type, exclude));
    setPartialFiltersErrors((prev: any) => ({
      ...prev,
      [type]: undefined
    }));
  };

  const handleJiraOrExcludeFilter = (key: string, value: boolean) => {
    setJiraOrFilters((filters: any) => {
      const _filters = { ...filters };
      const filterKey = get(valuesToFilters, [key], key);
      unset(_filters, filterKey);
      return buildExcludeQuery(filters, key, value);
    });
    setPartialFiltersErrors((prev: any) => ({
      ...prev,
      [key]: undefined
    }));
  };

  const handleJiraOrPartialFiltersChange = (key: string, value: any) => {
    const latestFilters = cloneDeep(filters);
    const { filters: newFilters, error } = buildPartialQuery(latestFilters || {}, key, value, "");
    if (!!error) {
      setPartialFiltersErrors((prev: any) => ({ ...prev, [key]: error }));
    } else {
      setPartialFiltersErrors((prev: any) => ({
        ...prev,
        [key]: undefined
      }));
      const initialValue = !value ? { [(valuesToFilters as any)[key]]: [] } : {};
      setJiraOrFilters({ ...newFilters, ...initialValue });
    }
  };

  const onJiraOrFilterRemoved = (key: string) => {
    const _filters = removeFilterKey(filters, key);
    const _range_filter_choice = cloneDeep(filters?.metadata?.range_filter_choice || {});
    delete _range_filter_choice[key];
    setJiraOrFilters({
      ..._filters,
      metadata: {
        ...(filters.metadata || {}),
        range_filter_choice: _range_filter_choice
      }
    });
    setJiraOrOrderedFilters((keys: string[]) => keys.filter(_key => _key !== key));
  };

  const handleJiraOrTimeRangeTypeChange = (key: string, value: any) => {
    setJiraOrFilters((filters: any) => ({
      ...(filters || {}),
      metadata: {
        ...(filters.metadata || {}),
        range_filter_choice: {
          ...(filters?.metadata?.range_filter_choice || {}),
          [key]: value
        }
      }
    }));
  };

  const handleJiraOrTimeRangeFilterValueChange = (value: any, type?: any, rangeType?: string, isCustom = false) => {
    setJiraOrFilters((filters: any) => {
      if (!isCustom) {
        return {
          ...(filters || {}),
          [type]: value.absolute,
          metadata: {
            ...(filters?.metadata || {}),
            range_filter_choice: {
              ...(filters?.metadata?.range_filter_choice || {}),
              [type]: { type: value.type, relative: value.relative }
            }
          }
        };
      } else {
        return {
          ...(filters || {}),
          ["custom_fields"]: {
            ...(filters?.custom_fields || {}),
            [type]: value.absolute
          },
          metadata: {
            ...(filters?.metadata || {}),
            range_filter_choice: {
              ...(filters?.metadata?.range_filter_choice || {}),
              [type]: { type: value.type, relative: value.relative }
            }
          }
        };
      }
    });
  };

  return (
    <div className={"dashboard_filters_wrapper"}>
      <span>
        <AntText>
          {description.split("ANY")[0]} <AntText strong>ANY</AntText> {description.split("ANY")[1]}
        </AntText>
        <AntText className={"text-primary"} onClick={() => setShowMore(!showMore)}>
          {!showMore ? "Learn more" : " Show less"}
        </AntText>
      </span>
      <AddFiltersComponent
        report={getJiraFilterReport()}
        integrationIds={integrationIds}
        filters={filters}
        hideHeader
        onFilterValueChange={onJiraOrFilterChange}
        onExcludeFilterChange={handleJiraOrExcludeFilter}
        handlePartialValueChange={handleJiraOrPartialFiltersChange}
        onFilterRemoved={onJiraOrFilterRemoved}
        handleTimeRangeFilterValueChange={handleJiraOrTimeRangeFilterValueChange}
        handleTimeRangeTypeChange={handleJiraOrTimeRangeTypeChange}
        orderedFilters={orderedFilters}
        setOrderedFilters={setJiraOrOrderedFilters}
        additionalId={"jiraFilters"}
        partialFiltersErrors={partialFiltersErrors}
      />
    </div>
  );
};

export default DashboardFiltersWrapper;
