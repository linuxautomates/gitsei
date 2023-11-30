import { Divider } from "antd";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { SCMReviewCollaborationReviewsConfig } from "dashboard/dashboard-types/scmReports.types";
import { SCMReviewCollaborationStateType } from "dashboard/reports/scm/scm-review-collaboration/scm-review-collaboration-report.enum";
import { forEach, get, map, maxBy, orderBy } from "lodash";
import React, { useCallback, useContext, useMemo, useState } from "react";
import { AntSelect, EmptyWidget } from "shared-resources/components";
import Xarrow from "react-xarrows";
import useId from "custom-hooks/useId";
import { WidgetFilterContext } from "dashboard/pages/context";
import {
  collabLegendMapping,
  collabStateColorMapping,
  MAX_LENGTH_TO_SHOW,
  RemainingUsersKeys,
  SCMCollabSortingValues,
  SCMReviewCollaborationCustomStateType
} from "shared-resources/charts/scm-review-sankey-chart/constant";
import {
  CommitterReviewerConnectionType,
  SCMCollabUsersConfigType
} from "shared-resources/charts/scm-review-sankey-chart/scm-review-chart.types";
import {
  getSCMReviewCollabSlicedUserList,
  reviewCollaborationPartialListSorting,
  SCMReviewListToPercentageList
} from "shared-resources/charts/scm-review-sankey-chart/scm-review-collaboration.helper";
import SCMReviewTotalBreakdownComponent from "shared-resources/charts/scm-review-sankey-chart/scm-review-total-breakdown.component";
import CollabItem from "shared-resources/charts/scm-review-sankey-chart/CollabItem";
import SCMReviewUserListHeaderActionComponent from "shared-resources/charts/scm-review-sankey-chart/scm-user-list-header.component";
import "./demo-scm-sanky-chart.style.scss";
import { DemoScmSankyChartProps } from "../Widget-Grapg-Types/demo-scm-sanky.types";
import SCMReviewCollborationFooterComponent from "shared-resources/charts/scm-review-sankey-chart/scm-review-collaboration.footer";

