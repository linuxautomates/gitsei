import { isEqual } from "lodash";
import { getGenericFilters } from "../helper";

export const transformerFn = (data: any) => {
  const { apiData } = data;

  const modifiedData = (apiData || []).map((record: any) => {
    const { org_name, score, section_responses, org_id, result_time = undefined } = record;

    const sectionData = (section_responses || []).reduce((acc: any, next: any) => {
      let key = next?.name.split(" ").join("_");
      key = `${key}_score`;
      return {
        ...acc,
        [key]: next.score,
        result_time
      };
    }, {});

    return {
      org_name,
      score,
      org_id,
      result_time,
      ...sectionData
    };
  });
  return { data: modifiedData || [] };
};

export const csvTransformerFn = (data: any) => {
  const { apiData, columns, filters } = data;
  return (apiData || []).map((record: any) => {
    return (columns || [])
      .map((col: any) => {
        let result = record[col.key];
        if (Array.isArray(result)) {
          if (!result.length) return "";
          return `"${result.join(",")}"`;
        }
        if (typeof result === "string") {
          if (result.includes(",")) {
            return `"${result}"`;
          }
          return result;
        }
        return result;
      })
      .join(",");
  });
};

export const getGraphFilters = (data: any) => {
  const { contextFilter, defaultSort, ou_ids, dashboardOuIdsRef, showDashboardOrg } = data;
  let genericFilters = getGenericFilters(data);
  let page = contextFilter?.page || 0;
  if (showDashboardOrg && !isEqual(ou_ids, dashboardOuIdsRef?.current)) {
    page = 0;
  }
  genericFilters = {
    ...genericFilters,
    sort: [contextFilter?.sort || defaultSort],
    page: page,
    page_size: contextFilter?.page_size || 10
  };
  delete genericFilters?.filter?.integration_ids;
  delete genericFilters?.filter?.product_id;
  return genericFilters;
};
