import * as React from "react";
import queryString from "query-string";
import cx from "classnames";
import { connect } from "react-redux";
import { get } from "lodash";
import { v1 as uuid } from "uuid";
import { Col, Row, Descriptions, Icon } from "antd";

import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { getData, loadingStatus } from "utils/loadingUtils";
import Loader from "../../components/Loader/Loader";
import { AntButton, AntText } from "shared-resources/components";
import { StagesPaginated, StagesCount, JobMatchesTable } from "../components";
import { mapPageSettingsDispatchToProps } from "reduxConfigs/maps/pagesettings.map";
import { mapGenericToProps } from "reduxConfigs/maps/restapi";
import { getBaseUrl } from "constants/routePaths";
import { Link } from "react-router-dom";

interface Props {
  triageDetailGet: (id: string) => void;
  triageStagesGet: (id: string, filter: any) => void;
  triageRuleResultsGet: (id: string, filter: any) => void;
  restapiClear: (uri: string, method: string, id: string) => void;
  clearPageSettings: (path: string) => void;
  setPageSettings: (path: string, settings: any) => void;
  genericGet: (uri: string, id: string) => void;
  location: any;
  history: any;
}

interface State {
  show_params: boolean;
  selected_triage: string | undefined;
  loading: boolean;
  triage_detail: any;
  stages_list_loading: boolean;
  stages_list: any[];
  passed_stages: number;
  failed_stages: number;
  aborted_stages: number;
  loading_details: boolean;
  moreFilters: any;
  selectedStageId: string;
  moreTopLevelFilters: any;
  showSelectedStage: boolean;
}

const leftData = [
  { key: "job_run_number", label: "Job run number" },
  { key: "duration", label: "Duration" },
  { key: "start_time", label: "Start time" },
  { key: "job_full_name", label: "Full job name" },
  { key: "branch_name", label: "Branch Name" },
  { key: "cicd_user_id", label: "Cicd User" },
  { key: "cicd_instance_name", label: "Cicd instance name" }
];
// can add more values
const rightData: any[] = [];

class TriageDetailsPage extends React.PureComponent<Props, State> {
  constructor(props: any) {
    super(props);

    const triageId = queryString.parse(props.location.search).id;
    const stageId = queryString.parse(props.location.search).stage_id;

    this.state = {
      show_params: false,
      selected_triage: triageId ? (triageId as string) : undefined,
      loading: triageId !== undefined,
      stages_list_loading: false,
      triage_detail: null,
      stages_list: [],
      passed_stages: 0,
      failed_stages: 0,
      aborted_stages: 0,
      loading_details: false,
      moreFilters: { result: stageId ? [] : ["FAILURE"] },
      moreTopLevelFilters: stageId ? { stage_ids: [stageId] } : {},
      selectedStageId: (stageId as string) || "",
      showSelectedStage: true
    };

    this.toggleStages = this.toggleStages.bind(this);
  }

  getJobStatus() {
    const status = get(this.state.triage_detail, ["status"], "");
    if (status.toLowerCase() === "aborted") {
      return "warning";
    } else if (status.toLowerCase() === "failed") {
      return "danger";
    } else {
      return "secondary";
    }
  }

  static getDerivedStateFromProps(props: any, state: any) {
    const triageId = queryString.parse(props.location.search).id;
    if (triageId && triageId !== state.selected_triage) {
      props.genericGet("jenkins_pipeline_job_runs", triageId);
      return {
        ...state,
        loading: true,
        selected_triage: triageId
      };
    }
    if (state.loading) {
      const { loading, error } = loadingStatus(
        props.rest_api,
        "jenkins_pipeline_job_runs",
        "get",
        state.selected_triage
      );

      if (!loading && !error) {
        console.log("received triage details data");
        const data = getData(props.rest_api, "jenkins_pipeline_job_runs", "get", state.selected_triage);

        if (data) {
          props.setPageSettings(props.location.pathname, {
            title: data.job_name
          });
          return {
            ...state,
            loading: false,
            triage_detail: data
          };
        }
      }
    }

    return null;
  }

  componentDidMount() {
    if (this.state.selected_triage) {
      this.props.genericGet("jenkins_pipeline_job_runs", this.state.selected_triage);
    }
  }

  componentWillUnmount() {
    this.props.restapiClear("triage", "get", this.state.selected_triage as string);
    this.props.restapiClear("triage_rule_results", "list", this.state.selected_triage as string);
    this.props.clearPageSettings(this.props.location.pathname);
  }

  getColor = (status: string) => {
    switch ((status || "").toLowerCase()) {
      case "aborted":
        return "gray";
      case "success":
        return "green";
      case "failure":
        return "red";
      default:
        return "gray";
    }
  };

  selectState = (result: string) => {
    console.log(result);
    if (result) {
      this.setState({ moreFilters: { result: [(result || "").toUpperCase()] } });
    }
  };

  handleParamsToggle = () => {
    this.setState((state: State) => ({ show_params: !state.show_params }));
  };

