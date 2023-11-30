import React, { useCallback, useMemo } from "react";
import { Icon } from "antd";
import { uniqBy } from "lodash";
import { AntButton, AntInput, AntSelect, AntTable, AntText } from "shared-resources/components";
import { toTitleCase } from "utils/stringUtils";
import { actionsColumn } from "utils/tableUtils";
import {
  configureAttributeDefaultDataSource,
  configureAttributeHint,
  configureAttributeSubHeading,
  ImportCsvsubHeading
} from "../../../Constants";

interface ConfigureAttributesProps {
  customFields: any[];
  setCustomFields: (val: any[]) => void;
  isEditable?: boolean;
  setIsEditable: (val: boolean) => void;
  exportSampleCsvHandler: () => void;
  exportExistingUsersHandler: () => void;
}

const ConfigureAttributes: React.FC<ConfigureAttributesProps> = (props: ConfigureAttributesProps) => {
  const {
    isEditable,
    setIsEditable,
    exportExistingUsersHandler,
    exportSampleCsvHandler,
    customFields,
    setCustomFields
  } = props;

  const handleOnChange = useCallback(
    (e: any, record: any, key: string) => {
      const newCustomFields = [...customFields];
      const value = key === "type" ? e : e.target.value;
      const field = newCustomFields.find((item: any) => item.title === record.title);
      if (field) {
        const index = newCustomFields.indexOf(field);
        field[key] = value;
        newCustomFields[index] = { ...field };
        setCustomFields(newCustomFields);
      }
    },
    [customFields]
  );

  const handleEdit = useCallback(() => {
    setIsEditable(true);
  }, []);

  const addNewCustomField = useCallback(() => {
    const lastIndex = customFields.reduce((acc: number, next: any) => {
      return acc > next.index ? acc : next.index || 1;
    }, 1);

    const newCustomField = {
      index: lastIndex + 1,
      title: "",
      type: "string",
      description: "",
      newField: true
    };
    const newCustomFields = uniqBy([...customFields, newCustomField], "title");
    setCustomFields(newCustomFields);
  }, [customFields]);

  const handleDelete = useCallback(
    (key: string) => {
      return () => {
        const newCustomFields = customFields.filter((item: any) => item.title !== key);
        setCustomFields([...newCustomFields]);
      };
    },
    [customFields]
  );

  const configureAttributesColumns = [
    {
      title: "Title",
      dataIndex: "title",
      key: 1,
      width: "200px",
      render: (item: any, record: any, index: any) => {
        if (record.newField) {
          return <AntInput defaultValue={record.title} onChange={(e: any) => handleOnChange(e, record, "title")} />;
        }
        if (record.required) {
          return (
            <AntText>
              {item} <span style={{ color: "red" }}>*</span>
            </AntText>
          );
        }
        return <AntText>{item}</AntText>;
      }
    },
    {
      title: "Type",
      dataIndex: "type",
      key: 2,
      render: (item: any, record: any, index: any) => {
        if (record.newField) {
          return (
            <AntSelect
              defaultValue={record.type === "string" ? "text" : record.type}
              value={record.type}
              onSelect={(value: any) => handleOnChange(value, record, "type")}
              style={{ width: "100px" }}
              options={[
                { label: "Text", value: "string" },
                { label: "Boolean", value: "boolean" },
                { label: "Date", value: "date" }
              ]}
            />
          );
        }
        return <AntText>{toTitleCase(item)}</AntText>;
      }
    },
    {
      title: "Description",
      dataIndex: "description",
      key: 3,
      width: "40%",
      render: (item: any, record: any, index: any) => {
        if (record.newField) {
          return (
            <AntInput
              defaultValue={record.description}
              onChange={(e: any) => handleOnChange(e, record, "description")}
            />
          );
        }
        return <AntText>{item}</AntText>;
      }
    },
    {
      ...actionsColumn(),
      title: "",
      width: 24,
      render: (item: any, record: any, index: any) => {
        if (record.newField) {
          return <Icon type="delete" onClick={handleDelete(record.title)} />;
        }
        return <div style={{ height: "14px", width: "14px" }} />;
      }
    }
  ];

  const modifiedDataSource = useMemo(() => {
    let newdataSource: any[] = [...configureAttributeDefaultDataSource];
    const moreRows = customFields.map((item: any) => {
      return {
        ...item,
        newField: isEditable
      };
    });
    newdataSource = [...newdataSource, ...moreRows];

    if (isEditable) {
      newdataSource.push({
        title: (
          <AntText>
            <a onClick={addNewCustomField}>Add Custom Attribute</a>
          </AntText>
        ),
        description: "Add a column for every custom field. For example, manager, tags, department.",
        default: true
      });
    } else {
      newdataSource.push({
        title: <AntButton onClick={handleEdit}>Configure Attributes</AntButton>,
        description: "Add a column for every custom field. For example, manager, tags, department.",
        default: true
      });
    }

    return newdataSource;
  }, [customFields, isEditable]);

  const exportContent = useMemo(() => {
    return (
      <div className="m-25">
        <AntButton icon={"download"} type={"secondary"} className={"mr-10"} onClick={exportSampleCsvHandler}>
          Export Sample CSV
        </AntButton>
        <AntButton icon={"download"} type={"secondary"} onClick={exportExistingUsersHandler}>
          Export Existing Contributors
        </AntButton>
      </div>
    );
  }, []);

  return (
    <div className="configure-attributes-container">
      <div className="m-25">
        <AntText className="sub-heading">{isEditable ? configureAttributeSubHeading : ImportCsvsubHeading}</AntText>
      </div>
      <AntTable
        className="m-25"
        columns={configureAttributesColumns}
        dataSource={modifiedDataSource}
        pagination={false}
      />
      <div className="m-25">
        <AntText className="footer-heading">{configureAttributeHint}</AntText>
      </div>
      {exportContent}
    </div>
  );
};

export default React.memo(ConfigureAttributes);