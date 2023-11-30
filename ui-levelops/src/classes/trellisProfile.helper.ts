export const defaultSections = [
  {
    name: "Quality",
    description: "Configure metrics to measure the quality of work",
    order: 0,
    enabled: false,
    weight: 0,
    features: [
      {
        feature_unit: "%",
        slow_to_good_is_ascending: false,
        feature_max_value_text: "Max % of work",
        name: "Percentage of Rework",
        description: "Provide the baseline for worst performance or 0% score",
        order: 0,
        type: "PERCENTAGE_OF_REWORK",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      },
      {
        feature_unit: "%",
        slow_to_good_is_ascending: false,
        feature_max_value_text: "Max % of work",
        name: "Percentage of Legacy Rework",
        description: "Provide the baseline for worst performance or 0% score",
        order: 1,
        type: "PERCENTAGE_OF_LEGACY_REWORK",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      }
    ]
  },
  {
    name: "Impact",
    description: "Configure metrics to measure the impact of work",
    order: 1,
    enabled: false,
    weight: 0,
    features: [
      {
        feature_unit: "Bugs",
        slow_to_good_is_ascending: true,
        feature_max_value_text: "Top # of Bugs",
        name: "High Impact bugs worked on per month",
        description: "Provide the baseline for best performance or 100% score",
        order: 0,
        type: "NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      },
      {
        feature_unit: "Stories",
        slow_to_good_is_ascending: true,
        feature_max_value_text: "Top # of Stories",
        name: "High Impact stories worked on per month",
        description: "Provide the baseline for best performance or 100% score",
        order: 1,
        type: "NUMBER_OF_CRITICAL_STORIES_RESOLVED_PER_MONTH",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      }
    ]
  },
  {
    name: "Volume",
    description: "Configure metrics to measure the volume of work",
    order: 2,
    enabled: false,
    weight: 0,
    features: [
      {
        feature_unit: "PRs",
        slow_to_good_is_ascending: true,
        feature_max_value_text: "Top # of PRs",
        name: "Number of PRs per month",
        description: "Provide the baseline for best performance or 100% score",
        order: 0,
        type: "NUMBER_OF_PRS_PER_MONTH",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      },
      {
        feature_unit: "Commits",
        slow_to_good_is_ascending: true,
        feature_max_value_text: "Top # of Commits",
        name: "Number of Commits per month",
        description: "Provide the baseline for best performance or 100% score",
        order: 1,
        type: "NUMBER_OF_COMMITS_PER_MONTH",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      },
      {
        feature_unit: "Lines",
        slow_to_good_is_ascending: true,
        feature_max_value_text: "Top # of lines",
        name: "Lines of Code per month",
        description: "Provide the baseline for best performance or 100% score",
        order: 2,
        type: "LINES_OF_CODE_PER_MONTH",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      },
      {
        feature_unit: "Bugs",
        slow_to_good_is_ascending: true,
        feature_max_value_text: "Top # of Bugs",
        name: "Number of bugs worked on per month",
        description: "Provide the baseline for best performance or 100% score",
        order: 3,
        type: "NUMBER_OF_BUGS_FIXED_PER_MONTH",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      },
      {
        feature_unit: "Stories",
        slow_to_good_is_ascending: true,
        feature_max_value_text: "Top # of Stories",
        name: "Number of stories worked on per month",
        description: "Provide the baseline for best performance or 100% score",
        order: 4,
        type: "NUMBER_OF_STORIES_RESOLVED_PER_MONTH",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      },
      {
        feature_unit: "Points",
        slow_to_good_is_ascending: true,
        feature_max_value_text: "Top # of Points",
        name: "Number of Story Points worked on per month",
        description: "Provide the baseline for best performance or 100% score",
        order: 5,
        type: "NUMBER_OF_STORY_POINTS_DELIVERED_PER_MONTH",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      }
    ]
  },
  {
    name: "Speed",
    description: "Configure metrics to measure the speed of work",
    order: 3,
    enabled: false,
    weight: 0,
    features: [
      {
        feature_unit: "DAYS",
        slow_to_good_is_ascending: true,
        feature_max_value_text: "Top # of Days",
        name: "Average Coding days per week",
        description: "Provide the baseline for best performance or 100% score",
        order: 0,
        type: "AVG_CODING_DAYS_PER_WEEK",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      },
      {
        feature_unit: "DAYS",
        slow_to_good_is_ascending: false,
        feature_max_value_text: "Max # of Days",
        name: "Average PR Cycle Time",
        description: "Provide the baseline for worst performance or 0% score",
        order: 1,
        type: "AVG_PR_CYCLE_TIME",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      },
      {
        feature_unit: "DAYS",
        slow_to_good_is_ascending: false,
        feature_max_value_text: "max # of Days",
        name: "Average time spent working on Issues",
        description: "Provide the baseline for worst performance or 0% score",
        order: 2,
        type: "AVG_ISSUE_RESOLUTION_TIME",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      }
    ]
  },
  {
    name: "Proficiency",
    description: "Configure metrics to measure technical proficiency",
    order: 4,
    enabled: false,
    weight: 0,
    features: [
      {
        feature_unit: "",
        slow_to_good_is_ascending: true,
        feature_max_value_text: "Top # of Extensions",
        name: "Technical Breadth - Number of unique file extension",
        description: "Provide the baseline for best performance or 100% score",
        order: 0,
        type: "TECHNICAL_BREADTH",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      },
      {
        feature_unit: "Repos",
        slow_to_good_is_ascending: true,
        feature_max_value_text: "Top # of Repos",
        name: "Repo Breadth - Number of unique repo",
        description: "Provide the baseline for best performance or 100% score",
        order: 1,
        type: "REPO_BREADTH",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      }
    ]
  },
  {
    name: "Leadership & Collaboration",
    description: "Configure metrics to measure collaboration",
    order: 5,
    enabled: false,
    weight: 0,
    features: [
      {
        feature_unit: "PRs",
        slow_to_good_is_ascending: true,
        feature_max_value_text: "Top # of PRs",
        name: "Number of PRs approved per month",
        description: "Provide the baseline for best performance or 100% score",
        order: 0,
        type: "NUMBER_OF_PRS_APPROVED_PER_MONTH",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      },
      {
        feature_unit: "PRs",
        slow_to_good_is_ascending: true,
        feature_max_value_text: "Top # of PRs",
        name: "Number of PRs commented on per month",
        description: "Provide the baseline for best performance or 100% score",
        order: 1,
        type: "NUMBER_OF_PRS_COMMENTED_ON_PER_MONTH",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      },
      {
        feature_unit: "DAYS",
        slow_to_good_is_ascending: false,
        feature_max_value_text: "Max # of Days",
        name: "Average response time for PR approvals",
        description: "Provide the baseline for worst performance or 0% score",
        order: 2,
        type: "PRS_AVG_APPROVAL_TIME",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      },
      {
        feature_unit: "DAYS",
        slow_to_good_is_ascending: false,
        feature_max_value_text: "Max # of Days",
        name: "Average response time for PR comments",
        description: "Provide the baseline for worst performance or 0% score",
        order: 3,
        type: "PRS_AVG_COMMENT_TIME",
        lower_limit_percentage: 33,
        upper_limit_percentage: 66,
        enabled: false
      }
    ]
  }
];
