import React, { useCallback, useState } from "react";
import { Form } from "antd";
import { difference, get, uniqBy } from "lodash";
import { AntSelect, NewCustomFormItemLabel } from "shared-resources/components";
import { useEffect } from "react";
import { useDispatch } from "react-redux";
import { azureTeamsFilterValuesGet } from "reduxConfigs/actions/restapi";
import { azureFilterValueIdSelector } from "reduxConfigs/selectors/azure.selector";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { optionType } from "dashboard/dashboard-types/common-types";
import TagSelect from "./tag-select/TagSelect";
import { baseColumnConfig } from "../../../utils/base-table-config";
import { stringSortingComparator } from "./sort.helper";
import { ITEM_TEST_ID } from "./Constants";
import CustomTreeSelectComponent from "../../../shared-resources/components/custom-tree-select-component/custom-tree-select-component";

interface AzureTeamsFilterProps {
  filters: any;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean, addToMetaData?: any) => void;
  visibleFiltersConfig?: { teams: boolean; code_area: boolean };
  withDelete?: {
    key: string;
    onDelete: (key: string) => void;
    showDelete: boolean;
  };
}

const AZURE_TEAM_FILTER_ID = "AZURE_TEAM_FILTER_ID";
const AzureTeamsFilter: React.FC<AzureTeamsFilterProps> = ({
  onFilterValueChange,
  filters,
  visibleFiltersConfig,
  withDelete
}) => {
  const dispatch = useDispatch();
  const [azureTeamsOptions, setAzureTeamsOptions] = useState<optionType[] | undefined>(undefined);
  const [azureAreaOptions, setAzureAreaOptions] = useState<optionType[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const azureTeamsFilterOptionState = useParamSelector(azureFilterValueIdSelector, { id: AZURE_TEAM_FILTER_ID });
  const [activePopupKey, setActivePopupKey] = useState<string | undefined>("");

  useEffect(() => {
    const apidata = get(azureTeamsFilterOptionState, ["data", "records"], []);
    if (!apidata.length) {
      dispatch(
        azureTeamsFilterValuesGet(
          {
            fields: ["teams", "code_area"],
            filter: {
              integration_ids: get(filters, ["integration_ids"], [])
            }
          },
          AZURE_TEAM_FILTER_ID
        )
      );
    }
  }, []);

  useEffect(() => {
    if (loading) {
      const { loading: apiLoading, error } = azureTeamsFilterOptionState;
      if (!apiLoading && !error) {
        const apidata: any[] = get(azureTeamsFilterOptionState, ["data", "records"], []);
        if (apidata.length > 1) {
          const teamsRecords = get(apidata[0], ["teams", "records"], []).map((record: { key: string }) => ({
            label: record?.key,
            value: record?.key
          }));

          const codeAreaRecords = get(apidata[1], ["code_area", "records"], []).map((record: { key: string }) => {
            const { key } = record;
            let to = key.lastIndexOf("\\");
            const parentKey = to !== -1 ? key.substring(0, to) : undefined;
            const childKey = to == -1 ? key : key.substring(to + 1, key.length);
            return {
              key: childKey,
              value: key,
              parent_key: parentKey
            };
          });
          setAzureTeamsOptions(teamsRecords);
          setAzureAreaOptions(codeAreaRecords);
        }
      }
      setLoading(apiLoading);
    }
  }, [azureTeamsFilterOptionState]);

  const getAzureTeamsFilterValue = (filterKey: "teams" | "code_area") => {
    const filterValue: any[] = get(filters, ["workitem_attributes", filterKey], []);
    if (filterValue.length) {
      if (filterKey === "code_area") {
        return filterValue.map(v => (v?.child ? v.child : v));
      }
      return filterValue;
    }
    return [];
  };

  const handleFilterValueChange = (value: any, filterKey: "teams" | "code_area") => {
    const modifiedValue = {
      ...get(filters, ["workitem_attributes"], {}),
      [filterKey]: value
    };
    onFilterValueChange(modifiedValue, "workitem_attributes");
  };

  const onCodeAreaValueChange = (event: any) => {
    let { value, checked } = event;
    const filterKey = "code_area";
    const oldFilters = get(filters, ["workitem_attributes"], {});
    const oldFiltersCodeArea = get(oldFilters, filterKey, []).map((value: string) =>
      typeof value === "string" ? { child: value } : value
    );
    let newFiltersCodeArea: any = [];
    if (checked) {
      const newIncludes = azureAreaOptions.filter((v: any) => v?.value === value).map(v => ({ child: v?.value }));
      newFiltersCodeArea = uniqBy([...oldFiltersCodeArea, ...newIncludes], "child");
    } else {
      newFiltersCodeArea = oldFiltersCodeArea.filter((v: any) => !v?.child?.startsWith(value));
    }
    const modifiedValue = {
      ...oldFilters,
      [filterKey]: newFiltersCodeArea
    };
    onFilterValueChange(modifiedValue, "workitem_attributes");
  };

  const columnConfig = useCallback(
    (key: string) => [
      {
        ...baseColumnConfig("Value", key),
        sorter: stringSortingComparator(),
        sortDirections: ["descend", "ascend"]
      }
    ],
    []
  );

  const handleAzureAreasSelectChange = (event: string[]) => {
    const filterKey = "code_area";
    const oldFilters = get(filters, ["workitem_attributes"], {});
    const oldFiltersCodeArea = get(oldFilters, filterKey, []);
    const removed = difference(oldFiltersCodeArea, event) as string[];
    const modifiedValue = {
      ...oldFilters,
      [filterKey]: event
        .filter((_item: string) => !_item.startsWith(removed[0]))
        .map((_item: string) => ({ child: _item }))
    };
    onFilterValueChange(modifiedValue, "workitem_attributes");
  };

  const preventDefault = (e: any) => {
    e.preventDefault();
    e.stopPropagation();
  };
  return (
    <>
      {visibleFiltersConfig?.teams && (
        <Form.Item
          label={<NewCustomFormItemLabel label={"Azure Teams"} withDelete={withDelete} />}
          key={`${ITEM_TEST_ID}-azure-teams`}
          data-filterselectornamekey={`${ITEM_TEST_ID}-azure-teams`}
          data-filtervaluesnamekey={`${ITEM_TEST_ID}-azure-teams`}>
          {(azureTeamsOptions || []).length < 10 ? (
            <AntSelect
              showSearch
              dropdownTestingKey={`${ITEM_TEST_ID}-azure-teams_dropdown`}
              mode="multiple"
              value={getAzureTeamsFilterValue("teams")}
              options={azureTeamsOptions}
              showArrow={true}
              onChange={(value: string[]) => handleFilterValueChange(value, "teams")}
              filterOption={(input: string, option: any) => {
                const teamsOptionValue = get(option, ["props", "children"], "");
                return teamsOptionValue.toLowerCase().indexOf((input || "").toLowerCase()) >= 0;
              }}
            />
          ) : (
            <TagSelect
              switchValue={null}
              selectMode={"full"}
              partialValue={null}
              switchWithDropdown={null}
              isCustom={false}
              isVisible={activePopupKey === "Azure Teams"}
              columns={columnConfig("label")}
              valueKey={"value"}
              labelKey={"label"}
              dataKey={"Azure Teams"}
              filterValueLoading={false}
              dataSource={azureTeamsOptions || []}
              selectedValues={
                getAzureTeamsFilterValue("teams") && !Array.isArray(getAzureTeamsFilterValue("teams"))
                  ? [getAzureTeamsFilterValue("teams")]
                  : getAzureTeamsFilterValue("teams") || []
              }
              tableHeader={"Azure Teams"}
              onCancel={() => setActivePopupKey(undefined)}
              onPartialValueChange={(dataKey: string, data: any) => {}}
              onFilterValueChange={(data: any) => {
                let finalData = data;
                handleFilterValueChange(finalData, "teams");
                setActivePopupKey(undefined);
              }}
              onVisibleChange={(key: string | undefined) => setActivePopupKey(key)}
              onChange={(data: any) => {
                let finalData = data;
                handleFilterValueChange(finalData, "teams");
              }}
            />
          )}
        </Form.Item>
      )}
      {visibleFiltersConfig?.code_area && (
        <Form.Item
          label={<NewCustomFormItemLabel label={"Azure Areas"} withDelete={withDelete} />}
          key={`${ITEM_TEST_ID}-azure-areas`}
          data-filterselectornamekey={`${ITEM_TEST_ID}-azure-areas`}
          data-filtervaluesnamekey={`${ITEM_TEST_ID}-azure-areas`}>
          <AntSelect
            mode="multiple"
            dropdownClassName="azure-areas-select"
            value={getAzureTeamsFilterValue("code_area")}
            onChange={handleAzureAreasSelectChange}
            dropdownRender={(noData: any) => (
              <div className="customTree-component-wrapper">
                <div onMouseDown={preventDefault}>
                  {azureAreaOptions?.length === 0 && noData}
                  <CustomTreeSelectComponent
                    data={azureAreaOptions}
                    selected={getAzureTeamsFilterValue("code_area")}
                    onCheckboxValueChange={onCodeAreaValueChange}
                  />
                </div>
              </div>
            )}
          />
        </Form.Item>
      )}
    </>
  );
};

AzureTeamsFilter.defaultProps = {
  visibleFiltersConfig: { teams: true, code_area: true }
};

export default AzureTeamsFilter;
