import * as React from "react";
import { useDispatch, useSelector } from "react-redux";
import { useEffect, useState } from "react";
import { genericList } from "reduxConfigs/actions/restapi";
import { get } from "lodash";
import "./job-matches-table.style.scss";
import { AntText, AntButton, AntTable } from "shared-resources/components";
import { SlideDown } from "react-slidedown";
import Loader from "components/Loader/Loader";
import { restapiClear } from "reduxConfigs/actions/restapi";

interface JobMatchesTableProps {
  jobId: string;
}

const JobMatchesTableComponent: React.FC<JobMatchesTableProps> = ({ jobId, ...props }) => {
  const dispatch = useDispatch();
  const resultState = useSelector(state => {
    // @ts-ignore
    return get(state.restapiReducer, ["triage_rule_results", "list", jobId], { loading: true, error: false });
  });
  const triageState = useSelector(state => {
    // @ts-ignore
    return get(state.restapiReducer, ["triage_rules", "list", jobId], { loading: true, error: false });
  });

  const [hits, setHits] = useState<any[]>([]);
  const [resultLoading, setResultLoading] = useState<boolean>(false);
  const [triageLoading, setTriageLoading] = useState<boolean>(false);
  const [rules, setRules] = useState<any[]>([]);
  const [expand, setExpand] = useState<boolean>(false);

  useEffect(() => {
    return () => {
      // @ts-ignore
      dispatch(restapiClear("triage_rules", "list", "-1"));
      // @ts-ignore
      dispatch(restapiClear("triage_rule_results", "list", "-1"));
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (jobId) {
      const filter = {
        job_run_ids: [jobId]
      };
      dispatch(genericList("triage_rule_results", "list", { filter }, null, jobId));
      setResultLoading(true);
    }
  }, [jobId]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (resultLoading) {
      const loading = get(resultState, ["loading"], true);
      const error = get(resultState, ["error"], true);
      if (!loading && !error) {
        const data = get(resultState, ["data", "records"]);
        const _data = data.filter((item: any) => item.job_run_id === jobId);
        const _hits = _data.reduce(
          (acc: any, item: any) => ({ ...acc, [item.rule_id]: item.count + (acc[item.rule_id] || 0) }),
          {}
        );
        setHits(_hits);
        dispatch(
          genericList(
            "triage_rules",
            "list",
            { filter: { rule_ids: _data.map((hit: any) => hit.rule_id) }, page_size: 100 },
            null,
            jobId
          )
        );
        setResultLoading(false);
        setTriageLoading(true);
      }
    }
  }, [resultState]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (triageLoading) {
      const loading = get(triageState, ["loading"], true);
      const error = get(triageState, ["error"], true);
      if (!loading && !error) {
        const data = get(triageState, ["data", "records"]);
        const _data = data.reduce((acc: any, item: any) => ({ ...acc, [item.id]: item.name }), {});
        setRules(_data);
        setTriageLoading(false);
      }
    }
  }, [triageState]); // eslint-disable-line react-hooks/exhaustive-deps

  const columns = [
    {
      title: "Rule",
      key: "rule_name",
      dataIndex: "rule_name",
      width: "30%"
    },
    {
      title: "Matches",
      key: "count",
      dataIndex: "count",
      width: "10%"
    }
  ];

  const data = Object.keys(hits).map((key: any) => ({ rule_name: rules[key], count: hits[key] }));

  const matchesCount = Object.values(hits).reduce((acc, count) => acc + count, 0);

  return (
    <div className="jobMatchesTable">
      <div className="jobMatchesTable__head">
        <div style={{ display: "flex" }}>
          <AntText style={{ fontSize: "18px", color: "#000", marginRight: "8px" }}>Total Matches : </AntText>
          {resultLoading || triageLoading ? (
            <Loader />
          ) : (
            <AntText style={{ fontSize: "18px", color: "#000", fontWeight: "600" }}>{matchesCount}</AntText>
          )}
        </div>
        {!resultLoading && !triageLoading && matchesCount > 0 && (
          <AntButton type="link" size="small" onClick={() => setExpand(value => !value)}>
            {expand ? "Collapse" : "Expand"}
          </AntButton>
        )}
      </div>
      <SlideDown>
        {expand ? (
          <div className="jobMatchesTable__content">
            {resultLoading || triageLoading ? (
              <Loader />
            ) : (
              <AntTable dataSource={data} columns={columns} size={"small"} pagination={false} bordered={false} />
            )}
          </div>
        ) : null}
      </SlideDown>
    </div>
  );
};

export default JobMatchesTableComponent;
