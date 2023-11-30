import React from "react";
import {
  AntBadge,
  AntCol,
  AntProgress,
  AntRow,
  AntTag,
  AntText,
  SeverityTag,
  AvatarWithText,
  NameAvatarList,
  AntTooltip
} from "shared-resources/components";
import { JiraLeadTimeTableConfig } from "dashboard/pages/dashboard-tickets/configs";
import { AcceptanceTimeUnit } from "classes/RestVelocityConfigs";
import { getTimeAndIndicator, convertToDay } from "custom-hooks/helpers/leadTime.helper";
//import { render } from "react-dom";
// @ts-ignore

export const leadTimeByStageColumns = [
  // ...JiraLeadTimeTableConfig,
  {
    title: "Jira Ticket Id",
    key: "additional_key",
    dataIndex: "additional_key",
    width: "128",
    fixed: "left"
  },
  {
    title: "Summary",
    key: "title",
    dataIndex: "title",
    width: "128"
  },
  {
    title: "Lead Time",
    key: "total",
    dataIndex: "total",
    width: "180",
    render: (value, record, index) => {
      const time = getTimeAndIndicator(convertToDay(record.total, AcceptanceTimeUnit.SECONDS), 10, 11);
      return (
        <div className={`total-lead-time-column-${time.rating}`}>
          {time.duration} {time.unit}
        </div>
      );
    }
  },
  {
    title: "To Do",
    key: "todo.value",
    dataIndex: "todo.value",
    width: "180",
    align: "right",
    render: (item, record, index) => {
      let badgeType = "";
      switch (record?.todo?.rating) {
        case "GOOD": {
          badgeType = "success";
          break;
        }
        case "SLOW": {
          badgeType = "warning";
          break;
        }
        case "ACCEPTABLE": {
          badgeType = "processing";
          break;
        }
        default: {
          badgeType = "default";
          break;
        }
      }
      return <AntBadge status={badgeType} text={record?.todo?.value} />;
    }
  },
  {
    title: "Lead time to First Commit",
    key: "LeadtimetoFirstCommit.value",
    dataIndex: "LeadtimetoFirstCommit.value",
    width: "180",
    align: "right",
    render: (item, record, index) => {
      let badgeType = "";
      switch (record?.LeadtimetoFirstCommit?.rating) {
        case "GOOD": {
          badgeType = "success";
          break;
        }
        case "SLOW": {
          badgeType = "warning";
          break;
        }
        case "ACCEPTABLE": {
          badgeType = "processing";
          break;
        }
        default: {
          badgeType = "default";
          break;
        }
      }
      return <AntBadge status={badgeType} text={record?.todo?.value} />;
    }
  },
  {
    title: "PR Creation Time",
    key: "PRCreationTime.value",
    dataIndex: "PRCreationTime.value",
    width: "180",
    align: "right",
    render: (item, record, index) => {
      let badgeType = "";
      switch (record?.PRCreationTime?.rating) {
        case "GOOD": {
          badgeType = "success";
          break;
        }
        case "SLOW": {
          badgeType = "warning";
          break;
        }
        case "ACCEPTABLE": {
          badgeType = "processing";
          break;
        }
        default: {
          badgeType = "default";
          break;
        }
      }
      return <AntBadge status={badgeType} text={record?.PRCreationTime?.value} />;
    }
  },
  {
    title: "Time to First Comment",
    key: "TimetoFirstComment.value",
    dataIndex: "TimetoFirstComment.value",
    width: "180",
    align: "right",
    render: (item, record, index) => {
      let badgeType = "";
      switch (record?.TimetoFirstComment?.rating) {
        case "GOOD": {
          badgeType = "success";
          break;
        }
        case "SLOW": {
          badgeType = "warning";
          break;
        }
        case "ACCEPTABLE": {
          badgeType = "processing";
          break;
        }
        default: {
          badgeType = "default";
          break;
        }
      }
      return <AntBadge status={badgeType} text={record?.TimetoFirstComment?.value} />;
    }
  },
  {
    title: "Approval Time",
    key: "ApprovalTime.value",
    dataIndex: "ApprovalTime.value",
    width: "180",
    align: "right",
    render: (item, record, index) => {
      let badgeType = "";
      switch (record?.ApprovalTime?.rating) {
        case "GOOD": {
          badgeType = "success";
          break;
        }
        case "SLOW": {
          badgeType = "warning";
          break;
        }
        case "ACCEPTABLE": {
          badgeType = "processing";
          break;
        }
        default: {
          badgeType = "default";
          break;
        }
      }
      return <AntBadge status={badgeType} text={record?.ApprovalTime?.value} />;
    }
  },
  {
    title: "Merge Time",
    key: "MergeTime.value",
    dataIndex: "MergeTime.value",
    width: "180",
    align: "right",
    render: (item, record, index) => {
      let badgeType = "";
      switch (record?.MergeTime?.rating) {
        case "GOOD": {
          badgeType = "success";
          break;
        }
        case "SLOW": {
          badgeType = "warning";
          break;
        }
        case "ACCEPTABLE": {
          badgeType = "processing";
          break;
        }
        default: {
          badgeType = "default";
          break;
        }
      }
      return <AntBadge status={badgeType} text={record?.MergeTime?.value} />;
    }
  },
  {
    title: "Ready for QA",
    key: "ReadyforQA.value",
    dataIndex: "ReadyforQA.value",
    width: "180",
    align: "right",
    render: (item, record, index) => {
      let badgeType = "";
      switch (record?.ReadyforQA?.rating) {
        case "GOOD": {
          badgeType = "success";
          break;
        }
        case "SLOW": {
          badgeType = "warning";
          break;
        }
        case "ACCEPTABLE": {
          badgeType = "processing";
          break;
        }
        default: {
          badgeType = "default";
          break;
        }
      }
      return <AntBadge status={badgeType} text={record?.ReadyforQA?.value} />;
    }
  },
  {
    title: "Ready for Prod",
    key: "ReadyforProd.value",
    dataIndex: "ReadyforProd.value",
    width: "180",
    align: "right",
    render: (item, record, index) => {
      let badgeType = "";
      switch (record?.ReadyforProd?.rating) {
        case "GOOD": {
          badgeType = "success";
          break;
        }
        case "SLOW": {
          badgeType = "warning";
          break;
        }
        case "ACCEPTABLE": {
          badgeType = "processing";
          break;
        }
        default: {
          badgeType = "default";
          break;
        }
      }
      return <AntBadge status={badgeType} text={record?.ReadyforProd?.value} />;
    }
  },
  {
    title: "Pushed to Prod",
    key: "PushedtoProd.value",
    dataIndex: "PushedtoProd.value",
    width: "180",
    align: "right",
    render: (item, record, index) => {
      let badgeType = "";
      switch (record?.ReadyforProd?.rating) {
        case "GOOD": {
          badgeType = "success";
          break;
        }
        case "SLOW": {
          badgeType = "warning";
          break;
        }
        case "ACCEPTABLE": {
          badgeType = "processing";
          break;
        }
        default: {
          badgeType = "default";
          break;
        }
      }
      return <AntBadge status={badgeType} text={record?.ReadyforProd?.value} />;
    }
  }
];

