import React, { useCallback, useMemo } from "react";
import { AntCardComponent as AntCard } from "../../ant-card/ant-card.component";
import { AntTextComponent as AntText } from "../../ant-text/ant-text.component";
import "./instance-card.scss";
import { Line, LineChart, ResponsiveContainer } from "recharts";
import { getInstanceStatus } from "../helper";
import cx from "classnames";
import SanitizedLink from "../../SanitizedLInk";
// @ts-ignore
import { instanceCardDisplayFields } from "./instanceCard.constants";

interface Instance {
  id: string;
  name: string;
  url: string;
  last_hb_at: number;
  created_at: number;
  updated_at: number;
  chartData?: Array<Object>;
  details: {
    jenkins_version: string;
    plugin_version: string;
  };
}

interface InstanceCardProps {
  instance: Instance;
  attached: boolean;
  className?: any;
  onAttach: (id: Instance) => void;
  onDetach: (id: Instance) => void;
  attaching?: boolean;
  detaching?: boolean;
  disabled?: boolean;
}

const InstanceCard: React.FC<InstanceCardProps> = ({
  instance,
  attached,
  className,
  onAttach,
  onDetach,
  attaching,
  detaching,
  disabled
}) => {
  const { details, url, chartData, name } = instance;

  const status = useMemo(() => getInstanceStatus(instance), [instance]);

  const isDown = useMemo(() => status?.toUpperCase() === "DOWN", [status]);

  const headerStyle = useMemo(
    () => ({
      backgroundColor: `${!isDown ? "#F5F5F5" : "#FEF1F0"}`,
      padding: "0 12px"
    }),
    [isDown]
  );

  const handleAttachNode = useCallback((instance: Instance) => !disabled && onAttach(instance), []);
  const handleDetachNode = useCallback((instance: Instance) => !disabled && onDetach(instance), []);

  return (
    <AntCard
      size="small"
      className={`instance-card ${className}`}
      headStyle={headerStyle}
      title={<AntText className={"instance-card-title"}>{name}</AntText>}
      extra={
        <AntText
          className={!isDown ? "instance-card-status text-green" : "instance-card-status"}
          type={!isDown ? "success" : "danger"}>
          {status}
        </AntText>
      }>
      <div className={"instance-card-body"}>
        {instanceCardDisplayFields(details).map(instanceDetail => (
          <div className={"key-value-pair"} key={instanceDetail.key}>
            <AntText className={"key-pair-key"} type={"secondary"}>
              {instanceDetail.label}
            </AntText>
            <AntText className={"key-pair-value"} ellipsis={true}>
              {instanceDetail.value}
            </AntText>
          </div>
        ))}
        <div className={"key-value-pair"}>
          <AntText className={"key-pair-key"} type={"secondary"}>
            Base URL
          </AntText>
          <AntText className={"key-pair-value"} ellipsis={true}>
            <SanitizedLink url={url} />
          </AntText>
        </div>
        {attached && chartData?.length && (
          <div className={"key-value-pair chart-container"}>
            <AntText className={"key-pair-key"} type={"secondary"}>
              Usage
            </AntText>
            <ResponsiveContainer height="100%" width="100%">
              <LineChart width={500} data={chartData} height={300}>
                <Line dataKey="usage" stroke="#82ca9d" dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
        <div className={"custom-attach-button"}>
          {attached ? (
            <AntText
              className={cx("text-red", { cursor_disabled: disabled })}
              type={"danger"}
              onClick={() => handleDetachNode(instance)}>
              {detaching ? "Detaching Node" : "Detach Node"}
            </AntText>
          ) : (
            <AntText
              className={cx("text-green", { cursor_disabled: disabled })}
              onClick={() => handleAttachNode(instance)}>
              {attaching ? "Attaching Node" : "Attach Node"}
            </AntText>
          )}
        </div>
      </div>
    </AntCard>
  );
};

export default InstanceCard;
