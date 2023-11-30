export const MAX_LENGTH_TO_SHOW = 10;

export const collabStateColorMapping = {
  unapproved: "#CF1322",
  self_approved: "#FFA333",
  self_approved_with_review: "#FFC847",
  unassigned_peer_approved: "#789FE9",
  assigned_peer_approved: "#61BA14"
};

export const collabLegendMapping = {
  unapproved: true,
  self_approved: true,
  self_approved_with_review: true,
  unassigned_peer_approved: true,
  assigned_peer_approved: true
};

export const submittersSortingOptions = [
  { label: "Most Submitted PRs", value: "most-submitted" },
  { label: "Least Submitted PRs", value: "least-submitted" }
];

export const reviewersSortingOptions = [
  { label: "Most Approved PRs", value: "most-approved" },
  { label: "Least Approved PRs", value: "least-approved" }
];

export enum SCMCollabSortingValues {
  MOST_SUBMITTED = "most-submitted",
  LEAST_SUBMITTED = "least-submitted",
  MOST_APPROVED = "most-approved",
  LEAST_APPROVED = "least-approved"
}

export enum RemainingUsersKeys {
  OTHER_REVIEWER = "other-reviewer",
  OTHER_COMMITTER = "other-submitters",
  LABEL_OTHER_REVIEWERS = "Other Reviewers",
  LABEL_OTHER_COMMITTERS = "Other Committers"
}

export enum SCMRevCollabUserHeaderActionText {
  SHOW_ALL = "Show All",
  SHOW_LESS = "Show Less",
  ALL = "All",
  SHOW_TOP_25 = "Show Top 25"
}

export enum SCMReviewCollaborationCustomStateType {
  SELF_APPROVED = "self_approved"
}
