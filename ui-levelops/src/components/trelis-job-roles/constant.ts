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
export const TEXT_EXCLUDE_PR_NOTE = " The associated commits will also be excluded when the PRs are excluded.";
export const TEXT_EXCLUDE_COMMIT_HEADER = "List the commits to be excluded from the current Trellis profile";
export const TEXT_EXCLUDE_COMMIT_NOTE = "The commits alone will be excluded.";
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
  LEADERSHIP_COLLABORATION = "Leadership & Collaboration",
  COLLABORATION = "Collaboration"
}

export const TRELLIS_SECTION_MAPPING: Record<string, string> = {
  [TRELLIS_PROFILE_MENU.LEADERSHIP_COLLABORATION]: "Collaboration"
};

export const ADVANCED_CONFIG_INFO =
  "The advanced configuration contains several additional elements that are not mandatory but are nice to have in a scenario where you would want to ignore certain types of PRs and commits.";
export const ADVANCED_CONFIG_LABEL = "Advanced Configuration";

export const FACTOR_NAME_TO_ICON_MAPPING: Record<string, string> = {
  [TRELLIS_PROFILE_MENU?.QUALITY]: "diamond",
  [TRELLIS_PROFILE_MENU?.IMPACT]: "impact",
  [TRELLIS_PROFILE_MENU?.VOLUME]: "volume",
  [TRELLIS_PROFILE_MENU?.PROFICIENCY]: "proficiency",
  [TRELLIS_PROFILE_MENU?.COLLABORATION]: "collaboration",
  [TRELLIS_PROFILE_MENU?.LEADERSHIP_COLLABORATION]: "collaboration",
  [TRELLIS_PROFILE_MENU?.SPEED]: "speed"
};

export const OrgColumn = [
  {
    title: "Org Name",
    dataIndex: "name",
    key: "name"
  }
];
export const PopupDropdownOptions: any[] = [
  {
    label: `Select manually`,
    value: "MANUALLY"
  }
];

export const PopupRadioOption: any[] = [
  {
    label: `Selected`,
    value: "SELECTED_JOBS"
  },
  {
    label: `All`,
    value: "ALL_JOBS"
  }
];

export const FEATURES_WITH_EFFORT_PROFILE = [
  "NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH",
  "NUMBER_OF_CRITICAL_STORIES_RESOLVED_PER_MONTH"
];

export const METRIC_LABELS: any = {
  "Percentage of Rework": {
    maxLabel: "What is the maximum percentage a contributor is permitted to dedicate to rework?",
    note: "A lower value indicates a better score, reflecting a reduced need for reworking the same code."
  },
  "Percentage of Legacy Rework": {
    maxLabel: "What is the maximum percentage a contributor is permitted to dedicate to legacy rework?",
    note: "A lower value indicates a better score, reflecting a reduced need for reworking the same code."
  },
  "High Impact bugs worked on per month": {
    maxLabel: "What is the maximum number of high-impact bugs that a contributor can effectively address?",
    note: "A higher value indicates a better score, reflecting active work in resolving bugs."
  },
  "High Impact stories worked on per month": {
    maxLabel: "What is the maximum number of high-impact bugs that a contributor can effectively address?",
    note: "A higher value indicates a better score, reflecting active work in resolving bugs."
  },
  "Number of PRs per month": {
    maxLabel: "What is the maximum number of PRs that a contributor can effectively close?",
    note: "A higher value indicates a better score, reflecting active work on multiple PRs."
  },
  "Number of Commits per month": {
    maxLabel: "What is the maximum number of commits that a contributor can effectively commit?",
    note: "A higher value indicates a better score, reflecting active work on multiple commits."
  },

  "Lines of Code per month": {
    maxLabel: "What is the maximum number of lines of code that a contributor can proficiently write?",
    note: "A higher value indicates a better score, reflecting greater code contribution."
  },
  "Number of bugs worked on per month": {
    maxLabel: "What is the maximum number of bugs that a contributor can effectively address?",
    note: "A higher value indicates a better score, reflecting active work in resolving bugs."
  },
  "Number of stories worked on per month": {
    maxLabel: "What is the maximum number of stories that a contributor can effectively address?",
    note: "A higher value indicates a better score, reflecting active work on resolving stories."
  },
  "Number of Story Points worked on per month": {
    maxLabel: "What is the maximum number of story points that a contributor can effectively address?",
    note: "A higher value indicates a better score, reflecting active work on resolving stories."
  },
  "Average Coding days per week": {
    maxLabel: "What is the maximum number of days the contributor can code?",
    note: "A higher value indicates a better score, reflecting consistent coding effort."
  },
  "Average PR Cycle Time": {
    maxLabel: "What is the maximum duration, on average, for closing a PR?",
    note: "A lower value indicates a better score, reflecting a shorter PR cycle time."
  },
  "Average time spent working on Issues": {
    maxLabel: "What is the maximum duration, on average, for closing an Issue?",
    note: "A lower value indicates a better score, reflecting a faster Issue resolution time."
  },
  "Technical Breadth - Number of unique file extension": {
    maxLabel: "What is the maximum number of unique file extensions a contributor can contribute to?",
    note: "A higher value indicates a better score, reflecting broader range of technical expertise."
  },
  "Repo Breadth - Number of unique repo": {
    maxLabel: "What is the maximum number of unique repositories a contributor can contribute to?",
    note: "A higher value indicates a better score, reflecting a broader contribution range."
  },
  "Number of PRs approved per month": {
    maxLabel: "What is the maximum number of PRs that a contributor can effectively approve?",
    note: "A higher value indicates a better score, reflecting active work on approving PRs."
  },
  "Number of PRs commented on per month": {
    maxLabel: "What is the maximum number of PRs that a contributor can effectively comment on?",
    note: "A higher value indicates a better score, reflecting active engagement in commenting on the PRs."
  },
  "Average response time for PR approvals": {
    maxLabel: "What is the maximum duration, on average, for approving a PR?",
    note: "A lower value indicates a better score, reflecting a shorter PR approval time."
  },
  "Average response time for PR comments": {
    maxLabel: "What is the maximum duration, on average, for commenting on a PR?",
    note: "A lower value indicates a better score, reflecting a faster PR comment time."
  }
};

export const ERROR_MSG_FOR_LOWER_METRIC = "* Lower limit value can't be greater than upper limit value.";
export const ENABLE_FACTOR_MSG = "Please enable factor";