/* export const drillDownData = {
    drilldown_data: {
        "91997674-da67-11ec-9d64-0242ac120002": {
            "f2c2700e-1b03-4c1c-b96e-1429948956e2": {
                Data: [
                    {
                        "key": "57b468a7-4658-4793-8045-1bef88ec5a16",
                        "additional_key": "SEP-78406",
                        "repo_ids": [],
                        "title": "[SymQual][Crash][14.3-RU4] Tse-14.3.7388.4000 [vsfF26CFCFC]",
                        "total": 11907280,
                        "data": [
                            {
                                "key": "TO DO",
                                "additional_key": "Ticket Status Changed from OPEN",
                                "median": 11907195,
                                "mean": 11907195,
                                "velocity_stage_result": {
                                    "lower_limit_value": 4,
                                    "lower_limit_unit": "DAYS",
                                    "upper_limit_value": 11,
                                    "upper_limit_unit": "DAYS",
                                    "rating": "SLOW"
                                },
                                "url": "",
                            },
                            {
                                "key": "Lead time to First Commit",
                                "additional_key": "Ticket Status Changed from TO DO TO FIRST COMMIT",
                                "median": 85,
                                "mean": 85,
                                "velocity_stage_result": {
                                    "lower_limit_value": 1,
                                    "lower_limit_unit": "DAYS",
                                    "upper_limit_value": 5,
                                    "upper_limit_unit": "DAYS",
                                    "rating": "GOOD"
                                }
                            },
                            {
                                "key": "PR Creation Time",
                                "additional_key": "PR Creation Time",
                                "velocity_stage_result": {
                                    "lower_limit_value": 864000,
                                    "lower_limit_unit": "SECONDS",
                                    "upper_limit_value": 2592000,
                                    "upper_limit_unit": "SECONDS",
                                    "rating": "MISSING"
                                }
                            },
                            {
                                "key": "Time to First Comment",
                                "additional_key": "Time to First Comment",
                                "velocity_stage_result": {
                                    "lower_limit_value": 864000,
                                    "lower_limit_unit": "SECONDS",
                                    "upper_limit_value": 2592000,
                                    "upper_limit_unit": "SECONDS",
                                    "rating": "MISSING"
                                }
                            },
                            {
                                "key": "Approval Time",
                                "additional_key": "Approval Time",
                                "velocity_stage_result": {
                                    "lower_limit_value": 864000,
                                    "lower_limit_unit": "SECONDS",
                                    "upper_limit_value": 2592000,
                                    "upper_limit_unit": "SECONDS",
                                    "rating": "MISSING"
                                }
                            },
                            {
                                "key": "Merge Time",
                                "additional_key": "Merge Time",
                                "velocity_stage_result": {
                                    "lower_limit_value": 864000,
                                    "lower_limit_unit": "SECONDS",
                                    "upper_limit_value": 2592000,
                                    "upper_limit_unit": "SECONDS",
                                    "rating": "MISSING"
                                }
                            },
                            {
                                "key": "ready for QA",
                                "additional_key": "ready for QA",
                                "velocity_stage_result": {
                                    "lower_limit_value": 864000,
                                    "lower_limit_unit": "SECONDS",
                                    "upper_limit_value": 2592000,
                                    "upper_limit_unit": "SECONDS",
                                    "rating": "MISSING"
                                }
                            },
                            {
                                "key": "Ready for Prod",
                                "additional_key": "Ready for Prod",
                                "velocity_stage_result": {
                                    "lower_limit_value": 864000,
                                    "lower_limit_unit": "SECONDS",
                                    "upper_limit_value": 2592000,
                                    "upper_limit_unit": "SECONDS",
                                    "rating": "MISSING"
                                }
                            },
                            {
                                "key": "Pushed to Prod",
                                "additional_key": "Pushed to Prod",
                                "velocity_stage_result": {
                                    "lower_limit_value": 864000,
                                    "lower_limit_unit": "SECONDS",
                                    "upper_limit_value": 2592000,
                                    "upper_limit_unit": "SECONDS",
                                    "rating": "MISSING"
                                }
                            }
                        ],
                        "integration_id": "1"
                    },
                    {
                        "key": "e20f0cd2-8af5-48f3-9028-c90ced2e6006",
                        "additional_key": "SEP-79808",
                        "repo_ids": [],
                        "title": "ccIPC is much slower in RU5 compared to RU4",
                        "total": 7050439,
                        "data": [
                            {
                                "key": "Backlog",
                                "additional_key": "Ticket Status Changed from OPEN",
                                "median": 7050439,
                                "mean": 7050439,
                                "velocity_stage_result": {
                                    "lower_limit_value": 4,
                                    "lower_limit_unit": "DAYS",
                                    "upper_limit_value": 11,
                                    "upper_limit_unit": "DAYS",
                                    "rating": "SLOW"
                                }
                            },
                            {
                                "key": "Development",
                                "additional_key": "Ticket Status Changed from IN PROGRESS",
                                "velocity_stage_result": {
                                    "lower_limit_value": 1,
                                    "lower_limit_unit": "DAYS",
                                    "upper_limit_value": 5,
                                    "upper_limit_unit": "DAYS",
                                    "rating": "MISSING"
                                }
                            },
                            {
                                "key": "Lead time to first commit",
                                "additional_key": "First Commit",
                                "velocity_stage_result": {
                                    "lower_limit_value": 864000,
                                    "lower_limit_unit": "SECONDS",
                                    "upper_limit_value": 2592000,
                                    "upper_limit_unit": "SECONDS",
                                    "rating": "MISSING"
                                }
                            },
                            {
                                "key": "PR creation time",
                                "additional_key": "Pull Request Created",
                                "velocity_stage_result": {
                                    "lower_limit_value": 864000,
                                    "lower_limit_unit": "SECONDS",
                                    "upper_limit_value": 2592000,
                                    "upper_limit_unit": "SECONDS",
                                    "rating": "MISSING"
                                }
                            },
                            {
                                "key": "Time to First Comment",
                                "additional_key": "Pull Request Review Started",
                                "velocity_stage_result": {
                                    "lower_limit_value": 864000,
                                    "lower_limit_unit": "SECONDS",
                                    "upper_limit_value": 2592000,
                                    "upper_limit_unit": "SECONDS",
                                    "rating": "MISSING"
                                }
                            },
                            {
                                "key": "Approval Time",
                                "additional_key": "Pull Request Approved",
                                "velocity_stage_result": {
                                    "lower_limit_value": 864000,
                                    "lower_limit_unit": "SECONDS",
                                    "upper_limit_value": 2592000,
                                    "upper_limit_unit": "SECONDS",
                                    "rating": "MISSING"
                                }
                            },
                            {
                                "key": "Merge Time",
                                "additional_key": "Pull Request Merged",
                                "velocity_stage_result": {
                                    "lower_limit_value": 864000,
                                    "lower_limit_unit": "SECONDS",
                                    "upper_limit_value": 2592000,
                                    "upper_limit_unit": "SECONDS",
                                    "rating": "MISSING"
                                }
                            }
                        ]
                    }
                ]
            }
        }
    }
}; */

