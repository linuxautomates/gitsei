import { useAPIFilter } from "custom-hooks/useAPIFilter";
import {
  BA_EFFORT_ATTRIBUTION_BE_KEY,
  BA_HISTORICAL_ASSIGNEES_STATUS_BE_KEY,
  BA_IN_PROGRESS_STATUS_BE_KEY,
  EffortAttributionOptions,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { EffortUnitType } from "dashboard/constants/enums/jira-ba-reports.enum";
import { get } from "lodash";
import { ApiDropDownData, DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo } from "react";
import { AddWidgetFilterContainerProps } from "./AddWidgetFilter/AddWidgetFilter.container";
import UniversalSelectFilterWrapper from "./GenericFilterComponents/UniversalSelectFilterWrapper";

interface FilterAssigneeByStatusFilterProps extends AddWidgetFilterContainerProps {
  filterProps: LevelOpsFilter;
  handleRemoveFilter: (key: string) => void;
}

const BAStatusBasedFilterContainer: React.FC<FilterAssigneeByStatusFilterProps> = (
  props: FilterAssigneeByStatusFilterProps
) => {
  const { filterProps } = props;
  const { filterMetaData } = filterProps;

  const getPayload: (args: Record<string, any>) => Record<string, any> = props => {
    const { integrationIds } = props;
    return {
      fields: ["status"],
      filter: {
        integration_ids: integrationIds
      },
      integration_ids: integrationIds
    };
  };

  const { data, loading } = useAPIFilter(
    {
      uri: "jira_filter_values",
      payload: getPayload,
      integration_ids: get(filterMetaData, ["integration_ids"], undefined),
      specialKey: "status"
    } as ApiDropDownData,
    {},
    [filterMetaData]
  );

  const getStatusesOptions = useMemo(() => {
    if (!loading && data && data.length) {
      const statusFilterData = data[0];
      const statusKey = Object.keys(statusFilterData)[0];
      const values = statusFilterData[statusKey] ?? [];
      return values.map((item: any) => ({ label: item?.key, value: item?.key }));
    }
    return [];
  }, [data, loading]);

  const renderFilter = useMemo(() => {
    const effortAttributionValue = get(
      filterProps,
      ["allFilters", BA_EFFORT_ATTRIBUTION_BE_KEY],
      EffortAttributionOptions.CURRENT_ASSIGNEE
    );

    const effortUnitValue = get(
      filterProps,
      ["allFilters", TICKET_CATEGORIZATION_UNIT_FILTER_KEY],
      EffortUnitType.TICKETS_REPORT
    );

    const filterBEKey = get(filterProps, ["beKey"], "");

    if (
      filterBEKey === BA_HISTORICAL_ASSIGNEES_STATUS_BE_KEY &&
      effortAttributionValue !== EffortAttributionOptions.CURRENT_ASSIGNEE_AND_PREV_ASSIGNEE
    ) {
      return null;
    }

    if (filterBEKey === BA_IN_PROGRESS_STATUS_BE_KEY && effortUnitValue !== EffortUnitType.TICKET_TIME_SPENT) {
      return null;
    }

    return React.createElement(UniversalSelectFilterWrapper, {
      ...props,
      filterProps: {
        ...filterProps,
        filterMetaData: {
          ...filterMetaData,
          options: getStatusesOptions
        } as DropDownData
      }
    });
  }, [props, getStatusesOptions]);

  return renderFilter;
};

export default BAStatusBasedFilterContainer;
