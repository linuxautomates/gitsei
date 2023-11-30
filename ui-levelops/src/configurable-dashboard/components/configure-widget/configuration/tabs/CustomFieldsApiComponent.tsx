import { Tag } from "antd";
import Loader from "components/Loader/Loader";
import { jiraCustomFieldsList } from "configurable-dashboard/helpers/helper";
import { get, upperCase, cloneDeep } from "lodash";
import React, { useState } from "react";
import { useMemo } from "react";
import { useEffect } from "react";
import { useDispatch } from "react-redux";
import { genericList } from "reduxConfigs/actions/restapi";
import { getGenericUUIDSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { AntText } from "shared-resources/components";
import cx from "classnames";
import moment from "moment";
import { CustomTimeBasedTypes } from "../../../../../dashboard/graph-filters/components/helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface CustomFieldsApiComponentProps {
  integrationIds: string[];
  dashboardCustomFields: any;
  filterWidth?: "half" | "full";
  application?: string;
}

const CustomFieldsApiComponent: React.FC<CustomFieldsApiComponentProps> = ({
  integrationIds,
  dashboardCustomFields,
  filterWidth,
  application
}) => {
  const [customFields, setCustomFields] = useState<any>([]);
  const [loading, setloading] = useState<boolean>(false);
  const uuid = (integrationIds || []).sort().join("_");
  const normalCustomFields = get(dashboardCustomFields, ["normalcustomfields"], {});
  const partialCustomFields = get(dashboardCustomFields, ["partial"], {});
  const excludeCustomeFields = get(dashboardCustomFields, ["exclude"], {});
  const missingFields = get(dashboardCustomFields, ["missing_fields"], {});

  const uri = useMemo(() => {
    if (
      [
        IntegrationTypes.JIRA,
        IntegrationTypes.JIRAZENDESK,
        IntegrationTypes.JIRA_SALES_FORCE,
        IntegrationTypes.GITHUBJIRA
      ].includes(application as any)
    ) {
      return "jira_fields";
    } else if (application === IntegrationTypes.AZURE) {
      return "issue_management_workItem_Fields_list";
    } else if (application === IntegrationTypes.ZENDESK) {
      return "zendesk_fields";
    } else if (application === IntegrationTypes.TESTRAILS) {
      return "testrails_fields";
    }
    return "jira_fields";
  }, [application]);

  const integrationConfigListState = useParamSelector(getGenericUUIDSelector, {
    uri: uri,
    method: "list",
    uuid: uuid
  });
  const dispatch = useDispatch();
  useEffect(() => {
    const data = get(integrationConfigListState, ["data"], {});
    if (
      !Object.keys(data).length &&
      !loading &&
      (Object.keys(normalCustomFields || {}).length ||
        Object.keys(partialCustomFields || {}).length ||
        Object.keys(missingFields || {}).length ||
        Object.keys(excludeCustomeFields || {}).length)
    ) {
      dispatch(genericList(uri, "list", { filter: { integration_ids: integrationIds } } || {}, null, uuid));
      setloading(true);
    }
  }, [
    Object.keys(normalCustomFields || {}).length +
      Object.keys(partialCustomFields || {}).length +
      Object.keys(missingFields || {}).length +
      Object.keys(excludeCustomeFields || {}).length,
    integrationIds
  ]);

  useEffect(() => {
    const error = get(integrationConfigListState, ["error"], true);

    if (!error) {
      const data = get(integrationConfigListState, ["data", "records"], []);
      const customfieldsdata = data;
      if (application === IntegrationTypes.ZENDESK) {
        setCustomFields(
          customfieldsdata.map((item: any) => {
            return {
              ...item,
              field_key: item?.field_id?.toString(),
              name: item.title
            };
          })
        );
      } else {
        setCustomFields(customfieldsdata);
      }

      setloading(false);
    }
  }, [integrationConfigListState]);

  if (loading) {
    return <Loader />;
  }

  return (
    <>
      {Object.keys(normalCustomFields || {}).length > 0 &&
        !loading &&
        customFields.length > 0 &&
        Object.keys(normalCustomFields).map((item: any) => {
          let finalKey = "";
          if (application === IntegrationTypes.ZENDESK && item.includes("customfield_")) {
            finalKey = item.replace("customfield_", "");
          } else if (item.includes("Custom.")) {
            finalKey = item.replace("Custom.", "");
          } else if (item.includes("custom_")) {
            finalKey = item.replace("custom_.", "");
          } else {
            const newKey = item.split("_");
            finalKey = newKey.length > 2 ? newKey[1] + "_" + newKey[2] : newKey[0] + "_" + newKey[1];
          }
          const itemObject = customFields.find((object: any) => object.field_key === finalKey);
          if (CustomTimeBasedTypes.includes(itemObject?.field_type)) {
            return (
              <div className={cx("global-filters-div-wrapper", { "half-width": filterWidth === "half" })} key={item}>
                <div>
                  <AntText style={{ color: "#595959", fontSize: "10px", fontWeight: "bold" }}>
                    {upperCase(itemObject?.name || finalKey)}
                  </AntText>
                </div>
                <div className="global-filters-div">
                  <Tag key={item.field_key} className="widget-filter_tags">
                    {`${moment
                      .unix(parseInt(normalCustomFields[item]?.["$gt"] || ""))
                      .utc()
                      .format("MM-DD-YYYY")} `}
                    -
                    {` ${moment
                      .unix(parseInt(normalCustomFields[item]?.["$lt"] || ""))
                      .utc()
                      .format("MM-DD-YYYY")}`}
                  </Tag>
                </div>
              </div>
            );
          } else {
            if (normalCustomFields[item].length) {
              return (
                <div className={cx("global-filters-div-wrapper", { "half-width": filterWidth === "half" })} key={item}>
                  <div>
                    <AntText style={{ color: "#595959", fontSize: "10px", fontWeight: "bold" }}>
                      {upperCase(itemObject?.name || finalKey)}
                    </AntText>
                  </div>
                  <div className="global-filters-div">
                    {Array.isArray(normalCustomFields[item])
                      ? normalCustomFields[item].map((value: any) => (
                        <Tag key={value}>{`${value}`}</Tag>
                      ))
                      : <Tag key={normalCustomFields[item]}>{`${normalCustomFields[item]}`}</Tag>
                    }
                  </div>
                </div>
              );
            }
          }
        })}
      {Object.keys(partialCustomFields || {}).length > 0 &&
        !loading &&
        customFields.length > 0 &&
        Object.keys(partialCustomFields).map((item: any) => {
          if (Object.keys(partialCustomFields[item]).length) {
            let finalKey = "";
            if (item.includes("Custom.")) {
              finalKey = item.replace("Custom.", "");
            } else {
              const newKey = item.split("_");
              finalKey = newKey.length > 2 ? newKey[1] + "_" + newKey[2] : newKey[0] + "_" + newKey[1];
            }
            const itemObject = customFields.find((object: any) => object.field_key === finalKey);
            const value = partialCustomFields[item]["$begins"] || partialCustomFields[item]["$contains"];
            const startingvalue = partialCustomFields[item]["$begins"] ? "Start With" : "Contain";
            return (
              <div className={cx("global-filters-div-wrapper", { "half-width": filterWidth === "half" })} key={item}>
                <div>
                  <AntText style={{ color: "#595959", fontSize: "10px", fontWeight: "bold" }}>
                    {upperCase(itemObject?.name || finalKey)}
                  </AntText>
                </div>
                {<AntText style={{ fontSize: "10px" }}>{`Includes all the values that:${startingvalue}`}</AntText>}
                <div className="global-filters-div">{<Tag key={value}>{`${value}`}</Tag>}</div>
              </div>
            );
          }
        })}
      {Object.keys(excludeCustomeFields || {}).length > 0 &&
        !loading &&
        customFields.length > 0 &&
        Object.keys(excludeCustomeFields).map((item: any) => {
          if (excludeCustomeFields[item].length) {
            let finalKey = "";
            if (item.includes("Custom.")) {
              finalKey = item.replace("Custom.", "");
            } else {
              if (item.includes("_")) {
                const newKey = item.split("_");
                finalKey = newKey.length > 2 ? newKey[1] + "_" + newKey[2] : newKey[0] + "_" + newKey[1];
              } else {
                finalKey = item;
              }
            }
            const itemObject = customFields.find((object: any) => object.field_key === finalKey);
            return (
              <div className={cx("global-filters-div-wrapper", { "half-width": filterWidth === "half" })} key={item}>
                <div>
                  <AntText style={{ color: "#595959", fontSize: "10px", fontWeight: "bold" }}>
                    {upperCase(itemObject?.name || finalKey)}
                  </AntText>
                </div>
                {<AntText style={{ fontSize: "10px" }}>Excludes</AntText>}
                <div className="global-filters-div">
                  {excludeCustomeFields[item].map((value: any) => (
                    <Tag key={value}>{`${value}`}</Tag>
                  ))}
                </div>
              </div>
            );
          }
        })}
      {Object.keys(missingFields || {}).length > 0 &&
        !loading &&
        customFields.length > 0 &&
        Object.keys(missingFields).map((item: any) => {
          if (Object.keys(missingFields[item]).length) {
            let finalKey = "";
            if (item.includes("Custom.")) {
              finalKey = item.replace("Custom.", "");
            } else {
              const newKey = item.split("_");
              finalKey = newKey.length > 2 ? newKey[1] + "_" + newKey[2] : newKey[0] + "_" + newKey[1];
            }
            const itemObject = customFields.find((object: any) => object.field_key === finalKey);
            return (
              <div className={cx("global-filters-div-wrapper", { "half-width": filterWidth === "half" })} key={item}>
                <div>
                  <AntText style={{ color: "#595959", fontSize: "10px", fontWeight: "bold" }}>
                    {upperCase(itemObject?.name || finalKey)} (Missing Field)
                  </AntText>
                </div>
                <div>
                  <Tag style={{ maxWidth: "100%" }} key={missingFields[item]}>{`${missingFields[item]}`}</Tag>
                </div>
              </div>
            );
          }
        })}
    </>
  );
};

CustomFieldsApiComponent.defaultProps = {
  filterWidth: "full"
};

export default CustomFieldsApiComponent;
