import { getGenericFilters } from "../helper";
import { get, isEqual } from "lodash";
import { toTitleCase } from "utils/stringUtils";

export const transformerFn = (data: any) => {
  const { apiData } = data;

  const modifiedData = (apiData || []).map((record: any, index: number) => {
    const { full_name, score, section_responses, org_user_id, result_time = undefined } = record;

    const sectionData = (section_responses || []).reduce((acc: any, next: any) => {
      let key = next?.name.split(" ").join("_");
      key = `${key}_score`;
      return {
        ...acc,
        [key]: next.score,
        result_time
      };
    }, {});

    if (index === 0) {
      const { org_name, score, org_id } = record;
      return {
        full_name: org_name,
        score,
        org_user_id: org_id,
        result_time,
        ...sectionData
      };
    }

    return {
      full_name,
      score,
      org_user_id,
      result_time,
      ...sectionData
    };
  });

  return { data: modifiedData || [] };
};

export const csvTransformerFn = (data: any) => {
  const { apiData, columns, filters } = data;
  return (apiData || []).map((record: any, index: number) => {
    return (columns || [])
      .map((col: any, col_index: number) => {
        let result = index === 0 && col_index === 0 ? record["org_name"] : record[col.key];
        if (Array.isArray(result)) {
          if (!result.length) return "";
          return `"${result.join(",")}"`;
        }
        if (typeof result === "string") {
          if (result.includes(",")) {
            return `"${result}"`;
          }
          return result ?? "NA";
        }
        return result ?? "NA";
      })
      .join(",");
  });
};

export const getGraphFilters = (data: any) => {
  const { contextFilter, defaultSort, ou_ids, dashboardOuIdsRef } = data;
  let genericFilters = getGenericFilters(data);
  let page = contextFilter?.page || 0;
  if (!isEqual(ou_ids, dashboardOuIdsRef?.current)) {
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

export const getFiltersCount = (filters: any) => {
  const count = Object.keys(filters).length;
  return count + 1; //+1 for show only needs improvement always present in metadata
};
