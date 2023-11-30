import React from "react";
import { AntButton, AntCard, AntCol, AntIcon, AntRow, AntTooltip, SvgIcon } from "..";
import "./Tile.scss";
import { SvgIconComponent } from "../svg-icon/svg-icon.component";
import { TileType, IconType } from "./Tile.types";

interface TileProps {
  tile: TileType;
  icon?: IconType;
  handleManage?: (params: any) => void;
  handleSelect?: (params: any) => void;
  hasManageAccess?: boolean;
  isTrialUser?: boolean;
}

const Tile: React.FC<TileProps> = props => {
  const {
    tile = undefined,
    icon = undefined,
    handleSelect = () => {},
    handleManage = () => {},
    hasManageAccess = false,
    isTrialUser = false
  } = props;
  return (
    <div className="tile-wrapper">
      <AntCard className="tile-bordered-container home-card" onClick={() => handleSelect(tile)}>
        <AntRow className="flex tile-row">
          <AntRow className="tile-title flex">
            {icon && (
              <AntCol span={2} className="tile-icon" style={{ backgroundColor: icon?.bgColor }}>
                <SvgIcon icon={icon?.name} />
              </AntCol>
            )}
            <AntCol className="tile-name">
              <AntTooltip title={tile?.name}>{tile?.name}</AntTooltip>
            </AntCol>
          </AntRow>
          <AntRow className="tile-desc">
            <AntTooltip title={tile?.description}>{tile?.description}</AntTooltip>
          </AntRow>
          <AntRow className="tile-buttons">
            {tile && (
              <span className="select-btn">
                <AntButton>Select</AntButton>
              </span>
            )}
          </AntRow>
        </AntRow>
      </AntCard>
      <AntRow className="mng-btn-container">
        <span className="mng-btn">
          {hasManageAccess && tile && (
            <div className={"manage-category-icon-container"}>
              <AntIcon
                className={isTrialUser ? "disabled-manage-category-icon" : "manage-category-icon-container"}
                type="setting"
                onClick={!isTrialUser ? () => handleManage(tile) : () => {}}
              />
            </div>
          )}
        </span>
      </AntRow>
    </div>
  );
};

export default Tile;
