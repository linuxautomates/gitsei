import { optionType } from "dashboard/dashboard-types/common-types";
import { get } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { azureTeamsFilterValuesGet } from "reduxConfigs/actions/restapi";
import { azureFilterValueIdSelector } from "reduxConfigs/selectors/azure.selector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { baseColumnConfig } from "utils/base-table-config";
import { stringSortingComparator } from "../../sort.helper";
import AzureTeamFilter from "./AzureTeamFilter";

interface AzureTeamFilterConatinerProps {
  filters: any;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean, addToMetaData?: any) => void;
  filterProps: LevelOpsFilter;
  handleRemoveFilter: any;
}

const AZURE_TEAM_FILTER_ID = "AZURE_TEAM_FILTER_ID";
export const AzureTeamFilterContainer: React.FC<AzureTeamFilterConatinerProps> = (
  props: AzureTeamFilterConatinerProps
) => {
  const { filterProps, filters, handleRemoveFilter, onFilterValueChange } = props;
  const dispatch = useDispatch();
  const [loading, setLoading] = useState<boolean>(true);
  const azureTeamsFilterOptionState = useParamSelector(azureFilterValueIdSelector, { id: AZURE_TEAM_FILTER_ID });
  const [activePopupKey, setActivePopupKey] = useState<boolean>(false);
  const [azureTeamsOptions, setAzureTeamsOptions] = useState<optionType[] | undefined>(undefined);
  const { label, apiFilterProps, beKey, parentKey } = filterProps;
  useEffect(() => {
    const apidata = get(azureTeamsFilterOptionState, ["data", "records"], []);
    if (!apidata.length) {
      dispatch(
        azureTeamsFilterValuesGet(
          {
            fields: ["teams"],
            filter: {
              integration_ids: get(filters, ["integration_ids"], [])
            }
          },
          AZURE_TEAM_FILTER_ID
        )
      );
      setLoading(true);
    }
  }, []);

  useEffect(() => {
    if (loading) {
      const { loading: apiLoading, error } = azureTeamsFilterOptionState;
      if (!apiLoading && !error) {
        const apidata: any[] = get(azureTeamsFilterOptionState, ["data", "records"], []);
        if (apidata.length > 0) {
          const teamsRecords = get(apidata[0], ["teams", "records"], []).map((record: { key: string }) => ({
            label: record?.key,
            value: record?.key
          }));
          setAzureTeamsOptions(teamsRecords);
        }
      }
      setLoading(apiLoading);
    }
  }, [azureTeamsFilterOptionState]);
  const onRemoveFilter = () => {
    return handleRemoveFilter?.(beKey, parentKey);
  };
  const apiFilters = useMemo(() => {
    if (apiFilterProps) {
      return apiFilterProps({
        ...filterProps,
        handleRemoveFilter: onRemoveFilter
      });
    }
    return {};
  }, [apiFilterProps, filterProps]);

  const getAzureTeamsFilterValue = (filterKey: string) => {
    const filterValue: string[] = get(filters, [filterProps.parentKey as string, filterKey], []);
    if (filterValue.length) {
      return filterValue;
    }
    return [];
  };

  const handleFilterValueChange = (value: any, filterKey: string) => {
    const modifiedValue = {
      ...get(filters, [filterProps.parentKey as string], {}),
      [filterKey]: value
    };
    onFilterValueChange(modifiedValue, filterProps.parentKey as string);
  };

  const columnConfig = useCallback(
    (key: string) => [
      {
        ...baseColumnConfig("Value", key),
        sorter: stringSortingComparator(),
        sortDirections: ["descend", "ascend"]
      }
    ],
    []
  );

  const setActiveKey = (value: boolean) => {
    setActivePopupKey(value);
  };

  return (
    <AzureTeamFilter
      label={label}
      activePopupKey={activePopupKey}
      apiFilters={apiFilters}
      azureTeamsOptions={azureTeamsOptions}
      columnConfig={columnConfig}
      filterProps={filterProps}
      handleFilterValueChange={handleFilterValueChange}
      handleRemoveFilter={handleRemoveFilter}
      selectedValue={getAzureTeamsFilterValue(filterProps.beKey)}
      setActivePopupKey={setActiveKey}
      beKey={filterProps.beKey}
      tableHeader={"Azure Teams"}
      dataKey={"Azure Teams"}
    />
  );
};
