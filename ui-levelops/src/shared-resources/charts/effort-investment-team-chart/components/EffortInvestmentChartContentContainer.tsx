import React, { useCallback, useMemo, useRef, useState } from "react";
import { Divider, Icon } from "antd";
import BurndownLegendComponent from "shared-resources/charts/jira-burndown/Components/BurndownLegendComponent";
import { getColorMapping } from "shared-resources/charts/jira-effort-allocation-chart/helper";
import { AntButton, AntModal, AntTable, TitleWithCount } from "shared-resources/components";
import "./contentContainer.styles.scss";
import { getTableDynamicConfig } from "./helper";

const LIMIT_PER_CONTENT = 2;

const EffortInvestmentChartContentContainer: React.FC<{
  unit: string;
  team: any;
  showDivider: boolean;
  onClick: (e: any) => void;
}> = ({ team, showDivider, unit, onClick }) => {
  const [showMoreModal, setShowMoreModal] = useState<boolean>(false);
  const { dependency_ids: ids } = team;
  const mappingRef = useRef<any>(getColorMapping(ids));

  const handleToggleModal = useCallback(() => {
    setShowMoreModal(prev => !prev);
  }, []);

  const getMore = useMemo(() => {
    return (team?.assignees || []).length - LIMIT_PER_CONTENT;
  }, [team?.assignees]);

  const getTableDataSource = useMemo(
    () => (team?.assignees || []).slice(0, Math.min(LIMIT_PER_CONTENT, (team?.assignees || []).length)),
    [team?.assignees]
  );

  const getColumnsConfig = useMemo(
    () => getTableDynamicConfig(team?.assignees ? team?.assignees[0] : [], mappingRef.current, ids, unit, onClick),
    [team?.assignees, ids]
  );

  const renderMoreModal = useMemo(() => {
    return (
      <AntModal
        visible={showMoreModal}
        footer={null}
        closable
        onCancel={handleToggleModal}
        centered
        title={team?.name}
        className="more-modal">
        <AntTable dataSource={team?.assignees || []} columns={getColumnsConfig} pagination={false} />
      </AntModal>
    );
  }, [getColumnsConfig, team, showMoreModal]);

  return (
    <div key={team?.name} className="content">
      <div className="content-header">
        <TitleWithCount
          containerClassName="title-with-count-container"
          title={team?.name}
          count={Math.min(LIMIT_PER_CONTENT, team?.assignees?.length)}
          titleClass={"title-count-text"}
        />
        <Icon type="down" className="content-header-icon" />
      </div>
      <div className="content-table">
        <AntTable dataSource={getTableDataSource} columns={getColumnsConfig} pagination={false} />
      </div>
      <div className="content-footer">
        <div className="content-footer-container">
          <BurndownLegendComponent statusList={ids} mapping={mappingRef.current} />
          {!!getMore && <AntButton className="more-button" onClick={handleToggleModal}>{`+ ${getMore}`}</AntButton>}
        </div>
        {showDivider && <Divider className="divider-style" />}
      </div>
      {renderMoreModal}
    </div>
  );
};

export default EffortInvestmentChartContentContainer;