const DemoScmReviewSankeyChartComponent: React.FC<DemoScmSankyChartProps> = (props: DemoScmSankyChartProps) => {
  const { data: transformed, onClick, id } = props;
  const { submittersList, reviewersList, apiData, totalBreakdowndata } = transformed;
  const _totalBreakdowndata = Array.isArray(totalBreakdowndata)
    ? totalBreakdowndata
        .sort((a: any, b: any) => a?.order - b?.order)
        .reduce((acc: any, next: any) => {
          return { ...acc, [next.key]: next.value };
        }, {})
    : totalBreakdowndata;
  const { setFilters, filters } = useContext(WidgetFilterContext);
  const [showBreakdown, setShowBreakdown] = useState<{ [x: string]: boolean }>();
  const modifiedCollabMapping = useMemo(() => {
    const approvalStatus = get(filters, [props?.id, "collab_states"], []).map((item: any) =>
      item?.replaceAll("-", "_")
    );
    return Object.keys(collabLegendMapping || {}).reduce((acc: any, next: any) => {
      const value = !approvalStatus.length || approvalStatus?.includes(next) ? true : false;
      return { ...acc, [next]: value };
    }, {});
  }, [filters]);
  const uncheckLegend = Object.values(modifiedCollabMapping || {})?.includes(false);
  const [legendMapping, setLegendMapping] = useState<basicMappingType<boolean>>(modifiedCollabMapping);
  const [reviewersSlicingFactor, setReviewersSlicingFactor] = useState<number>(MAX_LENGTH_TO_SHOW);
  const [submittersSlicingFactor, setSubmittersSlicingFactor] = useState<number>(MAX_LENGTH_TO_SHOW);
  const [submittersSortingValue, setSubmittersSortingValue] = useState<string>(SCMCollabSortingValues.MOST_SUBMITTED);
  const [reviewersSortingValue, setReviewersSortingValue] = useState<string>(SCMCollabSortingValues.MOST_APPROVED);
  const maxSubmitterValue = useMemo(
    () => (maxBy(submittersList, "total_prs") as any)?.total_prs || 0,
    [submittersList]
  );
  const maxReviewerValue = useMemo(() => (maxBy(reviewersList, "total_prs") as any)?.total_prs || 0, [reviewersList]);
  const UNIQUE_ID = useId();

  const collabOnStates = useMemo(() => {
    return Object.keys(legendMapping).filter(key => legendMapping[key]);
  }, [legendMapping]);

  const disableReviewersSorting = useMemo(() => {
    const key = Object.keys(showBreakdown ?? { key: false })[0];
    return (showBreakdown ?? {})[key] && key?.includes("submitter");
  }, [showBreakdown]);

  const disableSubmittersSorting = useMemo(() => {
    const key = Object.keys(showBreakdown ?? { key: false })[0];
    return (showBreakdown ?? {})[key] && key?.includes("reviewer");
  }, [showBreakdown]);

  const sortedReviewerList = useMemo(
    () =>
      orderBy(
        reviewersList,
        "total_prs",
        reviewersSortingValue === SCMCollabSortingValues.MOST_APPROVED ? "desc" : "asc"
      ),
    [reviewersList, reviewersSortingValue]
  );

  const sortedSubmittersList = useMemo(
    () =>
      orderBy(
        submittersList,
        "total_prs",
        submittersSortingValue === SCMCollabSortingValues.MOST_SUBMITTED ? "desc" : "asc"
      ),
    [submittersList, submittersSortingValue]
  );

  const finalSubmittersList = useMemo(() => {
    const key = Object.keys(showBreakdown || {})?.[0] || "";

    if (showBreakdown?.[key] === true && key?.includes("reviewer")) {
      const startIndex = parseInt(key.split("_")[0]);
      const startData = sortedReviewerList[startIndex];
      let totalPrs = 0;
      let onHoverUsersList: SCMCollabUsersConfigType[] = [];

      const connectionsToCommitters: CommitterReviewerConnectionType = {};

      forEach(apiData, apiRec => {
        const existsForCurSubmitter = (apiRec?.stacks ?? []).find(
          (stackData: SCMReviewCollaborationReviewsConfig) => stackData?.key === startData?.key
        );
        if (existsForCurSubmitter) {
          connectionsToCommitters[apiRec?.additional_key] = {
            ...connectionsToCommitters[apiRec?.additional_key],
            [apiRec?.collab_state]: existsForCurSubmitter.count,
            key: apiRec?.key
          };
        }
      });
      forEach(Object.keys(connectionsToCommitters), key => {
        const data = connectionsToCommitters[key];
        totalPrs = 0;

        if (data?.key === startData?.key) {
          if (collabOnStates.includes(SCMReviewCollaborationCustomStateType.SELF_APPROVED)) {
            // Getting PRs percent
            const total = startData?.total_prs;
            let percent = 0;
            if (total) {
              percent = Math.round(
                (((data?.[SCMReviewCollaborationStateType.SELF_APPROVED] as number) ?? 0) * 100.0) / total
              );
            }

            // adding unsupported at the top
            onHoverUsersList = onHoverUsersList.reverse();
            if (data?.[SCMReviewCollaborationStateType.SELF_APPROVED_WITH_REVIEW]) {
              onHoverUsersList.push({
                name: "Self Approved With Review",
                key: SCMReviewCollaborationCustomStateType.SELF_APPROVED,
                overallPrs: startData?.total_prs,
                reviewerPRsPercent: percent === 0 ? "<1" : percent,
                total_prs: data?.[SCMReviewCollaborationStateType.SELF_APPROVED_WITH_REVIEW] as number,
                self_approved_with_review: data?.[SCMReviewCollaborationStateType.SELF_APPROVED_WITH_REVIEW] as number,
                unapproved: 0,
                self_approved: 0,
                unassigned_peer_approved: 0,
                assigned_peer_approved: 0
              });
            } else {
              onHoverUsersList.push({
                name: "Self Approved",
                key: SCMReviewCollaborationCustomStateType.SELF_APPROVED,
                overallPrs: startData?.total_prs,
                reviewerPRsPercent: percent === 0 ? "<1" : percent,
                total_prs: data?.[SCMReviewCollaborationStateType.SELF_APPROVED] as number,
                self_approved: data?.[SCMReviewCollaborationStateType.SELF_APPROVED] as number,
                unapproved: 0,
                self_approved_with_review: 0,
                unassigned_peer_approved: 0,
                assigned_peer_approved: 0
              });
            }

            onHoverUsersList = onHoverUsersList.reverse();
          }
        } else {
          forEach(Object.keys(data), state => {
            if (state !== "key") {
              totalPrs += data?.[state] as number;
            }
          });

          // Getting PRs percent
          const total = startData?.total_prs;
          let percent = 0;
          if (total) {
            percent = Math.round((totalPrs * 100.0) / total);
          }

          let showCurCommitter = false;
          forEach(Object.keys(data), key => {
            if (collabOnStates.includes(key?.replaceAll("-", "_"))) {
              showCurCommitter = true;
            }
          });

          if (showCurCommitter) {
            onHoverUsersList.push({
              name: key,
              key: data?.key as string,
              total_prs: totalPrs,
              overallPrs: startData?.total_prs,
              reviewerPRsPercent: percent === 0 ? "<1" : percent,
              unapproved: (data?.[SCMReviewCollaborationStateType.UNAPPROVED] as number) ?? 0,
              self_approved: (data?.[SCMReviewCollaborationStateType.SELF_APPROVED] as number) ?? 0,
              self_approved_with_review:
                (data?.[SCMReviewCollaborationStateType.SELF_APPROVED_WITH_REVIEW] as number) ?? 0,
              unassigned_peer_approved:
                (data?.[SCMReviewCollaborationStateType.UNASSIGNED_PEER_APPROVED] as number) ?? 0,
              assigned_peer_approved: (data?.[SCMReviewCollaborationStateType.ASSIGNED_PEER_APPROVED] as number) ?? 0
            } as SCMCollabUsersConfigType);
          }
        }
      });

      onHoverUsersList = reviewCollaborationPartialListSorting(
        onHoverUsersList,
        "total_prs",
        submittersSortingValue === SCMCollabSortingValues.MOST_SUBMITTED ? "desc" : "asc",
        ["self_approved"]
      ) as SCMCollabUsersConfigType[];

      return SCMReviewListToPercentageList(onHoverUsersList);
    }

    return sortedSubmittersList;
  }, [showBreakdown, apiData, sortedReviewerList, sortedSubmittersList, collabOnStates, submittersSortingValue]);
  const finalReviewerList = useMemo(() => {
    const key = Object.keys(showBreakdown || {})?.[0] || "";
    if (showBreakdown?.[key] === true && key?.includes("submitter")) {
      const startIndex = parseInt(key.split("_")[0]);
      const startData = sortedSubmittersList[startIndex];
      let totalPrs = 0;
      let onHoverUsersList: SCMCollabUsersConfigType[] = [];

      const allRecordsOfCurSubm = (apiData ?? []).filter((apiRec: any) => apiRec?.key === startData?.key);
      const connectionsToReviewer: CommitterReviewerConnectionType = {};

      forEach(allRecordsOfCurSubm, rec => {
        let collabState = rec?.collab_state;
        forEach(rec?.stacks ?? [], stackData => {
          connectionsToReviewer[stackData?.additional_key] = {
            ...connectionsToReviewer[stackData?.additional_key],
            [collabState]: stackData?.count,
            key: stackData?.key
          };
        });
      });

      forEach(Object.keys(connectionsToReviewer), key => {
        const data = connectionsToReviewer[key];
        totalPrs = 0;
        if (key === "NONE") {
          if (collabOnStates.includes(SCMReviewCollaborationStateType.UNAPPROVED)) {
            // Getting PRs percent
            const total = startData?.total_prs;
            let percent = 0;
            if (total) {
              percent = Math.round(
                (((data?.[SCMReviewCollaborationStateType.UNAPPROVED] as number) ?? 0) * 100.0) / total
              );
            }

            // adding unsupported at the top
            onHoverUsersList = onHoverUsersList.reverse();
            onHoverUsersList.push({
              name: "Unapproved",
              key: SCMReviewCollaborationStateType.UNAPPROVED,
              overallPrs: startData?.total_prs,
              reviewerPRsPercent: percent === 0 ? "<1" : percent,
              total_prs: data?.[SCMReviewCollaborationStateType.UNAPPROVED] as number,
              unapproved: data?.[SCMReviewCollaborationStateType.UNAPPROVED] as number,
              self_approved: 0,
              self_approved_with_review: 0,
              unassigned_peer_approved: 0,
              assigned_peer_approved: 0
            });
            onHoverUsersList = onHoverUsersList.reverse();
          }
        } else {
          forEach(Object.keys(data), state => {
            if (state !== "key") {
              totalPrs += data?.[state] as number;
            }
          });

          // Getting PRs percent
          const total = startData?.total_prs;
          let percent = 0;
          if (total) {
            percent = Math.round((totalPrs * 100.0) / total);
          }

          let showCurReviwer = false;
          forEach(Object.keys(data), key => {
            if (collabOnStates.includes(key?.replaceAll("-", "_"))) {
              showCurReviwer = true;
            }
          });

          if (showCurReviwer) {
            onHoverUsersList.push({
              name: key,
              key: data?.key as string,
              total_prs: totalPrs,
              overallPrs: startData?.total_prs,
              reviewerPRsPercent: percent === 0 ? "<1" : percent,
              unapproved: (data?.[SCMReviewCollaborationStateType.UNAPPROVED] as number) ?? 0,
              self_approved: (data?.[SCMReviewCollaborationStateType.SELF_APPROVED] as number) ?? 0,
              self_approved_with_review:
                (data?.[SCMReviewCollaborationStateType.SELF_APPROVED_WITH_REVIEW] as number) ?? 0,
              unassigned_peer_approved:
                (data?.[SCMReviewCollaborationStateType.UNASSIGNED_PEER_APPROVED] as number) ?? 0,
              assigned_peer_approved: (data?.[SCMReviewCollaborationStateType.ASSIGNED_PEER_APPROVED] as number) ?? 0
            } as SCMCollabUsersConfigType);
          }
        }
      });

      onHoverUsersList = reviewCollaborationPartialListSorting(
        onHoverUsersList,
        "total_prs",
        reviewersSortingValue === SCMCollabSortingValues.MOST_APPROVED ? "desc" : "asc",
        ["unapproved"]
      ) as SCMCollabUsersConfigType[];

      return SCMReviewListToPercentageList(onHoverUsersList);
    }

    return sortedReviewerList;
  }, [showBreakdown, sortedReviewerList, sortedSubmittersList, apiData, collabOnStates, reviewersSortingValue]);

  const handleSubmittersSortSelect = (value: string) => {
    setSubmittersSortingValue(value);
  };

  const handleReviewersSortSelect = (value: string) => {
    setReviewersSortingValue(value);
  };

  const handleOnClick = useCallback((key: "submitter" | "reviewer", value: string, label?: string) => {
    if (key === "reviewer") {
      onClick?.({
        phaseId: value,
        name: label ?? value,
        titlePrefix: "Reviewer",
        widgetId: id
      });
      return;
    }
    onClick?.({ phaseId: value, name: label ?? value, titlePrefix: "Committer", widgetId: id });
  }, []);

  const slicedSubmittersList = useMemo(() => {
    const key = Object.keys(showBreakdown || {})?.[0] || "";
    if (showBreakdown?.[key] === true && key?.includes("reviewer")) {
      const startIndex = parseInt(key.split("_")[0]);
      const startData = sortedSubmittersList[startIndex];
      return getSCMReviewCollabSlicedUserList(
        finalSubmittersList,
        RemainingUsersKeys.OTHER_COMMITTER,
        submittersSlicingFactor,
        startData?.total_prs
      );
    }
    return getSCMReviewCollabSlicedUserList(
      finalSubmittersList,
      RemainingUsersKeys.OTHER_COMMITTER,
      submittersSlicingFactor
    );
  }, [showBreakdown, sortedSubmittersList, finalSubmittersList, submittersSlicingFactor]);

  const slicedReviewersList = useMemo(() => {
    const key = Object.keys(showBreakdown || {})?.[0] || "";
    if (showBreakdown?.[key] === true && key?.includes("submitter")) {
      const startIndex = parseInt(key.split("_")[0]);
      const startData = sortedReviewerList[startIndex];
      return getSCMReviewCollabSlicedUserList(
        finalReviewerList,
        RemainingUsersKeys.OTHER_REVIEWER,
        reviewersSlicingFactor,
        startData?.total_prs
      );
    }
    return getSCMReviewCollabSlicedUserList(
      finalReviewerList,
      RemainingUsersKeys.OTHER_REVIEWER,
      reviewersSlicingFactor
    );
  }, [showBreakdown, sortedReviewerList, finalReviewerList, reviewersSlicingFactor]);

  const linesData = useMemo(() => {
    const key = Object.keys(showBreakdown || {})?.[0] || "";
    if (showBreakdown?.[key] === true) {
      const startIndex = parseInt(key.split("_")[0]);
      const isReviewer = key?.includes("reviewer");
      /** if the clicked one is a reviewer then start list will be reviewers list */
      let startList = slicedSubmittersList;
      if (isReviewer) {
        startList = slicedReviewersList;
      }
      const startData = startList[startIndex]; // this is starting node.

      if (isReviewer) {
        const connectionsToCommitters: CommitterReviewerConnectionType = {};

        forEach(apiData, apiRec => {
          const existsForCurSubmitter = (apiRec?.stacks ?? []).find(
            (stackData: SCMReviewCollaborationReviewsConfig) => stackData?.key === startData?.key
          );
          if (existsForCurSubmitter) {
            connectionsToCommitters[apiRec?.additional_key] = {
              ...connectionsToCommitters[apiRec?.additional_key],
              [apiRec?.collab_state]: existsForCurSubmitter.count,
              key: apiRec?.key
            };
          }
        });

        const lines: any[] = [];

        forEach(Object.keys(connectionsToCommitters), key => {
          const data = connectionsToCommitters[key];
          if (data?.key === startData?.key) {
            if (collabOnStates.includes(SCMReviewCollaborationCustomStateType.SELF_APPROVED)) {
              lines.push({
                strokeValue: data?.[SCMReviewCollaborationStateType.SELF_APPROVED],
                strokeWidth: Math.min(10, data?.[SCMReviewCollaborationStateType.SELF_APPROVED] as number),
                color: collabStateColorMapping.self_approved,
                start: `${UNIQUE_ID}_${startData?.key}_reviewer`,
                end: `${UNIQUE_ID}_${SCMReviewCollaborationCustomStateType.SELF_APPROVED}`
              });
            }
          } else {
            const existsOnSlicedCommittersList = slicedSubmittersList.find(rec => rec?.key === data?.key);
            if (!!existsOnSlicedCommittersList) {
              forEach(Object.keys(data), state => {
                if (collabOnStates.includes(state?.replaceAll("-", "_"))) {
                  lines.push({
                    strokeValue: data?.[state],
                    strokeWidth: Math.min(10, data?.[state] as number),
                    color: (collabStateColorMapping as any)[state.replaceAll("-", "_")],
                    start: `${UNIQUE_ID}_${startData?.key}_reviewer`,
                    end: `${UNIQUE_ID}_${data?.key}`
                  });
                }
              });
            }
          }
        });

        // adding line for other committers
        if (
          slicedSubmittersList?.length &&
          slicedSubmittersList[slicedSubmittersList?.length - 1]?.key === RemainingUsersKeys.OTHER_COMMITTER
        ) {
          lines.push({
            strokeValue: 10,
            strokeWidth: 10,
            color: collabStateColorMapping.unassigned_peer_approved,
            start: `${UNIQUE_ID}_${startData?.key}_reviewer`,
            end: `${UNIQUE_ID}_${RemainingUsersKeys.OTHER_COMMITTER}`
          });
        }
        return lines;
      } else {
        const allRecordsOfCurSubm = (apiData ?? []).filter((apiRec: any) => apiRec?.key === startData?.key);
        const connectionsToReviewer: CommitterReviewerConnectionType = {};

        forEach(allRecordsOfCurSubm, rec => {
          let collabState = rec?.collab_state;
          forEach(rec?.stacks ?? [], stackData => {
            connectionsToReviewer[stackData?.additional_key] = {
              ...connectionsToReviewer[stackData?.additional_key],
              [collabState]: stackData?.count,
              key: stackData?.key
            };
          });
        });
        // Adding lines as per collab states for current committer
        const lines: any[] = [];
        forEach(Object.keys(connectionsToReviewer), key => {
          const data = connectionsToReviewer[key];
          if (key === "NONE") {
            if (collabOnStates.includes(SCMReviewCollaborationStateType.UNAPPROVED)) {
              lines.push({
                strokeValue: data?.[SCMReviewCollaborationStateType.UNAPPROVED],
                strokeWidth: Math.min(10, data?.[SCMReviewCollaborationStateType.UNAPPROVED] as number),
                color: collabStateColorMapping.unapproved,
                start: `${UNIQUE_ID}_${startData?.key}`,
                end: `${UNIQUE_ID}_${SCMReviewCollaborationStateType.UNAPPROVED}_reviewer`
              });
            }
          } else {
            const existsOnSlicedReviewList = slicedReviewersList.find(rec => rec?.key === data?.key);
            if (!!existsOnSlicedReviewList) {
              forEach(Object.keys(data), state => {
                if (collabOnStates.includes(state?.replaceAll("-", "_"))) {
                  lines.push({
                    strokeValue: data?.[state],
                    strokeWidth: Math.min(10, data?.[state] as number),
                    color: (collabStateColorMapping as any)[state.replaceAll("-", "_")],
                    start: `${UNIQUE_ID}_${startData?.key}`,
                    end: `${UNIQUE_ID}_${data?.key}_reviewer`
                  });
                }
              });
            }
          }
        });

        // adding line for other Reviewers
        if (
          slicedReviewersList?.length &&
          slicedReviewersList[slicedReviewersList?.length - 1]?.key === RemainingUsersKeys.OTHER_REVIEWER
        ) {
          lines.push({
            strokeValue: 10,
            strokeWidth: 10,
            color: collabStateColorMapping.unassigned_peer_approved,
            start: `${UNIQUE_ID}_${startData.key}`,
            end: `${UNIQUE_ID}_${RemainingUsersKeys.OTHER_REVIEWER}_reviewer`
          });
        }
        return lines;
      }
    }
  }, [apiData, slicedSubmittersList, slicedReviewersList, showBreakdown, collabOnStates]);

  const renderLines = useMemo(() => {
    if (linesData?.length) {
      return linesData
        .sort((a: any, b: any) => b.strokeWidth - a.strokeWidth)
        .map((data: any, index: number) => (
          <Xarrow
            key={index}
            color={data.color}
            strokeWidth={data.strokeWidth}
            start={data.start}
            end={data.end}
            showHead={false}
          />
        ));
    }
    return null;
  }, [linesData]);

  const isStart = useCallback(
    (dataKey: string) => {
      return !!linesData?.find((item: any) => item.start === dataKey);
    },
    [linesData]
  );

  const getCommittersHeaderPrefix = useMemo(() => {
    const key = Object.keys(showBreakdown || {})?.[0] || "";
    if (showBreakdown?.[key] === true && key?.includes("reviewer")) {
      const startIndex = parseInt(key.split("_")[0]);
      return `Committers for ${slicedReviewersList[startIndex]?.name ?? ""}`;
    }
    return "Committers";
  }, [showBreakdown, slicedReviewersList]);

  const getReviewersHeaderPrefix = useMemo(() => {
    const key = Object.keys(showBreakdown || {})?.[0] || "";
    if (showBreakdown?.[key] === true && key?.includes("submitter")) {
      const startIndex = parseInt(key.split("_")[0]);
      return `Reviewers for ${slicedSubmittersList[startIndex]?.name ?? ""}`;
    }
    return "Reviewers";
  }, [showBreakdown, slicedSubmittersList]);

  const getOnHoveReviewerListLen = useMemo(() => {
    let len = slicedReviewersList?.length;
    if (len && slicedReviewersList[len - 1]?.name === RemainingUsersKeys.LABEL_OTHER_REVIEWERS) {
      len -= 1;
    }
    return len;
  }, [slicedReviewersList]);

  const getOnHoveCommittersListLen = useMemo(() => {
    let len = slicedSubmittersList?.length;
    if (len && slicedSubmittersList[len - 1]?.name === RemainingUsersKeys.LABEL_OTHER_COMMITTERS) {
      len -= 1;
    }
    return len;
  }, [slicedSubmittersList]);

  const setLegendFilters = (filters: any) => {
    const filteredFilters = Object.keys(filters || {})
      .filter((item: string) => filters[item])
      .map((item: string) => item?.replaceAll("_", "-"));
    setFilters(props.id, { ...(filters as any)[props.id], collab_states: filteredFilters });
    setLegendMapping(filters);
  };

  if ((apiData || []).length === 0 && !uncheckLegend) return <EmptyWidget />;

  return (
    <div>
      <SCMReviewTotalBreakdownComponent totalBreakdown={_totalBreakdowndata} />
      <div className="scm_sankey_container">
        {(apiData || []).length !== 0 && renderLines}
        <div className="w-100 p-10 flex direction-column">
          {(apiData || []).length !== 0 ? (
            <div className="w-100 flex justify-space-between review-sankey-container">
              <div className="submitter-container flex direction-column" style={{ width: "42%" }}>
                <div className="submitters-header">
                  <span className="flex align-center header-wrapper">
                    <SCMReviewUserListHeaderActionComponent
                      setSlicingFactor={setSubmittersSlicingFactor}
                      prefix={getCommittersHeaderPrefix}
                      totalLength={finalSubmittersList?.length}
                      visualLength={getOnHoveCommittersListLen}
                      hovering={Object.values(showBreakdown ?? { key: false })[0]}
                    />
                  </span>
                  <span style={{ fontSize: 16, width: "50%" }}>
                    Sort By :
                    <AntSelect
                      disabled={disableSubmittersSorting}
                      options={[]}
                      onChange={handleSubmittersSortSelect}
                      value={submittersSortingValue}
                    />
                  </span>
                </div>
                <Divider style={{ margin: "0.5rem 0" }} />
                <div className={"list-container flex direction-column"}>
                  {map(slicedSubmittersList, (item, index) => {
                    return (
                      <div style={{ position: "relative" }} key={`${UNIQUE_ID}_${item?.key}`}>
                        <div
                          id={`${UNIQUE_ID}_${item.key}`}
                          style={{
                            zIndex: 1,
                            position: "absolute",
                            top: "30px",
                            height: "0px",
                            width: `calc(100% - 10px)`
                          }}
                        />
                        <div
                          key={index}
                          style={{ zIndex: 5 }}
                          onClick={() => {
                            if (![RemainingUsersKeys.OTHER_COMMITTER].includes(item?.key as any)) {
                              handleOnClick("submitter", item?.key, item?.name);
                            }
                          }}
                          onMouseEnter={() => {
                            setShowBreakdown({
                              [`${index}_submitter`]:
                                item?.name !== RemainingUsersKeys.LABEL_OTHER_COMMITTERS ? true : false
                            });
                          }}
                          onMouseLeave={() => {
                            setShowBreakdown({ [`${index}_submitter`]: false });
                          }}>
                          <CollabItem
                            index={index}
                            {...item}
                            collabOnStates={collabOnStates}
                            isStart={isStart(`${UNIQUE_ID}_${item.key}`)}
                            maxValue={maxSubmitterValue}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
              <div className="reviewer-container flex direction-column" style={{ width: "42%" }}>
                <div className="reviewers-header">
                  <span className="flex align-center header-wrapper">
                    <SCMReviewUserListHeaderActionComponent
                      setSlicingFactor={setReviewersSlicingFactor}
                      prefix={getReviewersHeaderPrefix}
                      totalLength={finalReviewerList?.length}
                      visualLength={getOnHoveReviewerListLen}
                      hovering={Object.values(showBreakdown ?? { key: false })[0]}
                    />
                  </span>
                  <span style={{ fontSize: 16, width: "50%" }}>
                    Sort By :
                    <AntSelect
                      disabled={disableReviewersSorting}
                      options={[]}
                      onChange={handleReviewersSortSelect}
                      value={reviewersSortingValue}
                    />
                  </span>
                </div>
                <Divider style={{ margin: "0.5rem 0" }} />
                <div className={"list-container flex direction-column"}>
                  {map(slicedReviewersList, (item, index) => {
                    return (
                      <div
                        style={{ position: "relative" }}
                        onClick={() => {
                          if (![RemainingUsersKeys.OTHER_COMMITTER].includes(item?.key as any)) {
                            handleOnClick("reviewer", item?.key, item?.name);
                          }
                        }}
                        onMouseEnter={() => {
                          setShowBreakdown({
                            [`${index}_reviewer`]:
                              item?.name !== RemainingUsersKeys.LABEL_OTHER_REVIEWERS ? true : false
                          });
                        }}
                        onMouseLeave={() => {
                          setShowBreakdown({ [`${index}_reviewer`]: false });
                        }}>
                        <div
                          id={`${UNIQUE_ID}_${item.key}_reviewer`}
                          style={{
                            zIndex: 1,
                            position: "absolute",
                            top: "30px",
                            left: "8px",
                            height: "0px",
                            width: `100px`
                          }}
                        />
                        <div style={{ zIndex: 5 }} key={index}>
                          <CollabItem
                            index={index}
                            {...item}
                            collabOnStates={collabOnStates}
                            isStart={isStart(`${UNIQUE_ID}_${item.key}_reviewer`)}
                            maxValue={maxReviewerValue}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          ) : (
            <EmptyWidget />
          )}
          <SCMReviewCollborationFooterComponent setLegendMapping={setLegendFilters} legendMapping={legendMapping} />
        </div>
      </div>
    </div>
  );
};

export default DemoScmReviewSankeyChartComponent;
