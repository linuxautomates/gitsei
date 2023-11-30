import Tag from "antd/lib/tag";
import Loader from "components/Loader/Loader";
import { get, upperCase } from "lodash";
import React, { useMemo } from "react";
import { useEffect, useState } from "react";
import { useDispatch } from "react-redux";
import { widgetFilterValuesGet, genericList } from "reduxConfigs/actions/restapi";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { AntText } from "shared-resources/components";
import { useSupportedFilters } from "custom-hooks";
import { jenkinsPipelineJobSupportedFilters } from "../../../constants/supported-filters.constant";
import { hasValue } from "../helper";
import moment from "moment";

interface MicrosoftGlobalFiltersProps {
  filters: any;
}

const TriageGridViewFilters: React.FC<MicrosoftGlobalFiltersProps> = ({ filters }) => {
  const filtersKeys = useMemo(() => Object.keys(filters || {}), [filters]);
  const dispatch = useDispatch();
  const [triageRuleData, setTriageRuleData] = useState<any[]>([]);
  const [rulesLoading, setRulesLoading] = useState<boolean>(false);

  const triageRulesState = useParamSelector(getGenericRestAPISelector, {
    uri: "triage_rules",
    method: "list",
    uuid: "0"
  });

  const { apiData, loading: apiDataLoading } = useSupportedFilters(
    jenkinsPipelineJobSupportedFilters,
    ["1"],
    "jenkins",
    [],
    true
  );

  useEffect(() => {
    const loading = get(triageRulesState, ["loading"], true);
    const error = get(triageRulesState, ["error"], true);
    if (!loading && !error) {
      const data = get(triageRulesState, ["data", "records"], []);
      setTriageRuleData(data);
      setRulesLoading(false);
    }
  }, [triageRulesState]);

  useEffect(() => {
    const data = get(triageRulesState, ["data", "records"], []);
    if (data.length === 0 && !rulesLoading) {
      if (filtersKeys.includes("triage_rule_ids")) {
        dispatch(genericList("triage_rules", "list", {}, null, "0"));
        setRulesLoading(true);
      }
    }
  }, [filtersKeys, triageRulesState]);

  const finalFilters = useMemo(() => {
    const final_filters: any = [];

    if (apiDataLoading) {
      return final_filters;
    }

    filtersKeys.forEach((filter_label: any) => {
      switch (filter_label) {
        case "job_ids":
        case "parent_job_ids":
          const index = (apiData || []).findIndex(
            (option: { [key: string]: any[] }) => Object.keys(option)[0] === "cicd_job_id"
          );
          const options: any[] = (Object.values(apiData[index] || {})[0] as any[]) || [];
          const values = filters[filter_label].map(
            (id: string) => options.find((option: any) => option.cicd_job_id === id)?.key
          );
          final_filters.push({
            label: filter_label === "job_ids" ? "Name" : "Parent Job Name",
            value: values
          });
          break;
        case "triage_rule_ids":
          if (triageRuleData.length) {
            let values: any = [];
            filters[filter_label].forEach((item: any) => {
              const index = triageRuleData.findIndex((option: any) => option.id === item);
              if (index !== -1) {
                values.push(triageRuleData[index]["name"]);
              }
            });
            if (values.length) {
              final_filters.push({
                label: "Triage Rules",
                value: values
              });
            }
          }
          break;
        case "start_time":
          let value = {
            $gt: "",
            $lt: ""
          };
          if (
            filters["start_time"].$gt.toString().includes("-") ||
            filters["start_time"].$lt.toString().includes("-")
          ) {
            value = {
              $gt: filters["start_time"].$gt,
              $lt: filters["start_time"].$lt
            };
          } else {
            value = {
              $gt: moment.unix(filters["start_time"].$gt).utc().format("MM/DD/YYYY"),
              $lt: moment.unix(filters["start_time"].$lt).utc().format("MM/DD/YYYY")
            };
          }

          final_filters.push({
            label: "date between",
            value
          });
          break;
        case "results":
          if (hasValue(filters[filter_label])) {
            final_filters.push({
              label: "status",
              value: filters[filter_label]
            });
          }
          break;

        default:
          if (hasValue(filters[filter_label])) {
            final_filters.push({
              label: filter_label,
              value: filters[filter_label]
            });
          }
          break;
      }
    });
    return final_filters;
  }, [apiData, filters, triageRuleData]);

  if (finalFilters.length === 0 && rulesLoading) {
    return <Loader />;
  }

  return (
    <div>
      {finalFilters.length > 0 &&
        finalFilters.map((item: any) => (
          <div className="global-filters-div-wrapper" key={item.label}>
            <AntText style={{ color: "#595959", fontSize: "10px", fontWeight: "bold" }}>
              {upperCase(item.label)}
            </AntText>
            {Array.isArray(item?.value) && (
              <div className="global-filters-div">
                {item?.value?.map((filter: any) => {
                  return <Tag key={filter}>{`${filter}`}</Tag>;
                })}
              </div>
            )}
            {!Array.isArray(item?.value) && (
              <div className="global-filters-div">
                <Tag key={item?.value?.$gt}>{`${item?.value?.$gt} - ${item?.value?.$lt}`}</Tag>
              </div>
            )}
          </div>
        ))}
    </div>
  );
};

export default TriageGridViewFilters;
