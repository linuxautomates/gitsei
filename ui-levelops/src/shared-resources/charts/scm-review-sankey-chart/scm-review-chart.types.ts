export type CommitterReviewerConnectionType = {
  [x: string]: {
    [y: string]: number | string;
  };
};

export type SCMCollabUsersConfigType = {
  name: string;
  key: string;
  total_prs: number;
  overallPrs?: number;
  reviewerPRsPercent?: number | string;
  unapproved: number;
  self_approved: number;
  self_approved_with_review: number;
  unassigned_peer_approved: number;
  assigned_peer_approved: number;
};
