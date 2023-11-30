import { transformActivityData } from "dashboard/pages/scorecard/components/PRActivity/helpers";
import { getPRActivityColumns } from "dashboard/pages/scorecard/components/PRActivity/prActivity.tableConfig";

export const transformData = (props: any) => {
  let data = {};
  if (props.apiData) {
    data = props.apiData.reduce((acc: any, currentData: any) => {
      const transformedData = transformActivityData(currentData);
      acc = {
        ...acc,
        ...transformedData
      };
      return acc;
    }, {});
  }
  const across = props.widgetFilters?.filter?.across;
  return { data, across };
};

export const getFilters = (props: any) => {
  return {
    page: props.contextFilter?.page || 0,
    page_size: 5,
    filter: {
      ...props.contextFilter,
      ou_ref_ids: props.ou_ids
    }
  };
};

export const getDynamicColumns = (data: any, contextFilter: any) => {
  if (contextFilter?.time_range) {
    return getPRActivityColumns(contextFilter.time_range, "170px", contextFilter.across === "repo_id");
  }
  return [];
};

export const chartProps = {
  columns: []
};
