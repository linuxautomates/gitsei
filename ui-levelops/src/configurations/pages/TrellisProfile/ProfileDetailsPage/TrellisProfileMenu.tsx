import { Menu } from "antd";
import { get } from "lodash";
import React, { useMemo } from "react";
import { AntText } from "shared-resources/components";
import { convertEpochToDate, convertUnixToDate, DateFormats } from "utils/dateUtils";
import { SECTIONS, TRELLIS_PROFILE_MENU, TRELLIS_SECTION_MAPPING } from "../constant";

interface TrellisProfileMenuProp {
  onChange: (selectedMenu: any) => void;
  updatedAt?: string;
}

const TrellisProfileMenu: React.FC<TrellisProfileMenuProp> = ({ onChange, updatedAt }) => {
  const updatedDateTime = useMemo(() => {
    const header = <AntText style={{ color: "#7E7E7E" }}>LAST UPDATED: </AntText>;
    const dtTm = updatedAt ? convertUnixToDate(Number(updatedAt), "MM/DD/YYYY hh:mm") : "";
    const details = <AntText style={{ fontSize: "12px", color: "#7E7E7E" }}>{dtTm}</AntText>;

    return (
      <>
        {header}
        {details}
      </>
    );
  }, [updatedAt]);
  return (
    <Menu
      onClick={onChange}
      className="dev-score-profile-container-menu"
      defaultSelectedKeys={[TRELLIS_PROFILE_MENU.BASIC_INFO]}>
      <Menu.Item className="dev-score-profile-container-menu-item" key={TRELLIS_PROFILE_MENU.BASIC_INFO}>
        BASIC INFO
      </Menu.Item>
      <Menu.Item className="dev-score-profile-container-menu-item" key={TRELLIS_PROFILE_MENU.ASSOCIATIONS}>
        ASSOCIATIONS
      </Menu.Item>
      <Menu.Item className="dev-score-profile-container-menu-item" key={TRELLIS_PROFILE_MENU.FACTORS_WEIGHTS}>
        {"FACTORS & WEIGHTS"}
      </Menu.Item>
      {SECTIONS.map((section: any) => (
        <Menu.Item className="dev-score-profile-container-menu-item sub-menu" key={section}>
          {get(TRELLIS_SECTION_MAPPING, [section], section).toUpperCase()}
        </Menu.Item>
      ))}
      <Menu.Item disabled={true}>{updatedDateTime}</Menu.Item>
    </Menu>
  );
};

export default TrellisProfileMenu;
