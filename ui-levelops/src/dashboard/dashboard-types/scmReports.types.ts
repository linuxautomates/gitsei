import { SCMReviewCollaborationStateType } from "dashboard/reports/scm/scm-review-collaboration/scm-review-collaboration-report.enum";

export type SCMReviewCollaborationReviewsConfig = { key: string; additional_key: string; count: number };

export type SCMReviewCollaborationReportApiDataType = {
  additional_key: string;
  key: string;
  collab_state: SCMReviewCollaborationStateType;
  count: number;
  stacks: SCMReviewCollaborationReviewsConfig[];
};
