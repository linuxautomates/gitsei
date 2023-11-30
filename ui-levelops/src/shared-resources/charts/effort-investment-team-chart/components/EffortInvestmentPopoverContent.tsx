import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { capitalize, forEach } from "lodash";
import React, { ReactNode, useCallback, useMemo } from "react";
import { AntButton, NameAvatar } from "shared-resources/components";
import { truncateAndEllipsis } from "utils/stringUtils";
import "./effortInvestmentPopoverContent.styles.scss";

interface EffortInvestmentPopoverProps {
  assigneeName: string;
  payload: basicMappingType<any>;
  unit?: string;
  showTotal: boolean;
  suffix: string;
  colorMapping: basicMappingType<string>;
  showViewReport: boolean;
}

const EffortInvestmentPopoverContent: React.FC<EffortInvestmentPopoverProps> = ({
  assigneeName,
  payload,
  unit,
  showTotal,
  suffix,
  showViewReport,
  colorMapping
}) => {
  const getTotal = useMemo(() => {
    let sum = 0;
    forEach(Object.keys(payload), key => {
      sum += payload[key] || 0;
    });
    return sum;
  }, [payload]);

  const getLabel = useCallback(
    key => {
      let finalValue: ReactNode = capitalize(key);

      if (colorMapping) {
        finalValue = (
          <div className="color-container">
            <div style={{ backgroundColor: colorMapping[key] }} className="color-avatar" />
            <div className="text">{finalValue}</div>
          </div>
        );
      }

      return finalValue;
    },
    [colorMapping]
  );

  return (
    <div className="popover-container">
      <div className="name-column">
        <div className="name-column-avatar">
          <NameAvatar name={assigneeName} />
        </div>
        <p className="name-column-text">{capitalize(assigneeName)}</p>
      </div>
      <div className="popover-container-content">
        {showTotal !== false && (
          <div className="row" key="total_tickets">
            <div className="total-col-1">{unit || "Total tickets"}</div>
            <div className="total-col-2">{getTotal}</div>
          </div>
        )}
        {Object.keys(payload ?? {}).map(key => (
          <div className="row" key={key}>
            <div className="col-1">{getLabel(key)}</div>
            <div className="col-2">{suffix ? `${payload[key]} ${suffix}` : payload[key]}</div>
          </div>
        ))}
      </div>
      {showViewReport !== false && (
        <div className="popover-container-lower">
          <div className="popover-container-lower-button_container">
            <AntButton className="report-button">View Report</AntButton>
          </div>
        </div>
      )}
    </div>
  );
};

export default React.memo(EffortInvestmentPopoverContent);
