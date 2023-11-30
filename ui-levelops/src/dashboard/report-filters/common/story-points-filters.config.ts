import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalInputTimeRangeWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalInputTimeRangeWrapper";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { withDeleteProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

export const StoryPointsFilterConfig: LevelOpsFilter = {
  id: "story_points",
  renderComponent: UniversalInputTimeRangeWrapper,
  label: "Story Points",
  beKey: "story_points",
  labelCase: "none",
  filterMetaData: {},
  deleteSupport: true,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  apiFilterProps: (args: any) => {
    const withDelete: withDeleteProps = {
      showDelete: args?.deleteSupport,
      key: args?.beKey,
      onDelete: args.handleRemoveFilter
    };
    return { withDelete };
  }
};

export const generateStoryPointsFilterConfig = (beKey: string, label: string): LevelOpsFilter => ({
  ...StoryPointsFilterConfig,
  beKey,
  label
});

export const ParentStoryPointsFilterConfig: LevelOpsFilter = {
  id: "parent_story_points",
  renderComponent: UniversalInputTimeRangeWrapper,
  label: "Parent Story Points",
  beKey: "parent_story_points",
  labelCase: "none",
  filterMetaData: {},
  deleteSupport: true,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  apiFilterProps: (args: any) => {
    const withDelete: withDeleteProps = {
      showDelete: args?.deleteSupport,
      key: args?.beKey,
      onDelete: args.handleRemoveFilter
    };
    return { withDelete };
  }
};

export const generateParentStoryPointsFilterConfig = (beKey: string, label: string): LevelOpsFilter => ({
  ...StoryPointsFilterConfig,
  beKey,
  label
});
