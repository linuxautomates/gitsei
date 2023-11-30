import React, { useEffect, useState } from "react";
import { connect } from "react-redux";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { AntCol, AntRow, AntSelect } from "../../../../../components";
import { getData, loadingStatus } from "../../../../../../utils/loadingUtils";
import { Spin } from "antd";
import { capitalize } from "lodash";
import { valuesToFilters } from "../../../../../../dashboard/constants/constants";

interface StoreProps {
  widgetFilterValuesGet: (data: any) => void;
  rest_api?: any;
}

interface JiraFilterProps extends StoreProps {
  appliedFilters: Object;
  onOptionSelectEvent: (field: any, value: any) => void;
}

const JiraFilter: React.FC<JiraFilterProps> = (props: JiraFilterProps) => {
  const filterTypes = ["status", "priority", "issue_type", "assignee", "project", "component"];
  const [dataFromApi, setDataFromApi] = useState<Array<any>>([]);
  const [filtersValueFetching, setFiltersValueFetching] = useState<boolean>(false);
  const [filters, setFilters] = useState(props.appliedFilters || {});

  useEffect(() => {
    const data = {
      fields: filterTypes
    };
    props.widgetFilterValuesGet(data);
    setFiltersValueFetching(true);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // eslint-disable-next-line
  useEffect(() => {
    const { loading: apiLoading, error: apiError } = loadingStatus(props.rest_api, "widget_filter_values", "list");
    if (filtersValueFetching && !apiLoading && !apiError) {
      const data = getData(props.rest_api, "widget_filter_values", "list").records;
      if (data && data.length) {
        setDataFromApi(data);
      }
      setFiltersValueFetching(false);
    }
  });

  useEffect(() => {
    setFilters(props.appliedFilters);
  }, [props.appliedFilters]);

  const getFilterOptions = (data: any): Array<any> => {
    return data.map((item: any) => ({
      label: (item["key"] || "").toUpperCase(),
      value: item["key"]
    }));
  };

  const onOptionSelect = (field: any) => {
    return (option: any) => {
      // @ts-ignore
      const filterValue = valuesToFilters[field];

      const filter = {
        ...(filters || {}),
        [filterValue]: option
      };

      setFilters(filter);

      props.onOptionSelectEvent(filterValue, option ? option : undefined);
    };
  };

  const getFilters = () => {
    return dataFromApi.map((item: any, index) => {
      const selectName = Object.keys(item)[0].replace(/_/g, " ");
      return (
        <AntCol key={index} className="gutter-row" span={12}>
          <h5>{capitalize(selectName)}</h5>
          <AntSelect
            value={
              // @ts-ignore
              filters[valuesToFilters[Object.keys(item)[0]]]
            }
            style={{ width: "100%" }}
            mode="multiple"
            options={getFilterOptions(item[Object.keys(item)[0]])}
            onChange={onOptionSelect(Object.keys(item)[0])}
          />
        </AntCol>
      );
    });
  };

  return (
    <div className={"filters"}>
      <AntRow gutter={[16, 16]} justify={"start"} type={"flex"}>
        {filtersValueFetching && <Spin />}
        {!filtersValueFetching && dataFromApi && getFilters()}
      </AntRow>
    </div>
  );
};

export default connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(JiraFilter);
