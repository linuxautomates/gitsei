import React, { useEffect, useMemo } from "react";
import { ouScoreWidgetsConfig } from "dashboard/dashboard-types/engineerScoreCard.types";
import { AntCard, AntInput } from "shared-resources/components";
import { scoreWidgetType } from "dashboard/pages/scorecard/constants";
import DemoOUScoreTableComponent from "../component/DemoOUScoreTableComponent";
import { useState } from "react";

interface DemoDevWidgetContainerProps {
  widget: ouScoreWidgetsConfig;
  ou_uuid: string;
  supportedSections?: Array<string>;
  trellisScoreDevData?: any;
  interval?: string;
}

const DemoDevWidgetContainer: React.FC<DemoDevWidgetContainerProps> = ({
  widget,
  ou_uuid,
  interval,
  supportedSections,
  trellisScoreDevData
}) => {
  const [searchValue, setSearchvalue] = useState<string>("");
  const [trellisScoreDev, setTrellisScoreDev] = useState<any>([]);

  useEffect(() => {
    setTrellisScoreDev(trellisScoreDevData);
  }, [trellisScoreDevData]);

  const renderContent = useMemo(() => {
    switch (widget?.widget_type) {
      case scoreWidgetType?.SCORE_TABLE:
        return (
          <DemoOUScoreTableComponent
            searchValue={searchValue}
            interval={interval}
            supportedSections={supportedSections}
            trellisScoreDevData={trellisScoreDev}
          />
        );
    }
  }, [widget, searchValue, trellisScoreDev, supportedSections]);

  const rendreWidgetExtra = useMemo(() => {
    return (
      <div className="dev-widget-extra">
        <AntInput
          placeholder="Search For Name"
          value={searchValue}
          onChange={(e: any) => setSearchvalue(e.target.value)}
        />
      </div>
    );
  }, [searchValue]);

  return (
    <AntCard
      className="dev-widget-container"
      title={widget?.name}
      style={{
        height: widget?.height,
        width: widget?.width
      }}
      extra={rendreWidgetExtra}>
      {renderContent}
    </AntCard>
  );
};

export default DemoDevWidgetContainer;