//not using this, now using mapping mockfile, delete it later
export const drillDownData = {
  drilldown_data: {
    "91997674-da67-11ec-9d64-0242ac120002": {
      "f2c2700e-1b03-4c1c-b96e-1429948956e2": {
        Data: [
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a16",
            additional_key: "SEP-78406",
            url: "",
            repo_ids: [],
            title: "[SymQual][Crash][14.3-RU4] Tse-14.3.7388.4000 [vsfF26CFCFC]",
            total: 11907280,
            todo: {
              value: 5,
              rating: "GOOD"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a16",
            additional_key: "SEP-78406",
            url: "",
            repo_ids: [],
            title: "ccIPC is much slower in RU5 compared to RU4",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a18",
            additional_key: "SEP-78409",
            url: "",
            repo_ids: [],
            title: "ccIPC is much slower in POUYT",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a18",
            additional_key: "SEP-78404",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a20",
            additional_key: "SEP-78402",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a53",
            additional_key: "SEP-78493",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: 11907195,
            LeadtimetoFirstCommit: 90,
            PRCreationTime: 20,
            TimetoFirstComment: 30,
            ApprovalTime: 40,
            MergeTime: 0,
            ReadyforQA: 50,
            ReadyforProd: 40,
            PushedtoProd: 30,
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a02",
            additional_key: "SEP-78430",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a20",
            additional_key: "SEP-78452",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a10",
            additional_key: "SEP-78402",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 42,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a40",
            additional_key: "SEP-78427",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 10,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          }
        ]
      },
      "f0640bf8-afa8-41b2-a475-db44080313e4": {
        Data: [
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a16",
            additional_key: "SEP-78408",
            url: "",
            repo_ids: [],
            title: "[SymQual][Crash][14.3-RU4] Tse-14.3.7388.4000 [vsfF26CFCFC]",
            total: 907280,
            todo: {
              value: 5,
              rating: "GOOD"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 3,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 7,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a16",
            additional_key: "SEP-78408",
            url: "",
            repo_ids: [],
            title: "ccIPC is much slower in RU5 compared to RU4",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a18",
            additional_key: "SEP-78409",
            url: "",
            repo_ids: [],
            title: "ccIPC is much slower in POUYT",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a18",
            additional_key: "SEP-78404",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a20",
            additional_key: "SEP-78402",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a53",
            additional_key: "SEP-78493",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: 11907195,
            LeadtimetoFirstCommit: 90,
            PRCreationTime: 20,
            TimetoFirstComment: 30,
            ApprovalTime: 40,
            MergeTime: 0,
            ReadyforQA: 50,
            ReadyforProd: 40,
            PushedtoProd: 30,
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a02",
            additional_key: "SEP-78430",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a20",
            additional_key: "SEP-78452",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a10",
            additional_key: "SEP-78402",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 42,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a40",
            additional_key: "SEP-78427",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 10,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          }
        ]
      },
      "d75ecba6-8b27-4e93-987c-75114b61dc66": {
        Data: [
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a16",
            additional_key: "SEP-78407",
            url: "",
            repo_ids: [],
            title: "[SymQual][Crash][14.3-RU4] Tse-14.3.7388.4000 [vsfF26CFCFC]",
            total: 21907280,
            todo: {
              value: 5,
              rating: "GOOD"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 7,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a16",
            additional_key: "SEP-78408",
            url: "",
            repo_ids: [],
            title: "ccIPC is much slower in RU5 compared to RU4",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a18",
            additional_key: "SEP-78409",
            url: "",
            repo_ids: [],
            title: "ccIPC is much slower in POUYT",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a18",
            additional_key: "SEP-78404",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a20",
            additional_key: "SEP-78402",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a53",
            additional_key: "SEP-78493",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: 11907195,
            LeadtimetoFirstCommit: 90,
            PRCreationTime: 20,
            TimetoFirstComment: 30,
            ApprovalTime: 40,
            MergeTime: 0,
            ReadyforQA: 50,
            ReadyforProd: 40,
            PushedtoProd: 30,
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a02",
            additional_key: "SEP-78430",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a20",
            additional_key: "SEP-78452",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 2,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a10",
            additional_key: "SEP-78402",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 42,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          },
          {
            key: "57b468a7-4658-4793-8045-1bef88ec5a40",
            additional_key: "SEP-78427",
            url: "",
            repo_ids: [],
            title: "Test",
            total: 11907280,
            todo: {
              value: 10,
              rating: "SLOW"
            },
            LeadtimetoFirstCommit: {
              value: 1,
              rating: "SLOW"
            },
            PRCreationTime: {
              value: 5,
              rating: "ACCEPTABLE"
            },
            TimetoFirstComment: {
              value: 5,
              rating: "MISSING"
            },
            ApprovalTime: {
              value: 5,
              rating: "GOOD"
            },
            MergeTime: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforQA: {
              value: 5,
              rating: "GOOD"
            },
            ReadyforProd: {
              value: 0.5,
              rating: "SLOW"
            },
            PushedtoProd: {
              value: 2,
              rating: "NOT ACCEPTABLE"
            },
            integration_id: "1"
          }
        ]
      }
    }
  }
};
