import React, { ReactNode } from "react";
import { capitalize, forEach, get } from "lodash";
import { Tag } from "antd";
import { RestOrganizationUnit } from "classes/RestOrganizationUnit";
import { ouOverviewFieldsType } from "dashboard/dashboard-types/engineerScoreCard.types";
import { ouFields } from "../constants";
import { OrgUnitSectionPayloadType, sectionSelectedFilterType } from "configurations/configuration-types/OUTypes";
import { integrationParameters } from "configurations/constants";
import moment from "moment";
import { basicRangeType } from "dashboard/dashboard-types/common-types";

const TAG_COLOR = "#EAF0FC";
const OUOverviewComponent: React.FC<{ orgUnit: RestOrganizationUnit; tags: { name: string; id: string }[] }> = ({
  orgUnit,
  tags
}) => {
  const getFields = (value: ReactNode, label: string, labelType: "large" | "small") => {
    return {
      label: label,
      className: labelType === "large" ? "label" : "label-small",
      value: value ?? ""
    };
  };

  const getDynamicFilterField = (
    iteratee: sectionSelectedFilterType[],
    fieldsConfig: ouOverviewFieldsType[],
    key: string
  ) => {
    forEach(iteratee, (filter: sectionSelectedFilterType) => {
      let result = `${filter?.key ?? ""} ${capitalize((filter?.param ?? "").replace(/_/g, " ").toLowerCase())} ${
        filter?.value ?? ""
      }`;
      if (filter?.param === integrationParameters.IS_BETWEEN) {
        let value: basicRangeType = filter?.value ?? {};
        let date = "";
        if (Object.keys(value).length && typeof value.$gt === "string" && typeof value.$lt === "string") {
          const low = moment.unix(parseInt(value?.$gt ?? "")).format("YYYY-MM-DD");
          const high = moment.unix(parseInt(value?.$lt ?? "")).format("YYYY-MM-DD");
          date = `${low} - ${high}`;
        }
        if (date) {
          result = `${filter?.key ?? ""} ${capitalize((filter?.param ?? "").replace(/_/g, " ").toLowerCase())} ${date}`;
        }
      }
      fieldsConfig.push(getFields(result, key, "small"));
    });
  };

  const getFieldsTransformed = () => {
    let fieldsConfig: ouOverviewFieldsType[] = [];
    let value: ReactNode[] = [];
    forEach(ouFields, field => {
      switch (field) {
        case "name":
          fieldsConfig.push(getFields(orgUnit?.name ?? "", "Label", "large"));
          break;
        case "description":
          fieldsConfig.push(getFields(orgUnit?.description ?? "", "Description", "large"));
          break;
        case "managers":
          forEach(orgUnit?.managers || [], managers => {
            value.push(<Tag color={TAG_COLOR}>{managers?.full_name}</Tag>);
          });
          fieldsConfig.push(getFields(value, "Managers", "large"));
          break;
        case "tags":
          value = [];
          forEach(orgUnit?.tags || [], tag => {
            let tagObj = (tags ?? []).find(apiTag => apiTag?.id === tag);
            let tagName = tag;
            if (tagObj) {
              tagName = tagObj.name ?? "";
            }
            value.push(<Tag color={TAG_COLOR}>{tagName}</Tag>);
          });
          fieldsConfig.push(getFields(value, "Tags", "large"));
          break;
        case "sections":
          const sections: OrgUnitSectionPayloadType[] = orgUnit?.sections || [];
          forEach(sections, section => {
            fieldsConfig.push(getFields(section.type || "", "Integration", "large"));
            const filters = get(section, ["integration", "filters"], []);
            getDynamicFilterField(filters, fieldsConfig, "Filter");
            const dynamicUsers = get(section, ["user_groups", "dynamic_user_definition"], []);
            getDynamicFilterField(dynamicUsers, fieldsConfig, "Users");
          });
          break;
        case "default_section":
          const dynamicUsers = get(orgUnit?.default_section || {}, ["dynamic_user_definition"], []);
          getDynamicFilterField(dynamicUsers, fieldsConfig, "Users");
          break;
      }
    });
    return fieldsConfig;
  };

  return (
    <div className="org-unit-detail-container">
      <p className="title">Collection Definition</p>
      <div className="fields-container">
        {(getFieldsTransformed() || []).map((fieldConfig, index) => (
          <div className="field" key={index}>
            <div className={fieldConfig.className}>{fieldConfig?.label}</div>
            <div className="value">{fieldConfig.value}</div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default OUOverviewComponent;
