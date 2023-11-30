import React, { CSSProperties, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSelector } from "react-redux";
import { Icon, Spin } from "antd";
import { capitalize, get, uniqBy } from "lodash";
import { zendeskFieldListSelector } from "reduxConfigs/selectors/zendesk.selector";
import { getColumns } from "configurations/containers/integration-steps/integrations-details-new/table-config";
import { CUSTOM_FIELD_MAPPING_NOT_FOUND, ADD_CUSTOM_FIELD } from "constants/formWarnings";
import { AntSelect, AntTable, AntText, TableRowActions, TitleWithInfo } from "shared-resources/components";
import { SelectRestApiNew } from "shared-resources/helpers";
import {
  DelimiterOptions,
  ZendeskCustomMappingActionType,
  TITLE_DESCRIPTION,
  headerStyle,
  dropdownStyle
} from "./helper";
import "./zendesk-custom-field-mapping.styles.scss";

interface ZendeskCustomFieldMappingProps {
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

const ZendeskCustomFieldMappingComponent: React.FC<ZendeskCustomFieldMappingProps> = (
  props: ZendeskCustomFieldMappingProps
) => {
  const {
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

  let { type, header, data: custom_field_list, noMapping, uuid } = mapping;

  const [zendeskFieldsLoaded, setZendeskFieldsLoaded] = useState<boolean>(false);

  const zendeskFieldListState = useSelector(zendeskFieldListSelector);

  useEffect(() => {
    const zendesk_fields = zendeskFieldListState;
    if (!zendeskFieldsLoaded && !!Object.keys(zendesk_fields).length) {
      const id = Object.keys(zendesk_fields)?.[0];
      const fieldsList = zendesk_fields?.[id];
      if (fieldsList?.data && fieldsList?.data?.records) {
        setZendeskFieldsLoaded(true);
      }
    }
  }, [zendeskFieldListState]);

  const buildAction = useCallback(
    (record: any, type: string) => {
      const action = [
        {
          type: ZendeskCustomMappingActionType.DELETE,
          id: record.id,
          description: capitalize(ZendeskCustomMappingActionType.DELETE),
          onClickEvent: onFieldRemove(type)
        }
      ];
      return <TableRowActions actions={action} />;
    },
    [custom_field_list, onFieldRemove]
  );

  const buildDelimiter = useCallback(
    (record: any) => {
      const records = get(zendeskFieldListState, [uuid, "data", "records"], []);
      if (!!records) {
        const currentField: any = records.find((r: any) => r?.field_key === record?.key);
        const currentFieldFromState = custom_field_list.find((s: any) => s?.key === currentField?.field_key);
        return currentField?.field_type === "string" ? (
          <AntSelect
            dropdownClassName="full-width-dropdown"
            defaultValue={currentFieldFromState?.delimiter || "none"}
            onChange={onDelimiterChange(currentField?.field_key)}
            options={DelimiterOptions}
          />
        ) : (
          "N/A"
        );
      }
    },
    [zendeskFieldListState, custom_field_list, onDelimiterChange]
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
      if (col.key === "delimiter" && zendeskFieldsLoaded) {
        return {
          ...col,
          render: (item: any, record: any) => buildDelimiter(record)
        };
      }
      return col;
    });
  }, [custom_field_list, zendeskFieldListState, zendeskFieldsLoaded, type]);

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

  const more_filters = useMemo(() => ({ integration_ids: [integration_id] }), [integration_id]);

  const renderHeader = useMemo(() => {
    return <TitleWithInfo title={header} description={TITLE_DESCRIPTION} titleStyle={headerStyle as CSSProperties} />;
  }, [header]);

  const renderTable = useMemo(() => {
    if (!custom_field_list.length) {
      return <AntText className="no-mapping-text">{CUSTOM_FIELD_MAPPING_NOT_FOUND}</AntText>;
    }

    return (
      <div className="custom-mapping-table-container">
        <AntTable bordered={false} columns={columns} dataSource={custom_field_list} pagination={false} />
      </div>
    );
  }, [columns, custom_field_list, noMapping]);

  const renderActionButton = useMemo(() => {
    return (
      <div onClick={handleAdd} className="add-btn">
        <Icon className="add-icon" type="plus-circle" />
        {ADD_CUSTOM_FIELD}
      </div>
    );
  }, [onAddClick, type]);

  const renderSelect = useMemo(() => {
    custom_field_list = custom_field_list.map((field: any) => {
      return {
        ...field,
        key: parseInt(field.key)
      };
    });
    return (
      <SelectRestApiNew
        uuid={uuid}
        className={`dropdown ${showDropdown ? "" : "hide-dropdown"}`}
        mode="multiple"
        uri="zendesk_fields"
        searchField="title"
        specialKey="field_id"
        value={custom_field_list || []}
        moreFilters={more_filters}
        dropdownStyle={dropdownStyle(showDropdown)}
        autoFocus
        onChange={handleSelectFieldChange}
        onBlur={handleSelectBlur}
        allowClear={false}
      />
    );
  }, [custom_field_list, showDropdown, type, integration_id, onSelectFieldChange, onSelectBlur]);

  if (loading) {
    return (
      <div className="spinner">
        <Spin />
      </div>
    );
  }

  return (
    <div className="custom-fields-mapping">
      <>
        {renderHeader}
        {renderTable}
        {renderActionButton}
        {renderSelect}
      </>
    </div>
  );
};

export default ZendeskCustomFieldMappingComponent;