  renderDescriptionItem = (label: string, value: string) => {
    const LABEL = (label || "").replace("_", " ").toUpperCase();
    return (
      <Descriptions.Item key={uuid()} label={<AntText strong>{LABEL}</AntText>}>
        {value}
      </Descriptions.Item>
    );
  };

  renderDescription(filter: { key: string; label: string }[]) {
    const { triage_detail } = this.state;
    if (!triage_detail) {
      return null;
    }
    return filter.map(filter => {
      const { key, label } = filter;
      const item = Object.keys(triage_detail).find((key: string) => key === filter.key);
      if (triage_detail[key] && item) {
        let value = triage_detail[key];
        switch (key) {
          case "duration":
            value += " seconds";
            break;
          case "start_time":
            value = new Date(value || 0).toLocaleString();
            break;
        }
        return this.renderDescriptionItem(label, value);
      }
      return true;
    });
  }

  get descriptionParams() {
    const { triage_detail, show_params } = this.state;
    if (!show_params || !triage_detail || !triage_detail.params || !triage_detail.params.length) {
      return null;
    }
    const { params } = triage_detail;
    return params.map((param: { name: string; value: string }) => {
      if (!param.value) {
        return null;
      }
      return this.renderDescriptionItem(param.name, param.value);
    });
  }

  get paramsToggleButton() {
    const { triage_detail, show_params } = this.state;
    if (!triage_detail) {
      return null;
    }
    const { params } = triage_detail;
    const hasParams = params && params.length;
    if (!hasParams) {
      return null;
    }
    return (
      <AntText className="bg-grey pb-10 pr-10 w-100 d-flex justify-end">
        {/*eslint-disable-next-line jsx-a11y/anchor-is-valid*/}
        <a
          href="#"
          onClick={(event: any) => {
            event.preventDefault();
            this.handleParamsToggle();
          }}>
          <Icon
            className={cx("icon-transition pl-5 pr-5", {
              expanded: this.state.show_params,
              collapsed: !this.state.show_params
            })}
            type="down"
          />
          {show_params ? "Collapse" : "Expand"}
        </a>
      </AntText>
    );
  }

  get descriptions() {
    const { triage_detail } = this.state;
    if (!triage_detail) {
      return null;
    }
    return (
      <>
        <Descriptions className="bg-grey px-10 pt-10">
          {this.renderDescription(leftData)}
          {this.renderDescription(rightData)}
          {this.descriptionParams}
        </Descriptions>
        {this.paramsToggleButton}
      </>
    );
  }

  toggleStages = () => {
    this.setState((state: any) => ({ showSelectedStage: !state.showSelectedStage }));
  };

  render() {
    if (this.state.loading) {
      return <Loader />;
    }

    // @ts-ignore
    return (
      <div>
        <Row type={"flex"} justify={"start"} gutter={[10, 10]} style={{ marginBottom: "10px" }}>
          <Col span={24}>
            {
              <AntText
                style={{
                  fontSize: "28px",
                  textTransform: "uppercase",
                  color: this.getColor(this.state.triage_detail?.status)
                }}
                type={this.getJobStatus()}>
                {this.state.triage_detail?.status}
              </AntText>
            }
          </Col>
          <Col span={24}>
            {this.state.triage_detail && (
              <AntText>
                <a href={this.state.triage_detail.url} target={"_blank"} rel="noopener noreferrer">
                  View Logs
                </a>
                {this.state.triage_detail.logs === true && (
                  <Link
                    style={{ marginLeft: "1rem" }}
                    to={`${getBaseUrl()}/failure-logs?job_id=${this.state.triage_detail.id}&log_name=${
                      this.state.triage_detail.job_name
                    }`}
                    target={"_blank"}
                    rel="noopener noreferrer">
                    View Raw Logs
                  </Link>
                )}
              </AntText>
            )}
          </Col>
        </Row>
        <Row>{this.descriptions}</Row>
        <Row>{this.state.selected_triage && <JobMatchesTable jobId={this.state.selected_triage} />}</Row>
        <Row>
          {this.state.selectedStageId && (
            <Col style={{ marginTop: "8px" }}>
              <AntButton type="link" onClick={() => this.toggleStages()}>
                {this.state.showSelectedStage ? "Show all stages" : "Show selected stage"}
              </AntButton>
            </Col>
          )}
        </Row>
        {
          // @ts-ignore
          <StagesPaginated
            // @ts-ignore
            jobRunId={this.state.selected_triage}
            moreFilters={this.state.moreFilters}
            selectedStageId={this.state.selectedStageId}
            topLevelFilters={this.state.showSelectedStage ? this.state.moreTopLevelFilters : {}}
            showSelectedStage={this.state.showSelectedStage}
            // @ts-ignore
            title={<StagesCount jobRunId={this.state.selected_triage as string} onClick={this.selectState} />}
          />
        }
      </div>
    );
  }
}

const mapDispatchToProps = (dispatch: any) => ({
  ...mapRestapiDispatchtoProps(dispatch),
  ...mapPageSettingsDispatchToProps(dispatch),
  ...mapGenericToProps(dispatch)
});

export default connect(mapRestapiStatetoProps, mapDispatchToProps)(TriageDetailsPage as any);
