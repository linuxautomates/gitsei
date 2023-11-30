import { filter, find } from "lodash";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const getFiltersByTab = (
  filtersConfig: LevelOpsFilter[]
): { [x in WIDGET_CONFIGURATION_KEYS]: LevelOpsFilter[] } => {
  return {
    [WIDGET_CONFIGURATION_KEYS.FILTERS]: filter(
      filtersConfig,
      config => config.tab === WIDGET_CONFIGURATION_KEYS.FILTERS
    ),
    [WIDGET_CONFIGURATION_KEYS.AGGREGATIONS]: filter(
      filtersConfig,
      config => config.tab === WIDGET_CONFIGURATION_KEYS.AGGREGATIONS
    ),
    [WIDGET_CONFIGURATION_KEYS.METRICS]: filter(
      filtersConfig,
      config => config.tab === WIDGET_CONFIGURATION_KEYS.METRICS
    ),
    [WIDGET_CONFIGURATION_KEYS.SETTINGS]: filter(
      filtersConfig,
      config => config.tab === WIDGET_CONFIGURATION_KEYS.SETTINGS
    ),
    [WIDGET_CONFIGURATION_KEYS.WEIGHTS]: filter(
      filtersConfig,
      config => config.tab === WIDGET_CONFIGURATION_KEYS.WEIGHTS
    ),
    [WIDGET_CONFIGURATION_KEYS.OTHERS]: filter(filtersConfig, config => config.tab === WIDGET_CONFIGURATION_KEYS.OTHERS)
  };
};

export const getFilterById = (filtersConfig: LevelOpsFilter[], id: LevelOpsFilter["id"]) => {
  return find(filtersConfig, filter => {
    return filter.id === id;
  });
};
