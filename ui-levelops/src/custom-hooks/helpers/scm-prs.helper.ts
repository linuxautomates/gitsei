import { basicMappingType } from "dashboard/dashboard-types/common-types";
import {
  SCMReviewCollaborationReportApiDataType,
  SCMReviewCollaborationReviewsConfig
} from "dashboard/dashboard-types/scmReports.types";
import { SCMReviewCollaborationStateType } from "dashboard/reports/scm/scm-review-collaboration/scm-review-collaboration-report.enum";
import { filter, forEach, map, uniq, uniqBy } from "lodash";

const DEFAULT_COLLAB_STATE_VALUE = [0, 0, 0, 0, 0];

export const SCMReviewCollaborationTransformer = (data: any) => {
  let { apiData: response = [] } = data;
  let totalBreakdowndata: basicMappingType<number> = {
    [SCMReviewCollaborationStateType.ASSIGNED_PEER_APPROVED]: 0,
    [SCMReviewCollaborationStateType.UNAPPROVED]: 0,
    [SCMReviewCollaborationStateType.UNASSIGNED_PEER_APPROVED]: 0,
    [SCMReviewCollaborationStateType.SELF_APPROVED]: 0,
    [SCMReviewCollaborationStateType.SELF_APPROVED_WITH_REVIEW]: 0
  };

  // making sure that there must exits key property
  let apiData = response.map((item: any) => {
    const key = item?.key ?? item?.additional_key;
    const stacks = item?.stacks?.map((sItem: any) => {
      const sKey = sItem?.key ?? sItem?.additional_key;
      return { ...sItem, key: sKey };
    });
    return { ...item, key, stacks };
  });

  // Calculation of total breakdown
  forEach(apiData, rec => {
    totalBreakdowndata[rec?.collab_state] += rec?.count;
  });

  // creating submitters list

  const allSubmitters = uniq(
    apiData.reduce((acc: any, item: any) => {
      acc.push(item?.key);
      return acc;
    }, [])
  );

  const submittersList = map(allSubmitters, (item: string) => {
    const currentSubmittersCollabInfo: Array<SCMReviewCollaborationReportApiDataType> = filter(
      apiData,
      (rec: { key: string }) => rec?.key === item
    );
    let totalPrs = 0;
    let name = item;

    // getting total prs from all collaboration states
    if (currentSubmittersCollabInfo && currentSubmittersCollabInfo?.length) {
      totalPrs = currentSubmittersCollabInfo.reduce((acc, data) => {
        return (acc += data?.count);
      }, 0);
      name = currentSubmittersCollabInfo[0].additional_key ?? item;
    }

    let [unapproved, self_approved, self_approved_with_review, unassigned_peer_approved, assigned_peer_approved] =
      DEFAULT_COLLAB_STATE_VALUE;

    if (totalPrs) {
      // calculating unapproved
      const unapprovedCollabObject = currentSubmittersCollabInfo.find(
        info => info?.collab_state === SCMReviewCollaborationStateType.UNAPPROVED
      );
      if (unapprovedCollabObject) {
        unapproved = (unapprovedCollabObject.count / totalPrs) * 100.0;
      }

      // calculating self-approved-with-review
      const selfApprovedWithReviewCollabObject = currentSubmittersCollabInfo.find(
        info => info?.collab_state === SCMReviewCollaborationStateType.SELF_APPROVED_WITH_REVIEW
      );
      if (selfApprovedWithReviewCollabObject) {
        self_approved_with_review = (selfApprovedWithReviewCollabObject.count / totalPrs) * 100.0;
      }

      // calculating self-approved
      const selfApprovedCollabObject = currentSubmittersCollabInfo.find(
        info => info?.collab_state === SCMReviewCollaborationStateType.SELF_APPROVED
      );
      if (selfApprovedCollabObject) {
        self_approved = (selfApprovedCollabObject.count / totalPrs) * 100;
      }

      // calculating assigned-peer-approved
      const assignedPeerApprovedCollabObject = currentSubmittersCollabInfo.find(
        info => info?.collab_state === SCMReviewCollaborationStateType.ASSIGNED_PEER_APPROVED
      );
      if (assignedPeerApprovedCollabObject) {
        assigned_peer_approved = (assignedPeerApprovedCollabObject.count / totalPrs) * 100;
      }

      // calculating unassigned-peer-approved
      const unassignedPeerApprovedCollabObject = currentSubmittersCollabInfo.find(
        info => info?.collab_state === SCMReviewCollaborationStateType.UNASSIGNED_PEER_APPROVED
      );
      if (unassignedPeerApprovedCollabObject) {
        unassigned_peer_approved = (unassignedPeerApprovedCollabObject.count / totalPrs) * 100;
      }
    }

    return {
      name,
      key: item,
      total_prs: totalPrs,
      unapproved,
      self_approved,
      self_approved_with_review,
      unassigned_peer_approved,
      assigned_peer_approved
    };
  });

  const allReviewers: SCMReviewCollaborationReviewsConfig[] = apiData.reduce((acc: any, item: any) => {
    acc = [...acc, ...(item.stacks || [])];
    return acc;
  }, []);

  const allUniqReviewersKeys = filter(
    map(uniqBy(allReviewers, "key"), (item: SCMReviewCollaborationReviewsConfig) => item),
    item => item.key !== "NONE"
  );

  const reviewersList = allUniqReviewersKeys.map((item: SCMReviewCollaborationReviewsConfig) => {
    const revPrAndCollabStateMapping: basicMappingType<number> = {
      [SCMReviewCollaborationStateType.ASSIGNED_PEER_APPROVED]: 0,
      [SCMReviewCollaborationStateType.UNAPPROVED]: 0,
      [SCMReviewCollaborationStateType.UNASSIGNED_PEER_APPROVED]: 0,
      [SCMReviewCollaborationStateType.SELF_APPROVED]: 0,
      [SCMReviewCollaborationStateType.SELF_APPROVED_WITH_REVIEW]: 0
    };

    // aggregating number of prs under each collab state
    forEach(apiData, (apiRec: SCMReviewCollaborationReportApiDataType) => {
      const reviewerOfCurSubmitter = (apiRec?.stacks ?? []).find(
        rec => rec?.key === item?.key || rec?.additional_key === item?.additional_key
      );
      if (reviewerOfCurSubmitter) {
        revPrAndCollabStateMapping[apiRec?.collab_state] += reviewerOfCurSubmitter.count;
      }
    });

    let totalPrs = 0;
    forEach(Object.values(revPrAndCollabStateMapping) as number[], (value: number) => {
      totalPrs += value;
    });

    if (totalPrs) {
      forEach(Object.keys(revPrAndCollabStateMapping), key => {
        const value = (revPrAndCollabStateMapping as any)[key];
        revPrAndCollabStateMapping[key] = (value / totalPrs) * 100.0;
      });
    }

    return {
      name: item?.additional_key ?? item?.key,
      key: item?.key,
      total_prs: totalPrs,
      unapproved: revPrAndCollabStateMapping[SCMReviewCollaborationStateType.UNAPPROVED],
      self_approved: revPrAndCollabStateMapping[SCMReviewCollaborationStateType.SELF_APPROVED],
      self_approved_with_review: revPrAndCollabStateMapping[SCMReviewCollaborationStateType.SELF_APPROVED_WITH_REVIEW],
      unassigned_peer_approved: revPrAndCollabStateMapping[SCMReviewCollaborationStateType.UNASSIGNED_PEER_APPROVED],
      assigned_peer_approved: revPrAndCollabStateMapping[SCMReviewCollaborationStateType.ASSIGNED_PEER_APPROVED]
    };
  });

  return {
    data: {
      apiData,
      submittersList,
      reviewersList,
      totalBreakdowndata
    }
  };
};

export const getGraphFilters = (params: any) => {
  const { contextFilter, finalFilters } = params;

  const updatedFilters = {
    ...finalFilters,
    filter: {
      ...finalFilters.filter,
      ...contextFilter
    }
  };
  return updatedFilters;
};
