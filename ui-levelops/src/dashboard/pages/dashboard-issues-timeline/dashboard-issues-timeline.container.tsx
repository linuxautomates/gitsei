import { Avatar, Badge, Card, Divider, notification, Tag, Timeline, Tooltip, Typography } from "antd";
import Loader from "components/Loader/Loader";
import queryString from "query-string";
import React, { Component } from "react";
import { connect } from "react-redux";
import { mapPageSettingsDispatchToProps } from "reduxConfigs/maps/pagesettings.map";
import { AntBadge, AntCol, AntRow, AntTag, AntText, AntTitle } from "shared-resources/components";
import { v1 as uuid } from "uuid";
import { mapGenericToProps } from "reduxConfigs/maps/restapi";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { getData, getError, getLoading } from "../../../utils/loadingUtils";
import { get, uniq } from "lodash";
import "./dashboard-issues-timeline.scss";
import { getDaysAndTimeWithUnit } from "utils/timeUtils";
import { RBAC, DASHBOARD_ID_KEY } from "constants/localStorageKeys";
import SingleStatContainer from "./single-stat.container";
import { joinUrl, truncatedString } from "utils/stringUtils";
import { timeStampToValue } from "dashboard/components/dashboard-view-page-secondary-header/helper";
import EmptyWidgetComponent from "shared-resources/components/empty-widget/empty-widget.component";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { convertToSeconds, unixToDate } from "utils/dateUtils";

import moment from "moment";
import { IntegrationTypes } from "constants/IntegrationTypes";

const tagColorArray = ["red", "blue", "green", "orange", "purple", "cyan"];

const PRECISION_VALUE = 1;

const getInitials = (ss: string) => {
  // @ts-ignore
  let names = ss?.toString().split(" "),
    // @ts-ignore
    initials = names && (names[0]?.substring(0, 1) || "").toUpperCase();

  // @ts-ignore
  if (names?.length > 1) {
    // @ts-ignore
    initials += (names[names.length - 1]?.substring(0, 1) || "").toUpperCase();
  }
  return initials;
};

const statusRender = (value: { replace: (arg0: string, arg1: string) => void }) => {
  switch (value) {
    case "Done":
      return <AntBadge status="success" text={value} />;
    case "Closed":
      return <AntBadge status="error" text={value} />;
    case "In Progress":
      return <AntBadge status="processing" text={value.replace("_", " ")} />;
    default:
      return <AntBadge status="default" text={value} />;
  }
};

interface Props {
  genericList: (uri: string, method: string, filters: any, complete: any, id?: any) => void;
  genericGet: (uri: string, id: string, complete: any) => void;
  restapiClear: (uri: string, method: string, id?: string) => void;
  setPageSettings: (path: string, dashboard_settings: any) => void;
  rest_api?: any;
  location: any;
  match: any;
  history: any;
}

interface State {
  loading: boolean;
  key: string;
  integration_ids: Array<string>;
  integration_list_id: string;
  integration_loading: boolean;
  fields_loading: boolean;
  url: string;
  userType: string | null;
  dashboardId?: string;
  application: string;
}

