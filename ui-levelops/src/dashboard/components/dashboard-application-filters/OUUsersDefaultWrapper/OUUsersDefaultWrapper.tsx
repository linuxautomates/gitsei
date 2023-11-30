import React from "react";
import { ouUserDesignationType } from "configurations/configuration-types/OUTypes";
import { AntSelect } from "shared-resources/components";
import { DEFAULT_OU_USERS_INFO_TEXT, ouApplicationsConfig } from "../constants";
import "./OUUsersDefaultWrapper.styles.scss";
import { toTitleCase } from "../../../../utils/stringUtils";

interface OUUsersDefaulSettingInterface {
  availableApplications: string[];
  ouUserFilterDesignation: ouUserDesignationType;
  handleOUUserFilterDesignationChange: (type: string, value: string[]) => void;
}

const OUUsersDefaultWrapper: React.FC<OUUsersDefaulSettingInterface> = ({
  ouUserFilterDesignation,
  handleOUUserFilterDesignationChange
}) => {
  /**  logic for filtering OU Users filter */

  // const options = useMemo(
  //   () =>
  //     ouApplicationsConfig.filter(config => {
  //       return availableApplications.includes(config.application);
  //     }),
  //   [availableApplications]
  // );

  return (
    <div className="ou-default-users-container">
      <div className="header">
        <span className="sub-heading">Optional Settings - </span>
        <span>{DEFAULT_OU_USERS_INFO_TEXT}</span>
      </div>
      <div className="content-list-container">
        {ouApplicationsConfig.map(config => {
          return (
            <div className="content-item" key={config.application}>
              <div className="application-name">{toTitleCase(config.application || "")}</div>
              <div className="fields-select">
                <AntSelect
                  showArrow={true}
                  mode="multiple"
                  options={config.options}
                  value={ouUserFilterDesignation[config.application]}
                  onChange={(value: string[]) => handleOUUserFilterDesignationChange(config.application, value)}
                />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default OUUsersDefaultWrapper;
