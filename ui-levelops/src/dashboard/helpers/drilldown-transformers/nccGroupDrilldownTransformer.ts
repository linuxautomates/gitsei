import { get, unset } from "lodash";
import { genericDrilldownTransformer } from "./genericDrilldownTransformer";

export const nccGroupDrilldownTransformer = (data: any) => {
  const { acrossValue, filters } = genericDrilldownTransformer(data);
  let filter = filters.filter;
  const component = get(filters, ["filter", "components"], undefined);
  const projects = get(filter, ["project"], []).map((project: string) => ({ key: project }));
  const tags = get(filter, ["tag"], []).map((tag: string) => ({ key: tag }));

  if (component) {
    unset(filter, ["components"]);
    filter = {
      ...filters.filter,
      component
    };
  }

  if (projects.length > 0) {
    filter = {
      ...filter,
      project: projects
    };
  }

  if (tags.length > 0) {
    filter = {
      ...filter,
      tag: tags
    };
  }

  const newFilters = {
    ...filters,
    filter: {
      ...filter
    }
  };

  return { acrossValue, filters: newFilters };
};
