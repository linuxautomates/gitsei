import { Card, Tooltip, Typography } from "antd";
import React, { useCallback } from "react";
import ReactJson from "react-json-view";

const { Text } = Typography;

interface LogCardProps {
  log: any;
}

export const LogCard: React.FC<LogCardProps> = (props: LogCardProps) => {
  const buildRow = useCallback((heading: string, data: string) => {
    return (
      <div className="mb-10" style={{ overflowWrap: "break-word" }}>
        <strong className="mr-10">{heading} :</strong>
        <Text>{data}</Text>
      </div>
    );
  }, []);

  const getErrorMessage = () => {
    if (props.log?.result?.errors?.[0]?.details?.script_error) {
      return buildRow("Message", JSON.stringify(props.log?.result?.errors?.[0]?.details?.script_error || {}));
    } else if (props.log?.result?.errors?.[0]?.details?.message) {
      return buildRow("Message", props.log?.result?.errors?.[0]?.details?.message || "");
    }
  };

  const getOutput = () => {
    return Object.keys(props.log.output || {}).map((out: any, index: number) => {
      const data = props.log.output?.[out];
      const widths = ["15%", "80%"];
      return (
        <div key={index} className="flex w-100" style={{ marginBottom: "1rem" }}>
          <div style={{ width: widths[0] }}>
            <span className="mr-5" style={{ fontSize: "8px" }}>{`● `}</span>
            <Tooltip title={data?.key}>
              <Text>{data?.key || "" + " :"}</Text>
            </Tooltip>
          </div>
          <div style={{ width: widths[1], wordWrap: "break-word", overflowX: "hidden", wordBreak: "break-word" }}>
            {data?.key === "link" ? (
              <a href={data?.value || ""} target="_blank">
                {data?.value || ""}
              </a>
            ) : (
              transformValue(data?.value || "")
            )}
          </div>
        </div>
      );
    });
  };

  const transformValue = useCallback((item: any) => {
    if (typeof item === "object" && !Array.isArray(item)) {
      return (
        <div className="flex direction-column">
          <ReactJson src={item} sortKeys name={false} collapsed={1} style={{ overflowWrap: "anywhere" }} />
        </div>
      );
    }
    return <Text>{Array.isArray(item) ? item.toString() : item}</Text>;
  }, []);

  const getLogData = () => {
    if (!props.log.state) {
      return null;
    }
    switch (props.log.state) {
      case "failure":
        return (
          <>
            {buildRow("Description", props.log?.result?.errors?.[0]?.description || "")}
            {getErrorMessage()}
          </>
        );
      default:
        return (
          <>
            <div style={{ overflowWrap: "break-word" }}>
              <strong className="mr-10">Output :</strong>
            </div>
            {getOutput()}
          </>
        );
    }
  };

  return <Card className="flex direction-column">{getLogData()}</Card>;
};
