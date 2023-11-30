import { get, unset } from "lodash";
import { genericDrilldownTransformer } from "./genericDrilldownTransformer";

export const praetorianDrilldownTransformer = (data: any) => {
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  let filter = filters.filter;
  const priority = get(filter, ["priorities"], undefined);
  const projects = get(filter, ["project"], []).map((project: string) => ({ key: project }));
  const tags = get(filter, ["tag"], []).map((tag: string) => ({ key: tag }));
  if (priority) {
    unset(filter, ["priorities"]);
    filter = {
      ...filter,
      priority
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
