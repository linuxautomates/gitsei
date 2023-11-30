import { Col, Row, Skeleton, Typography } from "antd";
import { PROPELS_ROUTES, getBaseUrl } from "constants/routePaths";
import { useGenericApi } from "custom-hooks";
import React from "react";
import { Link } from "react-router-dom";

const { Text } = Typography;

interface RunsLogsProps {
  propel: any;
}

const RunsLogs: React.FC<RunsLogsProps> = (props: RunsLogsProps) => {
  const runId = props.propel?.id || "";
  const propelId = props.propel.permanent_id || props.propel.runbook_id || "";

  const [propelLoading, propelData] = useGenericApi({ id: propelId, uri: "propels", method: "get" }, [runId]);

  const tableData = () => {
    let tableData: any[] = [];
    Object.keys(props.propel?.args || {}).forEach(arg => {
      tableData.push({ key: arg, value: props.propel?.args?.[arg]?.value });
    });
    return tableData;
  };

  const runsLogsRoute = `${getBaseUrl()}${PROPELS_ROUTES.RUNS_LOGS}?propelId=${propelId}&runId=${runId}&runbookId=${
    props.propel.runbook_id
  }&runState=${props.propel.state}`;

  const renderKeyValuePair = (dataToShow: Array<any>, dataLengthCheck: boolean) => {
    const widths = dataLengthCheck ? ["49%", "20%", "80%"] : ["100%", "15%", "85%"];
    return (
      <div style={{ width: widths[0] }}>
        {(dataToShow || []).map((data: { key: string; value: any }, index: number) => {
          return (
            <div key={index} style={{ display: "flex", height: "1.5rem", overflowY: "hidden" }}>
              <div style={{ width: widths[1] }}>
                <span className="mr-5" style={{ fontSize: "8px" }}>{`● `}</span>
                <Text>{`${data.key} : `}</Text>
              </div>
              <div style={{ width: widths[2] }}>
                <Text>{transformValue(data.value)}</Text>
              </div>
            </div>
          );
        })}
      </div>
    );
  };

  const transformValue = (item: any) => {
    if (typeof item === "object" && !Array.isArray(item)) {
      return JSON.stringify(item);
    }
    if (Array.isArray(item)) {
      return item.toString();
    }
    return item;
  };

  return (
    <div className="runs-logs">
      <Row>
        <Col span={24}>
          <div style={{ maxHeight: "12rem" }}>
            {propelLoading ? (
              <Skeleton active={true} />
            ) : (
              <div style={{ padding: "1rem" }}>
                <div className="my-10">
                  <strong>{`${propelData?.name} Arguments`}</strong>
                </div>
                <div
                  style={{ display: "flex", justifyContent: "space-between", maxHeight: "6rem", overflowY: "scroll" }}>
                  {renderKeyValuePair(tableData().slice(0, 5), tableData().length > 5)}
                  {tableData().length > 5 && renderKeyValuePair(tableData().slice(5, 10), true)}
                </div>
                <div className="my-10">
                  <Link target="_blank" to={runsLogsRoute}>
                    View More Details
                  </Link>
                </div>
              </div>
            )}
          </div>
        </Col>
      </Row>
    </div>
  );
};

export default RunsLogs;
