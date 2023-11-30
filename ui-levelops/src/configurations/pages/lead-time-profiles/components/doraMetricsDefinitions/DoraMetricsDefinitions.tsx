import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { get } from "lodash";
import React from "react";
import { AntIcon, AntText, AntTitle } from "shared-resources/components";
import { DORA_CONFIG_METRICS } from "../../helpers/constants";
import "./doraMetricsDefinitions.scss";

const DoraMetricsDefinitions = (props: any) => {
  const { selectedMetric, setSelectedMetric } = props;

  return (
    <div className="lead-time-definitions">
      <div className="header">
        <AntTitle level={4} className="basic-info-container-title mb-0">
          DEFINITIONS
        </AntTitle>
        <AntText className="description">These definitions are used to calculate DORA metrics</AntText>
      </div>
      {DORA_CONFIG_METRICS.map((item: { label: string; value: string }) => (
        <div key={item.value} className={`pt-5 ${selectedMetric === item ? "selected-config" : ""}`}>
          <AntText className="pr-5">
            {item.label === "RELEASES" ? item.label.replace("RELEASES", "NEW FEATURE") : item.label}
          </AntText>
          <AntIcon type="edit" style={{ cursor: "pointer" }} onClick={() => setSelectedMetric(item)} />
        </div>
      ))}
    </div>
  );
};

export default DoraMetricsDefinitions;
