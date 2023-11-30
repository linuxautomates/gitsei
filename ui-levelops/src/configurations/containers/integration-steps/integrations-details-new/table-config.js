import { actionsColumn } from "../../../../utils/tableUtils";
const commonFieldColumns = [
  {
    title: "Custom Field Name",
    dataIndex: "label",
    key: "label",
    width: "40%",
    ellipsis: true
  },
  {
    title: "Field Name",
    dataIndex: "key",
    key: "key",
    width: "40%",
    ellipsis: true
  },
  { ...actionsColumn() }
];
const customFieldColumns = [
  // used for custom field mappings section
  {
    title: "Custom Field Name",
    dataIndex: "label",
    key: "label",
    width: "40%",
    ellipsis: true
  },
  {
    title: "Delimiter",
    dataIndex: "delimiter",
    key: "delimiter",
    width: "40%",
    ellipsis: true
  },
  { ...actionsColumn() }
];

const repoFieldColumns = [
  {
    title: "Repo Name",
    dataIndex: "repo_id",
    key: "repo_id",
    width: "40%",
    ellipsis: true
  },
  {
    title: "Depot Path",
    dataIndex: "path_prefix",
    key: "path_prefix",
    width: "40%",
    ellipsis: true
  },
  { ...actionsColumn() }
];

export const getColumns = type => {
  // function that would return a fine tuned column list
  if (type === "custom_field_list") {
    return customFieldColumns;
  } else if (type === "repo_paths_list") {
    return repoFieldColumns;
  } else {
    return commonFieldColumns;
  }
};
