import React from "react";
import { AntCol, AntText, AntTitle, TitleWithInfo } from "shared-resources/components";
import { StatsDescription, StatsMap } from "constants/stat.description";

interface SingleStatContainerProps {
  title: string;
  value: any;
  hasStat: string;
}

const SingleStatContainer: React.FC<SingleStatContainerProps> = (props: SingleStatContainerProps) => {
  return (
    <AntCol span={4}>
      <div style={{ textAlign: "center", borderRight: "1px solid #dcdfe4", marginTop: "5px" }}>
        <div className="centered">
          {Object.values(StatsMap).includes(props.hasStat as any) ? (
            <TitleWithInfo
              titleStyle={{ fontSize: "12px", color: "#8a94a5" }}
              title={props.hasStat.replace(/_/g, " ").toUpperCase()}
              description={(StatsDescription as any)[props.hasStat]}
            />
          ) : (
            <AntText type={"secondary"} style={{ fontSize: "12px" }}>
              {props.title}
            </AntText>
          )}
        </div>
        <div>
          <AntTitle level={3}>
            {typeof props.value === "object" ? `${props.value.time || 0} ${props.value.unit || ""}` : props.value || 0}
          </AntTitle>
        </div>
      </div>
    </AntCol>
  );
};

export default SingleStatContainer;
