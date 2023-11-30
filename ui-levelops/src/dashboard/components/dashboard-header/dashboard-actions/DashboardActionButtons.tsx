import React, { useCallback, useMemo, useState } from "react";
import { useSelector } from "react-redux";
import { Dropdown } from "antd";
import { useHistory } from "react-router-dom";

import { AntButton, AntIcon, AntTooltip } from "shared-resources/components";
import {
  DashboardActionButtonLabelType,
  DashboardActionMenuType,
  dashboardConfigureActionButtonData,
  getDashboardActionMenuLabel
} from "../helper";
import "./DashboardActionButtons.scss";
import ActionButtonMenu from "./dashboard-configure-action-menu/ActionButtonMenu";
import LocalStoreService from "../../../../services/localStoreService";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { ThemeType } from "antd/lib/icon";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";

interface DashboardActionButtonsProps {
  disableActions: boolean;
  handleMenuClick: (id: any) => void;
  showDropdown: boolean;
  widgetsCount?: number;
}

type triggerType = ("click" | "hover" | "contextMenu")[] | undefined;
type actionMenuType = { key: string; value: string }[];
type actionButtonType = {
  icon_type?: string;
  button_className: string;
  button_label: DashboardActionButtonLabelType;
  menuData: actionMenuType;
  theme?: ThemeType | undefined;
};

const DashboardActionButtons: React.FC<DashboardActionButtonsProps> = ({
  disableActions,
  handleMenuClick,
  showDropdown,
  widgetsCount
}) => {
  const ls = new LocalStoreService();
  const dashboard = useSelector(selectedDashboard);
  const history = useHistory();
  const [visible, setVisible] = useState<any>({});

  const [hasCreateAccess, hasEditAccess] = useConfigScreenPermissions();

  const getTriggers: triggerType = useMemo(() => ["click"], []);
  const userRole = useMemo(() => ls.getUserRbac()?.toString()?.toLowerCase() || "", []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleVisibleChange = useCallback(
    index => {
      return (value: boolean) => {
        setVisible({
          ...visible,
          [index]: value
        });
      };
    },
    [visible]
  );

  const onMenuClick = useCallback(
    (index: number) => {
      return (param: any) => {
        handleMenuClick(param);
        setVisible({
          ...visible,
          [index]: false
        });
      };
    },
    [visible, handleMenuClick]
  );

  const disableDropdownOption = (item: any) => {
    return item === DashboardActionMenuType.EXPORT && widgetsCount === 0 ? true : false;
  };

  const hasAccess = useMemo(() => window.isStandaloneApp || hasEditAccess, [hasEditAccess]);

  const actionButtonData: actionButtonType[] = useMemo(() => {
    if (hasAccess) {
      return dashboardConfigureActionButtonData;
    }
    return [
      {
        theme: "outlined" as ThemeType,
        icon_type: "more",
        button_className: "more-button",
        button_label: DashboardActionButtonLabelType.EMPTY,
        menuData: [
          {
            key: DashboardActionMenuType.EXPORT,
            value: getDashboardActionMenuLabel(DashboardActionMenuType.EXPORT),
            icon_type: "export"
          }
        ]
      }
    ];
  }, [userRole]);

  return (
    <>
      {actionButtonData.map((actionButton: actionButtonType, index: number) => {
        return (
          <AntTooltip title={!hasEditAccess && "You do not have edit access"}>
            <Dropdown
              overlayClassName="dash-action-buttons"
              key={`dropdown_${index}`}
              overlay={
                <ActionButtonMenu
                  disableDropdownOption={disableDropdownOption}
                  handleMenuClick={onMenuClick(index)}
                  menuData={actionButton.menuData}
                />
              }
              trigger={getTriggers}
              placement="bottomRight"
              disabled={!hasAccess || disableActions}
              visible={visible[index]}
              onVisibleChange={handleVisibleChange(index)}>
              <AntButton className={actionButton.button_className}>
                {actionButton.button_label}{" "}
                <AntIcon
                  type={actionButton.icon_type ? actionButton.icon_type : "down"}
                  theme={actionButton?.theme}
                  className="button-icon"
                />
              </AntButton>
            </Dropdown>
          </AntTooltip>
        );
      })}
    </>
  );
};

export default React.memo(DashboardActionButtons);
