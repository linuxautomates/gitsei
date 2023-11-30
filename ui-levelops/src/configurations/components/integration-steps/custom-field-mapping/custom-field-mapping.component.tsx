import React, { useCallback, useEffect, useMemo, useState, useRef, CSSProperties } from "react";
import { Icon, Spin } from "antd";
import { get } from "lodash";
import { useSelector } from "react-redux";
import { jiraFieldsList } from "reduxConfigs/actions/restapi";
import { jiraFieldsSelector } from "reduxConfigs/selectors/jira.selector";
import { AntSelect, AntTable, AntText, TableRowActions, TitleWithInfo } from "shared-resources/components";
import { SelectRestapi } from "shared-resources/helpers";
import { getColumns } from "../../../containers/integration-steps/integrations-details-new/table-config";

interface CustomFieldMappingProps {
  custom_field_list: any[];
  custom_field_mapping: any;
  loading: boolean;
  onAddClick: any;
  showDropdown?: boolean;
  integration_id: string;
  onDelimiterChange: any;
  onFieldRemove: any;
  onSelectBlur: any;
  onSelectFieldChange: any;
}

const CustomFieldMapping: React.FC<CustomFieldMappingProps> = props => {
  const {
    custom_field_list,
    custom_field_mapping: mapping,
    showDropdown,
    integration_id,
    loading,
    onAddClick,
    onDelimiterChange,
    onSelectBlur,
    onSelectFieldChange,
    onFieldRemove
  } = props;
  const { type, header, data, noMapping, paddingTop, uuid, header_info, singleSelectFields } = mapping;
  const [jiraFieldsLoaded, setJiraFieldsLoaded] = useState(false);

  const selectRestApiRef = useRef<any>();
  const customFieldMappingStyle = useMemo(
    () => ({
      width: "47%",
      padding: "0 1rem",
      display: "flex",
      flexDirection: "column",
      justifyContent: "flex-start",
      paddingTop: paddingTop
    }),
    [paddingTop]
  );
  const dropdownClassName = useMemo(() => `dropdown ${showDropdown ? "" : "hide-dropdown"}`, [showDropdown]);
  const dropdownStyle = useMemo(() => (showDropdown ? {} : { display: "none" }), [showDropdown]);
  const fullWidthStyle = useMemo(() => ({ width: "100%" }), []);
  const headerStyle = useMemo(() => ({ fontSize: "1rem", fontWeight: "bold", textTransform: "uppercase" }), []);
  const noMappingTextStyle = useMemo(
    () => ({ fontSize: "12px", color: "f1f1f1", display: "block", margin: "0.5rem" }),
    []
  );
  const options = useMemo(
    () => [
      { label: "None", value: "none" },
      { label: "Comma (,)", value: "," },
      { label: "Semicolon (;)", value: ";" },
      { label: "Colon (:)", value: ":" },
      { label: "Single quote (')", value: "'" },
      { label: "Pipe (|)", value: "\\|" },
      { label: "HTML List (<></>)", value: "html_list" }
    ],
    []
  );

  const jiraFieldState = useSelector(jiraFieldsSelector);

  useEffect(() => {
    const jira_fields = get(jiraFieldState, ["list"], {});
    if (!jiraFieldsLoaded && !!Object.keys(jira_fields).length) {
      const id = Object.keys(jira_fields)?.[0];
      const fieldsList = jira_fields?.[id];
      if (fieldsList?.data && fieldsList?.data?.records) {
        setJiraFieldsLoaded(true);
      }
    }
  }, [jiraFieldState]);

  useEffect(() => {
    if (showDropdown) {
      selectRestApiRef.current && selectRestApiRef.current.focus();
    }
  }, [showDropdown]);

  const buildAction = useCallback(
    (record: any, type: string) => {
      const action = [
        {
          type: "delete",
          id: record.id,
          description: "Delete",
          onClickEvent: onFieldRemove(type)
        }
      ];
      return <TableRowActions actions={action} />;
    },
    [data, onFieldRemove]
  );

  const buildDelimiter = useCallback(
    (record: any) => {
      const records = get(jiraFieldState, ["list", uuid, "data", "records"], []);
      if (!!records) {
        const currentField: any = records.find((r: any) => r?.field_key === record?.key);
        const currentFieldFromState = custom_field_list.find((s: any) => s?.key === currentField?.field_key);
        return currentField?.field_type === "string" ? (
          <AntSelect
            style={fullWidthStyle}
            dropdownClassName="full-width-dropdown"
            defaultValue={currentFieldFromState?.delimiter || "none"}
            onChange={onDelimiterChange(currentField?.field_key)}
            options={options}
          />
        ) : (
          "N/A"
        );
      }
    },
    [jiraFieldState, custom_field_list, onDelimiterChange]
  );

  const columns = useMemo(() => {
    let columns: any = getColumns(type);
    return columns.map((col: any) => {
      if (col.key === "id") {
        return {
          ...col,
          render: (item: any, record: any) => buildAction(record, type)
        };
      }
      if (col.key === "delimiter" && jiraFieldsLoaded) {
        return {
          ...col,
          render: (item: any, record: any) => buildDelimiter(record)
        };
      }
      return col;
    });
  }, [custom_field_list, data, jiraFieldState, jiraFieldsLoaded, type]);

  const handleAdd = useCallback(() => {
    onAddClick(type);
  }, [onAddClick, type]);

  const handleSelectBlur = useCallback(() => {
    onSelectBlur(type);
  }, [onSelectBlur, type]);

  const handleSelectFieldChange = useCallback(
    (data: any) => {
      onSelectFieldChange(type, data);
    },
    [onSelectFieldChange, type]
  );

  const handleSingleSelectFieldChange = useCallback(
    (data: any) => {
      onSelectFieldChange(type, data ? [data] : []);
    },
    [onSelectFieldChange, type]
  );

  const more_filters = useMemo(() => ({ integration_id: integration_id }), [integration_id]);

  const renderHeader = useMemo(() => {
    return header_info ? (
      <TitleWithInfo
        className="align-self-start"
        title={header}
        description={header_info}
        titleStyle={headerStyle as CSSProperties}
      />
    ) : (
      <AntText style={headerStyle}>{header}</AntText>
    );
  }, [header]);

  const noDataText = useMemo(() => {
    if (!data.length) {
      return <AntText style={noMappingTextStyle}>{`No ${noMapping} field mapping found`}</AntText>;
    }
    return null;
  }, [data]);

  const renderTable = useMemo(() => {
    if (!data.length) {
      return <AntText style={noMappingTextStyle}>{`No ${noMapping} field mapping found`}</AntText>;
    }

    return (
      <div className="custom-mapping-table-container">
        <AntTable bordered={false} columns={columns} dataSource={data} pagination={false} />
      </div>
    );
  }, [columns, data, noMapping]);

  const renderActionButton = useMemo(() => {
    let text = "";
    switch (type) {
      case "custom_field_list":
        text = "custom";
        break;
      case "salesforce_field_list":
        text = "salesforce";
        break;
      case "epics_field_list":
        text = "epic";
        break;
      case "story_points_field_list":
        text = "story point";
        break;
    }
    return (
      <div onClick={handleAdd} className="add-btn">
        <Icon className="add-icon" type="plus-circle" />
        {`Add ${text} field`}
      </div>
    );
  }, [onAddClick, type]);

  const renderSelect = useMemo(() => {
    const value = data || [];

    return (
      <SelectRestapi
        uuid={uuid}
        innerRef={selectRestApiRef}
        className={dropdownClassName}
        mode="multiple"
        uri="jira_fields"
        fetchData={jiraFieldsList}
        searchField="name"
        specialKey="field_key"
        value={value}
        labelinValue
        defaultOpen
        moreFilters={more_filters}
        dropdownStyle={dropdownStyle}
        autoFocus
        onChange={handleSelectFieldChange}
        onBlur={handleSelectBlur}
        allowClear={false}
        loadAllData={mapping.loadAllData || false}
      />
    );
  }, [data, showDropdown, type, integration_id, onSelectFieldChange, onSelectBlur]);

  const renderSingleSelect = useMemo(() => {
    const value = data[0] || [];
    return (
      <SelectRestapi
        uuid={uuid}
        className={"dropdown"}
        mode="single"
        uri="jira_fields"
        fetchData={jiraFieldsList}
        searchField="name"
        specialKey="field_key"
        value={value}
        moreFilters={more_filters}
        autoFocus
        onChange={handleSingleSelectFieldChange}
        allowClear={true}
        loadAllData={mapping.loadAllData || false}
      />
    );
  }, [data]);

  const spinner = useMemo(() => {
    return (
      <div className="spinner">
        <Spin />
      </div>
    );
  }, []);

  return (
    // @ts-ignore
    <div style={customFieldMappingStyle}>
      {loading ? (
        spinner
      ) : (
        <>
          {renderHeader}
          {!singleSelectFields && (
            <>
              {renderTable}
              {renderActionButton}
              {renderSelect}
            </>
          )}
          {singleSelectFields && noDataText}
          {singleSelectFields && renderSingleSelect}
        </>
      )}
    </div>
  );
};

export default React.memo(CustomFieldMapping);
