import React, { useMemo, useState, useEffect } from "react";
import { difference, get, uniqBy } from "lodash";
import { useDispatch } from "react-redux";
import { azureTeamsFilterValuesGet } from "reduxConfigs/actions/restapi";
import { azureFilterValueIdSelector } from "reduxConfigs/selectors/azure.selector";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { optionType } from "dashboard/dashboard-types/common-types";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import AzureCodeAreaFilter from "./AzureCodeAreaFilter";

interface AzureCodeAreaFilterContainerProps {
  filters: any;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean, addToMetaData?: any) => void;
  handleRemoveFilter: any;
  filterProps: LevelOpsFilter;
}

const AZURE_CODE_AREA_FILTER_ID = "AZURE_CODE_AREA_FILTER_ID";
const AzureCodeAreaFilterContainer: React.FC<AzureCodeAreaFilterContainerProps> = ({
  onFilterValueChange,
  filters,
  handleRemoveFilter,
  filterProps
}) => {
  const dispatch = useDispatch();
  const { label, apiFilterProps, beKey, parentKey } = filterProps;
  const [azureAreaOptions, setAzureAreaOptions] = useState<optionType[] | undefined>(undefined);
  const [loading, setLoading] = useState<boolean>(true);
  const azureTeamsFilterOptionState = useParamSelector(azureFilterValueIdSelector, { id: AZURE_CODE_AREA_FILTER_ID });

  useEffect(() => {
    const apidata = get(azureTeamsFilterOptionState, ["data", "records"], []);
    if (!apidata.length) {
      dispatch(
        azureTeamsFilterValuesGet(
          {
            fields: ["code_area"],
            filter: {
              integration_ids: get(filters, ["integration_ids"], [])
            }
          },
          AZURE_CODE_AREA_FILTER_ID
        )
      );
    }
  }, []);
  const onRemoveFilter = () => {
    return handleRemoveFilter?.(beKey, parentKey);
  };
  useEffect(() => {
    if (loading) {
      const { loading: apiLoading, error } = azureTeamsFilterOptionState;
      if (!apiLoading && !error) {
        const apidata: any[] = get(azureTeamsFilterOptionState, ["data", "records"], []);
        if (apidata.length > 0) {
          const codeAreaRecords = get(apidata[0], ["code_area", "records"], []).map((record: { key: string }) => {
            const { key } = record;
            let to = key.lastIndexOf("\\");
            const parentKey = to !== -1 ? key.substring(0, to) : undefined;
            const childKey = to == -1 ? key : key.substring(to + 1, key.length);
            return {
              key: childKey,
              value: key,
              parent_key: parentKey
            };
          });
          setAzureAreaOptions(codeAreaRecords);
        }
      }
      setLoading(apiLoading);
    }
  }, [azureTeamsFilterOptionState]);

  const apiFilters = useMemo(() => {
    if (apiFilterProps) {
      return apiFilterProps({
        ...filterProps,
        handleRemoveFilter: onRemoveFilter
      });
    }
    return {};
  }, [apiFilterProps, filterProps]);

  const getAzureTeamsFilterValue = useMemo(() => {
    const filterValue: any[] = get(filters, [filterProps.parentKey as string, filterProps.beKey], []);
    if (filterValue.length) {
      return filterValue.map(v => v?.child);
    }
    return [];
  }, [filters, filterProps]);

  const handleFilterValueChange = (event: string[]) => {
    const oldFilters = get(filters, [filterProps.parentKey as string], {});
    const oldFiltersCodeArea = get(oldFilters, filterProps.beKey, []).map((item: any) => item?.child);
    const removed = difference(oldFiltersCodeArea, event) as string[];
    const modifiedValue = {
      ...oldFilters,
      [filterProps.beKey]: event
        .filter((_item: string) => !_item.startsWith(removed[0]))
        .map((_item: string) => ({ child: _item }))
    };
    onFilterValueChange(modifiedValue, filterProps.parentKey);
  };

  const onCodeAreaValueChange = (event: any) => {
    let { value, checked } = event;
    const oldFilters = get(filters, [filterProps.parentKey as string], {});
    const oldFiltersCodeArea = get(oldFilters, filterProps.beKey, []);
    let newFiltersCodeArea: any = [];
    if (checked) {
      const newIncludes = (azureAreaOptions ?? [])
        .filter((v: any) => v?.value === value)
        .map(v => ({ child: v?.value }));
      newFiltersCodeArea = uniqBy([...oldFiltersCodeArea, ...newIncludes], "child");
    } else {
      newFiltersCodeArea = oldFiltersCodeArea.filter((v: any) => !v?.child?.startsWith(value));
    }
    const modifiedValue = {
      ...oldFilters,
      [filterProps.beKey]: newFiltersCodeArea
    };
    onFilterValueChange(modifiedValue, filterProps.parentKey);
  };

  return (
    <AzureCodeAreaFilter
      apiFilters={apiFilters}
      azureAreaOptions={azureAreaOptions}
      handleFilterValueChange={handleFilterValueChange}
      onCodeAreaValueChange={onCodeAreaValueChange}
      label={label}
      selectedValueForCustomTree={getAzureTeamsFilterValue}
    />
  );
};

export default AzureCodeAreaFilterContainer;
