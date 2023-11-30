import { Form, Popover } from "antd";
import { apiCall, useApi } from "custom-hooks/useApi";
import { DynamicGraphFilter } from "dashboard/constants/applications/levelops.application";
import { filter, find, get, map, uniqBy } from "lodash";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { formClear } from "reduxConfigs/actions/formActions";
import { getIdsMap } from "reduxConfigs/actions/restapi/genericIdsMap.actions";
import { CustomFormItemLabel } from "shared-resources/components";
import { baseColumnConfig } from "utils/base-table-config";
import { v1 as uuid } from "uuid";

import { buildSpecificDataForFilterApiCalling } from "../container/helper";
import CustomTagSelectFilter from "./CustomTagSelectFilter";
import PopupDynamicPaginatedTable from "./PopupDynamicPaginatedTable";
import { FileReportRootURIs } from "dashboard/constants/helper";
import APIFilterManyOptions from "dashboard/graph-filters/components/APIFilterManyOptions/APIFilterManyOptions";

interface DynamicGraphFilterComponentProps {
  api: apiCall;
  supportedFilter: DynamicGraphFilter;
  filters: any;
  activePopkey: string | undefined;
  handleActivePopkey: (key: string | undefined) => void;
  onFilterValueChange: (value: any, type?: any) => void;
  labelInValue?: boolean;
}

const DynamicGraphFilterComponent: React.FC<DynamicGraphFilterComponentProps> = (
  props: DynamicGraphFilterComponentProps
) => {
  const { api, supportedFilter, filters, onFilterValueChange, activePopkey, handleActivePopkey } = props;

  const [apiSelectState, setApiSelectState] = useState<{ [x: string]: any }>({});
  const [filtersApiLoading, setFiltersApiLoading] = useState<boolean>(false);
  const [filtersApiData, setFiltersApiData] = useState<any>({});

  const [paginatedFilters, setPaginatedFilters] = useState<any>({ page: 1, pageSize: 10000, searchValue: "" });

  const getApi = useMemo(
    () => ({
      ...api,
      filters: {
        page: paginatedFilters.page - 1,
        page_size: paginatedFilters.pageSize,
        filter: {
          ...(api.filters || {}),
          partial: (paginatedFilters?.searchValue || "").length
            ? { [supportedFilter.searchField || "name"]: paginatedFilters.searchValue }
            : {}
        }
      }
    }),
    [api, paginatedFilters]
  );

  const [loading, apiData, apisMetaData] = useApi([getApi], [paginatedFilters]);

  const formName = useRef<string>();

  const dispatch = useDispatch();
  const formState = useSelector((state: any) => state.formReducer);

  const apisForCalling = useMemo(
    () => buildSpecificDataForFilterApiCalling(props.filters, supportedFilter.filterField),
    [filters, supportedFilter]
  );

  useEffect(() => {
    if (Object.keys(apisForCalling).length && !filtersApiLoading && !formName.current?.length) {
      const name = `graph_filters_${api.id}_${uuid()}`;
      dispatch(getIdsMap(name, apisForCalling as any));
      setFiltersApiLoading(true);
      formName.current = name;

      return () => {
        dispatch(formClear(name));
      };
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (filtersApiLoading) {
      const data = formState[formName.current || ""] || {};
      setFiltersApiData(data);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [formState]);

  useEffect(() => {
    if (Object.keys(filtersApiData).length) {
      let selectedValues: any = {};
      Object.keys(filtersApiData).forEach(key => {
        const data = get(filtersApiData, [key], []);
        if (key === "questionnaire_template_ids") {
          selectedValues = {
            ...selectedValues,
            questionnaire_template_id: data.map((res: any) => ({ value: res.id, label: res.name }))
          };
        }
        if (key === "product_ids") {
          selectedValues = {
            ...selectedValues,
            product_ids: data.map((res: any) => ({ value: res.id, label: res.name }))
          };
        }
        if (key === "tag_ids") {
          selectedValues = {
            ...selectedValues,
            tags: data.map((res: any) => ({ value: res.id, label: res.name }))
          };
        }

        if (key === "user_ids") {
          selectedValues = {
            ...selectedValues,
            assignees: data.map((res: any) => ({ value: res.id, label: res.email }))
          };
        }
        if (key === "state_ids") {
          selectedValues = {
            ...selectedValues,
            states: data.map((res: any) => ({ value: res.id, label: res.name }))
          };
        }
      });
      setApiSelectState(selectedValues);
      setFiltersApiLoading(false);
    }
  }, [filtersApiData]);

  const valueKey = useMemo(() => {
    let valueKey = "";
    if (supportedFilter.valueKey) {
      valueKey = supportedFilter.valueKey;
    } else {
      valueKey =
        supportedFilter.uri === "users"
          ? "email"
          : ["jira_salesforce_files_report", "jira_zendesk_files_report"].includes(supportedFilter.uri || "")
          ? "key"
          : "id";
    }
    return valueKey;
  }, [supportedFilter]);

  const labelKey = useMemo(() => {
    let labelKey = "";
    if (supportedFilter.labelKey) {
      labelKey = supportedFilter.labelKey;
    } else {
      labelKey =
        supportedFilter.uri === "users"
          ? "email"
          : ["jira_salesforce_files_report", "jira_zendesk_files_report"].includes(supportedFilter.uri || "")
          ? "key"
          : "name";
    }
    return labelKey;
  }, [supportedFilter]);

  const isMultiSelect = useMemo(() => supportedFilter.filterType === "apiMultiSelect", [supportedFilter]);

  const options = useMemo(() => {
    const finalOptions = uniqBy(
      [
        ...(!isMultiSelect && filters?.[supportedFilter.filterField]?.length > 0
          ? [
              {
                label: filters?.[supportedFilter.filterField] || "",
                value: filters?.[supportedFilter.filterField] || ""
              }
            ]
          : []),
        ...(apiSelectState?.[supportedFilter.filterField] || []),
        ...map((apiData as any)?.[api.id] || [], item => ({ label: item[labelKey], value: item[valueKey] }))
      ],
      "value"
    );

    return finalOptions;
  }, [apiData, apiSelectState, valueKey, labelKey]);

  const getSelectedValues = useMemo(() => {
    if (isMultiSelect) {
      return filters?.[supportedFilter.filterField] || null;
    }
    return [filters?.[supportedFilter.filterField] || ""];
  }, [isMultiSelect, filters, supportedFilter]);

  return (
    <APIFilterManyOptions
      data_testId={"dynamic-graph-filter-component-apifiltermanyoptions"}
      APIFiltersProps={{
        isCustom: true,
        activePopkey,
        handleActivePopkey,
        handlePartialValueChange: () => {},
        handleFilterValueChange: props.onFilterValueChange
      }}
      apiFilterProps={{
        dataKey: supportedFilter.filterField,
        selectName: supportedFilter.label,
        value: getSelectedValues,
        options,
        switchValue: false, // Exclude not supported for levelops widgets.
        partialValue: undefined, // Partial values not supported for levelops widgets
        withSwitchConfig: undefined // Same as above.
      }}
    />
  );
};

export default React.memo(DynamicGraphFilterComponent);