class TicketBouncePage extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    const queries = queryString.parse(props.location.search);
    this.state = {
      loading: true,
      integration_loading: true,
      key: "",
      integration_ids: [],
      integration_list_id: "",
      fields_loading: true,
      url: "",
      userType: localStorage.getItem(RBAC),
      dashboardId: queries.dashboardId as string,
      application: ""
    };
    this.getFieldName = this.getFieldName.bind(this);
  }

  componentDidMount(): void {
    const queries = queryString.parse(this.props.location.search);
    if (queries.integration_id && queries.key) {
      let integrationIds = queries.integration_id;
      if (queries.integration_id.includes(",")) {
        //@ts-ignore
        integrationIds = queries.integration_id.split(",");
      } else {
        //@ts-ignore
        integrationIds = [queries.integration_id];
      }
      const filter = {
        filter: {
          integration_ids: integrationIds,
          keys: [queries.key]
        }
      };
      const integrationFilter = {
        filter: {
          integration_ids: integrationIds
        }
      };
      const newListId = uuid();

      this.setState(
        {
          loading: true,
          // @ts-ignore
          key: queries!.key,
          integration_list_id: newListId,
          //@ts-ignore
          integration_ids: integrationIds,
          integration_loading: true,
          fields_loading: true
        },
        () => {
          this.props.genericList("integrations", "list", integrationFilter, null, newListId);
        }
      );
    }
    const addToLocalStorage = window.isStandaloneApp
      ? getRBACPermission(PermeableMetrics.ADD_DASHBOARD_ID_TO_LOCALSTORAGE)
      : true;
    if (addToLocalStorage) {
      localStorage.setItem(DASHBOARD_ID_KEY, queries.dashboardId as string);
    }
  }

  componentWillUnmount(): void {
    this.props.restapiClear("jira_tickets", "list", this.state.key);
    this.props.restapiClear("integrations", "list", this.state.integration_list_id);
    this.props.restapiClear("jira_fields", "list", this.state.integration_list_id);
  }

  static getDerivedStateFromProps(props: Props, state: State) {
    let ticketLoading = state.loading;
    let integrationLoading = state.integration_loading;
    let fieldsLoading = state.fields_loading;
    let url = state.url;
    let application = state.application;
    if (state.loading) {
      const loading = getLoading(
        props.rest_api,
        application === IntegrationTypes.JIRA ? "jira_tickets" : "issue_management_list",
        "list",
        state.key
      );
      const error = getError(
        props.rest_api,
        application === IntegrationTypes.JIRA ? "jira_tickets" : "issue_management_list",
        "list",
        state.key
      );
      if (!loading && !error) {
        const data = getData(
          props.rest_api,
          application === IntegrationTypes.JIRA ? "jira_tickets" : "issue_management_list",
          "list",
          state.key
        );
        if (data && data.records && data.records.length > 0) {
          props.setPageSettings(props.location.pathname, {
            title: <AntTitle level={4}>Ticket Details - {state.key}</AntTitle>
          });
        }

        ticketLoading = false;
      }
    }

    if (state.integration_loading) {
      const loading = getLoading(props.rest_api, "integrations", "list", state.integration_list_id);
      const error = getError(props.rest_api, "integrations", "list", state.integration_list_id);
      if (!loading && !error) {
        integrationLoading = false;
        const data = getData(props.rest_api, "integrations", "list", state.integration_list_id);
        if (data) {
          const currentIntegration = data.records || [];
          url = currentIntegration?.[0]?.url || "";
          application = currentIntegration?.[0]?.application || "";
          const filter =
            application === IntegrationTypes.JIRA
              ? {
                  filter: {
                    integration_ids: state.integration_ids,
                    keys: [state.key]
                  }
                }
              : {
                  filter: {
                    integration_ids: state.integration_ids,
                    workitem_ids: [state.key]
                  }
                };
          props.genericList(
            application === IntegrationTypes.JIRA ? "jira_tickets" : "issue_management_list",
            "list",
            filter,
            null,
            state.key
          );
          props.genericList(
            application === IntegrationTypes.JIRA ? "jira_fields" : "issue_management_workItem_Fields_list",
            "list",
            { filter: { integration_id: state.integration_ids[0] } },
            null,
            state.integration_list_id
          );
        }
      }
    }

    if (state.fields_loading) {
      const uri = application === IntegrationTypes.JIRA ? "jira_fields" : "issue_management_workItem_Fields_list";
      const loading = getLoading(props.rest_api, uri, "list", state.integration_list_id);
      const error = getError(props.rest_api, uri, "list", state.integration_list_id);
      if (!loading && !error) {
        fieldsLoading = false;
      }
    }

    return {
      ...state,
      loading: ticketLoading,
      integration_loading: integrationLoading,
      fields_loading: fieldsLoading,
      url: url,
      application: application
    };
  }

  get pageData() {
    const data = getData(
      this.props.rest_api,
      this.state.application === IntegrationTypes.JIRA ? "jira_tickets" : "issue_management_list",
      "list",
      this.state.key
    );
    if (data && data.records) {
      if (data.records.length > 0) {
        return data.records[0];
      }
    }
    return [];
  }

  get customFields() {
    return get(
      this.props.rest_api,
      [
        this.state.application === IntegrationTypes.JIRA ? "jira_fields" : "issue_management_workItem_Fields_list",
        "list",
        this.state.integration_list_id,
        "data",
        "records"
      ],
      []
    );
  }

  getFieldName(field: string) {
    return this.customFields.find((customField: any) => customField.field_key === field);
  }

  getLength = (user: string) => {
    const data = this.pageData;
    const users = (data.assignee_list || []).reduce((acc: any[], obj: { assignee: any }) => {
      if (!acc.includes(obj)) {
        acc.push(obj.assignee);
      }
      return acc;
    }, []);
    return (users.indexOf(user) % 5) * 15 + "%";
  };

  get colorMap() {
    const data = this.pageData;
    const colorLen = tagColorArray.length;
    return (data.assignee_list || []).reduce(
      (acc: { [x: string]: string }, obj: { [key: string]: string }) => {
        const users = Object.keys(acc);
        if (!users.includes(this.state.application === IntegrationTypes.JIRA ? obj.assignee : obj.field_value)) {
          acc[this.state.application === IntegrationTypes.JIRA ? obj.assignee : obj.field_value] =
            tagColorArray[users.length % colorLen];
        }
        return acc;
      },
      { unknown: undefined }
    );
  }

  get assignees() {
    const { assignee_list } = this.pageData;
    return (assignee_list || [])
      .sort((a: any, b: any) => {
        if (this.state.application === IntegrationTypes.JIRA) {
          return b.start_time - a.start_time;
        }
        return b.start_date - a.start_date;
      })
      .map((assignee: any) => ({ ...assignee, type: "assginee" }));
  }

  get uniqAssignees() {
    return uniq(
      this.assignees
        .map((assignee: any) =>
          this.state.application === IntegrationTypes.JIRA ? assignee.assignee : assignee.field_value
        )
        .filter((assignee: any) => !["UNASSIGNED", "_UNASSIGNED_"].includes(assignee))
    );
  }

  get stats() {
    const { bounces, hops, issue_created_at, first_comment_at, issue_resolved_at } = this.pageData;
    const statsMap: { title: string; value: any; hasStat: string }[] = [
      { title: "BOUNCES", value: bounces, hasStat: "bounces" },
      { title: "HOPS", value: hops, hasStat: "hops" },
      { title: "ENGINEERS", value: this.uniqAssignees.length, hasStat: "engineers" },
      { title: "CREATED", value: moment.unix(issue_created_at).fromNow(), hasStat: "created" },
      {
        title: "RESPONSE TIME",
        value:
          first_comment_at === 0 ? "NA" : getDaysAndTimeWithUnit(first_comment_at - issue_created_at, PRECISION_VALUE),
        hasStat: "response_time"
      },
      {
        title: "RESOLUTION TIME",
        value:
          issue_resolved_at === 0
            ? "NA"
            : getDaysAndTimeWithUnit(issue_resolved_at - issue_created_at, PRECISION_VALUE),
        hasStat: "resolution_time"
      }
    ];
    return statsMap;
  }

  get azureStats() {
    const { bounces, hops, workitem_created_at, first_comment_at, workitem_resolved_at } = this.pageData;
    const statsMap: { title: string; value: any; hasStat: string }[] = [
      { title: "BOUNCES", value: bounces, hasStat: "bounces" },
      { title: "HOPS", value: hops, hasStat: "hops" },
      { title: "ENGINEERS", value: this.uniqAssignees.length, hasStat: "engineers" },
      { title: "CREATED", value: unixToDate(workitem_created_at, true), hasStat: "created" },
      {
        title: "RESPONSE TIME",
        value:
          !first_comment_at || !workitem_created_at
            ? "NA"
            : getDaysAndTimeWithUnit(
                +truncatedString(first_comment_at) - +truncatedString(workitem_created_at),
                PRECISION_VALUE
              ),
        hasStat: "response_time"
      },
      {
        title: "RESOLUTION TIME",
        value:
          !workitem_resolved_at || !workitem_created_at
            ? "NA"
            : getDaysAndTimeWithUnit(
                +truncatedString(workitem_resolved_at) - +truncatedString(workitem_created_at),
                PRECISION_VALUE
              ),
        hasStat: "resolution_time"
      }
    ];
    return statsMap;
  }

  get isJira() {
    return this.state.application === IntegrationTypes.JIRA;
  }

  createdOn = (data: any) => {
    return this.isJira ? data?.issue_created_at : data?.workitem_created_at;
  };

  render() {
    const addToLocalStorage = window.isStandaloneApp
      ? getRBACPermission(PermeableMetrics.ADD_DASHBOARD_ID_TO_LOCALSTORAGE)
      : false;
    if (addToLocalStorage && !this.state.dashboardId) {
      notification.error({ message: "Insight Id error", description: "Insight Id is missing" });
      return <AntText type="danger">No insight found</AntText>;
    }

    if (this.state.loading) {
      return <Loader />;
    }

    const data = this.pageData;
    const statuses = (data.status_list || []).map((status: any) => ({ ...status, type: "status" }));
    const merged = [...statuses, ...this.assignees].sort((a: any, b: any) => {
      if (this.state.application === IntegrationTypes.JIRA) {
        return b.start_time - a.start_time;
      }
      return b.start_date - a.start_date;
    });
    const ticketCreate = unixToDate(this.createdOn(data));
    // @ts-ignore
    return (Array.isArray(data) && data.length) || (typeof data === "object" && Object.keys(data)?.length) ? (
      <>
        <Typography>
          <Badge status="success" text={this.isJira ? data.key : data.workitem_id} />
          <AntTitle level={4}>{data.summary}</AntTitle>
          <AntText type={"secondary"} className="CreatedOnText">
            Created On {ticketCreate.toLocaleString().split(", ")[0]}
          </AntText>
        </Typography>
        <br />
        <AntRow gutter={[20, 20]}>
          <AntCol span={24}>
            <Card>
              <AntRow type={"flex"} justify={"space-between"}>
                {(this.state.application === IntegrationTypes.JIRA ? this.stats : this.azureStats).map(stat => (
                  <SingleStatContainer {...stat} />
                ))}
              </AntRow>
            </Card>
          </AntCol>
          <AntCol span={6}>
            <div className="cardColumnDiv">
              <Card
                title={"Details"}
                className="ant-card-body-p-0"
                extra={
                  <small>
                    <a
                      href={joinUrl(
                        this.state.url,
                        this.state.application === IntegrationTypes.JIRA
                          ? `/browse/${data.key}`
                          : `/${data?.organization || data?.attributes?.organization}/${
                              data?.project
                            }/_workitems/edit/${data?.workitem_id}`,
                        false
                      )}
                      target={"_blank"}
                      rel="noopener noreferrer">
                      {this.state.application === IntegrationTypes.JIRA ? "Open in Jira" : "Open in Azure"}
                    </a>
                  </small>
                }>
                <div className="cardColumnDiv__innerDiv">
                  <AntRow type={"flex"} justify={"start"} gutter={[10, 10]}>
                    <AntCol span={24}>
                      <AntText type={"secondary"}>{"PROJECT"}</AntText>
                    </AntCol>
                    <AntCol span={24}>{data.project}</AntCol>
                    <AntCol span={24}>
                      <AntText type={"secondary"}>{"ISSUE TYPE"}</AntText>
                    </AntCol>
                    <AntCol span={24}>
                      {this.state.application === IntegrationTypes.JIRA ? data.issue_type : data.workitem_type}
                    </AntCol>
                    <AntCol span={24}>
                      <AntText type={"secondary"}>{"CURRENT STATE"}</AntText>
                    </AntCol>
                    <AntCol span={24}>{statusRender(data.status)}</AntCol>
                    <AntCol span={24}>
                      <AntText type={"secondary"}>{"REPORTER"}</AntText>
                    </AntCol>
                    <AntCol span={24}>
                      <Avatar
                        // @ts-ignore
                        size={"medium"}
                        style={{ backgroundColor: this.colorMap[data.reporter || "unknown"] }}>
                        {getInitials(data.reporter || "")}
                      </Avatar>{" "}
                      &nbsp;
                      {data.reporter || ""}
                    </AntCol>
                    <AntCol span={24}>
                      <AntText type={"secondary"}>{"CURRENT ASSIGNEE"}</AntText>
                    </AntCol>
                    <AntCol span={24}>
                      <Avatar
                        // @ts-ignore
                        size={"medium"}
                        style={{ backgroundColor: this.colorMap[data.assignee || "unknown"] }}>
                        {getInitials(data.assignee || "")}
                      </Avatar>{" "}
                      &nbsp;
                      {data.assignee || ""}
                    </AntCol>
                    <AntCol span={24}>
                      <AntText type={"secondary"}>{"PREV ASSIGNEES"}</AntText>
                    </AntCol>
                    <AntCol span={24}>
                      {this.uniqAssignees?.slice(0, Math.min(5, this.uniqAssignees.length)).map(
                        // @ts-ignore
                        (assignee: string) => (
                          <Tooltip title={assignee}>
                            <Avatar
                              size={"small"}
                              style={{ margin: "3px", backgroundColor: this.colorMap[assignee || "unknown"] }}>
                              {getInitials(assignee)}
                            </Avatar>
                          </Tooltip>
                        )
                      )}
                      {this.uniqAssignees?.length > 5 && (
                        <AntText type={"secondary"}>+ {this.uniqAssignees?.length - 5} more</AntText>
                      )}
                      {this.uniqAssignees?.length === 0 && <AntText type={"secondary"}>NA</AntText>}
                    </AntCol>
                    <AntCol span={24}>
                      <AntText type={"secondary"}>{"PRIORITY"}</AntText>
                    </AntCol>
                    <AntCol span={24}>{data.priority}</AntCol>
                    {this.state.application === IntegrationTypes.JIRA && (
                      <>
                        {" "}
                        <AntCol span={24}>
                          <AntText type={"secondary"}>{"STORY POINTS"}</AntText>
                        </AntCol>
                        <AntCol span={24}>{data.story_points}</AntCol>
                      </>
                    )}
                    {data.component_list && data.component_list.length > 0 && (
                      <>
                        <AntCol span={24}>
                          <AntText type={"secondary"}>{"COMPONENTS"}</AntText>
                        </AntCol>
                        <AntCol span={24}>
                          {data.component_list.map((component: any) => (
                            <Tag>{component}</Tag>
                          ))}
                        </AntCol>
                      </>
                    )}
                    {data.labels && data.labels.length > 0 && (
                      <>
                        <AntCol span={24}>
                          <AntText type={"secondary"}>{"LABELS"}</AntText>
                        </AntCol>
                        <AntCol span={24}>
                          {data.labels.map((label: any) => (
                            <Tag>{label}</Tag>
                          ))}
                        </AntCol>
                      </>
                    )}
                    {data.versions && data.versions.length > 0 && (
                      <>
                        <AntCol span={24}>
                          <AntText type={"secondary"}>{"VERSIONS"}</AntText>
                        </AntCol>
                        <AntCol span={24}>
                          {data.versions.map((version: any) => (
                            <Tag>{version}</Tag>
                          ))}
                        </AntCol>
                      </>
                    )}
                    {data.fix_versions && data.fix_versions.length > 0 && (
                      <>
                        <AntCol span={24}>
                          <AntText type={"secondary"}>{"FIX VERSIONS"}</AntText>
                        </AntCol>
                        <AntCol span={24}>
                          {data.fix_versions.map((version: any) => (
                            <Tag>{version}</Tag>
                          ))}
                        </AntCol>
                      </>
                    )}
                    {
                      // @ts-ignore
                      data.custom_fields &&
                        Object.keys(data.custom_fields)
                          .map(field => {
                            const custom_field_info = this.getFieldName(field) || {};
                            const value = data.custom_fields[field];
                            let tags: any = ["date", "datetime", "dateTime"].includes(custom_field_info?.field_type) ? (
                              <Tag>
                                {convertToSeconds(value) !== "" ? timeStampToValue(convertToSeconds(value)) : ""}
                              </Tag>
                            ) : (
                              <Tag>{value}</Tag>
                            );
                            if (Array.isArray(value)) {
                              tags = value.map((tag: string) => <Tag className="mt-5">{tag}</Tag>);
                            }
                            const fieldName = (this.getFieldName(field) || {}).name;
                            if (fieldName?.toLowerCase()?.replace(/ /, "") === "storypoints") {
                              return undefined;
                            }
                            return (
                              <>
                                <AntCol span={24}>
                                  <AntText type={"secondary"}>{fieldName}</AntText>
                                </AntCol>
                                <AntCol span={24}>{tags}</AntCol>
                              </>
                            );
                          })
                          .filter((item: any) => item !== undefined)
                    }
                  </AntRow>
                </div>
              </Card>
            </div>

            <div className="mb-10">
              <Card title={"Time Spent in Ticket Status"}>
                {(statuses || [])
                  .sort((a: any, b: any) => {
                    if (this.state.application === IntegrationTypes.JIRA) {
                      return b.start_time - a.start_time;
                    }
                    return b.start_date - a.start_date;
                  })
                  .map((status: any, index: number) => {
                    let { time, unit } = getDaysAndTimeWithUnit(
                      this.state.application === IntegrationTypes.JIRA
                        ? status.end_time - status.start_time
                        : status.end_date - status.start_date,
                      PRECISION_VALUE
                    );
                    return (
                      <div className="mb-10 flex justify-space-between align-center" key={index}>
                        <AntText>
                          {this.state.application === IntegrationTypes.JIRA ? status.status || "" : status.field_value}
                        </AntText>
                        <AntText>
                          {time || 0} {unit}
                        </AntText>
                      </div>
                    );
                  })}
              </Card>
            </div>
          </AntCol>
          <AntCol span={18}>
            <Card title={"Timeline"}>
              <div className="ant-timeline-div">
                <Timeline className="pt-20 ant-timeline-div__ant-timeline-ticket-bounce">
                  {merged.map((record: any, index: number) => {
                    //console.log('record = ',record)
                    // @ts-ignore
                    return (
                      <Timeline.Item
                        key={index}
                        //dot={<Badge color={"blue"} />}
                      >
                        <AntRow gutter={[5, 0]}>
                          <AntCol span={4} className="date-time-col">
                            <AntText type={"secondary"} className="f-12">
                              {unixToDate(
                                this.state.application === IntegrationTypes.JIRA
                                  ? record.start_time * 1000
                                  : record.start_date
                              )}
                            </AntText>
                          </AntCol>
                          {record.type === "status" && (
                            <AntCol span={16} className="timeline-details-col">
                              <div className="flex align-center justify-start px-20">
                                <AntText className="flex align-center">
                                  <small>Changed Status To &nbsp;</small>
                                </AntText>
                                <div className="timeline-details-col__tagdiv">
                                  <AntTag
                                    className="mr-10"
                                    style={{ cursor: "pointer" }}
                                    //color={this.colorMap[record.status] || "unknown"}
                                  >
                                    {this.state.application === IntegrationTypes.JIRA
                                      ? record.status || "unknown"
                                      : record.field_value || "unknown"}
                                  </AntTag>
                                </div>
                              </div>
                            </AntCol>
                          )}
                          {record.type !== "status" && (
                            <AntCol span={16} className="timeline-details-col">
                              <div className="flex align-center justify-start px-20">
                                <div className="timeline-details-col__tagdiv">
                                  <AntTag
                                    className="mr-10"
                                    style={{ cursor: "pointer" }}
                                    color={
                                      this.colorMap[
                                        this.state.application === IntegrationTypes.JIRA
                                          ? record.assignee
                                          : record.field_value
                                      ] || "unknown"
                                    }>
                                    {this.state.application === IntegrationTypes.JIRA
                                      ? record.assignee || "unknown"
                                      : record.field_value || "unknown"}
                                  </AntTag>
                                </div>

                                <div
                                  className="flex align-center mr-10"
                                  style={{ width: this.getLength(record.assignee) }}>
                                  <Divider className="m-0" />
                                </div>
                                <AntText className="flex align-center">
                                  <small>
                                    {record.type === "file" ? "Attached file" : "Assigned to"}
                                    &nbsp;
                                  </small>
                                  <Tooltip title={record.assignee}>
                                    <Avatar size={"small"} style={{ backgroundColor: this.colorMap[record.assignee] }}>
                                      {getInitials(
                                        this.state.application === IntegrationTypes.JIRA
                                          ? record.assignee
                                          : record.field_value
                                      )}
                                    </Avatar>
                                  </Tooltip>
                                </AntText>
                              </div>
                            </AntCol>
                          )}
                        </AntRow>
                      </Timeline.Item>
                    );
                  })}
                </Timeline>
              </div>
            </Card>
          </AntCol>
        </AntRow>
      </>
    ) : (
      <EmptyWidgetComponent></EmptyWidgetComponent>
    );
  }
}

const mapDispatchToProps = (dispatch: any) => ({
  ...mapGenericToProps(dispatch),
  ...mapPageSettingsDispatchToProps(dispatch),
  ...mapRestapiDispatchtoProps(dispatch)
});

// @ts-ignore
export default connect(mapRestapiStatetoProps, mapDispatchToProps)(TicketBouncePage);
