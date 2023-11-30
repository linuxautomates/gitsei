import React, { useEffect, useMemo, useState } from "react";

import "./CsvFormatModal.scss";
import {
  AntButton,
  AntInput,
  AntModal,
  AntSelect,
  AntTable,
  AntText
} from "../../../../../shared-resources/components";
import { useDispatch } from "react-redux";
import { OrgUserSchemaGet } from "reduxConfigs/actions/restapi/orgUserAction";
import { useHistory } from "react-router-dom";
import { parseQueryParamsIntoKeys } from "utils/queryUtils";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { orgUsersGenericSelector } from "reduxConfigs/selectors/orgUsersSelector";
import { get } from "lodash";
import { actionsColumn } from "utils/tableUtils";
import { Icon } from "antd";

interface CsvFormatModal {
  dataSource?: {
    default: boolean;
    required: boolean;
    key: string;
    title: string;
    type: string;
    description: string;
  }[];
  // columns: {
  //   title: string;
  //   dataIndex: string;
  //   key: number;
  // }[];
  subHeading: string;
  configureAttributeModel: boolean;
  configureAttributesHandler: () => void;
  exportSampleCsvHandler: () => void;
  exportExistingUsersHandler: () => void;
}

const footerHeading =
  " You can also export the existing user list (or a sample CSV file), add information and re-import the CSV.";

const CsvFormatModal: React.FC<CsvFormatModal> = ({
  exportSampleCsvHandler,
  exportExistingUsersHandler,
  dataSource,
  subHeading,
  configureAttributeModel,
  configureAttributesHandler
}) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const search = history.location.search;
  const queryObject = parseQueryParamsIntoKeys(search, ["version"]);
  const [customFields, setCustomFields] = useState<any[]>([]);
  let id: any = 0;
  if (Object.keys(queryObject).length) {
    id = `version=${queryObject.version[0]}`;
  }
  const userSchemaState = useParamSelector(orgUsersGenericSelector, {
    uri: "org_users_schema",
    method: "get",
    id: id
  });

  useEffect(() => {
    let id: any = 0;
    if (Object.keys(queryObject).length) {
      id = `version=${queryObject.version[0]}`;
    }
    dispatch(OrgUserSchemaGet(id, "org_users_schema"));
  }, []);

  useEffect(() => {
    const loading = get(userSchemaState, "loading", true);
    const error = get(userSchemaState, "error", true);
    if (!loading && !error) {
      const data = get(userSchemaState, ["data", "custom_fields"], []);
      setCustomFields(data);
    }
  }, [userSchemaState]);

  const configureAttributesDataSource = [
    {
      key: 2,
      title: "Email",
      type: "Text",
      description: "Unique email address per member",
      default: true,
      required: true
    },
    {
      key: 1,
      title: "Name",
      type: "Text",
      description: "Name",
      default: true,
      required: true
    },
    {
      key: 3,
      title: "Integration",
      type: "Text",
      description: "Add a column for every integration",
      default: true,
      required: true
    },
    {
      key: 4,
      title: "Start Date",
      type: "Date",
      description: "Contributors start date",
      default: true
    }
  ];

  const handleOnChange = (e: any, record: any, index: any, key: string) => {
    record[key] = key === "type" ? e : e.target.value;
    const newCustomField = [...customFields];
    newCustomField[index - 4] = record;
    setCustomFields(newCustomField);
  };

  const configureAttributesColumns = [
    {
      title: "Title",
      dataIndex: "title",
      key: 1,
      width: "200px",
      render: (item: any, record: any, index: any) => {
        if (record.newField) {
          return <AntInput value={record.title} onChange={(e: any) => handleOnChange(e, record, index, "title")} />;
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
        if (record.default) {
          return <AntText>{item}</AntText>;
        }
        return (
          <AntSelect
            defaultValue={record.type === "string" ? "text" : record.type}
            value={record.type}
            onSelect={(value: any) => handleOnChange(value, record, index, "type")}
            style={{ width: "100px" }}
            options={[
              { label: "Text", value: "string" },
              { label: "Boolean", value: "boolean" },
              { label: "Date", value: "date" }
            ]}
          />
        );
      }
    },
    {
      title: "Description",
      dataIndex: "description",
      key: 3,
      width: 200,
      render: (item: any, record: any, index: any) => {
        if (record.newField) {
          return (
            <AntInput
              value={record.description}
              onChange={(e: any) => handleOnChange(e, record, index, "description")}
            />
          );
        }
        return <AntText>{item}</AntText>;
      }
    },
    {
      ...actionsColumn(),
      title: "",
      width: "",
      render: (item: any, record: any, index: any) => {
        if (!record.default) {
          return <Icon type="delete" />;
        }
      }
    }
  ];

  const addNewCustomField = () => {
    const newCustomField = {
      title: "",
      type: "",
      description: "",
      newField: true
    };
    const newCustomFields = [...customFields, newCustomField];
    setCustomFields(newCustomFields);
  };

  const modifiedDataSource = useMemo(() => {
    let newdataSource = [...configureAttributesDataSource];
    const moreRows = customFields.map((item: any) => {
      return {
        ...item,
        title: item?.display_name || item.title || ""
      };
    });
    newdataSource = [...newdataSource, ...moreRows];
    const buttonRow: any = configureAttributeModel
      ? {
          title: (
            <AntText>
              <a onClick={addNewCustomField}>Add Custom Attribute</a>
            </AntText>
          ),
          description: "Add a column for every custom field. For example, manager, tags, department.",
          default: true
        }
      : {
          title: <AntButton onClick={configureAttributesHandler}>Configure Attributes</AntButton>,
          description: "Add a column for every custom field. For example, manager, tags, department.",
          default: true
        };
    newdataSource = [...newdataSource, buttonRow];
    return newdataSource;
  }, [customFields, configureAttributeModel]);

  const exportContent = useMemo(() => {
    return (
      <div className="m-25">
        <AntButton icon={"upload"} type={"secondary"} className={"mr-10"} onClick={exportSampleCsvHandler}>
          Export Sample CSV
        </AntButton>
        <AntButton icon={"upload"} type={"secondary"} onClick={exportExistingUsersHandler}>
          Export Existing Contributors
        </AntButton>
      </div>
    );
  }, []);

  return (
    <div className={"csv-upload-container"}>
      <div className={"m-25"}>
        <AntText className="sub-heading">{subHeading}</AntText>
      </div>
      <AntTable
        className={"m-25"}
        columns={configureAttributesColumns.slice(0, 3)}
        dataSource={dataSource ? dataSource : modifiedDataSource}
        pagination={false}
      />
      <div className={"m-25"}>
        <AntText className="footer-heading">{footerHeading}</AntText>
      </div>
      {exportContent}
    </div>
  );
};

export default CsvFormatModal;
