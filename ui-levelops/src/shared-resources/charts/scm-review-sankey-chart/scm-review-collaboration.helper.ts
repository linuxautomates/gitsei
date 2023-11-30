import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { cloneDeep, forEach, map, max, orderBy } from "lodash";
import { RemainingUsersKeys } from "./constant";
import { SCMCollabUsersConfigType } from "./scm-review-chart.types";

// for getting aggregation of all remaining users
export const getRemainingUsersConfig = (remainingList: any[]) => {
  const remainingDataMapping: basicMappingType<number> = {
    total_prs: 0,
    unapproved: 0,
    self_approved: 0,
    self_approved_with_review: 0,
    unassigned_peer_approved: 0,
    assigned_peer_approved: 0
  };

  forEach(Object.keys(remainingDataMapping), key => {
    forEach(remainingList, rec => {
      remainingDataMapping[key] += rec?.[key];
    });
  });

  return remainingDataMapping;
};

// Showing only max 7 users so for getting the remaining users count
export const getRemainingUsers = (
  slicedCommittersList: SCMCollabUsersConfigType[],
  slicedReviewersList: SCMCollabUsersConfigType[],
  committersList: SCMCollabUsersConfigType[],
  reviewersList: SCMCollabUsersConfigType[]
) => {
  let remainingSubmitters = 0;
  let remainingReviewers = 0;
  let slicedSubmittersListLength = slicedCommittersList.length;
  let slicedReviewersListLength = slicedReviewersList.length;

  if (slicedSubmittersListLength && slicedCommittersList[slicedSubmittersListLength - 1]?.name === "Other Committers") {
    slicedSubmittersListLength -= 1;
  }

  if (slicedReviewersListLength && slicedReviewersList[slicedReviewersListLength - 1]?.name === "Other Reviewers") {
    slicedReviewersListLength -= 1;
  }

  if (slicedSubmittersListLength !== committersList.length) {
    remainingSubmitters = committersList.length - slicedSubmittersListLength;
  }

  if (slicedReviewersListLength !== reviewersList.length) {
    remainingReviewers = reviewersList.length - slicedReviewersListLength;
  }
  return max([remainingReviewers, remainingSubmitters]);
};

export const getFirstAndLastChildWithNonZeroVal = (record: basicMappingType<number>, dataKeys: string[]) => {
  let firstKey = "",
    lastKey = "";

  forEach(dataKeys, key => {
    if (record[key]) {
      if (!firstKey) firstKey = key;
      lastKey = key;
    }
  });

  return [firstKey, lastKey];
};

export const SCMReviewListToPercentageList = (
  records: {
    unapproved: number;
    self_approved: number;
    self_approved_with_review: number;
    unassigned_peer_approved: number;
    assigned_peer_approved: number;
  }[]
) => {
  let dataKeys = [
    "unapproved",
    "self_approved",
    "self_approved_with_review",
    "unassigned_peer_approved",
    "assigned_peer_approved"
  ];

  return map(records, rec => {
    let total = 0;
    forEach(dataKeys, key => {
      total += (rec as any)?.[key] ?? 0;
    });

    let newRec: any = cloneDeep(rec);
    forEach(dataKeys, key => {
      newRec[key] = (newRec[key] / total) * 100.0;
    });

    return newRec;
  });
};

// Sorting the list after excluding the provided the keys
export const reviewCollaborationPartialListSorting = (
  userList: { total_prs: number; key: string }[],
  orderByKey: string,
  order: "desc" | "asc",
  ignoredKeys: string[]
) => {
  const ignoredKeysList = userList?.filter(rec => ignoredKeys.includes(rec?.key));
  const acceptedList = orderBy(
    userList?.filter(rec => !ignoredKeys.includes(rec?.key)),
    orderByKey,
    order
  );
  return [...ignoredKeysList, ...acceptedList];
};

export const getSCMReviewCollabSlicedUserList = (
  userList: SCMCollabUsersConfigType[],
  type: RemainingUsersKeys.OTHER_REVIEWER | RemainingUsersKeys.OTHER_COMMITTER,
  slicingFactor: number,
  overAllPrs?: number
) => {
  const usersLength = userList?.length;

  if (usersLength > slicingFactor) {
    const newUserList = userList.slice(0, Math.min(slicingFactor, usersLength));
    const remainingUsers = getRemainingUsersConfig(userList?.slice(Math.min(slicingFactor, usersLength), usersLength));
    let percent = 0,
      extra = {};

    if (overAllPrs) {
      percent = Math.round((remainingUsers?.total_prs * 100.0) / overAllPrs);
      extra = {
        overallPrs: overAllPrs,
        reviewerPRsPercent: percent === 0 ? "<1" : percent
      };
    }

    newUserList.push({
      name:
        type === RemainingUsersKeys.OTHER_COMMITTER
          ? RemainingUsersKeys.LABEL_OTHER_COMMITTERS
          : RemainingUsersKeys.LABEL_OTHER_REVIEWERS,
      key: type,
      ...remainingUsers,
      ...extra
    } as SCMCollabUsersConfigType);

    return newUserList;
  }

  return userList;
};
