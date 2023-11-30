import React, { useState, useEffect, useContext, useMemo, useRef } from "react";
import { AntTable } from "shared-resources/components";
import { ouScoreTableConfig } from "../../dev-productivity-report/component/ouTableConfig";

import "../../dev-productivity-report/component/ouScoreTableComponent.styles.scss";
import { legendColorByRating } from "../../../helpers/devProductivityRating.helper";
import { engineerRatingType } from "../../../constants/devProductivity.constant";
import { timeInterval } from "../../../constants/devProductivity.constant";

interface DemoOUScoreTableComponentProps {
  searchValue: string;
  interval?: string;
  supportedSections?: Array<string>;
  trellisScoreDevData?: any;
}

const DemoOUScoreTableComponent: React.FC<DemoOUScoreTableComponentProps> = ({
  searchValue,
  interval,
  supportedSections,
  trellisScoreDevData
}) => {
  const [showBlurred, setShowBlurred] = useState<boolean>(true);
  const [trellisDevData, setTrellisDevData] = useState<any>([]);

  useEffect(() => {
    let filteredData =
      trellisScoreDevData.length > 0 &&
      trellisScoreDevData.filter((data: any) =>
        (data?.name + "" || "").toLowerCase().includes((searchValue ?? "").toLowerCase())
      );
    setTrellisDevData(filteredData);
  }, [trellisScoreDevData, supportedSections, searchValue]);

  return (
    <div className="ou-score-table-container">
      {trellisDevData.length > 0 && (
        <AntTable
          className="ou-score-table"
          columns={ouScoreTableConfig({
            count: trellisScoreDevData.length - 1,
            handleBlurChange: setShowBlurred,
            isBlur: showBlurred,
            sections: supportedSections,
            interval:  interval?.toLowerCase(),
            isDemo: true
          })}
          dataSource={trellisDevData ?? []}
        />
      )}
      <div className="ou-score-legend">
        {Object.values(engineerRatingType).map(key => {
          return (
            <div className="legend">
              <div className="shape" style={{ backgroundColor: legendColorByRating(key) }} />
              <span className="text">{key.toLowerCase()}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default DemoOUScoreTableComponent;
