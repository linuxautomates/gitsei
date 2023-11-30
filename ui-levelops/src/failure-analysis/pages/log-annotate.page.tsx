import React, { useEffect, useState } from "react";
import { TextAnnotateContainer } from "../../shared-resources/containers/text-annotate/text-annotate.container";
import { Col, Empty, notification, Row } from "antd";
import "./log-annotate.style.scss";
import { TriageSection } from "../components/triage-section/triage-section.component";
import { useDispatch, useSelector } from "react-redux";
import {
  pipelineJobRunsLogsIdState,
  pipelineJobRunsStageIdState,
  triageRuleResultsCountState
} from "reduxConfigs/selectors/restapiSelector";
import queryString from "query-string";
import { genericGet, restapiClear, genericList } from "reduxConfigs/actions/restapi";
import { get } from "lodash";
import Loader from "components/Loader/Loader";
import { clearPageSettings, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";

const NO_ID_ERROR = "No log file found. No ID found!";

interface LogAnnotatePageProps {
  location: any;
}

export const LogAnnotatePage: React.FC<LogAnnotatePageProps> = (props: LogAnnotatePageProps) => {
  const dispatch = useDispatch();
  const [annotationList, setAnnotationList] = useState<string[]>([]);
  const [selectedTriageRule, setSelectedTriageRule] = useState<any>(null);
  const [editAddActionData, setEditAddActionData] = useState({ data: undefined, type: undefined });
  const [editRule, setEditRule] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [text, setText] = useState("");
  const [ruleCountList, setRuleCountList] = useState({});
  const [ruleCountLoading, setRuleCountLoading] = useState(false);
  const job_rest_api = useSelector(state => pipelineJobRunsLogsIdState(state, props.location));
  const stage_rest_api = useSelector(state => pipelineJobRunsStageIdState(state, props.location));
  const rule_count_api = useSelector(state => triageRuleResultsCountState(state, props.location));
  const hasSearch = !!props.location && !!props.location.search;
  const query = hasSearch ? { ...queryString.parse(props.location.search) } : "";
  let logType: any = undefined;
  let id: any = "";
  let name: any = "";

  if (query) {
    name = query.log_name;
    if (query.hasOwnProperty("job_id")) {
      logType = "job_log";
      id = query["job_id"];
    } else if (query.hasOwnProperty("stage_id")) {
      logType = "stage_log";
      id = query["stage_id"];
    }
  }

  useEffect(() => {
    dispatch(
      setPageSettings(props.location.pathname, {
        title: name ? `${name} Failure Log` : "Failure Log"
      })
    );

    if (logType && id) {
      let uri = logType === "job_log" ? "pipeline_job_runs_logs" : "pipeline_job_runs_stages_logs";
      dispatch(genericGet(uri, id));
      let options = {
        page_size: 100,
        filter: {
          job_run_ids: logType === "job_log" ? [id] : [],
          stage_ids: logType === "stage_log" ? [id] : []
        }
      };

      dispatch(genericList("triage_rule_results", "list", options, null, id));
      setRuleCountLoading(true);
      setLoading(true);
    }

    return () => {
      // @ts-ignore
      dispatch(restapiClear("pipeline_job_runs_logs", "get", "-1"));
      // @ts-ignore
      dispatch(restapiClear("pipeline_job_runs_stages_logs", "get", "-1"));
      // @ts-ignore
      dispatch(restapiClear("jenkins_pipeline_job_stages", "list", "-1"));
      dispatch(clearPageSettings(props.location.pathname));
    };
  }, []);

  useEffect(() => {
    if (logType) {
      const api = logType === "job_log" ? job_rest_api : stage_rest_api;
      if (loading) {
        const logLoading = get(api, ["loading"], true);
        const logError = get(api, ["error"], true);

        if (!logLoading) {
          if (!logError) {
            const data = get(api, ["data"], "");
            setText(data);
          } else {
            let error = get(api, ["data", "error"], "");
            if (error === "Not Found") {
              const message = get(api, ["data", "message"], "");
              notification.error({ message: error, description: message });
              error = message;
            } else {
              notification.error({ message: "Error occurred!", description: error });
            }
            setError(error);
          }

          setLoading(false);
        }
      }

      if (ruleCountLoading) {
        const ruleLoading = get(rule_count_api, ["loading"], true);
        const ruleError = get(rule_count_api, ["error"], true);
        if (!ruleLoading) {
          if (!ruleError) {
            const data = get(rule_count_api, ["data", "records"], []);
            const key = logType === "job_log" ? "job_run_id" : "stage_id";
            const countData = data.reduce((acc: any, item: any) => {
              if (item[key] === id) {
                return { ...acc, [item.rule_id]: (acc[item.rule_id] || 0) + item.count };
              }
              return acc;
            }, {});
            setRuleCountList(countData);
          } else {
            notification.error({ message: "Rule Count Error", description: "Failed to load rule counts" });
          }
          setRuleCountLoading(false);
        }
      }
    }
  }, [job_rest_api, stage_rest_api, rule_count_api]);

  useEffect(() => {
    if (selectedTriageRule) {
      setAnnotationList(selectedTriageRule.rule.regexes);
    } else {
      setAnnotationList([]);
    }
  }, [selectedTriageRule]);

  useEffect(() => {
    if (editAddActionData && editAddActionData.type) {
      setSelectedTriageRule(null);
      setEditRule(null);
    }
  }, [editAddActionData]);

  if (loading || ruleCountLoading) {
    return <Loader />;
  }

  return (
    <Row className="annotation-row">
      {(!logType || error || !text) && (
        <Empty description={!logType ? NO_ID_ERROR : error ? error : "No data found!"} />
      )}
      {logType && !error && text && (
        <>
          <Col span={16} className="annotation-text-col">
            <div className="annotation-logs-container">
              <TextAnnotateContainer
                text={text}
                annotations={annotationList}
                setAnnotationList={setAnnotationList}
                handleAnnotateChange={setEditAddActionData}
              />
            </div>
          </Col>

          <Col span={8} style={{ height: "100%" }}>
            <div className="triage-rule-container">
              <TriageSection
                editRule={editRule}
                setEditRule={setEditRule}
                editAddActionData={editAddActionData}
                selectedTriageRule={selectedTriageRule}
                setSelectedTriageRule={setSelectedTriageRule}
                setEditAddActionData={setEditAddActionData}
                ruleCountList={ruleCountList}
              />
            </div>
          </Col>
        </>
      )}
    </Row>
  );
};
