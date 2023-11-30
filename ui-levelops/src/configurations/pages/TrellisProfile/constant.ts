export const DEV_PROFILE_UUID = "dev_productivity_profile";
export const KEY_LESS_THAN = "$lt";
export const KEY_GREATER_THAN = "$gt";
export const KEY_EQUALS = "$eq";
export const KEY_CONTAINS = "$contains";
export const KEY_BEGINS = "$begins";
export const KEY_ENDS = "$ends";

export const EXCLUDE_SETTINGS_OPTIONS = [
  {
    label: "that contains",
    value: KEY_CONTAINS
  },
  {
    label: "starts with",
    value: KEY_BEGINS
  },
  {
    label: "ends with",
    value: KEY_ENDS
  }
];

export const EXCLUDE_SETTINGS_DEFAULT_OPTIONS = "$contains";
export const EXCLUDE_NUMBER_SETTINGS_DEFAULT_OPTIONS = KEY_GREATER_THAN;

export const EXCLUDE_SETTINGS = [
  {
    label: "Exclude PRs with the labels",
    BEKey: "labels"
  },
  {
    label: "Exclude PRs with the titles",
    BEKey: "title"
  },
  {
    label: "Exclude PRs in the branches",
    BEKey: "target_branch"
  }
];

export const EXCLUDE_COMMITS = [
  {
    label: "Exclude commits with title",
    BEKey: "commit_title"
  },
  {
    label: "Exclude commits having lines of code",
    BEKey: "loc",
    isNumeric: true
  }
];

export const EXCLUDE_NUMBER_SETTINGS_OPTIONS: Array<any> = [
  {
    label: "greater than",
    value: KEY_GREATER_THAN
  },
  {
    label: "less than",
    value: KEY_LESS_THAN
  },
  {
    label: "equal to",
    value: KEY_EQUALS
  }
];

export const SECTIONS = ["Quality", "Impact", "Volume", "Speed", "Proficiency", "Leadership & Collaboration"];

export const TEXT_EXCLUDE_PR_HEADER = "List the PRs to be excluded from the current Trellis profile";
export const TEXT_EXCLUDE_PR_NOTE = " Note: The associated commits will also be excluded when the PRs are excluded.";
export const TEXT_EXCLUDE_COMMIT_HEADER = "List the commits to be excluded from the current Trellis profile";
export const TEXT_EXCLUDE_COMMIT_NOTE = "Note: The commits alone will be excluded.";
export const TEXT_LIST_DEV_STAGES = "List the development stages involved";
export const TEXT_LIST_DEV_STAGES_NOTE =
  "Map the current Trellis profile with the development stages from your Issue Management tool to credit the members for the work accomplished.";
export const TEXT_ERROR_DESC = "Page failed to load. Please refresh the page";

export enum TRELLIS_PROFILE_MENU {
  BASIC_INFO = "basic Info",
  ASSOCIATIONS = "associations",
  FACTORS_WEIGHTS = "factors & weights",
  QUALITY = "Quality",
  IMPACT = "Impact",
  VOLUME = "Volume",
  SPEED = "Speed",
  PROFICIENCY = "Proficiency",
  LEADERSHIP_COLLABORATION = "Leadership & Collaboration"
}

export const TRELLIS_SECTION_MAPPING: Record<string, string> = {
  [TRELLIS_PROFILE_MENU.LEADERSHIP_COLLABORATION]: "Collaboration"
};

export const FEATURES_WITH_EFFORT_PROFILE = [
  "NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH",
  "NUMBER_OF_CRITICAL_STORIES_RESOLVED_PER_MONTH"
];