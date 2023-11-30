import React, { useCallback, useEffect, useMemo, useState, useRef } from "react";
import { Icon, Spin } from "antd";
import { get } from "lodash";
import { AntSelect, AntTable, AntText, TableRowActions } from "shared-resources/components";
import { SelectRestApiNew } from "shared-resources/helpers";
import { getColumns } from "configurations/containers/integration-steps/integrations-details-new/table-config";
import { integrationMappingType } from "configurations/configuration-types/integration-edit.types";
import { azureFieldsListSelector } from "reduxConfigs/selectors/azure.selector";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { dropdownStyle } from "../zendesk-custom-field-mapping/helper";
import { CustomMappingDelimiterOptions } from "./azureCustomField.constants";
import "./azureCustomFieldMapping.styles.scss";
interface AzureCustomFieldMappingProps {
  custom_field_mapping: integrationMappingType;
  loading: boolean;
  showDropdown?: boolean;
  more_filters: {
    integration_ids: string[];
  };
  onAddClick: (type: string) => void;
  onDelimiterChange: (key: any) => (delimiter: any) => void;
  onFieldRemove: (type: string) => (id: any) => void;
  onSelectBlur: (type: string) => void;
  onSelectFieldChange: (type: string, data: any[]) => void;
}

const AzureCustomFieldMappingComponent: React.FC<AzureCustomFieldMappingProps> = props => {
  const {
    custom_field_mapping: mapping,
    showDropdown,
    more_filters,
    loading,
    onAddClick,
    onDelimiterChange,
    onSelectBlur,
    onSelectFieldChange,
    onFieldRemove
  } = props;
  const { type, header, data: custom_field_list, noMapping, uuid } = mapping;
  const [azureFieldsLoaded, setAzureFieldsLoaded] = useState(false);
  const [azureFields, setAzureFields] = useState<any[]>([]);
  const azureFieldState = useParamSelector(azureFieldsListSelector, { id: uuid });
  const selectRestApiRef = useRef<any>();

  useEffect(() => {
    if (!azureFieldsLoaded) {
      const { loading, error } = azureFieldState;
      if (!loading && !error) {
        const fieldRecords = get(azureFieldState, ["records"], []);
        setAzureFields(fieldRecords);
        setAzureFieldsLoaded(true);
      }
    }
  }, [azureFieldState]);

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
    [custom_field_list, onFieldRemove]
  );

  const buildDelimiter = useCallback(
    (record: any) => {
      if (azureFields.length) {
        const currentField: any = azureFields.find((r: any) => r?.field_key === record?.key);
        const currentFieldFromState = custom_field_list.find((s: any) => s?.key === currentField?.field_key);
        return currentField?.field_type === "string" ? (
          <AntSelect
            className="full-width"
            dropdownClassName="full-width-dropdown"
            defaultValue={currentFieldFromState?.delimiter || "none"}
            onChange={onDelimiterChange(currentField?.field_key)}
            options={CustomMappingDelimiterOptions}
          />
        ) : (
          "N/A"
        );
      }
    },
    [azureFields, custom_field_list, onDelimiterChange]
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
      if (col.key === "delimiter" && azureFieldsLoaded) {
        return {
          ...col,
          render: (item: any, record: any) => buildDelimiter(record)
        };
      }
      return col;
    });
  }, [custom_field_list, azureFieldState, azureFieldsLoaded, type]);

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

  const renderHeader = useMemo(() => {
    return <AntText className="azure-custom-mapping-container_header">{header}</AntText>;
  }, [header]);

  const renderTable = useMemo(() => {
    if (!custom_field_list.length) {
      return (
        <AntText className="azure-custom-mapping-container_no-mapping-text">{`No ${noMapping} field mapping found`}</AntText>
      );
    }
    const customFieldData = custom_field_list.map(field => {
      const currentField: any = azureFields.find((r: any) => r?.field_key === field?.key);
      return {
        ...(field || {}),
        name: currentField?.name || field?.key,
        label: currentField?.name || field?.key
      };
    });

    return (
      <div className="custom-mapping-table-container">
        <AntTable bordered={false} columns={columns} dataSource={customFieldData} pagination={false} />
      </div>
    );
  }, [columns, custom_field_list]);

  const renderActionButton = useMemo(() => {
    return (
      <div onClick={handleAdd} className="add-btn">
        <Icon className="add-icon" type="plus-circle" />
        {`Add custom field`}
      </div>
    );
  }, [onAddClick, type]);

  const renderSelect = useMemo(() => {
    return (
      <SelectRestApiNew
        uuid={uuid}
        className={`dropdown ${showDropdown ? "" : "hide-dropdown"}`}
        mode="multiple"
        uri="issue_management_workItem_Fields_list"
        searchField="name"
        specialKey="field_key"
        value={custom_field_list || []}
        moreFilters={more_filters}
        dropdownStyle={dropdownStyle(showDropdown)}
        autoFocus
        onChange={handleSelectFieldChange}
        onBlur={handleSelectBlur}
        allowClear={false}
      />
    );
  }, [custom_field_list, showDropdown, type, more_filters, onSelectFieldChange, onSelectBlur]);

  if (loading) {
    return (
      <div className="spinner">
        <Spin />
      </div>
    );
  }

  return (
    <div className="azure-custom-mapping-container">
      {renderHeader}
      {renderTable}
      {renderActionButton}
      {renderSelect}
    </div>
  );
};

export default AzureCustomFieldMappingComponent;
