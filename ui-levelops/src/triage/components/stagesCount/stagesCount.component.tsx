import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { genericList, restapiClear } from "reduxConfigs/actions/restapi";
import { get, cloneDeep } from "lodash";
import { Spin, Row, Col } from "antd";
import { AntText } from "../../../shared-resources/components";

interface StoreProps {}

interface StagesCountComponentProps extends StoreProps {
  jobRunId: string;
  onClick: (type: string) => void;
}

// @ts-ignore
export const StagesCountComponent: React.FC<StagesCountComponentProps> = (props: StagesCountComponentProps) => {
  const dispatch = useDispatch();
  const [loading, setLoading] = useState(false);
  const [counts, setCounts] = useState({
    success: 0,
    failure: 0,
    aborted: 0
  });
  const rest_api = useSelector(state => {
    // @ts-ignore
    const success = get(state.restapiReducer, ["jenkins_pipeline_job_stages", "list", `${props.jobRunId}_success`], {
      loading: true,
      error: false
    });
    // @ts-ignore
    const failed = get(state.restapiReducer, ["jenkins_pipeline_job_stages", "list", `${props.jobRunId}_failed`], {
      loading: true,
      error: false
    });
    // @ts-ignore
    const aborted = get(state.restapiReducer, ["jenkins_pipeline_job_stages", "list", `${props.jobRunId}_aborted`], {
      loading: true,
      error: false
    });
    return {
      success: success,
      failure: failed,
      aborted: aborted
    };
  });

  useEffect(() => {
    if (props.jobRunId) {
      dispatch(
        genericList(
          "jenkins_pipeline_job_stages",
          "list",
          { filter: { result: ["SUCCESS"], job_run_id: props.jobRunId }, page_size: 1 },
          null,
          `${props.jobRunId}_success`
        )
      );
      dispatch(
        genericList(
          "jenkins_pipeline_job_stages",
          "list",
          { filter: { result: ["FAILURE"], job_run_id: props.jobRunId }, page_size: 1 },
          null,
          `${props.jobRunId}_failed`
        )
      );
      dispatch(
        genericList(
          "jenkins_pipeline_job_stages",
          "list",
          { filter: { result: ["ABORTED"], job_run_id: props.jobRunId }, page_size: 1 },
          null,
          `${props.jobRunId}_aborted`
        )
      );
      setLoading(true);
    }
    return () => {
      // @ts-ignore
      dispatch(restapiClear("jenkins_pipeline_job_stages", "list", `${props.jobRunId}_success`));
      // @ts-ignore
      dispatch(restapiClear("jenkins_pipeline_job_stages", "list", `${props.jobRunId}_failed`));
      // @ts-ignore
      dispatch(restapiClear("jenkins_pipeline_job_stages", "list", `${props.jobRunId}_aborted`));
    };
  }, []);

  useEffect(() => {
    if (loading) {
      let updatedCounts: any = cloneDeep(counts);
      let allStagesLoading = false;
      Object.keys(counts).forEach(state => {
        const stageLoading = get(rest_api, [state, "loading"], true);
        const stageError = get(rest_api, [state, "error"], false);
        if (!stageLoading) {
          if (!stageError) {
            const count = get(rest_api, [state, "data", "_metadata", "total_count"], 0);
            // @ts-ignore
            updatedCounts[state] = count;
          }
        } else {
          allStagesLoading = true;
        }
      });
      if (!allStagesLoading) {
        setLoading(allStagesLoading);
        setCounts(updatedCounts);
      }
    }
  }, [rest_api]);

  if (loading) {
    // @ts-ignore
    return (
      // @ts-ignore
      <div align={"center"}>
        <Spin />
      </div>
    );
  }

  const getType = (status: string) => {
    switch (status) {
      case "aborted":
      case "success":
        return "secondary";
      case "failure":
        return "danger";
      default:
        return "secondary";
    }
  };

  const getColor = (status: string) => {
    switch (status) {
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

  return (
    <Row style={{ paddingTop: "1.6rem", paddingBottom: "1.6rem" }} type={"flex"}>
      <Col span={6}>
        <AntText type={"secondary"} strong style={{ fontSize: "18px" }}>
          Stages/Jobs
        </AntText>
      </Col>
      {Object.keys(counts).map((count: string, index: number) => (
        <Col span={6} style={{ display: "flex" }} key={`stage-count-${index}`}>
          <div style={{ cursor: "pointer" }} onClick={e => props.onClick(count)}>
            <AntText type={"secondary"} strong style={{ fontSize: "18px" }}>
              {(count || "").toUpperCase()}:&nbsp;
            </AntText>
            <AntText style={{ color: getColor(count), fontSize: "18px" }} strong>
              {
                // @ts-ignore
                counts[count]
              }
            </AntText>
          </div>
        </Col>
      ))}
    </Row>
  );
};
